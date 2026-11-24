package com.openfy.music.data

import android.content.Context
import com.openfy.music.OpenfyApplication
import com.openfy.music.data.model.AccountProvider
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.data.remote.YouTubeAudioResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

object AccountManager {
    private val prefs by lazy {
        OpenfyApplication.instance.getSharedPreferences("openfy_account", Context.MODE_PRIVATE)
    }

    private val _activeProvider = MutableStateFlow(
        try {
            AccountProvider.valueOf(prefs.getString("provider", AccountProvider.GUEST.name) ?: AccountProvider.GUEST.name)
        } catch (e: Exception) {
            AccountProvider.GUEST
        }
    )
    val activeProvider = _activeProvider.asStateFlow()

    private val _youtubeEmail = MutableStateFlow(prefs.getString("youtube_email", null))
    val youtubeEmail = _youtubeEmail.asStateFlow()

    private val _youtubeCookie = MutableStateFlow(prefs.getString("youtube_cookie", null))
    val youtubeCookie: String?
        get() = _youtubeCookie.value

    private val _spotifyEmail = MutableStateFlow(prefs.getString("spotify_email", null))
    val spotifyEmail = _spotifyEmail.asStateFlow()

    private val _spotifyCookie = MutableStateFlow(prefs.getString("spotify_cookie", null))
    val spotifyCookie: String?
        get() = _spotifyCookie.value

    val isYouTubeConnected: Boolean
        get() = !_youtubeEmail.value.isNullOrEmpty() || !_youtubeCookie.value.isNullOrEmpty()

    val isSpotifyConnected: Boolean
        get() = SpotifyClient.isConnected || !_spotifyEmail.value.isNullOrEmpty() || !_spotifyCookie.value.isNullOrEmpty()

    fun setYouTubeGoogleCookies(cookies: String, email: String? = null) {
        val resolvedEmail = email ?: "Google Account"
        _youtubeCookie.value = cookies
        _youtubeEmail.value = resolvedEmail
        prefs.edit()
            .putString("youtube_cookie", cookies)
            .putString("youtube_email", resolvedEmail)
            .apply()
        switchAccount(AccountProvider.YOUTUBE)
    }

    fun setYouTubeAccount(email: String) {
        _youtubeEmail.value = email.trim()
        prefs.edit().putString("youtube_email", email.trim()).apply()
        switchAccount(AccountProvider.YOUTUBE)
    }

    fun disconnectYouTube() {
        _youtubeEmail.value = null
        _youtubeCookie.value = null
        prefs.edit().remove("youtube_email").remove("youtube_cookie").apply()
    }

    fun setSpotifySession(cookies: String, email: String? = null) {
        val resolvedEmail = email ?: "Spotify User"
        _spotifyCookie.value = cookies
        _spotifyEmail.value = resolvedEmail
        prefs.edit()
            .putString("spotify_cookie", cookies)
            .putString("spotify_email", resolvedEmail)
            .apply()

        // Extract sp_dc cookie if present
        val spDc = cookies.split(";").map { it.trim() }.firstNotNullOfOrNull { kv ->
            val parts = kv.split("=", limit = 2)
            if (parts.size == 2 && parts[0] == "sp_dc") parts[1] else null
        } ?: cookies

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            SpotifyClient.exchangeCookieForAccessToken(spDc)
        }

        switchAccount(AccountProvider.SPOTIFY)
    }

    fun setSpotifyEmailAccount(email: String, token: String? = null) {
        _spotifyEmail.value = email.trim()
        prefs.edit().putString("spotify_email", email.trim()).apply()
        if (!token.isNullOrEmpty()) {
            val trimmedToken = token.trim()
            if (trimmedToken.startsWith("sp_dc=") || trimmedToken.length > 50) {
                // If user pasted an sp_dc cookie or token
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    SpotifyClient.exchangeCookieForAccessToken(trimmedToken)
                }
            } else {
                SpotifyClient.accessToken = trimmedToken
            }
        }
        switchAccount(AccountProvider.SPOTIFY)
    }

    fun disconnectSpotify() {
        SpotifyClient.disconnect()
        _spotifyEmail.value = null
        _spotifyCookie.value = null
        prefs.edit().remove("spotify_email").remove("spotify_cookie").apply()
    }

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    fun switchAccount(provider: AccountProvider) {
        _activeProvider.value = provider
        prefs.edit().putString("provider", provider.name).apply()
    }

    /**
     * Compute SAPISIDHASH for InnerTube authenticated calls with Google account.
     * Follows the algorithm: SAPISIDHASH <epoch>_<sha1(epoch + " " + SAPISID + " " + origin)>
     */
    fun getSapisidHash(origin: String = "https://music.youtube.com"): String? {
        val cookie = _youtubeCookie.value ?: return null
        val sapisid = cookie.split(";").map { it.trim() }.firstNotNullOfOrNull { kv ->
            val parts = kv.split("=", limit = 2)
            if (parts.size == 2 && (parts[0] == "SAPISID" || parts[0] == "__Secure-3PAPISID")) {
                parts[1]
            } else null
        } ?: return null

        val epoch = System.currentTimeMillis() / 1000L
        val input = "$epoch $sapisid $origin"
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${epoch}_$hex"
    }

    suspend fun getLikedTracks(): List<Track> {
        return when (_activeProvider.value) {
            AccountProvider.SPOTIFY -> {
                if (SpotifyClient.isConnected) {
                    SpotifyClient.getLikedTracks()
                } else {
                    emptyList()
                }
            }
            AccountProvider.YOUTUBE -> {
                if (isYouTubeConnected && !youtubeCookie.isNullOrEmpty()) {
                    YouTubeAudioResolver.getLikedVideos()
                } else {
                    emptyList()
                }
            }
            AccountProvider.GUEST -> {
                emptyList()
            }
        }
    }

    suspend fun getUserPlaylists(): List<Playlist> {
        return when (_activeProvider.value) {
            AccountProvider.SPOTIFY -> {
                if (SpotifyClient.isConnected) {
                    SpotifyClient.getUserPlaylists()
                } else emptyList()
            }
            AccountProvider.YOUTUBE -> {
                if (isYouTubeConnected && !youtubeCookie.isNullOrEmpty()) {
                    YouTubeAudioResolver.getUserPlaylists()
                } else {
                    emptyList()
                }
            }
            AccountProvider.GUEST -> {
                emptyList()
            }
        }
    }

    suspend fun toggleLike(track: Track): Boolean {
        val wasLiked = track.isLiked
        val newLiked = !wasLiked

        val provider = _activeProvider.value
        val success = when (provider) {
            AccountProvider.SPOTIFY -> {
                if (newLiked) SpotifyClient.saveTrack(track)
                else SpotifyClient.removeTrack(track)
            }
            AccountProvider.YOUTUBE -> {
                val vid = track.youtubeId ?: track.id
                if (vid.length == 11 && !youtubeCookie.isNullOrEmpty()) {
                    YouTubeAudioResolver.rateSong(vid, newLiked)
                } else {
                    false
                }
            }
            AccountProvider.GUEST -> false
        }

        if (success) {
            track.isLiked = newLiked
            val message = if (newLiked) {
                when (provider) {
                    AccountProvider.SPOTIFY -> "Saved to Spotify Liked Songs!"
                    AccountProvider.YOUTUBE -> "Saved to YouTube Liked Music!"
                    AccountProvider.GUEST -> "Liked songs require an account."
                }
            } else {
                "Removed from ${provider.name.lowercase().replaceFirstChar { it.uppercase() }} Library"
            }
            _toastEvent.emit(message)
        } else {
            // Roll back state and notify failure
            track.isLiked = wasLiked
            val message = if (provider == AccountProvider.GUEST) {
                "Sign in to Spotify or YouTube Music to like tracks"
            } else {
                "Failed to update like on ${provider.name.lowercase().replaceFirstChar { it.uppercase() }}"
            }
            _toastEvent.emit(message)
        }

        return success
    }

    suspend fun addTrackToPlaylist(playlist: Playlist, track: Track): Boolean {
        val success = when (_activeProvider.value) {
            AccountProvider.SPOTIFY -> SpotifyClient.addTrackToPlaylist(playlist.id, track)
            else -> false
        }
        val message = if (success) "Added to ${playlist.title}!" else "Failed to add to ${playlist.title}"
        _toastEvent.emit(message)
        return success
    }

    suspend fun createPlaylist(name: String): Playlist? {
        val playlist = when (_activeProvider.value) {
            AccountProvider.SPOTIFY -> SpotifyClient.createPlaylist(name)
            else -> null
        }
        if (playlist != null) {
            _toastEvent.emit("Created playlist '${playlist.title}'!")
        }
        return playlist
    }
}
