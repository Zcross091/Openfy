package com.openfy.music

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.openfy.music.data.SettingsManager
import com.openfy.music.platform.PlatformConfig
import com.openfy.music.platform.PlatformType
import com.openfy.music.player.DesktopAudioPlayer
import com.openfy.music.player.DesktopSettings
import com.openfy.music.player.PlaybackManager
import java.awt.Dimension
import java.awt.Toolkit

fun main() = application {
    PlatformConfig.platform = PlatformType.DESKTOP
    SettingsManager.initSettings(DesktopSettings())
    DesktopAudioPlayer.initialize()
    PlaybackManager.restoreSession()

    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 820.dp),
        position = WindowPosition(Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Openfy"
    ) {
        window.minimumSize = Dimension(960, 600)
        try {
            val iconUrl = object {}.javaClass.classLoader?.getResource("icon.png")
            if (iconUrl != null) {
                window.iconImage = Toolkit.getDefaultToolkit().getImage(iconUrl)
            }
        } catch (_: Exception) {}
        App()
    }
}
