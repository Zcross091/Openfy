package com.openfy.music.player

import com.openfy.music.data.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val isShuffle: Boolean = false,
    val repeatMode: Int = 0 // 0: off, 1: all, 2: one
) {
    val progress: Float
        get() = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
