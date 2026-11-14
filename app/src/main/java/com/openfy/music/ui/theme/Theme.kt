package com.openfy.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = Black,
    background = SpotifyBlack,
    onBackground = White,
    surface = SpotifyDark,
    onSurface = White
)

@Composable
fun OpenfyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
