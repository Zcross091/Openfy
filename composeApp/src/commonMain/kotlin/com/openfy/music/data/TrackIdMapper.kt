package com.openfy.music.data

import com.openfy.music.data.model.Track
import io.ktor.util.date.getTimeMillis

/**
 * Bidirectional cross-service ID mapping cache keyed primarily by ISRC,
 * falling back to a normalized "title::primaryArtist" slug.
 */
object TrackIdMapper {
    data class Entry(
        val spotifyId: String? = null,
        val youtubeId: String? = null,
        val isrc: String? = null,
        val addedMs: Long = 0L
    )

    private const val TTL_MS = 24 * 3600_000L

    private val byIsrc = mutableMapOf<String, Entry>()
    private val bySlug = mutableMapOf<String, Entry>()
    private val spotifyToIsrc = mutableMapOf<String, String>()
    private val spotifyToSlug = mutableMapOf<String, String>()
    private val youtubeToIsrc = mutableMapOf<String, String>()
    private val youtubeToSlug = mutableMapOf<String, String>()

    internal var clockMs: () -> Long = { getTimeMillis() }

    private val parentheticalRegex = Regex("\\s*[\\(\\[].*?[\\)\\]]")
    private val featRegex = Regex("\\s+(feat\\.|ft\\.|featuring)\\s+.*$", RegexOption.IGNORE_CASE)
    private val nonAlnumRegex = Regex("[^a-z0-9 ]")
    private val multiSpaceRegex = Regex("\\s+")

    fun normalizeTitle(title: String): String {
        return title
            .replace(parentheticalRegex, "")
            .replace(featRegex, "")
            .lowercase()
            .replace(nonAlnumRegex, " ")
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    fun normalizeArtist(artistName: String): String {
        val primary = artistName.split(",", ";", "&", " feat.", " ft.")
            .firstOrNull()?.trim().orEmpty()
        return primary
            .lowercase()
            .replace(nonAlnumRegex, " ")
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    fun normalizeSlug(title: String, artistName: String): String {
        return "${normalizeTitle(title)}::${normalizeArtist(artistName)}"
    }

    fun putSpotify(spotifyId: String, isrc: String?, title: String, artistName: String) {
        if (spotifyId.isBlank()) return
        val slug = normalizeSlug(title, artistName)
        val cleanIsrc = isrc?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        val newEntry = Entry(spotifyId = spotifyId, isrc = cleanIsrc, addedMs = clockMs())

        if (cleanIsrc != null) {
            byIsrc[cleanIsrc] = merge(byIsrc[cleanIsrc], newEntry)
            spotifyToIsrc[spotifyId] = cleanIsrc
        }
        bySlug[slug] = merge(bySlug[slug], newEntry)
        spotifyToSlug[spotifyId] = slug
    }

    fun putYoutube(youtubeId: String, isrc: String?, title: String, artistName: String) {
        if (youtubeId.isBlank()) return
        val slug = normalizeSlug(title, artistName)
        val cleanIsrc = isrc?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        val newEntry = Entry(youtubeId = youtubeId, isrc = cleanIsrc, addedMs = clockMs())

        if (cleanIsrc != null) {
            byIsrc[cleanIsrc] = merge(byIsrc[cleanIsrc], newEntry)
            youtubeToIsrc[youtubeId] = cleanIsrc
        }
        bySlug[slug] = merge(bySlug[slug], newEntry)
        youtubeToSlug[youtubeId] = slug
    }

    fun putPair(spotifyId: String?, youtubeId: String?, isrc: String?, title: String, artistName: String) {
        if (!spotifyId.isNullOrBlank()) putSpotify(spotifyId, isrc, title, artistName)
        if (!youtubeId.isNullOrBlank()) putYoutube(youtubeId, isrc, title, artistName)
    }

    fun spotifyToYoutube(spotifyId: String): String? {
        spotifyToIsrc[spotifyId]?.let { isrc ->
            byIsrc[isrc]?.takeIf { !expired(it) }?.youtubeId?.let { return it }
        }
        spotifyToSlug[spotifyId]?.let { slug ->
            bySlug[slug]?.takeIf { !expired(it) }?.youtubeId?.let { return it }
        }
        return (byIsrc.values.asSequence() + bySlug.values.asSequence())
            .firstOrNull { it.spotifyId == spotifyId && !expired(it) && !it.youtubeId.isNullOrEmpty() }
            ?.youtubeId
    }

    fun youtubeToSpotify(youtubeId: String): String? {
        youtubeToIsrc[youtubeId]?.let { isrc ->
            byIsrc[isrc]?.takeIf { !expired(it) }?.spotifyId?.let { return it }
        }
        youtubeToSlug[youtubeId]?.let { slug ->
            bySlug[slug]?.takeIf { !expired(it) }?.spotifyId?.let { return it }
        }
        return (byIsrc.values.asSequence() + bySlug.values.asSequence())
            .firstOrNull { it.youtubeId == youtubeId && !expired(it) && !it.spotifyId.isNullOrEmpty() }
            ?.spotifyId
    }

    fun findByIsrc(isrc: String): Entry? {
        val clean = isrc.trim().uppercase()
        return byIsrc[clean]?.takeIf { !expired(it) }
    }

    fun findBySlug(title: String, artistName: String): Entry? {
        return bySlug[normalizeSlug(title, artistName)]?.takeIf { !expired(it) }
    }

    fun resolveCachedYoutubeId(track: Track): String? {
        track.youtubeId?.takeIf { it.isNotBlank() }?.let { return it }
        track.isrc?.let { findByIsrc(it)?.youtubeId }?.let { return it }
        track.spotifyId?.let { spotifyToYoutube(it) }?.let { return it }
        findBySlug(track.title, track.artistNames)?.youtubeId?.let { return it }
        return null
    }

    fun resolveCachedSpotifyId(track: Track): String? {
        track.spotifyId?.takeIf { it.isNotBlank() }?.let { return it }
        track.isrc?.let { findByIsrc(it)?.spotifyId }?.let { return it }
        track.youtubeId?.let { youtubeToSpotify(it) }?.let { return it }
        findBySlug(track.title, track.artistNames)?.spotifyId?.let { return it }
        return null
    }

    fun clear() {
        byIsrc.clear()
        bySlug.clear()
        spotifyToIsrc.clear()
        spotifyToSlug.clear()
        youtubeToIsrc.clear()
        youtubeToSlug.clear()
    }

    private fun merge(existing: Entry?, new: Entry): Entry {
        val active = existing?.takeIf { !expired(it) }
        return Entry(
            spotifyId = new.spotifyId ?: active?.spotifyId,
            youtubeId = new.youtubeId ?: active?.youtubeId,
            isrc = new.isrc ?: active?.isrc,
            addedMs = new.addedMs
        )
    }

    private fun expired(e: Entry): Boolean = (clockMs() - e.addedMs) > TTL_MS
}
