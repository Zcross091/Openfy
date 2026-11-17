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

        // 1. Configure robust streaming HTTP data source with clean range requests
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("com.google.ios.youtube/20.10.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)")
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        // 2. Configure snappy, low-latency buffer to start playback within 1.5 seconds
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,   // Min buffer before playback starts/resumes (15s)
                60_000,   // Max buffer capacity (60s)
                1_500,    // Initial buffer before playback starts (1.5s - snappy start!)
                3_000     // Buffer needed to resume after rebuffering (3s)
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
