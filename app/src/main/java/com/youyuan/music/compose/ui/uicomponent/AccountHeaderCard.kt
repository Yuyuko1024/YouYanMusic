package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.model.Profile

@Composable
@UnstableSaltUiApi
fun AccountHeaderCard(
    modifier: Modifier = Modifier,
    profile: Profile?,
    onClick: () -> Unit = {}
) {
    // 账户信息卡片组件
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(),
        border = BorderStroke(1.dp, SaltTheme.colors.stroke),
        colors = CardColors(
            contentColor = SaltTheme.colors.background,
            containerColor = SaltTheme.colors.background,
            disabledContainerColor = SaltTheme.colors.background,
            disabledContentColor = SaltTheme.colors.background
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // 背景图片
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profile?.backgroundUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "User Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            // 遮罩层
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(Color.DarkGray.copy(alpha = 0.3f))
            )
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profile?.avatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50.dp))
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Row(Modifier.padding(vertical = 8.dp)
                        .align(Alignment.CenterHorizontally)) {
                        Text(
                            text = profile?.nickname ?: stringResource(R.string.not_logged_in_account),
                            style = SaltTheme.textStyles.main,
                            maxLines = 1,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    Text(
                        text = if (profile != null) {
                            if (profile.signature.isNullOrBlank()) {
                                stringResource(R.string.bio_empty)
                            } else {
                                profile.signature
                            }
                        } else {
                            stringResource(R.string.click_to_login)
                        },
                        maxLines = 1,
                        style = SaltTheme.textStyles.sub,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}