package com.youyuan.music.compose.utils

import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerPlaylistManager {

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    fun clearPlaylist() {
        _playlist.value = emptyList()
        _currentIndex.value = 0
    }

    fun containsSong(songId: Long?): Boolean {
        if (songId == null) return false
        return _playlist.value.any { it.mediaId.toLongOrNull() == songId }
    }

    fun findSongIndex(songId: Long?): Int {
        if (songId == null) return -1
        return _playlist.value.indexOfFirst { it.mediaId.toLongOrNull() == songId }
    }

    fun addItemAt(index: Int, item: MediaItem) {
        val currentList = _playlist.value.toMutableList()
        val safeIndex = index.coerceIn(0, currentList.size)
        currentList.add(safeIndex, item)
        _playlist.value = currentList

        if (safeIndex <= _currentIndex.value) {
            _currentIndex.value += 1
        }
    }

    fun addItems(items: List<MediaItem>) {
        val existingIds = _playlist.value.map { it.mediaId }.toSet()
        val newItems = items.filter { it.mediaId !in existingIds }
        if (newItems.isNotEmpty()) {
            _playlist.value += newItems
        }
    }

    fun addItemsAt(index: Int, items: List<MediaItem>) {
        val currentList = _playlist.value.toMutableList()
        val existingIds = currentList.map { it.mediaId }.toSet()
        val newItems = items.filter { it.mediaId !in existingIds }

        if (newItems.isNotEmpty()) {
            val safeIndex = index.coerceIn(0, currentList.size)
            currentList.addAll(safeIndex, newItems)
            _playlist.value = currentList

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

    fun setPlaylist(items: List<MediaItem>) {
        _playlist.value = items
        _currentIndex.value = 0
    }

    fun setCurrentIndex(index: Int) {
        if (index in _playlist.value.indices) {
            _currentIndex.value = index
        }
    }

    fun getCurrentItem(): MediaItem? {
        val index = _currentIndex.value
        val list = _playlist.value
        return if (index in list.indices) list[index] else null
    }

    /** 获取指定索引位置的歌曲 ID */
    fun getSongIdAt(index: Int): Long? {
        val list = _playlist.value
        return list.getOrNull(index)?.mediaId?.toLongOrNull()
    }
}
