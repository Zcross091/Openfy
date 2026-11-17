package com.openfy.music.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.openfy.music.player.PlaybackManager

class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Configure robust streaming HTTP data source with desktop headers & range requests
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setDefaultRequestProperties(
                mapOf(
                    "Range" to "bytes=0-",
                    "Origin" to "https://music.youtube.com",
                    "Referer" to "https://music.youtube.com/"
                )
            )

        // 2. Configure generous background buffer (up to 3 minutes ahead) to prevent buffer starvation
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                45_000,   // Min buffer before playback can start if starved (45s)
                180_000,  // Max buffer capacity (180s = 3 minutes)
                2_500,    // Initial buffer before playback starts (2.5s)
                5_000     // Buffer needed to resume after rebuffering (5s)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 3. Build ExoPlayer with Network WakeLock so Android never throttles audio while screen is locked
        val player = ExoPlayer.Builder(this)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handle audio focus
            )
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSourceFactory)
            )
            .setLoadControl(loadControl)
            .build()

        PlaybackManager.setPlayer(player)

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
