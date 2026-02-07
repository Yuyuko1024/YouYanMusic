package com.youyuan.music.compose.ui.uicomponent.sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.ui.screens.ScreenRoute
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel

/**
 * 歌曲操作弹窗（当前先做最小可用）：
 * - 跳转：评论页
 * - 收藏：加入/移除“我喜欢的音乐”
 */
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@UnstableSaltUiApi
@UnstableApi
@Composable
fun SongActionSheetDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    playerViewModel: PlayerViewModel,
    song: SongActionInfo,
    navController: NavController? = null,
) {
    var liked by remember(song.songId) { mutableStateOf<Boolean?>(null) }
    var showArtistsList by remember(song.songId) { mutableStateOf(false) }

    LaunchedEffect(song.songId) {
        liked = playerViewModel.checkSongLikedOnce(song.songId)
    }

    BottomSheetDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) { dismiss ->
        val distinctArtists = song.artists
            .asSequence()
            .filter { it.artistId > 0L }
            .distinctBy { it.artistId }
            .toList()

        RoundedColumn {
            SongActionHeader(song = song)
        }

        Spacer(modifier = Modifier.size(8.dp))

        RoundedColumn {
            Item(
                onClick = {
                    song.albumId?.let { navController?.navigate(ScreenRoute.AlbumDetail.createRoute(it)) }
                    dismiss()
                },
                text = "${stringResource(R.string.song_action_album_name)}${song.album}",
                iconPainter = painterResource(R.drawable.ic_album_24px),
                iconColor = SaltTheme.colors.highlight,
            )

            val artistDisplay = (song.artist?.trim()).takeUnless { it.isNullOrBlank() }
                ?: distinctArtists.joinToString(", ") { it.name.orEmpty() }.ifBlank { null }
                ?: stringResource(R.string.unknown_artist)

            Item(
                onClick = {
                    when (distinctArtists.size) {
                        1 -> {
                            val id = distinctArtists.first().artistId
                            navController?.navigate(ScreenRoute.ArtistDetail.createRoute(id))
                            dismiss()
                        }

                        else -> {
                            if (distinctArtists.isNotEmpty()) showArtistsList = true
                        }
                    }
                },
                text = "${stringResource(R.string.song_action_artist_name)}$artistDisplay",
                iconPainter = painterResource(R.drawable.ic_artist_24px),
                iconColor = SaltTheme.colors.highlight,
            )

            Item(
                onClick = {
                    navController?.navigate(ScreenRoute.SongComments.createRoute(song.songId))
                    dismiss()
                },
                text = stringResource(R.string.song_action_comments),
                iconPainter = painterResource(R.drawable.ic_chat_bubble_count),
                iconColor = SaltTheme.colors.highlight,
            )

            val isLiked = liked == true
            Item(
                onClick = {
                    val targetLike = !isLiked
                    playerViewModel.setSongLiked(
                        songId = song.songId,
                        targetLike = targetLike,
                        onResult = { ok ->
                            if (ok) {
                                liked = targetLike
                                dismiss()
                            }
                        }
                    )
                },
                text = if (isLiked) {
                    stringResource(R.string.song_action_remove_from_liked)
                } else {
                    stringResource(R.string.song_action_add_to_liked)
                },
                iconPainter = painterResource(
                    if (isLiked) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                ),
                iconColor = SaltTheme.colors.highlight,
            )
        }

        if (showArtistsList) {
            ArtistsListSheetDialog(
                artists = distinctArtists,
                onDismissRequest = { showArtistsList = false },
                onArtistClick = { artist ->
                    navController?.navigate(ScreenRoute.ArtistDetail.createRoute(artist.artistId))
                    showArtistsList = false
                    dismiss()
                }
            )
        }
    }
}

@Composable
private fun SongActionHeader(
    song: SongActionInfo,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.artworkUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_nav_music)
                .build(),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .align(Alignment.CenterVertically),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = song.title ?: stringResource(R.string.unknown_song),
                style = SaltTheme.textStyles.main,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SaltTheme.colors.text
            )

            val sub = buildString {
                val artist = (song.artist?.trim()).takeUnless { it.isNullOrBlank() }
                    ?: song.artists.joinToString(", ") { it.name.orEmpty() }.ifBlank { null }
                    ?: stringResource(R.string.unknown_artist)
                val album = song.album ?: stringResource(R.string.unknown_album)
                append(artist)
                append(" - ")
                append(album)
            }
            Text(
                text = sub,
                style = SaltTheme.textStyles.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SaltTheme.colors.subText
            )
        }
    }
}

data class SongActionInfo(
    val songId: Long,
    val albumId: Long?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val artworkUrl: String?,
    val artists: List<SongActionArtist> = emptyList(),
)

data class SongActionArtist(
    val artistId: Long,
    val name: String?,
)
