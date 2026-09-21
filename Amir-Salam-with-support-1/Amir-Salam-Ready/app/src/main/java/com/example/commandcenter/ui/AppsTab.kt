package com.example.commandcenter.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.commandcenter.data.Rule
import com.example.commandcenter.domain.RuleEngine

data class InstalledApp(val label: String, val packageName: String)

private fun loadApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun drawableToBitmap(d: Drawable): Bitmap {
    val size = 192
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    d.setBounds(0, 0, size, size)
    d.draw(canvas)
    return bmp
}

private fun pinShortcut(context: Context, app: InstalledApp, newName: String) {
    val sm = context.getSystemService(ShortcutManager::class.java)
    if (sm == null || !sm.isRequestPinShortcutSupported) {
        Toast.makeText(context, "لانچر گوشی ساخت میانبر را پشتیبانی نمی‌کند", Toast.LENGTH_LONG).show()
        return
    }
    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
    if (launchIntent == null) {
        Toast.makeText(context, "این برنامه قابل اجرا نیست", Toast.LENGTH_LONG).show()
        return
    }
    try {
        val icon = Icon.createWithBitmap(drawableToBitmap(context.packageManager.getApplicationIcon(app.packageName)))
        val info = ShortcutInfo.Builder(context, "rename_${app.packageName}_${System.currentTimeMillis()}")
            .setShortLabel(newName)
            .setLongLabel(newName)
            .setIcon(icon)
            .setIntent(launchIntent)
            .build()
        sm.requestPinShortcut(info, null)
    } catch (e: Exception) {
        Toast.makeText(context, "ساخت میانبر انجام نشد", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun AppsTab(rules: List<Rule>) {
    val context = LocalContext.current
    val apps = remember { loadApps(context) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<InstalledApp?>(null) }
    val shown = apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("تغییر اسم برنامه‌ها", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "برنامه را انتخاب کن و اسم جدید بده. یک میانبر با آن اسم به صفحه اصلی اضافه می‌شود. برنامه‌ی اصلی می‌ماند و اگر خواستی آیکون قبلی را خودت از صفحه اصلی مخفی کن.",
            color = Color(0xFF9AA4B2)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            label = { Text("جست‌وجوی برنامه") }
        )
        LazyColumn {
            items(shown, key = { it.packageName }) { app ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151922))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(app.label, color = Color.White)
                        TextButton(onClick = { selected = app }) { Text("تغییر اسم") }
                    }
                }
            }
        }
    }

    selected?.let { app ->
        RenameDialog(
            app = app,
            suggestion = RuleEngine.replace(app.label, rules),
            onDismiss = { selected = null },
            onConfirm = { name ->
                pinShortcut(context, app, name)
                selected = null
            }
        )
    }
}

@Composable
private fun RenameDialog(
    app: InstalledApp,
    suggestion: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(suggestion) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اسم جدید برای " + app.label) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم جدید") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("ساخت میانبر") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } }
    )
}
