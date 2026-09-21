package com.example.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.commandcenter.permissions.AccessibilityStatus
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

@Composable
fun MainScreen(viewModel: AmirSalamViewModel, openAccessibility: () -> Unit) {
    var on by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var accessibilityEnabled by remember { mutableStateOf(AccessibilityStatus.isEnabled(context)) }
    val rules by viewModel.rules.collectAsState()
    val filtered by viewModel.filteredRules.collectAsState()
    val query by viewModel.search.collectAsState()

    Surface(Modifier.fillMaxSize(), color = Color(0xFF0B0D12)) {
        Column(Modifier.fillMaxSize()) {
            if (tab == 0) {
                LaunchedEffect(Unit) {
                    accessibilityEnabled = AccessibilityStatus.isEnabled(context)
                    if (!accessibilityEnabled) on = false
                }
                Home(
                    on = on,
                    active = rules.count { it.enabled },
                    accessibilityEnabled = accessibilityEnabled,
                    openAccessibility = {
                        openAccessibility()
                        accessibilityEnabled = true
                    },
                    toggle = {
                        accessibilityEnabled = AccessibilityStatus.isEnabled(context)
                        if (!accessibilityEnabled) {
                            openAccessibility()
                        } else {
                            on = !on
                        }
                    }
                )
            } else if (tab == 2) {
                SupportTab()
            } else {
                RulesTab(
                    rules = filtered,
                    query = query,
                    onQuery = viewModel::setQuery,
                    onToggle = viewModel::toggle,
                    onDelete = viewModel::delete,
                    onDuplicate = viewModel::duplicate,
                    onAdd = viewModel::add
                )
            }
            NavigationBar(containerColor = Color(0xFF11141B)) {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("خانه") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.List, null) }, label = { Text("اسم‌ها") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Email, null) }, label = { Text("پشتیبانی") })
            }
        }
    }
}

@Composable
private fun Home(
    on: Boolean,
    active: Int,
    accessibilityEnabled: Boolean,
    openAccessibility: () -> Unit,
    toggle: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("امیر.سلام", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("مرکز مدیریت نام‌های سفارشی", color = Color(0xFF9AA4B2))
        Spacer(Modifier.height(35.dp))

        Button(
            onClick = toggle,
            modifier = Modifier.size(210.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (on) Color(0xFF185B45) else Color(0xFF252A33)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (on) "روشن" else "خاموش", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(if (on) "سرویس نمایشی فعال" else "برای فعال‌سازی ضربه بزن")
            }
        }

        Spacer(Modifier.height(25.dp))
        StatusCard("نام‌ها", "$active قانون فعال")
        StatusCard(
            "دسترسی‌پذیری",
            if (accessibilityEnabled) "فعال ✓" else "غیرفعال — لازم است"
        )
        if (!accessibilityEnabled) {
            Button(onClick = openAccessibility) { Text("فعال‌سازی دسترسی‌پذیری") }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "تغییرها فقط نمایشی هستند و نام واقعی Android یا برنامه‌های دیگر دستکاری نمی‌شود.",
            color = Color(0xFF9AA4B2)
        )
    }
}

private const val SUPPORT_EMAIL = "zwla54329@gmail.com"

@Composable
private fun SupportTab() {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("پشتیبانی", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "برای گزارش مشکل، پیشنهاد یا سؤال به ایمیل زیر پیام بده. این برنامه اینترنت ندارد؛ فقط برنامه‌ی ایمیل گوشی‌ات باز می‌شود.",
            color = Color(0xFF9AA4B2)
        )
        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151922))
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("ایمیل پشتیبانی", color = Color(0xFF9AA4B2))
                Spacer(Modifier.height(4.dp))
                Text(SUPPORT_EMAIL, color = Color(0xFF8DE1BE), style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, "پشتیبانی امیر.سلام")
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "برنامه‌ی ایمیل پیدا نشد. آدرس را کپی کن.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ارسال ایمیل") }
        OutlinedButton(
            onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("email", SUPPORT_EMAIL))
                Toast.makeText(context, "ایمیل کپی شد", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("کپی آدرس ایمیل") }
    }
}

@Composable
private fun StatusCard(title: String, value: String) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151922))
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White)
            Text(value, color = Color(0xFF8DE1BE))
        }
    }
}

@Composable
private fun RulesTab(
    rules: List<com.example.commandcenter.data.Rule>,
    query: String,
    onQuery: (String) -> Unit,
    onToggle: (com.example.commandcenter.data.Rule) -> Unit,
    onDelete: (com.example.commandcenter.data.Rule) -> Unit,
    onDuplicate: (com.example.commandcenter.data.Rule) -> Unit,
    onAdd: (String, String, Boolean, Boolean) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("مدیریت اسم‌ها", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showAdd = true }) { Text("+ افزودن اسم جدید") }
        }
        OutlinedTextField(
            value = query, onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            label = { Text("جست‌وجوی اسم") }
        )
        LazyColumn {
            items(rules, key = { it.id }) { rule ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151922))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(rule.source, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("↓", color = Color(0xFF7B8797))
                        Text(rule.replacement, color = Color(0xFF8DE1BE))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onToggle(rule) }) { Text(if (rule.enabled) "فعال" else "غیرفعال") }
                            TextButton(onClick = { onDuplicate(rule) }) { Text("تکثیر") }
                            TextButton(onClick = { onDelete(rule) }) { Text("حذف") }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) AddDialog({ showAdd = false }, onAdd)
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
                OutlinedTextField(source, { source = it }, label = { Text("نام اصلی") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(replacement, { replacement = it }, label = { Text("نام جدید") })
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
fun AmirSalamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8DE1BE),
            background = Color(0xFF0B0D12),
            surface = Color(0xFF151922)
        ),
        content = content
    )
}
