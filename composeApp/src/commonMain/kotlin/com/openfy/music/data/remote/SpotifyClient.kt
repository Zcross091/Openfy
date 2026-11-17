package com.openfy.music.data.remote

import com.openfy.music.data.SettingsManager
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
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

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

    val isConnected: Boolean
        get() = !accessToken.isNullOrEmpty() || !spDcCookie.isNullOrEmpty()

    /**
     * Exchanges a persistent Spotify web session cookie (sp_dc) for an official Web Player Bearer Access Token.
     * Enables full private library access (Liked Songs, user playlists, following) with zero Developer App setup.
     */
    suspend fun exchangeCookieForAccessToken(spDc: String): Boolean = withContext(Dispatchers.IO) {
        val cleanCookie = spDc.trim().removePrefix("sp_dc=").trim('"', '\'', ';')
        if (cleanCookie.isEmpty()) return@withContext false

        try {
            val resp = NetworkClient.client.get("https://open.spotify.com/get_access_token?reason=transport&productType=web_player") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                header("Cookie", "sp_dc=$cleanCookie")
                header("Accept", "application/json")
            }

            if (resp.status.value in 200..299) {
                val body = resp.bodyAsText()
                val json = NetworkClient.json.parseToJsonElement(body).jsonObject
                val token = json["accessToken"]?.jsonPrimitive?.contentOrNull
                val expiry = json["accessTokenExpirationTimestampMs"]?.jsonPrimitive?.longOrNull
                    ?: (getTimeMillis() + 3600_000L)

                if (!token.isNullOrEmpty()) {
                    spDcCookie = cleanCookie
                    accessToken = token
                    tokenExpiryMs = expiry
                    fetchProfile()
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        false
    }

    /**
     * Ensures the current bearer token is fresh. If expired, automatically refreshes via stored sp_dc cookie.
     */
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

    private fun extractHighestResSpotifyImage(images: kotlinx.serialization.json.JsonArray?): String? {
        if (images == null || images.isEmpty()) return null
        val best = images.mapNotNull { it.jsonObject }.maxByOrNull { img ->
            val w = img["width"]?.jsonPrimitive?.intOrNull ?: 0
            val h = img["height"]?.jsonPrimitive?.intOrNull ?: 0
            w * h
        } ?: images.firstOrNull()?.jsonObject
        val url = best?.get("url")?.jsonPrimitive?.contentOrNull
        return ArtworkUtils.getHighResArtworkUrl(url)
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
                return@withContext profile
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }

    suspend fun getLikedTracks(limit: Int = 50): List<Track> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        val tracks = mutableListOf<Track>()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/tracks?limit=$limit") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val items = json["items"]?.jsonArray ?: return@withContext emptyList()
                for (item in items) {
                    val t = item.jsonObject["track"]?.jsonObject ?: continue
                    val id = t["id"]?.jsonPrimitive?.contentOrNull ?: continue
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

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artists = artistsList,
                            album = album,
                            durationSeconds = duration,
                            coverUrl = cover,
                            source = "spotify",
                            spotifyId = id,
                            isLiked = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        tracks
    }

    suspend fun getUserPlaylists(limit: Int = 30): List<Playlist> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/me/playlists?limit=$limit") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val items = json["items"]?.jsonArray ?: return@withContext emptyList()
                for (item in items) {
                    val obj = item.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val title = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Untitled Playlist"
                    val desc = obj["description"]?.jsonPrimitive?.contentOrNull
                    val images = obj["images"]?.jsonArray
                    val cover = extractHighestResSpotifyImage(images)
                    val count = obj["tracks"]?.jsonObject?.get("total")?.jsonPrimitive?.intOrNull ?: 0

                    playlists.add(
                        Playlist(
                            id = id,
                            title = title,
                            description = desc,
                            coverUrl = cover,
                            trackCount = count,
                            source = "spotify"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        playlists
    }

    suspend fun getPlaylistTracks(playlistId: String, limit: Int = 50): List<Track> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        val tracks = mutableListOf<Track>()
        try {
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=$limit") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val items = json["items"]?.jsonArray ?: return@withContext emptyList()
                for (item in items) {
                    val t = item.jsonObject["track"]?.jsonObject ?: continue
                    val id = t["id"]?.jsonPrimitive?.contentOrNull ?: continue
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

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artists = artistsList,
                            album = album,
                            durationSeconds = duration,
                            coverUrl = cover,
                            source = "spotify",
                            spotifyId = id
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        tracks
    }

    /**
     * Adds track to user's Liked Songs using PUT /v1/me/tracks?ids=...
     */
    suspend fun saveTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        var spotifyId = track.spotifyId
        if (spotifyId == null) {
            spotifyId = searchTrackId(track.title, track.artistNames)
        }
        if (spotifyId == null) return@withContext false

        try {
            val resp = NetworkClient.client.put("https://api.spotify.com/v1/me/tracks?ids=$spotifyId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            return@withContext resp.status.value in 200..299
        } catch (e: Exception) {
            // Ignore
        }
        false
    }

    suspend fun removeTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        val spotifyId = track.spotifyId ?: track.id
        try {
            val resp = NetworkClient.client.delete("https://api.spotify.com/v1/me/tracks?ids=$spotifyId") {
                header("Authorization", "Bearer $token")
            }
            return@withContext resp.status.value in 200..299
        } catch (e: Exception) {
            // Ignore
        }
        false
    }

    /**
     * Creates a new playlist on the user's authentic Spotify account.
     */
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
                val id = json["id"]?.jsonPrimitive?.contentOrNull ?: return@withContext null
                val title = json["name"]?.jsonPrimitive?.contentOrNull ?: name
                val desc = json["description"]?.jsonPrimitive?.contentOrNull
                return@withContext Playlist(
                    id = id,
                    title = title,
                    description = desc,
                    coverUrl = null,
                    trackCount = 0,
                    source = "spotify"
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }

    /**
     * Adds a track to a specific Spotify playlist.
     */
    suspend fun addTrackToPlaylist(playlistId: String, track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        var spotifyId = track.spotifyId
        if (spotifyId == null) {
            spotifyId = searchTrackId(track.title, track.artistNames)
        }
        if (spotifyId == null) return@withContext false

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
            // Ignore
        }
        false
    }

    private suspend fun searchTrackId(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext null
        try {
            val query = "track:$title artist:$artist"
            val resp = NetworkClient.client.get("https://api.spotify.com/v1/search?q=${query.encodeURLParameter()}&type=track&limit=1") {
                header("Authorization", "Bearer $token")
            }
            if (resp.status.value in 200..299) {
                val json = NetworkClient.json.parseToJsonElement(resp.bodyAsText()).jsonObject
                val items = json["tracks"]?.jsonObject?.get("items")?.jsonArray
                return@withContext items?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }

    fun disconnect() {
        accessToken = null
        spDcCookie = null
        tokenExpiryMs = 0L
        userProfile = null
    }
}
