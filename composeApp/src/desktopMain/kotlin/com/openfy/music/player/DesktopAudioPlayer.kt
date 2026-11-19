package com.openfy.music.player

import com.openfy.music.data.model.Track
import kotlinx.coroutines.*

object DesktopAudioPlayer : NativeAudioPlayer {
    private var fxPlayer: Any? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        PlaybackManager.setNativePlayer(this)

        try {
            val platformClass = Class.forName("javafx.application.Platform")
            val startupMethod = platformClass.getMethod("startup", Runnable::class.java)
            startupMethod.invoke(null, Runnable {})
        } catch (_: Throwable) {
            // Platform already initialized or headless
        }
    }

    override fun playStream(url: String, track: Track) {
        initialize()
        stop()

        scope.launch {
            try {
                val mediaClass = Class.forName("javafx.scene.media.Media")
                val playerClass = Class.forName("javafx.scene.media.MediaPlayer")
                val mediaConstructor = mediaClass.getConstructor(String::class.java)
                val playerConstructor = playerClass.getConstructor(mediaClass)

                val media = mediaConstructor.newInstance(url)
                val player = playerConstructor.newInstance(media)
                fxPlayer = player

                // Set end of media callback
                val setOnEndOfMedia = playerClass.getMethod("setOnEndOfMedia", Runnable::class.java)
                setOnEndOfMedia.invoke(player, Runnable {
                    PlaybackManager.onTrackEnded()
                })

                // Set error callback
                val setOnError = playerClass.getMethod("setOnError", Runnable::class.java)
                setOnError.invoke(player, Runnable {
                    PlaybackManager.onPlaybackError()
                })

                // Play stream
                val playMethod = playerClass.getMethod("play")
                playMethod.invoke(player)

                PlaybackManager.updatePlaybackState(isPlaying = true, isLoading = false)

                // Track playback progress
                progressJob?.cancel()
                progressJob = scope.launch {
                    val curTimeMethod = playerClass.getMethod("getCurrentTime")
                    val totalDurMethod = playerClass.getMethod("getTotalDuration")
                    val durationClass = Class.forName("javafx.util.Duration")
                    val toMillisMethod = durationClass.getMethod("toMillis")

                    while (isActive) {
                        try {
                            val curTime = curTimeMethod.invoke(player)
                            val totalDur = totalDurMethod.invoke(player)
                            if (curTime != null && totalDur != null) {
                                val pos = (toMillisMethod.invoke(curTime) as Double).toLong()
                                val dur = (toMillisMethod.invoke(totalDur) as Double).toLong()
                                if (pos >= 0) {
                                    PlaybackManager.updatePlaybackProgress(pos, dur)
                                }
                            }
                        } catch (_: Exception) {}
                        delay(500)
                    }
                }
            } catch (e: Throwable) {
                println("[DesktopAudioPlayer] Failed to load audio stream via JavaFX: ${e.message}")
                PlaybackManager.onPlaybackError()
            }
        }
    }

    override fun pause() {
        fxPlayer?.let { player ->
            try {
                player.javaClass.getMethod("pause").invoke(player)
                PlaybackManager.updatePlaybackState(isPlaying = false)
            } catch (_: Exception) {}
        }
    }

    override fun resume() {
        fxPlayer?.let { player ->
            try {
                player.javaClass.getMethod("play").invoke(player)
                PlaybackManager.updatePlaybackState(isPlaying = true)
            } catch (_: Exception) {}
        }
    }

    override fun seekTo(positionMs: Long) {
        fxPlayer?.let { player ->
            try {
                val durationClass = Class.forName("javafx.util.Duration")
                val millisMethod = durationClass.getMethod("millis", Double::class.javaPrimitiveType)
                val dur = millisMethod.invoke(null, positionMs.toDouble())
                player.javaClass.getMethod("seek", durationClass).invoke(player, dur)
                PlaybackManager.updatePlaybackProgress(positionMs, 0L)
            } catch (_: Exception) {}
        }
    }

    override fun stop() {
        progressJob?.cancel()
        progressJob = null
        fxPlayer?.let { player ->
            try {
                player.javaClass.getMethod("stop").invoke(player)
                player.javaClass.getMethod("dispose").invoke(player)
            } catch (_: Exception) {}
            fxPlayer = null
        }
    }

    override fun setVolume(volume: Float) {
        val fraction = volume.coerceIn(0f, 1f)
        fxPlayer?.let { player ->
            try {
                val setVolumeMethod = player.javaClass.getMethod("setVolume", Double::class.javaPrimitiveType)
                setVolumeMethod.invoke(player, fraction.toDouble())
            } catch (_: Exception) {}
        }
    }
}
