package com.openfy.music.util

expect class SyncLock() {
    fun <T> withLock(block: () -> T): T
}
