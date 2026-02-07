package com.youyuan.music.compose.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.model.AlbumDetail
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.ui.uicomponent.SongItem
import com.youyuan.music.compose.ui.uicomponent.SongItemPlaceholder
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionInfo
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionSheetDialog
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionArtist
import com.youyuan.music.compose.ui.viewmodel.AlbumDetailViewModel
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UnstableApi
@UnstableSaltUiApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun AlbumScreen(
    albumId: Long,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    var showSongActionDialog by remember { mutableStateOf(false) }
    var selectedSongForAction by remember { mutableStateOf<SongDetail?>(null) }

    if (showSongActionDialog) {
        val s = selectedSongForAction
        if (s != null) {
            SongActionSheetDialog(
                playerViewModel = playerViewModel,
                song = SongActionInfo(
                    songId = s.id,
                    albumId = s.al?.id,
                    title = s.name,
                    artist = s.ar?.joinToString(", ") { it.name.orEmpty() }?.ifBlank { null },
                    album = s.al?.name,
                    artworkUrl = s.al?.picUrl,
                    artists = s.ar.orEmpty().map { SongActionArtist(artistId = it.id, name = it.name) },
                ),
                navController = navController,
                onDismissRequest = {
                    showSongActionDialog = false
                    selectedSongForAction = null
                }
            )
        } else {
            showSongActionDialog = false
        }
    }

    val album by viewModel.album.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(albumId) {
        viewModel.loadAlbumDetail(albumId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading && album == null -> {
                Text(
                    text = "加载中...",
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            !error.isNullOrBlank() && album == null -> {
                Text(
                    text = error ?: "加载失败",
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                val data = album
                if (data == null) {
                    Text(
                        text = "暂无数据",
                        style = SaltTheme.textStyles.sub,
                        color = SaltTheme.colors.subText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    return@Box
                }

                // 背景艺术图高斯模糊
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data.picUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_album_24px)
                        .build(),
                    contentDescription = data.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(15.dp),
                    alpha = 0.3f
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        AlbumHeader(
                            album = data,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }

                    item {
                        Text(
                            text = "歌曲列表",
                            style = SaltTheme.textStyles.main,
                            color = SaltTheme.colors.text,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }

                    if (songs.isEmpty()) {
                        item {
                            Text(
                                text = "暂无歌曲",
                                style = SaltTheme.textStyles.sub,
                                color = SaltTheme.colors.subText,
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)
                            )
                        }
                    } else {
                        items(
                            count = songs.size,
                            key = { index -> songs[index].id }
                        ) { index ->
                            val song = songs.getOrNull(index)
                            if (song != null) {
                                SongItem(
                                    song = song,
                                    onMoreClick = {
                                        selectedSongForAction = it
                                        showSongActionDialog = true
                                    },
                                    onClick = { songId ->
                                        playerViewModel.playTargetSongWithPlaylistSmart(
                                            targetSongId = songId,
                                            allSongIds = songs.map { it.id },
                                        )
                                    }
                                )
                            } else {
                                SongItemPlaceholder()
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    album: AlbumDetail,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SaltTheme.colors.subBackground.copy(0.5f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = album.picUrl,
                contentDescription = album.name ?: "album",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(shape)
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = album.name.orEmpty(),
                    style = SaltTheme.textStyles.main,
                    color = SaltTheme.colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val artist = album.artist?.name
                    ?: album.artists?.joinToString(", ") { it.name.orEmpty() }?.ifBlank { null }
                    ?: "未知歌手"

                val publish = formatDate(album.publishTime)
                val sub = buildString {
                    append(artist)
                    if (!publish.isNullOrBlank()) append(" · $publish")
                    if (!album.company.isNullOrBlank()) append(" · ${album.company}")
                    if (!album.subType.isNullOrBlank()) append(" · ${album.subType}")
                }

                Text(
                    text = sub,
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val commentCount = album.info?.commentCount
                val shareCount = album.info?.shareCount
                val statText = buildString {
                    if (commentCount != null) append("${formatCount(commentCount)}评论")
                    if (shareCount != null) {
                        if (isNotEmpty()) append(" · ")
                        append("${formatCount(shareCount)}分享")
                    }
                }

                if (statText.isNotBlank()) {
                    Text(
                        text = statText,
                        style = SaltTheme.textStyles.sub,
                        color = SaltTheme.colors.subText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!album.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = album.description,
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDate(millis: Long?): String? {
    if (millis == null || millis <= 0L) return null
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fmt.format(Date(millis))
    } catch (_: Throwable) {
        null
    }
}

private fun formatCount(value: Long): String {
    return when {
        value >= 100_000_000L -> String.format("%.1f亿", value / 100_000_000f)
        value >= 10_000L -> String.format("%.1f万", value / 10_000f)
        else -> value.toString()
    }
}
