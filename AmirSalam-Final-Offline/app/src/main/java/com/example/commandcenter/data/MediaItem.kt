package com.example.commandcenter.data

import android.net.Uri

data class GalleryMediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    val dateAdded: Long
)
