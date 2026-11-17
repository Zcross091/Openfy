package com.openfy.music.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.AccountProvider
import androidx.compose.ui.graphics.Color

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthWebViewDialog(
    provider: AccountProvider,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val initialUrl = when (provider) {
        AccountProvider.YOUTUBE -> "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F"
        AccountProvider.SPOTIFY -> "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F"
        AccountProvider.GUEST -> ""
    }

    var isLoading by remember { mutableStateOf(true) }
    var currentLoadedUrl by remember { mutableStateOf(initialUrl) }

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
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Open in External / Custom Chrome Browser fallback
                        IconButton(onClick = {
                            try {
                                val targetUrl = if (currentLoadedUrl.isNotBlank()) currentLoadedUrl else initialUrl
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Chrome / Browser", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // In-App Secure WebView
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.setSupportMultipleWindows(true)
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.cacheMode = WebSettings.LOAD_DEFAULT

                            // Suppress X-Requested-With header on Android WebView to prevent Google 'browser or app may not be secure' block
                            try {
                                val method = settings.javaClass.getMethod("setRequestedWithHeaderOriginAllowList", java.util.Set::class.java)
                                method.invoke(settings, emptySet<String>())
                            } catch (_: Throwable) {}

                            // Clean Mobile Chrome User-Agent matching the device environment without '; wv' or 'Version/4.0'
                            val defaultUa = WebSettings.getDefaultUserAgent(ctx)
                            val cleanMobileChromeUa = defaultUa
                                .replace("; wv", "")
                                .replace(Regex("""Version/\d+\.\d+\s*"""), "")
                                .let { ua ->
                                    if (!ua.contains("Chrome/")) {
                                        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6723.107 Mobile Safari/537.36"
                                    } else {
                                        ua
                                    }
                                }
                            settings.userAgentString = cleanMobileChromeUa

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let { currentLoadedUrl = it }
                                    checkLoginStatus(url)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    url?.let { currentLoadedUrl = it }
                                    checkLoginStatus(url)
                                }

                                private fun checkLoginStatus(currentUrl: String?) {
                                    if (currentUrl == null) return
                                    cookieManager.flush()

                                    if (provider == AccountProvider.YOUTUBE) {
                                        val ytCookies = cookieManager.getCookie("https://music.youtube.com") ?: ""
                                        val googleCookies = cookieManager.getCookie("https://accounts.google.com") ?: ""
                                        val ytRootCookies = cookieManager.getCookie("https://www.youtube.com") ?: ""
                                        val combinedCookies = "$ytCookies; $googleCookies; $ytRootCookies"

                                        if (combinedCookies.contains("SAPISID") || combinedCookies.contains("__Secure-3PAPISID") || (combinedCookies.contains("SID=") && (currentUrl.contains("music.youtube.com") || currentUrl.contains("myaccount.google.com")))) {
                                            AccountManager.setYouTubeGoogleCookies(combinedCookies)
                                            onSuccess("Connected to YouTube Music via Google Account!")
                                            onDismiss()
                                        }
                                    } else if (provider == AccountProvider.SPOTIFY) {
                                        val spotCookies = cookieManager.getCookie("https://open.spotify.com") ?: ""
                                        val spotAccCookies = cookieManager.getCookie("https://accounts.spotify.com") ?: ""
                                        val combinedSpot = "$spotCookies; $spotAccCookies"

                                        if (combinedSpot.contains("sp_dc") || (currentUrl.contains("open.spotify.com") && !currentUrl.contains("login"))) {
                                            AccountManager.setSpotifySession(combinedSpot)
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
