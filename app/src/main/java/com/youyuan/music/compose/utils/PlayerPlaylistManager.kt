package com.youyuan.music.compose.utils

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.youyuan.music.compose.api.model.SongDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerPlaylistManager {
    
    data class PlaylistItem(
        val song: SongDetail,
        val playUrl: String?,
        val albumArtUrl: String?
    )
    
    private val _playlist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlist: StateFlow<List<PlaylistItem>> = _playlist.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    fun clearPlaylist() {
        _playlist.value = emptyList()
        _currentIndex.value = 0
    }
    
    fun containsSong(songId: Long?): Boolean {
        if (songId == null) return false
        return _playlist.value.any { it.song.id == songId }
    }
    
    fun findSongIndex(songId: Long?): Int {
        if (songId == null) return -1
        return _playlist.value.indexOfFirst { it.song.id == songId }
    }
    
    fun addItem(song: SongDetail, playUrl: String?, albumArtUrl: String?) {
        if (containsSong(song.id)) return
        val newItem = PlaylistItem(song, playUrl, albumArtUrl)
        _playlist.value += newItem
    }

    fun addItems(items: List<PlaylistItem>) {
        val newItems = items.filter { !containsSong(it.song.id) }
        if (newItems.isNotEmpty()) {
            _playlist.value += newItems
        }
    }

    // 新增：在指定位置插入多个 Item
    fun addItemsAt(index: Int, items: List<PlaylistItem>) {
        val currentList = _playlist.value.toMutableList()
        // 过滤掉已经存在的，避免重复
        val newItems = items.filter { newItem ->
            currentList.none { it.song.id == newItem.song.id }
        }

        if (newItems.isNotEmpty()) {
            // 确保索引不越界
            val safeIndex = index.coerceIn(0, currentList.size)
            currentList.addAll(safeIndex, newItems)
            _playlist.value = currentList

            // 如果插入位置在当前播放位置之前，需要调整 currentIndex
            val addedCount = newItems.size
            if (safeIndex <= _currentIndex.value) {
                _currentIndex.value += addedCount
            }
        }
    }
    
    fun removeItemAt(index: Int) {
        val currentList = _playlist.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _playlist.value = currentList
            
            val currentIdx = _currentIndex.value
            if (index < currentIdx) {
                _currentIndex.value = currentIdx - 1
            } else if (index == currentIdx && currentIdx >= currentList.size) {
                _currentIndex.value = (currentList.size - 1).coerceAtLeast(0)
            }
        }
    }
    
    fun setPlaylist(items: List<PlaylistItem>) {
        _playlist.value = items
        _currentIndex.value = 0
    }

    fun updatePlayUrlBySongId(songId: Long, playUrl: String) {
        val currentList = _playlist.value.toMutableList()
        val idx = currentList.indexOfFirst { it.song.id == songId }
        if (idx == -1) return
        currentList[idx] = currentList[idx].copy(playUrl = playUrl)
        _playlist.value = currentList
    }
    
    fun setCurrentIndex(index: Int) {
        if (index in _playlist.value.indices) {
            _currentIndex.value = index
        }
    }
    
    fun getCurrentItem(): PlaylistItem? {
        val index = _currentIndex.value
        val list = _playlist.value
        return if (index in list.indices) list[index] else null
    }
    
    fun PlaylistItem.toMediaItem(): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(song.id.toString())
        val resolvedUri = if (!playUrl.isNullOrBlank()) {
            Uri.parse(playUrl)
        } else {
            // 懒加载占位：由 Service 侧 DataSource 解析 songId -> 实际播放 URL
            Uri.parse("yym://song/${song.id}")
        }
        resolvedUri?.let { builder.setUri(it) }
        return builder
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.name ?: "Unknown")
                    .setArtist(song.ar?.joinToString(", ") { it.name ?: "Unknown" } ?: "Unknown")
                    .setAlbumTitle(song.al?.name)
                    .setArtworkUri(albumArtUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }
    
    fun toMediaItems(): List<MediaItem> {
        return _playlist.value.map { it.toMediaItem() }
    }
    
    fun buildMediaItem(song: SongDetail, playUrl: String?, albumArtUrl: String?): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(song.id.toString())
        val resolvedUri = if (!playUrl.isNullOrBlank()) {
            Uri.parse(playUrl)
        } else {
            // 懒加载占位：由 Service 侧 DataSource 解析 songId -> 实际播放 URL
            Uri.parse("yym://song/${song.id}")
        }
        resolvedUri?.let { builder.setUri(it) }
        return builder
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.name ?: "Unknown")
                    .setArtist(song.ar?.joinToString(", ") { it.name ?: "Unknown" } ?: "Unknown")
                    .setAlbumTitle(song.al?.name)
                    .setArtworkUri(albumArtUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }
}
