package com.youyuan.music.compose.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.ui.viewmodel.MyMusicViewModel
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel


@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@UnstableSaltUiApi
@UnstableApi
@Composable
fun LikedSongScreen(
    modifier: Modifier = Modifier,
    userId: Long,
    myMusicViewModel: MyMusicViewModel,
    playerViewModel: PlayerViewModel,
) {

    val lazyPagingItems = myMusicViewModel.songPagingFlow.collectAsLazyPagingItems()

    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        myMusicViewModel.loadLikedSongs(userId.toString())
    }

    LazyColumn(state = listState) {
        items(
            count = lazyPagingItems.itemCount,
            key = { index ->
                // 如果数据已加载，使用真实ID；如果是占位符，使用索引作为Key
                val song = lazyPagingItems[index]
                song?.id ?: index
            }
        ) { index ->
            val song = lazyPagingItems[index]

            if (song != null) {
                // 数据已加载，显示真实 Item
                SongItem(
                    song = song,
                    onClick = {
                        // 把当前已加载的列表项写入对象池，供播放器复用/补全后反哺列表
                        myMusicViewModel.putSongDetailsToPool(lazyPagingItems.itemSnapshotList.items)
                        val allIds = myMusicViewModel.allSongIds.value
                        playerViewModel.playLikedSongSmart(song.id, allIds)
                    }
                )
            } else {
                // 数据未加载 (Placeholder)，显示骨架屏或加载占位符
                SongItemPlaceholder()
            }
        }
    }
}

@Composable
fun SongItem(song: SongDetail, onClick: (Long) -> Unit = {}) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            onClick(song.id)
        }
    ) {
        val artworkUri = song.al?.picUrl ?: R.drawable.ic_nav_music.toDrawable()

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .crossfade(1000)
                .placeholder(R.drawable.ic_nav_music)
                .build(),
            modifier = Modifier
                .padding(start = 4.dp)
                .padding(8.dp)
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .align(Alignment.CenterVertically),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .padding(end = 16.dp)
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = song.name ?: stringResource(R.string.unknown_song),
                style = SaltTheme.textStyles.main,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = /*if (isCurrentPlaying) SaltTheme.colors.highlight else */SaltTheme.colors.text
            )

            val artist = song.ar?.joinToString(", ") { it.name ?: "" } ?: stringResource(R.string.unknown_artist)
            val album = song.al?.name ?: stringResource(R.string.unknown_album)

            val subTitle = "$artist - $album"
            Text(
                text = subTitle,
                style = SaltTheme.textStyles.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = /*if (isCurrentPlaying) SaltTheme.colors.highlight else */SaltTheme.colors.subText
            )
        }
    }
}

@Composable
fun SongItemPlaceholder() {
    // 一个简单的占位符 UI
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Box(modifier = Modifier.size(50.dp).background(Color.Gray.copy(alpha = 0.3f)).padding(start = 8.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Box(modifier = Modifier.width(150.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.width(100.dp).height(12.dp).background(Color.Gray.copy(alpha = 0.3f)))
        }
    }
}