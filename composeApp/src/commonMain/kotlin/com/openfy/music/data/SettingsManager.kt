package com.openfy.music.data

import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer

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
    AUTO("Auto (Recommended)", "Best quality available (Unrestricted 128k AAC - High Stability)", 140, 251),
    HIGH("High (128-140k AAC)", "Highest fidelity AAC audio stream (~128-140 kbps, itag 140)", 140, 251),
    NORMAL("Normal (128k AAC)", "Standard AAC audio stream (~128-130 kbps, itag 140)", 140, 251),
    DATA_SAVER("Low (64k Data Saver)", "Low bandwidth audio stream (~50-70 kbps, itag 250/249)", 250, 249)
}

enum class StreamingClient(val title: String, val description: String, val clientKey: String) {
    AUTO("Auto Fallback", "Automatically tries JioSaavn HQ, iOS, Android VR & NewPipe", "AUTO"),
    SAAVN("JioSaavn HQ (320k)", "Direct 320kbps CD-quality audio (No Throttling, Full Length)", "SAAVN"),
    ANDROID_VR("Android VR (RVX Default)", "Primary ReVanced VR client (Unthrottled, no cipher)", "ANDROID_VR"),
    IOS("iOS Direct", "Apple iOS Client (Unrestricted direct AAC)", "IOS"),
    VISIONOS("Apple visionOS", "Apple visionOS Cobalt Client", "VISIONOS"),
    NEWPIPE("NewPipe Extractor", "On-device signature deobfuscation engine", "NEWPIPE"),
    TV("TV Simply (Cobalt)", "Smart TV Leanback client fallback", "TVHTML5")
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

    private val _streamingClient = MutableStateFlow(loadStreamingClient())
    val streamingClient = _streamingClient.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(loadBoolean("gapless_playback", true))
    val gaplessPlayback = _gaplessPlayback.asStateFlow()

    private val _focusAudio = MutableStateFlow(loadBoolean("focus_audio", true))
    val focusAudio = _focusAudio.asStateFlow()

    private val _highResArt = MutableStateFlow(loadBoolean("high_res_art", true))
    val highResArt = _highResArt.asStateFlow()

    private val _showLyrics = MutableStateFlow(loadBoolean("show_lyrics", true))
    val showLyrics = _showLyrics.asStateFlow()

    private val _enableLocalMusic = MutableStateFlow(loadBoolean("enable_local_music", true))
    val enableLocalMusic = _enableLocalMusic.asStateFlow()

    private fun reload() {
        _audioQuality.value = loadAudioQuality()
        _streamingClient.value = loadStreamingClient()
        _gaplessPlayback.value = loadBoolean("gapless_playback", true)
        _focusAudio.value = loadBoolean("focus_audio", true)
        _highResArt.value = loadBoolean("high_res_art", true)
        _showLyrics.value = loadBoolean("show_lyrics", true)
        _enableLocalMusic.value = loadBoolean("enable_local_music", true)
    }

    private fun loadAudioQuality(): AudioQuality {
        return try {
            val saved = settings.getString("audio_quality", AudioQuality.AUTO.name)
            AudioQuality.valueOf(saved)
        } catch (e: Exception) {
            AudioQuality.AUTO
        }
    }

    private fun loadStreamingClient(): StreamingClient {
        return try {
            val saved = settings.getString("streaming_client", StreamingClient.AUTO.name)
            StreamingClient.valueOf(saved)
        } catch (e: Exception) {
            StreamingClient.AUTO
        }
    }

    private fun loadBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            settings.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        try {
            settings.putString("audio_quality", quality.name)
        } catch (e: Exception) {
        }
        _audioQuality.value = quality
    }

    fun setStreamingClient(client: StreamingClient) {
        try {
            settings.putString("streaming_client", client.name)
        } catch (e: Exception) {
        }
        _streamingClient.value = client
    }

    fun setGaplessPlayback(enable: Boolean) {
        try {
            settings.putBoolean("gapless_playback", enable)
        } catch (e: Exception) {
        }
        _gaplessPlayback.value = enable
    }

    fun setFocusAudio(enable: Boolean) {
        try {
            settings.putBoolean("focus_audio", enable)
        } catch (e: Exception) {
        }
        _focusAudio.value = enable
    }

    fun setHighResArt(enable: Boolean) {
        try {
            settings.putBoolean("high_res_art", enable)
        } catch (e: Exception) {
        }
        _highResArt.value = enable
    }

    fun setShowLyrics(enable: Boolean) {
        try {
            settings.putBoolean("show_lyrics", enable)
        } catch (e: Exception) {
        }
        _showLyrics.value = enable
    }

    fun setEnableLocalMusic(enable: Boolean) {
        try {
            settings.putBoolean("enable_local_music", enable)
        } catch (e: Exception) {
        }
        _enableLocalMusic.value = enable
    }

    fun savePlaybackSession(
        track: Track?,
        positionMs: Long,
        queue: List<Track>,
        volume: Float,
        isShuffle: Boolean,
        repeatMode: Int,
        streamingClient: StreamingClient
    ) {
        try {
            if (track != null) {
                val trackJson = NetworkClient.json.encodeToString(Track.serializer(), track)
                settings.putString("session_last_track", trackJson)
            } else {
                settings.remove("session_last_track")
            }
            settings.putLong("session_last_position", positionMs)
            if (queue.isNotEmpty()) {
                val queueJson = NetworkClient.json.encodeToString(
                    ListSerializer(Track.serializer()),
                    queue.take(50)
                )
                settings.putString("session_last_queue", queueJson)
            }
            settings.putString("session_volume", volume.toString())
            settings.putBoolean("session_shuffle", isShuffle)
            settings.putLong("session_repeat", repeatMode.toLong())
            settings.putString("session_streaming_client", streamingClient.name)
        } catch (_: Exception) {}
    }

    fun loadSavedSession(): PlaybackSessionState {
        val track = try {
            val json = settings.getStringOrNull("session_last_track")
            if (!json.isNullOrEmpty()) NetworkClient.json.decodeFromString(Track.serializer(), json) else null
        } catch (_: Exception) { null }

        val positionMs = settings.getLong("session_last_position", 0L)

        val queue = try {
            val json = settings.getStringOrNull("session_last_queue")
            if (!json.isNullOrEmpty()) NetworkClient.json.decodeFromString(
                ListSerializer(Track.serializer()),
                json
            ) else emptyList()
        } catch (_: Exception) { emptyList() }

        val volume = try {
            settings.getString("session_volume", "1.0").toFloatOrNull() ?: 1.0f
        } catch (_: Exception) { 1.0f }

        val shuffle = settings.getBoolean("session_shuffle", false)
        val repeat = settings.getLong("session_repeat", 0L).toInt()
        val client = try {
            val clientName = settings.getString("session_streaming_client", StreamingClient.AUTO.name)
            StreamingClient.valueOf(clientName)
        } catch (_: Exception) { StreamingClient.AUTO }

        return PlaybackSessionState(track, positionMs, queue, volume, shuffle, repeat, client)
    }
}

data class PlaybackSessionState(
    val track: Track?,
    val positionMs: Long,
    val queue: List<Track>,
    val volume: Float,
    val isShuffle: Boolean,
    val repeatMode: Int,
    val streamingClient: StreamingClient
)
