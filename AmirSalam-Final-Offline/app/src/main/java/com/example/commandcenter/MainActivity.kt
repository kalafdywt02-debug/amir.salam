package com.example.commandcenter

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.commandcenter.data.AppRepository
import com.example.commandcenter.data.InstalledAppInfo
import com.example.commandcenter.data.LauncherPrefsRepository
import com.example.commandcenter.data.RuleRepository
import com.example.commandcenter.ui.AmirSalamTheme
import com.example.commandcenter.ui.AmirSalamViewModel
import com.example.commandcenter.ui.MainScreen

class MainActivity : ComponentActivity() {

    private lateinit var vm: AmirSalamViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = AmirSalamViewModel(
            ruleRepo = RuleRepository(applicationContext),
            prefsRepo = LauncherPrefsRepository(applicationContext),
            appRepo = AppRepository(applicationContext)
        )

        // Back should never exit a launcher (there's nowhere "out" to go);
        // it just returns to the home tab if the user was in Settings.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vm.currentTab.value != 0) vm.setTab(0)
            }
        })

        setContent {
            val appearance by vm.appearance.collectAsState()
            AmirSalamTheme(
                background = Color(appearance.backgroundColor.toInt()),
                accent = Color(appearance.accentColor.toInt())
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainScreen(
                        viewModel = vm,
                        launchApp = { app -> openApp(app) }
                    )
                }
            }
        }
    }

    private fun openApp(app: InstalledAppInfo) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                // Our own screens (e.g. Gallery) stay in this same task so
                // the system Back button naturally returns to this home
                // screen. Only third-party apps need their own task, which
                // is the normal way a launcher opens other apps.
                if (app.packageName != packageName) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "این برنامه باز نشد", Toast.LENGTH_SHORT).show()
        }
    }
}
