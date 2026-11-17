package com.openfy.music.data

object CacheManager {
    private var simulatedCacheBytes = 28_400_000L // ~28.4 MB initial representation
    private var customSizeProvider: (() -> Long)? = null
    private var customClearHandler: (() -> Unit)? = null

    fun setPlatformProvider(sizeProvider: () -> Long, clearHandler: () -> Unit) {
        customSizeProvider = sizeProvider
        customClearHandler = clearHandler
    }

    fun getCacheSizeBytes(): Long {
        return customSizeProvider?.invoke() ?: simulatedCacheBytes
    }

    fun getFormattedCacheSize(): String {
        val bytes = getCacheSizeBytes()
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        val rounded = ((mb * 10).toLong()) / 10.0
        return if (mb <= 0.05) "0.0 MB" else "$rounded MB"
    }

    fun clearAudioCache(): Boolean {
        simulatedCacheBytes = (simulatedCacheBytes / 3).coerceAtLeast(0L)
        customClearHandler?.invoke()
        return true
    }

    fun clearImageCache(): Boolean {
        simulatedCacheBytes = (simulatedCacheBytes / 2).coerceAtLeast(0L)
        customClearHandler?.invoke()
        return true
    }

    fun clearAllCache(): Boolean {
        simulatedCacheBytes = 0L
        customClearHandler?.invoke()
        return true
    }
}
