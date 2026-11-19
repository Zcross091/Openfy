package com.openfy.music.player

import com.openfy.music.data.model.Track
import com.openfy.music.platform.DesktopPowerManager
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

        // Ensure persistent HTTP connection keep-alive to CDNs across all Java networking
        try {
            System.setProperty("http.keepAlive", "true")
            System.setProperty("http.maxConnections", "16")
        } catch (_: Throwable) {}

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

        DesktopPowerManager.acquireKeepAwake()

        scope.launch {
            try {
                val mediaClass = Class.forName("javafx.scene.media.Media")
                val playerClass = Class.forName("javafx.scene.media.MediaPlayer")
                val mediaConstructor = mediaClass.getConstructor(String::class.java)
                val playerConstructor = playerClass.getConstructor(mediaClass)

                val media = mediaConstructor.newInstance(url)
                val player = playerConstructor.newInstance(media)
                fxPlayer = player

                // Apply initial volume
                val setVolumeMethod = playerClass.getMethod("setVolume", Double::class.javaPrimitiveType)
                setVolumeMethod.invoke(player, PlaybackManager.volume.value.toDouble())

                // End of media callback
                val setOnEndOfMedia = playerClass.getMethod("setOnEndOfMedia", Runnable::class.java)
                setOnEndOfMedia.invoke(player, Runnable {
                    PlaybackManager.onTrackEnded()
                })

                // Fatal error callback
                val setOnError = playerClass.getMethod("setOnError", Runnable::class.java)
                setOnError.invoke(player, Runnable {
                    println("[DesktopAudioPlayer] Stream playback error detected!")
                    PlaybackManager.onPlaybackError()
                })

                // Stall callback (buffer underrun / socket pause)
                try {
                    val setOnStalled = playerClass.getMethod("setOnStalled", Runnable::class.java)
                    setOnStalled.invoke(player, Runnable {
                        println("[DesktopAudioPlayer] Stream buffer stalled. Requesting recovery...")
                        PlaybackManager.onPlaybackStalled()
                    })
                } catch (_: Throwable) {}

                // Halt callback
                try {
                    val setOnHalted = playerClass.getMethod("setOnHalted", Runnable::class.java)
                    setOnHalted.invoke(player, Runnable {
                        println("[DesktopAudioPlayer] Stream halted. Failing over...")
                        PlaybackManager.onPlaybackError()
                    })
                } catch (_: Throwable) {}

                // Play stream
                val playMethod = playerClass.getMethod("play")
                playMethod.invoke(player)

                PlaybackManager.updatePlaybackState(isPlaying = true, isLoading = false)

                // Track playback progress & active stall watchdog
                progressJob?.cancel()
                progressJob = scope.launch {
                    val curTimeMethod = playerClass.getMethod("getCurrentTime")
                    val totalDurMethod = playerClass.getMethod("getTotalDuration")
                    val statusMethod = playerClass.getMethod("getStatus")
                    val durationClass = Class.forName("javafx.util.Duration")
                    val toMillisMethod = durationClass.getMethod("toMillis")

                    var lastPos = -1L
                    var stallCount = 0

                    while (isActive) {
                        try {
                            val curTime = curTimeMethod.invoke(player)
                            val totalDur = totalDurMethod.invoke(player)
                            val statusObj = statusMethod.invoke(player)
                            val statusStr = statusObj?.toString() ?: ""

                            if (curTime != null && totalDur != null) {
                                val pos = (toMillisMethod.invoke(curTime) as Double).toLong()
                                val dur = (toMillisMethod.invoke(totalDur) as Double).toLong()
                                if (pos >= 0) {
                                    PlaybackManager.updatePlaybackProgress(pos, dur)

                                    // Active freeze watchdog: if status is supposed to be PLAYING or STALLED
                                    if (statusStr == "PLAYING" || statusStr == "STALLED") {
                                        if (pos == lastPos && pos > 0 && (dur <= 0 || pos < dur - 1500)) {
                                            stallCount++
                                            if (stallCount == 4) {
                                                // Buffer kick
                                                try { playMethod.invoke(player) } catch (_: Exception) {}
                                            } else if (stallCount >= 8) { // 4 seconds without any progress
                                                println("[DesktopAudioPlayer] Watchdog detected frozen stream at ${pos}ms. Triggering auto-fallback!")
                                                stallCount = 0
                                                PlaybackManager.onPlaybackStalled()
                                            }
                                        } else {
                                            stallCount = 0
                                            lastPos = pos
                                        }
                                    } else {
                                        stallCount = 0
                                    }
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
        DesktopPowerManager.releaseKeepAwake()
        fxPlayer?.let { player ->
            try {
                player.javaClass.getMethod("pause").invoke(player)
                PlaybackManager.updatePlaybackState(isPlaying = false)
            } catch (_: Exception) {}
        }
    }

    override fun resume() {
        DesktopPowerManager.acquireKeepAwake()
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
        DesktopPowerManager.releaseKeepAwake()
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
