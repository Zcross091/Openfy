package com.openfy.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String? = null,
    val name: String,
    val avatarUrl: String? = null,
    val followers: Int = 0
)

@Serializable
data class Album(
    val id: String? = null,
    val name: String,
    val coverUrl: String? = null,
    val artistName: String? = null,
    val releaseYear: String? = null
)

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val album: Album? = null,
    val durationSeconds: Int = 0,
    val coverUrl: String? = null,
    val source: String = "youtube",
    val youtubeId: String? = null,
    val spotifyId: String? = null,
    var isLiked: Boolean = false
) {
    val artistNames: String
        get() = if (artists.isEmpty()) "Unknown Artist" else artists.joinToString(", ") { it.name }
}

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val source: String = "youtube",
    val tracks: List<Track> = emptyList()
)
