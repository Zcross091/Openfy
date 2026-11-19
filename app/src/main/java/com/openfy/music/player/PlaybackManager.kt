package com.openfy.music.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.openfy.music.OpenfyApplication
import com.openfy.music.data.StreamingClient
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.YouTubeAudioResolver
import com.openfy.music.service.MusicService
import com.openfy.music.util.ArtworkUtils
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object PlaybackManager {
    private var exoPlayer: ExoPlayer? = null
    private var pendingMediaItem: MediaItem? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue = _queue.asStateFlow()

    private var currentIndex = -1

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0: off, 1: all, 2: one
    val repeatMode = _repeatMode.asStateFlow()

    private val failedClientsForCurrentTrack = mutableSetOf<StreamingClient>()

    private val _currentStreamingClient = MutableStateFlow<StreamingClient>(StreamingClient.AUTO)
    val currentStreamingClient = _currentStreamingClient.asStateFlow()

    private val _engineStatus = MutableStateFlow<String?>("RVX Audio Engine")
    val engineStatus = _engineStatus.asStateFlow()

    fun setPlayer(player: ExoPlayer) {
        exoPlayer = player
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) startProgressTicker()
                else stopProgressTicker()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _isLoading.value = false
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                } else if (state == Player.STATE_BUFFERING) {
                    _isLoading.value = true
                } else if (state == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                stopProgressTicker()
                _currentTrack.value?.let { track ->
                    val key = track.youtubeId ?: track.id
                    YouTubeAudioResolver.invalidateCache(key)
                    val failedClient = _currentStreamingClient.value
                    failedClientsForCurrentTrack.add(failedClient)
                    if (failedClientsForCurrentTrack.size < 5) {
                        _engineStatus.value = "Error on ${failedClient.title}, failing over..."
                        attemptFallbackPlayback(track)
                    } else {
                        _isLoading.value = false
                        _isPlaying.value = false
                        _engineStatus.value = "Playback error"
                    }
                } ?: run {
                    _isLoading.value = false
                    _isPlaying.value = false
                }
            }
        })

        // Instantly launch pending media item if user tapped a song before service initialized
        pendingMediaItem?.let { item ->
            pendingMediaItem = null
            player.setMediaItem(item)
            player.prepare()
            player.play()
        }
    }

    private fun ensureServiceRunning() {
        val context = OpenfyApplication.instance
        val intent = Intent(context, MusicService::class.java)
        context.startService(intent)
    }

    private fun applyMediaItem(track: Track, streamUrl: String) {
        val highResCover = ArtworkUtils.getHighResArtworkUrl(track.coverUrl)
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistNames)
            .setAlbumTitle(track.album?.name ?: "Openfy")
            .apply {
                if (highResCover != null) {
                    try {
                        setArtworkUri(Uri.parse(highResCover))
                    } catch (_: Exception) {}
                }
            }
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()

        val player = exoPlayer
        if (player != null) {
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } else {
            // Service is still spinning up; queue it for instant playback on service ready
            pendingMediaItem = mediaItem
        }
    }

    private fun attemptFallbackPlayback(track: Track) {
        scope.launch {
            _isLoading.value = true
            val resolved = YouTubeAudioResolver.resolveStream(
                track = track,
                failedClients = failedClientsForCurrentTrack
            )
            if (resolved != null) {
                _currentStreamingClient.value = resolved.client
                _engineStatus.value = "Active: ${resolved.client.title}"
                applyMediaItem(track, resolved.url)
            } else {
                _isLoading.value = false
                _isPlaying.value = false
                _engineStatus.value = "All stream fallbacks exhausted"
            }
        }
    }

    fun switchSource() {
        val track = _currentTrack.value ?: return
        val current = _currentStreamingClient.value
        failedClientsForCurrentTrack.add(current)
        if (failedClientsForCurrentTrack.size >= 5) {
            failedClientsForCurrentTrack.clear()
        }
        attemptFallbackPlayback(track)
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        ensureServiceRunning()
        _currentTrack.value = track
        _isLoading.value = true
        failedClientsForCurrentTrack.clear()

        if (newQueue != null) {
            _queue.value = newQueue
            currentIndex = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value = _queue.value + track
            currentIndex = _queue.value.lastIndex
        }

        scope.launch {
            try {
                if (track.id.startsWith("LOCAL:")) {
                    applyMediaItem(track, track.id.removePrefix("LOCAL:"))
                    _currentStreamingClient.value = StreamingClient.AUTO
                    _engineStatus.value = "Local Storage"
                } else {
                    val resolved = YouTubeAudioResolver.resolveStream(track)
                    if (resolved != null) {
                        _currentStreamingClient.value = resolved.client
                        _engineStatus.value = "Active: ${resolved.client.title}"
                        applyMediaItem(track, resolved.url)
                    } else {
                        _isLoading.value = false
                        _engineStatus.value = "Stream unavailable"
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun togglePlay() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause()
            else player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        var nextIndex = currentIndex + 1
        if (_isShuffle.value) {
            nextIndex = q.indices.random()
        }
        if (nextIndex >= q.size) {
            if (_repeatMode.value == 1) nextIndex = 0
            else return
        }
        currentIndex = nextIndex
        playTrack(q[nextIndex])
    }

    fun skipPrevious() {
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }
        val q = _queue.value
        if (q.isEmpty()) return
        var prevIndex = currentIndex - 1
        if (prevIndex < 0) {
            prevIndex = q.lastIndex
        }
        currentIndex = prevIndex
        playTrack(q[prevIndex])
    }

    private fun handleTrackEnded() {
        if (_repeatMode.value == 2) {
            seekTo(0)
            exoPlayer?.play()
        } else {
            skipNext()
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _currentPositionMs.value = player.currentPosition
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }
}
