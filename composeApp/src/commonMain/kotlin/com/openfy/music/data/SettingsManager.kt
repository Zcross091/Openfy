package com.openfy.music.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface Settings {
    fun getStringOrNull(key: String): String?
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun remove(key: String)
    fun clear()
}

class MapSettings : Settings {
    private val map = mutableMapOf<String, Any>()

    override fun getStringOrNull(key: String): String? = map[key] as? String
    override fun getString(key: String, defaultValue: String): String = (map[key] as? String) ?: defaultValue
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = (map[key] as? Boolean) ?: defaultValue
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = (map[key] as? Long) ?: defaultValue
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
    override fun clear() { map.clear() }
}

enum class AudioQuality(val title: String, val description: String, val targetItag: Int, val fallbackItag: Int) {
    AUTO("Auto (Recommended)", "Best quality available (Opus ~160k on Android, AAC ~130k on iOS)", 251, 140),
    HIGH("High Fidelity", "Opus audio stream (~160 kbps, itag 251)", 251, 140),
    NORMAL("Normal", "AAC audio stream (~130 kbps, itag 140)", 140, 251),
    DATA_SAVER("Data Saver", "Low bandwidth stream (~50-70 kbps, itag 250/249)", 250, 249)
}

object SettingsManager {
    private val defaultSettings: Settings = MapSettings()
    private var customSettings: Settings? = null

    fun initSettings(settings: Settings) {
        customSettings = settings
        reload()
    }

    val settings: Settings
        get() = customSettings ?: defaultSettings

    private val _audioQuality = MutableStateFlow(loadAudioQuality())
    val audioQuality = _audioQuality.asStateFlow()

    private val _enableLocalMusic = MutableStateFlow(loadEnableLocalMusic())
    val enableLocalMusic = _enableLocalMusic.asStateFlow()

    private fun reload() {
        _audioQuality.value = loadAudioQuality()
        _enableLocalMusic.value = loadEnableLocalMusic()
    }

    private fun loadAudioQuality(): AudioQuality {
        return try {
            val saved = settings.getString("audio_quality", AudioQuality.AUTO.name)
            AudioQuality.valueOf(saved)
        } catch (e: Exception) {
            AudioQuality.AUTO
        }
    }

    private fun loadEnableLocalMusic(): Boolean {
        return try {
            settings.getBoolean("enable_local_music", true)
        } catch (e: Exception) {
            true
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        try {
            settings.putString("audio_quality", quality.name)
        } catch (e: Exception) {
            // Ignore write errors
        }
        _audioQuality.value = quality
    }

    fun setEnableLocalMusic(enable: Boolean) {
        try {
            settings.putBoolean("enable_local_music", enable)
        } catch (e: Exception) {
            // Ignore write errors
        }
        _enableLocalMusic.value = enable
    }
}
