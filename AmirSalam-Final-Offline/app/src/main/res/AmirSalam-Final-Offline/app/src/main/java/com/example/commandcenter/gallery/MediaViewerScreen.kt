package com.example.commandcenter.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// A quick camera-shutter style flash + scale-in that plays once, right
// before the actual photo or video is revealed.
@Composable
private fun OpeningEffect(content: @Composable () -> Unit) {
    val scale = remember { Animatable(0.85f) }
    val flash = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(260))
    }
    LaunchedEffect(Unit) {
        flash.animateTo(0f, animationSpec = tween(380))
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().scale(scale.value)) { content() }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = flash.value))
        )
    }
}

@Composable
fun MediaViewerScreen(uri: Uri, isVideo: Boolean) {
    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        OpeningEffect {
            if (isVideo) VideoPlayer(uri) else PhotoView(uri)
        }
    }
}

@Composable
private fun PhotoView(uri: Uri) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = decodeSampledBitmap(context, uri, 1600)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
        } ?: CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun VideoPlayer(uri: Uri) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(uri)
                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setOnPreparedListener { it.isLooping = false; start() }
            }
        })
    }
}

private suspend fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp: Bitmap? = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            bmp?.asImageBitmap()
        }.getOrNull()
    }
