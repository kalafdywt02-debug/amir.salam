package com.example.commandcenter.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Reads the list of launchable apps. Icons are rasterized off the main
// thread since PackageManager and drawable decoding can be slow.
//
// PERFORMANCE FIX:
// Previously this scan (PackageManager query + icon rasterization for
// every installed app) ran again every single time the Home tab loaded
// (e.g. whenever the launcher activity was recreated), causing visible
// lag on devices with many apps installed.
//
// Now the result is cached in-memory (per process) in a companion
// object. The first call still does the full scan; every call after
// that returns instantly from cache unless a refresh is explicitly
// requested (forceRefresh = true) — e.g. after the user installs or
// removes an app.
class AppRepository(private val context: Context) {

    companion object {
        // Shared across all AppRepository instances in this process.
        @Volatile
        private var cachedApps: List<InstalledAppInfo>? = null

        // Smaller than the old 128px target: still crisp for a 56dp grid
        // icon, but noticeably faster to rasterize for large app lists.
        private const val ICON_PX = 96
    }

    suspend fun loadApps(forceRefresh: Boolean = false): List<InstalledAppInfo> {
        cachedApps?.let { if (!forceRefresh) return it }
        return withContext(Dispatchers.Default) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .distinctBy { it.activityInfo.packageName }
                .map { resolveInfo ->
                    val activityInfo = resolveInfo.activityInfo
                    InstalledAppInfo(
                        label = resolveInfo.loadLabel(pm).toString(),
                        packageName = activityInfo.packageName,
                        activityName = activityInfo.name,
                        icon = drawableToBitmap(resolveInfo.loadIcon(pm)).asImageBitmap()
                    )
                }
                .sortedBy { it.label.lowercase() }
            cachedApps = apps
            apps
        }
    }

    // Call this if you add a "refresh app list" button, or after
    // detecting an app was installed/uninstalled (e.g. via a
    // PACKAGE_ADDED / PACKAGE_REMOVED broadcast receiver).
    fun invalidateCache() {
        cachedApps = null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, ICON_PX, ICON_PX)
        drawable.draw(canvas)
        return bitmap
    }
}
