package com.openfy.music.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.openfy.music.data.model.LyricLine
import com.openfy.music.data.model.LyricsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.regex.Pattern

object LyricsClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val lrcPattern = Pattern.compile("\\[(\\d{2}):(\\d{2}(?:\\.\\d+)?)\\](.*)")

    suspend fun getLyrics(trackTitle: String, artistName: String, durationSeconds: Int = 0): LyricsData = withContext(Dispatchers.IO) {
        try {
            val titleEncoded = URLEncoder.encode(trackTitle, "UTF-8")
            val artistEncoded = URLEncoder.encode(artistName, "UTF-8")
            var url = "https://lrclib.net/api/get?track_name=$titleEncoded&artist_name=$artistEncoded"
            if (durationSeconds > 0) {
                url += "&duration=$durationSeconds"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Openfy-Android/1.0 (https://github.com/Zcross091/Openfy)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)

                val syncedLyrics = json.get("syncedLyrics")?.asString
                val plainLyrics = json.get("plainLyrics")?.asString

                if (!syncedLyrics.isNullOrEmpty()) {
                    val lines = parseLrc(syncedLyrics)
                    return@withContext LyricsData(
                        trackTitle = trackTitle,
                        artistName = artistName,
                        isSynced = true,
                        lines = lines,
                        plainLyrics = plainLyrics
                    )
                } else if (!plainLyrics.isNullOrEmpty()) {
                    return@withContext LyricsData(
                        trackTitle = trackTitle,
                        artistName = artistName,
                        isSynced = false,
                        lines = emptyList(),
                        plainLyrics = plainLyrics
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        LyricsData(
            trackTitle = trackTitle,
            artistName = artistName,
            isSynced = false,
            plainLyrics = "Lyrics not available for this track."
        )
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcText.lines().forEach { line ->
            val matcher = lrcPattern.matcher(line.trim())
            if (matcher.matches()) {
                val mins = matcher.group(1)?.toFloatOrNull() ?: 0f
                val secs = matcher.group(2)?.toFloatOrNull() ?: 0f
                val text = matcher.group(3)?.trim() ?: ""
                val totalSecs = mins * 60f + secs
                lines.add(LyricLine(timeSeconds = totalSecs, text = text))
            }
        }
        return lines
    }
}
