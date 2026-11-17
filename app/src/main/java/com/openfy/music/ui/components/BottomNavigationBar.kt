package com.openfy.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openfy.music.ui.theme.Black
import com.openfy.music.ui.theme.SpotifySubdued
import com.openfy.music.ui.theme.White

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY
}

@Composable
fun BottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(60.dp),
        containerColor = Black,
        contentColor = White
    ) {
        // Home
        NavigationBarItem(
            selected = currentTab == NavigationTab.HOME,
            onClick = { onTabSelected(NavigationTab.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = White,
                selectedTextColor = White,
                unselectedIconColor = SpotifySubdued,
                unselectedTextColor = SpotifySubdued,
                indicatorColor = Color.Transparent
            )
        )

        // Search
        NavigationBarItem(
            selected = currentTab == NavigationTab.SEARCH,
            onClick = { onTabSelected(NavigationTab.SEARCH) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Search", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = White,
                selectedTextColor = White,
                unselectedIconColor = SpotifySubdued,
                unselectedTextColor = SpotifySubdued,
                indicatorColor = Color.Transparent
            )
        )

        // Your Library
        NavigationBarItem(
            selected = currentTab == NavigationTab.LIBRARY,
            onClick = { onTabSelected(NavigationTab.LIBRARY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = "Your Library",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Your Library", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = White,
                selectedTextColor = White,
                unselectedIconColor = SpotifySubdued,
                unselectedTextColor = SpotifySubdued,
                indicatorColor = Color.Transparent
            )
        )
    }
}
