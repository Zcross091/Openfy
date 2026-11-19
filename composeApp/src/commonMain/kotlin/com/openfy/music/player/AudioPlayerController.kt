package com.openfy.music.player

import com.openfy.music.data.StreamingClient
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.YouTubeAudioResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface NativeAudioPlayer {
    fun playStream(url: String, track: Track)
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun stop()
}

interface AudioPlayerController {
    val playbackState: StateFlow<PlaybackState>

    val currentTrack: StateFlow<Track?>
    val isPlaying: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val queue: StateFlow<List<Track>>
    val isShuffle: StateFlow<Boolean>
    val repeatMode: StateFlow<Int>

    fun playTrack(track: Track, newQueue: List<Track>? = null)
    fun togglePlay()
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun toggleShuffle()
    fun toggleRepeat()
}

object PlaybackManager : AudioPlayerController {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var nativePlayer: NativeAudioPlayer? = null
    private var customController: AudioPlayerController? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading = _isLoading.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    override val queue = _queue.asStateFlow()

    private var currentIndex = -1

    private val _isShuffle = MutableStateFlow(false)
    override val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0: off, 1: all, 2: one
    override val repeatMode = _repeatMode.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val failedClientsForCurrentTrack = mutableSetOf<StreamingClient>()

    private val _currentStreamingClient = MutableStateFlow<StreamingClient>(StreamingClient.AUTO)
    val currentStreamingClient: StateFlow<StreamingClient> = _currentStreamingClient.asStateFlow()

    private val _engineStatus = MutableStateFlow<String?>("RVX Audio Engine")
    val engineStatus: StateFlow<String?> = _engineStatus.asStateFlow()

    fun setNativePlayer(player: NativeAudioPlayer) {
        this.nativePlayer = player
    }

    fun setController(controller: AudioPlayerController) {
        this.customController = controller
    }

    fun updatePlaybackProgress(positionMs: Long, durationMs: Long) {
        _currentPositionMs.value = positionMs
        if (durationMs > 0) {
            _durationMs.value = durationMs
        }
        syncState()
    }

    fun updatePlaybackState(isPlaying: Boolean, isLoading: Boolean = false) {
        _isPlaying.value = isPlaying
        _isLoading.value = isLoading
        if (isPlaying) startProgressTicker() else stopProgressTicker()
        syncState()
    }

    fun onTrackEnded() {
        handleTrackEnded()
    }

    fun onPlaybackError() {
        _currentTrack.value?.let { track ->
            val key = track.youtubeId ?: track.id
            YouTubeAudioResolver.invalidateCache(key)
            val current = _currentStreamingClient.value
            failedClientsForCurrentTrack.add(current)
            if (failedClientsForCurrentTrack.size < 6) {
                _engineStatus.value = "Error on ${current.title}, failing over..."
                playWithFallback(track)
            } else {
                _isLoading.value = false
                _isPlaying.value = false
                _engineStatus.value = "Playback error"
                syncState()
            }
        }
    }

    private fun playWithFallback(track: Track) {
        _isLoading.value = true
        syncState()
        scope.launch(Dispatchers.IO) {
            try {
                val resolved = YouTubeAudioResolver.resolveStream(
                    track = track,
                    failedClients = failedClientsForCurrentTrack
                )
                if (resolved != null) {
                    _currentStreamingClient.value = resolved.client
                    _engineStatus.value = "Active: ${resolved.client.title}"
                    nativePlayer?.playStream(resolved.url, track)
                    _isLoading.value = false
                    _isPlaying.value = true
                    startProgressTicker()
                } else {
                    _isLoading.value = false
                    _isPlaying.value = false
                    _engineStatus.value = "All audio sources exhausted"
                }
            } catch (_: Exception) {
                _isLoading.value = false
            }
            syncState()
        }
    }

    fun switchSource() {
        val track = _currentTrack.value ?: return
        val current = _currentStreamingClient.value
        failedClientsForCurrentTrack.add(current)
        if (failedClientsForCurrentTrack.size >= 6) {
            failedClientsForCurrentTrack.clear()
        }
        playWithFallback(track)
    }

    private fun syncState() {
        _playbackState.value = PlaybackState(
            currentTrack = _currentTrack.value,
            isPlaying = _isPlaying.value,
            isLoading = _isLoading.value,
            currentPositionMs = _currentPositionMs.value,
            durationMs = _durationMs.value,
            queue = _queue.value,
            isShuffle = _isShuffle.value,
            repeatMode = _repeatMode.value
        )
    }

    override fun playTrack(track: Track, newQueue: List<Track>?) {
        customController?.let {
            it.playTrack(track, newQueue)
            return
        }

        _currentTrack.value = track
        _isLoading.value = true
        _currentPositionMs.value = 0L
        _durationMs.value = (track.durationSeconds * 1000L).coerceAtLeast(0L)
        failedClientsForCurrentTrack.clear()

        if (newQueue != null) {
            _queue.value = newQueue
            currentIndex = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value = _queue.value + track
            currentIndex = _queue.value.lastIndex
        }
        syncState()

        scope.launch(Dispatchers.IO) {
            try {
                if (track.id.startsWith("LOCAL:")) {
                    nativePlayer?.playStream(track.id.removePrefix("LOCAL:"), track)
                    _currentStreamingClient.value = StreamingClient.AUTO
                    _engineStatus.value = "Local Storage"
                    _isLoading.value = false
                    _isPlaying.value = true
                    startProgressTicker()
                } else {
                    val resolved = YouTubeAudioResolver.resolveStream(track)
                    if (resolved != null) {
                        _currentStreamingClient.value = resolved.client
                        _engineStatus.value = "Active: ${resolved.client.title}"
                        nativePlayer?.playStream(resolved.url, track)
                        _isLoading.value = false
                        _isPlaying.value = true
                        startProgressTicker()
                    } else {
                        _isLoading.value = false
                        _engineStatus.value = "Stream unavailable"
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
            syncState()
        }
    }

    override fun togglePlay() {
        customController?.let {
            it.togglePlay()
            return
        }
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    override fun pause() {
        customController?.let {
            it.pause()
            return
        }
        nativePlayer?.pause()
        _isPlaying.value = false
        stopProgressTicker()
        syncState()
    }

    override fun resume() {
        customController?.let {
            it.resume()
            return
        }
        nativePlayer?.resume()
        _isPlaying.value = true
        startProgressTicker()
        syncState()
    }

    override fun seekTo(positionMs: Long) {
        customController?.let {
            it.seekTo(positionMs)
            return
        }
        nativePlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
        syncState()
    }

    override fun skipNext() {
        customController?.let {
            it.skipNext()
            return
        }
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

    override fun skipPrevious() {
        customController?.let {
            it.skipPrevious()
            return
        }
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
            resume()
        } else {
            skipNext()
        }
    }

    override fun toggleShuffle() {
        customController?.let {
            it.toggleShuffle()
            return
        }
        _isShuffle.value = !_isShuffle.value
        syncState()
    }

    override fun toggleRepeat() {
        customController?.let {
            it.toggleRepeat()
            return
        }
        _repeatMode.value = (_repeatMode.value + 1) % 3
        syncState()
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    val current = _currentPositionMs.value + 500
                    val dur = _durationMs.value
                    if (dur > 0 && current >= dur) {
                        handleTrackEnded()
                    } else {
                        _currentPositionMs.value = current
                        syncState()
                    }
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
