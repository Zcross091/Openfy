package com.openfy.music.data.remote

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.openfy.music.OpenfyApplication
import com.openfy.music.data.AccountManager
import com.openfy.music.data.AudioQuality
import com.openfy.music.data.SettingsManager
import com.openfy.music.data.StreamingClient
import com.openfy.music.data.model.Album
import com.openfy.music.data.model.Artist
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.util.ArtworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ResolvedStream(
    val url: String,
    val client: StreamingClient,
    val userAgent: String
)

object YouTubeAudioResolver {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val fastCheckClient = client.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val streamCache = mutableMapOf<String, String>()
    private val streamClientCache = mutableMapOf<String, StreamingClient>()

    private var cachedVisitorData: String? = null
    private var visitorDataExpiryMs: Long = 0L

    private var cachedClientsJson: JsonObject? = null

    /**
     * Loads dynamic client definitions from assets/clients.json (borrowed from LiMusic).
     */
    fun getClientConfig(clientKey: String): JsonObject? {
        if (cachedClientsJson == null) {
            try {
                OpenfyApplication.instance.assets.open("clients.json").use { stream ->
                    InputStreamReader(stream, "UTF-8").use { reader ->
                        cachedClientsJson = gson.fromJson(reader, JsonObject::class.java)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return cachedClientsJson?.getAsJsonObject(clientKey)
    }

    /**
     * Scrape fresh session visitorData from YouTube Music service worker data endpoint.
     * Verified architecture from LiMusic (crates/innertube/src/transport.rs).
     */
    suspend fun getOrFetchVisitorData(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedVisitorData != null && System.currentTimeMillis() < visitorDataExpiryMs) {
            return@withContext cachedVisitorData
        }

        try {
            val req = Request.Builder()
                .url("https://music.youtube.com/sw.js_data")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val jsonStart = body.indexOf('[')
                if (jsonStart != -1) {
                    val jsonStr = body.substring(jsonStart)
                    val jsonArray = gson.fromJson(jsonStr, JsonArray::class.java)
                    val outer = jsonArray.get(0)?.asJsonArray
                    val arr = outer?.get(2)?.asJsonArray
                    if (arr != null) {
                        for (element in arr) {
                            if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                                val str = element.asString
                                if (str.startsWith("Cgt") || str.startsWith("Cgs") || str.startsWith("Cg")) {
                                    cachedVisitorData = str
                                    visitorDataExpiryMs = System.currentTimeMillis() + (3600_000L * 2) // Cache 2 hours
                                    return@withContext str
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return fallback or previously cached token
        return@withContext cachedVisitorData ?: "CgtsODE2NnBuLXc1QSiI1OfXBjIKCgJJThIEGgAgJ2KrAg=="
    }

    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()

        // 1. Hybrid Search Tier 1: Query Oracle Cloud backend server cache (<20ms response time)
        try {
            val backendUrl = "http://140.245.226.77/api/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val fastClient = client.newBuilder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()

            val req = Request.Builder()
                .url(backendUrl)
                .header("User-Agent", "Openfy-Android/1.0")
                .build()

            val resp = fastClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                val trackArray = json.getAsJsonArray("tracks")
                if (trackArray != null && trackArray.size() > 0) {
                    for (item in trackArray) {
                        if (!item.isJsonObject) continue
                        val obj = item.asJsonObject
                        val id = obj.get("id")?.asString ?: continue
                        val title = obj.get("title")?.asString ?: "Unknown"
                        val artist = obj.get("artist")?.asString ?: "Unknown Artist"
                        val cover = ArtworkUtils.getHighResArtworkUrl(obj.get("coverUrl")?.asString)
                        val duration = obj.get("durationSeconds")?.asInt ?: 0

                        tracks.add(
                            Track(
                                id = id,
                                title = title,
                                artists = listOf(Artist(null, artist)),
                                album = Album(name = title, coverUrl = cover),
                                durationSeconds = duration,
                                coverUrl = cover,
                                source = "youtube",
                                youtubeId = id
                            )
                        )
                        if (tracks.size >= limit) break
                    }
                    if (tracks.isNotEmpty()) {
                        return@withContext tracks
                    }
                }
            }
        } catch (e: Exception) {
            // Backend offline or timeout: seamlessly fall back to on-device InnerTube
        }

        // 2. Hybrid Search Tier 2: Local On-Device InnerTube Scraper Fallback
        try {
            val visitorData = getOrFetchVisitorData()
            val url = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", "WEB_REMIX")
                        addProperty("clientVersion", "1.20241110.01.00")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                        if (!visitorData.isNullOrEmpty()) {
                            addProperty("visitorData", visitorData)
                        }
                    }
                    add("client", clientObj)
                }
                add("context", context)
                addProperty("query", query)
                addProperty("params", "EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D") // Filter songs
            }

            val reqBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", "https://music.youtube.com")

            if (!visitorData.isNullOrEmpty()) {
                reqBuilder.header("x-goog-visitor-id", visitorData)
            }

            val response = client.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                parseSearchTracks(json, tracks, limit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (tracks.isEmpty()) {
            tracks.addAll(getMockSearchResults(query))
        }
        tracks
    }

    fun invalidateCache(trackId: String) {
        streamCache.remove(trackId)
        streamClientCache.remove(trackId)
    }

    fun isStreamUrlValid(url: String): Boolean {
        val expireMatch = java.util.regex.Pattern.compile("[&?]expire=(\\d+)").matcher(url)
        if (expireMatch.find()) {
            val expireEpochSec = expireMatch.group(1)?.toLongOrNull() ?: return true
            val currentEpochSec = System.currentTimeMillis() / 1000L
            return currentEpochSec < (expireEpochSec - 60)
        }
        return true
    }

    fun getClientUserAgent(client: StreamingClient): String {
        return when (client) {
            StreamingClient.IOS -> "com.google.ios.youtube/20.10.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)"
            StreamingClient.ANDROID_VR -> "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
            StreamingClient.VISIONOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"
            StreamingClient.TV -> "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/23.lts.3.306811-gold (unlike Gecko) v8/8.8.278.8-jit gles Starfish/2.0.3 (firmware; model)"
            else -> "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        }
    }

    fun isStreamPlayable(url: String, userAgent: String? = null): Boolean {
        return try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1")
            if (!userAgent.isNullOrEmpty()) {
                reqBuilder.header("User-Agent", userAgent)
            }
            val resp = fastCheckClient.newCall(reqBuilder.build()).execute()
            resp.use {
                it.code in 200..206
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Resolves the best playable audio stream for a track with dynamic RVX fallback:
     * Attempts preferred client -> automatically switches to fallbacks (iOS, Android VR, visionOS, NewPipe, TV, Piped)
     * if the stream fails or returns 403.
     */
    suspend fun resolveStream(
        track: Track,
        preferredClient: StreamingClient = SettingsManager.streamingClient.value,
        failedClients: Set<StreamingClient> = emptySet()
    ): ResolvedStream? = withContext(Dispatchers.IO) {
        val cacheKey = track.youtubeId ?: track.id
        if (failedClients.isEmpty()) {
            streamCache[cacheKey]?.let { cachedUrl ->
                if (isStreamUrlValid(cachedUrl)) {
                    val client = streamClientCache[cacheKey] ?: StreamingClient.IOS
                    return@withContext ResolvedStream(cachedUrl, client, getClientUserAgent(client))
                } else {
                    streamCache.remove(cacheKey)
                    streamClientCache.remove(cacheKey)
                }
            }
        }

        var videoId = if (track.youtubeId?.length == 11) {
            track.youtubeId
        } else if (track.id.length == 11 && !track.id.startsWith("LOCAL:") && !track.id.startsWith("spotify:")) {
            track.id
        } else {
            // Search for YouTube videoId using Title + Artist
            val searchResults = searchTracks("${track.title} ${track.artistNames}", limit = 3)
            searchResults.firstOrNull()?.youtubeId ?: searchResults.firstOrNull()?.id
        }

        if (videoId == null) return@withContext null

        val quality = SettingsManager.audioQuality.value

        // Assemble prioritized client fallback chain
        val fallbackOrder = mutableListOf<StreamingClient>()
        if (preferredClient != StreamingClient.AUTO && !failedClients.contains(preferredClient)) {
            fallbackOrder.add(preferredClient)
        }

        val defaultChain = listOf(
            StreamingClient.IOS,         // Fast, unciphered, bypasses music login requirements
            StreamingClient.ANDROID_VR,  // RVX #1 Spoof Client (Meta Quest VR, no signature cipher)
            StreamingClient.VISIONOS,    // Apple visionOS client
            StreamingClient.NEWPIPE,     // Local on-device NewPipe deobfuscation
            StreamingClient.TV           // TV Cobalt fallback
        )

        for (c in defaultChain) {
            if (!fallbackOrder.contains(c) && !failedClients.contains(c)) {
                fallbackOrder.add(c)
            }
        }

        // Pass 1: Try candidate clients and pre-verify range stream viability
        for (client in fallbackOrder) {
            val candidateUrl = when (client) {
                StreamingClient.IOS -> fetchIosStreamUrl(videoId, quality)
                StreamingClient.ANDROID_VR -> fetchAndroidVrStreamUrl(videoId, quality)
                StreamingClient.VISIONOS -> fetchVisionOsStreamUrl(videoId, quality)
                StreamingClient.NEWPIPE -> fetchNewPipeStreamUrl(videoId, quality)
                StreamingClient.TV -> fetchTvHtml5StreamUrl(videoId, quality)
                else -> null
            }

            if (!candidateUrl.isNullOrEmpty()) {
                val ua = getClientUserAgent(client)
                if (isStreamPlayable(candidateUrl, ua)) {
                    streamCache[cacheKey] = candidateUrl
                    streamClientCache[cacheKey] = client
                    return@withContext ResolvedStream(candidateUrl, client, ua)
                }
            }
        }

        // Pass 2: If fast pre-check was blocked by CDN or timed out, attempt raw stream URLs
        for (client in fallbackOrder) {
            val candidateUrl = when (client) {
                StreamingClient.IOS -> fetchIosStreamUrl(videoId, quality)
                StreamingClient.ANDROID_VR -> fetchAndroidVrStreamUrl(videoId, quality)
                StreamingClient.VISIONOS -> fetchVisionOsStreamUrl(videoId, quality)
                StreamingClient.NEWPIPE -> fetchNewPipeStreamUrl(videoId, quality)
                else -> null
            }
            if (!candidateUrl.isNullOrEmpty()) {
                val ua = getClientUserAgent(client)
                streamCache[cacheKey] = candidateUrl
                streamClientCache[cacheKey] = client
                return@withContext ResolvedStream(candidateUrl, client, ua)
            }
        }

        // Pass 3: External Piped distributed instance fallback
        val pipedUrl = fetchPipedStreamUrl(videoId, quality)
        if (!pipedUrl.isNullOrEmpty()) {
            return@withContext ResolvedStream(pipedUrl, StreamingClient.AUTO, "Mozilla/5.0")
        }

        null
    }

    suspend fun resolveStreamUrl(track: Track): String? {
        return resolveStream(track)?.url
    }

    private suspend fun fetchAndroidMusicStreamUrl(
        videoId: String,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cfg = getClientConfig("ANDROID_MUSIC")
            val clientName = cfg?.get("clientName")?.asString ?: "ANDROID_MUSIC"
            val clientVersion = cfg?.get("clientVersion")?.asString ?: "7.27.52"
            val clientId = cfg?.get("clientId")?.asString ?: "21"
            val userAgent = cfg?.get("userAgent")?.asString
                ?: "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14; en_US) gzip"

            val playerUrl = "https://music.youtube.com/youtubei/v1/player?prettyPrint=false"
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", clientName)
                        addProperty("clientVersion", clientVersion)
                        addProperty("clientId", clientId)
                        addProperty("osName", "Android")
                        addProperty("osVersion", "14")
                        addProperty("androidSdkVersion", 34)
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", context)
                addProperty("videoId", videoId)
            }

            val reqBuilder = Request.Builder()
                .url(playerUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .header("X-YouTube-Client-Name", clientId)
                .header("X-YouTube-Client-Version", clientVersion)

            val cookie = AccountManager.youtubeCookie
            if (!cookie.isNullOrEmpty()) {
                reqBuilder.header("Cookie", cookie)
                val authHeader = AccountManager.getSapisidHash()
                if (!authHeader.isNullOrEmpty()) {
                    reqBuilder.header("Authorization", authHeader)
                }
            }

            val response = client.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                val status = json.getAsJsonObject("playabilityStatus")?.get("status")?.asString
                if (status == "OK") {
                    val streamingData = json.getAsJsonObject("streamingData")
                    if (streamingData != null && streamingData.has("adaptiveFormats")) {
                        val formats = streamingData.getAsJsonArray("adaptiveFormats")
                        return@withContext extractBestAudioFormat(formats, quality)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun fetchNewPipeStreamUrl(videoId: String, quality: AudioQuality): String? {
        return try {
            NewPipeDownloader.init()
            val streamInfo = org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
                org.schabi.newpipe.extractor.ServiceList.YouTube,
                "https://www.youtube.com/watch?v=$videoId"
            )
            val audioStreams = streamInfo.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val targetStream = audioStreams.find { it.itag == 140 }
                    ?: audioStreams.find { it.itag == quality.targetItag }
                    ?: audioStreams.find { it.itag == quality.fallbackItag }
                    ?: audioStreams.maxByOrNull { it.averageBitrate }
                    ?: audioStreams.firstOrNull()
                targetStream?.content ?: targetStream?.url
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchIosStreamUrl(
        videoId: String,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cfg = getClientConfig("IOS")
            val clientName = cfg?.get("clientName")?.asString ?: "IOS"
            val clientVersion = cfg?.get("clientVersion")?.asString ?: "20.10.1"
            val clientId = cfg?.get("clientId")?.asString ?: "5"
            val userAgent = cfg?.get("userAgent")?.asString
                ?: "com.google.ios.youtube/20.10.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)"

            val playerUrl = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", clientName)
                        addProperty("clientVersion", clientVersion)
                        addProperty("deviceMake", "Apple")
                        addProperty("deviceModel", "iPhone16,2")
                        addProperty("userAgent", userAgent)
                        addProperty("osName", "iOS")
                        addProperty("osVersion", "17.5.1.21F90")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", context)
                addProperty("videoId", videoId)
            }

            val req = Request.Builder()
                .url(playerUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .header("X-YouTube-Client-Name", clientId)
                .header("X-YouTube-Client-Version", clientVersion)
                .build()

            val response = client.newCall(req).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                val status = json.getAsJsonObject("playabilityStatus")?.get("status")?.asString
                if (status == "OK") {
                    val streamingData = json.getAsJsonObject("streamingData")
                    if (streamingData != null && streamingData.has("adaptiveFormats")) {
                        val formats = streamingData.getAsJsonArray("adaptiveFormats")
                        return@withContext extractBestAudioFormat(formats, quality)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private suspend fun fetchAndroidVrStreamUrl(
        videoId: String,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cfg = getClientConfig("ANDROID_VR")
            val clientName = cfg?.get("clientName")?.asString ?: "ANDROID_VR"
            val clientVersion = cfg?.get("clientVersion")?.asString ?: "1.65.10"
            val clientId = cfg?.get("clientId")?.asString ?: "28"
            val userAgent = cfg?.get("userAgent")?.asString
                ?: "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

            val playerUrl = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", clientName)
                        addProperty("clientVersion", clientVersion)
                        addProperty("deviceMake", "Oculus")
                        addProperty("deviceModel", "Quest 3")
                        addProperty("androidSdkVersion", 32)
                        addProperty("osName", "Android")
                        addProperty("osVersion", "12L")
                        addProperty("userAgent", userAgent)
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", context)
                addProperty("videoId", videoId)
            }

            val req = Request.Builder()
                .url(playerUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .header("X-YouTube-Client-Name", clientId)
                .header("X-YouTube-Client-Version", clientVersion)
                .build()

            val response = client.newCall(req).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                val status = json.getAsJsonObject("playabilityStatus")?.get("status")?.asString
                if (status == "OK") {
                    val streamingData = json.getAsJsonObject("streamingData")
                    if (streamingData != null && streamingData.has("adaptiveFormats")) {
                        val formats = streamingData.getAsJsonArray("adaptiveFormats")
                        return@withContext extractBestAudioFormat(formats, quality)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private suspend fun fetchVisionOsStreamUrl(
        videoId: String,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cfg = getClientConfig("VISIONOS")
            val clientName = cfg?.get("clientName")?.asString ?: "VISIONOS"
            val clientVersion = cfg?.get("clientVersion")?.asString ?: "0.1"
            val clientId = cfg?.get("clientId")?.asString ?: "101"
            val userAgent = cfg?.get("userAgent")?.asString
                ?: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"

            val playerUrl = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", clientName)
                        addProperty("clientVersion", clientVersion)
                        addProperty("deviceMake", "Apple")
                        addProperty("deviceModel", "RealityDevice14,1")
                        addProperty("osName", "visionOS")
                        addProperty("osVersion", "1.3.21O771")
                        addProperty("userAgent", userAgent)
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", context)
                addProperty("videoId", videoId)
            }

            val req = Request.Builder()
                .url(playerUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .header("X-YouTube-Client-Name", clientId)
                .header("X-YouTube-Client-Version", clientVersion)
                .build()

            val response = client.newCall(req).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                val status = json.getAsJsonObject("playabilityStatus")?.get("status")?.asString
                if (status == "OK") {
                    val streamingData = json.getAsJsonObject("streamingData")
                    if (streamingData != null && streamingData.has("adaptiveFormats")) {
                        val formats = streamingData.getAsJsonArray("adaptiveFormats")
                        return@withContext extractBestAudioFormat(formats, quality)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private suspend fun fetchTvHtml5StreamUrl(
        videoId: String,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cfg = getClientConfig("TVHTML5")
            val clientName = cfg?.get("clientName")?.asString ?: "TVHTML5"
            val clientVersion = cfg?.get("clientVersion")?.asString ?: "7.20230405.08.01"
            val clientId = cfg?.get("clientId")?.asString ?: "85"
            val userAgent = cfg?.get("userAgent")?.asString
                ?: "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/23.lts.3.306811-gold (unlike Gecko) v8/8.8.278.8-jit gles Starfish/2.0.3 (firmware; model)"

            val playerUrl = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", clientName)
                        addProperty("clientVersion", clientVersion)
                        addProperty("clientId", clientId)
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", context)
                addProperty("videoId", videoId)
            }

            val req = Request.Builder()
                .url(playerUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .header("X-YouTube-Client-Name", clientId)
                .header("X-YouTube-Client-Version", clientVersion)
                .build()

            val response = client.newCall(req).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                val status = json.getAsJsonObject("playabilityStatus")?.get("status")?.asString
                if (status == "OK") {
                    val streamingData = json.getAsJsonObject("streamingData")
                    if (streamingData != null && streamingData.has("adaptiveFormats")) {
                        val formats = streamingData.getAsJsonArray("adaptiveFormats")
                        return@withContext extractBestAudioFormat(formats, quality)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private suspend fun fetchPipedStreamUrl(
        videoId: String,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.private.coffee",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.drgns.space",
            "https://pipedapi.reallyaweso.me"
        )

        for (base in pipedInstances) {
            try {
                val req = Request.Builder()
                    .url("$base/streams/$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val fastClient = client.newBuilder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)
                    .build()

                val resp = fastClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val audioStreams = json.getAsJsonArray("audioStreams")
                    if (audioStreams != null && audioStreams.size() > 0) {
                        var bestUrl: String? = null
                        var highestBitrate = 0L

                        for (element in audioStreams) {
                            val streamObj = element.asJsonObject
                            val streamUrl = streamObj.get("url")?.asString ?: continue
                            val itag = streamObj.get("itag")?.asInt ?: 0
                            val bitrate = streamObj.get("bitrate")?.asLong ?: 0L

                            if (itag == quality.targetItag) {
                                return@withContext streamUrl
                            } else if (itag == quality.fallbackItag && bestUrl == null) {
                                bestUrl = streamUrl
                            } else if (bitrate > highestBitrate) {
                                highestBitrate = bitrate
                                bestUrl = streamUrl
                            }
                        }

                        if (bestUrl != null) {
                            return@withContext bestUrl
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next Piped mirror
            }
        }
        null
    }

    private suspend fun fetchWebRemixStreamUrl(
        videoId: String,
        visitorData: String?,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        try {
            val playerUrl = "https://music.youtube.com/youtubei/v1/player?prettyPrint=false"
            val sts = HeadlessCipherSolver.getOrFetchSignatureTimestamp()

            val payload = JsonObject().apply {
                val context = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", "WEB_REMIX")
                        addProperty("clientVersion", "1.20241110.01.00")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                        if (!visitorData.isNullOrEmpty()) {
                            addProperty("visitorData", visitorData)
                        }
                    }
                    add("client", clientObj)
                }
                add("context", context)
                addProperty("videoId", videoId)

                // Mandatory SignatureTimestamp (STS) bypasses CDN 1MB / 50-second throttling
                if (sts > 0) {
                    val playbackContext = JsonObject().apply {
                        val contentPlaybackContext = JsonObject().apply {
                            addProperty("signatureTimestamp", sts)
                        }
                        add("contentPlaybackContext", contentPlaybackContext)
                    }
                    add("playbackContext", playbackContext)
                }
            }

            val reqBuilder = Request.Builder()
                .url(playerUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", "1.20241110.01.00")

            if (!visitorData.isNullOrEmpty()) {
                reqBuilder.header("x-goog-visitor-id", visitorData)
            }

            // Attach user's Google YouTube cookies if logged in
            val cookie = AccountManager.youtubeCookie
            if (!cookie.isNullOrEmpty()) {
                reqBuilder.header("Cookie", cookie)
                val authHeader = AccountManager.getSapisidHash()
                if (!authHeader.isNullOrEmpty()) {
                    reqBuilder.header("Authorization", authHeader)
                }
            }

            val response = client.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                val streamingData = json.getAsJsonObject("streamingData")
                if (streamingData != null && streamingData.has("adaptiveFormats")) {
                    val formats = streamingData.getAsJsonArray("adaptiveFormats")
                    return@withContext extractBestAudioFormat(formats, quality)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Extracts and prioritizes the best audio format based on user's AudioQuality preference,
     * resolving direct URLs, ciphered URLs, and n-transforms when applicable.
     */
    private suspend fun extractBestAudioFormat(formats: JsonArray, quality: AudioQuality): String? {
        var directMatchUrl: String? = null
        var aac140Url: String? = null
        var fallbackMatchUrl: String? = null
        var genericUrl: String? = null
        var highestBitrate = 0L

        for (element in formats) {
            val format = element.asJsonObject
            val mimeType = format.get("mimeType")?.asString ?: ""
            if (!mimeType.contains("audio")) continue

            val itag = format.get("itag")?.asInt ?: 0
            val bitrate = format.get("bitrate")?.asLong ?: 0L

            // 1. Get raw URL or solve cipher
            var resolvedUrl: String? = null
            if (format.has("url")) {
                val rawUrl = format.get("url").asString
                resolvedUrl = HeadlessCipherSolver.resolveNTransform(rawUrl)
            } else if (format.has("signatureCipher")) {
                val cipherStr = format.get("signatureCipher").asString
                resolvedUrl = HeadlessCipherSolver.resolveCipheredUrl(cipherStr)
            } else if (format.has("cipher")) {
                val cipherStr = format.get("cipher").asString
                resolvedUrl = HeadlessCipherSolver.resolveCipheredUrl(cipherStr)
            }

            if (resolvedUrl.isNullOrEmpty()) continue

            // Prefer AAC itag 140: YouTube Google Video CDN serves HTTP 206 Partial Content reliably on it
            if (itag == 140) {
                aac140Url = resolvedUrl
            }

            // 2. Prioritize according to quality
            if (itag == quality.targetItag) {
                directMatchUrl = resolvedUrl
                break
            } else if (itag == quality.fallbackItag && fallbackMatchUrl == null) {
                fallbackMatchUrl = resolvedUrl
            } else if (bitrate > highestBitrate) {
                highestBitrate = bitrate
                genericUrl = resolvedUrl
            }
        }

        return directMatchUrl ?: aac140Url ?: fallbackMatchUrl ?: genericUrl
    }

    /**
     * Likes or unlikes a song in the user's authentic YouTube Music library.
     * Uses SAPISIDHASH authenticated InnerTube endpoints.
     */
    suspend fun rateSong(videoId: String, isLiked: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cookie = AccountManager.youtubeCookie ?: return@withContext false
        val authHeader = AccountManager.getSapisidHash() ?: return@withContext false
        val visitorData = getOrFetchVisitorData()

        val action = if (isLiked) "like/like" else "like/removelike"
        val url = "https://music.youtube.com/youtubei/v1/$action?prettyPrint=false"

        val payload = JsonObject().apply {
            val context = JsonObject().apply {
                val clientObj = JsonObject().apply {
                    addProperty("clientName", "WEB_REMIX")
                    addProperty("clientVersion", "1.20241110.01.00")
                    addProperty("hl", "en")
                    addProperty("gl", "US")
                    if (!visitorData.isNullOrEmpty()) {
                        addProperty("visitorData", visitorData)
                    }
                }
                add("client", clientObj)
            }
            add("context", context)
            val target = JsonObject().apply {
                addProperty("videoId", videoId)
            }
            add("target", target)
        }

        try {
            val req = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .header("Cookie", cookie)
                .header("Authorization", authHeader)
                .build()

            val resp = client.newCall(req).execute()
            return@withContext resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Fetches user's authentic YouTube Music Liked Videos (FEmusic_liked_videos).
     */
    suspend fun getLikedVideos(): List<Track> = withContext(Dispatchers.IO) {
        val cookie = AccountManager.youtubeCookie
        val authHeader = AccountManager.getSapisidHash()
        if (cookie.isNullOrEmpty() || authHeader.isNullOrEmpty()) {
            return@withContext getTrendingTracks()
        }

        val visitorData = getOrFetchVisitorData()
        val url = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
        val payload = JsonObject().apply {
            val context = JsonObject().apply {
                val clientObj = JsonObject().apply {
                    addProperty("clientName", "WEB_REMIX")
                    addProperty("clientVersion", "1.20241110.01.00")
                    addProperty("hl", "en")
                    addProperty("gl", "US")
                    if (!visitorData.isNullOrEmpty()) {
                        addProperty("visitorData", visitorData)
                    }
                }
                add("client", clientObj)
            }
            add("context", context)
            addProperty("browseId", "FEmusic_liked_videos")
        }

        try {
            val req = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .header("Cookie", cookie)
                .header("Authorization", authHeader)
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val json = gson.fromJson(body, JsonObject::class.java)
                val list = mutableListOf<Track>()
                parseSearchTracks(json, list, limit = 50)
                if (list.isNotEmpty()) {
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext getTrendingTracks()
    }

    suspend fun getUserPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        return@withContext getCuratedPlaylists()
    }

    suspend fun getTrendingTracks(limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        try {
            val hits = searchTracks("Top Global Hits 2026", limit)
            if (hits.isNotEmpty()) {
                return@withContext hits
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        listOf(
            Track("J7p4bzqLvCw", "Blinding Lights", listOf(Artist(name = "The Weeknd")), durationSeconds = 202, coverUrl = "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36", isLiked = true, youtubeId = "J7p4bzqLvCw"),
            Track("hT_nvWreIhg", "Counting Stars", listOf(Artist(name = "OneRepublic")), durationSeconds = 257, coverUrl = "https://i.scdn.co/image/ab67616d0000b273b749622cf3977e2d97adba4a", youtubeId = "hT_nvWreIhg"),
            Track("JGwWNGJdvx8", "Shape of You", listOf(Artist(name = "Ed Sheeran")), durationSeconds = 233, coverUrl = "https://i.scdn.co/image/ab67616d0000b273ba5db46f4b838ef6027e6f96", youtubeId = "JGwWNGJdvx8"),
            Track("4NRXx6U8ABQ", "Save Your Tears", listOf(Artist(name = "The Weeknd")), durationSeconds = 215, coverUrl = "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36", isLiked = true, youtubeId = "4NRXx6U8ABQ"),
            Track("0VwhorQBdMc", "Sunflower", listOf(Artist(name = "Post Malone"), Artist(name = "Swae Lee")), durationSeconds = 158, coverUrl = "https://i.scdn.co/image/ab67616d0000b273e2e352d89826aef6dbd5ff8f", youtubeId = "0VwhorQBdMc"),
            Track("nfWlot6h_JM", "Shake It Off", listOf(Artist(name = "Taylor Swift")), durationSeconds = 219, coverUrl = "https://i.scdn.co/image/ab67616d0000b273904445d70d04eb34d6de7970", youtubeId = "nfWlot6h_JM"),
            Track("YQHsXMglC9A", "Hello", listOf(Artist(name = "Adele")), durationSeconds = 295, coverUrl = "https://i.scdn.co/image/ab67616d0000b27329df04c45b59f77f3050c266", youtubeId = "YQHsXMglC9A")
        )
    }

    fun getCuratedPlaylists(): List<Playlist> {
        return listOf(
            Playlist("RDCLAK5uy_kmPRjFGN7YstCHfpYq2KRxTyCeoogiqcU", "Today's Top Hits", "The hottest tracks on YouTube Music & Spotify.", "https://i.scdn.co/image/ab67706f00000002724554f35da514fa501739a0", 50, "youtube"),
            Playlist("RDCLAK5uy_n9Fbdw7e6ap-98_A-8JYMwW6viui0iaw4", "All Out 2020s", "The biggest hits of the decade so far.", "https://i.scdn.co/image/ab67706f00000002b55b6074da1d43715fc16d6d", 45, "youtube"),
            Playlist("RDCLAK5uy_kL6A4y4P3x8o0R5X5o3T0g2J1r9c6p5E", "RapCaviar", "New hip-hop and rap releases from top artists.", "https://i.scdn.co/image/ab67706f00000002364c0f135b5420d20d75a5c6", 60, "youtube"),
            Playlist("RDCLAK5uy_l7T5_9_Gz2n7W8i3P9b4E2r1o6c8y5Q", "Pop Rising", "The hits of tomorrow are here today.", "https://i.scdn.co/image/ab67706f000000029b35b6f3c051912a7732ad51", 40, "youtube")
        )
    }

    private fun parseSearchTracks(json: JsonObject, list: MutableList<Track>, limit: Int) {
        try {
            val contents = json.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")

            if (contents != null) {
                for (section in contents) {
                    val sectionObj = section.asJsonObject

                    // Parse Top Result Card (e.g. 'The Box') if present
                    val cardShelf = sectionObj.getAsJsonObject("musicCardShelfRenderer")
                    if (cardShelf != null) {
                        val title = cardShelf.getAsJsonObject("title")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: "Unknown Track"
                        val subtitleRuns = cardShelf.getAsJsonObject("subtitle")?.getAsJsonArray("runs")
                        val artistName = if (subtitleRuns != null && subtitleRuns.size() > 0) {
                            subtitleRuns.get(0).asJsonObject.get("text")?.asString ?: "Unknown Artist"
                        } else {
                            "Unknown Artist"
                        }
                        val cardVideoId = cardShelf.getAsJsonObject("onTap")?.getAsJsonObject("watchEndpoint")?.get("videoId")?.asString
                            ?: if (cardShelf.has("buttons") && cardShelf.getAsJsonArray("buttons").size() > 0) {
                                cardShelf.getAsJsonArray("buttons").get(0).asJsonObject
                                    .getAsJsonObject("buttonRenderer")
                                    ?.getAsJsonObject("command")
                                    ?.getAsJsonObject("watchEndpoint")
                                    ?.get("videoId")?.asString
                            } else null

                        if (cardVideoId != null && list.none { it.id == cardVideoId }) {
                            val thumbnails = cardShelf.getAsJsonObject("thumbnail")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")
                            val rawCoverUrl = thumbnails?.lastOrNull()?.asJsonObject?.get("url")?.asString
                            val coverUrl = ArtworkUtils.getHighResArtworkUrl(rawCoverUrl)

                            list.add(
                                Track(
                                    id = cardVideoId,
                                    title = title,
                                    artists = listOf(Artist(name = artistName)),
                                    durationSeconds = 210,
                                    coverUrl = coverUrl,
                                    source = "youtube",
                                    youtubeId = cardVideoId
                                )
                            )
                        }
                    }

                    // Parse Standard Song Shelf Items
                    val itemSection = sectionObj.getAsJsonObject("musicShelfRenderer") ?: continue
                    val items = itemSection.getAsJsonArray("contents") ?: continue
                    for (item in items) {
                        val mfr = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue
                        val flexColumns = mfr.getAsJsonArray("flexColumns") ?: continue
                        if (flexColumns.size() < 2) continue

                        val titleCol = flexColumns[0].asJsonObject
                            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")?.get(0)?.asJsonObject
                        val title = titleCol?.get("text")?.asString ?: "Unknown Track"

                        val artistCol = flexColumns[1].asJsonObject
                            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")?.get(0)?.asJsonObject
                        val artistName = artistCol?.get("text")?.asString ?: "Unknown Artist"

                        val videoId = mfr.getAsJsonObject("playlistItemData")?.get("videoId")?.asString
                            ?: mfr.getAsJsonObject("overlay")
                                ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                                ?.getAsJsonObject("content")
                                ?.getAsJsonObject("musicPlayButtonRenderer")
                                ?.getAsJsonObject("playNavigationEndpoint")
                                ?.getAsJsonObject("watchEndpoint")
                                ?.get("videoId")?.asString

                        if (videoId != null && list.none { it.id == videoId }) {
                            val thumbnails = mfr.getAsJsonObject("thumbnail")
                                ?.getAsJsonObject("musicThumbnailRenderer")
                                ?.getAsJsonObject("thumbnail")
                                ?.getAsJsonArray("thumbnails")
                            val rawCoverUrl = thumbnails?.lastOrNull()?.asJsonObject?.get("url")?.asString
                            val coverUrl = ArtworkUtils.getHighResArtworkUrl(rawCoverUrl)

                            list.add(
                                Track(
                                    id = videoId,
                                    title = title,
                                    artists = listOf(Artist(name = artistName)),
                                    durationSeconds = 210,
                                    coverUrl = coverUrl,
                                    source = "youtube",
                                    youtubeId = videoId
                                )
                            )
                            if (list.size >= limit) return
                        }
                    }
                }
            }

            // Regex Fallback if structured parsing yields no items
            if (list.isEmpty()) {
                val vidPattern = java.util.regex.Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"")
                val matcher = vidPattern.matcher(json.toString())
                val seen = mutableSetOf<String>()
                while (matcher.find() && list.size < limit) {
                    val vid = matcher.group(1) ?: continue
                    if (seen.add(vid)) {
                        list.add(
                            Track(
                                id = vid,
                                title = "Track",
                                artists = listOf(Artist(name = "YouTube Music")),
                                durationSeconds = 210,
                                source = "youtube",
                                youtubeId = vid
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMockSearchResults(query: String): List<Track> {
        return listOf(
            Track("J7p4bzqLvCw", "$query (Official Audio)", listOf(Artist(name = "Featured Artist")), durationSeconds = 215, coverUrl = "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36", youtubeId = "J7p4bzqLvCw"),
            Track("4NRXx6U8ABQ", "$query - Acoustic Version", listOf(Artist(name = "Various Artists")), durationSeconds = 198, coverUrl = "https://i.scdn.co/image/ab67616d0000b273b749622cf3977e2d97adba4a", youtubeId = "4NRXx6U8ABQ"),
            Track("hT_nvWreIhg", "$query (Remix)", listOf(Artist(name = "Openfy Mix")), durationSeconds = 240, coverUrl = "https://i.scdn.co/image/ab67616d0000b273ba5db46f4b838ef6027e6f96", youtubeId = "hT_nvWreIhg")
        )
    }
}
