package com.openfy.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openfy.music.data.AccountManager
import com.openfy.music.data.model.Playlist
import com.openfy.music.data.model.Track
import com.openfy.music.data.service.PlaylistSyncEntry
import com.openfy.music.data.service.PlaylistSyncReport
import com.openfy.music.data.service.ServiceProvider
import com.openfy.music.data.service.ServiceResult
import com.openfy.music.ui.theme.SpotifyDark
import com.openfy.music.ui.theme.SpotifyGreen
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White
import kotlinx.coroutines.launch

private val YoutubeRed = Color(0xFFFF0000)

private sealed class AddSheetState {
    data object Loading : AddSheetState()
    data class Selecting(
        val spotifyPlaylists: List<Playlist>,
        val youtubePlaylists: List<Playlist>,
        val spotifySelected: Set<String> = emptySet(),
        val youtubeSelected: Set<String> = emptySet(),
        val spotifyError: String? = null,
        val youtubeError: String? = null
    ) : AddSheetState() {
        val totalSelected: Int get() = spotifySelected.size + youtubeSelected.size
    }

    data object Saving : AddSheetState()
    data class Result(
        val report: PlaylistSyncReport,
        val isBusy: Boolean = false
    ) : AddSheetState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val coordinator = remember { AccountManager.syncCoordinator }
    val activeAccount by AccountManager.activeAccount.collectAsState()
    val spotifyEmail by AccountManager.spotifyEmail.collectAsState()
    val youtubeEmail by AccountManager.youtubeEmail.collectAsState()
    val connectedProviders = remember(activeAccount, spotifyEmail, youtubeEmail) {
        coordinator.connectedProviders
    }

    var sheetState by remember { mutableStateOf<AddSheetState>(AddSheetState.Loading) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var createOnSpotify by remember(connectedProviders) { mutableStateOf(ServiceProvider.SPOTIFY in connectedProviders) }
    var createOnYoutube by remember(connectedProviders) { mutableStateOf(ServiceProvider.YOUTUBE_MUSIC in connectedProviders) }

    suspend fun loadAllPlaylists() {
        sheetState = AddSheetState.Loading
        val dual = coordinator.getPlaylists()
        val spList = (dual.spotify as? ServiceResult.Success)?.value.orEmpty()
        val ytList = (dual.youtube as? ServiceResult.Success)?.value.orEmpty()
        val spErr = (dual.spotify as? ServiceResult.Failure)?.message
        val ytErr = (dual.youtube as? ServiceResult.Failure)?.message
        sheetState = AddSheetState.Selecting(
            spotifyPlaylists = spList,
            youtubePlaylists = ytList,
            spotifyError = spErr,
            youtubeError = ytErr
        )
    }

    LaunchedEffect(Unit) {
        loadAllPlaylists()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SpotifyDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF555555))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add to playlist",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${track.title} • ${track.artistNames}",
                        color = SpotifySubdued,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (val current = sheetState) {
                AddSheetState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(32.dp))
                    }
                }

                AddSheetState.Saving -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Syncing across connected services...", color = SpotifySubdued, fontSize = 13.sp)
                    }
                }

                is AddSheetState.Selecting -> {
                    if (connectedProviders.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign in to Spotify or YouTube Music to save tracks to playlists.",
                                color = SpotifySubdued,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // + New Playlist button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E))
                                .clickable { showCreateDialog = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF282828)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Playlist",
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("New playlist", color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Create on ${connectedProviders.joinToString(" / ") { it.displayName }}",
                                    color = SpotifySubdued,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Spotify section
                            if (ServiceProvider.SPOTIFY in connectedProviders) {
                                item(key = "header_spotify") {
                                    ServiceSectionHeader(label = "Spotify", badgeColor = SpotifyGreen)
                                }
                                if (current.spotifyError != null) {
                                    item(key = "err_spotify") {
                                        Text(
                                            text = "Could not load Spotify playlists: ${current.spotifyError}",
                                            color = Color(0xFFFF6B6B),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                } else if (current.spotifyPlaylists.isEmpty()) {
                                    item(key = "empty_spotify") {
                                        Text(
                                            text = "No Spotify playlists found",
                                            color = SpotifySubdued,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    items(current.spotifyPlaylists, key = { "sp_${it.id}" }) { playlist ->
                                        val isChecked = playlist.id in current.spotifySelected
                                        PlaylistCheckboxRow(
                                            playlist = playlist,
                                            checked = isChecked,
                                            accentColor = SpotifyGreen,
                                            onToggle = {
                                                val updated = if (isChecked) {
                                                    current.spotifySelected - playlist.id
                                                } else {
                                                    current.spotifySelected + playlist.id
                                                }
                                                sheetState = current.copy(spotifySelected = updated)
                                            }
                                        )
                                    }
                                }
                            }

                            // YouTube Music section
                            if (ServiceProvider.YOUTUBE_MUSIC in connectedProviders) {
                                item(key = "header_youtube") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ServiceSectionHeader(label = "YouTube Music", badgeColor = YoutubeRed)
                                }
                                if (current.youtubeError != null) {
                                    item(key = "err_youtube") {
                                        Text(
                                            text = "Could not load YouTube playlists: ${current.youtubeError}",
                                            color = Color(0xFFFF6B6B),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                } else if (current.youtubePlaylists.isEmpty()) {
                                    item(key = "empty_youtube") {
                                        Text(
                                            text = "No YouTube Music playlists found",
                                            color = SpotifySubdued,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    items(current.youtubePlaylists, key = { "yt_${it.id}" }) { playlist ->
                                        val isChecked = playlist.id in current.youtubeSelected
                                        PlaylistCheckboxRow(
                                            playlist = playlist,
                                            checked = isChecked,
                                            accentColor = YoutubeRed,
                                            onToggle = {
                                                val updated = if (isChecked) {
                                                    current.youtubeSelected - playlist.id
                                                } else {
                                                    current.youtubeSelected + playlist.id
                                                }
                                                sheetState = current.copy(youtubeSelected = updated)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val spToSave = current.spotifyPlaylists.filter { it.id in current.spotifySelected }
                                val ytToSave = current.youtubePlaylists.filter { it.id in current.youtubeSelected }
                                sheetState = AddSheetState.Saving
                                scope.launch {
                                    val report = coordinator.addToPlaylists(track, spToSave, ytToSave)
                                    AccountManager.refreshLibrary()
                                    sheetState = AddSheetState.Result(report)
                                }
                            },
                            enabled = current.totalSelected > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreen,
                                disabledContainerColor = Color(0xFF2E2E2E)
                            ),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = if (current.totalSelected > 0) {
                                    "Save to ${current.totalSelected} playlist${if (current.totalSelected == 1) "" else "s"}"
                                } else {
                                    "Select playlists to save"
                                },
                                color = if (current.totalSelected > 0) Color.Black else SpotifySubdued,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                is AddSheetState.Result -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(current.report.entries, key = { "${it.provider}_${it.playlist.id}" }) { entry ->
                            SyncResultEntryRow(entry)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (current.report.anyFailed && !current.isBusy) {
                            OutlinedButton(
                                onClick = {
                                    sheetState = current.copy(isBusy = true)
                                    scope.launch {
                                        val updated = coordinator.retryFailed(track, current.report)
                                        AccountManager.refreshLibrary()
                                        sheetState = AddSheetState.Result(updated, isBusy = false)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = White)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Failed", fontSize = 13.sp)
                            }
                        }

                        if (current.report.anySucceeded && current.report.anyFailed && !current.isBusy) {
                            OutlinedButton(
                                onClick = {
                                    sheetState = current.copy(isBusy = true)
                                    scope.launch {
                                        val rolled = coordinator.rollbackSucceeded(track, current.report)
                                        AccountManager.refreshLibrary()
                                        AccountManager.emitToast("Rolled back successful playlist additions")
                                        sheetState = AddSheetState.Result(rolled, isBusy = false)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80))
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rollback", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            enabled = !current.isBusy,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF242424),
            title = {
                Text(text = "Create new playlist", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("My playlist #1", color = Color.Gray, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = Color(0xFF555555)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (ServiceProvider.SPOTIFY in connectedProviders) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { createOnSpotify = !createOnSpotify }
                        ) {
                            Checkbox(
                                checked = createOnSpotify,
                                onCheckedChange = { createOnSpotify = it },
                                colors = CheckboxDefaults.colors(checkedColor = SpotifyGreen)
                            )
                            Text("Create on Spotify", color = White, fontSize = 13.sp)
                        }
                    }
                    if (ServiceProvider.YOUTUBE_MUSIC in connectedProviders) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { createOnYoutube = !createOnYoutube }
                        ) {
                            Checkbox(
                                checked = createOnYoutube,
                                onCheckedChange = { createOnYoutube = it },
                                colors = CheckboxDefaults.colors(checkedColor = YoutubeRed)
                            )
                            Text("Create on YouTube Music", color = White, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPlaylistName.trim().ifEmpty { "My Playlist" }
                        val targets = buildSet {
                            if (createOnSpotify) add(ServiceProvider.SPOTIFY)
                            if (createOnYoutube) add(ServiceProvider.YOUTUBE_MUSIC)
                        }
                        if (targets.isEmpty()) return@Button
                        showCreateDialog = false
                        newPlaylistName = ""
                        sheetState = AddSheetState.Saving
                        scope.launch {
                            val createdDual = coordinator.createPlaylist(name, targets)
                            val spList = listOfNotNull(createdDual.spotify?.getOrNull())
                            val ytList = listOfNotNull(createdDual.youtube?.getOrNull())
                            val addReport = if (spList.isNotEmpty() || ytList.isNotEmpty()) {
                                coordinator.addToPlaylists(track, spList, ytList)
                            } else {
                                PlaylistSyncReport(emptyList())
                            }

                            val creationFailures = buildList {
                                if (ServiceProvider.SPOTIFY in targets && createdDual.spotify is ServiceResult.Failure) {
                                    add(
                                        PlaylistSyncEntry(
                                            provider = ServiceProvider.SPOTIFY,
                                            playlist = Playlist(id = "create_failed_sp", title = name, source = "spotify"),
                                            result = ServiceResult.Failure(
                                                message = "Create playlist failed: ${createdDual.spotify.message}",
                                                isRetryable = false
                                            )
                                        )
                                    )
                                }
                                if (ServiceProvider.YOUTUBE_MUSIC in targets && createdDual.youtube is ServiceResult.Failure) {
                                    add(
                                        PlaylistSyncEntry(
                                            provider = ServiceProvider.YOUTUBE_MUSIC,
                                            playlist = Playlist(id = "create_failed_yt", title = name, source = "youtube"),
                                            result = ServiceResult.Failure(
                                                message = "Create playlist failed: ${createdDual.youtube.message}",
                                                isRetryable = false
                                            )
                                        )
                                    )
                                }
                            }

                            AccountManager.refreshLibrary()
                            sheetState = AddSheetState.Result(
                                PlaylistSyncReport(addReport.entries + creationFailures)
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text("Create & Add", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = White)
                }
            }
        )
    }
}

@Composable
private fun ServiceSectionHeader(label: String, badgeColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            color = White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun PlaylistCheckboxRow(
    playlist: Playlist,
    checked: Boolean,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (checked) Color(0xFF282828) else Color.Transparent)
            .clickable { onToggle() }
            .padding(vertical = 6.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OpenfyAsyncImage(
            url = playlist.coverUrl,
            contentDescription = playlist.title,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
            placeholder = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF282828)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = SpotifySubdued,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (playlist.trackCount > 0) {
                Text(
                    text = "${playlist.trackCount} songs",
                    color = SpotifySubdued,
                    fontSize = 11.sp
                )
            }
        }

        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                uncheckedColor = SpotifySubdued,
                checkmarkColor = Color.Black
            )
        )
    }
}

@Composable
private fun SyncResultEntryRow(entry: PlaylistSyncEntry) {
    val isSuccess = entry.result is ServiceResult.Success
    val failureMsg = (entry.result as? ServiceResult.Failure)?.message
    val serviceColor = if (entry.provider == ServiceProvider.SPOTIFY) SpotifyGreen else YoutubeRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(serviceColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${entry.provider.displayName} • ${entry.playlist.title}",
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isSuccess) "Added" else (failureMsg ?: "Failed"),
                color = if (isSuccess) SpotifyGreen else Color(0xFFFF6B6B),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isSuccess) SpotifyGreen else Color(0xFFFF6B6B),
            modifier = Modifier.size(20.dp)
        )
    }
}
