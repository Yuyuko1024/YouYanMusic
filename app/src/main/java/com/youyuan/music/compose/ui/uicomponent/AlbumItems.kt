package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.model.AlbumDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UnstableApi
@UnstableSaltUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun AlbumItem(
    album: AlbumDetail,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        val artworkUri = album.picUrl
        Row(modifier = Modifier.weight(1f)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .crossfade(true)
                    .crossfade(600)
                    .placeholder(R.drawable.ic_album_24px)
                    .build(),
                modifier = Modifier
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
                    .padding(end = 8.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = album.name ?: "",
                    style = SaltTheme.textStyles.main,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = SaltTheme.colors.text,
                )

                val publish = formatDate(album.publishTime)
                val size = album.size

                val sub = buildString {
                    if (!publish.isNullOrBlank()) append(publish)
                    if (size != null) {
                        if (isNotEmpty()) append(" · ")
                        append(stringResource(R.string.artist_songs_count, size))
                    }
                }

                Text(
                    text = sub,
                    style = SaltTheme.textStyles.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = SaltTheme.colors.subText,
                )
            }
        }
    }
}

private fun formatDate(timestampMs: Long?): String? {
    if (timestampMs == null || timestampMs <= 0L) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Throwable) {
        null
    }
}
