package com.openfy.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.openfy.music.data.AccountManager
import com.openfy.music.ui.components.AccountSwitcherDialog
import com.openfy.music.ui.components.BottomMiniPlayer
import com.openfy.music.ui.components.BottomNavigationBar
import com.openfy.music.ui.components.NavigationTab
import com.openfy.music.ui.components.NowPlayingSheet
import com.openfy.music.ui.components.SyncedLyricsDialog
import com.openfy.music.ui.screens.HomeScreen
import com.openfy.music.ui.screens.LikedSongsScreen
import com.openfy.music.ui.screens.SearchScreen
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.OpenfyTheme

@Composable
fun OpenfyApp() {
    OpenfyTheme {
        var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
        var isNowPlayingExpanded by remember { mutableStateOf(false) }
        var isLyricsOpen by remember { mutableStateOf(false) }
        var isAccountSwitcherOpen by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }

        // Listen for save / like toast notifications
        LaunchedEffect(Unit) {
            AccountManager.toastEvent.collect { message ->
                snackbarHostState.showSnackbar(message)
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

                    // Spotify Bottom Navigation Bar
                    BottomNavigationBar(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            if (tab == NavigationTab.ACCOUNT) {
                                isAccountSwitcherOpen = true
                            } else {
                                currentTab = tab
                            }
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
                            onNavigateToLiked = { currentTab = NavigationTab.LIBRARY }
                        )
                    }
                    NavigationTab.SEARCH -> {
                        SearchScreen()
                    }
                    NavigationTab.LIBRARY -> {
                        LikedSongsScreen()
                    }
                    NavigationTab.ACCOUNT -> {
                        // Fallback view if opened directly
                        HomeScreen(
                            onNavigateToLiked = { currentTab = NavigationTab.LIBRARY }
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

            // Account Switcher Dialog
            if (isAccountSwitcherOpen) {
                AccountSwitcherDialog(
                    onDismiss = { isAccountSwitcherOpen = false }
                )
            }
        }
    }
}
