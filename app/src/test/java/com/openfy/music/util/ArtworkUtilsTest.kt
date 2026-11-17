package com.openfy.music.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkUtilsTest {

    @Test
    fun testYouTubeMusicUrlUpscaling() {
        // Standard YouTube Music / Google user content with w/h parameters
        val url1 = "https://lh3.googleusercontent.com/f9-90b_vK6r2h=w120-h120-l90-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/f9-90b_vK6r2h=w1024-h1024-l90-rj",
            ArtworkUtils.getHighResArtworkUrl(url1)
        )

        val url2 = "https://lh3.googleusercontent.com/some_hash=w544-h544-l90-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/some_hash=w1024-h1024-l90-rj",
            ArtworkUtils.getHighResArtworkUrl(url2)
        )

        val url3 = "https://lh3.googleusercontent.com/image_id=w60-h60"
        assertEquals(
            "https://lh3.googleusercontent.com/image_id=w1024-h1024-l90-rj",
            ArtworkUtils.getHighResArtworkUrl(url3)
        )

        // Dimension specifiers with =s
        val url4 = "https://lh3.googleusercontent.com/avatar=s544-c-k-c0x00ffffff-no-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/avatar=s1200",
            ArtworkUtils.getHighResArtworkUrl(url4)
        )

        val url5 = "https://yt3.ggpht.com/channel_avatar=s120"
        assertEquals(
            "https://yt3.ggpht.com/channel_avatar=s1200",
            ArtworkUtils.getHighResArtworkUrl(url5)
        )
    }

    @Test
    fun testYouTubeVideoThumbnailUpscalingAndFallback() {
        val hqUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        assertTrue(ArtworkUtils.isYouTubeVideoThumbnail(hqUrl))
        assertEquals(
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
            ArtworkUtils.getHighResArtworkUrl(hqUrl)
        )
        assertEquals(
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            ArtworkUtils.getFallbackArtworkUrl(hqUrl)
        )

        val mqUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg"
        assertEquals(
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
            ArtworkUtils.getHighResArtworkUrl(mqUrl)
        )
        assertEquals(
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            ArtworkUtils.getFallbackArtworkUrl(mqUrl)
        )

        val webpUrl = "https://i.ytimg.com/vi_webp/dQw4w9WgXcQ/hqdefault.webp"
        assertEquals(
            "https://i.ytimg.com/vi_webp/dQw4w9WgXcQ/maxresdefault.jpg",
            ArtworkUtils.getHighResArtworkUrl(webpUrl)
        )
        assertEquals(
            "https://i.ytimg.com/vi_webp/dQw4w9WgXcQ/hqdefault.jpg",
            ArtworkUtils.getFallbackArtworkUrl(webpUrl)
        )
    }

    @Test
    fun testSpotifyUrlUpscaling() {
        // Track/Album 300x300 -> 640x640
        val spotify300 = "https://i.scdn.co/image/ab67616d00001e028863bc11d2aa12b54f5aeb36"
        assertEquals(
            "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36",
            ArtworkUtils.getHighResArtworkUrl(spotify300)
        )

        // Track/Album 64x64 -> 640x640
        val spotify64 = "https://i.scdn.co/image/ab67616d000048518863bc11d2aa12b54f5aeb36"
        assertEquals(
            "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36",
            ArtworkUtils.getHighResArtworkUrl(spotify64)
        )

        // Artist avatar 320x320 -> 640x640
        val artist320 = "https://i.scdn.co/image/ab676161000051748863bc11d2aa12b54f5aeb36"
        assertEquals(
            "https://i.scdn.co/image/ab6761610000e5eb8863bc11d2aa12b54f5aeb36",
            ArtworkUtils.getHighResArtworkUrl(artist320)
        )

        // Already 640x640 unchanged
        val spotify640 = "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36"
        assertEquals(
            spotify640,
            ArtworkUtils.getHighResArtworkUrl(spotify640)
        )
    }

    @Test
    fun testEdgeCases() {
        assertEquals(null, ArtworkUtils.getHighResArtworkUrl(null))
        assertEquals("", ArtworkUtils.getHighResArtworkUrl(""))
        assertEquals("   ", ArtworkUtils.getHighResArtworkUrl("   "))
        assertFalse(ArtworkUtils.isYouTubeVideoThumbnail(null))
        assertFalse(ArtworkUtils.isYouTubeVideoThumbnail("https://example.com/image.png"))
    }
}
