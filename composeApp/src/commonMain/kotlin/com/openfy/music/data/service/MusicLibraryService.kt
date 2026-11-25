package com.openfy.music.data.service

import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track

sealed class ServiceResult<out T> {
    data class Success<T>(val value: T) : ServiceResult<T>()
    data class Failure(
        val message: String,
        val cause: Throwable? = null,
        val isRetryable: Boolean = true
    ) : ServiceResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.value
}

enum class ServiceProvider(val displayName: String) {
    SPOTIFY("Spotify"),
    YOUTUBE_MUSIC("YouTube Music")
}

data class DualResult<T>(
    val spotify: ServiceResult<T>? = null,
    val youtube: ServiceResult<T>? = null
) {
    val hasAnyService: Boolean get() = spotify != null || youtube != null

    val allSucceeded: Boolean
        get() = hasAnyService &&
            (spotify == null || spotify is ServiceResult.Success) &&
            (youtube == null || youtube is ServiceResult.Success)

    val anySucceeded: Boolean
        get() = spotify is ServiceResult.Success || youtube is ServiceResult.Success

    val anyFailed: Boolean
        get() = spotify is ServiceResult.Failure || youtube is ServiceResult.Failure
}

interface MusicLibraryService {
    val provider: ServiceProvider
    val isConnected: Boolean

    suspend fun like(track: Track): ServiceResult<Unit>
    suspend fun unlike(track: Track): ServiceResult<Unit>
    suspend fun getPlaylists(): ServiceResult<List<Playlist>>
    suspend fun addToPlaylist(track: Track, playlistId: String): ServiceResult<Unit>
    suspend fun removeFromPlaylist(track: Track, playlistId: String): ServiceResult<Unit>
    suspend fun createPlaylist(name: String, description: String = ""): ServiceResult<Playlist>
}
