package com.openfy.music.util

actual class SyncLock actual constructor() {
    private val monitor = Any()
    actual fun <T> withLock(block: () -> T): T = synchronized(monitor, block)
}
