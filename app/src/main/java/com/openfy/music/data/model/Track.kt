package com.openfy.music.data.model

data class Artist(
    val id: String? = null,
    val name: String
)

data class Album(
    val id: String? = null,
    val name: String,
    val coverUrl: String? = null
)

data class Track(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val album: Album? = null,
    val durationSeconds: Int = 0,
    val coverUrl: String? = null,
    val source: String = "youtube",
    val youtubeId: String? = null,
    val spotifyId: String? = null,
    var isLiked: Boolean = false
) {
    val artistNames: String
        get() = artists.joinToString(", ") { it.name }
}

data class Playlist(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val source: String = "youtube",
    val tracks: List<Track> = emptyList()
)
