package com.openfy.music.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.AccountProvider
import com.openfy.music.ui.theme.SpotifyDark
import com.openfy.music.ui.theme.White

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthWebViewDialog(
    provider: AccountProvider,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val initialUrl = when (provider) {
        AccountProvider.YOUTUBE -> "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F"
        AccountProvider.SPOTIFY -> "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F"
        AccountProvider.GUEST -> ""
    }

    var isLoading by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = SpotifyDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (provider == AccountProvider.YOUTUBE) "Google Sign-In (YouTube)" else "Spotify Sign-In",
                        color = White,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = White)
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // In-App WebView
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            // Chrome Mobile User-Agent prevents Google disallowed_useragent block
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.127 Mobile Safari/537.36"

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    checkLoginStatus(url)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    checkLoginStatus(url)
                                }

                                private fun checkLoginStatus(currentUrl: String?) {
                                    if (currentUrl == null) return
                                    if (provider == AccountProvider.YOUTUBE) {
                                        val ytCookies = cookieManager.getCookie("https://music.youtube.com") ?: ""
                                        if (ytCookies.contains("SAPISID") || ytCookies.contains("__Secure-3PAPISID")) {
                                            AccountManager.setYouTubeGoogleCookies(ytCookies)
                                            onSuccess("Connected to YouTube Music via Google Account!")
                                            onDismiss()
                                        }
                                    } else if (provider == AccountProvider.SPOTIFY) {
                                        val spotCookies = cookieManager.getCookie("https://open.spotify.com") ?: ""
                                        if (spotCookies.contains("sp_dc") || currentUrl.contains("open.spotify.com")) {
                                            AccountManager.setSpotifySession(spotCookies)
                                            onSuccess("Connected to Spotify Account!")
                                            onDismiss()
                                        }
                                    }
                                }
                            }
                            loadUrl(initialUrl)
                        }
                    }
                )
            }
        }
    }
}
