package com.openfy.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openfy.music.data.model.Track
import com.openfy.music.data.remote.YouTubeAudioResolver
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.components.CategoryCard
import com.openfy.music.ui.components.CategoryItem
import com.openfy.music.ui.components.TrackRowItem
import com.openfy.music.ui.theme.SpotifyBlack
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val currentTrack by PlaybackManager.currentTrack.collectAsState()

    val categories = remember {
        listOf(
            CategoryItem("Pop", listOf(Color(0xFFE91E63), Color(0xFFC2185B))),
            CategoryItem("Hip-Hop", listOf(Color(0xFFFF9800), Color(0xFFF57C00))),
            CategoryItem("Rock", listOf(Color(0xFFE53935), Color(0xFFB71C1C))),
            CategoryItem("Dance / EDM", listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))),
            CategoryItem("Chill", listOf(Color(0xFF2196F3), Color(0xFF1976D2))),
            CategoryItem("Indie", listOf(Color(0xFF009688), Color(0xFF00796B))),
            CategoryItem("R&B", listOf(Color(0xFFFFB300), Color(0xFFFFA000))),
            CategoryItem("Workout", listOf(Color(0xFF00BCD4), Color(0xFF0097A7))),
            CategoryItem("Focus & Study", listOf(Color(0xFF4CAF50), Color(0xFF388E3C))),
            CategoryItem("Gaming", listOf(Color(0xFF673AB7), Color(0xFF512DA8)))
        )
    }

    // Debounced search
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            searchResults = emptyList()
            isSearching = false
        } else {
            isSearching = true
            delay(400)
            searchResults = YouTubeAudioResolver.searchTracks(q)
            isSearching = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) {
        // Search Header & Input Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Search",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("What do you want to play?", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) White else Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = White)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF242424),
                    unfocusedContainerColor = Color(0xFF242424),
                    focusedBorderColor = White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        // Body: Categories or Search Results
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        } else if (searchQuery.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                itemsIndexed(searchResults) { index, track ->
                    TrackRowItem(
                        track = track,
                        index = index,
                        isCurrent = currentTrack?.id == track.id,
                        onClick = { PlaybackManager.playTrack(track, searchResults) }
                    )
                }
                if (searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tracks found for \"$searchQuery\"",
                                color = SpotifySubdued,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Browse All Categories Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Browse all",
                    color = White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = { searchQuery = category.name }
                        )
                    }
                }
            }
        }
    }
}
