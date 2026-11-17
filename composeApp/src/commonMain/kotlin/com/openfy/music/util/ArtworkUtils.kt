package com.openfy.music.util

object ArtworkUtils {
    private val YOUTUBE_MUSIC_WH_REGEX = Regex("=w\\d+-h\\d+[^?&#]*")
    private val YOUTUBE_MUSIC_S_REGEX = Regex("=s\\d+[^?&#]*")
    private val YOUTUBE_VIDEO_THUMB_REGEX = Regex("/(hqdefault|mqdefault|sddefault|default)\\.(jpg|webp)")
    private val YOUTUBE_VIDEO_ANY_THUMB_REGEX = Regex("/(maxresdefault|hqdefault|mqdefault|sddefault|default)\\.(jpg|webp)")

    /**
     * Upscales thumbnail and cover art URLs to high-resolution versions:
     * - YouTube Music: =w120-h120 / =w544-h544 -> =w1024-h1024-l90-rj, =s544 -> =s1200
     * - YouTube Video: hqdefault.jpg -> maxresdefault.jpg
     * - Spotify: ab67616d00001e02 / ab67616d00004851 -> ab67616d0000b273 (640x640)
     */
    fun getHighResArtworkUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url

        // 1. YouTube Music / Google user content dimension parameters
        if (YOUTUBE_MUSIC_WH_REGEX.containsMatchIn(url)) {
            return YOUTUBE_MUSIC_WH_REGEX.replace(url, "=w1024-h1024-l90-rj")
        }
        if (YOUTUBE_MUSIC_S_REGEX.containsMatchIn(url)) {
            return YOUTUBE_MUSIC_S_REGEX.replace(url, "=s1200")
        }
        if ((url.contains("googleusercontent.com") || url.contains("ggpht.com")) && url.contains("=")) {
            return Regex("=[^?&#]+$").replace(url, "=w1024-h1024-l90-rj")
        }

        // 2. Standard YouTube video thumbnails
        if (isYouTubeVideoThumbnail(url)) {
            return YOUTUBE_VIDEO_THUMB_REGEX.replace(url, "/maxresdefault.jpg")
        }

        // 3. Spotify track/album artwork (upscale 300x300 / 64x64 to 640x640)
        if (url.contains("i.scdn.co/image/")) {
            if (url.contains("ab67616d00001e02")) {
                return url.replace("ab67616d00001e02", "ab67616d0000b273")
            }
            if (url.contains("ab67616d00004851")) {
                return url.replace("ab67616d00004851", "ab67616d0000b273")
            }
            if (url.contains("ab67616100005174")) {
                return url.replace("ab67616100005174", "ab6761610000e5eb")
            }
            if (url.contains("ab6761610000f178")) {
                return url.replace("ab6761610000f178", "ab6761610000e5eb")
            }
        }

        return url
    }

    /**
     * Fallback URL for standard YouTube video thumbnails when maxresdefault.jpg returns 404.
     */
    fun getFallbackArtworkUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        if (isYouTubeVideoThumbnail(url)) {
            return YOUTUBE_VIDEO_ANY_THUMB_REGEX.replace(url, "/hqdefault.jpg")
        }
        return url
    }

    /**
     * Checks if the given URL is a standard YouTube video thumbnail (e.g. i.ytimg.com/vi/...).
     */
    fun isYouTubeVideoThumbnail(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return (url.contains("i.ytimg.com") || url.contains("img.youtube.com") || url.contains("/vi/")) &&
                YOUTUBE_VIDEO_ANY_THUMB_REGEX.containsMatchIn(url)
    }
}
