package com.openfy.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LyricLine(
    val timeSeconds: Float,
    val text: String
)

typealias LyricsLine = LyricLine

@Serializable
data class LyricsData(
    val trackTitle: String,
    val artistName: String,
    val isSynced: Boolean,
    val lines: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null
)
