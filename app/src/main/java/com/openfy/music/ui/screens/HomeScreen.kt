package com.openfy.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.data.remote.YouTubeAudioResolver
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.components.OpenfyAsyncImage
import com.openfy.music.ui.components.TrackRowItem
import com.openfy.music.ui.theme.SpotifyBlack
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigateToLiked: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenSpotifyAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var trendingTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var recentlyPlayed by remember { mutableStateOf<List<Track>>(emptyList()) }
    var userPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val isConnected = SpotifyClient.isConnected
    val userProfile = SpotifyClient.userProfile

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LaunchedEffect(isConnected) {
        isLoading = true
        if (isConnected) {
            recentlyPlayed = SpotifyClient.getRecentlyPlayed(15)
            userPlaylists = SpotifyClient.getUserPlaylists(6)
        }
        trendingTracks = YouTubeAudioResolver.getTrendingTracks(20)
        isLoading = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Spotify Top Header: Avatar + Greeting + Settings Gear
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Avatar Pill
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) SpotifyGreen else Color(0xFF2E2E2E))
                            .clickable(onClick = onOpenSpotifyAuth),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile?.avatarUrl != null) {
                            OpenfyAsyncImage(
                                url = userProfile.avatarUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = if (isConnected) Color.Black else White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = greeting,
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
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

        // Spotify Connect Promo Banner (if not logged in)
        if (!isConnected) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1DB954), Color(0xFF14833B))
                            )
                        )
                        .clickable(onClick = onOpenSpotifyAuth)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connect your Spotify Account",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Sync your playlists, liked songs, and artists.",
                                color = Color(0xFF0F3D1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = onOpenSpotifyAuth,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = White),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

                    // Card 2: First playlist or first trending track
                    if (userPlaylists.isNotEmpty()) {
                        val p = userPlaylists[0]
                        QuickPlaylistCard(playlist = p, onClick = onNavigateToLiked, modifier = Modifier.weight(1f))
                    } else if (trendingTracks.isNotEmpty()) {
                        val t = trendingTracks[0]
                        QuickCard(track = t, onClick = { PlaybackManager.playTrack(t, trendingTracks) }, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (userPlaylists.size > 1) {
                        QuickPlaylistCard(playlist = userPlaylists[1], onClick = onNavigateToLiked, modifier = Modifier.weight(1f))
                    } else if (trendingTracks.size > 1) {
                        QuickCard(track = trendingTracks[1], onClick = { PlaybackManager.playTrack(trendingTracks[1], trendingTracks) }, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (userPlaylists.size > 2) {
                        QuickPlaylistCard(playlist = userPlaylists[2], onClick = onNavigateToLiked, modifier = Modifier.weight(1f))
                    } else if (trendingTracks.size > 2) {
                        QuickCard(track = trendingTracks[2], onClick = { PlaybackManager.playTrack(trendingTracks[2], trendingTracks) }, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Recently Played Horizontal Carousel (if logged into Spotify)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Text(
                        text = "Recently Played",
                        color = White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(recentlyPlayed) { track ->
                            Column(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { PlaybackManager.playTrack(track, recentlyPlayed) }
                            ) {
                                OpenfyAsyncImage(
                                    url = track.coverUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(130.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = track.title,
                                    color = White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artistNames,
                                    color = SpotifySubdued,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                    text = "Popular across Spotify",
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

@Composable
private fun QuickPlaylistCard(
    playlist: Playlist,
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
                url = playlist.coverUrl,
                contentDescription = playlist.title,
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
                text = playlist.title,
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
