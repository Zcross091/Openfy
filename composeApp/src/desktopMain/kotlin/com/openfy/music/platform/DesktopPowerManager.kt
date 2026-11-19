package com.openfy.music.platform

import com.sun.jna.Library
import com.sun.jna.Native

object DesktopPowerManager {
    private interface Kernel32 : Library {
        fun SetThreadExecutionState(esFlags: Int): Int
    }

    private val kernel32: Kernel32? by lazy {
        try {
            val os = System.getProperty("os.name", "").lowercase()
            if (os.contains("win")) {
                Native.load("kernel32", Kernel32::class.java)
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    private const val ES_CONTINUOUS = 0x80000000.toInt()
    private const val ES_SYSTEM_REQUIRED = 0x00000001
    private const val ES_AWAYMODE_REQUIRED = 0x00000040
    private const val PLAYBACK_POWER_FLAGS = ES_CONTINUOUS or ES_SYSTEM_REQUIRED or ES_AWAYMODE_REQUIRED

    private var caffeinateProcess: Process? = null
    private var isAwake = false

    fun acquireKeepAwake() {
        if (isAwake) return
        isAwake = true
        try {
            val os = System.getProperty("os.name", "").lowercase()
            if (os.contains("win")) {
                kernel32?.SetThreadExecutionState(PLAYBACK_POWER_FLAGS)
                println("[DesktopPowerManager] Windows SetThreadExecutionState active - screen-off playback enabled")
            } else if (os.contains("mac")) {
                if (caffeinateProcess == null || !caffeinateProcess!!.isAlive) {
                    caffeinateProcess = ProcessBuilder("caffeinate", "-i", "-s").start()
                    println("[DesktopPowerManager] macOS caffeinate active - screen-off playback enabled")
                }
            }
        } catch (e: Throwable) {
            println("[DesktopPowerManager] Failed to acquire power lock: ${e.message}")
        }
    }

    fun releaseKeepAwake() {
        if (!isAwake) return
        isAwake = false
        try {
            val os = System.getProperty("os.name", "").lowercase()
            if (os.contains("win")) {
                kernel32?.SetThreadExecutionState(ES_CONTINUOUS)
                println("[DesktopPowerManager] Windows SetThreadExecutionState reset to default")
            } else if (os.contains("mac")) {
                caffeinateProcess?.let {
                    if (it.isAlive) it.destroy()
                    caffeinateProcess = null
                }
                println("[DesktopPowerManager] macOS caffeinate terminated")
            }
        } catch (e: Throwable) {
            println("[DesktopPowerManager] Failed to release power lock: ${e.message}")
        }
    }
}
