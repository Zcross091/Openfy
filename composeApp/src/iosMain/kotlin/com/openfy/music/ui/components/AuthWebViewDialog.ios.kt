package com.openfy.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.AccountProvider
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AuthWebViewDialog(
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
            color = Color(0xFF181818)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connect Spotify Account",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val nsUrl = NSURL.URLWithString(initialUrl)
                            if (nsUrl != null) {
                                UIApplication.sharedApplication.openURL(nsUrl)
                            }
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Safari", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                UIKitView(
                    factory = {
                        val config = WKWebViewConfiguration().apply {
                            defaultWebpagePreferences.allowsContentJavaScript = true
                        }
                        val webView = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)

                        val delegate = object : NSObject(), WKNavigationDelegateProtocol {
                            override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
                                isLoading = true
                            }

                            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                                isLoading = false
                                val currentUrl = webView.URL?.absoluteString ?: ""
                                webView.configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies ->
                                    val cookieList = cookies as? List<platform.Foundation.NSHTTPCookie> ?: return@getAllCookies
                                    val cookieStr = cookieList.joinToString("; ") { "${it.name}=${it.value}" }

                                    if (provider == AccountProvider.YOUTUBE) {
                                        if (cookieStr.contains("SAPISID") || cookieStr.contains("__Secure-3PAPISID") || (cookieStr.contains("SID=") && currentUrl.contains("music.youtube.com"))) {
                                            AccountManager.setYouTubeGoogleCookies(cookieStr)
                                            onSuccess("Connected to YouTube Music via Google Account!")
                                            onDismiss()
                                        }
                                    } else if (provider == AccountProvider.SPOTIFY) {
                                        if (cookieStr.contains("sp_dc") || (currentUrl.contains("open.spotify.com") && !currentUrl.contains("login"))) {
                                            AccountManager.setSpotifySession(cookieStr)
                                            onSuccess("Connected to Spotify Account!")
                                            onDismiss()
                                        }
                                    }
                                }
                            }
                        }
                        webView.navigationDelegate = delegate

                        val nsUrl = NSURL.URLWithString(initialUrl)
                        if (nsUrl != null) {
                            webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                        }
                        webView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
