package com.openfy.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.openfy.music.data.AccountManager
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.SpotifyDark
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun NowPlayingSheet(
    onDismiss: () -> Unit,
    onOpenLyrics: () -> Unit
) {
    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val isLoading by PlaybackManager.isLoading.collectAsState()
    val positionMs by PlaybackManager.currentPositionMs.collectAsState()
    val durationMs by PlaybackManager.durationMs.collectAsState()
    val isShuffle by PlaybackManager.isShuffle.collectAsState()
    val repeatMode by PlaybackManager.repeatMode.collectAsState()
    val activeProvider by AccountManager.activeProvider.collectAsState()
    val currentStreamingClient by PlaybackManager.currentStreamingClient.collectAsState()
    val engineStatus by PlaybackManager.engineStatus.collectAsState()

    val scope = rememberCoroutineScope()
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

    if (currentTrack == null) return

    fun formatTime(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%d:%02d".format(mins, secs)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A282E), Color(0xFF141316), Black)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Dismiss Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM",
                            color = SpotifySubdued,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${activeProvider.name.lowercase().replaceFirstChar { it.uppercase() }} Library",
                            color = White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(modifier = Modifier.size(32.dp))
                }

                // Big Album Artwork
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OpenfyAsyncImage(
                        url = currentTrack?.coverUrl,
                        contentDescription = currentTrack?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        placeholder = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SpotifyDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = SpotifySubdued,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }
                    )
                }

                // Controls Area
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Track Title & Heart Like Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack?.title ?: "",
                                color = White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTrack?.artistNames ?: "",
                                color = SpotifySubdued,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isLoading && engineStatus != null && (engineStatus!!.contains("fail", ignoreCase = true) || engineStatus!!.contains("switch", ignoreCase = true))) {
                                Text(
                                    text = engineStatus!!,
                                    color = SpotifyGreen,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Add to Playlist (+) button
                            IconButton(
                                onClick = { showAddToPlaylistSheet = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Add to playlist",
                                    tint = SpotifySubdued,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // 1-Tap Save to Liked Songs (Heart)
                            IconButton(
                                onClick = {
                                    currentTrack?.let {
                                        scope.launch { AccountManager.toggleLike(it) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTrack?.isLiked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (currentTrack?.isLiked == true) SpotifyGreen else SpotifySubdued,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Slider
                    var isSeeking by remember { mutableStateOf(false) }
                    var seekProgress by remember { mutableFloatStateOf(0f) }

                    val progress = if (isSeeking) seekProgress
                    else if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    else 0f

                    Slider(
                        value = progress,
                        onValueChange = {
                            isSeeking = true
                            seekProgress = it
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            PlaybackManager.seekTo((seekProgress * durationMs).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = White,
                            activeTrackColor = White,
                            inactiveTrackColor = Color(0xFF444444)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(if (isSeeking) (seekProgress * durationMs).toLong() else positionMs),
                            color = SpotifySubdued,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = SpotifySubdued,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle
                        IconButton(onClick = { PlaybackManager.toggleShuffle() }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) SpotifyGreen else SpotifySubdued,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Previous
                        IconButton(onClick = { PlaybackManager.skipPrevious() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Play/Pause
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(White),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { PlaybackManager.togglePlay() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Black,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Black,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        // Next
                        IconButton(onClick = { PlaybackManager.skipNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Repeat
                        IconButton(onClick = { PlaybackManager.toggleRepeat() }) {
                            Icon(
                                imageVector = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeatMode > 0) SpotifyGreen else SpotifySubdued,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Lyrics Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenLyrics,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Lyrics",
                                tint = SpotifyGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lyrics",
                                color = White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            onClick = { PlaybackManager.switchSource() },
                            color = Color(0xFF282828),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Switch Source",
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = currentStreamingClient.title,
                                    color = SpotifyGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddToPlaylistSheet && currentTrack != null) {
        AddToPlaylistSheet(
            track = currentTrack!!,
            onDismiss = { showAddToPlaylistSheet = false }
        )
    }
}
