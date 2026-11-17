package com.openfy.music.ui.components

import androidx.compose.runtime.Composable
import com.openfy.music.data.model.AccountProvider

/**
 * Platform-adaptive Web Authentication Dialog for Google (YouTube Music) and Spotify accounts.
 *
 * Implemented natively via Android WebView / Chrome Custom Tabs on Android
 * and via WKWebView / Safari Services on iOS.
 */
@Composable
expect fun AuthWebViewDialog(
    provider: AccountProvider,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
)
