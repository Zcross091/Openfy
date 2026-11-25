package com.openfy.music.data.service

import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.YouTubeAudioResolver

class YouTubeMusicLibraryService : MusicLibraryService {
    override val provider: ServiceProvider = ServiceProvider.YOUTUBE_MUSIC

    override val isConnected: Boolean
        get() = AccountManager.isYouTubeConnected && !AccountManager.youtubeCookie.isNullOrEmpty()

    // Tracks setVideoId returned by ACTION_ADD_VIDEO so rollback can cleanly remove the exact entry
    private val addedSetVideoIds = mutableMapOf<Pair<String, String>, String>()

    override suspend fun like(track: Track): ServiceResult<Unit> {
        if (!isConnected) {
            return ServiceResult.Failure("YouTube Music not connected", isRetryable = false)
        }
        return try {
            val videoId = YouTubeAudioResolver.resolveOrSearchVideoId(track)
                ?: return ServiceResult.Failure("Could not match '${track.title}' on YouTube Music", isRetryable = false)
            val ok = YouTubeAudioResolver.rateSong(videoId, isLiked = true)
            if (ok) ServiceResult.Success(Unit)
            else ServiceResult.Failure("YouTube Music rateSong failed")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "YouTube Music error", cause = e)
        }
    }

    override suspend fun unlike(track: Track): ServiceResult<Unit> {
        if (!isConnected) {
            return ServiceResult.Failure("YouTube Music not connected", isRetryable = false)
        }
        return try {
            val videoId = YouTubeAudioResolver.resolveOrSearchVideoId(track)
                ?: return ServiceResult.Failure("Could not match '${track.title}' on YouTube Music", isRetryable = false)
            val ok = YouTubeAudioResolver.rateSong(videoId, isLiked = false)
            if (ok) ServiceResult.Success(Unit)
            else ServiceResult.Failure("YouTube Music unlike failed")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "YouTube Music error", cause = e)
        }
    }

    override suspend fun getPlaylists(): ServiceResult<List<Playlist>> {
        if (!isConnected) {
            return ServiceResult.Failure("YouTube Music not connected", isRetryable = false)
        }
        return try {
            val playlists = YouTubeAudioResolver.getUserPlaylists()
            val err = YouTubeAudioResolver.lastError
            if (playlists.isEmpty() && err != null) {
                ServiceResult.Failure(err)
            } else {
                ServiceResult.Success(playlists)
            }
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "Failed to fetch YouTube Music playlists", cause = e)
        }
    }

    override suspend fun addToPlaylist(track: Track, playlistId: String): ServiceResult<Unit> {
        if (!isConnected) {
            return ServiceResult.Failure("YouTube Music not connected", isRetryable = false)
        }
        return try {
            val videoId = YouTubeAudioResolver.resolveOrSearchVideoId(track)
                ?: return ServiceResult.Failure("Could not match '${track.title}' on YouTube Music", isRetryable = false)
            val editResult = YouTubeAudioResolver.addTrackToYtmPlaylist(playlistId, videoId)
            if (editResult.succeeded) {
                editResult.setVideoId?.let { addedSetVideoIds[playlistId to videoId] = it }
                ServiceResult.Success(Unit)
            } else {
                ServiceResult.Failure(editResult.errorMessage ?: "Failed to add to YouTube Music playlist")
            }
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "YouTube Music playlist add error", cause = e)
        }
    }

    override suspend fun removeFromPlaylist(track: Track, playlistId: String): ServiceResult<Unit> {
        if (!isConnected) {
            return ServiceResult.Failure("YouTube Music not connected", isRetryable = false)
        }
        return try {
            val videoId = YouTubeAudioResolver.resolveOrSearchVideoId(track)
                ?: return ServiceResult.Failure("Could not resolve videoId for rollback", isRetryable = false)
            val setVideoId = addedSetVideoIds.remove(playlistId to videoId)
            val ok = YouTubeAudioResolver.removeTrackFromYtmPlaylist(playlistId, videoId, setVideoId)
            if (ok) ServiceResult.Success(Unit)
            else ServiceResult.Failure("Failed to rollback YouTube Music playlist item")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "YouTube Music rollback error", cause = e)
        }
    }

    override suspend fun createPlaylist(name: String, description: String): ServiceResult<Playlist> {
        if (!isConnected) {
            return ServiceResult.Failure("YouTube Music not connected", isRetryable = false)
        }
        return try {
            val created = YouTubeAudioResolver.createYtmPlaylist(name, description)
            if (created != null) ServiceResult.Success(created)
            else ServiceResult.Failure(YouTubeAudioResolver.lastError ?: "Failed to create YouTube Music playlist")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "YouTube Music create playlist error", cause = e)
        }
    }
}
