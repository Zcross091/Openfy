package com.openfy.music.data.remote

import com.openfy.music.data.model.LyricLine
import com.openfy.music.data.model.LyricsData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

object LyricsClient {
    private val lrcRegex = Regex("""\[(\d{2}):(\d{2}(?:\.\d+)?)\\(.*)""")

    suspend fun getLyrics(
        trackTitle: String,
        artistName: String,
        durationSeconds: Int = 0,
        videoId: String? = null
    ): LyricsData = withContext(Dispatchers.IO) {
        // 1. Try YouTube Music IOS_MUSIC Timed Lyrics if videoId is known
        if (!videoId.isNullOrEmpty() && videoId.length == 11) {
            val ytLyrics = fetchYouTubeTimedLyrics(videoId, trackTitle, artistName)
            if (ytLyrics != null && ytLyrics.lines.isNotEmpty()) {
                return@withContext ytLyrics
            }
        }

        // 2. Fallback to LRCLIB API
        try {
            val titleEncoded = trackTitle.encodeURLParameter()
            val artistEncoded = artistName.encodeURLParameter()
            var url = "https://lrclib.net/api/get?track_name=$titleEncoded&artist_name=$artistEncoded"
            if (durationSeconds > 0) {
                url += "&duration=$durationSeconds"
            }

            val resp = NetworkClient.client.get(url) {
                header("User-Agent", "Openfy-Multiplatform/1.0 (https://github.com/Zcross091/Openfy)")
            }

            if (resp.status.value in 200..299) {
                val body = resp.bodyAsText()
                val json = NetworkClient.json.parseToJsonElement(body).jsonObject
                val syncedLyrics = json["syncedLyrics"]?.jsonPrimitive?.contentOrNull
                val plainLyrics = json["plainLyrics"]?.jsonPrimitive?.contentOrNull

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
            // Ignore
        }

        LyricsData(
            trackTitle = trackTitle,
            artistName = artistName,
            isSynced = false,
            plainLyrics = "Lyrics not available for this track."
        )
    }

    private suspend fun fetchYouTubeTimedLyrics(
        videoId: String,
        trackTitle: String,
        artistName: String
    ): LyricsData? = withContext(Dispatchers.IO) {
        try {
            val visitorData = YouTubeAudioResolver.getOrFetchVisitorData()
            val url = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
            val payload = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", "IOS_MUSIC")
                        put("clientVersion", "7.01.05")
                        put("clientId", "26")
                        put("hl", "en")
                        put("gl", "US")
                        if (!visitorData.isNullOrEmpty()) {
                            put("visitorData", visitorData)
                        }
                    }
                }
                put("browseId", "MPLYt_$videoId")
            }

            val resp = NetworkClient.client.post(url) {
                contentType(ContentType.Application.Json)
                header("User-Agent", "com.google.ios.youtubemusic/7.01.05 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)")
                header("X-YouTube-Client-Name", "26")
                header("X-YouTube-Client-Version", "7.01.05")
                setBody(payload.toString())
            }

            if (resp.status.value in 200..299) {
                val body = resp.bodyAsText()
                if (body.contains("timedLyricsData")) {
                    val json = NetworkClient.json.parseToJsonElement(body).jsonObject
                    val timedData = json["timedLyricsData"]?.jsonObject
                    val items = timedData?.get("lines")?.jsonArray
                    val lines = mutableListOf<LyricLine>()
                    if (items != null) {
                        for (item in items) {
                            val lineObj = item.jsonObject
                            val text = lineObj["line"]?.jsonPrimitive?.contentOrNull ?: ""
                            val startMs = lineObj["startMs"]?.jsonPrimitive?.longOrNull ?: 0L
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
            // Ignore
        }
        null
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcText.lines().forEach { rawLine ->
            val match = lrcRegex.find(rawLine.trim())
            if (match != null) {
                val mins = match.groupValues[1].toFloatOrNull() ?: 0f
                val secs = match.groupValues[2].toFloatOrNull() ?: 0f
                val text = match.groupValues[3].trim()
                val totalSecs = mins * 60f + secs
                lines.add(LyricLine(timeSeconds = totalSecs, text = text))
            }
        }
        return lines
    }
}
