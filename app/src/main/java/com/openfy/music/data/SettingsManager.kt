package com.openfy.music.data

import android.content.Context
import com.openfy.music.OpenfyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioQuality(val title: String, val description: String, val targetItag: Int, val fallbackItag: Int) {
    AUTO("Auto (Recommended)", "Best quality available (Opus ~160k with AAC fallback)", 251, 140),
    HIGH("High Fidelity", "Opus audio stream (~160 kbps, itag 251)", 251, 140),
    NORMAL("Normal", "AAC audio stream (~130 kbps, itag 140)", 140, 251),
    DATA_SAVER("Data Saver", "Low bandwidth stream (~50-70 kbps, itag 250/249)", 250, 249)
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

    private val _enableLocalMusic = MutableStateFlow(
        prefs.getBoolean("enable_local_music", true)
    )
    val enableLocalMusic = _enableLocalMusic.asStateFlow()

    fun setAudioQuality(quality: AudioQuality) {
        prefs.edit().putString("audio_quality", quality.name).apply()
        _audioQuality.value = quality
    }

    fun setEnableLocalMusic(enable: Boolean) {
        prefs.edit().putBoolean("enable_local_music", enable).apply()
        _enableLocalMusic.value = enable
    }
}
