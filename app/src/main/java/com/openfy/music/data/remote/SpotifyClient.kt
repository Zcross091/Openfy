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

    var userProfile: UserProfile? = null

    val isConnected: Boolean
        get() = !accessToken.isNullOrEmpty()

    suspend fun fetchProfile(): UserProfile? = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext null
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
        val token = accessToken ?: return@withContext emptyList()
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
        val token = accessToken ?: return@withContext emptyList()
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

    suspend fun saveTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext false
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
        val token = accessToken ?: return@withContext false
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

    private suspend fun searchTrackId(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext null
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
        userProfile = null
    }
}
