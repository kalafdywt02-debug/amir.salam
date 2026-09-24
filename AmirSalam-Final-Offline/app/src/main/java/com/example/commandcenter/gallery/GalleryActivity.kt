package com.example.commandcenter.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.commandcenter.data.LauncherPrefsRepository
import com.example.commandcenter.data.MediaRepository
import com.example.commandcenter.ui.AmirSalamTheme

// Internal-only gallery: not exported and not registered for
// ACTION_VIEW/"open with", so it can only be launched from inside
// this launcher's own home grid, never by other apps.
class GalleryActivity : ComponentActivity() {

    private lateinit var vm: GalleryViewModel
    private lateinit var prefsRepo: LauncherPrefsRepository

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) vm.loadGrid()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = GalleryViewModel(MediaRepository(applicationContext))
        prefsRepo = LauncherPrefsRepository(applicationContext)

        // First Back press closes the photo/video viewer and returns to the
        // grid; if already on the grid, default Back finishes this screen,
        // which (since it shares MainActivity's task) reveals the launcher's
        // own home screen rather than exiting to whatever was open before.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vm.viewer.value != null) {
                    vm.closeViewer()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            val appearance by prefsRepo.appearance.collectAsState(
                initial = com.example.commandcenter.data.LauncherAppearance()
            )
            AmirSalamTheme(
                background = Color(appearance.backgroundColor.toInt()),
                accent = Color(appearance.accentColor.toInt())
            ) {
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

@androidx.compose.runtime.Composable
private fun GalleryHost(vm: GalleryViewModel, requestLoad: () -> Unit) {
    val loading by vm.loading.collectAsState()
    val items by vm.items.collectAsState()
    val viewer by vm.viewer.collectAsState()
    LaunchedEffect(Unit) { requestLoad() }

    when {
        viewer != null -> {
            val v = viewer!!
            MediaViewerScreen(uri = v.uri, isVideo = v.isVideo, effect = v.effect)
        }
        loading -> Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        else -> GalleryGrid(items = items, viewModel = vm) { uri, isVideo ->
            vm.openViewer(uri, isVideo)
        }
    }
}
