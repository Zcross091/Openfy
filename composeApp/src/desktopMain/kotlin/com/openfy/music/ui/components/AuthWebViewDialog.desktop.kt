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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openfy.music.data.model.AccountProvider
import com.openfy.music.ui.theme.SpotifyGreen
import java.awt.Desktop
import java.net.URI

@Composable
actual fun AuthWebViewDialog(
    provider: AccountProvider,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var manualToken by remember { mutableStateOf("") }
    val authUrl = when (provider) {
        AccountProvider.SPOTIFY -> "https://accounts.spotify.com/en/login"
        AccountProvider.YOUTUBE -> "https://accounts.google.com/ServiceLogin?service=youtube"
        AccountProvider.GUEST -> ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(480.dp)
                .wrapContentHeight(),
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connect ${provider.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Text(
                    text = "Open your default browser to sign in to your ${provider.name} account. Once signed in, your playlists, liked tracks, and library will sync seamlessly with Openfy.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                Button(
                    onClick = {
                        try {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                Desktop.getDesktop().browse(URI(authUrl))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Browser Sign-in", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = Color(0xFF333333))

                Text(
                    text = "Or paste your session token / sp_dc cookie:",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = manualToken,
                    onValueChange = { manualToken = it },
                    placeholder = { Text("Paste cookie / token here", color = Color.DarkGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = Color(0xFF444444)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (manualToken.isNotBlank()) {
                                onSuccess(manualToken.trim())
                            }
                        },
                        enabled = manualToken.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                    ) {
                        Text("Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
