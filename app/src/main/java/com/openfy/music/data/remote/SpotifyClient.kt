package com.openfy.music.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.openfy.music.OpenfyApplication
import com.openfy.music.data.model.Album
import com.openfy.music.data.model.Artist
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object SpotifyClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val prefs by lazy {
        OpenfyApplication.instance.getSharedPreferences("spotify_auth", Context.MODE_PRIVATE)
    }

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var spDcCookie: String?
        get() = prefs.getString("sp_dc", null)
        set(value) = prefs.edit().putString("sp_dc", value).apply()

    var tokenExpiryMs: Long
        get() = prefs.getLong("token_expiry", 0L)
        set(value) = prefs.edit().putLong("token_expiry", value).apply()

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
            val req = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                .header("Cookie", "sp_dc=$cleanCookie")
                .header("Accept", "application/json")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                val token = json.get("accessToken")?.asString
                val expiry = json.get("accessTokenExpirationTimestampMs")?.asLong
                    ?: (System.currentTimeMillis() + 3600_000L)

                if (!token.isNullOrEmpty()) {
                    spDcCookie = cleanCookie
                    accessToken = token
                    tokenExpiryMs = expiry
                    fetchProfile()
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    /**
     * Ensures the current bearer token is fresh. If expired, automatically refreshes via stored sp_dc cookie.
     */
    suspend fun ensureFreshToken(): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
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

    suspend fun fetchProfile(): UserProfile? = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext null
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me")
                .header("Authorization", "Bearer $token")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                val id = json.get("id")?.asString ?: "spotify_user"
                val name = json.get("display_name")?.asString ?: "Spotify User"
                val images = json.getAsJsonArray("images")
                val avatar = if (images != null && images.size() > 0) {
                    images[0].asJsonObject.get("url")?.asString
                } else null

                val profile = UserProfile(id, name, avatarUrl = avatar)
                userProfile = profile
                return@withContext profile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun getLikedTracks(limit: Int = 50): List<Track> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        val tracks = mutableListOf<Track>()
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks?limit=$limit")
                .header("Authorization", "Bearer $token")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                val items = json.getAsJsonArray("items") ?: return@withContext emptyList()
                for (item in items) {
                    val t = item.asJsonObject.getAsJsonObject("track") ?: continue
                    val id = t.get("id")?.asString ?: continue
                    val title = t.get("name")?.asString ?: "Unknown"

                    val artistsList = mutableListOf<Artist>()
                    t.getAsJsonArray("artists")?.forEach {
                        val a = it.asJsonObject
                        artistsList.add(Artist(a.get("id")?.asString, a.get("name")?.asString ?: ""))
                    }

                    val albumObj = t.getAsJsonObject("album")
                    val albumName = albumObj?.get("name")?.asString ?: ""
                    val albumImages = albumObj?.getAsJsonArray("images")
                    val cover = albumImages?.firstOrNull()?.asJsonObject?.get("url")?.asString
                    val album = Album(name = albumName, coverUrl = cover)

                    val duration = (t.get("duration_ms")?.asInt ?: 0) / 1000

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
            e.printStackTrace()
        }
        tracks
    }

    suspend fun getUserPlaylists(limit: Int = 30): List<Playlist> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/playlists?limit=$limit")
                .header("Authorization", "Bearer $token")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                val items = json.getAsJsonArray("items") ?: return@withContext emptyList()
                for (item in items) {
                    val obj = item.asJsonObject
                    val id = obj.get("id")?.asString ?: continue
                    val title = obj.get("name")?.asString ?: "Untitled Playlist"
                    val desc = obj.get("description")?.asString
                    val images = obj.getAsJsonArray("images")
                    val cover = images?.firstOrNull()?.asJsonObject?.get("url")?.asString
                    val count = obj.getAsJsonObject("tracks")?.get("total")?.asInt ?: 0

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
            e.printStackTrace()
        }
        playlists
    }

    suspend fun getPlaylistTracks(playlistId: String, limit: Int = 50): List<Track> = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext emptyList()
        val tracks = mutableListOf<Track>()
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=$limit")
                .header("Authorization", "Bearer $token")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                val items = json.getAsJsonArray("items") ?: return@withContext emptyList()
                for (item in items) {
                    val t = item.asJsonObject.getAsJsonObject("track") ?: continue
                    val id = t.get("id")?.asString ?: continue
                    val title = t.get("name")?.asString ?: "Unknown"

                    val artistsList = mutableListOf<Artist>()
                    t.getAsJsonArray("artists")?.forEach {
                        val a = it.asJsonObject
                        artistsList.add(Artist(a.get("id")?.asString, a.get("name")?.asString ?: ""))
                    }

                    val albumObj = t.getAsJsonObject("album")
                    val albumName = albumObj?.get("name")?.asString ?: ""
                    val albumImages = albumObj?.getAsJsonArray("images")
                    val cover = albumImages?.firstOrNull()?.asJsonObject?.get("url")?.asString
                    val album = Album(name = albumName, coverUrl = cover)
                    val duration = (t.get("duration_ms")?.asInt ?: 0) / 1000

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
            e.printStackTrace()
        }
        tracks
    }

    suspend fun saveTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        var spotifyId = track.spotifyId
        if (spotifyId == null) {
            // Search Spotify ID
            spotifyId = searchTrackId(track.title, track.artistNames)
        }
        if (spotifyId == null) return@withContext false

        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks?ids=$spotifyId")
                .header("Authorization", "Bearer $token")
                .put("".toRequestBody("application/json".toMediaType()))
                .build()
            val resp = client.newCall(req).execute()
            return@withContext resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    suspend fun removeTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        val spotifyId = track.spotifyId ?: track.id
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks?ids=$spotifyId")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            val resp = client.newCall(req).execute()
            return@withContext resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    /**
     * Creates a new playlist on the user's authentic Spotify account.
     * Reflects immediately in the official Spotify app.
     */
    suspend fun createPlaylist(name: String, description: String = ""): Playlist? = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext null
        try {
            val payload = JsonObject().apply {
                addProperty("name", name)
                addProperty("description", description)
                addProperty("public", false)
            }
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/playlists")
                .header("Authorization", "Bearer $token")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                val id = json.get("id")?.asString ?: return@withContext null
                val title = json.get("name")?.asString ?: name
                val desc = json.get("description")?.asString
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
            e.printStackTrace()
        }
        null
    }

    /**
     * Adds a track to a specific Spotify playlist.
     * Reflects immediately in the official Spotify app.
     */
    suspend fun addTrackToPlaylist(playlistId: String, track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext false
        var spotifyId = track.spotifyId
        if (spotifyId == null) {
            spotifyId = searchTrackId(track.title, track.artistNames)
        }
        if (spotifyId == null) return@withContext false

        try {
            val payload = JsonObject().apply {
                val uris = com.google.gson.JsonArray().apply {
                    add("spotify:track:$spotifyId")
                }
                add("uris", uris)
            }
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId/tracks")
                .header("Authorization", "Bearer $token")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            return@withContext resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    private suspend fun searchTrackId(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val token = ensureFreshToken() ?: return@withContext null
        try {
            val query = "track:$title artist:$artist"
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&type=track&limit=1")
                .header("Authorization", "Bearer $token")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                val items = json.getAsJsonObject("tracks")?.getAsJsonArray("items")
                return@withContext items?.firstOrNull()?.asJsonObject?.get("id")?.asString
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
