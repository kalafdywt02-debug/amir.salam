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
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaViewerScreen(uri: Uri, isVideo: Boolean, effect: Int) {
    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        OpeningEffect(effect) {
            if (isVideo) VideoPlayer(uri) else PhotoView(uri)
        }
    }
}

// Five distinct opening transitions, picked in rotation by GalleryViewModel
// so consecutive photos never play the same one.
@Composable
private fun OpeningEffect(effect: Int, content: @Composable () -> Unit) {
    when (effect % OPENING_EFFECT_COUNT) {
        0 -> FlashScaleEffect(content)
        1 -> FadeEffect(content)
        2 -> ZoomOutEffect(content)
        3 -> SlideUpEffect(content)
        else -> RotateFadeEffect(content)
    }
}

// 1) Camera-shutter style: white flash fading out while the photo scales in.
@Composable
private fun FlashScaleEffect(content: @Composable () -> Unit) {
    val scale = remember { Animatable(0.85f) }
    val flash = remember { Animatable(1f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, animationSpec = tween(260)) }
    LaunchedEffect(Unit) { flash.animateTo(0f, animationSpec = tween(380)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().scale(scale.value)) { content() }
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
    }
}

// 2) Plain, calm fade-in.
@Composable
private fun FadeEffect(content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, animationSpec = tween(400)) }
    Box(Modifier.fillMaxSize().alpha(alpha.value)) { content() }
}

// 3) Starts slightly zoomed in and settles down to full size.
@Composable
private fun ZoomOutEffect(content: @Composable () -> Unit) {
    val scale = remember { Animatable(1.25f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, animationSpec = tween(360)) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, animationSpec = tween(280)) }
    Box(Modifier.fillMaxSize().scale(scale.value).alpha(alpha.value)) { content() }
}

// 4) Slides up from below the screen while fading in.
@Composable
private fun SlideUpEffect(content: @Composable () -> Unit) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(Unit) { progress.animateTo(0f, animationSpec = tween(360)) }
    val density = LocalDensity.current
    val offsetPx = with(density) { 80.dp.toPx() } * progress.value
    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetPx.toInt()) }
            .alpha(1f - progress.value)
    ) { content() }
}

// 5) Small rotation + scale settling into place, like a photo being flipped in.
@Composable
private fun RotateFadeEffect(content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, animationSpec = tween(360)) }
    val rotation = (1f - progress.value) * -8f
    val scale = 0.9f + 0.1f * progress.value
    Box(
        Modifier
            .fillMaxSize()
            .rotate(rotation)
            .scale(scale)
            .alpha(progress.value)
    ) { content() }
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
