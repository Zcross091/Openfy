package com.openfy.music.data.service

import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.SpotifyClient

class SpotifyLibraryService : MusicLibraryService {
    override val provider: ServiceProvider = ServiceProvider.SPOTIFY

    override val isConnected: Boolean
        get() = AccountManager.isSpotifyConnected && SpotifyClient.isConnected

    override suspend fun like(track: Track): ServiceResult<Unit> {
        if (!isConnected) {
            return ServiceResult.Failure("Spotify not connected", isRetryable = false)
        }
        return try {
            val ok = SpotifyClient.saveTrack(track)
            if (ok) ServiceResult.Success(Unit)
            else ServiceResult.Failure(SpotifyClient.lastError ?: "Failed to save track to Spotify")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "Spotify error", cause = e)
        }
    }

    override suspend fun unlike(track: Track): ServiceResult<Unit> {
        if (!isConnected) {
            return ServiceResult.Failure("Spotify not connected", isRetryable = false)
        }
        return try {
            val ok = SpotifyClient.removeTrack(track)
            if (ok) ServiceResult.Success(Unit)
            else ServiceResult.Failure(SpotifyClient.lastError ?: "Failed to remove track from Spotify")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "Spotify error", cause = e)
        }
    }

    override suspend fun getPlaylists(): ServiceResult<List<Playlist>> {
        if (!isConnected) {
            return ServiceResult.Failure("Spotify not connected", isRetryable = false)
        }
        return try {
            val playlists = SpotifyClient.getUserPlaylists(limit = 50)
            val err = SpotifyClient.lastError
            if (playlists.isEmpty() && err != null) {
                ServiceResult.Failure(err)
            } else {
                ServiceResult.Success(playlists)
            }
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "Failed to fetch Spotify playlists", cause = e)
        }
    }

    override suspend fun addToPlaylist(track: Track, playlistId: String): ServiceResult<Unit> {
        if (!isConnected) {
            return ServiceResult.Failure("Spotify not connected", isRetryable = false)
        }
        return try {
            val ok = SpotifyClient.addTrackToPlaylist(playlistId, track)
            if (ok) ServiceResult.Success(Unit)
            else ServiceResult.Failure(SpotifyClient.lastError ?: "Failed to add track to Spotify playlist")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "Spotify playlist add error", cause = e)
        }
    }

    override suspend fun removeFromPlaylist(track: Track, playlistId: String): ServiceResult<Unit> {
        if (!isConnected) {
            return ServiceResult.Failure("Spotify not connected", isRetryable = false)
        }
        return try {
            val ok = SpotifyClient.removeTrackFromPlaylist(playlistId, track)
            if (ok) ServiceResult.Success(Unit)
            else ServiceResult.Failure(SpotifyClient.lastError ?: "Failed to rollback track from Spotify playlist")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "Spotify rollback error", cause = e)
        }
    }

    override suspend fun createPlaylist(name: String, description: String): ServiceResult<Playlist> {
        if (!isConnected) {
            return ServiceResult.Failure("Spotify not connected", isRetryable = false)
        }
        return try {
            val created = SpotifyClient.createPlaylist(name, description)
            if (created != null) ServiceResult.Success(created)
            else ServiceResult.Failure(SpotifyClient.lastError ?: "Failed to create Spotify playlist")
        } catch (e: Exception) {
            ServiceResult.Failure(e.message ?: "Spotify create playlist error", cause = e)
        }
    }
}
