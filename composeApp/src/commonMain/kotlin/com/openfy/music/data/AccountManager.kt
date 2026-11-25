package com.openfy.music.data

import com.openfy.music.data.model.AccountProvider
import com.openfy.music.data.model.ActiveAccount
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.data.remote.YouTubeAudioResolver
import com.openfy.music.data.service.SpotifyLibraryService
import com.openfy.music.data.service.SyncCoordinator
import com.openfy.music.data.service.YouTubeMusicLibraryService
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

    private val _likedTracks = MutableStateFlow<List<Track>>(emptyList())
    val likedTracks = _likedTracks.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    private val _libraryError = MutableStateFlow<String?>(null)
    val libraryError = _libraryError.asStateFlow()

    val syncCoordinator: SyncCoordinator by lazy {
        SyncCoordinator(SpotifyLibraryService(), YouTubeMusicLibraryService())
    }

    val isYouTubeConnected: Boolean
        get() = !_youtubeEmail.value.isNullOrEmpty() || !_youtubeCookie.value.isNullOrEmpty()

    val isSpotifyConnected: Boolean
        get() = SpotifyClient.isConnected || !_spotifyEmail.value.isNullOrEmpty() || !_spotifyCookie.value.isNullOrEmpty()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val toastEvent = _toastEvent.asSharedFlow()

    suspend fun emitToast(message: String) {
        _toastEvent.emit(message)
    }

    init {
        // If stored Spotify cookie exists, ensure token is refreshed
        val savedSpDc = settings.getStringOrNull("spotify_sp_dc") ?: _spotifyCookie.value
        if (!savedSpDc.isNullOrEmpty()) {
            scope.launch {
                SpotifyClient.exchangeCookieForAccessToken(savedSpDc)
                updateActiveAccount()
                refreshLibrary()
            }
        } else {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        scope.launch {
            _libraryError.value = null
            try {
                val tracks = getLikedTracks()
                val pls = getUserPlaylists()
                _likedTracks.value = tracks
                _playlists.value = pls
                val providerErr = when (_activeProvider.value) {
                    AccountProvider.SPOTIFY -> SpotifyClient.lastError
                    AccountProvider.YOUTUBE -> YouTubeAudioResolver.lastError
                    AccountProvider.GUEST -> null
                }
                if (providerErr != null && tracks.isEmpty() && pls.isEmpty()) {
                    _libraryError.value = providerErr
                }
            } catch (e: Exception) {
                _libraryError.value = e.message ?: "Failed to refresh library"
                println("[AccountManager] refreshLibrary error: ${e.message}")
            }
        }
    }

    fun toggleLikeTrack(track: Track) {
        scope.launch {
            toggleLike(track)
            refreshLibrary()
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
        refreshLibrary()
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
                } else {
                    emptyList()
                }
            }
            AccountProvider.YOUTUBE -> {
                if (isYouTubeConnected && !youtubeCookie.isNullOrEmpty()) {
                    YouTubeAudioResolver.getUserPlaylists()
                } else {
                    emptyList()
                }
            }
            AccountProvider.GUEST -> emptyList()
        }
    }

    /**
     * Likes or unlikes a track across ALL connected services (Spotify + YouTube Music simultaneously).
     * Only mutates [Track.isLiked] when at least one connected backend confirms success,
     * rolling back otherwise.
     */
    suspend fun toggleLike(track: Track): Boolean {
        val connected = syncCoordinator.connectedProviders
        if (connected.isEmpty()) {
            _toastEvent.emit("Sign in to Spotify or YouTube Music to like tracks")
            return false
        }

        val wasLiked = track.isLiked
        val newLiked = !wasLiked

        val dual = if (newLiked) syncCoordinator.like(track) else syncCoordinator.unlike(track)

        if (dual.anySucceeded) {
            track.isLiked = newLiked
            val succeededNames = buildList {
                if (dual.spotify?.isSuccess == true) add("Spotify")
                if (dual.youtube?.isSuccess == true) add("YouTube Music")
            }
            val failedNames = buildList {
                if (dual.spotify?.isFailure == true) add("Spotify")
                if (dual.youtube?.isFailure == true) add("YouTube Music")
            }

            val actionWord = if (newLiked) "Saved to" else "Removed from"
            val message = if (failedNames.isEmpty()) {
                "$actionWord ${succeededNames.joinToString(" & ")}!"
            } else {
                "$actionWord ${succeededNames.joinToString(" & ")} (${failedNames.joinToString(", ")} failed)"
            }
            _toastEvent.emit(message)
            return true
        } else {
            // Rollback — every connected service failed
            track.isLiked = wasLiked
            _toastEvent.emit("Failed to update like on ${connected.joinToString(" & ") { it.displayName }}")
            return false
        }
    }

    suspend fun addTrackToPlaylist(playlist: Playlist, track: Track): Boolean {
        val success = when (playlist.source) {
            "youtube" -> {
                val vid = YouTubeAudioResolver.resolveOrSearchVideoId(track)
                if (vid != null) YouTubeAudioResolver.addTrackToYtmPlaylist(playlist.id, vid).succeeded
                else false
            }
            else -> SpotifyClient.addTrackToPlaylist(playlist.id, track)
        }
        val message = if (success) "Added to ${playlist.title}!" else "Failed to add to ${playlist.title}"
        _toastEvent.emit(message)
        return success
    }

    suspend fun createPlaylist(name: String): Playlist? {
        val playlist = when (_activeProvider.value) {
            AccountProvider.SPOTIFY -> SpotifyClient.createPlaylist(name)
            AccountProvider.YOUTUBE -> YouTubeAudioResolver.createYtmPlaylist(name)
            AccountProvider.GUEST -> null
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
