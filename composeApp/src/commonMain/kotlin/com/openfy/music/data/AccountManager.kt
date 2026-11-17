package com.openfy.music.data

import com.openfy.music.data.model.AccountProvider
import com.openfy.music.data.model.ActiveAccount
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.data.remote.YouTubeAudioResolver
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AccountManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val settings get() = SettingsManager.settings

    private val _activeProvider = MutableStateFlow(loadActiveProvider())
    val activeProvider = _activeProvider.asStateFlow()

    private val _youtubeEmail = MutableStateFlow(settings.getStringOrNull("youtube_email"))
    val youtubeEmail = _youtubeEmail.asStateFlow()

    private val _youtubeCookie = MutableStateFlow(settings.getStringOrNull("youtube_cookie"))
    val youtubeCookie: String?
        get() = _youtubeCookie.value

    private val _spotifyEmail = MutableStateFlow(settings.getStringOrNull("spotify_email"))
    val spotifyEmail = _spotifyEmail.asStateFlow()

    private val _spotifyCookie = MutableStateFlow(settings.getStringOrNull("spotify_cookie"))
    val spotifyCookie: String?
        get() = _spotifyCookie.value

    private val _activeAccount = MutableStateFlow(buildActiveAccount())
    val activeAccount = _activeAccount.asStateFlow()

    val isYouTubeConnected: Boolean
        get() = !_youtubeEmail.value.isNullOrEmpty() || !_youtubeCookie.value.isNullOrEmpty()

    val isSpotifyConnected: Boolean
        get() = SpotifyClient.isConnected || !_spotifyEmail.value.isNullOrEmpty() || !_spotifyCookie.value.isNullOrEmpty()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val toastEvent = _toastEvent.asSharedFlow()

    init {
        // If stored Spotify cookie exists, ensure token is refreshed
        val savedSpDc = settings.getStringOrNull("spotify_sp_dc") ?: _spotifyCookie.value
        if (!savedSpDc.isNullOrEmpty()) {
            scope.launch {
                SpotifyClient.exchangeCookieForAccessToken(savedSpDc)
                updateActiveAccount()
            }
        }
    }

    private fun loadActiveProvider(): AccountProvider {
        return try {
            val name = settings.getString("account_provider", AccountProvider.GUEST.name)
            AccountProvider.valueOf(name)
        } catch (e: Exception) {
            AccountProvider.GUEST
        }
    }

    private fun updateActiveAccount() {
        _activeAccount.value = buildActiveAccount()
    }

    private fun buildActiveAccount(): ActiveAccount {
        return when (_activeProvider.value) {
            AccountProvider.SPOTIFY -> ActiveAccount(
                provider = AccountProvider.SPOTIFY,
                email = _spotifyEmail.value,
                displayName = SpotifyClient.userProfile?.displayName ?: _spotifyEmail.value ?: "Spotify User",
                avatarUrl = SpotifyClient.userProfile?.avatarUrl,
                sessionToken = SpotifyClient.accessToken ?: _spotifyCookie.value,
                isConnected = isSpotifyConnected
            )
            AccountProvider.YOUTUBE -> ActiveAccount(
                provider = AccountProvider.YOUTUBE,
                email = _youtubeEmail.value,
                displayName = _youtubeEmail.value ?: "Google Account",
                avatarUrl = null,
                sessionToken = _youtubeCookie.value,
                isConnected = isYouTubeConnected
            )
            AccountProvider.GUEST -> ActiveAccount(
                provider = AccountProvider.GUEST,
                email = null,
                displayName = "Guest User",
                avatarUrl = null,
                sessionToken = null,
                isConnected = false
            )
        }
    }

    fun setYouTubeGoogleCookies(cookies: String, email: String? = null) {
        val resolvedEmail = email ?: "Google Account"
        _youtubeCookie.value = cookies
        _youtubeEmail.value = resolvedEmail
        settings.putString("youtube_cookie", cookies)
        settings.putString("youtube_email", resolvedEmail)
        switchAccount(AccountProvider.YOUTUBE)
    }

    fun setYouTubeAccount(email: String) {
        val trimmed = email.trim()
        _youtubeEmail.value = trimmed
        settings.putString("youtube_email", trimmed)
        switchAccount(AccountProvider.YOUTUBE)
    }

    fun disconnectYouTube() {
        _youtubeEmail.value = null
        _youtubeCookie.value = null
        settings.remove("youtube_email")
        settings.remove("youtube_cookie")
        if (_activeProvider.value == AccountProvider.YOUTUBE) {
            switchAccount(AccountProvider.GUEST)
        } else {
            updateActiveAccount()
        }
    }

    fun setSpotifySession(cookies: String, email: String? = null) {
        val resolvedEmail = email ?: "Spotify User"
        _spotifyCookie.value = cookies
        _spotifyEmail.value = resolvedEmail
        settings.putString("spotify_cookie", cookies)
        settings.putString("spotify_email", resolvedEmail)

        val spDc = cookies.split(";").map { it.trim() }.firstNotNullOfOrNull { kv ->
            val parts = kv.split("=", limit = 2)
            if (parts.size == 2 && parts[0] == "sp_dc") parts[1] else null
        } ?: cookies

        scope.launch {
            SpotifyClient.exchangeCookieForAccessToken(spDc)
            updateActiveAccount()
        }

        switchAccount(AccountProvider.SPOTIFY)
    }

    fun setSpotifyEmailAccount(email: String, token: String? = null) {
        val trimmedEmail = email.trim()
        _spotifyEmail.value = trimmedEmail
        settings.putString("spotify_email", trimmedEmail)

        if (!token.isNullOrEmpty()) {
            val trimmedToken = token.trim()
            if (trimmedToken.startsWith("sp_dc=") || trimmedToken.length > 50) {
                scope.launch {
                    SpotifyClient.exchangeCookieForAccessToken(trimmedToken)
                    updateActiveAccount()
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
        settings.remove("spotify_email")
        settings.remove("spotify_cookie")
        if (_activeProvider.value == AccountProvider.SPOTIFY) {
            switchAccount(AccountProvider.GUEST)
        } else {
            updateActiveAccount()
        }
    }

    fun switchAccount(provider: AccountProvider) {
        _activeProvider.value = provider
        settings.putString("account_provider", provider.name)
        updateActiveAccount()
    }

    /**
     * Compute SAPISIDHASH for InnerTube authenticated calls with Google account.
     * Follows algorithm: SAPISIDHASH <epoch>_<sha1(epoch + " " + SAPISID + " " + origin)>
     */
    fun getSapisidHash(origin: String = "https://music.youtube.com"): String? {
        val cookie = _youtubeCookie.value ?: return null
        val sapisid = cookie.split(";").map { it.trim() }.firstNotNullOfOrNull { kv ->
            val parts = kv.split("=", limit = 2)
            if (parts.size == 2 && (parts[0] == "SAPISID" || parts[0] == "__Secure-3PAPISID")) {
                parts[1]
            } else null
        } ?: return null

        val epoch = getTimeMillis() / 1000L
        val input = "$epoch $sapisid $origin"
        val hash = Sha1Digest.hexString(input)
        return "SAPISIDHASH ${epoch}_$hash"
    }

    suspend fun getLikedTracks(): List<Track> {
        return when (_activeProvider.value) {
            AccountProvider.SPOTIFY -> {
                if (SpotifyClient.isConnected) {
                    SpotifyClient.getLikedTracks()
                } else {
                    YouTubeAudioResolver.getTrendingTracks()
                }
            }
            AccountProvider.YOUTUBE -> {
                if (isYouTubeConnected && !youtubeCookie.isNullOrEmpty()) {
                    YouTubeAudioResolver.getLikedVideos()
                } else {
                    YouTubeAudioResolver.getTrendingTracks()
                }
            }
            AccountProvider.GUEST -> {
                YouTubeAudioResolver.getTrendingTracks()
            }
        }
    }

    suspend fun getUserPlaylists(): List<Playlist> {
        return when (_activeProvider.value) {
            AccountProvider.SPOTIFY -> {
                if (SpotifyClient.isConnected) {
                    SpotifyClient.getUserPlaylists()
                } else {
                    YouTubeAudioResolver.getCuratedPlaylists()
                }
            }
            AccountProvider.YOUTUBE -> {
                if (isYouTubeConnected && !youtubeCookie.isNullOrEmpty()) {
                    YouTubeAudioResolver.getUserPlaylists()
                } else {
                    YouTubeAudioResolver.getCuratedPlaylists()
                }
            }
            AccountProvider.GUEST -> {
                YouTubeAudioResolver.getCuratedPlaylists()
            }
        }
    }

    suspend fun toggleLike(track: Track): Boolean {
        val wasLiked = track.isLiked
        val newLiked = !wasLiked
        track.isLiked = newLiked

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
                    true
                }
            }
            AccountProvider.GUEST -> true
        }

        val message = if (newLiked) {
            when (provider) {
                AccountProvider.SPOTIFY -> "Saved to Spotify Liked Songs!"
                AccountProvider.YOUTUBE -> "Saved to YouTube Liked Music!"
                AccountProvider.GUEST -> "Added to Liked Songs (Guest)"
            }
        } else {
            "Removed from ${provider.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} Library"
        }
        _toastEvent.emit(message)

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

/**
 * Pure Kotlin SHA-1 implementation (FIPS 180-1).
 * Completely self-contained, multiplatform, zero JVM / Android / Apple dependencies.
 */
internal object Sha1Digest {
    fun digest(input: ByteArray): ByteArray {
        val messageLenBits = input.size.toLong() * 8L
        val paddingLength = ((56 - (input.size + 1) % 64) % 64 + 64) % 64
        val totalLength = input.size + 1 + paddingLength + 8
        val padded = ByteArray(totalLength)
        input.copyInto(padded, 0, 0, input.size)
        padded[input.size] = 0x80.toByte()

        for (i in 0 until 8) {
            padded[totalLength - 1 - i] = ((messageLenBits ushr (i * 8)) and 0xFF).toByte()
        }

        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476
        var h4 = 0xC3D2E1F0.toInt()

        val w = IntArray(80)
        for (chunk in 0 until totalLength step 64) {
            for (i in 0 until 16) {
                val j = chunk + i * 4
                w[i] = ((padded[j].toInt() and 0xFF) shl 24) or
                        ((padded[j + 1].toInt() and 0xFF) shl 16) or
                        ((padded[j + 2].toInt() and 0xFF) shl 8) or
                        (padded[j + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) {
                val s = w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]
                w[i] = (s shl 1) or (s ushr 31)
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4

            for (i in 0 until 80) {
                val f: Int
                val k: Int
                when (i) {
                    in 0..19 -> {
                        f = (b and c) or (b.inv() and d)
                        k = 0x5A827999
                    }
                    in 20..39 -> {
                        f = b xor c xor d
                        k = 0x6ED9EBA1.toInt()
                    }
                    in 40..59 -> {
                        f = (b and c) or (b and d) or (c and d)
                        k = 0x8F1BBCDC.toInt()
                    }
                    else -> {
                        f = b xor c xor d
                        k = 0xCA62C1D6.toInt()
                    }
                }
                val temp = ((a shl 5) or (a ushr 27)) + f + e + k + w[i]
                e = d
                d = c
                c = (b shl 30) or (b ushr 2)
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

    fun hexString(input: String): String {
        val digest = digest(input.encodeToByteArray())
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }
}
