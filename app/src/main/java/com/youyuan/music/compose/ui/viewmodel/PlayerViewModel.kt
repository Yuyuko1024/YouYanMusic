package com.youyuan.music.compose.ui.viewmodel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.AlbumApi
import com.youyuan.music.compose.api.apis.CommentApi
import com.youyuan.music.compose.api.apis.LyricsApi
import com.youyuan.music.compose.api.apis.SongLikeApi
import com.youyuan.music.compose.api.apis.SongApi
import com.youyuan.music.compose.api.apis.SongUrlApi
import com.youyuan.music.compose.api.model.Artist
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.api.model.Song
import com.youyuan.music.compose.data.PlaylistInvalidationBus
import com.youyuan.music.compose.data.SongDetailPool
import com.youyuan.music.compose.utils.Logger
import com.youyuan.music.compose.utils.PlayerController
import com.youyuan.music.compose.utils.PlayerPlaylistManager
import com.youyuan.music.compose.utils.toSong
import com.youyuan.music.compose.pref.AudioQualityLevel
import com.youyuan.music.compose.pref.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@UnstableApi
@UnstableSaltUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val playerController: PlayerController,
    private val songDetailPool: SongDetailPool,
    private val playlistInvalidationBus: PlaylistInvalidationBus,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {
    companion object {
        const val TAG = "PlayerViewModel"
    }

    private val songUrlApi: SongUrlApi = apiClient.createService(SongUrlApi::class.java)
    private val albumApi: AlbumApi = apiClient.createService(AlbumApi::class.java)
    private val songApi: SongApi = apiClient.createService(SongApi::class.java)
    private val lyricsApi: LyricsApi = apiClient.createService(LyricsApi::class.java)
    private val commentApi: CommentApi = apiClient.createService(CommentApi::class.java)
    private val songLikeApi: SongLikeApi = apiClient.createService(SongLikeApi::class.java)

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _currentSongUrl = MutableStateFlow<String?>(null)
    val currentSongUrl: StateFlow<String?> = _currentSongUrl.asStateFlow()

    private val _currentAlbumArtUrl = MutableStateFlow<String?>(null)
    val currentAlbumArtUrl: StateFlow<String?> = _currentAlbumArtUrl.asStateFlow()

    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()

    // === 音质选择（/song/url/v1 level） ===
    val selectedAudioQualityLevel: StateFlow<String> = settingsDataStore.playerAudioQualityLevel
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AudioQualityLevel.default().level)

    data class AudioQualityAvailability(
        val requested: AudioQualityLevel,
        val available: Boolean,
        val actualLevel: String? = null,
        val br: Long? = null,
        val encodeType: String? = null,
        val message: String? = null,
    )

    private val _audioQualityLoading = MutableStateFlow(false)
    val audioQualityLoading: StateFlow<Boolean> = _audioQualityLoading.asStateFlow()

    private val _audioQualityError = MutableStateFlow<String?>(null)
    val audioQualityError: StateFlow<String?> = _audioQualityError.asStateFlow()

    private val _availableAudioQualities = MutableStateFlow<List<AudioQualityAvailability>>(emptyList())
    val availableAudioQualities: StateFlow<List<AudioQualityAvailability>> = _availableAudioQualities.asStateFlow()

    private val _currentArtists = MutableStateFlow<List<Artist>>(emptyList())
    val currentArtists: StateFlow<List<Artist>> = _currentArtists.asStateFlow()

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics: StateFlow<String?> = _lyrics.asStateFlow()

    val currentArtistNames: StateFlow<String> = _currentArtists.map { artists ->
        artists.joinToString(", ") { it.name ?: "Unknown" }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // === 播放进度相关 ===
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isCurrentSongLiked = MutableStateFlow(false)
    val isCurrentSongLiked: StateFlow<Boolean> = _isCurrentSongLiked.asStateFlow()

    fun consumeError() {
        _error.value = null
    }

    val playlist: StateFlow<List<PlayerPlaylistManager.PlaylistItem>> = PlayerPlaylistManager.playlist

    val currentMediaItem: StateFlow<MediaItem?> = combine(
        playerController.currentMediaItemIndex,
        PlayerPlaylistManager.playlist
    ) { index, list ->
        list.getOrNull(index)?.let { item ->
            PlayerPlaylistManager.buildMediaItem(item.song, item.playUrl, item.albumArtUrl)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val isBuffering: StateFlow<Boolean> = playerController.isBuffering

    // 播放器音高和速度
    val playbackSpeed = playerController.playbackSpeed
    val pitch = playerController.pitch

    // 循环/随机模式
    val repeatMode: StateFlow<Int> = playerController.repeatMode
    val shuffleModeEnabled: StateFlow<Boolean> = playerController.shuffleModeEnabled

    // 维护当前播放列表的完整 ID 顺序
    private var _fullPlaylistIds: List<Long> = emptyList()
    private var _fullPlaylistHash: Long = 0L
    private var _fullPlaylistFirst: Long? = null
    private var _fullPlaylistLast: Long? = null
    private var _fullPlaylistIndexMap: Map<Long, Int> = emptyMap()

    // 维护已经加载进播放器的 ID 集合，用于快速查找
    private val _loadedSongIds = Collections.synchronizedSet(mutableSetOf<Long>())

    // 已经获取到（但可能还没插入播放器）的 item 缓存：用于“加载中点击其它歌”避免重复请求
    private val _preparedItemCache = ConcurrentHashMap<Long, PlayerPlaylistManager.PlaylistItem>()

    // 单写入者：所有对 PlayerPlaylistManager + MediaController 队列的结构性变更必须串行化
    private val playlistMutationMutex = Mutex()

    // 构建会话：用于丢弃旧任务的晚到提交
    private val buildSessionId = AtomicLong(0L)
    private fun newBuildSessionId(): Long = buildSessionId.incrementAndGet()
    private fun isCurrentBuildSession(sessionId: Long): Boolean = buildSessionId.get() == sessionId

    private var positionUpdateJob: Job? = null
    private var buildLikedPlaylistJob: Job? = null
    private var commentCountJob: Job? = null

    private val commentCountCache = ConcurrentHashMap<Long, Int>()

    // 触发风控(-462)后，短时间暂停所有后台补齐/请求，避免继续把风控刷得更严重
    @Volatile
    private var riskBlockedUntilMs: Long = 0L

    private class RiskControlException(message: String) : Exception(message)

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun isRiskBlocked(): Boolean = nowMs() < riskBlockedUntilMs

    private fun enterRiskBlocked(message: String?) {
        // 保守退避 5 分钟（可按需要调整）
        riskBlockedUntilMs = nowMs() + 5L * 60L * 1000L
        _error.value = message ?: "检测到您的网络环境存在风险，请稍后再试"
        _isLoading.value = false
        // 不影响正在播放，只停止后台补齐任务
        buildLikedPlaylistJob?.cancel()
    }

    private fun isRiskControlThrowable(t: Throwable): Boolean {
        val msg = t.message ?: return false
        return msg.contains("RISK_CONTROL_-462") || msg.contains("网络环境") || msg.contains("存在风险")
    }

    private fun throwIfRisk(t: Throwable) {
        if (!isRiskControlThrowable(t)) return
        val msg = t.message?.substringAfter("RISK_CONTROL_-462:")?.takeIf { it.isNotBlank() }
            ?: "检测到您的网络环境存在风险，请稍后再试"
        throw RiskControlException(msg)
    }

    private fun throwIfRiskCode(code: Int?, message: String? = null) {
        if (code == -462) throw RiskControlException(message ?: "检测到您的网络环境存在风险，请稍后再试")
    }

    private fun throwIfRiskCode(code: Long?, message: String? = null) {
        if (code == -462L) throw RiskControlException(message ?: "检测到您的网络环境存在风险，请稍后再试")
    }

    private fun idsQueryJson(ids: List<Long>): String {
        // 按文档：ids=[2058263032,1497529942]
        return ids.joinToString(prefix = "[", postfix = "]")
    }

    fun refreshCurrentSongLiked(songId: Long?) {
        if (songId == null) {
            _isCurrentSongLiked.value = false
            return
        }
        if (isRiskBlocked()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = songLikeApi.checkSongLike(ids = idsQueryJson(listOf(songId)))
                val code = resp.code
                if (code != null && code != 200) {
                    // 未登录/失败时，不弹错；仅回退为未喜欢
                    Logger.debug(TAG, "checkSongLike failed: code=$code")
                    _isCurrentSongLiked.value = false
                    return@launch
                }
                _isCurrentSongLiked.value = resp.likedIds().contains(songId)
            } catch (e: Exception) {
                if (e is RiskControlException) {
                    enterRiskBlocked(e.message)
                    return@launch
                }
                throwIfRisk(e)
                Logger.debug(TAG, "checkSongLike error: ${e.message}")
                _isCurrentSongLiked.value = false
            }
        }
    }

    fun toggleCurrentSongFavorite() {
        val songId = _currentSong.value?.id
        if (songId == null) {
            _error.value = "当前没有正在播放的歌曲"
            return
        }
        if (isRiskBlocked()) {
            _error.value = "检测到您的网络环境存在风险，请稍后再试"
            return
        }

        val targetLike = !_isCurrentSongLiked.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = if (targetLike) {
                    songLikeApi.likeSong(id = songId)
                } else {
                    songLikeApi.unlikeSong(id = songId)
                }
                val code = resp.code
                if (code != null && code != 200) {
                    _error.value = if (targetLike) "添加到我喜欢的音乐失败" else "从我喜欢的音乐移除失败"
                    return@launch
                }
                _isCurrentSongLiked.value = targetLike

                // /like 会返回 playlistId（对应“我喜欢的音乐”歌单）。通知相关页面强制刷新歌单详情。
                resp.playlistId?.let { playlistInvalidationBus.invalidate(it) }
            } catch (e: Exception) {
                if (e is RiskControlException) {
                    enterRiskBlocked(e.message)
                    return@launch
                }
                throwIfRisk(e)
                _error.value = (if (targetLike) "添加到我喜欢的音乐失败" else "从我喜欢的音乐移除失败") +
                    (e.message?.let { ": $it" } ?: "")
            }
        }
    }

    /**
     * 单次检查某首歌是否在“我喜欢的音乐”中。
     * 返回 null 表示未登录/请求失败（不作为错误上抛）。
     */
    suspend fun checkSongLikedOnce(songId: Long): Boolean? {
        if (isRiskBlocked()) return null
        return try {
            val resp = songLikeApi.checkSongLike(ids = idsQueryJson(listOf(songId)))
            val code = resp.code
            if (code != null && code != 200) {
                Logger.debug(TAG, "checkSongLikedOnce failed: code=$code")
                null
            } else {
                resp.likedIds().contains(songId)
            }
        } catch (e: Exception) {
            if (e is RiskControlException) {
                enterRiskBlocked(e.message)
                return null
            }
            throwIfRisk(e)
            Logger.debug(TAG, "checkSongLikedOnce error: ${e.message}")
            null
        }
    }

    /**
     * 设置某首歌喜欢状态（加入/移除“我喜欢的音乐”）。
     * onResult(true) 表示成功；失败会通过 error flow 给出提示。
     */
    fun setSongLiked(
        songId: Long,
        targetLike: Boolean,
        onResult: ((Boolean) -> Unit)? = null,
    ) {
        if (isRiskBlocked()) {
            _error.value = "检测到您的网络环境存在风险，请稍后再试"
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = if (targetLike) {
                    songLikeApi.likeSong(id = songId)
                } else {
                    songLikeApi.unlikeSong(id = songId)
                }
                val code = resp.code
                if (code != null && code != 200) {
                    _error.value = if (targetLike) "添加到我喜欢的音乐失败" else "从我喜欢的音乐移除失败"
                    withContext(Dispatchers.Main) { onResult?.invoke(false) }
                    return@launch
                }

                // 如果操作的是当前播放歌曲，同步更新 UI
                if (_currentSong.value?.id == songId) {
                    _isCurrentSongLiked.value = targetLike
                }

                resp.playlistId?.let { playlistInvalidationBus.invalidate(it) }
                withContext(Dispatchers.Main) { onResult?.invoke(true) }
            } catch (e: Exception) {
                if (e is RiskControlException) {
                    enterRiskBlocked(e.message)
                    withContext(Dispatchers.Main) { onResult?.invoke(false) }
                    return@launch
                }
                throwIfRisk(e)
                _error.value = (if (targetLike) "添加到我喜欢的音乐失败" else "从我喜欢的音乐移除失败") +
                    (e.message?.let { ": $it" } ?: "")
                withContext(Dispatchers.Main) { onResult?.invoke(false) }
            }
        }
    }

    fun clearCommentCount() {
        _commentCount.value = 0
    }

    /**
     * 获取歌曲评论数量（取 /comment/music 的 total 字段）。
     * 注意：此方法本身不限制调用时机；由 UI 在“播放器展开且切歌”时触发。
     */
    fun refreshCommentCount(songId: Long, force: Boolean = false) {
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
                val response = commentApi.getMusicComments(songId = songId, limit = 1, offset = 0)
                throwIfRiskCode(response.code)
                if (response.code != 200) return@launch

                val total = (response.total ?: 0).coerceAtLeast(0)
                commentCountCache[songId] = total
                _commentCount.value = total
            } catch (e: Exception) {
                if (e is RiskControlException) {
                    enterRiskBlocked(e.message)
                    return@launch
                }
                throwIfRisk(e)
                Logger.err(TAG, "获取评论数量失败: ${e.message}")
            }
        }
    }
    
    init {
        viewModelScope.launch {
            combine(
                playerController.currentMediaItemIndex,
                PlayerPlaylistManager.playlist
            ) { index, list ->
                index to list.getOrNull(index)
            }.collect { (index, item) ->
                _currentSongIndex.value = index
                if (item != null) {
                    _currentSong.value = item.song
                    _currentArtists.value = item.song.artists ?: emptyList()
                    _currentSongUrl.value = item.playUrl
                    _currentAlbumArtUrl.value = item.albumArtUrl
                } else {
                    // 播放列表变化但当前索引无有效项时，清空显示
                    _currentSong.value = null
                    _currentArtists.value = emptyList()
                    _currentSongUrl.value = null
                    _currentAlbumArtUrl.value = null
                }
            }
        }

        // 当前歌曲变化时，刷新“是否已喜欢”的状态
        viewModelScope.launch {
            currentSong
                .map { it?.id }
                .distinctUntilChanged()
                .collectLatest { songId ->
                    refreshCurrentSongLiked(songId)
                }
        }

        // 当前播放歌曲变化 -> 自动拉取歌词（lrc.lyric）
        viewModelScope.launch {
            currentSong
                .map { it?.id }
                .distinctUntilChanged()
                .collectLatest { songId ->
                    if (songId == null) {
                        _lyrics.value = null
                        return@collectLatest
                    }

                    val lrc = fetchLyrics(songId)
                    _lyrics.value = lrc
                }
        }

        // 开始位置更新
        startPositionUpdates()
    }

    private suspend fun fetchLyrics(songId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val response = lyricsApi.getLyricById(songId)
            throwIfRiskCode(response.code)
            if (response.code != 200) return@withContext null

            val main = normalizeLrcText(response.lrc?.lyric)
            val translated = normalizeLrcText(response.tlyric?.lyric)

            if (translated.isNullOrBlank()) return@withContext main
            if (main.isNullOrBlank()) return@withContext translated

            mergeLyricsByTimestamp(main, translated)
        } catch (e: Exception) {
            if (e is RiskControlException) {
                enterRiskBlocked(e.message)
                return@withContext null
            }
            throwIfRisk(e)
            Logger.err(TAG, "获取歌词失败: ${e.message}")
            null
        }
    }

    private fun normalizeLrcText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        // 兼容服务端返回 "\\n" 或正常换行 "\n" 两种情况；并统一 Windows 换行
        return raw
            .replace("\r\n", "\n")
            .replace("\\n", "\n")
    }

    private fun mergeLyricsByTimestamp(main: String, translated: String): String {
        val translatedMap = buildTimestampToTextMap(translated)
        if (translatedMap.isEmpty()) return main

        val out = ArrayList<String>()
        for (line in main.split('\n')) {
            out.add(line)
            val tags = extractTimeTags(line)
            if (tags.isEmpty()) continue

            val key = tags.first()
            val t = translatedMap[key]?.trim().orEmpty()
            if (t.isBlank()) continue

            // 复用同一个时间戳，让 AutoParser 按同一时间点显示两行
            out.add(tags.joinToString(separator = "") { "[$it]" } + t)
        }
        return out.joinToString("\n")
    }

    private fun buildTimestampToTextMap(lrc: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (line in lrc.split('\n')) {
            val tags = extractTimeTags(line)
            if (tags.isEmpty()) continue

            val text = stripTimeTags(line).trim()
            if (text.isBlank()) continue

            for (tag in tags) {
                // 同一时间戳出现多次时，保留第一条非空翻译
                map.putIfAbsent(tag, text)
            }
        }
        return map
    }

    private fun extractTimeTags(line: String): List<String> {
        // 支持 [mm:ss.xx] / [m:ss.xxx] 等
        val regex = Regex("\\[(\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?)\\]")
        return regex.findAll(line)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .map { it.lowercase(Locale.US) }
            .toList()
    }

    private fun stripTimeTags(line: String): String {
        val regex = Regex("\\[\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?\\]")
        return line.replace(regex, "")
    }
    
    fun getCurrentPosition(): Long = playerController.getCurrentPosition()
    
    fun getDuration(): Long = playerController.getDuration()
    
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun togglePlayPause() = playerController.togglePlayPause()
    
    fun skipToNext() = playerController.skipToNext()
    
    fun skipToPrevious() = playerController.skipToPrevious()
    
    fun getPlayer() = playerController.getPlayer()

    /**
     * 循环切换：列表全部循环 -> 单曲循环 -> 随机模式 -> 列表全部循环
     * 默认：列表全部循环（REPEAT_MODE_ALL + shuffle=false）
     */
    fun toggleLoopMode() {
        val shuffle = shuffleModeEnabled.value
        val repeat = repeatMode.value

        when {
            // 随机 -> 列表循环
            shuffle -> {
                playerController.setShuffleModeEnabled(false)
                playerController.setRepeatMode(Player.REPEAT_MODE_ALL)
            }
            // 列表循环 -> 单曲循环
            repeat == Player.REPEAT_MODE_ALL -> {
                playerController.setShuffleModeEnabled(false)
                playerController.setRepeatMode(Player.REPEAT_MODE_ONE)
            }
            // 单曲循环 -> 随机
            repeat == Player.REPEAT_MODE_ONE -> {
                playerController.setRepeatMode(Player.REPEAT_MODE_ALL)
                playerController.setShuffleModeEnabled(true)
            }
            // 兜底
            else -> {
                playerController.setShuffleModeEnabled(false)
                playerController.setRepeatMode(Player.REPEAT_MODE_ALL)
            }
        }
    }

    private fun startPositionUpdates() {
        // Media3 MediaController 要求在它的 application thread（通常是主线程）调用。
        // 这里做降频，避免高频轮询导致主线程压力过大。
        positionUpdateJob = viewModelScope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                if (playerController.isPlayerAvailable()) {
                    _currentPosition.value = playerController.getCurrentPosition()
                    _duration.value = playerController.getDuration()
                }
                delay(250)
            }
        }
    }

    private suspend fun onPlayerThread(block: () -> Unit) {
        withContext(Dispatchers.Main.immediate) { block() }
    }

    private suspend fun commitSetPlaylist(
        sessionId: Long,
        items: List<PlayerPlaylistManager.PlaylistItem>,
        startIndex: Int,
        startPlay: Boolean = true,
    ) {
        if (!isCurrentBuildSession(sessionId)) return

        playlistMutationMutex.withLock {
            if (!isCurrentBuildSession(sessionId)) return
            onPlayerThread {
                _loadedSongIds.clear()
                _preparedItemCache.clear()
                items.forEach { it.song.id?.let { id ->
                    _loadedSongIds.add(id)
                    _preparedItemCache[id] = it
                } }

                PlayerPlaylistManager.setPlaylist(items)
                PlayerPlaylistManager.setCurrentIndex(startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
                playerController.setMediaItems(
                    mediaItems = items.map { it.toMediaItemSafe() },
                    startIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                    startPlay = startPlay,
                )
            }
        }
    }

    private suspend fun commitAddItemsAppend(
        sessionId: Long,
        newItems: List<PlayerPlaylistManager.PlaylistItem>,
    ) {
        if (newItems.isEmpty()) return
        if (!isCurrentBuildSession(sessionId)) return

        playlistMutationMutex.withLock {
            if (!isCurrentBuildSession(sessionId)) return
            onPlayerThread {
                val commitItems = newItems.filter { it.song.id != null && _loadedSongIds.add(it.song.id!!) }
                if (commitItems.isEmpty()) return@onPlayerThread
                commitItems.forEach { it.song.id?.let { id -> _preparedItemCache[id] = it } }

                PlayerPlaylistManager.addItems(commitItems)
                playerController.addMediaItems(commitItems.map { it.toMediaItemSafe() })
            }
        }
    }

    private suspend fun commitAddItemsInsert(
        sessionId: Long,
        index: Int,
        newItems: List<PlayerPlaylistManager.PlaylistItem>,
    ) {
        if (newItems.isEmpty()) return
        if (!isCurrentBuildSession(sessionId)) return

        playlistMutationMutex.withLock {
            if (!isCurrentBuildSession(sessionId)) return
            onPlayerThread {
                val commitItems = newItems.filter { it.song.id != null && _loadedSongIds.add(it.song.id!!) }
                if (commitItems.isEmpty()) return@onPlayerThread
                commitItems.forEach { it.song.id?.let { id -> _preparedItemCache[id] = it } }

                PlayerPlaylistManager.addItemsAt(index, commitItems)
                playerController.addMediaItems(index, commitItems.map { it.toMediaItemSafe() })
            }
        }
    }

    private suspend fun commitInsertSingleAndPlayById(
        sessionId: Long,
        desiredIndex: Int,
        item: PlayerPlaylistManager.PlaylistItem,
    ) {
        val songId = item.song.id ?: return
        if (!isCurrentBuildSession(sessionId)) return

        playlistMutationMutex.withLock {
            if (!isCurrentBuildSession(sessionId)) return
            onPlayerThread {
                // 若已经存在，直接定位播放（避免并发下重复插入导致索引错乱）
                val existingIndex = PlayerPlaylistManager.findSongIndex(songId)
                if (existingIndex != -1) {
                    playerController.getPlayer()?.seekTo(existingIndex, 0L)
                    return@onPlayerThread
                }

                if (!_loadedSongIds.add(songId)) {
                    val idx = PlayerPlaylistManager.findSongIndex(songId)
                    if (idx != -1) playerController.playAtIndex(idx)
                    return@onPlayerThread
                }
                _preparedItemCache[songId] = item

                PlayerPlaylistManager.addItemsAt(desiredIndex, listOf(item))
                playerController.addMediaItems(desiredIndex, listOf(item.toMediaItemSafe()))

                val playIndex = PlayerPlaylistManager.findSongIndex(songId).takeIf { it != -1 } ?: desiredIndex
                playerController.getPlayer()?.seekTo(playIndex, 0L)
            }
        }
    }

    private fun computePlaylistHash(ids: List<Long>): Long {
        var h = 1L
        for (id in ids) {
            h = h * 31L + id
        }
        return h
    }

    private fun setFullPlaylistIds(ids: List<Long>) {
        _fullPlaylistIds = ids
        _fullPlaylistHash = computePlaylistHash(ids)
        _fullPlaylistFirst = ids.firstOrNull()
        _fullPlaylistLast = ids.lastOrNull()
        _fullPlaylistIndexMap = ids.withIndex().associate { it.value to it.index }
    }

    private fun isSameFullPlaylist(ids: List<Long>): Boolean {
        if (_fullPlaylistIds.isEmpty() || ids.isEmpty()) return false
        if (ids.size != _fullPlaylistIds.size) return false
        if (ids.firstOrNull() != _fullPlaylistFirst) return false
        if (ids.lastOrNull() != _fullPlaylistLast) return false
        return computePlaylistHash(ids) == _fullPlaylistHash
    }

    /**
     * 智能播放（用于“我喜欢的音乐”）：
     * - 如果当前就是同一个喜欢列表且目标已加载进播放器：直接 seek 到目标 index 播放
     * - 如果同列表但目标未加载：优先用缓存/单曲请求拿到目标 Item，插入到正确位置并播放；不清空，不打断后台补齐
     * - 如果不是同一个列表：走完整构建逻辑（target-first + 后台补齐）
     */
    fun playLikedSongSmart(
        targetSongId: Long,
        allSongIds: List<Long>,
        preloadCount: Int = 50,
        backgroundChunkSize: Int = 20,
        urlConcurrency: Int = 6,
    ) {
        if (isRiskBlocked()) {
            _error.value = "检测到您的网络环境存在风险，请稍后再试"
            return
        }

        if (allSongIds.isEmpty()) {
            _error.value = "播放列表为空"
            return
        }
        if (!allSongIds.contains(targetSongId)) {
            _error.value = "未找到目标歌曲"
            return
        }

        // 同一列表：优先 seek / 插入目标，不打断后台 job
        if (isSameFullPlaylist(allSongIds)) {
            val existingIndex = PlayerPlaylistManager.findSongIndex(targetSongId)
            if (existingIndex != -1) {
                viewModelScope.launch {
                    onPlayerThread { playerController.playAtIndex(existingIndex) }
                }
                return
            }

            // 未加载：只获取/复用目标 item 并插入，再 seek 播放
            viewModelScope.launch(Dispatchers.Default) {
                val item = try {
                    getOrFetchPlaylistItem(targetSongId)
                } catch (e: Exception) {
                    if (e is RiskControlException) {
                        enterRiskBlocked(e.message)
                        null
                    } else {
                        throwIfRisk(e)
                        null
                    }
                } ?: return@launch

                val targetFullIndex = _fullPlaylistIndexMap[targetSongId] ?: allSongIds.indexOf(targetSongId)
                val current = PlayerPlaylistManager.playlist.value
                val insertIndex = current.count { cur ->
                    val id = cur.song.id
                    if (id == null) false
                    else (_fullPlaylistIndexMap[id] ?: Int.MAX_VALUE) < targetFullIndex
                }

                // 走单写入者提交：避免与后台补齐并发写导致列表错位
                commitInsertSingleAndPlayById(
                    sessionId = buildSessionId.get(),
                    desiredIndex = insertIndex,
                    item = item,
                )

                // 占位 URI 会在 Service 的 DataSource 中解析为真实 URL
                onPlayerThread { playerController.playAtIndex(insertIndex) }
            }
            return
        }

        // 不同列表：重新构建
        playTargetSongWithPlaylist(
            targetSongId = targetSongId,
            allSongIds = allSongIds,
            preloadCount = preloadCount,
            backgroundChunkSize = backgroundChunkSize,
            urlConcurrency = urlConcurrency,
        )
    }

    /**
     * 智能播放（通用列表版本）：
     * - 如果当前就是同一个全量列表：优先 seek / 插入目标（避免重复提交全量 songIds 触发重建/转换）
     * - 如果不是同一个列表：走完整构建逻辑（target-first + 后台补齐）
     */
    fun playTargetSongWithPlaylistSmart(
        targetSongId: Long,
        allSongIds: List<Long>,
        preloadCount: Int = 50,
        backgroundChunkSize: Int = 20,
        urlConcurrency: Int = 6,
    ) {
        // 复用现有的“同 full playlist 则 seek/插入，否则重建”的实现
        playLikedSongSmart(
            targetSongId = targetSongId,
            allSongIds = allSongIds,
            preloadCount = preloadCount,
            backgroundChunkSize = backgroundChunkSize,
            urlConcurrency = urlConcurrency,
        )
    }

    fun clearPlaylist() {
        buildLikedPlaylistJob?.cancel()
        newBuildSessionId()
        _loadedSongIds.clear()
        _preparedItemCache.clear()
        viewModelScope.launch {
            playlistMutationMutex.withLock {
                onPlayerThread {
                    PlayerPlaylistManager.clearPlaylist()
                    playerController.clearPlaylist()
                }
            }
        }
        _currentSong.value = null
        _currentArtists.value = emptyList()
        _currentSongUrl.value = null
        _currentAlbumArtUrl.value = null
        _currentSongIndex.value = 0
        _lyrics.value = null
    }

    fun removeSongInPlaylistById(songId: Long?) {
        if (songId == null) return
        
        val currentPlaylist = PlayerPlaylistManager.playlist.value
        val index = currentPlaylist.indexOfFirst { it.song.id == songId }
        
        if (index != -1) {
            if (currentPlaylist.size == 1) {
                clearPlaylist()
                return
            }

            val currentIndex = _currentSongIndex.value
            viewModelScope.launch {
                playlistMutationMutex.withLock {
                    onPlayerThread {
                        if (index == currentIndex) {
                            val nextIndex = if (index < currentPlaylist.size - 1) index else index - 1
                            playerController.getPlayer()?.seekToDefaultPosition(nextIndex)
                            playerController.getPlayer()?.play()
                        }

                        PlayerPlaylistManager.removeItemAt(index)
                        playerController.removeMediaItemAt(index)
                    }
                }
            }

            _loadedSongIds.remove(songId)
            _preparedItemCache.remove(songId)
        }
    }

    /**
     * 设置播放器音高
     * @param pitch 音高值，1.0 为正常音高
     */
    fun setPlayerPitch(pitch: Float) {
        playerController.setPitch(pitch)
        Logger.debug(TAG, "设置播放器音高: $pitch")
    }

    /**
     * 设置播放器播放速度
     * @param speed 播放速度，1.0 为正常速度
     */
    fun setPlayerSpeed(speed: Float) {
        playerController.setPlaybackSpeed(speed)
        Logger.debug(TAG, "设置播放器播放速度: $speed")
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _currentArtists.value = song.artists ?: emptyList()

        val existingIndex = PlayerPlaylistManager.findSongIndex(song.id)
        if (existingIndex != -1) {
            playerController.getPlayer()?.seekToDefaultPosition(existingIndex)
            playerController.getPlayer()?.play()
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val playUrl = fetchSongUrl(song.id)
                val albumArtUrl = fetchAlbumArt(song.album?.id)

                if (playUrl != null) {
                    _currentSongUrl.value = playUrl
                    _currentAlbumArtUrl.value = albumArtUrl

                    PlayerPlaylistManager.addItem(song, playUrl, albumArtUrl)
                    val newIndex = PlayerPlaylistManager.playlist.value.size - 1
                    PlayerPlaylistManager.setCurrentIndex(newIndex)

                    val mediaItem = PlayerPlaylistManager.buildMediaItem(song, playUrl, albumArtUrl)
                    playerController.addAndPlay(mediaItem)
                } else {
                    _error.value = "无法获取歌曲播放链接"
                }
            } catch (e: Exception) {
                _currentSongUrl.value = null
                _currentAlbumArtUrl.value = null
                _error.value = "播放失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchSongUrl(songId: Long?): String? {
        if (songId == null) return null
        return try {
            val level = selectedAudioQualityLevel.value.ifBlank { AudioQualityLevel.default().level }
            val response = songUrlApi.getSongUrl(songIds = songId.toString(), qualityLevel = level)
            throwIfRiskCode(response.code)
            response.data?.firstOrNull()?.url
        } catch (e: Exception) {
            if (e is RiskControlException) {
                enterRiskBlocked(e.message)
                throw e
            }
            throwIfRisk(e)
            null
        }
    }

    fun refreshAvailableAudioQualities(songId: Long?) {
        if (songId == null) {
            _availableAudioQualities.value = emptyList()
            _audioQualityError.value = null
            _audioQualityLoading.value = false
            return
        }
        if (_audioQualityLoading.value) return
        if (isRiskBlocked()) {
            _audioQualityError.value = "检测到您的网络环境存在风险，请稍后再试"
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _audioQualityLoading.value = true
            _audioQualityError.value = null
            try {
                val candidates = AudioQualityLevel.probeOrder()
                // 并发控制：避免一次弹窗打太多请求
                val semaphore = Semaphore(permits = 3)
                val results = coroutineScope {
                    candidates.map { q ->
                        async {
                            semaphore.withPermit {
                                try {
                                    val resp = songUrlApi.getSongUrl(
                                        songIds = songId.toString(),
                                        qualityLevel = q.level
                                    )
                                    throwIfRiskCode(resp.code)
                                    val item = resp.data?.firstOrNull()
                                    val urlOk = !item?.url.isNullOrBlank()
                                    val actual = item?.level
                                    val available = urlOk && actual?.equals(q.level, ignoreCase = true) == true
                                    AudioQualityAvailability(
                                        requested = q,
                                        available = available,
                                        actualLevel = actual,
                                        br = item?.br,
                                        encodeType = item?.encodeType,
                                        message = item?.message,
                                    )
                                } catch (e: Exception) {
                                    if (e is RiskControlException) {
                                        enterRiskBlocked(e.message)
                                        throw e
                                    }
                                    throwIfRisk(e)
                                    AudioQualityAvailability(
                                        requested = q,
                                        available = false,
                                        actualLevel = null,
                                        br = null,
                                        encodeType = null,
                                        message = e.message,
                                    )
                                }
                            }
                        }
                    }.awaitAll()
                }

                // 过滤掉“显然不可用且没有任何信息”的项仍保留，让用户知道被探测过
                _availableAudioQualities.value = results
            } catch (e: Exception) {
                _availableAudioQualities.value = emptyList()
                _audioQualityError.value = e.message ?: "音质信息获取失败"
            } finally {
                _audioQualityLoading.value = false
            }
        }
    }

    fun applyAudioQualityToCurrentSong(level: AudioQualityLevel) {
        val song = _currentSong.value ?: run {
            _error.value = "当前没有正在播放的歌曲"
            return
        }
        val songId = song.id ?: run {
            _error.value = "歌曲 ID 无效"
            return
        }

        viewModelScope.launch {
            try {
                // 再拉一次 URL 做“确认”：如果返回的 level 不是用户选择的，视为不可用
                val resp = songUrlApi.getSongUrl(songIds = songId.toString(), qualityLevel = level.level)
                throwIfRiskCode(resp.code)
                val item = resp.data?.firstOrNull()
                val url = item?.url
                val actual = item?.level
                if (url.isNullOrBlank()) {
                    _error.value = "该歌曲在 ${level.displayName} 档位无可用播放源"
                    return@launch
                }
                if (actual == null || !actual.equals(level.level, ignoreCase = true)) {
                    _error.value = "该歌曲不支持 ${level.displayName}（回落到 ${actual ?: "未知"}）"
                    return@launch
                }

                // 确认可用后再保存选择
                settingsDataStore.setPlayerAudioQualityLevel(level.level)

                // 更新当前播放列表项 url，并替换 MediaItem（尽量保持播放进度）
                PlayerPlaylistManager.updatePlayUrlBySongId(songId, url)
                val index = playerController.getCurrentMediaItemIndex()
                val position = playerController.getCurrentPosition()
                val wasPlaying = isPlaying.value

                val mediaItem = PlayerPlaylistManager.buildMediaItem(
                    song = song,
                    playUrl = url,
                    albumArtUrl = _currentAlbumArtUrl.value,
                )
                playerController.replaceMediaItemAt(index, mediaItem)
                playerController.getPlayer()?.seekTo(index, position)
                if (wasPlaying) {
                    playerController.getPlayer()?.play()
                }
            } catch (e: Exception) {
                if (e is RiskControlException) {
                    enterRiskBlocked(e.message)
                    return@launch
                }
                throwIfRisk(e)
                _error.value = e.message ?: "切换音质失败"
            }
        }
    }

    private suspend fun fetchAlbumArt(albumId: Long?): String? {
        if (albumId == null) return null
        return try {
            val response = albumApi.getAlbumDetails(albumId = albumId)
            response.album?.picUrl
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从“我喜欢的音乐”的 ID 列表构建播放列表：
     * - 先构建“仅歌曲详情”的占位列表（不预取 URL）
     * - 只有当播放切到某一项时，才为该项请求 URL 并替换 MediaItem
     */
    fun playTargetSongWithPlaylist(
        targetSongId: Long,
        allSongIds: List<Long>,
        preloadCount: Int = 50,
        backgroundChunkSize: Int = 20,
        urlConcurrency: Int = 6,
    ) {
        if (isRiskBlocked()) {
            _error.value = "检测到您的网络环境存在风险，请稍后再试"
            return
        }

        if (allSongIds.isEmpty()) {
            _error.value = "播放列表为空"
            return
        }

        val targetIndex = allSongIds.indexOf(targetSongId)
        if (targetIndex < 0) {
            _error.value = "未找到目标歌曲"
            return
        }

        val sessionId = newBuildSessionId()

        buildLikedPlaylistJob?.cancel()
        // 网络请求放后台；但所有 MediaController 调用必须切到主线程（application thread）
        buildLikedPlaylistJob = viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            _error.value = null

            try {
                setFullPlaylistIds(allSongIds)
                _loadedSongIds.clear()
                _preparedItemCache.clear()
                // 先清空：优先加载目标歌曲并立即播放；再补齐窗口与全量
                playlistMutationMutex.withLock {
                    if (!isCurrentBuildSession(sessionId)) return@withLock
                    onPlayerThread {
                        PlayerPlaylistManager.clearPlaylist()
                        playerController.clearPlaylist()
                    }
                }
                _currentSong.value = null
                _currentArtists.value = emptyList()
                _currentSongUrl.value = null
                _currentAlbumArtUrl.value = null
                _currentSongIndex.value = 0
                _lyrics.value = null

                // === 阶段 A：快速窗口（preloadCount，默认 50） ===
                val safePreload = preloadCount.coerceAtLeast(1)
                val beforeCount = ((safePreload - 1) / 2).coerceAtLeast(0)
                val afterCount = (safePreload - 1 - beforeCount).coerceAtLeast(0)

                val windowStart = (targetIndex - beforeCount).coerceAtLeast(0)
                val windowEndExclusive = (targetIndex + afterCount + 1).coerceAtMost(allSongIds.size)
                val windowIds = allSongIds.subList(windowStart, windowEndExclusive)

                // 1) 目标歌曲优先：先插入无 URL 的占位 Item，URL 在真正播放时再加载
                val targetItem = buildPlaylistItemsByIds(listOf(targetSongId)).firstOrNull()
                    ?: PlayerPlaylistManager.PlaylistItem(song = Song(id = targetSongId), playUrl = null, albumArtUrl = null)

                // 占位 URI 会在 Service 的 DataSource 中解析为真实 URL
                commitSetPlaylist(sessionId, listOf(targetItem), startIndex = 0, startPlay = true)

                // 用户已开始播放，取消 Loading
                _isLoading.value = false

                val windowBeforeIds = allSongIds.subList(windowStart, targetIndex)
                val windowAfterIds = allSongIds.subList(targetIndex + 1, windowEndExclusive)

                // 后台批量请求时的 chunk，越大请求越少，但失败/超时成本越高
                val fetchChunkSize = 50

                // 2) 先追加窗口“后半段”（更符合用户下一首/下几首的需求）
                for (chunk in windowAfterIds.chunked(fetchChunkSize)) {
                    ensureActive()
                    if (!isCurrentBuildSession(sessionId)) return@launch
                    val items = buildPlaylistItemsByIds(chunk)
                    commitAddItemsAppend(sessionId, items)
                }

                // 3) 再插入窗口“前半段”（按 chunk 逆序插入头部，保持最终顺序）
                val beforeChunks = windowBeforeIds.chunked(fetchChunkSize)
                for (chunk in beforeChunks.asReversed()) {
                    ensureActive()
                    if (!isCurrentBuildSession(sessionId)) return@launch
                    val items = buildPlaylistItemsByIds(chunk)
                    commitAddItemsInsert(sessionId, 0, items)
                }

                // === 阶段 B：全量补齐（批量请求 + 分批提交） ===
                val remainingBeforeIds = allSongIds.subList(0, windowStart)
                val remainingAfterIds = allSongIds.subList(windowEndExclusive, allSongIds.size)

                val bgFetchChunkSize = backgroundChunkSize.coerceAtLeast(20)

                // 1) 插入目标之前剩余（同样逆序按头插，保持顺序）
                val remainingBeforeChunks = remainingBeforeIds.chunked(bgFetchChunkSize)
                for (chunk in remainingBeforeChunks.asReversed()) {
                    ensureActive()
                    if (!isCurrentBuildSession(sessionId)) return@launch
                    val items = buildPlaylistItemsByIds(chunk)
                    commitAddItemsInsert(sessionId, 0, items)
                }

                // 2) 追加目标之后剩余
                for (chunk in remainingAfterIds.chunked(bgFetchChunkSize)) {
                    ensureActive()
                    if (!isCurrentBuildSession(sessionId)) return@launch
                    val items = buildPlaylistItemsByIds(chunk)
                    commitAddItemsAppend(sessionId, items)
                }
            } catch (e: Exception) {
                if (e is RiskControlException) {
                    enterRiskBlocked(e.message)
                    return@launch
                }
                throwIfRisk(e)
                _error.value = "构建播放列表失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun buildPlaylistItemsByIds(
        ids: List<Long>,
    ): List<PlayerPlaylistManager.PlaylistItem> {
        if (ids.isEmpty()) return emptyList()

        // 先过滤已加载；再尽量复用缓存，减少重复网络请求
        val pending = ids.filter { !_loadedSongIds.contains(it) }
        if (pending.isEmpty()) return emptyList()

        val cached = pending.mapNotNull { _preparedItemCache[it] }
        val missing = pending.filter { !_preparedItemCache.containsKey(it) }
        if (missing.isEmpty()) {
            // 严格按 pending 顺序返回
            return pending.mapNotNull { _preparedItemCache[it] }
        }

        // 1) 批量 song/detail（不请求 URL；URL 在实际播放到该项时再加载）
        val detailsById: Map<Long, SongDetail> = try {
            val response = songApi.getSongDetails(missing.joinToString(","))
            throwIfRiskCode(response.code)
            response.songs.orEmpty().also { songDetailPool.putAll(it) }.associateBy { it.id }
        } catch (e: Exception) {
            if (e is RiskControlException) throw e
            throwIfRisk(e)
            emptyMap()
        }

        // 2) 严格按 ids 顺序组装 Item，保证最终播放列表顺序正确
        for (id in missing) {
            val detail = detailsById[id]
            val song = detail?.toSong() ?: Song(id = id)
            val albumArtUrl = detail?.al?.picUrl

            val item = PlayerPlaylistManager.PlaylistItem(
                song = song,
                playUrl = null,
                albumArtUrl = albumArtUrl
            )
            song.id?.let { sid -> _preparedItemCache[sid] = item }
        }

        // 严格按 pending 顺序返回（缓存 + 本次新填充）
        return pending.mapNotNull { _preparedItemCache[it] }
    }

    private sealed interface LoadResult {
        data class Item(val item: PlayerPlaylistManager.PlaylistItem) : LoadResult
        data object Skip : LoadResult
    }

    private suspend fun streamPlaylistItemsInOrder(
        ids: List<Long>,
        concurrency: Int,
        onItemsReady: suspend (List<PlayerPlaylistManager.PlaylistItem>) -> Unit,
    ) = coroutineScope {
        if (ids.isEmpty()) return@coroutineScope

        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        val results = arrayOfNulls<LoadResult>(ids.size)
        val mutex = Mutex()
        var nextToEmit = 0

        suspend fun emitReadyLocked(): List<PlayerPlaylistManager.PlaylistItem> {
            val emit = ArrayList<PlayerPlaylistManager.PlaylistItem>()
            while (nextToEmit < results.size) {
                val r = results[nextToEmit] ?: break
                when (r) {
                    is LoadResult.Item -> emit.add(r.item)
                    LoadResult.Skip -> Unit
                }
                nextToEmit++
                // 小批量输出，减少一次性插入带来的卡顿
                if (emit.size >= 3) break
            }
            return emit
        }

        val jobs = ids.mapIndexed { index, id ->
            async {
                val result = semaphore.withPermit {
                    fetchPlaylistItem(id)
                }

                var toEmit: List<PlayerPlaylistManager.PlaylistItem>
                mutex.lock()
                try {
                    results[index] = result
                    toEmit = emitReadyLocked()
                } finally {
                    mutex.unlock()
                }

                if (toEmit.isNotEmpty()) {
                    onItemsReady(toEmit)
                }
            }
        }
        jobs.awaitAll()

        // flush remaining
        while (true) {
            val toEmit = mutex.withLock {
                emitReadyLocked()
            }
            if (toEmit.isEmpty()) break
            onItemsReady(toEmit)
        }
    }

    private suspend fun fetchPlaylistItem(songId: Long): LoadResult {
        if (_loadedSongIds.contains(songId)) return LoadResult.Skip
        _preparedItemCache[songId]?.let { return LoadResult.Item(it) }

        val detail = try {
            val r = songApi.getSongDetails(songId.toString())
            throwIfRiskCode(r.code)
            r.songs?.also { songDetailPool.putAll(it) }?.firstOrNull()
        } catch (e: Exception) {
            if (e is RiskControlException) throw e
            throwIfRisk(e)
            null
        }

        val song = detail?.toSong() ?: Song(id = songId)
        val albumArtUrl = detail?.al?.picUrl

        val item = PlayerPlaylistManager.PlaylistItem(
            song = song,
            playUrl = null,
            albumArtUrl = albumArtUrl,
        )
        song.id?.let { sid -> _preparedItemCache[sid] = item }
        return LoadResult.Item(item)
    }

    private suspend fun getOrFetchPlaylistItem(songId: Long): PlayerPlaylistManager.PlaylistItem? {
        _preparedItemCache[songId]?.let { return it }

        return when (val r = fetchPlaylistItem(songId)) {
            is LoadResult.Item -> r.item
            LoadResult.Skip -> null
        }
    }

    private fun PlayerPlaylistManager.PlaylistItem.toMediaItemSafe(): MediaItem {
        return PlayerPlaylistManager.buildMediaItem(song, playUrl, albumArtUrl)
    }
}