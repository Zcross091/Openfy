package com.openfy.music.util

import platform.Foundation.NSRecursiveLock

actual class SyncLock actual constructor() {
    private val lock = NSRecursiveLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }
}
