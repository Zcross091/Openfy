package com.openfy.music

import com.openfy.music.data.TrackIdMapper
import com.openfy.music.data.remote.NetworkClient
import com.openfy.music.data.remote.YouTubeAudioResolver
import kotlinx.serialization.json.jsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YouTubeInnerTubeParserTest {
    @BeforeTest
    fun setUp() {
        TrackIdMapper.clear()
    }

    @Test
    fun parsesLikedPlaylistsAndStripsVlPrefixAndSyntheticEntries() {
        val sampleJson = """
            {
              "contents": {
                "singleColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "gridRenderer": {
                                  "items": [
                                    {
                                      "musicTwoRowItemRenderer": {
                                        "title": { "runs": [{ "text": "Your Likes" }] },
                                        "subtitle": { "runs": [{ "text": "Auto playlist" }] },
                                        "navigationEndpoint": {
                                          "browseEndpoint": { "browseId": "VLLM" }
                                        }
                                      }
                                    },
                                    {
                                      "musicTwoRowItemRenderer": {
                                        "title": { "runs": [{ "text": "Gym Bangers" }] },
                                        "subtitle": { "runs": [{ "text": "Playlist • 24 songs" }] },
                                        "thumbnailRenderer": {
                                          "musicThumbnailRenderer": {
                                            "thumbnail": {
                                              "thumbnails": [{ "url": "https://lh3.googleusercontent.com/cover" }]
                                            }
                                          }
                                        },
                                        "navigationEndpoint": {
                                          "browseEndpoint": { "browseId": "VLPLr9x_gym123" }
                                        }
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val jsonObj = NetworkClient.json.parseToJsonElement(sampleJson).jsonObject
        val playlists = YouTubeAudioResolver.parseUserPlaylistsJson(jsonObj)

        // Synthetic VLLM ("LM") should be filtered out, only editable playlist kept
        assertEquals(1, playlists.size)
        val pl = playlists.first()
        assertEquals("PLr9x_gym123", pl.id)
        assertEquals("Gym Bangers", pl.title)
        assertEquals(24, pl.trackCount)
        assertEquals("youtube", pl.source)
    }

    @Test
    fun parsesBrowseTracksFromLikedVideosAndPopulatesMapper() {
        val sampleJson = """
            {
              "contents": {
                "singleColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "musicPlaylistShelfRenderer": {
                                  "contents": [
                                    {
                                      "musicResponsiveListItemRenderer": {
                                        "playlistItemData": { "videoId": "fHI8X4OXluQ" },
                                        "flexColumns": [
                                          {
                                            "musicResponsiveListItemFlexColumnRenderer": {
                                              "text": { "runs": [{ "text": "Blinding Lights" }] }
                                            }
                                          },
                                          {
                                            "musicResponsiveListItemFlexColumnRenderer": {
                                              "text": { "runs": [{ "text": "The Weeknd" }] }
                                            }
                                          }
                                        ]
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val jsonObj = NetworkClient.json.parseToJsonElement(sampleJson).jsonObject
        val tracks = YouTubeAudioResolver.parseBrowseTracksJson(jsonObj, isLiked = true)

        assertEquals(1, tracks.size)
        assertEquals("fHI8X4OXluQ", tracks[0].youtubeId)
        assertEquals("Blinding Lights", tracks[0].title)
        assertEquals("The Weeknd", tracks[0].artistNames)
        assertTrue(tracks[0].isLiked)
        assertEquals("fHI8X4OXluQ", TrackIdMapper.findBySlug("Blinding Lights", "The Weeknd")?.youtubeId)
    }

    @Test
    fun parsesEditAndCreatePlaylistResponses() {
        val editOkJson = """
            {
              "status": "STATUS_SUCCEEDED",
              "playlistEditResults": [
                {
                  "playlistEditVideoAddedResultData": {
                    "videoId": "fHI8X4OXluQ",
                    "setVideoId": "SET_VID_999"
                  }
                }
              ]
            }
        """.trimIndent()
        val editRes = YouTubeAudioResolver.parseEditPlaylistResponse(
            NetworkClient.json.parseToJsonElement(editOkJson).jsonObject
        )
        assertTrue(editRes.succeeded)
        assertEquals("SET_VID_999", editRes.setVideoId)

        val editFailJson = """{ "status": "STATUS_FAILED" }"""
        val failRes = YouTubeAudioResolver.parseEditPlaylistResponse(
            NetworkClient.json.parseToJsonElement(editFailJson).jsonObject
        )
        assertFalse(failRes.succeeded)

        val createJson = """{ "playlistId": "PLnew_98765" }"""
        val created = YouTubeAudioResolver.parseCreatePlaylistResponse(
            NetworkClient.json.parseToJsonElement(createJson).jsonObject,
            name = "Chill Mix"
        )
        assertNotNull(created)
        assertEquals("PLnew_98765", created.id)
        assertEquals("Chill Mix", created.title)
        assertEquals("youtube", created.source)
    }
}
