package com.openfy.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.data.remote.YouTubeAudioResolver
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.components.OpenfyAsyncImage
import com.openfy.music.ui.components.TrackRowItem
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.SpotifyBlack
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White

@Composable
fun LikedSongsScreen(
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Playlists") } // "Playlists" or "Liked Songs"
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playlistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var likedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val activeProvider by AccountManager.activeProvider.collectAsState()

    // Load initial data
    LaunchedEffect(activeProvider) {
        isLoading = true
        likedTracks = AccountManager.getLikedTracks()
        playlists = AccountManager.getUserPlaylists()
        isLoading = false
    }

    // Load selected playlist tracks
    LaunchedEffect(selectedPlaylist) {
        val p = selectedPlaylist
        if (p != null) {
            isLoading = true
            playlistTracks = if (p.source == "spotify" && SpotifyClient.isConnected) {
                SpotifyClient.getPlaylistTracks(p.id)
            } else {
                YouTubeAudioResolver.getTrendingTracks(p.trackCount.coerceAtLeast(10))
            }
            isLoading = false
        }
    }

    // If a playlist is selected, show Playlist Detail View
    if (selectedPlaylist != null) {
        val p = selectedPlaylist!!
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(SpotifyBlack),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Back navigation & Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2C3E50), Color(0xFF1A1A24), SpotifyBlack)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Column {
                        IconButton(
                            onClick = { selectedPlaylist = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OpenfyAsyncImage(
                                url = p.coverUrl,
                                contentDescription = p.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                placeholder = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF333333)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            )

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
                                    text = p.title,
                                    color = White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!p.description.isNullOrEmpty()) {
                                    Text(
                                        text = p.description,
                                        color = SpotifySubdued,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val sourceName = p.source.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                }
                                Text(
                                    text = "$sourceName • ${playlistTracks.size.coerceAtLeast(p.trackCount)} songs",
                                    color = SpotifySubdued,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Play Button
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
                                if (playlistTracks.isNotEmpty()) {
                                    PlaybackManager.playTrack(playlistTracks.first(), playlistTracks)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play playlist",
                                tint = Black,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            // Tracks
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
            } else {
                itemsIndexed(playlistTracks) { index, track ->
                    TrackRowItem(
                        track = track,
                        index = index,
                        isCurrent = currentTrack?.id == track.id,
                        onClick = { PlaybackManager.playTrack(track, playlistTracks) }
                    )
                }
            }
        }
        return
    }

    // Main Library View (Filter Pills + Playlists / Liked Songs)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top Filter Bar (Spotify Style)
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
                        text = "Your Library",
                        color = White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

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
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipPill(
                        label = "Playlists",
                        isSelected = selectedFilter == "Playlists",
                        onClick = { selectedFilter = "Playlists" }
                    )
                    FilterChipPill(
                        label = "Liked Songs",
                        isSelected = selectedFilter == "Liked Songs",
                        onClick = { selectedFilter = "Liked Songs" }
                    )
                }
            }
        }

        // Show Liked Songs Header when on "Liked Songs" filter
        if (selectedFilter == "Liked Songs") {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF3B1E78), Color(0xFF200F48), SpotifyBlack)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
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
                                modifier = Modifier.size(44.dp)
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
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val providerName = activeProvider.name.lowercase().replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                            }
                            Text(
                                text = "$providerName Library • ${likedTracks.size} songs",
                                color = SpotifySubdued,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Play All Liked Songs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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

            // Liked Tracks List
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
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
                    val providerName = activeProvider.name.lowercase().replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Songs you like will appear here.\nTap the Heart icon while listening to save tracks to your $providerName library!",
                            color = SpotifySubdued,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            // "Playlists" view: Shows Liked Songs card + all user/curated playlists!
            item {
                // Pinned Liked Songs Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedFilter = "Liked Songs" }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(6.dp))
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
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Liked Songs",
                            color = SpotifyGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Playlist • ${likedTracks.size} songs",
                            color = SpotifySubdued,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Playlist items list
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                }
            } else {
                items(playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlaylist = playlist }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OpenfyAsyncImage(
                            url = playlist.coverUrl,
                            contentDescription = playlist.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            placeholder = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF333333)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                color = White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val sourceName = playlist.source.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                            }
                            Text(
                                text = "Playlist • $sourceName",
                                color = SpotifySubdued,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) SpotifyGreen else Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Black else White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
