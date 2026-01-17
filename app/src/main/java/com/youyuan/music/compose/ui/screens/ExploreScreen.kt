package com.youyuan.music.compose.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.navigation.NavHostController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.SaltTheme
import com.youyuan.music.compose.api.model.BannerItem
import com.youyuan.music.compose.api.model.PersonalFmSong
import com.youyuan.music.compose.api.model.PersonalizedNewSongItem
import com.youyuan.music.compose.api.model.PersonalizedPlaylistItem
import com.youyuan.music.compose.api.model.RecommendResourceItem
import com.youyuan.music.compose.api.model.SongDetail
import com.youyuan.music.compose.api.model.ToplistItem
import com.youyuan.music.compose.ui.viewmodel.ExploreViewModel
import com.youyuan.music.compose.ui.viewmodel.ProfileViewModel
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.utils.toSong
import kotlinx.coroutines.delay

@SuppressLint("UnusedBoxWithConstraintsScope")
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@UnstableSaltUiApi
@UnstableApi
@Composable
fun ExploreScreen(
    modifier: Modifier = Modifier,
    context: Context,
    navController: NavHostController,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {

    val isLoggedIn by profileViewModel.isLoggedIn.collectAsState()

    val banners by exploreViewModel.banners.collectAsState()
    val bannerLoading by exploreViewModel.bannerLoading.collectAsState()
    val bannerError by exploreViewModel.bannerError.collectAsState()

    val personalizedPlaylists by exploreViewModel.personalizedPlaylists.collectAsState()
    val personalizedPlaylistsLoading by exploreViewModel.personalizedPlaylistsLoading.collectAsState()
    val personalizedPlaylistsError by exploreViewModel.personalizedPlaylistsError.collectAsState()

    val newSongs by exploreViewModel.personalizedNewSongs.collectAsState()
    val newSongsLoading by exploreViewModel.personalizedNewSongsLoading.collectAsState()
    val newSongsError by exploreViewModel.personalizedNewSongsError.collectAsState()

    val toplists by exploreViewModel.toplists.collectAsState()
    val toplistsLoading by exploreViewModel.toplistsLoading.collectAsState()
    val toplistsError by exploreViewModel.toplistsError.collectAsState()

    val dailyRecommendPlaylists by exploreViewModel.dailyRecommendPlaylists.collectAsState()
    val dailyRecommendPlaylistsLoading by exploreViewModel.dailyRecommendPlaylistsLoading.collectAsState()
    val dailyRecommendPlaylistsError by exploreViewModel.dailyRecommendPlaylistsError.collectAsState()

    val dailyRecommendSongs by exploreViewModel.dailyRecommendSongs.collectAsState()
    val dailyRecommendSongsLoading by exploreViewModel.dailyRecommendSongsLoading.collectAsState()
    val dailyRecommendSongsError by exploreViewModel.dailyRecommendSongsError.collectAsState()

    val personalFmSongs by exploreViewModel.personalFmSongs.collectAsState()
    val personalFmSongsLoading by exploreViewModel.personalFmSongsLoading.collectAsState()
    val personalFmSongsError by exploreViewModel.personalFmSongsError.collectAsState()

    LaunchedEffect(Unit) {
        exploreViewModel.loadBanner(type = 2)
        exploreViewModel.loadPersonalizedPlaylists(limit = 10)
        exploreViewModel.loadPersonalizedNewSongs(limit = 10)
        exploreViewModel.loadToplistDetail()
    }

    LaunchedEffect(isLoggedIn) {
        exploreViewModel.onLoginStateChanged(isLoggedIn)
        if (isLoggedIn) {
            exploreViewModel.loadDailyRecommendPlaylists(isLoggedIn = true)
            exploreViewModel.loadDailyRecommendSongs(isLoggedIn = true)
            exploreViewModel.loadPersonalFm(isLoggedIn = true)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BannerSection(
                    banners = banners,
                    loading = bannerLoading,
                    error = bannerError,
                    onBannerClick = { item ->
                        val url = item.url
                        if (!url.isNullOrBlank()) {
                            navController.navigate(ScreenRoute.InAppWebView.createRoute(url))
                        }
                    }
                )
            }

            item {
                if (!isLoggedIn) {
                    LoginRequiredCard(
                        text = "登录后可查看：每日推荐歌单 / 每日推荐歌曲 / 私人FM",
                        onLoginClick = { navController.navigate(ScreenRoute.LoginPage.route) }
                    )
                }
            }

            if (isLoggedIn) {
                item {
                    SectionTitle(text = "每日推荐歌单")
                }

                item {
                    DailyRecommendPlaylistSection(
                        playlists = dailyRecommendPlaylists,
                        loading = dailyRecommendPlaylistsLoading,
                        error = dailyRecommendPlaylistsError,
                        onPlaylistClick = { playlistId ->
                            navController.navigate(ScreenRoute.PlaylistDetail.createRoute(playlistId))
                        }
                    )
                }

                item {
                    SectionTitle(text = "每日推荐歌曲")
                }

                item {
                    DailyRecommendSongsSection(
                        songs = dailyRecommendSongs,
                        loading = dailyRecommendSongsLoading,
                        error = dailyRecommendSongsError,
                        onSongClick = { songId ->
                            val ids = dailyRecommendSongs.map { it.id }
                            if (ids.isNotEmpty()) {
                                playerViewModel.playTargetSongWithPlaylist(
                                    targetSongId = songId,
                                    allSongIds = ids,
                                )
                            }
                        }
                    )
                }

                item {
                    SectionTitle(text = "私人FM")
                }

                item {
                    PersonalFmSection(
                        songs = personalFmSongs,
                        loading = personalFmSongsLoading,
                        error = personalFmSongsError,
                        onSongClick = { songId ->
                            val ids = personalFmSongs.map { it.id }
                            if (ids.isNotEmpty()) {
                                playerViewModel.playTargetSongWithPlaylist(
                                    targetSongId = songId,
                                    allSongIds = ids,
                                )
                            }
                        }
                    )
                }
            }

            item {
                SectionTitle(text = "推荐歌单")
            }

            item {
                PersonalizedPlaylistSection(
                    playlists = personalizedPlaylists,
                    loading = personalizedPlaylistsLoading,
                    error = personalizedPlaylistsError,
                    onPlaylistClick = { playlistId ->
                        navController.navigate(ScreenRoute.PlaylistDetail.createRoute(playlistId))
                    }
                )
            }

            item {
                SectionTitle(text = "新歌推荐")
            }

            item {
                NewSongSection(
                    songs = newSongs,
                    loading = newSongsLoading,
                    error = newSongsError,
                    onSongClick = { songId ->
                        val ids = newSongs.mapNotNull { it.resolvedSongId() }
                        if (songId != null && ids.isNotEmpty()) {
                            playerViewModel.playTargetSongWithPlaylist(
                                targetSongId = songId,
                                allSongIds = ids,
                            )
                        }
                    }
                )
            }

            item {
                SectionTitle(text = "排行榜")
            }

            item {
                ToplistSection(
                    toplists = toplists,
                    loading = toplistsLoading,
                    error = toplistsError,
                    onToplistClick = { playlistId ->
                        navController.navigate(ScreenRoute.PlaylistDetail.createRoute(playlistId))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

}

@Composable
private fun LoginRequiredCard(
    text: String,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SaltTheme.colors.subBackground)
            .clickable(onClick = onLoginClick)
            .padding(12.dp)
    ) {
        Text(
            text = text,
            style = SaltTheme.textStyles.sub,
            color = SaltTheme.colors.subText,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "去登录",
            style = SaltTheme.textStyles.main,
            color = SaltTheme.colors.text,
        )
    }
}

@Composable
private fun DailyRecommendPlaylistSection(
    playlists: List<RecommendResourceItem>,
    loading: Boolean,
    error: String?,
    onPlaylistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> Text(text = "加载中...", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        !error.isNullOrBlank() -> Text(text = error, style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        playlists.isEmpty() -> Text(text = "暂无每日推荐歌单", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        else -> {
            LazyRow(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.id }) { pl ->
                    PlaylistCard(
                        coverUrl = pl.picUrl,
                        title = pl.name,
                        subtitle = pl.playCount?.let { "${formatCount(it)} 播放" },
                        onClick = { onPlaylistClick(pl.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyRecommendSongsSection(
    songs: List<SongDetail>,
    loading: Boolean,
    error: String?,
    onSongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> Text(text = "加载中...", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        !error.isNullOrBlank() -> Text(text = error, style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        songs.isEmpty() -> Text(text = "暂无每日推荐歌曲", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        else -> {
            Column(modifier = modifier.fillMaxWidth()) {
                songs.take(10).forEach { detail ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSongClick(detail.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = detail.al?.picUrl,
                            contentDescription = detail.name ?: "song",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = detail.name.orEmpty(),
                                style = SaltTheme.textStyles.main,
                                color = SaltTheme.colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val artist = detail.ar?.joinToString(", ") { it.name.orEmpty() }?.takeIf { it.isNotBlank() }
                            if (!artist.isNullOrBlank()) {
                                Text(
                                    text = artist,
                                    style = SaltTheme.textStyles.sub,
                                    color = SaltTheme.colors.subText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalFmSection(
    songs: List<PersonalFmSong>,
    loading: Boolean,
    error: String?,
    onSongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> Text(text = "加载中...", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        !error.isNullOrBlank() -> Text(text = error, style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        songs.isEmpty() -> Text(text = "暂无私人FM", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = modifier)
        else -> {
            Column(modifier = modifier.fillMaxWidth()) {
                songs.take(10).forEach { fm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSongClick(fm.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = fm.album?.picUrl,
                            contentDescription = fm.name ?: "fm",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = fm.name.orEmpty(),
                                style = SaltTheme.textStyles.main,
                                color = SaltTheme.colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val artist = fm.artists?.joinToString(", ") { it.name.orEmpty() }?.takeIf { it.isNotBlank() }
                            if (!artist.isNullOrBlank()) {
                                Text(
                                    text = artist,
                                    style = SaltTheme.textStyles.sub,
                                    color = SaltTheme.colors.subText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@UnstableSaltUiApi
@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = SaltTheme.textStyles.main,
        modifier = modifier.padding(top = 2.dp),
        color = SaltTheme.colors.text
    )
}

@Composable
private fun PersonalizedPlaylistSection(
    playlists: List<PersonalizedPlaylistItem>,
    loading: Boolean,
    error: String?,
    onPlaylistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> {
            Text(
                text = "加载中...",
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        !error.isNullOrBlank() -> {
            Text(
                text = error,
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        playlists.isEmpty() -> {
            Text(
                text = "暂无推荐歌单",
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        else -> {
            LazyRow(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.id ?: it.hashCode().toLong() }) { pl ->
                    val id = pl.id
                    PlaylistCard(
                        coverUrl = pl.picUrl,
                        title = pl.name,
                        subtitle = pl.playCount?.let { "${formatCount(it)} 播放" },
                        onClick = { if (id != null) onPlaylistClick(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    coverUrl: String?,
    title: String?,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember { RoundedCornerShape(12.dp) }
    Column(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .background(SaltTheme.colors.subBackground)
            .padding(10.dp)
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title ?: "playlist",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(shape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title ?: "",
            style = SaltTheme.textStyles.sub,
            color = SaltTheme.colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
}

@Composable
private fun NewSongSection(
    songs: List<PersonalizedNewSongItem>,
    loading: Boolean,
    error: String?,
    onSongClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> {
            Text(
                text = "加载中...",
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        !error.isNullOrBlank() -> {
            Text(
                text = error,
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        songs.isEmpty() -> {
            Text(
                text = "暂无新歌",
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        else -> {
            Column(modifier = modifier.fillMaxWidth()) {
                songs.forEach { item ->
                    val songId = item.resolvedSongId()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSongClick(songId) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.picUrl ?: item.song?.album?.picUrl,
                            contentDescription = item.name ?: "song",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.name ?: "",
                                style = SaltTheme.textStyles.main,
                                color = SaltTheme.colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val artist = item.song?.artists?.joinToString(", ") { it.name.orEmpty() }?.takeIf { it.isNotBlank() }
                            if (!artist.isNullOrBlank()) {
                                Text(
                                    text = artist,
                                    style = SaltTheme.textStyles.sub,
                                    color = SaltTheme.colors.subText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToplistSection(
    toplists: List<ToplistItem>,
    loading: Boolean,
    error: String?,
    onToplistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> {
            Text(
                text = "加载中...",
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        !error.isNullOrBlank() -> {
            Text(
                text = error,
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        toplists.isEmpty() -> {
            Text(
                text = "暂无排行榜",
                style = SaltTheme.textStyles.sub,
                color = SaltTheme.colors.subText,
                modifier = modifier
            )
        }

        else -> {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                toplists.take(10).forEachIndexed { index, item ->
                    val id = item.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { if (id != null) onToplistClick(id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = SaltTheme.textStyles.sub,
                            color = SaltTheme.colors.subText,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        AsyncImage(
                            model = item.coverImgUrl,
                            contentDescription = item.name ?: "toplist",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.name ?: "",
                                style = SaltTheme.textStyles.main,
                                color = SaltTheme.colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = item.updateFrequency?.takeIf { it.isNotBlank() }
                                ?: item.description?.takeIf { it.isNotBlank() }
                            if (!sub.isNullOrBlank()) {
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
                }
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
@ExperimentalFoundationApi
@UnstableSaltUiApi
@Composable
private fun BannerSection(
    banners: List<BannerItem>,
    loading: Boolean,
    error: String?,
    onBannerClick: (BannerItem) -> Unit
) {
    val shape = remember { RoundedCornerShape(14.dp) }
    val pagerState = rememberPagerState(pageCount = { banners.size.coerceAtLeast(1) })

    LaunchedEffect(banners) {
        if (banners.size <= 1) return@LaunchedEffect
        while (true) {
            delay(3500)
            val next = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        when {
            loading && banners.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(shape)
                        .background(SaltTheme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Banner 加载中…", color = SaltTheme.colors.subText)
                }
            }

            !error.isNullOrBlank() && banners.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(shape)
                        .background(SaltTheme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Banner 加载失败：$error", color = SaltTheme.colors.subText)
                }
            }

            banners.isNotEmpty() -> {
                Box {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(shape)
                    ) { page ->
                        val item = banners.getOrNull(page) ?: return@HorizontalPager
                        AsyncImage(
                            model = item.pic,
                            contentDescription = item.typeTitle ?: "banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onBannerClick(item) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val current = pagerState.currentPage
                        val dotCount = banners.size.coerceAtMost(10)
                        val start = (current - dotCount / 2).coerceIn(0, (banners.size - dotCount).coerceAtLeast(0))
                        val endExclusive = (start + dotCount).coerceAtMost(banners.size)
                        for (i in start until endExclusive) {
                            val active = i == current
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (active) 7.dp else 6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (active) Color.White else Color.White.copy(alpha = 0.55f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}