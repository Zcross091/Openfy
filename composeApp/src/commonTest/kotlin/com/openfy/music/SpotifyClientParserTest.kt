package com.openfy.music

import com.openfy.music.data.TrackIdMapper
import com.openfy.music.data.remote.NetworkClient
import com.openfy.music.data.remote.SpotifyClient
import kotlinx.serialization.json.jsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpotifyClientParserTest {
    @BeforeTest
    fun setUp() {
        TrackIdMapper.clear()
    }

    @Test
    fun parsesSavedTracksWithIsrcAndPopulatesMapper() {
        val sampleJson = """
            {
              "total": 1,
              "items": [
                {
                  "added_at": "2025-11-01T10:00:00Z",
                  "track": {
                    "id": "0VjIjW4GlUZAMYd2vXMi3b",
                    "name": "Blinding Lights",
                    "duration_ms": 200040,
                    "external_ids": {
                      "isrc": "USUG11904206"
                    },
                    "artists": [
                      { "id": "1Xyo4u8uXC1ZmMpatF05PJ", "name": "The Weeknd" }
                    ],
                    "album": {
                      "name": "After Hours",
                      "images": [
                        { "url": "https://i.scdn.co/image/small", "width": 64, "height": 64 },
                        { "url": "https://i.scdn.co/image/large", "width": 640, "height": 640 }
                      ]
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val jsonObj = NetworkClient.json.parseToJsonElement(sampleJson).jsonObject
        val tracks = SpotifyClient.parseSavedTracksPage(jsonObj, isLiked = true)

        assertEquals(1, tracks.size)
        val t = tracks.first()
        assertEquals("0VjIjW4GlUZAMYd2vXMi3b", t.id)
        assertEquals("0VjIjW4GlUZAMYd2vXMi3b", t.spotifyId)
        assertEquals("Blinding Lights", t.title)
        assertEquals("The Weeknd", t.artistNames)
        assertEquals("After Hours", t.album?.name)
        assertEquals(200, t.durationSeconds)
        assertEquals("USUG11904206", t.isrc)
        assertTrue(t.isLiked)

        val mapped = TrackIdMapper.findByIsrc("USUG11904206")
        assertEquals("0VjIjW4GlUZAMYd2vXMi3b", mapped?.spotifyId)
    }

    @Test
    fun parsesUserPlaylistsPageCorrectly() {
        val sampleJson = """
            {
              "total": 2,
              "items": [
                {
                  "id": "37i9dQZF1DXcBWIGoYBM5M",
                  "name": "Workout Mix",
                  "description": "High energy tracks",
                  "images": [
                    { "url": "https://i.scdn.co/image/workout", "width": 300, "height": 300 }
                  ],
                  "tracks": { "total": 42 }
                },
                {
                  "id": "5ABHKGoOzxkaa28ttQV9sE",
                  "name": "Late Night Drives",
                  "description": null,
                  "images": [],
                  "tracks": { "total": 18 }
                }
              ]
            }
        """.trimIndent()

        val jsonObj = NetworkClient.json.parseToJsonElement(sampleJson).jsonObject
        val playlists = SpotifyClient.parsePlaylistsPage(jsonObj)

        assertEquals(2, playlists.size)
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", playlists[0].id)
        assertEquals("Workout Mix", playlists[0].title)
        assertEquals(42, playlists[0].trackCount)
        assertEquals("spotify", playlists[0].source)

        assertEquals("Late Night Drives", playlists[1].title)
        assertEquals(18, playlists[1].trackCount)
    }
}
