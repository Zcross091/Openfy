package com.openfy.music.data.model

enum class AccountProvider {
    SPOTIFY,
    YOUTUBE,
    GUEST
}

data class UserProfile(
    val id: String,
    val displayName: String,
    val channel: String? = null,
    val avatarUrl: String? = null
)

data class LyricLine(
    val timeSeconds: Float,
    val text: String
)

data class LyricsData(
    val trackTitle: String,
    val artistName: String,
    val isSynced: Boolean,
    val lines: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null
)
