package com.example.commandcenter.data

import android.net.Uri

data class GalleryMediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    val dateAdded: Long,
    // Original dimensions from MediaStore. Used to request a thumbnail at
    // the same aspect ratio as the source, instead of a fixed square (which
    // is what was causing the visible crop on every grid tile). 0 means
    // unknown/unavailable, and callers should fall back to a square.
    val width: Int = 0,
    val height: Int = 0
)
