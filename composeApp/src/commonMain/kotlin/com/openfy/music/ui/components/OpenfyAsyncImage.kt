package com.openfy.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.openfy.music.util.ArtworkUtils

@Composable
fun OpenfyAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholder: (@Composable () -> Unit)? = null
) {
    var isError by remember(url) { mutableStateOf(false) }

    if ((url.isNullOrEmpty() || isError) && placeholder != null) {
        Box(modifier = modifier, contentAlignment = alignment) {
            placeholder()
        }
        return
    }

    if (url.isNullOrEmpty()) {
        Box(modifier = modifier)
        return
    }

    val highResUrl = remember(url) { ArtworkUtils.getHighResArtworkUrl(url) }
    val fallbackUrl = remember(url) { ArtworkUtils.getFallbackArtworkUrl(url) }
    var currentUrl by remember(url) { mutableStateOf(highResUrl) }
    val context = LocalPlatformContext.current

    val imageRequest = remember(currentUrl) {
        ImageRequest.Builder(context)
            .data(currentUrl)
            .crossfade(true)
            .crossfade(350)
            .build()
    }

    Box(modifier = modifier, contentAlignment = alignment) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = contentScale,
            alignment = alignment,
            modifier = Modifier.matchParentSize(),
            onError = {
                if (fallbackUrl != null && currentUrl != fallbackUrl) {
                    currentUrl = fallbackUrl
                } else {
                    isError = true
                }
            }
        )
    }
}
