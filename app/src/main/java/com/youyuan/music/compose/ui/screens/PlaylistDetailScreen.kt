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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.api.model.PlaylistCreator
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.ui.viewmodel.PlaylistDetailViewModel
import com.youyuan.music.compose.ui.uicomponent.SongItem
import com.youyuan.music.compose.ui.uicomponent.SongItemPlaceholder
import com.youyuan.music.compose.ui.uicomponent.TiltedPhotoWall
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionInfo
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionSheetDialog

@UnstableApi
@UnstableSaltUiApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    modifier: Modifier = Modifier,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    var showSongActionDialog by remember { mutableStateOf(false) }
    var selectedSongForAction by remember { mutableStateOf<com.youyuan.music.compose.api.model.SongDetail?>(null) }

    if (showSongActionDialog) {
        val s = selectedSongForAction
        if (s != null) {
            SongActionSheetDialog(
                playerViewModel = playerViewModel,
                song = SongActionInfo(
                    songId = s.id,
                    title = s.name,
                    artist = s.ar?.joinToString(", ") { it.name.orEmpty() }?.ifBlank { null },
                    album = s.al?.name,
                    artworkUrl = s.al?.picUrl,
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

    val playlist by viewModel.playlist.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val allSongIds by viewModel.allSongIds.collectAsState()
    val lazyPagingItems = viewModel.songPagingFlow.collectAsLazyPagingItems()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistDetail(playlistId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        val coverUrlList by remember {
                            derivedStateOf {
                                lazyPagingItems.itemSnapshotList.items.map { it.al?.picUrl }
                            }
                        }

                        Box(
                            Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            TiltedPhotoWall(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .matchParentSize(),
                                imageUrls = coverUrlList
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                SaltTheme.colors.background.copy(alpha = 0.2f),
                                                SaltTheme.colors.background
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    item {
                        PlaylistHeader(
                            modifier = modifier.padding(horizontal = 8.dp),
                            coverUrl = data.coverImgUrl,
                            creator = data.creator,
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
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }

                    if (lazyPagingItems.itemCount == 0) {
                        item {
                            Text(
                                text = "暂无歌曲",
                                style = SaltTheme.textStyles.sub,
                                color = SaltTheme.colors.subText,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = { index ->
                                val song = lazyPagingItems[index]
                                song?.id ?: index
                            }
                        ) { index ->
                            val song = lazyPagingItems[index]
                            if (song != null) {
                                SongItem(
                                    song = song,
                                    onMoreClick = {
                                        selectedSongForAction = it
                                        showSongActionDialog = true
                                    },
                                    onClick = { songId ->
                                        // 把当前已加载的列表项写入对象池，供播放器复用/补全后反哺列表
                                        viewModel.putSongDetailsToPool(
                                            lazyPagingItems.itemSnapshotList.items
                                        )

                                        val ids = allSongIds.ifEmpty {
                                            lazyPagingItems.itemSnapshotList.items.map { it.id }
                                        }

                                        playerViewModel.playTargetSongWithPlaylistSmart(
                                            targetSongId = songId,
                                            allSongIds = ids,
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
private fun PlaylistHeader(
    coverUrl: String?,
    name: String?,
    playCount: Long?,
    creator: PlaylistCreator?,
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
                if (creator != null) {
                    Row {
                        AsyncImage(
                            model = creator.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .size(16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .align(Alignment.CenterVertically)
                        )
                        creator.nickname?.let {
                            Text(
                                modifier = Modifier.padding(4.dp).align(Alignment.CenterVertically),
                                text = it,
                                color = SaltTheme.colors.subText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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

// 歌曲条目复用 LikedSongScreen 的 SongItem（带封面）

private fun formatCount(value: Long): String {
    return when {
        value >= 100_000_000L -> String.format("%.1f亿", value / 100_000_000f)
        value >= 10_000L -> String.format("%.1f万", value / 10_000f)
        else -> value.toString()
    }
}
