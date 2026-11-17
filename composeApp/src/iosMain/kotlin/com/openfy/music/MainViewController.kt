package com.openfy.music

import androidx.compose.ui.window.ComposeUIViewController
import com.openfy.music.data.SettingsManager
import com.openfy.music.platform.PlatformConfig
import com.openfy.music.platform.PlatformType
import com.openfy.music.player.IosAudioPlayer
import com.openfy.music.player.IosSettings
import platform.UIKit.UIViewController

/**
 * Main entry point for the iOS application.
 *
 * Configures platform configuration to [PlatformType.IOS], initializes
 * native NSUserDefaults settings persistence, starts the audio player,
 * and returns a [ComposeUIViewController] hosting [OpenfyApp].
 */
fun MainViewController(): UIViewController {
    PlatformConfig.platform = PlatformType.IOS
    SettingsManager.initSettings(IosSettings())
    IosAudioPlayer.initialize()
    return ComposeUIViewController {
        OpenfyApp()
    }
}
