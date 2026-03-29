package com.youyuan.music.compose.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.youyuan.music.compose.R
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.DownloadApi
import com.youyuan.music.compose.api.apis.SongUrlApi
import com.youyuan.music.compose.pref.AudioQualityLevel
import com.youyuan.music.compose.pref.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class SongDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_SONG_TITLE = "song_title"
        const val KEY_SONG_ARTIST = "song_artist"
        const val KEY_SONG_ALBUM = "song_album"
        const val KEY_ARTWORK_URL = "artwork_url"
        const val KEY_QUALITY_LEVEL = "quality_level"

        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS_TEXT = "status_text"
        const val KEY_LOCAL_URI = "local_uri"

        const val WORK_TAG = "song_download_task"

        private const val CHANNEL_ID = "song_download_channel"
        private const val CHANNEL_NAME = "Song Download"
        private const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result {
        setForeground(
            createForegroundInfo(
                progress = 0,
                text = applicationContext.getString(R.string.download_task_preparing)
            )
        )

        val songId = inputData.getLong(KEY_SONG_ID, 0L)
        if (songId <= 0L) return Result.failure()

        val songTitle = inputData.getString(KEY_SONG_TITLE)
        val songArtist = inputData.getString(KEY_SONG_ARTIST)
        val songAlbum = inputData.getString(KEY_SONG_ALBUM)
        val artworkUrl = inputData.getString(KEY_ARTWORK_URL)
        val requestedLevel = inputData.getString(KEY_QUALITY_LEVEL).orEmpty()

        return try {
            val settingsDataStore = SettingsDataStore(applicationContext)
            val apiBase = settingsDataStore.appApiUrl.first()

            val apiClient =
                ApiClient.getInstance(applicationContext, apiBase, isDebug = false).also {
                    it.setBaseUrl(apiBase)
                }
            val songUrlApi = apiClient.createService(SongUrlApi::class.java)
            val downloadApi = createDirectDownloadApi()

            val selectedLevel = requestedLevel.ifBlank { AudioQualityLevel.default().level }
            val fallbackLevel = AudioQualityLevel.default().level

            val selected = fetchSongUrl(songUrlApi, songId, selectedLevel)
            val resolved =
                selected ?: if (!selectedLevel.equals(fallbackLevel, ignoreCase = true)) {
                    fetchSongUrl(songUrlApi, songId, fallbackLevel)
                } else {
                    null
                }

            if (resolved == null) {
                return Result.failure()
            }

            val ext = resolveAudioExtension(resolved)
            val mime = resolveMimeType(ext)
            val fileName = buildSafeFileName(songTitle, songArtist, ext)

            setProgressAsync(
                androidx.work.Data.Builder()
                    .putInt(KEY_PROGRESS, 10)
                    .putString(
                        KEY_STATUS_TEXT,
                        applicationContext.getString(R.string.download_task_fetching)
                    )
                    .putString(KEY_SONG_TITLE, songTitle)
                    .putString(KEY_SONG_ARTIST, songArtist)
                    .putString(KEY_ARTWORK_URL, artworkUrl)
                    .build()
            )
            setForeground(
                createForegroundInfo(
                    10,
                    applicationContext.getString(R.string.download_task_fetching)
                )
            )

            val localUri = saveToMediaStore(
                url = resolved.url,
                fileName = fileName,
                mimeType = mime,
                ext = ext,
                title = songTitle,
                artist = songArtist,
                album = songAlbum,
                artworkUrl = artworkUrl,
                downloadApi = downloadApi,
            )

            setProgressAsync(
                androidx.work.Data.Builder()
                    .putInt(KEY_PROGRESS, 100)
                    .putString(
                        KEY_STATUS_TEXT,
                        applicationContext.getString(R.string.download_task_completed)
                    )
                    .putString(KEY_SONG_TITLE, songTitle)
                    .putString(KEY_SONG_ARTIST, songArtist)
                    .putString(KEY_ARTWORK_URL, artworkUrl)
                    .putString(KEY_LOCAL_URI, localUri)
                    .build()
            )
            setForeground(
                createForegroundInfo(
                    100,
                    applicationContext.getString(R.string.download_task_completed)
                )
            )

            Result.success(
                androidx.work.Data.Builder()
                    .putString(KEY_SONG_TITLE, songTitle)
                    .putString(KEY_SONG_ARTIST, songArtist)
                    .putString(KEY_ARTWORK_URL, artworkUrl)
                    .putString(KEY_LOCAL_URI, localUri)
                    .putString(
                        KEY_STATUS_TEXT,
                        applicationContext.getString(R.string.download_task_completed)
                    )
                    .build()
            )
        } catch (_: Exception) {
            Result.failure(
                androidx.work.Data.Builder()
                    .putString(KEY_SONG_TITLE, songTitle)
                    .putString(KEY_SONG_ARTIST, songArtist)
                    .putString(KEY_ARTWORK_URL, artworkUrl)
                    .putString(KEY_STATUS_TEXT, applicationContext.getString(R.string.download_task_status_failed))
                    .build()
            )
        }
    }

    private data class DownloadSource(
        val url: String,
        val type: String?,
        val encodeType: String?,
    )

    private fun createDirectDownloadApi(): DownloadApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .header("Accept", "*/*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(client)
            .build()
            .create(DownloadApi::class.java)
    }

    private suspend fun fetchSongUrl(
        songUrlApi: SongUrlApi,
        songId: Long,
        level: String
    ): DownloadSource? {
        val response = songUrlApi.getSongUrl(songIds = songId.toString(), qualityLevel = level)
        val item = response.data?.firstOrNull() ?: return null
        val url = item.url?.trim().orEmpty()
        if (url.isBlank()) return null
        return DownloadSource(url = url, type = item.type, encodeType = item.encodeType)
    }

    private fun resolveAudioExtension(source: DownloadSource): String {
        val type = source.type?.trim()?.lowercase(Locale.ROOT)
        if (!type.isNullOrBlank()) return type
        val encode = source.encodeType?.trim()?.lowercase(Locale.ROOT)
        if (!encode.isNullOrBlank()) return encode
        return when {
            source.url.contains(".flac", ignoreCase = true) -> "flac"
            source.url.contains(".m4a", ignoreCase = true) -> "m4a"
            source.url.contains(".aac", ignoreCase = true) -> "aac"
            else -> "mp3"
        }
    }

    private fun resolveMimeType(ext: String): String {
        return when (ext.lowercase(Locale.ROOT)) {
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "wav" -> "audio/wav"
            else -> "audio/mpeg"
        }
    }

    private fun buildSafeFileName(title: String?, artist: String?, ext: String): String {
        val safeTitle = title.orEmpty().trim().ifBlank { "Unknown Song" }
        val safeArtist = artist.orEmpty().trim().ifBlank { "Unknown Artist" }
        val base = "$safeTitle - $safeArtist".replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return "$base.$ext"
    }

    @Throws(IOException::class)
    private suspend fun saveToMediaStore(
        url: String,
        fileName: String,
        mimeType: String,
        ext: String,
        title: String?,
        artist: String?,
        album: String?,
        artworkUrl: String?,
        downloadApi: DownloadApi,
    ): String {
        val resolver = applicationContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/YouYanMusic")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (!title.isNullOrBlank()) put(MediaStore.Audio.Media.TITLE, title)
            if (!artist.isNullOrBlank()) put(MediaStore.Audio.Media.ARTIST, artist)
            if (!album.isNullOrBlank()) put(MediaStore.Audio.Media.ALBUM, album)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create media store row")

        val rawFile = withContext(Dispatchers.IO) {
            File.createTempFile(
                "yym-download-raw-",
                ".${ext.lowercase(Locale.ROOT)}",
                applicationContext.cacheDir
            )
        }

        try {
            downloadApi.downloadFile(url).byteStream().use { input ->
                rawFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val artworkBytes = fetchArtworkBytes(downloadApi = downloadApi, artworkUrl = artworkUrl)
            writeAudioTagsWithJaudiotagger(
                file = rawFile,
                ext = ext,
                title = title,
                artist = artist,
                album = album,
                artworkBytes = artworkBytes,
            )

            resolver.openOutputStream(uri)?.use { output ->
                rawFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Failed to open media output stream")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri.toString()
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        } finally {
            rawFile.delete()
        }
    }

    private suspend fun fetchArtworkBytes(
        downloadApi: DownloadApi,
        artworkUrl: String?
    ): ByteArray? {
        val url = artworkUrl?.trim().orEmpty()
        if (url.isBlank()) return null

        return try {
            downloadApi.downloadFile(url).byteStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                val limit = 5 * 1024 * 1024
                val out = ByteArrayOutputStream()
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > limit) return null
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeAudioTagsWithJaudiotagger(
        file: File,
        ext: String,
        title: String?,
        artist: String?,
        album: String?,
        artworkBytes: ByteArray?,
    ) {
        val lowerExt = ext.lowercase(Locale.ROOT)
        if (lowerExt != "mp3" && lowerExt != "flac") return

        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            if (!title.isNullOrBlank()) tag.setField(FieldKey.TITLE, title)
            if (!artist.isNullOrBlank()) tag.setField(FieldKey.ARTIST, artist)
            if (!album.isNullOrBlank()) tag.setField(FieldKey.ALBUM, album)
            // jaudiotagger 在 Android 上处理 FLAC 封面会触发 javax.imageio 依赖，
            // 这里仅对 mp3 写入内嵌封面，避免 NoClassDefFoundError 导致任务失败。
            if (lowerExt == "mp3" && artworkBytes != null && artworkBytes.isNotEmpty()) {
                tag.deleteArtworkField()
                val mimeType = detectArtworkMimeType(artworkBytes)
                val artworkExt = if (mimeType == "image/png") "png" else "jpg"
                val artworkFile = File.createTempFile(
                    "yym-artwork-",
                    ".${artworkExt}",
                    applicationContext.cacheDir
                )
                try {
                    artworkFile.outputStream().use { it.write(artworkBytes) }
                    val artwork = ArtworkFactory.createArtworkFromFile(artworkFile)
                    tag.setField(artwork)
                } finally {
                    artworkFile.delete()
                }
            }

            audioFile.commit()
        } catch (_: Throwable) {
            // 元数据写入失败时不阻塞下载主流程
        }
    }

    private fun detectArtworkMimeType(bytes: ByteArray): String {
        if (bytes.size >= 8) {
            val pngSig = byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
            )
            if (bytes.copyOfRange(0, 8).contentEquals(pngSig)) {
                return "image/png"
            }
        }
        if (bytes.size >= 3) {
            val isJpeg = bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xD8.toByte() &&
                    bytes[2] == 0xFF.toByte()
            if (isJpeg) {
                return "image/jpeg"
            }
        }
        return "image/jpeg"
    }

    private fun createForegroundInfo(progress: Int, text: String): ForegroundInfo {
        ensureNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_music)
            .setContentTitle(applicationContext.getString(R.string.download_task_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.coerceIn(0, 100), false)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun ensureNotificationChannel() {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }
}
