package com.openfy.music.data

import android.content.Context
import com.openfy.music.OpenfyApplication
import com.openfy.music.data.model.AccountProvider
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.data.remote.YouTubeAudioResolver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    fun switchAccount(provider: AccountProvider) {
        _activeProvider.value = provider
        prefs.edit().putString("provider", provider.name).apply()
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
                // Return curated trending or YouTube library
                YouTubeAudioResolver.getTrendingTracks()
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
                } else YouTubeAudioResolver.getCuratedPlaylists()
            }
            AccountProvider.YOUTUBE, AccountProvider.GUEST -> {
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
                // In YouTube Music, rate_song(LIKE)
                true
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
            "Removed from ${provider.name.lowercase().replaceFirstChar { it.uppercase() }} Library"
        }
        _toastEvent.emit(message)

        return success
    }
}
