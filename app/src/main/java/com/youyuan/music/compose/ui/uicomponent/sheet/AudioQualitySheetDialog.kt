package com.youyuan.music.compose.ui.uicomponent.sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemOuterTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.pref.AudioQualityLevel
import com.youyuan.music.compose.ui.viewmodel.PlayerViewModel

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@UnstableSaltUiApi
@UnstableApi
@Composable
fun AudioQualitySheetDialog(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    onDismissRequest: () -> Unit = {},
) {
    val currentSongId = playerViewModel.currentSong.collectAsState().value?.id
    val selectedLevelRaw = playerViewModel.selectedAudioQualityLevel.collectAsState().value
    val selectedLevel = AudioQualityLevel.fromLevel(selectedLevelRaw) ?: AudioQualityLevel.default()

    val loading = playerViewModel.audioQualityLoading.collectAsState().value
    val error = playerViewModel.audioQualityError.collectAsState().value
    val availability = playerViewModel.availableAudioQualities.collectAsState().value

    LaunchedEffect(currentSongId) {
        playerViewModel.refreshAvailableAudioQualities(currentSongId)
    }

    BottomSheetDialog(
        title = stringResource(R.string.audio_quality_title),
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) { dismiss ->
        ItemOuterTitle(stringResource(R.string.audio_quality_current_format, selectedLevel.displayName))

        RoundedColumn {
            if (loading) {
                Item(
                    text = stringResource(R.string.audio_quality_loading),
                    onClick = {}
                )
            } else {
                if (!error.isNullOrBlank()) {
                    Item(text = error, onClick = {})
                }

                val availableLevels = availability.filter { it.available }.map { it.requested }

                val levelsToShow = if (availableLevels.isNotEmpty()) {
                    availableLevels
                } else {
                    // 探测失败/无结果时，仍允许用户选回标准，保证可播放
                    listOf(AudioQualityLevel.STANDARD)
                }

                levelsToShow.forEach { level ->
                    val isSelected = level == selectedLevel
                    Item(
                        text = if (isSelected) {
                            stringResource(R.string.audio_quality_item_selected_format, level.displayName)
                        } else {
                            level.displayName
                        },
                        onClick = {
                            playerViewModel.applyAudioQualityToCurrentSong(level)
                            dismiss()
                        }
                    )
                }
            }
        }

        ItemOuterTitle(stringResource(R.string.audio_quality_hint_title))
        RoundedColumn {
            Item(text = stringResource(R.string.audio_quality_hint_text), onClick = {})
        }
    }
}
