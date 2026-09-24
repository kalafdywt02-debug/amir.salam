package com.example.commandcenter.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.commandcenter.data.InstalledAppInfo
import com.example.commandcenter.data.Rule

@Composable
fun MainScreen(viewModel: AmirSalamViewModel, launchApp: (InstalledAppInfo) -> Unit) {
    val tab by viewModel.currentTab.collectAsState()
    val context = LocalContext.current
    val apps by viewModel.displayedApps.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val query by viewModel.search.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadApps(context) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeTab(
                        apps = apps,
                        columns = appearance.columns,
                        showSearchBar = appearance.showSearchBar,
                        query = query,
                        onQuery = viewModel::setQuery,
                        onOpen = launchApp
                    )
                    else -> SettingsTab(
                        rules = rules,
                        columns = appearance.columns,
                        showSearchBar = appearance.showSearchBar,
                        backgroundColor = appearance.backgroundColor,
                        accentColor = appearance.accentColor,
                        onToggle = viewModel::toggle,
                        onDelete = viewModel::delete,
                        onDuplicate = viewModel::duplicate,
                        onAdd = viewModel::add,
                        onColumnsChange = viewModel::setColumns,
                        onShowSearchChange = viewModel::setShowSearchBar,
                        onBackgroundChange = viewModel::setBackgroundColor,
                        onAccentChange = viewModel::setAccentColor
                    )
                }
            }
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(tab == 0, { viewModel.setTab(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("خانه") })
                NavigationBarItem(tab == 1, { viewModel.setTab(1) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("تنظیمات") })
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HomeTab(
    apps: List<Pair<String, InstalledAppInfo>>,
    columns: Int,
    showSearchBar: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (InstalledAppInfo) -> Unit
) {
    val context = LocalContext.current
    var uninstallTarget by remember { mutableStateOf<Pair<String, InstalledAppInfo>?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("امیر.سلام", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp))
        if (showSearchBar) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                singleLine = true,
                label = { Text("جست‌وجوی برنامه") }
            )
        } else {
            Spacer(Modifier.height(12.dp))
        }
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.weight(1f)) {
            gridItems(apps, key = { it.second.packageName }) { (name, app) ->
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .combinedClickable(
                            onClick = { onOpen(app) },
                            onLongClick = {
                                // Own package (e.g. the built-in Gallery entry) can't be
                                // offered for uninstall from here.
                                if (app.packageName != context.packageName) {
                                    uninstallTarget = name to app
                                }
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = name,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        name,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    val target = uninstallTarget
    if (target != null) {
        val (name, app) = target
        AlertDialog(
            onDismissRequest = { uninstallTarget = null },
            title = { Text("حذف $name") },
            text = { Text("برای حذف این برنامه، صفحه‌ی رسمی حذف Android باز می‌شود و باید آنجا تأیید کنید.") },
            confirmButton = {
                Button(onClick = {
                    uninstallTarget = null
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${app.packageName}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "حذف این برنامه از اینجا ممکن نیست", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("ادامه و حذف") }
            },
            dismissButton = {
                TextButton(onClick = { uninstallTarget = null }) { Text("لغو") }
            }
        )
    }
}

@Composable
private fun SettingsTab(
    rules: List<Rule>,
    columns: Int,
    showSearchBar: Boolean,
    backgroundColor: Long,
    accentColor: Long,
    onToggle: (Rule) -> Unit,
    onDelete: (Rule) -> Unit,
    onDuplicate: (Rule) -> Unit,
    onAdd: (String, String, Boolean, Boolean) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onShowSearchChange: (Boolean) -> Unit,
    onBackgroundChange: (Long) -> Unit,
    onAccentChange: (Long) -> Unit
) {
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    val onSurface = MaterialTheme.colorScheme.onBackground

    // The whole tab is now ONE scrollable LazyColumn. It used to be a plain
    // Column with no scrolling at all: once the appearance/color/quick-access
    // cards already filled the screen height, everything after them --
    // "مدیریت اسم‌ها", the "+ افزودن اسم" button, the rule list, and the
    // support section -- was pushed off the bottom of the screen with no
    // way to reach it. That's the bug: the button wasn't broken, it was
    // simply never visible/reachable on screens where the cards above it
    // didn't leave enough room.
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("تنظیمات", color = onSurface, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
        }

        item {
            SettingsSection(title = "ظاهر صفحه اصلی") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("تعداد ستون‌ها: $columns", color = onSurface)
                    Row {
                        OutlinedButton(onClick = { if (columns > 3) onColumnsChange(columns - 1) }) { Text("−") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { if (columns < 6) onColumnsChange(columns + 1) }) { Text("+") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("نمایش نوار جست‌وجو", color = onSurface)
                    Switch(checked = showSearchBar, onCheckedChange = onShowSearchChange)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            SettingsSection(title = "رنگ پس‌زمینه") {
                ColorSwatchRow(
                    colors = listOf(0xFF0B0D12, 0xFF10131C, 0xFF1A1030, 0xFF0F1F16, 0xFF2A1216, 0xFF16181C, 0xFF000000),
                    selected = backgroundColor,
                    onSelect = onBackgroundChange
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            SettingsSection(title = "رنگ اصلی (تم)") {
                ColorSwatchRow(
                    colors = listOf(0xFF8DE1BE, 0xFFE1C88D, 0xFF8DC7E1, 0xFFE18D9E, 0xFFB98DE1, 0xFFE1E1E1),
                    selected = accentColor,
                    onSelect = onAccentChange
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            SettingsSection(title = "دسترسی سریع") {
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "تنظیمات گوشی پیدا نشد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("باز کردن تنظیمات گوشی") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "این گزینه روی این گوشی موجود نیست", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("تغییر صفحه اصلی پیش‌فرض") }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("مدیریت اسم‌ها", color = onSurface, style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showAdd = true }) { Text("+ افزودن اسم") }
            }
            Spacer(Modifier.height(8.dp))
        }

        items(rules, key = { it.id }) { rule ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(rule.source, color = onSurface, style = MaterialTheme.typography.titleSmall)
                    Text("↓", color = Color(0xFF7B8797))
                    Text(rule.replacement, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onToggle(rule) }) { Text(if (rule.enabled) "فعال" else "غیرفعال") }
                        TextButton(onClick = { onDuplicate(rule) }) { Text("تکثیر") }
                        TextButton(onClick = { onDelete(rule) }) { Text("حذف") }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            SupportRow()
        }
    }
    if (showAdd) AddDialog({ showAdd = false }, onAdd)
}

@Composable
private fun ColorSwatchRow(colors: List<Long>, selected: Long, onSelect: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { value ->
            val isSelected = value == selected
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(value.toInt()))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF3A3F4A),
                        shape = CircleShape
                    )
                    .clickable { onSelect(value) }
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private const val SUPPORT_EMAIL = "zwla54329@gmail.com"

@Composable
private fun SupportRow() {
    val context = LocalContext.current
    SettingsSection(title = "پشتیبانی") {
        Text(SUPPORT_EMAIL, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, "پشتیبانی امیر.سلام")
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "برنامه ایمیل پیدا نشد", Toast.LENGTH_LONG).show()
                }
            }) { Text("ارسال ایمیل") }
            TextButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("email", SUPPORT_EMAIL))
                Toast.makeText(context, "ایمیل کپی شد", Toast.LENGTH_SHORT).show()
            }) { Text("کپی آدرس") }
        }
    }
}

@Composable
private fun AddDialog(close: () -> Unit, save: (String, String, Boolean, Boolean) -> Unit) {
    var source by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var exact by remember { mutableStateOf(false) }
    var sensitive by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("افزودن اسم جدید") },
        text = {
            Column {
                OutlinedTextField(source, { source = it }, label = { Text("نام اصلی برنامه") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(replacement, { replacement = it }, label = { Text("نام دلخواه") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(exact, { exact = it }); Text("تطبیق دقیق")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(sensitive, { sensitive = it }); Text("حساس به حروف")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                save(source, replacement, exact, sensitive)
                close()
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = close) { Text("لغو") } }
    )
}

@Composable
fun AmirSalamTheme(
    background: Color = Color(0xFF0B0D12),
    accent: Color = Color(0xFF8DE1BE),
    content: @Composable () -> Unit
) {
    val surface = lerp(background, Color.White, 0.07f)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            background = background,
            surface = surface,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}
