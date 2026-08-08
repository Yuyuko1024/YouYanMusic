package com.youyuan.music.compose.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.data.model.SongItem
import com.youyuan.music.compose.data.repo.LyricsRepo
import com.youyuan.music.compose.data.repo.RepoRiskException
import com.youyuan.music.compose.data.repo.SongRepo
import com.youyuan.music.compose.data.repo.SongRepo.AudioQualityResult
import com.youyuan.music.compose.pref.AudioQualityLevel
import com.youyuan.music.compose.pref.SettingsDataStore
import com.youyuan.music.compose.utils.PlayerController
import com.youyuan.music.compose.utils.PlayerPlaylistManager
import com.youyuan.music.compose.utils.SessionQualityOverride
import com.youyuan.music.compose.worker.DownloadTaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * 播放器 ViewModel。
 *
 * 职责：
 * - 维护 SongItem 缓存（Map<Long, SongItem>）供 UI 按 ID 查找
 * - 暴露 currentSongItem（从 PlayerController 索引推导）
 * - 管理播放进场（play）：调用方须提供完整的 SongItem 列表，VM 不做懒加载
 * - 代理播放控制给 PlayerController（UI 可直接调用其方法）
 * - 歌词/喜欢状态/评论数/音质切换 等辅助功能
 */
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@UnstableSaltUiApi
@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepo: SongRepo,
    private val lyricsRepo: LyricsRepo,
    private val playerController: PlayerController,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    data class LyricsShareOverlayState(
        val lyrics: SyncedLyrics,
        val initialLineStart: Int,
    )

    companion object {
        private const val TAG = "PlayerViewModel"
        private const val MAX_PERSISTED_PLAYLIST_SIZE = 500
        private const val PLAYBACK_SESSION_SAVE_INTERVAL_MS = 5_000L

        private const val SESSION_KEY_VERSION = "version"
        private const val SESSION_KEY_SONG_IDS = "songIds"
        private const val SESSION_KEY_CURRENT_INDEX = "currentIndex"
        private const val SESSION_KEY_POSITION_MS = "positionMs"
        private const val SESSION_KEY_WAS_PLAYING = "wasPlaying"
    }

    // ============================================================
    // SongItem 缓存 & 全量 ID 列表
    // ============================================================

    private val _songItemMap = ConcurrentHashMap<Long, SongItem>()

    /** 从 index → songId → SongItem 推导当前播放的歌曲 */
    val currentSongItem: StateFlow<SongItem?> = combine(
        playerController.currentMediaItemIndex,
        PlayerPlaylistManager.playlist,
    ) { index, playlist ->
        val songId = playlist.getOrNull(index)?.mediaId?.toLongOrNull() ?: return@combine null
        _songItemMap[songId]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ============================================================
    // 播放状态（直接代理 PlayerController）
    // ============================================================

    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val isBuffering: StateFlow<Boolean> = playerController.isBuffering
    val playbackSpeed: StateFlow<Float> = playerController.playbackSpeed
    val pitch: StateFlow<Float> = playerController.pitch
    val repeatMode: StateFlow<Int> = playerController.repeatMode
    val shuffleModeEnabled: StateFlow<Boolean> = playerController.shuffleModeEnabled
    val playlist: StateFlow<List<MediaItem>> = PlayerPlaylistManager.playlist
    val currentSongIndex: StateFlow<Int> = playerController.currentMediaItemIndex

    /** 当前歌曲封面 URL（从 SongItem 推导） */
    val currentAlbumArtUrl: StateFlow<String?> = currentSongItem
        .map { it?.album?.picUrl }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 当前歌曲艺术家名称（从 SongItem 推导） */
    val currentArtistNames: StateFlow<String?> = currentSongItem
        .map { it?.artists?.joinToString(", ") { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 当前选择的音质级别（默认 STANDARD） */
    private val _selectedAudioQualityLevel = MutableStateFlow(AudioQualityLevel.STANDARD.level)
    val selectedAudioQualityLevel: StateFlow<String> = _selectedAudioQualityLevel.asStateFlow()

    /** 当前歌曲各音质级别的可用性探测结果 */
    private val _availableQualities = MutableStateFlow<List<AudioQualityResult>>(emptyList())
    val availableQualities: StateFlow<List<AudioQualityResult>> = _availableQualities.asStateFlow()

    /** 音质探测是否进行中 */
    private val _qualitiesLoading = MutableStateFlow(false)
    val qualitiesLoading: StateFlow<Boolean> = _qualitiesLoading.asStateFlow()

    // ============================================================
    // 自有状态
    // ============================================================

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics: StateFlow<String?> = _lyrics.asStateFlow()

    private val _parsedLyrics = MutableStateFlow<SyncedLyrics?>(null)
    val parsedLyrics: StateFlow<SyncedLyrics?> = _parsedLyrics.asStateFlow()

    private val _isCurrentSongLiked = MutableStateFlow(false)
    val isCurrentSongLiked: StateFlow<Boolean> = _isCurrentSongLiked.asStateFlow()

    private val _lyricsShareOverlayState = MutableStateFlow<LyricsShareOverlayState?>(null)
    val lyricsShareOverlayState: StateFlow<LyricsShareOverlayState?> =
        _lyricsShareOverlayState.asStateFlow()

    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()

    // ============================================================
    // 内部状态
    // ============================================================

    private val gson = Gson()

    private var positionUpdateJob: Job? = null
    private var commentCountJob: Job? = null
    private var playbackSessionSaveJob: Job? = null
    private var playbackSessionRestoreAttempted = false
    private val commentCountCache = ConcurrentHashMap<Long, Int>()

    @Volatile
    private var riskBlockedUntilMs: Long = 0L

    private fun nowMs(): Long = System.currentTimeMillis()
    private fun isRiskBlocked(): Boolean = nowMs() < riskBlockedUntilMs

    private fun enterRiskBlocked(message: String?) {
        riskBlockedUntilMs = nowMs() + 5L * 60L * 1000L
        _error.value = message ?: "检测到您的网络环境存在风险，请稍后再试"
        _isLoading.value = false
    }

    // ============================================================
    // Init
    // ============================================================

    init {
        startPositionUpdates()

        // 切歌 → 清空音质选项 + 拉歌词
        viewModelScope.launch {
            currentSongItem
                .map { it?.id }
                .distinctUntilChanged()
                .collectLatest { songId ->
                    if (songId == null) {
                        _lyrics.value = null
                        _parsedLyrics.value = null
                        _isCurrentSongLiked.value = false
                        _availableQualities.value = emptyList()
                        return@collectLatest
                    }
                    // 切歌：重置音质到 DataStore 默认值，清除临时覆盖
                    SessionQualityOverride.clear()
                    viewModelScope.launch(Dispatchers.IO) {
                        _selectedAudioQualityLevel.value =
                            settingsDataStore.playerAudioQualityLevel.first()
                    }
                    _availableQualities.value = emptyList()
                    _qualitiesLoading.value = false
                    refreshLyrics(songId)
                    refreshLikeStatus(songId)
                }
        }

        // 等待 MediaController 连接后，尝试恢复上次播放会话
        viewModelScope.launch {
            playerController.isConnected
                .filter { it }
                .first()
            restorePlaybackSessionIfNeeded()
            startPlaybackSessionPersistence()
        }
    }

    // ============================================================
    // 公开方法：错误清除 & 歌词分享弹窗
    // ============================================================

    fun consumeError() {
        _error.value = null
    }

    fun showLyricsShareOverlay(lyrics: SyncedLyrics, initialLineStart: Int) {
        _lyricsShareOverlayState.value = LyricsShareOverlayState(lyrics, initialLineStart)
    }

    fun dismissLyricsShareOverlay() {
        _lyricsShareOverlayState.value = null
    }

    // ============================================================
    // 公开方法：播放控制（直接代理 PlayerController）
    // ============================================================

    fun togglePlayPause() = playerController.togglePlayPause()
    fun skipToNext() = playerController.skipToNext()
    fun skipToPrevious() = playerController.skipToPrevious()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun getCurrentPosition(): Long = playerController.getCurrentPosition()
    fun getDuration(): Long = playerController.getDuration()
    fun playAtIndex(index: Int) = playerController.playAtIndex(index)

    fun setPlayerPitch(pitch: Float) = playerController.setPitch(pitch)
    fun setPlayerSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)

    fun toggleLoopMode() {
        val shuffle = shuffleModeEnabled.value
        val repeat = repeatMode.value
        when {
            shuffle -> {
                playerController.setShuffleModeEnabled(false)
                playerController.setRepeatMode(Player.REPEAT_MODE_ALL)
            }
            repeat == Player.REPEAT_MODE_ALL -> {
                playerController.setShuffleModeEnabled(false)
                playerController.setRepeatMode(Player.REPEAT_MODE_ONE)
            }
            repeat == Player.REPEAT_MODE_ONE -> {
                playerController.setRepeatMode(Player.REPEAT_MODE_ALL)
                playerController.setShuffleModeEnabled(true)
            }
            else -> {
                playerController.setShuffleModeEnabled(false)
                playerController.setRepeatMode(Player.REPEAT_MODE_ALL)
            }
        }
    }

    // ============================================================
    // 公开方法：播放进场
    // ============================================================

    private var currentPlaylistId: Long = 0L

    /**
     * 从外部页面点击一首歌进入播放。
     *
     * @param songId 要播放的目标歌曲 ID
     * @param songList 调用方已有的 SongItem 完整列表
     * @param playlistId 歌单 ID（非歌单场景传 0 或不传）。若与上次相同则只 seek 不重建队列
     */
    fun play(
        songId: Long,
        songList: List<SongItem>,
        playlistId: Long = 0L,
    ) {
        if (isRiskBlocked()) {
            _error.value = "检测到您的网络环境存在风险，请稍后再试"
            return
        }
        if (songList.isEmpty()) {
            _error.value = "播放列表为空"
            return
        }

        // 同一歌单内切歌：只 seek，不重建队列
        if (playlistId != 0L && playlistId == currentPlaylistId) {
            val targetIndex = songList.indexOfFirst { it.id == songId }
            if (targetIndex < 0) {
                _error.value = "未找到目标歌曲"
                return
            }
            playerController.playAtIndex(targetIndex)
            return
        }

        val targetIndex = songList.indexOfFirst { it.id == songId }
        if (targetIndex < 0) {
            _error.value = "未找到目标歌曲"
            return
        }

        _isLoading.value = true
        _error.value = null

        // 写入 SongItem 到缓存
        for (item in songList) {
            _songItemMap[item.id] = item
        }

        // 构建 MediaItem 列表
        val mediaItems = songList.mapNotNull { item -> _songItemMap[item.id]?.toMediaItem() }
        if (mediaItems.isEmpty()) {
            _error.value = "无法加载歌曲数据"
            _isLoading.value = false
            return
        }

        val playIndex = mediaItems.indexOfFirst {
            it.mediaId.toLongOrNull() == songId
        }.coerceAtLeast(0)

        PlayerPlaylistManager.setPlaylist(mediaItems)
        PlayerPlaylistManager.setCurrentIndex(playIndex)
        playerController.setMediaItems(mediaItems, playIndex, startPlay = true)
        currentPlaylistId = playlistId
        SessionQualityOverride.clear()
        _isLoading.value = false
    }

    // 添加歌曲到下一首的默认播放重载
    fun addSong(song: SongItem) {
        addSong(song, true)
    }

    // 添加歌曲到下一首的 ID 重载
    fun addSong(songId: Long) {
        addSong(songId, true)
    }

    fun addSong(songId: Long, play: Boolean) {
        viewModelScope.launch {
            // 先查重：已在播放列表则直接 seek
            val existingIndex = PlayerPlaylistManager.findSongIndex(songId)
            if (existingIndex != -1) {
                playerController.playAtIndex(existingIndex)
                return@launch
            }

            // 不在列表：拉取完整信息后插入
            val song = getSongItem(songId) ?: return@launch
            addSong(song, play)
        }
    }

    /**
     * 添加一首歌到当前播放队列的下一首
     * @param song 要添加的歌曲对象
     * @param play 是否添加完就播放
     */
    fun addSong(song: SongItem, play: Boolean) {
        if (isRiskBlocked()) {
            _error.value = "检测到您的网络环境存在风险，请稍后再试"
            return
        }

        // 缓存 SongItem（无论是否已存在都要更新）
        _songItemMap[song.id] = song

        // 已存在于播放列表,直接 seek 过去并播放
        val existingIndex = PlayerPlaylistManager.findSongIndex(song.id)
        if (existingIndex != -1) {
            playerController.playAtIndex(existingIndex)
            return
        }

        val currentIndex = PlayerPlaylistManager.currentIndex.value
        val mediaItem = song.toMediaItem()
        val insertIndex = currentIndex + 1

        PlayerPlaylistManager.addItemAt(insertIndex, mediaItem)
        playerController.addMediaItems(insertIndex, listOf(mediaItem))

        if (play) {
            playerController.playAtIndex(insertIndex)
        }
    }

    fun clearQueue() {
        playbackSessionSaveJob?.cancel()
        _songItemMap.clear()
        SessionQualityOverride.clear()
        PlayerPlaylistManager.clearPlaylist()
        playerController.clearPlaylist()
        _lyrics.value = null
        _parsedLyrics.value = null
        _isCurrentSongLiked.value = false
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataStore.clearPlayerSession()
        }
    }

    fun removeFromQueue(songId: Long) {
        val index = PlayerPlaylistManager.findSongIndex(songId)
        if (index == -1) return

        val playlist = PlayerPlaylistManager.playlist.value
        if (playlist.size == 1) {
            clearQueue()
            return
        }

        PlayerPlaylistManager.removeItemAt(index)
        playerController.removeAtIndex(index)
        _songItemMap.remove(songId)
    }

    // ============================================================
    // 播放会话持久化（冷启动恢复）
    // ============================================================

    private fun startPlaybackSessionPersistence() {
        playbackSessionSaveJob?.cancel()
        playbackSessionSaveJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                persistPlaybackSession()
                delay(PLAYBACK_SESSION_SAVE_INTERVAL_MS)
            }
        }
    }

    private suspend fun persistPlaybackSession() {
        val playlist = PlayerPlaylistManager.playlist.value
        if (playlist.isEmpty()) return

        val allIds = playlist.mapNotNull { it.mediaId.toLongOrNull() }
        if (allIds.isEmpty()) return

        val currentIndex = currentSongIndex.value.coerceIn(0, (allIds.size - 1).coerceAtLeast(0))
        val (persistedIds, adjustedIndex) = cropPersistedWindow(allIds, currentIndex, MAX_PERSISTED_PLAYLIST_SIZE)

        val json = JsonObject().apply {
            addProperty(SESSION_KEY_VERSION, 1)
            val arr = JsonArray()
            persistedIds.forEach { arr.add(it) }
            add(SESSION_KEY_SONG_IDS, arr)
            addProperty(SESSION_KEY_CURRENT_INDEX, adjustedIndex)
            addProperty(SESSION_KEY_POSITION_MS, currentPosition.value.coerceAtLeast(0L))
            addProperty(SESSION_KEY_WAS_PLAYING, isPlaying.value)
        }
        settingsDataStore.setPlayerSessionJson(gson.toJson(json))
    }

    private fun cropPersistedWindow(
        songIds: List<Long>,
        currentIndex: Int,
        maxSize: Int,
    ): Pair<List<Long>, Int> {
        if (songIds.size <= maxSize) {
            return songIds to currentIndex.coerceIn(0, (songIds.size - 1).coerceAtLeast(0))
        }
        val safeIndex = currentIndex.coerceIn(0, songIds.size - 1)
        val half = maxSize / 2
        var start = (safeIndex - half).coerceAtLeast(0)
        var end = (start + maxSize).coerceAtMost(songIds.size)
        start = (end - maxSize).coerceAtLeast(0)
        val window = songIds.subList(start, end)
        val adjusted = (safeIndex - start).coerceIn(0, (window.size - 1).coerceAtLeast(0))
        return window to adjusted
    }

    private suspend fun restorePlaybackSessionIfNeeded() {
        if (playbackSessionRestoreAttempted) return
        playbackSessionRestoreAttempted = true

        // 已经在播放中就跳过
        if (PlayerPlaylistManager.playlist.value.isNotEmpty()) return

        val raw = settingsDataStore.playerSessionJson.first().trim()
        if (raw.isBlank()) return

        val json = runCatching { gson.fromJson(raw, JsonObject::class.java) }.getOrNull() ?: return
        val songIds = json.getAsJsonArray(SESSION_KEY_SONG_IDS)
            ?.mapNotNull { runCatching { it.asLong }.getOrNull() }
            ?.filter { it > 0L }
            .orEmpty()
        if (songIds.isEmpty()) {
            settingsDataStore.clearPlayerSession()
            return
        }

        val cappedIds = songIds.take(MAX_PERSISTED_PLAYLIST_SIZE)
        val savedIndex = json.get(SESSION_KEY_CURRENT_INDEX)?.asInt ?: 0
        val savedPos = json.get(SESSION_KEY_POSITION_MS)?.asLong ?: 0L
        val restoreIndex = savedIndex.coerceIn(0, (cappedIds.size - 1).coerceAtLeast(0))

        try {
            val items = songRepo.getSongItems(cappedIds)
            if (items.isEmpty()) return

            for (item in items) _songItemMap[item.id] = item

            val mediaItems = cappedIds.mapNotNull { id -> _songItemMap[id]?.toMediaItem() }
            if (mediaItems.isEmpty()) return

            PlayerPlaylistManager.setPlaylist(mediaItems)
            PlayerPlaylistManager.setCurrentIndex(restoreIndex)
            playerController.setMediaItems(mediaItems, restoreIndex, startPlay = false)
            playerController.seekTo(savedPos)
        } catch (_: Exception) {
            // 恢复失败不弹错误，静默跳过
        }
    }

    // ============================================================
    // 歌词 & 喜欢状态
    // ============================================================

    private suspend fun refreshLyrics(songId: Long) {
        try {
            val lrc = lyricsRepo.fetch(songId)
            _lyrics.value = lrc
            _parsedLyrics.value = lyricsRepo.parse(lrc)
        } catch (e: RepoRiskException) {
            enterRiskBlocked(e.message)
        } catch (_: Exception) {
            _lyrics.value = null
            _parsedLyrics.value = null
        }
    }

    private suspend fun refreshLikeStatus(songId: Long) {
        if (isRiskBlocked()) return
        try {
            val liked = songRepo.checkLiked(listOf(songId))
            _isCurrentSongLiked.value = liked?.contains(songId) == true
        } catch (e: RepoRiskException) {
            enterRiskBlocked(e.message)
        } catch (_: Exception) {
            _isCurrentSongLiked.value = false
        }
    }

    fun toggleLike() {
        val songId = currentSongItem.value?.id ?: run {
            _error.value = "当前没有正在播放的歌曲"
            return
        }
        if (isRiskBlocked()) {
            _error.value = "检测到您的网络环境存在风险，请稍后再试"
            return
        }

        val targetLike = !_isCurrentSongLiked.value
        viewModelScope.launch {
            try {
                val resp = if (targetLike) songRepo.likeSong(songId) else songRepo.unlikeSong(songId)
                if (resp == null) {
                    _error.value = if (targetLike) "添加到我喜欢的音乐失败" else "从我喜欢的音乐移除失败"
                    return@launch
                }
                _isCurrentSongLiked.value = targetLike
            } catch (e: RepoRiskException) {
                enterRiskBlocked(e.message)
            } catch (e: Exception) {
                _error.value = (if (targetLike) "添加到我喜欢的音乐失败" else "从我喜欢的音乐移除失败") +
                        (e.message?.let { ": $it" } ?: "")
            }
        }
    }

    suspend fun getSongItem(songId: Long): SongItem? {
        try {
            return songRepo.getSongItem(songId)
        } catch (e: RepoRiskException) {
            enterRiskBlocked(e.message)
            return null
        } catch (e: Exception) {
            _error.value = "获取歌曲信息失败：$e"
            return null
        }
    }

    fun switchQuality(level: AudioQualityLevel) {
        val previousLevel = _selectedAudioQualityLevel.value
        if (previousLevel == level.level) return
        _selectedAudioQualityLevel.value = level.level

        // 通知 Service 临时覆盖音质
        SessionQualityOverride.set(level.level)

        // 重新准备当前歌曲，让 ResolvingDataSource 用新音质解析 URL
        val currentPos = currentPosition.value
        val playlist = PlayerPlaylistManager.playlist.value
        val index = currentSongIndex.value
        if (playlist.isNotEmpty() && index in playlist.indices) {
            playerController.setMediaItems(playlist, index, startPlay = false)
            // Media3 重新 prepare 后 seek 回之前的位置
            viewModelScope.launch {
                delay(300) // 等待 prepare 完成
                playerController.seekTo(currentPos)
            }
        }
    }

    /** 探测当前歌曲所有候选音质级别的可用性，结果写入 availableQualities */
    fun probeQualities(songId: Long) {
        if (_qualitiesLoading.value) return
        _qualitiesLoading.value = true
        viewModelScope.launch {
            try {
                val results = songRepo.checkAllQualities(songId)
                _availableQualities.value = results
            } catch (_: RepoRiskException) {
                _availableQualities.value = emptyList()
            } catch (_: Exception) {
                _availableQualities.value = emptyList()
            } finally {
                _qualitiesLoading.value = false
            }
        }
    }

    suspend fun checkSongLikedOnce(songId: Long): Boolean? {
        if (isRiskBlocked()) return null
        return try {
            val liked = songRepo.checkLiked(listOf(songId))
            liked?.contains(songId) ?: false
        } catch (_: Exception) {
            null
        }
    }

    fun setSongLiked(songId: Long, targetLike: Boolean, onResult: (Boolean) -> Unit) {
        if (isRiskBlocked()) {
            _error.value = "检测到您的网络环境存在风险，请稍后再试"
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                val resp = if (targetLike) songRepo.likeSong(songId) else songRepo.unlikeSong(songId)
                val ok = resp != null
                if (!ok) {
                    _error.value = if (targetLike) "添加到我喜欢的音乐失败" else "从我喜欢的音乐移除失败"
                }
                onResult(ok)
            } catch (e: RepoRiskException) {
                enterRiskBlocked(e.message)
                onResult(false)
            } catch (e: Exception) {
                _error.value = e.message ?: "操作失败"
                onResult(false)
            }
        }
    }

    fun saveSongToDevice(
        songId: Long,
        songTitle: String?,
        artistName: String?,
        albumName: String?,
        artworkUrl: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        download(songId, songTitle, artistName, albumName, artworkUrl, onResult)
    }

    // ============================================================
    // 评论数
    // ============================================================",

    fun refreshComments(songId: Long, force: Boolean = false) {
        if (!force) {
            val cached = commentCountCache[songId]
            if (cached != null) {
                _commentCount.value = cached
                return
            }
        }
        if (isRiskBlocked()) return

        commentCountJob?.cancel()
        commentCountJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val total = songRepo.fetchCommentCount(songId) ?: return@launch
                commentCountCache[songId] = total
                _commentCount.value = total
            } catch (e: RepoRiskException) {
                enterRiskBlocked(e.message)
            } catch (_: Exception) {
                // 评论数拉取失败不影响播放
            }
        }
    }

    fun clearComments() {
        _commentCount.value = 0
    }

    // ============================================================
    // 下载
    // ============================================================

    fun download(
        songId: Long,
        songTitle: String?,
        artistName: String?,
        albumName: String?,
        artworkUrl: String?,
        onResult: ((Boolean, String) -> Unit)? = null,
    ) {
        if (isRiskBlocked()) {
            val message = "检测到您的网络环境存在风险，请稍后再试"
            _error.value = message
            onResult?.invoke(false, message)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedLevel = settingsDataStore.downloadAudioQualityLevel.first()
                    .ifBlank { AudioQualityLevel.default().level }
                DownloadTaskManager.enqueueSongDownload(
                    context = context,
                    songId = songId,
                    songTitle = songTitle,
                    songArtist = artistName,
                    songAlbum = albumName,
                    artworkUrl = artworkUrl,
                    qualityLevel = selectedLevel,
                )
                withContext(Dispatchers.Main) { onResult?.invoke(true, "已加入下载任务") }
            } catch (e: Exception) {
                val message = e.message ?: "保存歌曲失败"
                _error.value = message
                withContext(Dispatchers.Main) { onResult?.invoke(false, message) }
            }
        }
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private fun SongItem.toMediaItem(): MediaItem {
        val resolvedUri = qualities.values
            .firstOrNull { it.url != null }
            ?.url
            ?.toUri()
            ?: "yym://song/$id".toUri()
        val isPlaceholder = resolvedUri.toString().startsWith("yym://")
        if (isPlaceholder && qualities.isNotEmpty()) {
            Log.w(TAG, "toMediaItem: song $id has ${qualities.size} qualities but ALL urls are null, using placeholder")
        } else if (qualities.isEmpty()) {
            Log.d(TAG, "toMediaItem: song $id has NO qualities, using placeholder yym://song/$id")
        }

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(resolvedUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setArtist(artists.joinToString(", ") { it.name })
                    .setAlbumTitle(album.name)
                    .setArtworkUri(album.picUrl?.toUri())
                    .build()
            )
            .build()
    }

    private fun startPositionUpdates() {
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (playerController.isPlayerAvailable()) {
                    _currentPosition.value = playerController.getCurrentPosition()
                    _duration.value = playerController.getDuration()
                }
                delay(250)
            }
        }
    }
}