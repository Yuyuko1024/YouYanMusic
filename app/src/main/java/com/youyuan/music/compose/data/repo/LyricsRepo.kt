package com.youyuan.music.compose.data.repo

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import com.youyuan.music.compose.api.ApiClient
import com.youyuan.music.compose.api.apis.LyricsApi
import com.youyuan.music.compose.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 歌词数据仓库：持有 lyricsApi + AutoParser，负责网络获取与本地解析。
 */
@Singleton
class LyricsRepo @Inject constructor(
    private val apiClient: ApiClient,
) {
    companion object {
        private const val TAG = "LyricsRepo"
    }

    private val lyricsApi: LyricsApi by lazy { apiClient.createService(LyricsApi::class.java) }
    private val lyricsParser by lazy { AutoParser.Builder().build() }

    private fun throwIfRisk(code: Int?) {
        if (code == -462) throw RepoRiskException("检测到您的网络环境存在风险，请稍后再试")
    }

    /** 获取歌曲原始歌词文本（已合并翻译），失败返回 null */
    suspend fun fetch(songId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val response = lyricsApi.getLyricById(songId)
            throwIfRisk(response.code)
            if (response.code != 200) return@withContext null

            val main = normalizeLrcText(response.lrc?.lyric)
            val translated = normalizeLrcText(response.tlyric?.lyric)

            if (translated.isNullOrBlank()) return@withContext main
            if (main.isNullOrBlank()) return@withContext translated

            mergeLyricsByTimestamp(main, translated)
        } catch (e: RepoRiskException) {
            throw e
        } catch (e: Exception) {
            Logger.err(TAG, "fetch failed: ${e.message}")
            null
        }
    }

    /** 解析歌词文本为 SyncedLyrics，失败返回 null */
    suspend fun parse(lyricsText: String?): SyncedLyrics? = withContext(Dispatchers.Default) {
        if (lyricsText.isNullOrBlank()) {
            Logger.debug(TAG, "parse skipped, empty text")
            return@withContext null
        }
        val sanitized = sanitizeLyricsForParser(lyricsText)
        if (sanitized.isBlank()) {
            Logger.debug(TAG, "parse skipped, empty after sanitize")
            return@withContext null
        }
        try {
            lyricsParser.parse(sanitized)
        } catch (e: Exception) {
            Logger.err(TAG, "parse failed: ${e.message}, sample=${sanitized.take(160)}")
            null
        }
    }

    // ------- private helpers (从 PVM 原样迁移) -------

    private fun sanitizeLyricsForParser(lyricsText: String): String {
        val timeTagRegex = Regex("\\[(\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?)\\]")
        val lines = ArrayList<String>()
        var lastTsMs = Int.MIN_VALUE
        lyricsText.lineSequence().forEach { line ->
            val tags = timeTagRegex.findAll(line).toList()
            if (tags.isEmpty()) { lines.add(line); return@forEach }
            if (line.replace(timeTagRegex, "").trim().isBlank()) return@forEach
            val firstTime = tags.first().groupValues.getOrNull(1)
            val ts = firstTime?.let { parseTimestampToMs(it) }
            if (ts != null && ts < lastTsMs) return@forEach
            if (ts != null) lastTsMs = ts
            lines.add(line)
        }
        return lines.joinToString("\n")
    }

    private fun parseTimestampToMs(timestamp: String): Int? {
        val parts = timestamp.trim().split(":")
        if (parts.size != 2) return null
        val min = parts[0].toIntOrNull() ?: return null
        val secParts = parts[1].split(".", limit = 2)
        val sec = secParts[0].toIntOrNull() ?: return null
        val ms = when {
            secParts.size < 2 -> 0
            secParts[1].isEmpty() -> 0
            secParts[1].length == 1 -> (secParts[1] + "00").toIntOrNull() ?: return null
            secParts[1].length == 2 -> (secParts[1] + "0").toIntOrNull() ?: return null
            else -> secParts[1].take(3).toIntOrNull() ?: return null
        }
        return min * 60_000 + sec * 1_000 + ms
    }

    private fun normalizeLrcText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.replace("\r\n", "\n").replace("\\n", "\n")
        return normalizeTimestampWithoutMillis(normalized)
    }

    private fun normalizeTimestampWithoutMillis(lrc: String): String {
        val regex = Regex("\\[(\\d{1,2}:\\d{2})(?!\\.\\d{1,3})\\]")
        return regex.replace(lrc) { "[${it.groupValues[1]}.000]" }
    }

    private fun mergeLyricsByTimestamp(main: String, translated: String): String {
        val translatedMap = buildTimestampToTextMap(translated)
        if (translatedMap.isEmpty()) return main
        val out = ArrayList<String>()
        for (line in main.split('\n')) {
            out.add(line)
            val tags = extractTimeTags(line)
            if (tags.isEmpty()) continue
            val t = translatedMap[tags.first()]?.trim().orEmpty()
            if (t.isBlank()) continue
            out.add(tags.joinToString("") { "[$it]" } + t)
        }
        return out.joinToString("\n")
    }

    private fun buildTimestampToTextMap(lrc: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (line in lrc.split('\n')) {
            val tags = extractTimeTags(line)
            if (tags.isEmpty()) continue
            val text = stripTimeTags(line).trim()
            if (text.isBlank()) continue
            for (tag in tags) map.putIfAbsent(tag, text)
        }
        return map
    }

    private fun extractTimeTags(line: String): List<String> {
        val regex = Regex("\\[(\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?)\\]")
        return regex.findAll(line)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .map { it.lowercase(Locale.US) }
            .toList()
    }

    private fun stripTimeTags(line: String): String {
        return line.replace(Regex("\\[\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?\\]"), "")
    }
}