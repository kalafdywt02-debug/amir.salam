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

// Keep in sync with the `when` in MediaViewerScreen's effect picker.
const val OPENING_EFFECT_COUNT = 5

data class GalleryViewerState(val uri: Uri, val isVideo: Boolean, val effect: Int)

class GalleryViewModel(private val repo: MediaRepository) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _items = MutableStateFlow<List<GalleryMediaItem>>(emptyList())
    val items: StateFlow<List<GalleryMediaItem>> = _items

    private val _viewer = MutableStateFlow<GalleryViewerState?>(null)
    val viewer: StateFlow<GalleryViewerState?> = _viewer

    // Cycles through the 5 opening effects in order, so two photos opened
    // back-to-back never play the same one, and every effect gets used.
    private var nextEffect = 0

    fun loadGrid() {
        viewModelScope.launch {
            _loading.value = true
            _items.value = repo.loadMedia()
            _loading.value = false
        }
    }

    fun openViewer(uri: Uri, isVideo: Boolean) {
        val effect = nextEffect
        nextEffect = (nextEffect + 1) % OPENING_EFFECT_COUNT
        _viewer.value = GalleryViewerState(uri, isVideo, effect)
    }

    fun closeViewer() {
        _viewer.value = null
    }

    // Small in-memory thumbnail cache so scrolling the grid doesn't re-decode images.
    private val thumbCache = LinkedHashMap<Uri, ImageBitmap>()

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
