package com.openfy.music.platform

enum class PlatformType {
    ANDROID,
    IOS,
    DESKTOP,
    WEB
}

object PlatformConfig {
    var platform: PlatformType = PlatformType.ANDROID

    val isIos: Boolean
        get() = platform == PlatformType.IOS

    val isAndroid: Boolean
        get() = platform == PlatformType.ANDROID
}
