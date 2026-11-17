package com.openfy.music

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.openfy.music.data.AccountManager
import com.openfy.music.ui.components.AuthWebViewDialog
import com.openfy.music.ui.components.BottomMiniPlayer
import com.openfy.music.ui.components.BottomNavigationBar
import com.openfy.music.ui.components.NavigationTab
import com.openfy.music.ui.components.NowPlayingSheet
import com.openfy.music.ui.components.SyncedLyricsDialog
import com.openfy.music.ui.screens.HomeScreen
import com.openfy.music.ui.screens.LikedSongsScreen
import com.openfy.music.ui.screens.SearchScreen
import com.openfy.music.ui.screens.SettingsScreen
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.OpenfyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpenfyTheme {
                var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
                var isNowPlayingExpanded by remember { mutableStateOf(false) }
                var isLyricsOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isSpotifyAuthOpen by remember { mutableStateOf(false) }

                val snackbarHostState = remember { SnackbarHostState() }

                // Listen for save / like toast notifications
                LaunchedEffect(Unit) {
                    AccountManager.toastEvent.collect { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Black,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        Column {
                            // Persistent Floating Mini-Player
                            BottomMiniPlayer(
                                onExpand = { isNowPlayingExpanded = true }
                            )

                            // Spotify Bottom Navigation Bar (3-tab: Home, Search, Your Library)
                            BottomNavigationBar(
                                currentTab = currentTab,
                                onTabSelected = { tab ->
                                    currentTab = tab
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            NavigationTab.HOME -> {
                                HomeScreen(
                                    onNavigateToLiked = { currentTab = NavigationTab.LIBRARY },
                                    onOpenSettings = { isSettingsOpen = true },
                                    onOpenSpotifyAuth = { isSpotifyAuthOpen = true }
                                )
                            }
                            NavigationTab.SEARCH -> {
                                SearchScreen()
                            }
                            NavigationTab.LIBRARY -> {
                                LikedSongsScreen(
                                    onOpenSettings = { isSettingsOpen = true },
                                    onOpenSpotifyAuth = { isSpotifyAuthOpen = true }
                                )
                            }
                        }
                    }

                    // Fullscreen Now Playing Modal
                    if (isNowPlayingExpanded) {
                        NowPlayingSheet(
                            onDismiss = { isNowPlayingExpanded = false },
                            onOpenLyrics = {
                                isNowPlayingExpanded = false
                                isLyricsOpen = true
                            }
                        )
                    }

                    // Fullscreen Synced Lyrics Modal
                    if (isLyricsOpen) {
                        SyncedLyricsDialog(
                            onDismiss = { isLyricsOpen = false }
                        )
                    }

                    // Spotify Sign-In / Connect Modal
                    if (isSpotifyAuthOpen) {
                        AuthWebViewDialog(
                            onDismiss = { isSpotifyAuthOpen = false },
                            onSuccess = { msg ->
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                isSpotifyAuthOpen = false
                            }
                        )
                    }

                    // Fullscreen Settings Modal
                    if (isSettingsOpen) {
                        SettingsScreen(
                            onDismiss = { isSettingsOpen = false }
                        )
                    }
                }
            }
        }
    }
}
