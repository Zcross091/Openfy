package com.openfy.music.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.openfy.music.data.AccountManager
import com.openfy.music.data.AudioQuality
import com.openfy.music.data.SettingsManager
import com.openfy.music.data.StreamingClient
import com.openfy.music.data.model.Album
import com.openfy.music.data.model.Artist
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.LyricsClient
import com.openfy.music.data.remote.SpotifyClient
import com.openfy.music.data.remote.YouTubeAudioResolver
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.components.CategoryCard
import com.openfy.music.ui.theme.*
import com.openfy.music.util.ArtworkUtils
import kotlinx.coroutines.launch

enum class DesktopNavTab {
    HOME,
    SEARCH,
    LIBRARY,
    LIKED_SONGS,
    SETTINGS,
    LYRICS
}

@Composable
fun SpotifyDesktopLayout() {
    var currentTab by remember { mutableStateOf(DesktopNavTab.HOME) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var isRightPanelOpen by remember { mutableStateOf(false) }
    var showProfileDropdown by remember { mutableStateOf(false) }

    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val isLoading by PlaybackManager.isLoading.collectAsState()
    val currentPositionMs by PlaybackManager.currentPositionMs.collectAsState()
    val durationMs by PlaybackManager.durationMs.collectAsState()
    val currentClient by PlaybackManager.currentStreamingClient.collectAsState()
    val isShuffle by PlaybackManager.isShuffle.collectAsState()
    val repeatMode by PlaybackManager.repeatMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        bottomBar = {
            SpotifyDesktopPlayerBar(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                isLoading = isLoading,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                currentClient = currentClient,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                isRightPanelOpen = isRightPanelOpen,
                onToggleRightPanel = { isRightPanelOpen = !isRightPanelOpen },
                onToggleLyrics = {
                    currentTab = if (currentTab == DesktopNavTab.LYRICS) DesktopNavTab.HOME else DesktopNavTab.LYRICS
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // 1. Top Windows Desktop Header
            SpotifyDesktopTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { q ->
                    searchQuery = q
                    if (q.isNotBlank() && currentTab != DesktopNavTab.SEARCH) {
                        currentTab = DesktopNavTab.SEARCH
                    }
                },
                onNavigateHome = {
                    selectedPlaylist = null
                    currentTab = DesktopNavTab.HOME
                },
                currentTab = currentTab,
                onOpenSettings = { currentTab = DesktopNavTab.SETTINGS },
                showProfileDropdown = showProfileDropdown,
                onToggleProfileDropdown = { showProfileDropdown = !showProfileDropdown }
            )

            // 2. Main Desktop Body (Sidebar + Center Content + Optional Right Panel)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Sidebar: Your Library & Nav
                SpotifyDesktopSidebar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        selectedPlaylist = null
                        currentTab = tab
                    },
                    onSelectLikedSongs = {
                        selectedPlaylist = null
                        currentTab = DesktopNavTab.LIKED_SONGS
                    },
                    onSelectPlaylist = { p ->
                        selectedPlaylist = p
                    }
                )

                // Center Stage: Dynamic Content Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    color = Color(0xFF121212),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    when {
                        currentTab == DesktopNavTab.SETTINGS -> {
                            DesktopSettingsView()
                        }
                        currentTab == DesktopNavTab.LYRICS -> {
                            DesktopLyricsView(currentTrack, currentPositionMs)
                        }
                        currentTab == DesktopNavTab.SEARCH -> {
                            DesktopSearchView(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it }
                            )
                        }
                        selectedPlaylist != null -> {
                            DesktopPlaylistDetailView(
                                playlist = selectedPlaylist!!,
                                onBack = { selectedPlaylist = null }
                            )
                        }
                        currentTab == DesktopNavTab.LIKED_SONGS -> {
                            DesktopLikedSongsView()
                        }
                        else -> {
                            DesktopHomeView(
                                onNavigateToLiked = { currentTab = DesktopNavTab.LIKED_SONGS },
                                onSelectPlaylist = { selectedPlaylist = it }
                            )
                        }
                    }
                }

                // Optional Right Panel (Now Playing Details & Lyrics)
                if (isRightPanelOpen && currentTrack != null) {
                    SpotifyDesktopRightPanel(
                        track = currentTrack!!,
                        currentPositionMs = currentPositionMs,
                        onClose = { isRightPanelOpen = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyDesktopTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateHome: () -> Unit,
    currentTab: DesktopNavTab,
    onOpenSettings: () -> Unit,
    showProfileDropdown: Boolean,
    onToggleProfileDropdown: () -> Unit
) {
    val activeAccount by AccountManager.activeAccount.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: History Back/Forward & Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121212))
                    .clickable { onNavigateHome() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Openfy Logo branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateHome() }
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Openfy",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center: Spotify Search Pill
        Surface(
            modifier = Modifier
                .width(420.dp)
                .height(40.dp),
            color = Color(0xFF242424),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text("What do you want to play?", color = Color.Gray, fontSize = 13.sp)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.LightGray)
                    }
                }
            }
        }

        // Right: Profile Pill & Dropdown
        Box {
            Surface(
                onClick = onToggleProfileDropdown,
                color = Color(0xFF121212),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (activeAccount?.displayName ?: "G").take(1).uppercase(),
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeAccount?.displayName ?: "Guest Library",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showProfileDropdown,
                onDismissRequest = onToggleProfileDropdown,
                modifier = Modifier.background(Color(0xFF282828))
            ) {
                DropdownMenuItem(
                    text = { Text("Settings", color = Color.White) },
                    onClick = {
                        onToggleProfileDropdown()
                        onOpenSettings()
                    },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text(if (activeAccount?.provider?.name == "SPOTIFY") "Spotify Connected" else "Connect Spotify", color = SpotifyGreen) },
                    onClick = {
                        onToggleProfileDropdown()
                        onOpenSettings()
                    },
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SpotifyGreen) }
                )
            }
        }
    }
}

@Composable
private fun SpotifyDesktopSidebar(
    currentTab: DesktopNavTab,
    onTabSelected: (DesktopNavTab) -> Unit,
    onSelectLikedSongs: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit
) {
    val likedTracks by AccountManager.likedTracks.collectAsState()
    val playlists by AccountManager.playlists.collectAsState()

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Navigation Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF121212),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                SidebarNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentTab == DesktopNavTab.HOME,
                    onClick = { onTabSelected(DesktopNavTab.HOME) }
                )
                Spacer(modifier = Modifier.height(14.dp))
                SidebarNavItem(
                    icon = Icons.Default.Search,
                    label = "Search",
                    isSelected = currentTab == DesktopNavTab.SEARCH,
                    onClick = { onTabSelected(DesktopNavTab.SEARCH) }
                )
            }
        }

        // Your Library Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color(0xFF121212),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onTabSelected(DesktopNavTab.LIBRARY) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Your Library",
                            tint = Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Your Library",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { /* create playlist */ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Library Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = { },
                        label = { Text("Playlists", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF242424),
                            labelColor = Color.White
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = false,
                        onClick = { },
                        label = { Text("Artists", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF242424),
                            labelColor = Color.White
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Library Items
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pinned Liked Songs
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (currentTab == DesktopNavTab.LIKED_SONGS) Color(0xFF282828) else Color.Transparent)
                                .clickable { onSelectLikedSongs() }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF450AF5), Color(0xFF8E8EE5), Color(0xFFC4EFD9))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Liked Songs",
                                    color = if (currentTab == DesktopNavTab.LIKED_SONGS) SpotifyGreen else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = SpotifyGreen, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Playlist • ${likedTracks.size} songs",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // User / Guest Playlists
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSelectPlaylist(playlist) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = playlist.coverUrl,
                                contentDescription = playlist.name,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF282828))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Playlist • ${playlist.trackCount} songs",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun DesktopHomeView(
    onNavigateToLiked: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit
) {
    val likedTracks by AccountManager.likedTracks.collectAsState()
    var trendingTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    LaunchedEffect(Unit) {
        trendingTracks = YouTubeAudioResolver.searchTracks("Top Global Hits 2024", limit = 12)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Greeting
        item {
            Text(
                text = "Good afternoon",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick 6-card Access Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Card 1: Liked Songs
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    title = "Liked Songs",
                    gradient = Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFF8E8EE5))),
                    icon = Icons.Default.Favorite,
                    onClick = onNavigateToLiked,
                    onPlay = {
                        if (likedTracks.isNotEmpty()) PlaybackManager.playTrack(likedTracks[0], likedTracks)
                    }
                )

                // Card 2: Today's Top Hits
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    title = "Today's Top Hits",
                    gradient = Brush.linearGradient(listOf(Color(0xFF0D5EAF), Color(0xFF1396EA))),
                    icon = Icons.Default.Whatshot,
                    onClick = {
                        if (trendingTracks.isNotEmpty()) {
                            onSelectPlaylist(
                                Playlist(id = "top_hits", name = "Today's Top Hits", tracks = trendingTracks)
                            )
                        }
                    },
                    onPlay = {
                        if (trendingTracks.isNotEmpty()) PlaybackManager.playTrack(trendingTracks[0], trendingTracks)
                    }
                )

                // Card 3: Global Top 50
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    title = "Global Top 50",
                    gradient = Brush.linearGradient(listOf(Color(0xFF0F7540), Color(0xFF1ED760))),
                    icon = Icons.Default.BarChart,
                    onClick = { },
                    onPlay = {
                        if (trendingTracks.isNotEmpty()) PlaybackManager.playTrack(trendingTracks.last(), trendingTracks)
                    }
                )
            }
        }

        // Shelf 1: Trending Now
        item {
            Text(
                text = "Trending Now",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(trendingTracks) { track ->
                    DesktopTrackCard(
                        track = track,
                        onClick = { PlaybackManager.playTrack(track, trendingTracks) },
                        onPlay = { PlaybackManager.playTrack(track, trendingTracks) }
                    )
                }
            }
        }

        // Shelf 2: Made For You
        item {
            Text(
                text = "Made For You",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(likedTracks.take(8)) { track ->
                    DesktopTrackCard(
                        track = track,
                        onClick = { PlaybackManager.playTrack(track, likedTracks) },
                        onPlay = { PlaybackManager.playTrack(track, likedTracks) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    modifier: Modifier = Modifier,
    title: String,
    gradient: Brush,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = Color(0xFF282828)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DesktopTrackCard(
    track: Track,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = Color(0xFF181818)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(152.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF282828))
            ) {
                AsyncImage(
                    model = ArtworkUtils.getHighResArtworkUrl(track.coverUrl),
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.artistNames,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DesktopLikedSongsView() {
    val likedTracks by AccountManager.likedTracks.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val currentTrack by PlaybackManager.currentTrack.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF450AF5), Color(0xFF121212))
                    )
                )
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Box(
                    modifier = Modifier
                        .size(192.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF450AF5), Color(0xFF8E8EE5), Color(0xFFC4EFD9))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text("PLAYLIST", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Liked Songs", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Guest User • ${likedTracks.size} songs",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    if (likedTracks.isNotEmpty()) {
                        if (isPlaying) PlaybackManager.togglePlay()
                        else PlaybackManager.playTrack(likedTracks[0], likedTracks)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Engine Switcher Badge
            Surface(
                onClick = { PlaybackManager.switchSource() },
                color = Color(0xFF282828),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = PlaybackManager.currentStreamingClient.collectAsState().value.title,
                        color = SpotifyGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Desktop Track Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .border(width = 1.dp, color = Color(0xFF282828)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.width(36.dp))
            Text("Title", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(2f))
            Text("Album", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
            Text("Duration", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.width(60.dp))
        }

        // Track Table Rows
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
        ) {
            itemsIndexed(likedTracks) { index, track ->
                val isCurrent = currentTrack?.id == track.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { PlaybackManager.playTrack(track, likedTracks) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = if (isCurrent) SpotifyGreen else Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.width(36.dp)
                    )
                    Row(
                        modifier = Modifier.weight(2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ArtworkUtils.getHighResArtworkUrl(track.coverUrl),
                            contentDescription = track.title,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = track.title,
                                color = if (isCurrent) SpotifyGreen else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artistNames,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = track.album?.name ?: "Openfy",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDuration(track.durationSeconds.toLong() * 1000L),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopPlaylistDetailView(
    playlist: Playlist,
    onBack: () -> Unit
) {
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val currentTrack by PlaybackManager.currentTrack.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2E77D0), Color(0xFF121212))
                    )
                )
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier
                        .size(192.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF282828))
                )
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text("PLAYLIST", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(playlist.name, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${playlist.trackCount} songs",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    if (playlist.tracks.isNotEmpty()) {
                        PlaybackManager.playTrack(playlist.tracks[0], playlist.tracks)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Tracks list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
        ) {
            itemsIndexed(playlist.tracks) { index, track ->
                val isCurrent = currentTrack?.id == track.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { PlaybackManager.playTrack(track, playlist.tracks) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = if (isCurrent) SpotifyGreen else Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.width(36.dp)
                    )
                    Row(
                        modifier = Modifier.weight(2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ArtworkUtils.getHighResArtworkUrl(track.coverUrl),
                            contentDescription = track.title,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = track.title,
                                color = if (isCurrent) SpotifyGreen else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artistNames,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = track.album?.name ?: "Openfy",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDuration(track.durationSeconds.toLong() * 1000L),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopSearchView(
    query: String,
    onQueryChange: (String) -> Unit
) {
    var searchResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.length >= 2) {
            isSearching = true
            searchResults = YouTubeAudioResolver.searchTracks(query)
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (query.isEmpty()) {
            item {
                Text(
                    text = "Browse All",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                val categories = listOf(
                    "Pop" to Color(0xFF8D67AB),
                    "Hip-Hop" to Color(0xFFBA5D07),
                    "Rock" to Color(0xFFE91429),
                    "Dance" to Color(0xFFD84000),
                    "Indie" to Color(0xFF608108),
                    "R&B" to Color(0xFF1E3264),
                    "Chill" to Color(0xFF477D95),
                    "Workout" to Color(0xFF777777)
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (chunk in categories.chunked(4)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (cat in chunk) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onQueryChange(cat.first) },
                                    color = cat.second
                                ) {
                                    Box(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = cat.first,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Search Results for \"$query\"",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isSearching) {
                item {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            } else {
                items(searchResults) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { PlaybackManager.playTrack(track, searchResults) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ArtworkUtils.getHighResArtworkUrl(track.coverUrl),
                            contentDescription = track.title,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = track.artistNames,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = { PlaybackManager.playTrack(track, searchResults) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = SpotifyGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopLyricsView(
    track: Track?,
    currentPositionMs: Long
) {
    if (track == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Play a track to view synchronized lyrics", color = Color.Gray)
        }
        return
    }

    var lyrics by remember { mutableStateOf<List<com.openfy.music.data.model.LyricLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(track.id) {
        isLoading = true
        lyrics = LyricsClient.getSyncedLyrics(track.title, track.artistNames, track.durationSeconds)
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(32.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.align(Alignment.Center))
        } else if (lyrics.isEmpty()) {
            Text(
                "Lyrics not available for this track",
                color = Color.Gray,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(lyrics) { line ->
                    val isActive = currentPositionMs >= line.startTimeMs &&
                            (line.endTimeMs == 0L || currentPositionMs <= line.endTimeMs)
                    Text(
                        text = line.text,
                        color = if (isActive) Color.White else Color(0xFF6A6A6A),
                        fontSize = if (isActive) 26.sp else 22.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { PlaybackManager.seekTo(line.startTimeMs) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopSettingsView() {
    val currentClient by SettingsManager.streamingClient.collectAsState()
    val currentQuality by SettingsManager.audioQuality.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // Section: Streaming Engine
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("STREAMING AUDIO ENGINE", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Select your primary audio resolver. Openfy automatically fails over if a source is blocked.", color = Color.Gray, fontSize = 13.sp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamingClient.entries.forEach { client ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { SettingsManager.setStreamingClient(client) }
                            .background(if (currentClient == client) Color(0xFF282828) else Color.Transparent)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentClient == client,
                            onClick = { SettingsManager.setStreamingClient(client) },
                            colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(client.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(client.description, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Audio Quality
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AUDIO QUALITY", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { SettingsManager.setAudioQuality(quality) }
                            .background(if (currentQuality == quality) Color(0xFF282828) else Color.Transparent)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentQuality == quality,
                            onClick = { SettingsManager.setAudioQuality(quality) },
                            colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(quality.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(quality.description, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyDesktopRightPanel(
    track: Track,
    currentPositionMs: Long,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        color = Color(0xFF121212),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Now Playing",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = ArtworkUtils.getHighResArtworkUrl(track.coverUrl),
                contentDescription = track.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = track.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track.artistNames,
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // About the artist / album
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF242424),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Album", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(track.album?.name ?: "Single", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SpotifyDesktopPlayerBar(
    currentTrack: Track?,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    currentClient: StreamingClient,
    isShuffle: Boolean,
    repeatMode: Int,
    isRightPanelOpen: Boolean,
    onToggleRightPanel: () -> Unit,
    onToggleLyrics: () -> Unit
) {
    var volume by remember { mutableStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        color = Color(0xFF181818),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Track Info
            Row(
                modifier = Modifier.width(300.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentTrack != null) {
                    AsyncImage(
                        model = ArtworkUtils.getHighResArtworkUrl(currentTrack.coverUrl),
                        contentDescription = currentTrack.title,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentTrack.artistNames,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { AccountManager.toggleLikeTrack(currentTrack) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (currentTrack.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (currentTrack.isLiked) SpotifyGreen else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Center Group: Controls & Scrubber
            Column(
                modifier = Modifier.width(500.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top control buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { PlaybackManager.toggleShuffle() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) SpotifyGreen else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { PlaybackManager.skipPrevious() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    // Main Play/Pause Button
                    IconButton(
                        onClick = { PlaybackManager.togglePlay() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { PlaybackManager.skipNext() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    IconButton(
                        onClick = { PlaybackManager.toggleRepeat() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode > 0) SpotifyGreen else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Scrubber Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDuration(currentPositionMs),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    val maxDuration = if (durationMs > 0) durationMs else 1L
                    val progressFraction = (currentPositionMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)

                    Slider(
                        value = progressFraction,
                        onValueChange = { fraction ->
                            val seekTarget = (fraction * maxDuration).toLong()
                            PlaybackManager.seekTo(seekTarget)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = SpotifyGreen,
                            inactiveTrackColor = Color(0xFF4D4D4D)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatDuration(durationMs),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            // Right Group: Engine Switcher, Lyrics & Volume
            Row(
                modifier = Modifier.width(300.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Interactive Engine Switcher Badge
                Surface(
                    onClick = { PlaybackManager.switchSource() },
                    color = Color(0xFF282828),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Switch Engine", tint = SpotifyGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currentClient.title,
                            color = SpotifyGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onToggleLyrics,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Lyrics", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onToggleRightPanel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ViewSidebar,
                        contentDescription = "Now Playing View",
                        tint = if (isRightPanelOpen) SpotifyGreen else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Volume
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        PlaybackManager.setVolume(if (isMuted) 0f else volume)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted || volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Slider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = { newVol ->
                        volume = newVol
                        isMuted = false
                        PlaybackManager.setVolume(newVol)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF4D4D4D)
                    ),
                    modifier = Modifier.width(90.dp)
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"
}
