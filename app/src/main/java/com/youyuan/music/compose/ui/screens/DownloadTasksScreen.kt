package com.youyuan.music.compose.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.R
import com.youyuan.music.compose.ui.uicomponent.YouYanTitleBar
import com.youyuan.music.compose.ui.view.ScreenScaffold
import com.youyuan.music.compose.worker.DownloadTaskManager
import com.youyuan.music.compose.worker.SongDownloadWorker
import kotlinx.coroutines.launch

@UnstableSaltUiApi
@ExperimentalMaterial3Api
@Composable
fun DownloadTasksScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tasksFlow = remember(context) { DownloadTaskManager.observeVisibleDownloadTasks(context) }
    val tasks by tasksFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var pendingDeleteTaskId by remember { mutableStateOf<String?>(null) }
    var deleteFileTogether by remember { mutableStateOf(false) }

    val sortedTasks = remember(tasks) {
        tasks.sortedByDescending { it.runAttemptCount }
    }
    val visibleTasks = sortedTasks

    ScreenScaffold(
        modifier = modifier,
        useContentPadding = true,
        topBar = {
            YouYanTitleBar(
                onBack = onBack,
                text = stringResource(R.string.drawer_download_tasks),
            )
        },
    ) { padding: PaddingValues ->
        if (visibleTasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.download_task_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleTasks, key = { it.id.toString() }) { info ->
                DownloadTaskItem(
                    info = info,
                    onCancelTask = { DownloadTaskManager.cancelTask(context, info.id) },
                    onDeleteRecord = {
                        pendingDeleteTaskId = info.id.toString()
                        deleteFileTogether = false
                    },
                )
            }
        }

        val deleteTarget = visibleTasks.firstOrNull { it.id.toString() == pendingDeleteTaskId }
        if (deleteTarget != null) {
            val localUri = deleteTarget.outputData.getString(SongDownloadWorker.KEY_LOCAL_URI)
            val canDeleteFile = !localUri.isNullOrBlank()

            AlertDialog(
                onDismissRequest = { pendingDeleteTaskId = null },
                title = {
                    Text(text = stringResource(R.string.download_task_delete_confirm_title))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.download_task_delete_confirm_message))
                        if (canDeleteFile) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = deleteFileTogether,
                                    onCheckedChange = { deleteFileTogether = it },
                                )
                                Text(text = stringResource(R.string.download_task_delete_with_file))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (deleteFileTogether && canDeleteFile) {
                            val deleted =
                                DownloadTaskManager.deleteDownloadedFile(context, localUri)
                            Toast.makeText(
                                context,
                                if (deleted) {
                                    context.getString(R.string.download_task_file_deleted)
                                } else {
                                    context.getString(R.string.download_task_file_delete_failed)
                                },
                                Toast.LENGTH_SHORT,
                            ).show()
                        }

                        DownloadTaskManager.removeTaskRecord(context, deleteTarget.id)
                        coroutineScope.launch {
                            DownloadTaskManager.dismissTaskRecord(context, deleteTarget.id)
                        }
                        pendingDeleteTaskId = null
                    }) {
                        Text(text = stringResource(R.string.download_task_delete_record))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteTaskId = null }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun DownloadTaskItem(
    info: WorkInfo,
    onCancelTask: () -> Unit,
    onDeleteRecord: () -> Unit,
) {
    val statusText = when (info.state) {
        WorkInfo.State.ENQUEUED -> stringResource(R.string.download_task_status_enqueued)
        WorkInfo.State.RUNNING -> stringResource(R.string.download_task_status_running)
        WorkInfo.State.SUCCEEDED -> stringResource(R.string.download_task_status_succeeded)
        WorkInfo.State.FAILED -> stringResource(R.string.download_task_status_failed)
        WorkInfo.State.BLOCKED -> stringResource(R.string.download_task_status_blocked)
        WorkInfo.State.CANCELLED -> stringResource(R.string.download_task_status_cancelled)
    }

    val songTitle = info.progress.getString(SongDownloadWorker.KEY_SONG_TITLE)
        ?: info.outputData.getString(SongDownloadWorker.KEY_SONG_TITLE)
        ?: info.progress.getString(SongDownloadWorker.KEY_STATUS_TEXT)
        ?: info.outputData.getString(SongDownloadWorker.KEY_STATUS_TEXT)
        ?: stringResource(R.string.download_task_notification_title)
    val songArtist = info.progress.getString(SongDownloadWorker.KEY_SONG_ARTIST)
        ?: info.outputData.getString(SongDownloadWorker.KEY_SONG_ARTIST)
    val artworkUrl = info.progress.getString(SongDownloadWorker.KEY_ARTWORK_URL)
        ?: info.outputData.getString(SongDownloadWorker.KEY_ARTWORK_URL)
    val progress = info.progress.getInt(SongDownloadWorker.KEY_PROGRESS, -1)
    val statusDetail = info.progress.getString(SongDownloadWorker.KEY_STATUS_TEXT)
        ?: info.outputData.getString(SongDownloadWorker.KEY_STATUS_TEXT)
        ?: statusText
    val isProgressKnown = progress in 0..100

    val chipColor = when (info.state) {
        WorkInfo.State.SUCCEEDED -> Color(0xFF0B7A4B)
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> Color(0xFFB3261E)
        WorkInfo.State.RUNNING -> Color(0xFF00639A)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(artworkUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_nav_music)
                        .build(),
                    contentDescription = stringResource(R.string.download_task_cover_content_description),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    if (!songArtist.isNullOrBlank()) {
                        Text(
                            text = songArtist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED || isProgressKnown) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (isProgressKnown) {
                        LinearProgressIndicator(
                            progress = { (progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(text = if (isProgressKnown) "$statusText $progress%" else statusText)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = chipColor.copy(alpha = 0.14f),
                        disabledLabelColor = chipColor,
                    ),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = statusDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )

                if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.BLOCKED) {
                    TextButton(onClick = onCancelTask) {
                        Text(text = stringResource(R.string.download_task_cancel_action))
                    }
                }

                TextButton(onClick = onDeleteRecord) {
                    Text(text = stringResource(R.string.download_task_delete_record))
                }
            }

            if (info.state == WorkInfo.State.FAILED) {
                Text(
                    text = stringResource(R.string.download_task_failed_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
