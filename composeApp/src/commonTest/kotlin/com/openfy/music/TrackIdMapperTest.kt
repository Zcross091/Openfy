package com.openfy.music

import com.openfy.music.data.TrackIdMapper
import com.openfy.music.data.model.Artist
import com.openfy.music.data.model.Track
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TrackIdMapperTest {
    private var fakeTimeMs = 1_700_000_000_000L

    @BeforeTest
    fun setUp() {
        TrackIdMapper.clear()
        TrackIdMapper.clockMs = { fakeTimeMs }
    }

    @AfterTest
    fun tearDown() {
        TrackIdMapper.clear()
    }

    @Test
    fun mapsByIsrcAcrossSpotifyAndYoutube() {
        TrackIdMapper.putSpotify(
            spotifyId = "sp_123",
            isrc = "usum71403405",
            title = "Blinding Lights",
            artistName = "The Weeknd"
        )
        TrackIdMapper.putYoutube(
            youtubeId = "4NRXx6U8ABQ",
            isrc = "USUM71403405",
            title = "Blinding Lights (Official Audio)",
            artistName = "The Weeknd"
        )

        assertEquals("4NRXx6U8ABQ", TrackIdMapper.spotifyToYoutube("sp_123"))
        assertEquals("sp_123", TrackIdMapper.youtubeToSpotify("4NRXx6U8ABQ"))

        val byIsrc = TrackIdMapper.findByIsrc("USUM71403405")
        assertNotNull(byIsrc)
        assertEquals("sp_123", byIsrc.spotifyId)
        assertEquals("4NRXx6U8ABQ", byIsrc.youtubeId)
    }

    @Test
    fun mapsByNormalizedSlugWhenIsrcIsAbsent() {
        TrackIdMapper.putSpotify(
            spotifyId = "sp_456",
            isrc = null,
            title = "Starboy (feat. Daft Punk)",
            artistName = "The Weeknd, Daft Punk"
        )
        TrackIdMapper.putYoutube(
            youtubeId = "34Na4j8AVgA",
            isrc = null,
            title = "Starboy [Official Video]",
            artistName = "The Weeknd"
        )

        assertEquals("34Na4j8AVgA", TrackIdMapper.spotifyToYoutube("sp_456"))
        assertEquals("sp_456", TrackIdMapper.youtubeToSpotify("34Na4j8AVgA"))

        val trackFromSpotify = Track(
            id = "sp_456",
            title = "Starboy",
            artists = listOf(Artist(name = "The Weeknd")),
            spotifyId = "sp_456"
        )
        assertEquals("34Na4j8AVgA", TrackIdMapper.resolveCachedYoutubeId(trackFromSpotify))
    }

    @Test
    fun entriesExpireAfter24Hours() {
        TrackIdMapper.putSpotify("sp_exp", "ISRC00001", "Song", "Artist")
        TrackIdMapper.putYoutube("yt_exp12345", "ISRC00001", "Song", "Artist")

        assertEquals("yt_exp12345", TrackIdMapper.spotifyToYoutube("sp_exp"))

        // Advance clock past 24h TTL
        fakeTimeMs += 25 * 3600_000L
        assertNull(TrackIdMapper.spotifyToYoutube("sp_exp"))
        assertNull(TrackIdMapper.findByIsrc("ISRC00001"))
    }
}
