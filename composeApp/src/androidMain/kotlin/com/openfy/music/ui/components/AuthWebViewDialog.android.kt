package com.openfy.music.ui.components

import android.annotation.SuppressLint
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.AccountProvider
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun AuthWebViewDialog(
    provider: AccountProvider,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedAuthMode by remember { mutableStateOf(0) } // 0: In-App WebView, 1: Quick Cookie Paste
    var spDcInput by remember { mutableStateOf("") }
    var isVerifyingCookie by remember { mutableStateOf(false) }
    var cookieErrorMessage by remember { mutableStateOf<String?>(null) }

    val initialUrl = "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F"
    var isLoading by remember { mutableStateOf(true) }
    var currentLoadedUrl by remember { mutableStateOf(initialUrl) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }

    // Helper: checks all cookie sources and URL state to connect
    fun attemptConnect(extraCookies: String? = null) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()

        val cOpen = cookieManager.getCookie("https://open.spotify.com") ?: ""
        val cAcc = cookieManager.getCookie("https://accounts.spotify.com") ?: ""
        val cSpot = cookieManager.getCookie("https://spotify.com") ?: ""
        val cDot = cookieManager.getCookie(".spotify.com") ?: ""
        val combined = listOf(cOpen, cAcc, cSpot, cDot, extraCookies ?: "")
            .filter { it.isNotBlank() }
            .joinToString("; ")

        val hasSpDc = combined.contains("sp_dc")
        val onLoggedInAccountPage = (currentLoadedUrl.contains("accounts.spotify.com") && !currentLoadedUrl.contains("login"))
        val onOpenSpotify = (currentLoadedUrl.contains("open.spotify.com") && !currentLoadedUrl.contains("login"))

        if (hasSpDc || (onLoggedInAccountPage && combined.contains("sp_t")) || (onOpenSpotify && combined.contains("sp_t"))) {
            AccountManager.setSpotifySession(combined)
            onSuccess("Connected to Spotify Account!")
            onDismiss()
        } else if (onLoggedInAccountPage) {
            activeWebView?.loadUrl("https://open.spotify.com/")
        }
    }

    // Auto-polling for seamless zero-tap connection as soon as login completes
    LaunchedEffect(selectedAuthMode) {
        if (selectedAuthMode == 0) {
            while (true) {
                delay(1000)
                attemptConnect()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                        text = "Connect Spotify Account",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(initialUrl)).apply {
                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Auth Mode Switcher (In-App WebView vs Quick Cookie)
                TabRow(
                    selectedTabIndex = selectedAuthMode,
                    containerColor = Color(0xFF222222),
                    contentColor = SpotifyGreen
                ) {
                    Tab(
                        selected = selectedAuthMode == 0,
                        onClick = { selectedAuthMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("In-App Login", fontSize = 13.sp)
                            }
                        },
                        selectedContentColor = SpotifyGreen,
                        unselectedContentColor = SpotifySubdued
                    )
                    Tab(
                        selected = selectedAuthMode == 1,
                        onClick = { selectedAuthMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quick Cookie (sp_dc)", fontSize = 13.sp)
                            }
                        },
                        selectedContentColor = SpotifyGreen,
                        unselectedContentColor = SpotifySubdued
                    )
                }

                if (selectedAuthMode == 0) {
                    // --- MODE 0: IN-APP WEBVIEW ---
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = SpotifyGreen,
                            trackColor = Color(0xFF282828)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    activeWebView = this
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.setSupportMultipleWindows(false)
                                    settings.javaScriptCanOpenWindowsAutomatically = true
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT

                                    settings.userAgentString =
                                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            super.onProgressChanged(view, newProgress)
                                            if (newProgress >= 100) {
                                                isLoading = false
                                            }
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString() ?: return false
                                            currentLoadedUrl = url
                                            view?.loadUrl(url)
                                            return true
                                        }

                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoading = true
                                            url?.let {
                                                currentLoadedUrl = it
                                                attemptConnect()
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                            url?.let {
                                                currentLoadedUrl = it
                                                attemptConnect()
                                            }
                                            view?.evaluateJavascript("document.cookie") { jsCookie ->
                                                val clean = jsCookie?.trim('"', '\'')
                                                if (!clean.isNullOrEmpty() && clean != "null") {
                                                    attemptConnect(clean)
                                                }
                                            }
                                        }
                                    }
                                    loadUrl(initialUrl)
                                }
                            }
                        )
                    }

                    // Bottom Action Bar: Instant Connect Button
                    Surface(
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    activeWebView?.evaluateJavascript("document.cookie") { jsCookie ->
                                        val clean = jsCookie?.trim('"', '\'')
                                        attemptConnect(clean)
                                    }
                                    attemptConnect()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(23.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("I'm Logged In - Connect Openfy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                text = "When you see 'Logged in as' or your profile above, tap to finish connecting.",
                                color = SpotifySubdued,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // --- MODE 1: QUICK COOKIE PASTE (sp_dc) ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Instant 1-Tap Spotify Connect",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Sign in via your mobile browser (Chrome/Brave) without any WebView restrictions:\n\n" +
                                    "1. Tap 'Open Spotify in Browser' below and log into Spotify.\n" +
                                    "2. Copy your 'sp_dc' cookie from browser settings or inspect.\n" +
                                    "3. Tap 'Paste & Connect' below.",
                            color = SpotifySubdued,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        OutlinedTextField(
                            value = spDcInput,
                            onValueChange = {
                                spDcInput = it
                                cookieErrorMessage = null
                            },
                            label = { Text("sp_dc Cookie or Session String") },
                            placeholder = { Text("e.g. AQD... or sp_dc=AQD...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                unfocusedBorderColor = Color(0xFF444444),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = SpotifyGreen
                            )
                        )

                        if (cookieErrorMessage != null) {
                            Text(
                                text = cookieErrorMessage!!,
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipData = clipboard.primaryClip
                                        if (clipData != null && clipData.itemCount > 0) {
                                            val text = clipData.getItemAt(0).text?.toString() ?: ""
                                            spDcInput = text.trim()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(initialUrl)).apply {
                                            addCategory(Intent.CATEGORY_BROWSABLE)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A), contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Browser", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val trimmed = spDcInput.trim()
                                if (trimmed.isBlank()) {
                                    cookieErrorMessage = "Please paste your Spotify sp_dc cookie"
                                    return@Button
                                }
                                isVerifyingCookie = true
                                cookieErrorMessage = null

                                AccountManager.setSpotifySession(trimmed)
                                onSuccess("Connected to Spotify Account!")
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            if (isVerifyingCookie) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Connect Spotify", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
