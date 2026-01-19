package com.youyuan.music.compose.service

import android.app.PendingIntent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.youyuan.music.compose.BuildConfig
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.SongUrlApi
import com.youyuan.music.compose.pref.PlayerSeekToPreviousAction
import com.youyuan.music.compose.pref.SettingsDataStore
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.youyuan.music.compose.utils.MediaCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import androidx.core.net.toUri

@UnstableApi
@AndroidEntryPoint
class MusicPlaybackService : MediaLibraryService() {

    companion object {
        const val CUSTOM_COMMAND_CLOSE = "${BuildConfig.APPLICATION_ID}.COMMAND_CLOSE"
        const val CUSTOM_COMMAND_FAVORITE = "${BuildConfig.APPLICATION_ID}.COMMAND_FAVORITE"

        // 自定义action
        const val ACTION_EXIT_APP = "${BuildConfig.APPLICATION_ID}.ACTION_EXIT_APP"
    }

    // 设置数据存储
    private lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var apiClient: ApiClient

    private val songUrlApi: SongUrlApi by lazy { apiClient.createService(SongUrlApi::class.java) }

    private val playUrlCache = ConcurrentHashMap<Long, String>()

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var player: ExoPlayer? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val closeCommandButton: CommandButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("Close")
        .setCustomIconResId(R.drawable.ic_close_24px)
        .setSessionCommand(SessionCommand(CUSTOM_COMMAND_CLOSE, Bundle.EMPTY))
        .build()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        settingsDataStore = SettingsDataStore(this)

        initializePlayer()
        initializeSession()
    }

    private fun initializePlayer() {
        val httpUpstreamFactory = DefaultHttpDataSource.Factory()

        // Media3 Cache（LRU 1GB）：提高命中率，减少重复请求
        val cache = MediaCache.get(this)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpUpstreamFactory)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // 解析占位 URI：yym://song/{id} -> 真实播放 URL
        val resolvingFactory = ResolvingDataSource.Factory(cacheDataSourceFactory) { dataSpec: DataSpec ->
            val uri = dataSpec.uri
            if (uri.scheme != "yym" || uri.host != "song") return@Factory dataSpec

            val songId = uri.pathSegments.firstOrNull()?.toLongOrNull()
                ?: throw IOException("Invalid song placeholder uri: $uri")

            val playUrl = playUrlCache[songId] ?: runBlocking {
                val response = songUrlApi.getSongUrl(songIds = songId.toString())
                response.data?.firstOrNull()?.url
            }?.also { resolved ->
                if (resolved.isNotBlank()) playUrlCache[songId] = resolved
            }

            if (playUrl.isNullOrBlank()) {
                throw IOException("Empty play url for songId=$songId")
            }

            dataSpec.buildUpon()
                .setUri(playUrl.toUri())
                .build()
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(resolvingFactory)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setMediaSourceFactory(mediaSourceFactory)
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
                .setCustomLayout(listOf(closeCommandButton))
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

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_CLOSE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_COMMAND_CLOSE) {
                // 通知 Activity 退出
                val exitIntent = android.content.Intent(ACTION_EXIT_APP)
                LocalBroadcastManager.getInstance(this@MusicPlaybackService)
                    .sendBroadcast(exitIntent)

                // 停止播放并停止服务
                player?.stop()
                player?.clearMediaItems()
                serviceScope.launch {
                    delay(300)
                    stopSelf()
                }

                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
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
