package com.example.commandcenter.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.commandcenter.data.MediaRepository
import com.example.commandcenter.ui.AmirSalamTheme

// Internal-only gallery: not exported and not registered for
// ACTION_VIEW/"open with", so it can only be launched from inside
// this launcher's own home grid, never by other apps.
class GalleryActivity : ComponentActivity() {

    private lateinit var vm: GalleryViewModel

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) vm.loadGrid()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = GalleryViewModel(MediaRepository(applicationContext))

        setContent {
            AmirSalamTheme {
                GalleryHost(vm, ::ensurePermissionThenLoad)
            }
        }
    }

    private fun ensurePermissionThenLoad() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            needed += Manifest.permission.READ_MEDIA_IMAGES
            needed += Manifest.permission.READ_MEDIA_VIDEO
        } else {
            needed += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) vm.loadGrid() else requestPermissions.launch(missing.toTypedArray())
    }
}

@Composable
private fun GalleryHost(vm: GalleryViewModel, requestLoad: () -> Unit) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { requestLoad() }

    var viewerUri by remember { mutableStateOf<Uri?>(null) }
    var viewerIsVideo by remember { mutableStateOf(false) }

    when (val s = state) {
        is GalleryScreenState.Loading -> Surface(color = Color(0xFF0B0D12), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF8DE1BE))
            }
        }
        is GalleryScreenState.Grid -> {
            if (viewerUri == null) {
                GalleryGrid(items = s.items, viewModel = vm) { uri, isVideo ->
                    viewerUri = uri
                    viewerIsVideo = isVideo
                }
            } else {
                MediaViewerScreen(uri = viewerUri!!, isVideo = viewerIsVideo)
            }
        }
        is GalleryScreenState.Viewer -> MediaViewerScreen(uri = s.uri, isVideo = s.isVideo)
    }
}
