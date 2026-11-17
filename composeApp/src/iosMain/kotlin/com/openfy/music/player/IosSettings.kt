package com.openfy.music.player

import com.openfy.music.data.Settings
import platform.Foundation.NSUserDefaults

/**
 * iOS native persistence backed by [NSUserDefaults.standardUserDefaults].
 */
class IosSettings : Settings {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getStringOrNull(key: String): String? {
        return defaults.stringForKey(key)
    }

    override fun getString(key: String, defaultValue: String): String {
        return defaults.stringForKey(key) ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        val num = defaults.objectForKey(key)
        return if (num != null) defaults.integerForKey(key).toLong() else defaultValue
    }

    override fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    override fun clear() {
        val dict = defaults.dictionaryRepresentation()
        for (key in dict.keys) {
            val keyStr = key as? String ?: continue
            defaults.removeObjectForKey(keyStr)
        }
    }
}
