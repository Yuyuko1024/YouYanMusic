package com.youyuan.music.compose.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.ui.viewmodel.PlaylistDetailViewModel
import com.youyuan.music.compose.utils.toSong

@UnstableApi
@UnstableSaltUiApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val playlist by viewModel.playlist.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistDetail(playlistId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        when {
            loading && playlist == null -> {
                Text(
                    text = "加载中...",
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            !error.isNullOrBlank() && playlist == null -> {
                Text(
                    text = error ?: "加载失败",
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                val data = playlist
                if (data == null) {
                    Text(
                        text = "暂无数据",
                        style = SaltTheme.textStyles.sub,
                        color = SaltTheme.colors.subText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    return@Box
                }

                val tracks: List<SongDetail> = data.tracks.orEmpty()
                val allSongIds = tracks.map { it.id }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        PlaylistHeader(
                            coverUrl = data.coverImgUrl,
                            name = data.name,
                            playCount = data.playCount,
                            trackCount = data.trackCount,
                            description = data.description,
                        )
                    }

                    item {
                        Text(
                            text = "歌曲列表",
                            style = SaltTheme.textStyles.main,
                            color = SaltTheme.colors.text,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (tracks.isEmpty()) {
                        item {
                            Text(
                                text = "暂无歌曲",
                                style = SaltTheme.textStyles.sub,
                                color = SaltTheme.colors.subText,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        itemsIndexed(tracks, key = { _, s -> s.id }) { index, songDetail ->
                            val song = songDetail.toSong()
                            PlaylistSongRow(
                                index = index + 1,
                                title = song.name,
                                subtitle = song.artists?.joinToString(", ") { it.name ?: "" }?.trim().orEmpty(),
                                onClick = {
                                    playerViewModel.playTargetSongWithPlaylist(
                                        targetSongId = songDetail.id,
                                        allSongIds = allSongIds,
                                    )
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeader(
    coverUrl: String?,
    name: String?,
    playCount: Long?,
    trackCount: Int?,
    description: String?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SaltTheme.colors.subBackground)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coverUrl,
                contentDescription = name ?: "playlist",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(shape)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name ?: "",
                    style = SaltTheme.textStyles.main,
                    color = SaltTheme.colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = buildString {
                    if (trackCount != null) append("${trackCount}首")
                    if (playCount != null) {
                        if (isNotEmpty()) append(" · ")
                        append("${formatCount(playCount)}播放")
                    }
                }
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = SaltTheme.textStyles.sub,
                        color = SaltTheme.colors.subText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    title: String?,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = SaltTheme.textStyles.sub,
            color = SaltTheme.colors.subText,
            modifier = Modifier.padding(end = 10.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title ?: "",
                style = SaltTheme.textStyles.main,
                color = SaltTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatCount(value: Long): String {
    return when {
        value >= 100_000_000L -> String.format("%.1f亿", value / 100_000_000f)
        value >= 10_000L -> String.format("%.1f万", value / 10_000f)
        else -> value.toString()
    }
}
