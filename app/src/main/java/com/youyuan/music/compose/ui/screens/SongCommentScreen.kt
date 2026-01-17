package com.youyuan.music.compose.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemOuterTextButton
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.model.CommentItem
import com.youyuan.music.compose.ui.viewmodel.SongCommentViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Heart

@UnstableSaltUiApi
@ExperimentalFoundationApi
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SongCommentScreen(
    navController: NavHostController,
    songId: Long,
    viewModel: SongCommentViewModel = hiltViewModel(),
) {
    val hotComments by viewModel.hotComments.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val total by viewModel.total.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(songId) {
        viewModel.load(songId, forceRefresh = true)
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.load(songId, forceRefresh = true) },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = buildString {
                        append(stringResource(R.string.comments_title))
                        if (total > 0) append(" ($total)")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (error != null) {
                item {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (hotComments.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.comments_hot))
                }
                items(hotComments.size) { idx ->
                    CommentRow(comment = hotComments[idx])
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.comments_latest))
            }

            items(comments.size) { idx ->
                CommentRow(comment = comments[idx])
            }

            item {
                when {
                    isLoadingMore -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }

                    hasMore -> {
                        ItemOuterTextButton(
                            onClick = { viewModel.loadMore() },
                            text = stringResource(R.string.comments_load_more),
                        )
                    }

                    else -> {
                        Text(
                            text = stringResource(R.string.comments_no_more),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(12.dp))
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (isRefreshing && hotComments.isEmpty() && comments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun CommentRow(comment: CommentItem) {
    val avatar = comment.user?.avatarUrl
    val nickname = comment.user?.nickname.orEmpty().ifBlank { "用户" }
    val timeText = comment.timeStr.orEmpty()
    val content = comment.content.orEmpty()
    val likedCount = comment.likedCount ?: 0
    val ip = comment.ipLocation?.location.orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // do nothing
            }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = avatar,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nickname,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (timeText.isNotBlank() || ip.isNotBlank()) {
                        Text(
                            text = buildString {
                                if (timeText.isNotBlank()) append(timeText)
                                if (ip.isNotBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append(stringResource(R.string.comments_ip_location_format, ip))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = TablerIcons.Heart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = likedCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            if (content.isNotBlank()) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            val replied = comment.beReplied.firstOrNull()
            if (replied?.content?.isNotBlank() == true) {
                val repliedUser = replied.user?.nickname.orEmpty().ifBlank { "用户" }
                Text(
                    text = "@${repliedUser}：${replied.content}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
        }
    }
}
