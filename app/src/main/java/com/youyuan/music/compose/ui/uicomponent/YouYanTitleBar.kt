package com.youyuan.music.compose.ui.uicomponent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.icons.ArrowBack
import com.moriafly.salt.ui.icons.SaltIcons
import com.moriafly.salt.ui.noRippleClickable
import com.youyuan.music.compose.R
import com.youyuan.music.compose.constants.AppBarHeight

@Suppress("ktlint:compose:modifier-missing-check")
@UnstableSaltUiApi
@Composable
fun YouYanTitleBar(
    onBack: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    showBackBtn: Boolean = true,
    titleVisible: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppBarHeight)
    ) {
        if (showBackBtn) {
            val backButtonContentDescription = stringResource(R.string.back_btn)
            Icon(
                modifier = Modifier
                    .size(AppBarHeight)
                    .semantics {
                        this.role = Role.Button
                        this.contentDescription = backButtonContentDescription
                    }
                    .noRippleClickable { onBack() }
                    .padding(18.dp),
                painter = rememberVectorPainter(SaltIcons.ArrowBack),
                contentDescription = backButtonContentDescription,
                tint = SaltTheme.colors.text
            )
        }

        AnimatedVisibility(
            visible = titleVisible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 180),
                initialOffsetY = { fullHeight -> fullHeight },
            ) + fadeIn(animationSpec = tween(durationMillis = 120), initialAlpha = 0f),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 180),
                targetOffsetY = { fullHeight -> -fullHeight },
            ) + fadeOut(animationSpec = tween(durationMillis = 120), targetAlpha = 0f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppBarHeight),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}
