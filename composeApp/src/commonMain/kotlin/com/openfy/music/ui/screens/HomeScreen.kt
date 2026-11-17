package com.openfy.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.YouTubeAudioResolver
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.components.OpenfyAsyncImage
import com.openfy.music.ui.components.TrackRowItem
import com.openfy.music.ui.theme.SpotifyBlack
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White

@Composable
fun HomeScreen(
    onNavigateToLiked: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var trendingTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val activeProvider by AccountManager.activeProvider.collectAsState()

    val greeting = remember { "Welcome back" }

    LaunchedEffect(Unit) {
        isLoading = true
        trendingTracks = YouTubeAudioResolver.getTrendingTracks(20)
        isLoading = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Greeting & Active Provider Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = greeting,
                        color = White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    val providerName = activeProvider.name.lowercase().replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Active: $providerName",
                                color = SpotifyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = White
                            )
                        }
                    }
                }
            }
        }

        // Quick Access 6-Grid (Spotify Style)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pinned Liked Songs Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF282828))
                            .clickable(onClick = onNavigateToLiked)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
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
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Liked Songs",
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Sample 1
                    if (trendingTracks.isNotEmpty()) {
                        val t = trendingTracks[0]
                        QuickCard(
                            track = t,
                            onClick = { PlaybackManager.playTrack(t, trendingTracks) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (trendingTracks.size > 1) {
                        QuickCard(
                            track = trendingTracks[1],
                            onClick = { PlaybackManager.playTrack(trendingTracks[1], trendingTracks) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (trendingTracks.size > 2) {
                        QuickCard(
                            track = trendingTracks[2],
                            onClick = { PlaybackManager.playTrack(trendingTracks[2], trendingTracks) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section Title: Today's Biggest Hits
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)) {
                Text(
                    text = "Today's Biggest Hits",
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by RVX YouTube Music Engine",
                    color = SpotifySubdued,
                    fontSize = 12.sp
                )
            }
        }

        // List of Hits
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        } else {
            itemsIndexed(trendingTracks) { index, track ->
                TrackRowItem(
                    track = track,
                    index = index,
                    isCurrent = currentTrack?.id == track.id,
                    onClick = { PlaybackManager.playTrack(track, trendingTracks) }
                )
            }
        }
    }
}

@Composable
private fun QuickCard(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF282828))
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OpenfyAsyncImage(
                url = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp),
                placeholder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF383838)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = White
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = track.title,
                color = White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}
