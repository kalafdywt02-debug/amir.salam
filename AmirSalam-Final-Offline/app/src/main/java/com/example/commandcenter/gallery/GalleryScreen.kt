package com.example.commandcenter.gallery

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.commandcenter.data.GalleryMediaItem

@Composable
fun GalleryGrid(
    items: List<GalleryMediaItem>,
    viewModel: GalleryViewModel,
    // Hoisted so it can be created above wherever this grid gets removed
    // from composition (e.g. while the photo/video viewer is open) and
    // passed back in, instead of being recreated from scratch each time
    // the grid returns — which is what was resetting the scroll position.
    gridState: LazyGridState = rememberLazyGridState(),
    onOpen: (Uri, Boolean) -> Unit
) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("چیزی برای نمایش پیدا نشد", color = Color(0xFF9AA4B2))
            }
            return@Surface
        }
        LazyVerticalGrid(columns = GridCells.Fixed(3), state = gridState, modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.uri.toString() }) { item ->
                var thumb by remember(item.uri) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(item.uri) {
                    thumb = viewModel.thumbnailFor(context, item)
                }
                Box(
                    Modifier
                        .padding(2.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onOpen(item.uri, item.isVideo) }
                ) {
                    thumb?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            // Fit (not Crop): shows the whole thumbnail with
                            // no cropping. Since the thumbnail is now
                            // requested at the item's real aspect ratio,
                            // this mostly fills the square tile anyway.
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (item.isVideo) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "ویدیو",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp).padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
