package com.openfy.music

import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.service.MusicLibraryService
import com.openfy.music.data.service.ServiceProvider
import com.openfy.music.data.service.ServiceResult
import com.openfy.music.data.service.SyncCoordinator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeMusicService(
    override val provider: ServiceProvider,
    override val isConnected: Boolean = true
) : MusicLibraryService {
    var shouldFailNextAdd = false
    var shouldFailLike = false
    val likedIds = mutableSetOf<String>()
    val playlistAdditions = mutableListOf<Pair<String, String>>()
    val playlistRemovals = mutableListOf<Pair<String, String>>()
    var playlists = listOf(Playlist(id = "pl_1", title = "${provider.displayName} Favorites", source = provider.name.lowercase()))

    override suspend fun like(track: Track): ServiceResult<Unit> {
        if (shouldFailLike) return ServiceResult.Failure("${provider.displayName} like error")
        likedIds.add(track.id)
        return ServiceResult.Success(Unit)
    }

    override suspend fun unlike(track: Track): ServiceResult<Unit> {
        likedIds.remove(track.id)
        return ServiceResult.Success(Unit)
    }

    override suspend fun getPlaylists(): ServiceResult<List<Playlist>> {
        return ServiceResult.Success(playlists)
    }

    override suspend fun addToPlaylist(track: Track, playlistId: String): ServiceResult<Unit> {
        if (shouldFailNextAdd) return ServiceResult.Failure("${provider.displayName} rate limited")
        playlistAdditions.add(playlistId to track.id)
        return ServiceResult.Success(Unit)
    }

    override suspend fun removeFromPlaylist(track: Track, playlistId: String): ServiceResult<Unit> {
        playlistRemovals.add(playlistId to track.id)
        return ServiceResult.Success(Unit)
    }

    override suspend fun createPlaylist(name: String, description: String): ServiceResult<Playlist> {
        val pl = Playlist(id = "created_${provider.name}", title = name, source = provider.name.lowercase())
        playlists = playlists + pl
        return ServiceResult.Success(pl)
    }
}

class SyncCoordinatorTest {
    private val sampleTrack = Track(id = "trk_1", title = "Nightcall", spotifyId = "trk_1", youtubeId = "MV_3Dpw-BRY")

    @Test
    fun likesAcrossBothConnectedServicesConcurrently() = runTest {
        val sp = FakeMusicService(ServiceProvider.SPOTIFY)
        val yt = FakeMusicService(ServiceProvider.YOUTUBE_MUSIC)
        val coordinator = SyncCoordinator(sp, yt)

        val res = coordinator.like(sampleTrack)
        assertTrue(res.allSucceeded)
        assertTrue("trk_1" in sp.likedIds)
        assertTrue("trk_1" in yt.likedIds)

        yt.shouldFailLike = true
        val partial = coordinator.like(sampleTrack)
        assertFalse(partial.allSucceeded)
        assertTrue(partial.anySucceeded)
        assertTrue(partial.anyFailed)
    }

    @Test
    fun addToPlaylistsSupportsRetryAndRollbackOnPartialFailure() = runTest {
        val sp = FakeMusicService(ServiceProvider.SPOTIFY)
        val yt = FakeMusicService(ServiceProvider.YOUTUBE_MUSIC)
        yt.shouldFailNextAdd = true

        val coordinator = SyncCoordinator(sp, yt)
        val spPl = Playlist("sp_pl1", "Spotify Workout", source = "spotify")
        val ytPl = Playlist("yt_pl1", "YTM Gym", source = "youtube")

        val initialReport = coordinator.addToPlaylists(sampleTrack, listOf(spPl), listOf(ytPl))
        assertFalse(initialReport.allSucceeded)
        assertEquals(1, initialReport.succeededEntries.size)
        assertEquals(1, initialReport.failedEntries.size)
        assertEquals("sp_pl1" to "trk_1", sp.playlistAdditions.single())
        assertTrue(yt.playlistAdditions.isEmpty())

        // Retry after clearing error -> succeeds for YTM
        yt.shouldFailNextAdd = false
        val retriedReport = coordinator.retryFailed(sampleTrack, initialReport)
        assertTrue(retriedReport.allSucceeded)
        assertEquals("yt_pl1" to "trk_1", yt.playlistAdditions.single())

        // Rollback removes from succeeded entries
        val rolledBack = coordinator.rollbackSucceeded(sampleTrack, initialReport)
        assertEquals(1, sp.playlistRemovals.size)
        assertEquals("sp_pl1" to "trk_1", sp.playlistRemovals.single())
        assertFalse(rolledBack.anySucceeded)
    }

    @Test
    fun fetchesPlaylistsAndCreatesTargetedPlaylistsAcrossConnectedServices() = runTest {
        val sp = FakeMusicService(ServiceProvider.SPOTIFY, isConnected = true)
        val yt = FakeMusicService(ServiceProvider.YOUTUBE_MUSIC, isConnected = false)
        val coordinator = SyncCoordinator(sp, yt)

        assertEquals(listOf(ServiceProvider.SPOTIFY), coordinator.connectedProviders)

        val fetched = coordinator.getPlaylists()
        assertTrue(fetched.spotify?.isSuccess == true)
        assertEquals(null, fetched.youtube)

        val created = coordinator.createPlaylist(
            name = "Road Trip",
            targetProviders = setOf(ServiceProvider.SPOTIFY, ServiceProvider.YOUTUBE_MUSIC)
        )
        assertEquals("Road Trip", created.spotify?.getOrNull()?.title)
        assertEquals(null, created.youtube)
    }
}
