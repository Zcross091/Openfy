package com.openfy.music.data.service

import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class PlaylistSyncEntry(
    val provider: ServiceProvider,
    val playlist: Playlist,
    val result: ServiceResult<Unit>
)

data class PlaylistSyncReport(
    val entries: List<PlaylistSyncEntry>
) {
    val allSucceeded: Boolean get() = entries.isNotEmpty() && entries.all { it.result.isSuccess }
    val anySucceeded: Boolean get() = entries.any { it.result.isSuccess }
    val anyFailed: Boolean get() = entries.any { it.result.isFailure }
    val failedEntries: List<PlaylistSyncEntry> get() = entries.filter { it.result.isFailure }
    val succeededEntries: List<PlaylistSyncEntry> get() = entries.filter { it.result.isSuccess }
}

class SyncCoordinator(
    private val spotify: MusicLibraryService? = null,
    private val youtube: MusicLibraryService? = null
) {
    private val activeSpotify: MusicLibraryService? get() = spotify?.takeIf { it.isConnected }
    private val activeYoutube: MusicLibraryService? get() = youtube?.takeIf { it.isConnected }

    val connectedProviders: List<ServiceProvider>
        get() = buildList {
            if (activeSpotify != null) add(ServiceProvider.SPOTIFY)
            if (activeYoutube != null) add(ServiceProvider.YOUTUBE_MUSIC)
        }

    suspend fun like(track: Track): DualResult<Unit> = dispatchDual { it.like(track) }

    suspend fun unlike(track: Track): DualResult<Unit> = dispatchDual { it.unlike(track) }

    suspend fun getPlaylists(): DualResult<List<Playlist>> = coroutineScope {
        val spDeferred = activeSpotify?.let { svc -> async { svc.getPlaylists() } }
        val ytDeferred = activeYoutube?.let { svc -> async { svc.getPlaylists() } }
        DualResult(
            spotify = spDeferred?.await(),
            youtube = ytDeferred?.await()
        )
    }

    suspend fun createPlaylist(
        name: String,
        targetProviders: Set<ServiceProvider>,
        description: String = ""
    ): DualResult<Playlist> = coroutineScope {
        val spDeferred = if (ServiceProvider.SPOTIFY in targetProviders) {
            activeSpotify?.let { svc -> async { svc.createPlaylist(name, description) } }
        } else null
        val ytDeferred = if (ServiceProvider.YOUTUBE_MUSIC in targetProviders) {
            activeYoutube?.let { svc -> async { svc.createPlaylist(name, description) } }
        } else null
        DualResult(
            spotify = spDeferred?.await(),
            youtube = ytDeferred?.await()
        )
    }

    suspend fun addToPlaylists(
        track: Track,
        spotifyPlaylists: List<Playlist>,
        youtubePlaylists: List<Playlist>
    ): PlaylistSyncReport = coroutineScope {
        val tasks = buildList {
            spotifyPlaylists.forEach { pl ->
                add(async {
                    val res = activeSpotify?.addToPlaylist(track, pl.id)
                        ?: ServiceResult.Failure("Spotify not connected", isRetryable = false)
                    PlaylistSyncEntry(ServiceProvider.SPOTIFY, pl, res)
                })
            }
            youtubePlaylists.forEach { pl ->
                add(async {
                    val res = activeYoutube?.addToPlaylist(track, pl.id)
                        ?: ServiceResult.Failure("YouTube Music not connected", isRetryable = false)
                    PlaylistSyncEntry(ServiceProvider.YOUTUBE_MUSIC, pl, res)
                })
            }
        }
        PlaylistSyncReport(tasks.awaitAll())
    }

    suspend fun retryFailed(track: Track, previousReport: PlaylistSyncReport): PlaylistSyncReport = coroutineScope {
        val retried = previousReport.failedEntries.map { entry ->
            async {
                val svc = when (entry.provider) {
                    ServiceProvider.SPOTIFY -> activeSpotify
                    ServiceProvider.YOUTUBE_MUSIC -> activeYoutube
                }
                val newRes = svc?.addToPlaylist(track, entry.playlist.id)
                    ?: ServiceResult.Failure("${entry.provider.displayName} not connected", isRetryable = false)
                entry.copy(result = newRes)
            }
        }.awaitAll()

        val retriedByKey = retried.associateBy { it.provider to it.playlist.id }
        val merged = previousReport.entries.map { original ->
            retriedByKey[original.provider to original.playlist.id] ?: original
        }
        PlaylistSyncReport(merged)
    }

    suspend fun rollbackSucceeded(track: Track, previousReport: PlaylistSyncReport): PlaylistSyncReport = coroutineScope {
        val rolledBack = previousReport.succeededEntries.map { entry ->
            async {
                val svc = when (entry.provider) {
                    ServiceProvider.SPOTIFY -> activeSpotify
                    ServiceProvider.YOUTUBE_MUSIC -> activeYoutube
                }
                val rollbackRes = svc?.removeFromPlaylist(track, entry.playlist.id)
                    ?: ServiceResult.Failure("${entry.provider.displayName} not connected", isRetryable = false)
                val updatedResult = if (rollbackRes.isSuccess) {
                    ServiceResult.Failure("Rolled back", isRetryable = false)
                } else {
                    rollbackRes
                }
                entry.copy(result = updatedResult)
            }
        }.awaitAll()

        val rolledBackByKey = rolledBack.associateBy { it.provider to it.playlist.id }
        val merged = previousReport.entries.map { original ->
            rolledBackByKey[original.provider to original.playlist.id] ?: original
        }
        PlaylistSyncReport(merged)
    }

    private suspend fun <T> dispatchDual(op: suspend (MusicLibraryService) -> ServiceResult<T>): DualResult<T> =
        coroutineScope {
            val spDeferred = activeSpotify?.let { async { op(it) } }
            val ytDeferred = activeYoutube?.let { async { op(it) } }
            DualResult(
                spotify = spDeferred?.await(),
                youtube = ytDeferred?.await()
            )
        }
}
