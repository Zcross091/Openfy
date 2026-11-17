package com.openfy.music.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.openfy.music.data.AccountManager
import com.openfy.music.data.AudioQuality
import com.openfy.music.data.CacheManager
import com.openfy.music.data.SettingsManager
import com.openfy.music.data.StreamingClient
import com.openfy.music.data.model.AccountProvider
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.RvxRed
import com.openfy.music.ui.theme.SpotifyBlack
import com.openfy.music.ui.theme.SpotifyDark
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White

@Composable
fun SettingsScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val audioQuality by SettingsManager.audioQuality.collectAsState()
    val streamingClient by SettingsManager.streamingClient.collectAsState()
    val gaplessPlayback by SettingsManager.gaplessPlayback.collectAsState()
    val focusAudio by SettingsManager.focusAudio.collectAsState()
    val highResArt by SettingsManager.highResArt.collectAsState()
    val showLyrics by SettingsManager.showLyrics.collectAsState()
    val enableLocalMusic by SettingsManager.enableLocalMusic.collectAsState()

    val activeProvider by AccountManager.activeProvider.collectAsState()
    val spotifyEmail by AccountManager.spotifyEmail.collectAsState()
    val youtubeEmail by AccountManager.youtubeEmail.collectAsState()

    var showQualityDialog by remember { mutableStateOf(false) }
    var showClientDialog by remember { mutableStateOf(false) }
    var spDcInput by remember { mutableStateOf("") }
    var cacheDisplaySize by remember { mutableStateOf(CacheManager.getFormattedCacheSize()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SpotifyBlack
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        color = White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = Color(0xFF222222), thickness = 0.8.dp)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 1. Audio & Playback
                    item {
                        SettingsSectionHeader("Audio & Playback")

                        SettingsClickableRow(
                            title = "Audio Quality",
                            subtitle = "${audioQuality.title} - ${audioQuality.description}",
                            onClick = { showQualityDialog = true }
                        )

                        SettingsClickableRow(
                            title = "Streaming Client",
                            subtitle = "${streamingClient.title} - ${streamingClient.description}",
                            onClick = { showClientDialog = true }
                        )

                        SettingsToggleRow(
                            title = "Gapless Playback",
                            subtitle = "Enables continuous audio playback without silence between songs",
                            checked = gaplessPlayback,
                            onCheckedChange = { SettingsManager.setGaplessPlayback(it) }
                        )

                        SettingsToggleRow(
                            title = "Audio Focus",
                            subtitle = "Pause playback when another application takes system audio focus",
                            checked = focusAudio,
                            onCheckedChange = { SettingsManager.setFocusAudio(it) }
                        )
                    }

                    // 2. Visuals & Content
                    item {
                        SettingsSectionHeader("Visuals & Content")

                        SettingsToggleRow(
                            title = "High-Resolution Album Art",
                            subtitle = "Upscale thumbnail and banner artwork to 1024x1024 / maxres",
                            checked = highResArt,
                            onCheckedChange = { SettingsManager.setHighResArt(it) }
                        )

                        SettingsToggleRow(
                            title = "Show Synced Lyrics",
                            subtitle = "Fetch time-synced karaoke lyrics from LRCLIB automatically",
                            checked = showLyrics,
                            onCheckedChange = { SettingsManager.setShowLyrics(it) }
                        )

                        SettingsToggleRow(
                            title = "Local Device Audio",
                            subtitle = "Scan device storage and include offline audio files in Your Library",
                            checked = enableLocalMusic,
                            onCheckedChange = { SettingsManager.setEnableLocalMusic(it) }
                        )
                    }

                    // 3. Storage & Cache
                    item {
                        SettingsSectionHeader("Storage & Cache")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Cache Usage",
                                        color = White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Cached audio streams, decoded chunks & album art",
                                        color = SpotifySubdued,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = cacheDisplaySize,
                                    color = SpotifyGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        CacheManager.clearAudioCache()
                                        cacheDisplaySize = CacheManager.getFormattedCacheSize()
                                        Toast.makeText(context, "Audio cache cleared", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Audio", color = White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        CacheManager.clearImageCache()
                                        cacheDisplaySize = CacheManager.getFormattedCacheSize()
                                        Toast.makeText(context, "Image cache cleared", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Images", color = White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        CacheManager.clearAllCache()
                                        cacheDisplaySize = CacheManager.getFormattedCacheSize()
                                        Toast.makeText(context, "All cache cleared", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381A1A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6666), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear All", color = Color(0xFFFF6666), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 4. Account & Sync
                    item {
                        SettingsSectionHeader("Account & Sync")

                        // Spotify Sync Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF181818))
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (SpotifyClient.isConnected || AccountManager.isSpotifyConnected) SpotifyGreen else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Spotify Sync",
                                            color = White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (SpotifyClient.isConnected || AccountManager.isSpotifyConnected) {
                                        TextButton(
                                            onClick = {
                                                AccountManager.disconnectSpotify()
                                                AccountManager.switchAccount(AccountProvider.GUEST)
                                                Toast.makeText(context, "Spotify disconnected", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                                        ) {
                                            Text("Disconnect", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (SpotifyClient.isConnected || AccountManager.isSpotifyConnected) {
                                    val name = SpotifyClient.userProfile?.displayName ?: spotifyEmail ?: "Spotify Account"
                                    Text(
                                        text = "Connected as $name",
                                        color = SpotifyGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Playlists and Liked Songs are actively synced. Liking songs adds them to your Spotify library.",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Text(
                                        text = "Sync Liked Songs & playlists with your Spotify sp_dc cookie:",
                                        color = SpotifySubdued,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = spDcInput,
                                            onValueChange = { spDcInput = it },
                                            placeholder = {
                                                Text("Paste sp_dc cookie or token", fontSize = 11.sp, color = Color.Gray)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = White,
                                                unfocusedTextColor = White,
                                                focusedBorderColor = SpotifyGreen,
                                                unfocusedBorderColor = Color(0xFF444444)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (spDcInput.isNotBlank()) {
                                                    AccountManager.setSpotifySession(spDcInput.trim())
                                                    spDcInput = ""
                                                    Toast.makeText(context, "Syncing Spotify session...", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Sync", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // YouTube Status Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF181818))
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (AccountManager.isYouTubeConnected) RvxRed else Color(0xFF5588FF))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "YouTube Music Engine",
                                            color = White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (AccountManager.isYouTubeConnected) {
                                        TextButton(
                                            onClick = {
                                                AccountManager.disconnectYouTube()
                                                AccountManager.switchAccount(AccountProvider.GUEST)
                                                Toast.makeText(context, "YouTube account disconnected", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                                        ) {
                                            Text("Disconnect", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (AccountManager.isYouTubeConnected) {
                                    Text(
                                        text = "Active (Google: ${youtubeEmail ?: "Connected"})",
                                        color = White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "InnerTube streams resolved with personalized account data and playback recommendations.",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Text(
                                        text = "Active (Guest Mode - No login required)",
                                        color = Color(0xFF88AAFF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Anonymous high-speed InnerTube streaming with automatic cipher solving and audio extraction.",
                                        color = SpotifySubdued,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // 5. About
                    item {
                        SettingsSectionHeader("About")

                        SettingsClickableRow(
                            title = "Openfy",
                            subtitle = "Version 1.2.0 (Build 3) • Release",
                            onClick = {}
                        )

                        SettingsClickableRow(
                            title = "Source Code & GitHub",
                            subtitle = "github.com/Zcross091/Openfy",
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open",
                                    tint = SpotifySubdued,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                uriHandler.openUri("https://github.com/Zcross091/Openfy")
                            }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Credits & Acknowledgments",
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Openfy is a modern, ad-free music streaming client powered by RVX Music and InnerTube architecture with 1:1 Spotify UX.\nBuilt with Compose Multiplatform.\nLicensed under GNU General Public License v3.0.",
                                color = SpotifySubdued,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Audio Quality Picker Dialog
    if (showQualityDialog) {
        Dialog(onDismissRequest = { showQualityDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpotifyDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Select Audio Quality",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    AudioQuality.entries.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SettingsManager.setAudioQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = audioQuality == quality,
                                onClick = {
                                    SettingsManager.setAudioQuality(quality)
                                    showQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = SpotifyGreen,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = quality.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = quality.description, color = SpotifySubdued, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showQualityDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = SpotifyGreen)
                    }
                }
            }
        }
    }

    // Streaming Client Picker Dialog
    if (showClientDialog) {
        Dialog(onDismissRequest = { showClientDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpotifyDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Select Streaming Client",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    StreamingClient.entries.forEach { client ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SettingsManager.setStreamingClient(client)
                                    showClientDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = streamingClient == client,
                                onClick = {
                                    SettingsManager.setStreamingClient(client)
                                    showClientDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = SpotifyGreen,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = client.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = client.description, color = SpotifySubdued, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showClientDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = SpotifyGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = SpotifyGreen,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SpotifySubdued,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingIcon()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SpotifySubdued,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = SpotifyGreen,
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}
