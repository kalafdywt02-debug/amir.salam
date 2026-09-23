package com.example.commandcenter.gallery

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commandcenter.data.GalleryMediaItem
import com.example.commandcenter.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class GalleryScreenState {
    object Loading : GalleryScreenState()
    data class Grid(val items: List<GalleryMediaItem>) : GalleryScreenState()
    data class Viewer(val uri: Uri, val isVideo: Boolean) : GalleryScreenState()
}

class GalleryViewModel(private val repo: MediaRepository) : ViewModel() {

    private val _state = MutableStateFlow<GalleryScreenState>(GalleryScreenState.Loading)
    val state: StateFlow<GalleryScreenState> = _state

    // Small in-memory thumbnail cache so scrolling the grid doesn't re-decode images.
    private val thumbCache = LinkedHashMap<Uri, ImageBitmap>()

    fun loadGrid() {
        viewModelScope.launch {
            _state.value = GalleryScreenState.Loading
            val items = repo.loadMedia()
            _state.value = GalleryScreenState.Grid(items)
        }
    }

    fun openViewer(uri: Uri, isVideo: Boolean) {
        _state.value = GalleryScreenState.Viewer(uri, isVideo)
    }

    fun backToGrid() {
        val current = _state.value
        if (current is GalleryScreenState.Grid) return
        loadGrid()
    }

    suspend fun thumbnailFor(context: Context, uri: Uri): ImageBitmap? {
        thumbCache[uri]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val bmp: Bitmap = context.contentResolver.loadThumbnail(uri, Size(220, 220), null)
                bmp.asImageBitmap()
            }.getOrNull()?.also {
                if (thumbCache.size > 300) thumbCache.clear()
                thumbCache[uri] = it
            }
        }
    }
}
