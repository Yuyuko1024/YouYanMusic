package com.youyuan.music.compose.ui.uicomponent.listitem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.ui.theme.Theme

@Composable
fun PlaylistItem(
    modifier: Modifier = Modifier,
    song: SongDetail,
    currentPlayingIndex: Int,
    itemIndex: Int,
    textColor: Color,
    onClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {}
) {
    val title = song.name ?: stringResource(R.string.unknown_song)
    val artist = song.ar?.joinToString(", ") { it.name ?: "" } ?: stringResource(R.string.unknown_artist)
    val album = song.al?.name ?: stringResource(R.string.unknown_album)

    val isCurrentPlaying = currentPlayingIndex == itemIndex

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isCurrentPlaying) Theme.colors.alphaStroke else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                modifier = Modifier.padding(vertical = 1.dp),
                text = title,
                style = SaltTheme.textStyles.main,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor
            )
            Text(
                modifier = Modifier.padding(vertical = 1.dp),
                text = "$artist - $album",
                style = SaltTheme.textStyles.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor.copy(alpha = 0.5f)
            )
        }
        IconButton(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .align(Alignment.CenterVertically),
            onClick = onRemoveClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_remove_24px),
                contentDescription = "Remove",
                tint = textColor
            )
        }
    }
}