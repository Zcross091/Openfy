package com.openfy.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.openfy.music.data.AccountManager
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.theme.SpotifyDark
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun BottomMiniPlayer(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val isLoading by PlaybackManager.isLoading.collectAsState()
    val positionMs by PlaybackManager.currentPositionMs.collectAsState()
    val durationMs by PlaybackManager.durationMs.collectAsState()

    val scope = rememberCoroutineScope()

    if (currentTrack == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SpotifyDark)
            .clickable(onClick = onExpand)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork
                if (!currentTrack?.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = currentTrack?.coverUrl,
                        contentDescription = currentTrack?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF282828)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = SpotifySubdued
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Track Title & Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentTrack?.title ?: "",
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack?.artistNames ?: "",
                        color = SpotifySubdued,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Heart Button
                IconButton(
                    onClick = {
                        currentTrack?.let {
                            scope.launch { AccountManager.toggleLike(it) }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (currentTrack?.isLiked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save to active library",
                        tint = if (currentTrack?.isLiked == true) SpotifyGreen else SpotifySubdued,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause Button
                IconButton(
                    onClick = { PlaybackManager.togglePlay() },
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = SpotifyGreen,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Bottom scrub progress bar
            val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF333333))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .background(SpotifyGreen)
                )
            }
        }
    }
}
