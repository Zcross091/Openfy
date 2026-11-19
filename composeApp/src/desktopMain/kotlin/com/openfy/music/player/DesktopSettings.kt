package com.openfy.music.player

import com.openfy.music.data.Settings
import java.util.prefs.Preferences

class DesktopSettings : Settings {
    private val prefs: Preferences = try {
        Preferences.userRoot().node("com/openfy/music")
    } catch (_: Exception) {
        Preferences.userNodeForPackage(DesktopSettings::class.java)
    }

    override fun getStringOrNull(key: String): String? {
        return prefs.get(key, null)
    }

    override fun getString(key: String, defaultValue: String): String {
        return prefs.get(key, defaultValue)
    }

    override fun putString(key: String, value: String) {
        prefs.put(key, value)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    override fun putLong(key: String, value: Long) {
        prefs.putLong(key, value)
    }

    override fun remove(key: String) {
        prefs.remove(key)
    }

    override fun clear() {
        prefs.clear()
    }
}
