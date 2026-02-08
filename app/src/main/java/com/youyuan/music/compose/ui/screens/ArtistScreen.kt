package com.youyuan.music.compose.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
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
import com.youyuan.music.compose.api.model.ArtistProfile
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.ui.uicomponent.AlbumItem
import com.youyuan.music.compose.ui.uicomponent.SongItem
import com.youyuan.music.compose.ui.uicomponent.SongItemPlaceholder
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionArtist
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionInfo
import com.youyuan.music.compose.ui.uicomponent.sheet.SongActionSheetDialog
import com.youyuan.music.compose.ui.viewmodel.ArtistViewModel
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@UnstableApi
@UnstableSaltUiApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ArtistScreen(
    artistId: Long,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    var showSongActionDialog by remember { mutableStateOf(false) }
    var selectedSongForAction by remember { mutableStateOf<SongDetail?>(null) }

    val artist by viewModel.artist.collectAsState()
    val topSongs by viewModel.topSongs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(error) {
        val message = error ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.consumeError()
        if (artist == null) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
    }

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

    Box(modifier = modifier.fillMaxSize()) {
        val coverUrl = artist?.cover ?: artist?.avatar
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_artist_24px)
                    .build(),
                contentDescription = artist?.name ?: "artist",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp),
                alpha = 0.25f
            )
        }

        when {
            loading && artist == null -> {
                Text(
                    text = "加载中...",
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            !error.isNullOrBlank() && artist == null -> {
                Text(
                    text = error ?: "加载失败",
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0)
                val scope = rememberCoroutineScope()

                val songsListState = rememberLazyListState()
                val albumsListState = rememberLazyListState()

                val density = LocalDensity.current
                val maxExpandedHeaderHeight = 210.dp
                // 56dp is a bit tight for avatar + text + padding; bump to avoid top clipping.
                val collapsedHeaderHeight = 64.dp

                var measuredExpandedHeaderHeightPx by remember(artistId) { mutableStateOf(0) }

                // Offstage measurement: get the natural height of the expanded header content.
                MeasureExpandedHeaderHeight(
                    artist = artist,
                    onMeasuredHeightPx = { h ->
                        if (h > 0 && h != measuredExpandedHeaderHeightPx) {
                            measuredExpandedHeaderHeightPx = h
                        }
                    }
                )

                val measuredExpandedHeaderHeightDp = remember(measuredExpandedHeaderHeightPx, density) {
                    if (measuredExpandedHeaderHeightPx <= 0) null
                    else with(density) { measuredExpandedHeaderHeightPx.toDp() }
                }

                val expandedHeaderHeight = (measuredExpandedHeaderHeightDp ?: maxExpandedHeaderHeight)
                    .coerceAtMost(maxExpandedHeaderHeight)
                    .coerceAtLeast(collapsedHeaderHeight)

                val collapseRangePx = remember(density, expandedHeaderHeight, collapsedHeaderHeight) {
                    with(density) { (expandedHeaderHeight - collapsedHeaderHeight).toPx() }
                }

                val collapseFraction by remember {
                    derivedStateOf {
                        val listState = if (pagerState.currentPage == 0) {
                            songsListState
                        } else {
                            albumsListState
                        }
                        val rawScrollPx = if (listState.firstVisibleItemIndex > 0) {
                            collapseRangePx
                        } else {
                            listState.firstVisibleItemScrollOffset.toFloat()
                        }
                        if (collapseRangePx <= 0f) 1f else (rawScrollPx / collapseRangePx).coerceIn(0f, 1f)
                    }
                }

                val headerHeight by animateDpAsState(
                    targetValue = lerp(expandedHeaderHeight, collapsedHeaderHeight, collapseFraction),
                    label = "artistHeaderHeight"
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    CollapsingArtistHeader(
                        artist = artist,
                        height = headerHeight,
                        collapseFraction = collapseFraction,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                    )

                    SecondaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = SaltTheme.colors.background.copy(alpha = 0f),
                        contentColor = SaltTheme.colors.text,
                        indicator = {
                            SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                                color = SaltTheme.colors.highlight
                            )
                        }
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            text = {
                                Text(
                                    text = stringResource(R.string.artist_hot_songs),
                                    style = SaltTheme.textStyles.sub,
                                    color = SaltTheme.colors.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            text = {
                                Text(
                                    text = stringResource(R.string.artist_albums),
                                    style = SaltTheme.textStyles.sub,
                                    color = SaltTheme.colors.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            0 -> {
                                LazyColumn(
                                    state = songsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (topSongs.isEmpty()) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.artist_no_songs),
                                                style = SaltTheme.textStyles.sub,
                                                color = SaltTheme.colors.subText,
                                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)
                                            )
                                        }
                                    } else {
                                        items(
                                            count = topSongs.size,
                                            key = { index -> topSongs[index].id }
                                        ) { index ->
                                            val song = topSongs.getOrNull(index)
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
                                                            allSongIds = topSongs.map { it.id },
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

                            else -> {
                                LazyColumn(
                                    state = albumsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (albums.isEmpty()) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.artist_no_albums),
                                                style = SaltTheme.textStyles.sub,
                                                color = SaltTheme.colors.subText,
                                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)
                                            )
                                        }
                                    } else {
                                        items(
                                            count = albums.size,
                                            key = { index -> albums[index].id ?: index.toLong() }
                                        ) { index ->
                                            val album = albums.getOrNull(index)
                                            if (album != null) {
                                                AlbumItem(
                                                    album = album,
                                                    onClick = {
                                                        val id = album.id ?: return@AlbumItem
                                                        navController.navigate(ScreenRoute.AlbumDetail.createRoute(id))
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    item { Spacer(modifier = Modifier.height(12.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsingArtistHeader(
    artist: ArtistProfile?,
    height: androidx.compose.ui.unit.Dp,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
) {
    val expandedAlpha = remember(collapseFraction) {
        val t = ((collapseFraction - 0.05f) / 0.65f).coerceIn(0f, 1f)
        1f - FastOutSlowInEasing.transform(t)
    }
    val collapsedAlpha = remember(collapseFraction) {
        val t = ((collapseFraction - 0.30f) / 0.70f).coerceIn(0f, 1f)
        FastOutSlowInEasing.transform(t)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(SaltTheme.shapes.large)
    ) {
        // Expanded header (fades out)
        if (expandedAlpha > 0.001f) {
            ArtistHeader(
                artist = artist,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(expandedAlpha)
            )
        }

        // Collapsed bar (fades in). Don't draw it at all when fully expanded.
        if (collapsedAlpha > 0.001f) {
            val bg = SaltTheme.colors.subBackground
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .heightIn(min = 0.dp, max = this@BoxWithConstraints.maxHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                bg.copy(alpha = 0f),
                                bg.copy(alpha = 0.65f * collapsedAlpha),
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .alpha(collapsedAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artist?.avatar ?: artist?.cover)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_artist_24px)
                        .build(),
                    contentDescription = artist?.name ?: "artist",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(SaltTheme.shapes.small)
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = artist?.name ?: stringResource(R.string.unknown_artist),
                    style = SaltTheme.textStyles.main,
                    color = SaltTheme.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    artist: ArtistProfile?,
    modifier: Modifier = Modifier,
) {
    val shape = SaltTheme.shapes.large

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SaltTheme.colors.subBackground.copy(alpha = 0.6f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist?.avatar ?: artist?.cover)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_artist_24px)
                    .build(),
                contentDescription = artist?.name ?: "artist",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(shape)
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = artist?.name ?: stringResource(R.string.unknown_artist),
                    style = SaltTheme.textStyles.main,
                    color = SaltTheme.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val alias = buildString {
                    val trans = artist?.transNames.orEmpty().joinToString(" / ").trim()
                    val alias = artist?.alias.orEmpty().joinToString(" / ").trim()
                    if (trans.isNotBlank()) append(trans)
                    if (alias.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(alias)
                    }
                }.ifBlank { null }

                if (alias != null) {
                    Text(
                        text = alias,
                        style = SaltTheme.textStyles.sub,
                        color = SaltTheme.colors.subText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val counts = buildString {
                    val songs = artist?.musicSize
                    val albums = artist?.albumSize
                    if (songs != null) append(stringResource(R.string.artist_songs_count, songs))
                    if (albums != null) {
                        if (isNotEmpty()) append(" · ")
                        append(stringResource(R.string.artist_albums_count, albums))
                    }
                }.ifBlank { null }

                if (counts != null) {
                    Text(
                        text = counts,
                        style = SaltTheme.textStyles.sub,
                        color = SaltTheme.colors.subText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val desc = artist?.briefDesc?.trim().orEmpty()
        if (desc.isNotBlank()) {
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = desc,
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MeasureExpandedHeaderHeight(
    artist: ArtistProfile?,
    onMeasuredHeightPx: (Int) -> Unit,
) {
    Layout(
        content = {
            ArtistHeader(
                artist = artist,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(0, 0) {}
        }

        val placeable = measurables.first().measure(
            constraints.copy(
                minHeight = 0,
                maxHeight = androidx.compose.ui.unit.Constraints.Infinity,
            )
        )

        onMeasuredHeightPx(placeable.height)

        // Don't take any space and don't draw.
        layout(0, 0) {}
    }
}
