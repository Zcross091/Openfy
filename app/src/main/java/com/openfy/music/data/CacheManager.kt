package com.openfy.music.data

import com.openfy.music.OpenfyApplication
import java.io.File

object CacheManager {

    fun getCacheSizeBytes(): Long {
        return try {
            val app = OpenfyApplication.instance
            var total = 0L
            app.cacheDir?.walkTopDown()?.filter { it.isFile }?.forEach { total += it.length() }
            app.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.forEach { total += it.length() }
            total
        } catch (e: Exception) {
            0L
        }
    }

    fun getFormattedCacheSize(): String {
        val bytes = getCacheSizeBytes()
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb < 0.1) "< 0.1 MB" else String.format("%.1f MB", mb)
    }

    fun clearAudioCache(): Boolean {
        return try {
            val app = OpenfyApplication.instance
            deleteDirContents(File(app.cacheDir, "exoplayer"))
            deleteDirContents(File(app.cacheDir, "media"))
            deleteDirContents(File(app.cacheDir, "audio"))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearImageCache(): Boolean {
        return try {
            val app = OpenfyApplication.instance
            deleteDirContents(File(app.cacheDir, "image_cache"))
            deleteDirContents(File(app.cacheDir, "coil"))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllCache(): Boolean {
        return try {
            val app = OpenfyApplication.instance
            deleteDirContents(app.cacheDir)
            deleteDirContents(app.externalCacheDir)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteDirContents(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return true
        val children = dir.listFiles() ?: return true
        for (child in children) {
            child.deleteRecursively()
        }
        return true
    }
}
