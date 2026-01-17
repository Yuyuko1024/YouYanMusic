package com.youyuan.music.compose.service

import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.youyuan.music.compose.BuildConfig
import com.youyuan.music.compose.R
import com.youyuan.music.compose.pref.PlayerSeekToPreviousAction
import com.youyuan.music.compose.pref.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicPlaybackService : MediaLibraryService() {

    companion object {
        const val CUSTOM_COMMAND_CLOSE = "${BuildConfig.APPLICATION_ID}.COMMAND_CLOSE"
        const val CUSTOM_COMMAND_FAVORITE = "${BuildConfig.APPLICATION_ID}.COMMAND_FAVORITE"

        // 自定义action
        const val ACTION_EXIT_APP = "${BuildConfig.APPLICATION_ID}.ACTION_EXIT_APP"
    }

    // 设置数据存储
    private lateinit var settingsDataStore: SettingsDataStore

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        settingsDataStore = SettingsDataStore(this)

        initializePlayer()
        initializeSession()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()
        player?.repeatMode = Player.REPEAT_MODE_ALL
    }

    @OptIn(UnstableApi::class)
    private fun initializeSession() {
        player?.let { player ->
            val sessionActivityPendingIntent =
                packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                    PendingIntent.getActivity(
                        this,
                        0,
                        sessionIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }

            // 设置自定义Player包装类
            val wrapperPlayer = MyAppPlayerWrapper(
                exoPlayer = player,
                settingsDataStore = settingsDataStore
            )

            mediaLibrarySession = MediaLibrarySession.Builder(this, wrapperPlayer,
                LibrarySessionCallback()
            )
                .setSessionActivity(sessionActivityPendingIntent!!)
                .build()

            val provider : DefaultMediaNotificationProvider = DefaultMediaNotificationProvider.Builder(this)
                .build()
            val notificationProvider = provider.apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
            }
            setMediaNotificationProvider(notificationProvider)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        player?.release()
        player = null
        super.onDestroy()
    }

    private class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .build()
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                connectionResult.availablePlayerCommands
            )
        }
    }

    // 我们的自定义Player包装类
    @UnstableApi
    private class MyAppPlayerWrapper(
        exoPlayer: ExoPlayer,
        private val settingsDataStore: SettingsDataStore
    ) : ForwardingPlayer(exoPlayer) {

        private var previousAction: PlayerSeekToPreviousAction = PlayerSeekToPreviousAction.DEFAULT

        init {
            CoroutineScope(Dispatchers.Main).launch {
                settingsDataStore.playerSeekToPreviousAction.collect { actionOrdinal ->
                    previousAction = PlayerSeekToPreviousAction.entries.getOrNull(actionOrdinal)
                        ?: PlayerSeekToPreviousAction.DEFAULT
                }
            }
        }

        // 修改过的上一曲方法
        override fun seekToPrevious() {
            when (previousAction) {
                PlayerSeekToPreviousAction.DEFAULT -> super.seekToPrevious()
                PlayerSeekToPreviousAction.ALWAYS_PREVIOUS -> seekToPreviousMediaItem()
                PlayerSeekToPreviousAction.ALWAYS_RESTART -> seekTo(0L)
            }
        }

        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands()
                .buildUpon()
                .add(COMMAND_SEEK_TO_PREVIOUS)
                .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
        }
    }
}
