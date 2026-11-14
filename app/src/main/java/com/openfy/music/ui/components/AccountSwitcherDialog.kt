package com.openfy.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val scope = rememberCoroutineScope()
    var tokenInput by remember { mutableStateOf("") }

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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Openfy lets you log in from your Spotify account or YouTube account (one at a time). When listening, tapping the Heart icon immediately saves that track to your active library!",
                    color = SpotifySubdued,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Account Selection Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Spotify Card
                    AccountTile(
                        title = "Spotify",
                        subtitle = if (SpotifyClient.isConnected) "Connected" else "Not Linked",
                        color = SpotifyGreen,
                        isSelected = activeProvider == AccountProvider.SPOTIFY,
                        onClick = {
                            AccountManager.switchAccount(AccountProvider.SPOTIFY)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // YouTube Card
                    AccountTile(
                        title = "YouTube",
                        subtitle = "Active",
                        color = RvxRed,
                        isSelected = activeProvider == AccountProvider.YOUTUBE,
                        onClick = {
                            AccountManager.switchAccount(AccountProvider.YOUTUBE)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Guest Card
                    AccountTile(
                        title = "Guest",
                        subtitle = "Free",
                        color = Color(0xFF888888),
                        isSelected = activeProvider == AccountProvider.GUEST,
                        onClick = {
                            AccountManager.switchAccount(AccountProvider.GUEST)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Provider Status Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF101010))
                        .padding(12.dp)
                ) {
                    when (activeProvider) {
                        AccountProvider.SPOTIFY -> {
                            Column {
                                Text(
                                    text = "Spotify Account",
                                    color = White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (SpotifyClient.isConnected) {
                                    Text(
                                        text = "Logged in. Your Spotify playlists and Liked Songs are active. Tap Heart to save tracks to Spotify!",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            SpotifyClient.disconnect()
                                            AccountManager.switchAccount(AccountProvider.GUEST)
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                                    ) {
                                        Text("Disconnect", fontSize = 12.sp)
                                    }
                                } else {
                                    Text(
                                        text = "Paste Spotify access token to sync your library:",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = tokenInput,
                                            onValueChange = { tokenInput = it },
                                            placeholder = { Text("Token...", fontSize = 11.sp, color = Color.Gray) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                if (tokenInput.isNotBlank()) {
                                                    SpotifyClient.accessToken = tokenInput.trim()
                                                    scope.launch {
                                                        SpotifyClient.fetchProfile()
                                                    }
                                                    tokenInput = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Save", color = Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        AccountProvider.YOUTUBE -> {
                            Column {
                                Text(
                                    text = "YouTube Music Account",
                                    color = White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Using YouTube Music audio engine. All songs you like with the Heart icon will be added to your YouTube Liked Music!",
                                    color = SpotifySubdued,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        AccountProvider.GUEST -> {
                            Column {
                                Text(
                                    text = "Guest Mode Active",
                                    color = White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enjoy ad-free music streaming powered by the RVX engine without logging in.",
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
