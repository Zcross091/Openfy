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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openfy.music.data.model.Album
import com.openfy.music.data.model.Artist
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

@Composable
fun LikedSongsScreen(
    onOpenSettings: () -> Unit = {},
    onOpenSpotifyAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Playlists") }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playlistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var likedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val isConnected = SpotifyClient.isConnected

    // Progressive Library Loader
    LaunchedEffect(isConnected) {
        isLoading = true
        if (isConnected) {
            likedTracks = SpotifyClient.getLikedTracks(50)
            playlists = SpotifyClient.getUserPlaylists(50)
            artists = SpotifyClient.getFollowedArtists(50)
            albums = SpotifyClient.getSavedAlbums(50)
            isLoading = false

            // 2. Background progressive sync
            launch(Dispatchers.IO) {
                SpotifyClient.getAllLikedTracksProgressive { updatedTracks ->
                    likedTracks = updatedTracks
                }
            }
            launch(Dispatchers.IO) {
                SpotifyClient.getAllUserPlaylistsProgressive { updatedPlaylists ->
                    playlists = updatedPlaylists
                }
            }
        } else {
            likedTracks = emptyList()
            playlists = emptyList()
            artists = emptyList()
            albums = emptyList()
            isLoading = false
        }
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OpenfyAsyncImage(
                                url = p.coverUrl,
                                contentDescription = p.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                placeholder = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF282828)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = SpotifySubdued,
                                            modifier = Modifier.size(40.dp)
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${playlistTracks.size} songs",
                                    color = SpotifySubdued,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Play Button Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
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

    // Main Library View
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) SpotifyGreen else Color(0xFF2E2E2E))
                                .clickable(onClick = onOpenSpotifyAuth),
                            contentAlignment = Alignment.Center
                        ) {
                            if (SpotifyClient.userProfile?.avatarUrl != null) {
                                OpenfyAsyncImage(
                                    url = SpotifyClient.userProfile?.avatarUrl,
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = if (isConnected) Color.Black else White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Your Library",
                            color = White,
                            fontSize = 24.sp,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Filter Chips: Playlists, Artists, Albums, Liked Songs
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChipPill(
                            label = "Playlists",
                            isSelected = selectedFilter == "Playlists",
                            onClick = { selectedFilter = "Playlists" }
                        )
                    }
                    item {
                        FilterChipPill(
                            label = "Liked Songs",
                            isSelected = selectedFilter == "Liked Songs",
                            onClick = { selectedFilter = "Liked Songs" }
                        )
                    }
                    item {
                        FilterChipPill(
                            label = "Artists",
                            isSelected = selectedFilter == "Artists",
                            onClick = { selectedFilter = "Artists" }
                        )
                    }
                    item {
                        FilterChipPill(
                            label = "Albums",
                            isSelected = selectedFilter == "Albums",
                            onClick = { selectedFilter = "Albums" }
                        )
                    }
                }
            }
        }

        // Spotify Connect Promo Card if user is not signed in
        if (!isConnected) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222222))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Connect to view your Spotify Library",
                            color = White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sign in to see your Liked Songs, all personal playlists, followed artists, and saved albums.",
                            color = SpotifySubdued,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenSpotifyAuth,
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Connect Spotify", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // --- FILTER: LIKED SONGS ---
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
                                )
                                .clickable {
                                    if (likedTracks.isNotEmpty()) {
                                        PlaybackManager.playTrack(likedTracks.first(), likedTracks)
                                    }
                                },
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
                            Text(
                                text = "${likedTracks.size} songs",
                                color = SpotifySubdued,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (likedTracks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { PlaybackManager.playTrack(likedTracks.first(), likedTracks) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play All",
                                    tint = Black,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(likedTracks) { index, track ->
                    TrackRowItem(
                        track = track,
                        index = index,
                        isCurrent = currentTrack?.id == track.id,
                        onClick = { PlaybackManager.playTrack(track, likedTracks) }
                    )
                }
            } else if (!isLoading && isConnected) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No liked songs found on Spotify.", color = SpotifySubdued, fontSize = 14.sp)
                    }
                }
            }
        }

        // --- FILTER: PLAYLISTS ---
        if (selectedFilter == "Playlists") {
            // Pinned Liked Songs Row at Top of Playlists
            item {
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
                            .clip(RoundedCornerShape(4.dp))
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
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Liked Songs",
                            color = White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Playlist • ${likedTracks.size} songs",
                            color = SpotifySubdued,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // User Playlists
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
                            .clip(RoundedCornerShape(4.dp)),
                        placeholder = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF282828)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = SpotifySubdued,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.title,
                            color = White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Playlist • ${playlist.trackCount} songs",
                            color = SpotifySubdued,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // --- FILTER: ARTISTS ---
        if (selectedFilter == "Artists") {
            items(artists) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlaylist = Playlist(
                                id = artist.id ?: "artist_${artist.name}",
                                title = "${artist.name} Radio",
                                coverUrl = artist.avatarUrl,
                                trackCount = 20,
                                source = "spotify"
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OpenfyAsyncImage(
                        url = artist.avatarUrl,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        placeholder = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF282828)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SpotifySubdued,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            color = White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (artist.followers > 0) "Artist • ${artist.followers} followers" else "Artist",
                            color = SpotifySubdued,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            if (artists.isEmpty() && !isLoading && isConnected) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No followed artists found on Spotify.", color = SpotifySubdued, fontSize = 14.sp)
                    }
                }
            }
        }

        // --- FILTER: ALBUMS ---
        if (selectedFilter == "Albums") {
            items(albums) { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlaylist = Playlist(
                                id = album.id ?: "album_${album.name}",
                                title = album.name,
                                coverUrl = album.coverUrl,
                                trackCount = 15,
                                source = "spotify"
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OpenfyAsyncImage(
                        url = album.coverUrl,
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        placeholder = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF282828)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = SpotifySubdued,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.name,
                            color = White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val subtitle = listOfNotNull(album.artistName, album.releaseYear).joinToString(" • ")
                        Text(
                            text = if (subtitle.isNotBlank()) "Album • $subtitle" else "Album",
                            color = SpotifySubdued,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            if (albums.isEmpty() && !isLoading && isConnected) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No saved albums found on Spotify.", color = SpotifySubdued, fontSize = 14.sp)
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
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) SpotifyGreen else Color(0xFF282828))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Black else White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
