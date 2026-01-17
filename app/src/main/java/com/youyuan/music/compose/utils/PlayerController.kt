package com.youyuan.music.compose.utils

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.service.MusicPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@UnstableApi
@UnstableSaltUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // 协程作用域
    val scope = CoroutineScope(Dispatchers.Main)

    // 连接状态
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // === 播放速度与音高 ===
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    // === 循环/随机模式 ===
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_ALL)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    // 当前播放的媒体项索引
    private val _currentMediaItemIndex = MutableStateFlow(0)
    val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // 当切换到新的媒体项时更新索引
            _currentMediaItemIndex.value = mediaController?.currentMediaItemIndex ?: 0
            Log.d("MusicController", "MediaItem transition: index=${_currentMediaItemIndex.value}, reason=$reason")
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            // 当播放列表发生变化（例如插入/追加媒体项）时，同步当前索引
            _currentMediaItemIndex.value = mediaController?.currentMediaItemIndex ?: 0
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
        }
    }

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            try {
                mediaController = mediaControllerFuture?.get()
                mediaController?.addListener(playerListener)
                _isConnected.value = true

                mediaController?.let { controller ->
                    _playbackSpeed.value = controller.playbackParameters.speed
                    _pitch.value = controller.playbackParameters.pitch

                    _repeatMode.value = controller.repeatMode
                    _shuffleModeEnabled.value = controller.shuffleModeEnabled
                }
                Log.d("MusicController", "MediaController connected")
            } catch (e: Exception) {
                _isConnected.value = false
                Log.e("MusicController", "Failed to connect MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        try {
            mediaController?.removeListener(playerListener)
            mediaController?.release()
        } catch (e: Exception) {
            Log.e("MusicController", "Failed to disconnect MediaController", e)
        } finally {
            mediaController = null
            mediaControllerFuture = null
            _isConnected.value = false
        }
    }

    fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPlay: Boolean) {
        mediaController?.setMediaItems(mediaItems, startIndex, 0)
        if (startPlay) {
            mediaController?.prepare()
            mediaController?.play()
        }
    }

    fun addMediaItem(mediaItem: MediaItem) {
        mediaController?.addMediaItem(mediaItem)
    }

    fun addMediaItems(mediaItems: List<MediaItem>) {
        mediaController?.addMediaItems(mediaItems)
    }

    fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
        mediaController?.addMediaItems(index, mediaItems)
    }

    fun setRepeatMode(mode: Int) {
        mediaController?.repeatMode = mode
        _repeatMode.value = mode
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
        _shuffleModeEnabled.value = enabled
    }

    /**
     * 检查播放器是否可用
     */
    fun isPlayerAvailable(): Boolean {
        return mediaController != null && _isConnected.value
    }

    /**
     * 获取当前播放位置（实时查询）
     */
    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition ?: 0L
    }

    /**
     * 获取当前媒体总时长（实时查询）
     */
    fun getDuration(): Long {
        return mediaController?.duration?.coerceAtLeast(0L) ?: 0L
    }

    fun play(mediaItem: MediaItem) {
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()
    }

    /**
     * 添加单个 MediaItem 到播放列表末尾并播放
     */
    fun addAndPlay(mediaItem: MediaItem) {
        mediaController?.let { controller ->
            controller.addMediaItem(mediaItem)
            controller.prepare()
            // 跳转到新添加的项（最后一个）并播放
            val newIndex = controller.mediaItemCount - 1
            if (newIndex >= 0) {
                controller.seekTo(newIndex, 0L)
            }
            controller.play()
        }
    }

    /**
     * 仅添加 MediaItem 到播放列表末尾，不改变当前播放
     */
    fun addToPlaylist(mediaItem: MediaItem) {
        mediaController?.addMediaItem(mediaItem)
    }

    /**
     * 获取当前播放列表的项数
     */
    fun getMediaItemCount(): Int {
        return mediaController?.mediaItemCount ?: 0
    }

    /**
     * 获取当前播放的索引
     */
    fun getCurrentMediaItemIndex(): Int {
        return mediaController?.currentMediaItemIndex ?: 0
    }

    /**
     * 跳转到指定索引播放
     */
    fun playAtIndex(index: Int) {
        mediaController?.let { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.seekTo(index, 0L)
                controller.prepare()
                controller.play()
            }
        }
    }

    /**
     * 从播放列表移除指定索引的项
     */
    fun removeAtIndex(index: Int) {
        mediaController?.let { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.removeMediaItem(index)
            }
        }
    }

    /**
     * 从播放列表移除指定索引的媒体项
     */
    fun removeMediaItemAt(index: Int) {
        mediaController?.let { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.removeMediaItem(index)
            }
        }
    }

    fun replaceMediaItemAt(index: Int, mediaItem: MediaItem) {
        mediaController?.let { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.replaceMediaItem(index, mediaItem)
            }
        }
    }

    /**
     * 清空播放列表
     */
    fun clearPlaylist() {
        mediaController?.clearMediaItems()
    }

    /**
     * 设置播放器播放速度
     * @param speed 播放速度，范围通常为 0.5 到 2.0
     */
    fun setPlaybackSpeed(speed: Float) {
        mediaController?.let { controller ->
            val params = controller.playbackParameters
            controller.playbackParameters = params.withSpeed(speed)
            _playbackSpeed.value = speed
        } ?: run {
            Logger.warn("PlayerController", "媒体控制器未连接，无法设置播放速度")
        }
    }

    /**
     * 设置播放器音高
     * @param pitch 音高值，范围通常为 0.5 到 2.0
     */
    fun setPitch(pitch: Float) {
        mediaController?.let { controller ->
            val params = controller.playbackParameters
            controller.playbackParameters = params.withPitch(pitch)
            _pitch.value = pitch
        } ?: run {
            Logger.warn("PlayerController", "媒体控制器未连接，无法设置音高")
        }
    }

    fun playPlaylist(mediaItems: List<MediaItem>, startIndex: Int = 0) {
        mediaController?.setMediaItems(mediaItems, startIndex, 0L)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun release() {
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
    }

    fun getPlayer(): Player? = mediaController

}