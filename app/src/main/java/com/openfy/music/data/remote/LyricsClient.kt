package com.openfy.music.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.openfy.music.data.model.LyricLine
import com.openfy.music.data.model.LyricsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.regex.Pattern

object LyricsClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val lrcPattern = Pattern.compile("\\[(\\d{2}):(\\d{2}(?:\\.\\d+)?)\\](.*)")

    suspend fun getLyrics(trackTitle: String, artistName: String, durationSeconds: Int = 0, videoId: String? = null): LyricsData = withContext(Dispatchers.IO) {
        // 1. Try YouTube Music IOS_MUSIC Timed Lyrics (LiMusic approach) if videoId is known
        if (!videoId.isNullOrEmpty() && videoId.length == 11) {
            val ytLyrics = fetchYouTubeTimedLyrics(videoId, trackTitle, artistName)
            if (ytLyrics != null && ytLyrics.lines.isNotEmpty()) {
                return@withContext ytLyrics
            }
        }

        // 2. Fallback to LRCLIB API
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

    private suspend fun fetchYouTubeTimedLyrics(videoId: String, trackTitle: String, artistName: String): LyricsData? = withContext(Dispatchers.IO) {
        try {
            val visitorData = YouTubeAudioResolver.getOrFetchVisitorData()
            val url = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", "IOS_MUSIC")
                        addProperty("clientVersion", "7.01.05")
                        addProperty("clientId", "26")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                        if (!visitorData.isNullOrEmpty()) {
                            addProperty("visitorData", visitorData)
                        }
                    }
                    add("client", clientObj)
                }
                add("context", context)
                addProperty("browseId", "MPLYt_$videoId")
            }

            val req = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("User-Agent", "com.google.ios.youtubemusic/7.01.05 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)")
                .header("X-YouTube-Client-Name", "26")
                .header("X-YouTube-Client-Version", "7.01.05")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                if (body.contains("timedLyricsData")) {
                    val lines = mutableListOf<LyricLine>()
                    val timedData = json.getAsJsonObject("timedLyricsData")
                    val items = timedData?.getAsJsonArray("lines")
                    if (items != null) {
                        for (item in items) {
                            val lineObj = item.asJsonObject
                            val text = lineObj.get("line")?.asString ?: ""
                            val startMs = lineObj.get("startMs")?.asLong ?: 0L
                            lines.add(LyricLine(timeSeconds = startMs / 1000f, text = text))
                        }
                    }
                    if (lines.isNotEmpty()) {
                        return@withContext LyricsData(
                            trackTitle = trackTitle,
                            artistName = artistName,
                            isSynced = true,
                            lines = lines
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
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
