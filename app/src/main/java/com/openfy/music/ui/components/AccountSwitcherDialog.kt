package com.openfy.music.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.AccountProvider
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.RvxRed
import com.openfy.music.ui.theme.SpotifyDark
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun AccountSwitcherDialog(
    onDismiss: () -> Unit
) {
    val activeProvider by AccountManager.activeProvider.collectAsState()
    val spotifyEmail by AccountManager.spotifyEmail.collectAsState()
    val youtubeEmail by AccountManager.youtubeEmail.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSpotifyEmailInput by remember { mutableStateOf(false) }
    var spotifyEmailInput by remember { mutableStateOf("") }
    var spotifyTokenInput by remember { mutableStateOf("") }

    var youtubeEmailInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SpotifyDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account & Library Switcher",
                        color = White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select your active account. Tapping the Heart icon in playback saves songs directly to your active library!",
                    color = SpotifySubdued,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Account Selection Tiles (Spotify, YouTube, Guest)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Spotify Card
                    AccountTile(
                        title = "Spotify",
                        subtitle = if (SpotifyClient.isConnected || AccountManager.isSpotifyConnected) "Connected" else "Not Linked",
                        color = SpotifyGreen,
                        isSelected = activeProvider == AccountProvider.SPOTIFY,
                        onClick = { AccountManager.switchAccount(AccountProvider.SPOTIFY) },
                        modifier = Modifier.weight(1f)
                    )

                    // YouTube Card
                    AccountTile(
                        title = "YouTube",
                        subtitle = if (AccountManager.isYouTubeConnected) "Linked" else "Active",
                        color = RvxRed,
                        isSelected = activeProvider == AccountProvider.YOUTUBE,
                        onClick = { AccountManager.switchAccount(AccountProvider.YOUTUBE) },
                        modifier = Modifier.weight(1f)
                    )

                    // Guest Card
                    AccountTile(
                        title = "Guest",
                        subtitle = "Free",
                        color = Color(0xFF888888),
                        isSelected = activeProvider == AccountProvider.GUEST,
                        onClick = { AccountManager.switchAccount(AccountProvider.GUEST) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Provider Login / Management Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141414))
                        .padding(14.dp)
                ) {
                    when (activeProvider) {
                        AccountProvider.SPOTIFY -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Spotify Account Login",
                                    color = White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (SpotifyClient.isConnected || AccountManager.isSpotifyConnected) {
                                    val displayName = SpotifyClient.userProfile?.displayName ?: spotifyEmail ?: "Spotify User"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Logged in as $displayName",
                                            color = White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "Your Spotify playlists & Liked Songs are active. Heart icon saves tracks to Spotify library.",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            AccountManager.disconnectSpotify()
                                            AccountManager.switchAccount(AccountProvider.GUEST)
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                                    ) {
                                        Text("Disconnect Spotify", fontSize = 12.sp)
                                    }
                                } else {
                                    Text(
                                        text = "Choose how to log into your Spotify account:",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Button 1: Google Login for Spotify
                                    Button(
                                        onClick = {
                                            val spotifyOAuthUrl = "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyOAuthUrl))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Log in with Google Account", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Button 2: Email Login for Spotify
                                    OutlinedButton(
                                        onClick = { showSpotifyEmailInput = !showSpotifyEmailInput },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Log in with Spotify Email / Token", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (showSpotifyEmailInput) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = spotifyEmailInput,
                                            onValueChange = { spotifyEmailInput = it },
                                            placeholder = { Text("Spotify Email (e.g. user@gmail.com)", fontSize = 11.sp, color = Color.Gray) },
                                            modifier = Modifier.fillMaxWidth().height(50.dp),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = spotifyTokenInput,
                                            onValueChange = { spotifyTokenInput = it },
                                            placeholder = { Text("Access Token or Password", fontSize = 11.sp, color = Color.Gray) },
                                            modifier = Modifier.fillMaxWidth().height(50.dp),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                if (spotifyEmailInput.isNotBlank() || spotifyTokenInput.isNotBlank()) {
                                                    AccountManager.setSpotifyEmailAccount(
                                                        email = if (spotifyEmailInput.isNotBlank()) spotifyEmailInput else "spotify_user",
                                                        token = spotifyTokenInput.ifBlank { null }
                                                    )
                                                    scope.launch {
                                                        SpotifyClient.fetchProfile()
                                                    }
                                                    spotifyEmailInput = ""
                                                    spotifyTokenInput = ""
                                                    showSpotifyEmailInput = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Link Spotify Account", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        AccountProvider.YOUTUBE -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "YouTube Music Account (Google)",
                                    color = White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (AccountManager.isYouTubeConnected) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = RvxRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Logged in as ${youtubeEmail ?: "Google Account"}",
                                            color = White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "Powered by RVX YouTube Music engine. Liking tracks with the Heart icon saves them to your YouTube Liked Music library.",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            AccountManager.disconnectYouTube()
                                            AccountManager.switchAccount(AccountProvider.GUEST)
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                                    ) {
                                        Text("Disconnect YouTube", fontSize = 12.sp)
                                    }
                                } else {
                                    Text(
                                        text = "Log in with your Google account to sync your YouTube Music library:",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Button 1: Google Account Sign-In Web Portal
                                    Button(
                                        onClick = {
                                            val ytLoginUrl = "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ytLoginUrl))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = RvxRed, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sign in with Google Account", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Button 2: Direct Google Email linking
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = youtubeEmailInput,
                                            onValueChange = { youtubeEmailInput = it },
                                            placeholder = { Text("Google Email (user@gmail.com)", fontSize = 11.sp, color = Color.Gray) },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (youtubeEmailInput.isNotBlank()) {
                                                    AccountManager.setYouTubeAccount(youtubeEmailInput)
                                                    youtubeEmailInput = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RvxRed),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Link", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        AccountProvider.GUEST -> {
                            Column {
                                Text(
                                    text = "Guest Mode Active",
                                    color = White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enjoy ad-free music streaming powered by the RVX engine without logging into any account.",
                                    color = SpotifySubdued,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = White),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Done", color = Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AccountTile(
    title: String,
    subtitle: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color(0xFF222222))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) color else Color(0xFF333333),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(2).uppercase(),
                    color = if (color == SpotifyGreen) Black else White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = SpotifySubdued,
                fontSize = 9.sp
            )
        }
    }
}
