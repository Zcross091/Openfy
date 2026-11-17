package com.openfy.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.Track
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun TrackRowItem(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Index
        Text(
            text = (index + 1).toString(),
            color = if (isCurrent) SpotifyGreen else SpotifySubdued,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(28.dp)
        )

        // Thumbnail
        OpenfyAsyncImage(
            url = track.coverUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(4.dp)),
            placeholder = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = SpotifySubdued
                    )
                }
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artists
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) SpotifyGreen else White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artistNames,
                color = SpotifySubdued,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Like Heart Button
        IconButton(
            onClick = {
                scope.launch {
                    AccountManager.toggleLike(track)
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (track.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Save to library",
                tint = if (track.isLiked) SpotifyGreen else SpotifySubdued,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
