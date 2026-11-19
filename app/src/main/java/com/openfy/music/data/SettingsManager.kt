package com.openfy.music.data

import android.content.Context
import com.openfy.music.OpenfyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioQuality(val title: String, val description: String, val targetItag: Int, val fallbackItag: Int) {
    AUTO("Auto (Recommended)", "Best quality available (Unrestricted 128k AAC - High Stability)", 140, 251),
    HIGH("High (128-140k AAC)", "AAC audio stream (~128-140 kbps, itag 140)", 140, 251),
    NORMAL("Normal (128k AAC)", "AAC audio stream (~128-130 kbps, itag 140)", 140, 251),
    DATA_SAVER("Low (64k Data Saver)", "Low bandwidth stream (~50-70 kbps, itag 250/249)", 250, 249)
}

enum class StreamingClient(val title: String, val description: String, val clientKey: String) {
    AUTO("Auto Fallback (RVX)", "Automatically tries Android VR, iOS, visionOS & NewPipe", "AUTO"),
    ANDROID_VR("Android VR (RVX Default)", "Primary ReVanced VR client (Unthrottled, no cipher)", "ANDROID_VR"),
    IOS("iOS Direct", "Apple iOS Client (Unrestricted direct AAC)", "IOS"),
    VISIONOS("Apple visionOS", "Apple visionOS Cobalt Client", "VISIONOS"),
    NEWPIPE("NewPipe Extractor", "On-device signature deobfuscation engine", "NEWPIPE"),
    TV("TV Simply (Cobalt)", "Smart TV Leanback client fallback", "TVHTML5")
}

object SettingsManager {
    private val prefs by lazy {
        OpenfyApplication.instance.getSharedPreferences("openfy_settings", Context.MODE_PRIVATE)
    }

    private val _audioQuality = MutableStateFlow(
        try {
            val saved = prefs.getString("audio_quality", AudioQuality.AUTO.name) ?: AudioQuality.AUTO.name
            AudioQuality.valueOf(saved)
        } catch (e: Exception) {
            AudioQuality.AUTO
        }
    )
    val audioQuality = _audioQuality.asStateFlow()

    private val _streamingClient = MutableStateFlow(
        try {
            val saved = prefs.getString("streaming_client", StreamingClient.AUTO.name) ?: StreamingClient.AUTO.name
            StreamingClient.valueOf(saved)
        } catch (e: Exception) {
            StreamingClient.AUTO
        }
    )
    val streamingClient = _streamingClient.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(
        prefs.getBoolean("gapless_playback", true)
    )
    val gaplessPlayback = _gaplessPlayback.asStateFlow()

    private val _focusAudio = MutableStateFlow(
        prefs.getBoolean("focus_audio", true)
    )
    val focusAudio = _focusAudio.asStateFlow()

    private val _highResArt = MutableStateFlow(
        prefs.getBoolean("high_res_art", true)
    )
    val highResArt = _highResArt.asStateFlow()

    private val _showLyrics = MutableStateFlow(
        prefs.getBoolean("show_lyrics", true)
    )
    val showLyrics = _showLyrics.asStateFlow()

    private val _enableLocalMusic = MutableStateFlow(
        prefs.getBoolean("enable_local_music", true)
    )
    val enableLocalMusic = _enableLocalMusic.asStateFlow()

    fun setAudioQuality(quality: AudioQuality) {
        prefs.edit().putString("audio_quality", quality.name).apply()
        _audioQuality.value = quality
    }

    fun setStreamingClient(client: StreamingClient) {
        prefs.edit().putString("streaming_client", client.name).apply()
        _streamingClient.value = client
    }

    fun setGaplessPlayback(enable: Boolean) {
        prefs.edit().putBoolean("gapless_playback", enable).apply()
        _gaplessPlayback.value = enable
    }

    fun setFocusAudio(enable: Boolean) {
        prefs.edit().putBoolean("focus_audio", enable).apply()
        _focusAudio.value = enable
    }

    fun setHighResArt(enable: Boolean) {
        prefs.edit().putBoolean("high_res_art", enable).apply()
        _highResArt.value = enable
    }

    fun setShowLyrics(enable: Boolean) {
        prefs.edit().putBoolean("show_lyrics", enable).apply()
        _showLyrics.value = enable
    }

    fun setEnableLocalMusic(enable: Boolean) {
        prefs.edit().putBoolean("enable_local_music", enable).apply()
        _enableLocalMusic.value = enable
    }
}
