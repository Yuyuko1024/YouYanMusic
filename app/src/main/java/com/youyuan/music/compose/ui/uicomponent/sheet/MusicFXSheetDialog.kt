package com.youyuan.music.compose.ui.uicomponent.sheet

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemOuterTitle
import com.moriafly.salt.ui.ItemSlider
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.pref.SettingsDataStore
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel
import com.youyuan.music.compose.utils.IntentUtils
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@UnstableSaltUiApi
@UnstableApi
@Composable
fun MusicFXSheetDialog(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    onDismissRequest: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsDataStore = remember { SettingsDataStore(context) }

    val playbackSpeed = playerViewModel.playbackSpeed.collectAsState().value
    val pitch = playerViewModel.pitch.collectAsState().value
    val showCombinedSlider by settingsDataStore.playerCombinedSpeedPitch.collectAsState(initial = false)

    BottomSheetDialog(
        title = stringResource(R.string.music_effects),
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        RoundedColumn {
            Item(
                text = stringResource(R.string.system_equalizer),
                onClick = {
                    IntentUtils.openSystemEqualizer(context) ?: Toast.makeText(
                        context,
                        context.getString(R.string.equalizer_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        ItemOuterTitle(stringResource(R.string.speed_and_pitch))

        val options = remember {
            (10..40).map { it * 0.05f }
        }

        val speedIndex = remember(playbackSpeed) {
            options.indexOfFirst { abs(it - playbackSpeed) < 0.01f }
                .coerceAtLeast(0)
        }

        val pitchIndex = remember(pitch) {
            options.indexOfFirst { abs(it - pitch) < 0.01f }
                .coerceAtLeast(0)
        }

        RoundedColumn {
            ItemSwitcher(
                text = "同步调整",
                onChange = {
                    coroutineScope.launch {
                        settingsDataStore.setPlayerCombinedSpeedPitch(it)
                    }
                },
                state = showCombinedSlider
            )
            AnimatedContent(
                targetState = showCombinedSlider,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 })
                        .togetherWith(fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 4 })
                        .using(SizeTransform(clip = false))
                },
            ) { combined ->
                if (combined) {
                    ItemSlider(
                        text = "速度音高调整",
                        value = speedIndex.toFloat(),
                        valueRange = 0f..(options.lastIndex.toFloat()),
                        steps = options.size - 2,
                        onValueChange = {
                            val selected = options[it.roundToInt()]
                            playerViewModel.setPlayerSpeed(selected)
                            playerViewModel.setPlayerPitch(selected)
                        },
                        sub = String.format("%.2fx", playbackSpeed)
                    )
                } else {
                    Column {
                        ItemSlider(
                            text = stringResource(R.string.playback_speed),
                            value = speedIndex.toFloat(),
                            valueRange = 0f..(options.lastIndex.toFloat()),
                            steps = options.size - 2,
                            onValueChange = {
                                val selectedSpeed = options[it.roundToInt()]
                                playerViewModel.setPlayerSpeed(selectedSpeed)
                            },
                            sub = String.format("%.2fx", playbackSpeed)
                        )
                        ItemSlider(
                            text = stringResource(R.string.pitch_adjustment),
                            value = pitchIndex.toFloat(),
                            valueRange = 0f..(options.lastIndex.toFloat()),
                            steps = options.size - 2,
                            onValueChange = {
                                val selectedPitch = options[it.roundToInt()]
                                playerViewModel.setPlayerPitch(selectedPitch)
                            },
                            sub = String.format("%.2f", pitch)
                        )
                    }
                }
            }
        }
    }
}