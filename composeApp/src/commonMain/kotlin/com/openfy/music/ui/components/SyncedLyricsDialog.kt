package com.openfy.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.openfy.music.data.model.LyricsData
import com.openfy.music.data.remote.LyricsClient
import com.openfy.music.player.PlaybackManager
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White

@Composable
fun SyncedLyricsDialog(
    onDismiss: () -> Unit
) {
    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val positionMs by PlaybackManager.currentPositionMs.collectAsState()
    val currentPositionSec = positionMs / 1000f

    var lyricsData by remember { mutableStateOf<LyricsData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(currentTrack?.id) {
        val track = currentTrack
        if (track != null) {
            isLoading = true
            lyricsData = LyricsClient.getLyrics(
                trackTitle = track.title,
                artistName = track.artistNames,
                durationSeconds = track.durationSeconds,
                videoId = track.youtubeId ?: track.id
            )
            isLoading = false
        }
    }

    // Active line index
    val activeIndex = remember(currentPositionSec, lyricsData) {
        val lines = lyricsData?.lines ?: emptyList()
        lines.indexOfLast { currentPositionSec >= it.timeSeconds }
    }

    // Autoscroll to active line
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
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
                        listOf(Color(0xFF0F3A30), Color(0xFF081814), Black)
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentTrack?.title ?: "Lyrics",
                                color = White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentTrack?.artistNames ?: "",
                                color = SpotifySubdued,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = White
                        )
                    }
                }

                // Lyrics Content
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                } else if (lyricsData?.isSynced == true && lyricsData?.lines?.isNotEmpty() == true) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(vertical = 40.dp)
                    ) {
                        itemsIndexed(lyricsData!!.lines) { idx, line ->
                            val isActive = idx == activeIndex
                            Text(
                                text = line.text,
                                color = if (isActive) White else White.copy(alpha = 0.35f),
                                fontSize = if (isActive) 26.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        PlaybackManager.seekTo((line.timeSeconds * 1000).toLong())
                                    }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lyricsData?.plainLyrics ?: "Lyrics unavailable for this track.",
                            color = SpotifySubdued,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}
