package com.openfy.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.Track
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.components.TrackRowItem
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.SpotifyBlack
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White

@Composable
fun LikedSongsScreen(
    modifier: Modifier = Modifier
) {
    var likedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val activeProvider by AccountManager.activeProvider.collectAsState()

    LaunchedEffect(activeProvider) {
        isLoading = true
        likedTracks = AccountManager.getLikedTracks()
        isLoading = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Hero Header (Spotify Purple Gradient)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3B1E78), Color(0xFF200F48), SpotifyBlack)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big Heart Artwork Square
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF450AF5), Color(0xFF8E8EE5))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "PLAYLIST",
                            color = White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Liked Songs",
                            color = White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${activeProvider.name.lowercase().replaceFirstChar { it.uppercase() }} Library • ${likedTracks.size} songs",
                            color = SpotifySubdued,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Action Play All Button Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (likedTracks.isNotEmpty()) {
                                PlaybackManager.playTrack(likedTracks.first(), likedTracks)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play all",
                            tint = Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // Tracks List
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        } else if (likedTracks.isNotEmpty()) {
            itemsIndexed(likedTracks) { index, track ->
                TrackRowItem(
                    track = track,
                    index = index,
                    isCurrent = currentTrack?.id == track.id,
                    onClick = { PlaybackManager.playTrack(track, likedTracks) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Songs you like will appear here.\nTap the Heart icon while listening to save tracks to your ${activeProvider.name.lowercase().replaceFirstChar { it.uppercase() }} library!",
                        color = SpotifySubdued,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
