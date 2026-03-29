package com.youyuan.music.compose.worker

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

private val Context.downloadTaskDataStore by preferencesDataStore(name = "download_task_store")

object DownloadTaskManager {

    private val DISMISSED_TASK_IDS = stringSetPreferencesKey("dismissed_task_ids")

    fun enqueueSongDownload(
        context: Context,
        songId: Long,
        songTitle: String?,
        songArtist: String?,
        songAlbum: String?,
        artworkUrl: String?,
        qualityLevel: String,
    ): UUID {
        val input = Data.Builder()
            .putLong(SongDownloadWorker.KEY_SONG_ID, songId)
            .putString(SongDownloadWorker.KEY_SONG_TITLE, songTitle)
            .putString(SongDownloadWorker.KEY_SONG_ARTIST, songArtist)
            .putString(SongDownloadWorker.KEY_SONG_ALBUM, songAlbum)
            .putString(SongDownloadWorker.KEY_ARTWORK_URL, artworkUrl)
            .putString(SongDownloadWorker.KEY_QUALITY_LEVEL, qualityLevel)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SongDownloadWorker>()
            .setInputData(input)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(SongDownloadWorker.WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "song-download-${request.id}",
            ExistingWorkPolicy.KEEP,
            request,
        )

        return request.id
    }

    fun observeDownloadTasks(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosByTagFlow(SongDownloadWorker.WORK_TAG)
    }

    fun observeVisibleDownloadTasks(context: Context): Flow<List<WorkInfo>> {
        val allTasksFlow = observeDownloadTasks(context)
        val dismissedFlow = context.downloadTaskDataStore.data.map { pref ->
            pref[DISMISSED_TASK_IDS] ?: emptySet()
        }

        return combine(allTasksFlow, dismissedFlow) { tasks, dismissedIds ->
            tasks.filterNot { dismissedIds.contains(it.id.toString()) }
        }
    }

    fun cancelTask(context: Context, id: UUID) {
        WorkManager.getInstance(context).cancelWorkById(id)
    }

    fun removeTaskRecord(context: Context, id: UUID) {
        // WorkManager does not support deleting exactly one finished record.
        // Avoid pruneWork() here because it may clear many finished tasks globally.
        WorkManager.getInstance(context).cancelWorkById(id)
    }

    suspend fun dismissTaskRecord(context: Context, id: UUID) {
        context.downloadTaskDataStore.edit { pref ->
            val current = pref[DISMISSED_TASK_IDS] ?: emptySet()
            pref[DISMISSED_TASK_IDS] = current + id.toString()
        }
    }

    fun deleteDownloadedFile(context: Context, localUri: String): Boolean {
        return runCatching {
            val uri = localUri.toUri()
            context.contentResolver.delete(uri, null, null) > 0
        }.getOrDefault(false)
    }
}
