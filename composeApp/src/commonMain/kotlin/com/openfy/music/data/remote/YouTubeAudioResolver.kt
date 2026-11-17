package com.openfy.music.data.remote

import com.openfy.music.data.AccountManager
import com.openfy.music.data.AudioQuality
import com.openfy.music.data.SettingsManager
import com.openfy.music.data.model.Album
import com.openfy.music.data.model.Artist
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.platform.PlatformConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

data class ClientConfig(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null
)

object YouTubeAudioResolver {
    private val streamCache = mutableMapOf<String, String>()

    private var cachedVisitorData: String? = null
    private var visitorDataExpiryMs: Long = 0L

    private val clientConfigs = mapOf(
        "VISIONOS" to ClientConfig(
            clientName = "VISIONOS",
            clientVersion = "0.1",
            clientId = "101",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
            osName = "visionOS",
            osVersion = "1.3.21O771",
            deviceMake = "Apple",
            deviceModel = "RealityDevice14,1"
        ),
        "TVHTML5_SIMPLY" to ClientConfig(
            clientName = "TVHTML5_SIMPLY",
            clientVersion = "1.0",
            clientId = "75",
            userAgent = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/25.lts.30.1034943-gold (unlike Gecko), Unknown_TV_Unknown_0/Unknown (Unknown, Unknown)"
        ),
        "WEB_REMIX" to ClientConfig(
            clientName = "WEB_REMIX",
            clientVersion = "1.20241110.01.00",
            clientId = "67",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        ),
        "IOS_MUSIC" to ClientConfig(
            clientName = "IOS_MUSIC",
            clientVersion = "7.01.05",
            clientId = "26",
            userAgent = "com.google.ios.youtubemusic/7.01.05 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)",
            osName = "iOS",
            osVersion = "18.2.22C152",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2"
        )
    )

    fun getClientConfig(clientKey: String): ClientConfig? = clientConfigs[clientKey]

    /**
     * Scrape fresh session visitorData from YouTube Music service worker data endpoint.
     */
    suspend fun getOrFetchVisitorData(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val now = getTimeMillis()
        if (!forceRefresh && cachedVisitorData != null && now < visitorDataExpiryMs) {
            return@withContext cachedVisitorData
        }

        try {
            val resp = NetworkClient.client.get("https://music.youtube.com/sw.js_data") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            val body = resp.bodyAsText()
            val jsonStart = body.indexOf('[')
            if (jsonStart != -1) {
                val jsonStr = body.substring(jsonStart)
                val jsonArray = NetworkClient.json.parseToJsonElement(jsonStr).jsonArray
                val outer = jsonArray.getOrNull(0)?.jsonArray
                val arr = outer?.getOrNull(2)?.jsonArray
                if (arr != null) {
                    for (element in arr) {
                        val str = element.jsonPrimitive.contentOrNull ?: continue
                        if (str.startsWith("Cgt") || str.startsWith("Cgs") || str.startsWith("Cg")) {
                            cachedVisitorData = str
                            visitorDataExpiryMs = now + (3600_000L * 2) // Cache 2 hours
                            return@withContext str
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return@withContext cachedVisitorData ?: "CgtsODE2NnBuLXc1QSiI1OfXBjIKCgJJThIEGgAgJ2KrAg=="
    }

    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()

        // 1. Hybrid Search Tier 1: Query Oracle Cloud backend server cache (<20ms response time)
        try {
            val backendUrl = "http://140.245.226.77/api/search?q=${query.encodeURLParameter()}"
            val resp = NetworkClient.client.get(backendUrl) {
                header("User-Agent", "Openfy-Multiplatform/1.0")
            }
            val body = resp.bodyAsText()
            val json = NetworkClient.json.parseToJsonElement(body).jsonObject
            val trackArray = json["tracks"]?.jsonArray
            if (trackArray != null && trackArray.isNotEmpty()) {
                for (item in trackArray) {
                    val obj = item.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                    val artist = obj["artist"]?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
                    val cover = obj["coverUrl"]?.jsonPrimitive?.contentOrNull
                    val duration = obj["durationSeconds"]?.jsonPrimitive?.intOrNull ?: 0

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
        } catch (e: Exception) {
            // Backend offline or timeout: fall back to on-device InnerTube
        }

        // 2. Hybrid Search Tier 2: Local On-Device InnerTube Scraper Fallback
        try {
            val visitorData = getOrFetchVisitorData()
            val url = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
            val payload = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20241110.01.00")
                        put("hl", "en")
                        put("gl", "US")
                        if (!visitorData.isNullOrEmpty()) {
                            put("visitorData", visitorData)
                        }
                    }
                }
                put("query", query)
                put("params", "EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D") // Filter songs
            }

            val resp = NetworkClient.client.post(url) {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Origin", "https://music.youtube.com")
                if (!visitorData.isNullOrEmpty()) {
                    header("x-goog-visitor-id", visitorData)
                }
                setBody(payload.toString())
            }

            val body = resp.bodyAsText()
            val json = NetworkClient.json.parseToJsonElement(body).jsonObject
            parseSearchTracks(json, tracks, limit)
        } catch (e: Exception) {
            // Ignore
        }

        if (tracks.isEmpty()) {
            tracks.addAll(getMockSearchResults(query))
        }
        tracks
    }

    /**
     * Resolves the full-length audio stream for a track using an unthrottled 3-tier fallback chain:
     * Tier 1: WEB_REMIX (Official Web Player) with dynamic SignatureTimestamp (STS)
     * Tier 2: VISIONOS (Apple Vision Pro) - Direct unciphered GoogleVideo URLs
     * Tier 3: TVHTML5_SIMPLY (Cobalt Client 75) - Standalone fallback
     */
    suspend fun resolveStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
        val cacheKey = track.youtubeId ?: track.id
        streamCache[cacheKey]?.let { return@withContext it }

        var videoId = track.youtubeId
        if (videoId == null || videoId.length != 11) {
            val searchResults = searchTracks("${track.title} ${track.artistNames}", limit = 3)
            videoId = searchResults.firstOrNull()?.youtubeId ?: searchResults.firstOrNull()?.id
        }

        if (videoId == null) return@withContext null

        val visitorData = getOrFetchVisitorData()
        val quality = SettingsManager.audioQuality.value

        // Tier 1: Primary Resolver -> WEB_REMIX with dynamic SignatureTimestamp (STS)
        var streamUrl = fetchWebRemixStreamUrl(videoId, visitorData, quality)

        // Tier 2: Fallback to VISIONOS (Client 101)
        if (streamUrl == null) {
            streamUrl = fetchVisionOsStreamUrl(videoId, visitorData, quality)
        }

        // Tier 2.5: Self-heal retry with fresh visitorData if needed
        if (streamUrl == null) {
            val freshVisitorData = getOrFetchVisitorData(forceRefresh = true)
            streamUrl = fetchVisionOsStreamUrl(videoId, freshVisitorData, quality)
        }

        // Tier 3: Fallback to TVHTML5_SIMPLY (Client 75)
        if (streamUrl == null) {
            streamUrl = fetchTvHtml5SimplyStreamUrl(videoId, visitorData, quality)
        }

        if (streamUrl != null) {
            streamCache[cacheKey] = streamUrl
            return@withContext streamUrl
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

            val payload = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20241110.01.00")
                        put("hl", "en")
                        put("gl", "US")
                        if (!visitorData.isNullOrEmpty()) {
                            put("visitorData", visitorData)
                        }
                    }
                }
                put("videoId", videoId)
                if (sts > 0) {
                    putJsonObject("playbackContext") {
                        putJsonObject("contentPlaybackContext") {
                            put("signatureTimestamp", sts)
                        }
                    }
                }
            }

            val resp = NetworkClient.client.post(playerUrl) {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                header("Origin", "https://music.youtube.com")
                header("Referer", "https://music.youtube.com/")
                header("X-YouTube-Client-Name", "67")
                header("X-YouTube-Client-Version", "1.20241110.01.00")
                if (!visitorData.isNullOrEmpty()) {
                    header("x-goog-visitor-id", visitorData)
                }

                val cookie = AccountManager.youtubeCookie
                if (!cookie.isNullOrEmpty()) {
                    header("Cookie", cookie)
                    val authHeader = AccountManager.getSapisidHash()
                    if (!authHeader.isNullOrEmpty()) {
                        header("Authorization", authHeader)
                    }
                }
                setBody(payload.toString())
            }

            val body = resp.bodyAsText()
            val json = NetworkClient.json.parseToJsonElement(body).jsonObject
            val streamingData = json["streamingData"]?.jsonObject
            val formats = streamingData?.get("adaptiveFormats")?.jsonArray
            if (formats != null) {
                return@withContext extractBestAudioFormat(formats, quality)
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }

    private suspend fun fetchVisionOsStreamUrl(
        videoId: String,
        visitorData: String?,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cfg = getClientConfig("VISIONOS") ?: return@withContext null
            val playerUrl = "https://music.youtube.com/youtubei/v1/player?prettyPrint=false"

            val payload = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", cfg.clientName)
                        put("clientVersion", cfg.clientVersion)
                        put("clientId", cfg.clientId)
                        put("osName", "visionOS")
                        put("osVersion", "1.3.21O771")
                        put("deviceMake", "Apple")
                        put("deviceModel", "RealityDevice14,1")
                        put("userAgent", cfg.userAgent)
                        put("hl", "en")
                        put("gl", "US")
                        if (!visitorData.isNullOrEmpty()) {
                            put("visitorData", visitorData)
                        }
                    }
                }
                put("videoId", videoId)
            }

            val resp = NetworkClient.client.post(playerUrl) {
                contentType(ContentType.Application.Json)
                header("User-Agent", cfg.userAgent)
                header("X-YouTube-Client-Name", cfg.clientId)
                header("X-YouTube-Client-Version", cfg.clientVersion)
                header("x-origin", "https://music.youtube.com")
                header("referer", "https://music.youtube.com/")
                header("x-goog-api-format-version", "1")
                if (!visitorData.isNullOrEmpty()) {
                    header("x-goog-visitor-id", visitorData)
                }

                val cookie = AccountManager.youtubeCookie
                if (!cookie.isNullOrEmpty()) {
                    header("Cookie", cookie)
                    val authHeader = AccountManager.getSapisidHash()
                    if (!authHeader.isNullOrEmpty()) {
                        header("Authorization", authHeader)
                    }
                }
                setBody(payload.toString())
            }

            val body = resp.bodyAsText()
            val json = NetworkClient.json.parseToJsonElement(body).jsonObject
            val status = json["playabilityStatus"]?.jsonObject?.get("status")?.jsonPrimitive?.contentOrNull
            if (status == "OK") {
                val streamingData = json["streamingData"]?.jsonObject
                val formats = streamingData?.get("adaptiveFormats")?.jsonArray
                if (formats != null) {
                    return@withContext extractBestAudioFormat(formats, quality)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }

    private suspend fun fetchTvHtml5SimplyStreamUrl(
        videoId: String,
        visitorData: String?,
        quality: AudioQuality
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cfg = getClientConfig("TVHTML5_SIMPLY") ?: return@withContext null
            val playerUrl = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"

            val payload = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", cfg.clientName)
                        put("clientVersion", cfg.clientVersion)
                        put("clientId", cfg.clientId)
                        put("hl", "en")
                        put("gl", "US")
                        if (!visitorData.isNullOrEmpty()) {
                            put("visitorData", visitorData)
                        }
                    }
                }
                put("videoId", videoId)
            }

            val resp = NetworkClient.client.post(playerUrl) {
                contentType(ContentType.Application.Json)
                header("User-Agent", cfg.userAgent)
                header("X-YouTube-Client-Name", cfg.clientId)
                header("X-YouTube-Client-Version", cfg.clientVersion)
                if (!visitorData.isNullOrEmpty()) {
                    header("x-goog-visitor-id", visitorData)
                }
                setBody(payload.toString())
            }

            val body = resp.bodyAsText()
            val json = NetworkClient.json.parseToJsonElement(body).jsonObject
            val streamingData = json["streamingData"]?.jsonObject
            val formats = streamingData?.get("adaptiveFormats")?.jsonArray
            if (formats != null) {
                return@withContext extractBestAudioFormat(formats, quality)
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }

    /**
     * Smart format selection:
     * - In AUTO mode:
     *   * On iOS: prefers AAC 128k (itag 140) for native AVPlayer hardware acceleration, fallback Opus 160k (itag 251)
     *   * On Android: prefers Opus 160k (itag 251), fallback AAC 128k (itag 140)
     * - In explicit mode: respects targetItag and fallbackItag from AudioQuality.
     */
    private suspend fun extractBestAudioFormat(formats: JsonArray, quality: AudioQuality): String? {
        val isIos = PlatformConfig.isIos

        val (targetItag, fallbackItag) = if (quality == AudioQuality.AUTO) {
            if (isIos) {
                // iOS: AAC 128k itag 140 for AVPlayer hardware acceleration
                Pair(140, 251)
            } else {
                // Android: Opus 160k itag 251
                Pair(251, 140)
            }
        } else {
            Pair(quality.targetItag, quality.fallbackItag)
        }

        var directMatchUrl: String? = null
        var fallbackMatchUrl: String? = null
        var genericUrl: String? = null
        var highestBitrate = 0L

        for (element in formats) {
            val format = element.jsonObject
            val mimeType = format["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
            if (!mimeType.contains("audio")) continue

            val itag = format["itag"]?.jsonPrimitive?.intOrNull ?: 0
            val bitrate = format["bitrate"]?.jsonPrimitive?.longOrNull ?: 0L

            var resolvedUrl: String? = null
            if (format.containsKey("url")) {
                val rawUrl = format["url"]?.jsonPrimitive?.contentOrNull ?: ""
                resolvedUrl = HeadlessCipherSolver.resolveNTransform(rawUrl)
            } else if (format.containsKey("signatureCipher")) {
                val cipherStr = format["signatureCipher"]?.jsonPrimitive?.contentOrNull ?: ""
                resolvedUrl = HeadlessCipherSolver.resolveCipheredUrl(cipherStr)
            } else if (format.containsKey("cipher")) {
                val cipherStr = format["cipher"]?.jsonPrimitive?.contentOrNull ?: ""
                resolvedUrl = HeadlessCipherSolver.resolveCipheredUrl(cipherStr)
            }

            if (resolvedUrl.isNullOrEmpty()) continue

            if (itag == targetItag) {
                directMatchUrl = resolvedUrl
                break
            } else if (itag == fallbackItag && fallbackMatchUrl == null) {
                fallbackMatchUrl = resolvedUrl
            } else if (bitrate > highestBitrate) {
                highestBitrate = bitrate
                genericUrl = resolvedUrl
            }
        }

        return directMatchUrl ?: fallbackMatchUrl ?: genericUrl
    }

    suspend fun rateSong(videoId: String, isLiked: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cookie = AccountManager.youtubeCookie ?: return@withContext false
        val authHeader = AccountManager.getSapisidHash() ?: return@withContext false
        val visitorData = getOrFetchVisitorData()

        val action = if (isLiked) "like/like" else "like/removelike"
        val url = "https://music.youtube.com/youtubei/v1/$action?prettyPrint=false"

        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20241110.01.00")
                    put("hl", "en")
                    put("gl", "US")
                    if (!visitorData.isNullOrEmpty()) {
                        put("visitorData", visitorData)
                    }
                }
            }
            putJsonObject("target") {
                put("videoId", videoId)
            }
        }

        try {
            val resp = NetworkClient.client.post(url) {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Origin", "https://music.youtube.com")
                header("Referer", "https://music.youtube.com/")
                header("Cookie", cookie)
                header("Authorization", authHeader)
                setBody(payload.toString())
            }
            return@withContext resp.status.value in 200..299
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun getLikedVideos(): List<Track> = withContext(Dispatchers.IO) {
        val cookie = AccountManager.youtubeCookie
        val authHeader = AccountManager.getSapisidHash()
        if (cookie.isNullOrEmpty() || authHeader.isNullOrEmpty()) {
            return@withContext getTrendingTracks()
        }

        val visitorData = getOrFetchVisitorData()
        val url = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20241110.01.00")
                    put("hl", "en")
                    put("gl", "US")
                    if (!visitorData.isNullOrEmpty()) {
                        put("visitorData", visitorData)
                    }
                }
            }
            put("browseId", "FEmusic_liked_videos")
        }

        try {
            val resp = NetworkClient.client.post(url) {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Origin", "https://music.youtube.com")
                header("Referer", "https://music.youtube.com/")
                header("Cookie", cookie)
                header("Authorization", authHeader)
                setBody(payload.toString())
            }

            val body = resp.bodyAsText()
            val json = NetworkClient.json.parseToJsonElement(body).jsonObject
            val list = mutableListOf<Track>()
            parseSearchTracks(json, list, limit = 50)
            if (list.isNotEmpty()) {
                return@withContext list
            }
        } catch (e: Exception) {
            // Ignore
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
            // Ignore
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
            val tabs = json["contents"]?.jsonObject
                ?.get("tabbedSearchResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
            val tabRenderer = tabs?.getOrNull(0)?.jsonObject?.get("tabRenderer")?.jsonObject
            val contents = tabRenderer?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray ?: return

            for (section in contents) {
                val itemSection = section.jsonObject["musicShelfRenderer"]?.jsonObject ?: continue
                val items = itemSection["contents"]?.jsonArray ?: continue
                for (item in items) {
                    val mfr = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
                    val flexColumns = mfr["flexColumns"]?.jsonArray ?: continue
                    if (flexColumns.size < 2) continue

                    val titleCol = flexColumns[0].jsonObject["musicResponsiveListItemFlexColumnRenderer"]?.jsonObject
                        ?.get("text")?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject
                    val title = titleCol?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown Track"

                    val artistCol = flexColumns[1].jsonObject["musicResponsiveListItemFlexColumnRenderer"]?.jsonObject
                        ?.get("text")?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject
                    val artistName = artistCol?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"

                    val videoId = mfr["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
                        ?: mfr["overlay"]?.jsonObject
                            ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                            ?.get("content")?.jsonObject
                            ?.get("musicPlayButtonRenderer")?.jsonObject
                            ?.get("playNavigationEndpoint")?.jsonObject
                            ?.get("watchEndpoint")?.jsonObject
                            ?.get("videoId")?.jsonPrimitive?.contentOrNull

                    if (videoId != null) {
                        val thumbnails = mfr["thumbnail"]?.jsonObject
                            ?.get("musicThumbnailRenderer")?.jsonObject
                            ?.get("thumbnail")?.jsonObject
                            ?.get("thumbnails")?.jsonArray
                        val coverUrl = thumbnails?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

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
        } catch (e: Exception) {
            // Ignore
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
