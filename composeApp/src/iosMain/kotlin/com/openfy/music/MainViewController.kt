package com.openfy.music

import androidx.compose.ui.window.ComposeUIViewController
import com.openfy.music.platform.PlatformConfig
import com.openfy.music.platform.PlatformType
import com.openfy.music.player.IosAudioPlayer
import platform.UIKit.UIViewController

/**
 * Main entry point for the iOS application.
 *
 * Configures platform configuration to [PlatformType.IOS], initializes the
 * native audio player with background playback and remote command center,
 * and returns a [ComposeUIViewController] hosting [OpenfyApp].
 */
fun MainViewController(): UIViewController {
    PlatformConfig.platform = PlatformType.IOS
    IosAudioPlayer.initialize()
    return ComposeUIViewController {
        OpenfyApp()
    }
}
