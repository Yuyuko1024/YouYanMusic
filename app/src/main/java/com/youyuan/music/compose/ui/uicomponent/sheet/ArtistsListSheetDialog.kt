package com.youyuan.music.compose.ui.uicomponent.sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@UnstableSaltUiApi
@UnstableApi
@Composable
fun ArtistsListSheetDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    artists: List<SongActionArtist>,
    onArtistClick: (SongActionArtist) -> Unit,
) {
    BottomSheetDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) { dismiss ->
        RoundedColumn {
            artists
                .asSequence()
                .filter { it.artistId > 0L }
                .distinctBy { it.artistId }
                .forEach { artist ->
                    Item(
                        onClick = {
                            onArtistClick(artist)
                            dismiss()
                        },
                        text = artist.name.orEmpty().ifBlank { "Unknown Artist" },
                        iconPainter = painterResource(R.drawable.ic_artist_24px),
                        iconColor = SaltTheme.colors.highlight,
                    )
                }
        }

        Spacer(modifier = Modifier.size(8.dp))
    }
}
