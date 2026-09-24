package com.example.commandcenter.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Reads images and videos straight from MediaStore, newest first.
// No files are copied or uploaded; everything stays on-device.
class MediaRepository(private val context: Context) {

    suspend fun loadMedia(): List<GalleryMediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<GalleryMediaItem>()
        items += query(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            dateColumn = MediaStore.Images.Media.DATE_ADDED,
            isVideo = false
        )
        items += query(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            dateColumn = MediaStore.Video.Media.DATE_ADDED,
            isVideo = true
        )
        items.sortedByDescending { it.dateAdded }
    }

    private fun query(
        collection: android.net.Uri,
        dateColumn: String,
        isVideo: Boolean
    ): List<GalleryMediaItem> {
        val result = mutableListOf<GalleryMediaItem>()
        // WIDTH/HEIGHT let us ask the system for a thumbnail at the media's
        // own aspect ratio instead of a fixed square (see GalleryViewModel).
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            dateColumn,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )
        context.contentResolver.query(
            collection, projection, null, null, "$dateColumn DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateCol = cursor.getColumnIndexOrThrow(dateColumn)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val date = cursor.getLong(dateCol)
                // Some providers can report 0 for these; callers treat 0 as
                // "unknown" and fall back to a square thumbnail.
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val uri = ContentUris.withAppendedId(collection, id)
                result += GalleryMediaItem(uri, isVideo, date, width, height)
            }
        }
        return result
    }
}
