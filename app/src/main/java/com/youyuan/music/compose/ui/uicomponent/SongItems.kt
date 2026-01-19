package com.youyuan.music.compose.ui.uicomponent

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.model.SongDetail

@Composable
fun SongItem(song: SongDetail, onClick: (Long) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(song.id) }
    ) {
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        val artworkUri = song.al?.picUrl ?: R.drawable.ic_nav_music.toDrawable()

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .crossfade(1000)
                .placeholder(R.drawable.ic_nav_music)
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
                .padding(end = 16.dp)
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = song.name ?: stringResource(R.string.unknown_song),
                style = SaltTheme.textStyles.main,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SaltTheme.colors.text
            )

            val artist = song.ar?.joinToString(", ") { it.name ?: "" }
                ?: stringResource(R.string.unknown_artist)
            val album = song.al?.name ?: stringResource(R.string.unknown_album)

            Text(
                text = "$artist - $album",
                style = SaltTheme.textStyles.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SaltTheme.colors.subText
            )
        }
    }
}

@Composable
fun SongItemPlaceholder() {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
                .padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Box(modifier = Modifier.width(150.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.width(100.dp).height(12.dp).background(Color.Gray.copy(alpha = 0.3f)))
        }
    }
}
