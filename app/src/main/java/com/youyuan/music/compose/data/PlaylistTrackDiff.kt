package com.youyuan.music.compose.data

/**
 * 歌单 trackIds 的差异：用于在刷新后识别增删变化。
 */
data class PlaylistTrackDiff(
    val addedSongIds: List<Long> = emptyList(),
    val removedSongIds: List<Long> = emptyList(),
) {
    val hasChanges: Boolean get() = addedSongIds.isNotEmpty() || removedSongIds.isNotEmpty()
}
