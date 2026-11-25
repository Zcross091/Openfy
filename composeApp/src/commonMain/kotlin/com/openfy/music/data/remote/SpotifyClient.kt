package com.openfy.music.data.remote

import com.openfy.music.data.SettingsManager
import com.openfy.music.data.TrackIdMapper
import com.openfy.music.data.model.Album
import com.openfy.music.data.model.Artist
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.model.UserProfile
import com.openfy.music.util.ArtworkUtils
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

object PureSha1 {
    private fun rotateLeft(n: Int, bits: Int): Int = (n shl bits) or (n ushr (32 - bits))

    fun sha1(bytes: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = -0x10325477
        var h2 = -0x67452302
        var h3 = 0x10325476
        var h4 = -0x3C2D1E10

        val msgLenBits = bytes.size.toLong() * 8
        val paddingLength = if (bytes.size % 64 < 56) 56 - (bytes.size % 64) else 120 - (bytes.size % 64)
        val padded = ByteArray(bytes.size + paddingLength + 8)
        bytes.copyInto(padded)
        padded[bytes.size] = 0x80.toByte()

        for (i in 0 until 8) {
            padded[padded.size - 1 - i] = ((msgLenBits ushr (i * 8)) and 0xFF).toByte()
        }

        val w = IntArray(80)
        for (chunk in padded.indices step 64) {
            for (i in 0 until 16) {
                val j = chunk + i * 4
                w[i] = ((padded[j].toInt() and 0xFF) shl 24) or
                        ((padded[j + 1].toInt() and 0xFF) shl 16) or
                        ((padded[j + 2].toInt() and 0xFF) shl 8) or
                        (padded[j + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) {
                w[i] = rotateLeft(w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16], 1)
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4

            for (i in 0 until 80) {
                val (f, k) = when (i) {
                    in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                    in 20..39 -> (b xor c xor d) to 0x6ED9EBA1
                    in 40..59 -> ((b and c) or (b and d) or (c and d)) to -0x70E44324
                    else -> (b xor c xor d) to -0x359D3E2A
                }
                val temp = rotateLeft(a, 5) + f + e + k + w[i]
                e = d
                d = c
                c = rotateLeft(b, 30)
                b = a
                a = temp
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
        }

        val result = ByteArray(20)
        for (i in 0 until 4) {
            result[i] = ((h0 ushr (24 - i * 8)) and 0xFF).toByte()
            result[i + 4] = ((h1 ushr (24 - i * 8)) and 0xFF).toByte()
            result[i + 8] = ((h2 ushr (24 - i * 8)) and 0xFF).toByte()
            result[i + 12] = ((h3 ushr (24 - i * 8)) and 0xFF).toByte()
            result[i + 16] = ((h4 ushr (24 - i * 8)) and 0xFF).toByte()
        }
        return result
    }

    fun hmacSha1(key: ByteArray, message: ByteArray): ByteArray {
        val blockSize = 64
        val paddedKey = ByteArray(blockSize)
        if (key.size > blockSize) {
            val hashed = sha1(key)
            hashed.copyInto(paddedKey)
        } else {
            key.copyInto(paddedKey)
        }

        val oKeyPad = ByteArray(blockSize) { (paddedKey[it].toInt() xor 0x5C).toByte() }
        val iKeyPad = ByteArray(blockSize) { (paddedKey[it].toInt() xor 0x36).toByte() }

        val inner = sha1(iKeyPad + message)
        return sha1(oKeyPad + inner)
    }
}

object SpotifyTotp {
    private const val RAW_SECRET = ",7/*F(\"rLJ2oxaKL^f+E1xvP@N"
    const val VERSION = 61

    private val keyBytes by lazy {
        val rStr = buildString {
            RAW_SECRET.forEachIndexed { idx, c ->
                append(c.code xor (idx % 33 + 9))
            }
        }
        rStr.encodeToByteArray()
    }

    fun generateTotp(timestampMs: Long = getTimeMillis()): String {
        val counter = timestampMs / 1000 / 30
        val buffer = ByteArray(8)
        for (i in 0 until 8) {
            buffer[7 - i] = ((counter ushr (i * 8)) and 0xFF).toByte()
        }
        val hash = PureSha1.hmacSha1(keyBytes, buffer)
        val offset = (hash.last().toInt() and 0x0F)
        val code = ((hash[offset].toInt() and 0x7F) shl 24) or
                   ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                   ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                   (hash[offset + 3].toInt() and 0xFF)
        val otp = (code % 1000000).toString().padStart(6, '0')
        return otp
    }
}

object SpotifyClient {
    private val settings get() = SettingsManager.settings

    var accessToken: String?
        get() = settings.getStringOrNull("spotify_access_token")
        set(value) {
            if (value != null) settings.putString("spotify_access_token", value)
            else settings.remove("spotify_access_token")
        }

    var spDcCookie: String?
        get() = settings.getStringOrNull("spotify_sp_dc")
        set(value) {
            if (value != null) settings.putString("spotify_sp_dc", value)
            else settings.remove("spotify_sp_dc")
        }

    var tokenExpiryMs: Long
        get() = settings.getLong("spotify_token_expiry", 0L)
        set(value) = settings.putLong("spotify_token_expiry", value)

    var userProfile: UserProfile? = null
    private var anonymousAccessToken: String? = null
    private var anonymousTokenExpiryMs: Long = 0L

    val isConnected: Boolean
        get() = !accessToken.isNullOrEmpty() || !spDcCookie.isNullOrEmpty()

    /**
     * Exchanges a persistent Spotify web session cookie (sp_dc) for an official Web Player Bearer Access Token.
     */
    suspend fun exchangeCookieForAccessToken(spDc: String): Boolean = withContext(Dispatchers.IO) {
        val cleanCookie = spDc.trim().removePrefix("sp_dc=").trim('"', '\'', ';')
        if (cleanCookie.isEmpty()) return@withContext false

        try {
            val totp = SpotifyTotp.generateTotp()
            val url = "https://open.spotify.com/api/token?reason=init&productType=web-player&totp=$totp&totpServer=unavailable&totpVer=${SpotifyTotp.VERSION}"
            val resp = NetworkClient.client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                header("Cookie", "sp_dc=$cleanCookie")
                header("Referer", "https://open.spotify.com/")
                header("Accept", "application/json")
            }

            if (resp.status.value in 200..299) {
                val body = resp.bodyAsText()
                val json = NetworkClient.json.parseToJsonElement(body).jsonObject
                val token = json["accessToken"]?.jsonPrimitive?.contentOrNull
                val isAnon = json["isAnonymous"]?.jsonPrimitive?.booleanOrNull ?: false
                val expiry = json["accessTokenExpirationTimestampMs"]?.jsonPrimitive?.longOrNull
                    ?: (getTimeMillis() + 3600_000L)

                if (!token.isNullOrEmpty() && !isAnon) {
                    spDcCookie = cleanCookie
                    accessToken = token
                    tokenExpiryMs = expiry
                    lastError = null
                    fetchProfile()
                    return@withContext true
                } else {
                    lastError = "Spotify session cookie expired or returned anonymous session"
                }
            } else {
                lastError = "Spotify token exchange HTTP ${resp.status.value}"
            }
        } catch (e: Exception) {
            lastError = "Spotify token exchange failed: ${e.message}"
            println("[SpotifyClient] exchangeCookieForAccessToken failed: ${e.message}")
        }
        false
    }

    suspend fun fetchAnonymousAccessToken(): String? = withContext(Dispatchers.IO) {
        val now = getTimeMillis()
        if (!anonymousAccessToken.isNullOrEmpty() && now < (anonymousTokenExpiryMs - 60_000L)) {
            return@withContext anonymousAccessToken
        }
        try {
            val totp = SpotifyTotp.generateTotp()
            val url = "https://open.spotify.com/api/token?reason=init&productType=web-player&totp=$totp&totpServer=unavailable&totpVer=${SpotifyTotp.VERSION}"
            val resp = NetworkClient.client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                header("Referer", "https://open.spotify.com/")
                header("Accept", "application/json")
            }

            if (resp.status.value in 200..299) {
                val body = resp.bodyAsText()
                val json = NetworkClient.json.parseToJsonElement(body).jsonObject
                val token = json["accessToken"]?.jsonPrimitive?.contentOrNull
                val expiry = json["accessTokenExpirationTimestampMs"]?.jsonPrimitive?.longOrNull
                    ?: (getTimeMillis() + 3600_000L)

                if (!token.isNullOrEmpty()) {
                    anonymousAccessToken = token
                    anonymousTokenExpiryMs = expiry
                    return@withContext token
                }
            }
        } catch (e: Exception) {
            println("[SpotifyClient] fetchAnonymousAccessToken failed: ${e.message}")
        }
        null
    }

    /** Returns a valid user-authenticated token; never promotes an anonymous token to [accessToken]. */
    suspend fun ensureFreshToken(): String? = withContext(Dispatchers.IO) {
        val now = getTimeMillis()
        if (!accessToken.isNullOrEmpty() && now < (tokenExpiryMs - 60_000L)) {
            return@withContext accessToken
        }

        val cookie = spDcCookie
        if (!cookie.isNullOrEmpty()) {
            if (exchangeCookieForAccessToken(cookie)) {
                return@withContext accessToken
            }
        }

        accessToken
    }

    /** Returns either a user token or an anonymous public catalog token (for search without login). */
    suspend fun ensurePublicToken(): String? = ensureFreshToken() ?: fetchAnonymousAccessToken()

    private fun extractHighestResSpotifyImage(images: JsonArray?): String? {
        if (images == null || images.isEmpty()) return null
        var bestUrl: String? = null
        var maxPixels = -1
        for (element in images) {
            val obj = element.jsonObject
            val width = obj["width"]?.jsonPrimitive?.intOrNull ?: 0
            val height = obj["height"]?.jsonPrimitive?.intOrNull ?: 0
            val pixels = width * height
            if (pixels > maxPixels || bestUrl == null) {
                maxPixels = pixels
                bestUrl = obj["url"]?.jsonPrimitive?.contentOrNull
            }
        }
        return ArtworkUtils.getHighResArtworkUrl(bestUrl)
    }

    var lastError: String? = null

    internal fun parseTrackObject(t: JsonObject, isLiked: Boolean = false): Track? {
        val id = t["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = t["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown"

        val artistsList = mutableListOf<Artist>()
        t["artists"]?.jsonArray?.forEach {
            val a = it.jsonObject
            artistsList.add(Artist(a["id"]?.jsonPrimitive?.contentOrNull, a["name"]?.jsonPrimitive?.contentOrNull ?: ""))
        }

        val albumObj = t["album"]?.jsonObject
        val albumName = albumObj?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
        val albumImages = albumObj?.get("images")?.jsonArray
        val cover = extractHighestResSpotifyImage(albumImages)
        val album = Album(name = albumName, coverUrl = cover)
        val duration = (t["duration_ms"]?.jsonPrimitive?.intOrNull ?: 0) / 1000
        val isrc = t["external_ids"]?.jsonObject?.get("isrc")?.jsonPrimitive?.contentOrNull

        val primaryArtist = artistsList.firstOrNull()?.name.orEmpty()
        TrackIdMapper.putSpotify(id, isrc, title, primaryArtist)

        return Track(
            id = id,
            title = title,
            artists = artistsList,
            album = album,
            durationSeconds = duration,
            coverUrl = cover,
            source = "spotify",
            spotifyId = id,
            isrc = isrc,
            isLiked = isLiked
        )
    }

    internal fun parseSavedTracksPage(json: JsonObject, isLiked: Boolean = true): List<Track> {
        val items = json["items"]?.jsonArray ?: return emptyList()
        return items.mapNotNull { item ->
            val trackObj = item.jsonObject["track"]?.jsonObject ?: return@mapNotNull null
            parseTrackObject(trackObj, isLiked = isLiked)
        }
    }

    internal fun parsePlaylistsPage(json: JsonObject): List<Playlist> {
        val items = json["items"]?.jsonArray ?: return emptyList()
        return items.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val title = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Untitled Playlist"
            val desc = obj["description"]?.jsonPrimitive?.contentOrNull
            val images = obj["images"]?.jsonArray
            val cover = extractHighestResSpotifyImage(images)
            val count = obj["tracks"]?.jsonObject?.get("total")?.jsonPrimitive?.intOrNull ?: 0

            Playlist(
                id = id,
                title = title,
                description = desc,
                coverUrl = cover,
                trackCount = count,
                source = "spotify"
            )
        }
    }

    suspend fun fetchProfile(): UserProfile? = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext null
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/me") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val id = json["id"]?.jsonPrimitive?.contentOrNull ?: "spotify_user"
                val name = json["display_name"]?.jsonPrimitive?.contentOrNull ?: "Spotify User"
                val images = json["images"]?.jsonArray
                val avatar = extractHighestResSpotifyImage(images)

                val profile = UserProfile(id, name, avatarUrl = avatar)
                userProfile = profile
                lastError = null
                return@withContext profile
            } else {
                lastError = "Spotify profile HTTP ${resp.status.value}"
            }
        } catch (e: Exception) {
            lastError = "Spotify profile error: ${e.message}"
            println("[SpotifyClient] fetchProfile failed: ${e.message}")
        }
        null
    }

    suspend fun getLikedTracks(limit: Int = 50, offset: Int = 0): List<Track> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/tracks?limit=$limit&offset=$offset") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                lastError = null
                return@withContext parseSavedTracksPage(json, isLiked = true)
            } else {
                lastError = "Spotify liked tracks HTTP ${resp.status.value}"
                println("[SpotifyClient] getLikedTracks HTTP ${resp.status.value}")
            }
        } catch (e: Exception) {
            lastError = "Spotify liked tracks error: ${e.message}"
            println("[SpotifyClient] getLikedTracks failed: ${e.message}")
        }
        emptyList()
    }

    suspend fun getAllLikedTracksProgressive(
        onBatchLoaded: (List<Track>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext
        val allTracks = mutableListOf<Track>()
        var offset = 0
        val pageSize = 50
        var total = Int.MAX_VALUE

        while (offset < total) {
            try {
                val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/tracks?limit=$pageSize&offset=$offset") {
                    header("Authorization", "Bearer $token")
                }
                if (resp.status.value !in 200..299) {
                    lastError = "Spotify liked tracks page HTTP ${resp.status.value}"
                    break
                }
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                total = json["total"]?.jsonPrimitive?.intOrNull ?: 0
                val batch = parseSavedTracksPage(json, isLiked = true)
                if (batch.isEmpty()) break

                allTracks.addAll(batch)
                onBatchLoaded(allTracks.toList())
                offset += pageSize
            } catch (e: Exception) {
                lastError = "Spotify liked tracks pagination failed: ${e.message}"
                println("[SpotifyClient] getAllLikedTracksProgressive failed: ${e.message}")
                break
            }
        }
    }

    suspend fun getUserPlaylists(limit: Int = 50, offset: Int = 0): List<Playlist> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/playlists?limit=$limit&offset=$offset") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                lastError = null
                return@withContext parsePlaylistsPage(json)
            } else {
                lastError = "Spotify playlists HTTP ${resp.status.value}"
                println("[SpotifyClient] getUserPlaylists HTTP ${resp.status.value}")
            }
        } catch (e: Exception) {
            lastError = "Spotify playlists error: ${e.message}"
            println("[SpotifyClient] getUserPlaylists failed: ${e.message}")
        }
        emptyList()
    }

    suspend fun getAllUserPlaylistsProgressive(
        onBatchLoaded: (List<Playlist>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext
        val allPlaylists = mutableListOf<Playlist>()
        var offset = 0
        val pageSize = 50
        var total = Int.MAX_VALUE

        while (offset < total) {
            try {
                val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/playlists?limit=$pageSize&offset=$offset") {
                    header("Authorization", "Bearer $token")
                }
                if (resp.status.value !in 200..299) {
                    lastError = "Spotify playlists page HTTP ${resp.status.value}"
                    break
                }
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                total = json["total"]?.jsonPrimitive?.intOrNull ?: 0
                val batch = parsePlaylistsPage(json)
                if (batch.isEmpty()) break

                allPlaylists.addAll(batch)
                onBatchLoaded(allPlaylists.toList())
                offset += pageSize
            } catch (e: Exception) {
                lastError = "Spotify playlists pagination failed: ${e.message}"
                println("[SpotifyClient] getAllUserPlaylistsProgressive failed: ${e.message}")
                break
            }
        }
    }

    suspend fun getFollowedArtists(limit: Int = 50): List<Artist> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        val artists = mutableListOf<Artist>()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/following?type=artist&limit=$limit") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val items = json["artists"]?.jsonObject?.get("items")?.jsonArray ?: return@withContext emptyList()
                for (item in items) {
                    val obj = item.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
                    val images = obj["images"]?.jsonArray
                    val avatar = extractHighestResSpotifyImage(images)
                    val followers = obj["followers"]?.jsonObject?.get("total")?.jsonPrimitive?.intOrNull ?: 0
                    artists.add(Artist(id = id, name = name, avatarUrl = avatar, followers = followers))
                }
            }
        } catch (e: Exception) {
            println("[SpotifyClient] getFollowedArtists failed: ${e.message}")
        }
        artists
    }

    suspend fun getSavedAlbums(limit: Int = 50, offset: Int = 0): List<Album> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        val albums = mutableListOf<Album>()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/albums?limit=$limit&offset=$offset") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val items = json["items"]?.jsonArray ?: return@withContext emptyList()
                for (item in items) {
                    val albumObj = item.jsonObject["album"]?.jsonObject ?: continue
                    val id = albumObj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val name = albumObj["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown Album"
                    val images = albumObj["images"]?.jsonArray
                    val cover = extractHighestResSpotifyImage(images)
                    val artistName = albumObj["artists"]?.jsonArray?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                    val releaseDate = albumObj["release_date"]?.jsonPrimitive?.contentOrNull
                    val releaseYear = releaseDate?.take(4)
                    albums.add(Album(id = id, name = name, coverUrl = cover, artistName = artistName, releaseYear = releaseYear))
                }
            }
        } catch (e: Exception) {
            println("[SpotifyClient] getSavedAlbums failed: ${e.message}")
        }
        albums
    }

    suspend fun getRecentlyPlayed(limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/player/recently-played?limit=$limit") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                return@withContext parseSavedTracksPage(json, isLiked = false)
            }
        } catch (e: Exception) {
            println("[SpotifyClient] getRecentlyPlayed failed: ${e.message}")
        }
        emptyList()
    }

    suspend fun searchSpotifyTracks(query: String, limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val token = ensurePublicToken() ?: return@withContext emptyList()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/search?q=${query.encodeURLParameter()}&type=track&limit=$limit") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val items = json["tracks"]?.jsonObject?.get("items")?.jsonArray ?: return@withContext emptyList()
                return@withContext items.mapNotNull { parseTrackObject(it.jsonObject, isLiked = false) }
            }
        } catch (e: Exception) {
            println("[SpotifyClient] searchSpotifyTracks failed: ${e.message}")
        }
        emptyList()
    }

    suspend fun getPlaylistTracks(playlistId: String, limit: Int = 50): List<Track> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=$limit") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                return@withContext parseSavedTracksPage(json, isLiked = false)
            }
        } catch (e: Exception) {
            println("[SpotifyClient] getPlaylistTracks failed: ${e.message}")
        }
        emptyList()
    }

    suspend fun resolveOrSearchTrackId(track: Track): String? = withContext(Dispatchers.IO) {
        TrackIdMapper.resolveCachedSpotifyId(track)?.let { return@withContext it }

        val cleanTitle = TrackIdMapper.normalizeTitle(track.title)
        val cleanArtist = TrackIdMapper.normalizeArtist(track.artistNames)
        val query = if (cleanArtist.isNotEmpty()) "$cleanTitle $cleanArtist" else track.title
        val candidates = searchSpotifyTracks(query, limit = 5)
        if (candidates.isEmpty()) return@withContext null

        val best = candidates.maxByOrNull { cand ->
            var score = 0
            if (TrackIdMapper.normalizeTitle(cand.title) == cleanTitle) score += 3
            if (TrackIdMapper.normalizeArtist(cand.artistNames) == cleanArtist) score += 3
            if (track.durationSeconds > 0 && kotlin.math.abs(cand.durationSeconds - track.durationSeconds) <= 4) {
                score += 2
            }
            score
        } ?: candidates.first()

        val resolvedId = best.spotifyId ?: best.id
        TrackIdMapper.putPair(
            spotifyId = resolvedId,
            youtubeId = track.youtubeId,
            isrc = best.isrc ?: track.isrc,
            title = track.title,
            artistName = track.artistNames
        )
        resolvedId
    }

    suspend fun saveTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        val spotifyId = resolveOrSearchTrackId(track) ?: return@withContext false

        try {
            val resp = NetworkClient.client.put("https://api.spotify.com/v1/me/tracks?ids=$spotifyId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            return@withContext resp.status.value in 200..299
        } catch (e: Exception) {
            lastError = "Spotify saveTrack error: ${e.message}"
            println("[SpotifyClient] saveTrack failed: ${e.message}")
        }
        false
    }

    suspend fun removeTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        val spotifyId = resolveOrSearchTrackId(track) ?: return@withContext false
        try {
            val resp = NetworkClient.client.delete("https://api.spotify.com/v1/me/tracks?ids=$spotifyId") {
                header("Authorization", "Bearer $token")
            }
            return@withContext resp.status.value in 200..299
        } catch (e: Exception) {
            lastError = "Spotify removeTrack error: ${e.message}"
            println("[SpotifyClient] removeTrack failed: ${e.message}")
        }
        false
    }

    suspend fun createPlaylist(name: String, description: String = ""): Playlist? = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext null
        try {
            val payload = buildJsonObject {
                put("name", name)
                put("description", description)
                put("public", false)
            }
            val resp = NetworkClient.client.post("https://api.spotify.com/v1/me/playlists") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                return@withContext parsePlaylistsPage(buildJsonObject { put("items", buildJsonArray { add(json) }) }).firstOrNull()
                    ?: Playlist(id = json["id"]?.jsonPrimitive?.contentOrNull ?: return@withContext null, title = name, source = "spotify")
            }
        } catch (e: Exception) {
            lastError = "Spotify createPlaylist error: ${e.message}"
            println("[SpotifyClient] createPlaylist failed: ${e.message}")
        }
        null
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        val spotifyId = resolveOrSearchTrackId(track) ?: return@withContext false

        try {
            val payload = buildJsonObject {
                val uriArray = buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive("spotify:track:$spotifyId"))
                }
                put("uris", uriArray)
            }
            val resp = NetworkClient.client.post("https://api.spotify.com/v1/playlists/$playlistId/tracks") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            return@withContext resp.status.value in 200..299
        } catch (e: Exception) {
            lastError = "Spotify addTrackToPlaylist error: ${e.message}"
            println("[SpotifyClient] addTrackToPlaylist failed: ${e.message}")
        }
        false
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        val spotifyId = TrackIdMapper.resolveCachedSpotifyId(track) ?: return@withContext false

        try {
            val payload = buildJsonObject {
                put("tracks", buildJsonArray {
                    add(buildJsonObject {
                        put("uri", "spotify:track:$spotifyId")
                    })
                })
            }
            val resp = NetworkClient.client.delete("https://api.spotify.com/v1/playlists/$playlistId/tracks") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            return@withContext resp.status.value in 200..299
        } catch (e: Exception) {
            lastError = "Spotify removeTrackFromPlaylist error: ${e.message}"
            println("[SpotifyClient] removeTrackFromPlaylist failed: ${e.message}")
        }
        false
    }

    fun disconnect() {
        accessToken = null
        spDcCookie = null
        tokenExpiryMs = 0L
        userProfile = null
        anonymousAccessToken = null
        anonymousTokenExpiryMs = 0L
        lastError = null
    }
}
