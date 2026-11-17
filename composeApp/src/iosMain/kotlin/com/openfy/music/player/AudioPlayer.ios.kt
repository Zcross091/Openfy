@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.openfy.music.player

import com.openfy.music.data.model.Track
import com.openfy.music.util.ArtworkUtils
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive
import platform.AVFoundation.*
import platform.CoreGraphics.CGSize
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSData
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.UIKit.UIImage
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

/**
 * Native iOS Audio Player powered by AVPlayer, AVPlayerItem, AVAudioSession,
 * MPNowPlayingInfoCenter, and MPRemoteCommandCenter.
 *
 * Implements [NativeAudioPlayer] and provides background playback support,
 * lock screen scrubber controls, Control Center updates, and remote accessory commands.
 */
object IosAudioPlayer : NativeAudioPlayer {

    private var player: AVPlayer? = null
    private var currentItem: AVPlayerItem? = null
    private var timeObserver: Any? = null
    private var currentTrack: Track? = null
    private var endNotificationObserver: Any? = null
    private var isInitialized = false

    /**
     * Initializes audio session, remote command handlers, and notification observers,
     * registering itself with [PlaybackManager].
     */
    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        configureAudioSession()
        setupRemoteCommandCenter()
        setupNotificationObservers()
        PlaybackManager.setNativePlayer(this)
    }

    /**
     * Configures AVAudioSession with [AVAudioSessionCategoryPlayback] so audio continues
     * seamlessly in the background and when the screen is locked.
     */
    fun configureAudioSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(
                category = AVAudioSessionCategoryPlayback,
                mode = AVAudioSessionModeDefault,
                options = 0u,
                error = null
            )
            session.setActive(true, null)
        } catch (e: Throwable) {
            println("[IosAudioPlayer] Audio session activation error: ${e.message}")
        }
    }

    /**
     * Plays a resolved HTTPS stream URL for the specified [track].
     */
    override fun playStream(url: String, track: Track) {
        initialize()
        configureAudioSession()
        currentTrack = track

        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            println("[IosAudioPlayer] Invalid audio URL: $url")
            PlaybackManager.updatePlaybackState(isPlaying = false, isLoading = false)
            return
        }

        val item = AVPlayerItem.playerItemWithURL(nsUrl)
        currentItem = item

        val currentPlayer = player
        if (currentPlayer == null) {
            val newPlayer = AVPlayer.playerWithPlayerItem(item) ?: AVPlayer(uRL = nsUrl)
            player = newPlayer
            setupTimeObserver(newPlayer)
        } else {
            currentPlayer.replaceCurrentItemWithPlayerItem(item)
        }

        player?.play()
        PlaybackManager.updatePlaybackState(isPlaying = true, isLoading = false)

        val durationSec = if (track.durationSeconds > 0) track.durationSeconds.toDouble() else 0.0
        updateNowPlayingInfo(
            track = track,
            isPlaying = true,
            playbackPositionSeconds = 0.0,
            durationSeconds = durationSec
        )
    }

    override fun pause() {
        player?.pause()
        PlaybackManager.updatePlaybackState(isPlaying = false, isLoading = false)
        val curSec = currentPositionSeconds()
        val durSec = currentDurationSeconds()
        currentTrack?.let {
            updateNowPlayingInfo(
                track = it,
                isPlaying = false,
                playbackPositionSeconds = curSec,
                durationSeconds = durSec
            )
        }
    }

    override fun resume() {
        configureAudioSession()
        player?.play()
        PlaybackManager.updatePlaybackState(isPlaying = true, isLoading = false)
        val curSec = currentPositionSeconds()
        val durSec = currentDurationSeconds()
        currentTrack?.let {
            updateNowPlayingInfo(
                track = it,
                isPlaying = true,
                playbackPositionSeconds = curSec,
                durationSeconds = durSec
            )
        }
    }

    override fun seekTo(positionMs: Long) {
        val targetSeconds = positionMs.toDouble() / 1000.0
        val cmTime = CMTimeMakeWithSeconds(targetSeconds, 1000)
        player?.seekToTime(cmTime)
        val durSec = currentDurationSeconds()
        val isPlaying = (player?.rate ?: 0f) != 0f
        currentTrack?.let {
            updateNowPlayingInfo(
                track = it,
                isPlaying = isPlaying,
                playbackPositionSeconds = targetSeconds,
                durationSeconds = durSec
            )
        }
    }

    override fun stop() {
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        currentItem = null
        currentTrack = null
        PlaybackManager.updatePlaybackState(isPlaying = false, isLoading = false)
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    /**
     * Updates lock screen, Control Center, and connected accessory info via [MPNowPlayingInfoCenter].
     */
    fun updateNowPlayingInfo(
        track: Track,
        isPlaying: Boolean,
        playbackPositionSeconds: Double,
        durationSeconds: Double
    ) {
        val info = mutableMapOf<Any?, Any?>()
        info[MPMediaItemPropertyTitle] = track.title
        info[MPMediaItemPropertyArtist] = track.artistNames
        track.album?.name?.let { info[MPMediaItemPropertyAlbumTitle] = it }
        info[MPMediaItemPropertyPlaybackDuration] = NSNumber(double = durationSeconds)
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = NSNumber(double = playbackPositionSeconds)
        info[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(double = if (isPlaying) 1.0 else 0.0)

        // Asynchronously load artwork if URL is available
        val coverUrl = ArtworkUtils.getHighResArtworkUrl(track.coverUrl)
        if (!coverUrl.isNullOrEmpty()) {
            loadArtworkAsync(coverUrl) { image ->
                if (image != null) {
                    try {
                        val artwork = MPMediaItemArtwork(boundsSize = image.size) { image }
                        val currentInfo = MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo?.toMutableMap()
                            ?: mutableMapOf<Any?, Any?>()
                        currentInfo[MPMediaItemPropertyArtwork] = artwork
                        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = currentInfo
                    } catch (e: Throwable) {
                        println("[IosAudioPlayer] Artwork creation error: ${e.message}")
                    }
                }
            }
        }

        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
    }

    private fun loadArtworkAsync(url: String, onLoaded: (UIImage?) -> Unit) {
        val nsUrl = NSURL.URLWithString(url) ?: run {
            onLoaded(null)
            return
        }
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            val data = NSData.dataWithContentsOfURL(nsUrl)
            val image = data?.let { UIImage(data = it) }
            dispatch_async(dispatch_get_main_queue()) {
                onLoaded(image)
            }
        }
    }

    /**
     * Sets up [MPRemoteCommandCenter] handlers for play, pause, togglePlayPause,
     * next, previous, and position scrubbing.
     */
    private fun setupRemoteCommandCenter() {
        val rcc = MPRemoteCommandCenter.sharedCommandCenter()

        rcc.playCommand.setEnabled(true)
        rcc.playCommand.addTargetWithHandler { _ ->
            PlaybackManager.resume()
            MPRemoteCommandHandlerStatusSuccess
        }

        rcc.pauseCommand.setEnabled(true)
        rcc.pauseCommand.addTargetWithHandler { _ ->
            PlaybackManager.pause()
            MPRemoteCommandHandlerStatusSuccess
        }

        rcc.togglePlayPauseCommand.setEnabled(true)
        rcc.togglePlayPauseCommand.addTargetWithHandler { _ ->
            PlaybackManager.togglePlay()
            MPRemoteCommandHandlerStatusSuccess
        }

        rcc.nextTrackCommand.setEnabled(true)
        rcc.nextTrackCommand.addTargetWithHandler { _ ->
            PlaybackManager.skipNext()
            MPRemoteCommandHandlerStatusSuccess
        }

        rcc.previousTrackCommand.setEnabled(true)
        rcc.previousTrackCommand.addTargetWithHandler { _ ->
            PlaybackManager.skipPrevious()
            MPRemoteCommandHandlerStatusSuccess
        }

        rcc.changePlaybackPositionCommand.setEnabled(true)
        rcc.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val posEvent = event as? MPChangePlaybackPositionCommandEvent
            if (posEvent != null) {
                val targetSec = posEvent.positionTime
                PlaybackManager.seekTo((targetSec * 1000.0).toLong())
                MPRemoteCommandHandlerStatusSuccess
            } else {
                MPRemoteCommandHandlerStatusCommandFailed
            }
        }
    }

    private fun setupNotificationObservers() {
        if (endNotificationObserver == null) {
            endNotificationObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                name = AVPlayerItemDidPlayToEndTimeNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { _ ->
                PlaybackManager.onTrackEnded()
            }

            NSNotificationCenter.defaultCenter.addObserverForName(
                name = AVPlayerItemFailedToPlayToEndTimeNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { _ ->
                PlaybackManager.updatePlaybackState(isPlaying = false, isLoading = false)
                currentTrack?.let { track ->
                    val key = track.youtubeId ?: track.id
                    YouTubeAudioResolver.invalidateCache(key)
                }
            }
        }
    }

    private fun setupTimeObserver(avPlayer: AVPlayer) {
        removeTimeObserver()
        val interval = CMTimeMake(value = 1, timescale = 2) // Every 0.5s
        timeObserver = avPlayer.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = dispatch_get_main_queue()
        ) { time ->
            val posSec = CMTimeGetSeconds(time)
            if (!posSec.isNaN() && !posSec.isInfinite() && posSec >= 0) {
                val posMs = (posSec * 1000.0).toLong()
                val durSec = currentDurationSeconds()
                val durMs = (durSec * 1000.0).toLong()
                PlaybackManager.updatePlaybackProgress(posMs, durMs)
            }
        }
    }

    private fun removeTimeObserver() {
        timeObserver?.let {
            player?.removeTimeObserver(it)
            timeObserver = null
        }
    }

    private fun currentPositionSeconds(): Double {
        val p = player ?: return 0.0
        val time = p.currentTime()
        val sec = CMTimeGetSeconds(time)
        return if (sec.isNaN() || sec.isInfinite() || sec < 0.0) 0.0 else sec
    }

    private fun currentDurationSeconds(): Double {
        val item = currentItem ?: return (currentTrack?.durationSeconds?.toDouble() ?: 0.0)
        val dur = item.duration
        val sec = CMTimeGetSeconds(dur)
        return if (sec.isNaN() || sec.isInfinite() || sec <= 0.0) {
            currentTrack?.durationSeconds?.toDouble() ?: 0.0
        } else {
            sec
        }
    }
}
