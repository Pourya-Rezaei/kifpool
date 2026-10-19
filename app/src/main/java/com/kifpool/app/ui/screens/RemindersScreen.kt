package com.kifpool.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kifpool.app.WalletViewModel
import com.kifpool.app.data.ReminderEntity
import com.kifpool.app.ui.components.EmptyState
import com.kifpool.app.util.Fmt
import com.kifpool.app.worker.ReminderWorker

/**
 * صفحه یادآورهای پرداخت ماهانه.
 *
 * رفع کرش ۱٫۵٫۲: روی اندروید ۱۳+ هنگام زدن «یادآور جدید»، درخواست دسترسی
 * POST_NOTIFICATIONS با rememberLauncherForActivityResult نمایش داده می‌شود.
 * همه مسیرها (اجازه دادن / رد کردن / خطا) ایمن هستند و برنامه هرگز بسته نمی‌شود.
 */
@Composable
fun RemindersScreen(
    vm: WalletViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val reminders by vm.reminders.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ReminderEntity?>(null) }
    var editing by remember { mutableStateOf<ReminderEntity?>(null) }

    // ---- وضعیت دسترسی اعلان --------------------------------------------------
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var notifGranted by remember {
        mutableStateOf(
            if (needsPermission) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // درخواست دسترسی اعلان — هر نتیجه‌ای فقط دیالوگ افزودن را باز می‌کند، هیچ کرشی وجود ندارد
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        showAddDialog = true
        if (!granted) {
            Toast.makeText(
                context,
                "بدون دسترسی اعلان، یادآورها ثبت می‌شوند ولی اعلان نشان داده نمی‌شود",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestPermissionOrOpenDialog() {
        try {
            if (needsPermission && !notifGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showAddDialog = true
            }
        } catch (e: Exception) {
            // حتی اگر سیستم درخواست را نداد، ادامه با دیالوگ افزودن
            showAddDialog = true
        }
    }

    LaunchedEffect(Unit) {
        ReminderWorker.scheduleDaily(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(Modifier.weight(1f)) {
                Text("یادآورها", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "یادآوری پرداخت‌های ماهانه مثل اجاره، قبض و قسط",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- بنر دسترسی اعلان (وقتی اجازه داده نشده) --------------------------
        if (!notifGranted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "دسترسی اعلان فعال نیست",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "برای دیدن یادآوری‌ها، اجازه بده کیف پول اعلان نشان بدهد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } catch (e: Exception) {
                                    openAppSettings(context)
                                }
                            },
                            enabled = needsPermission
                        ) {
                            Text(if (needsPermission) "اجازه دادن" else "خودکار فعال شد")
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(onClick = { openAppSettings(context) }) {
                                Text("تنظیمات")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ---- دکمه یادآور جدید ---------------------------------------------------
        Button(
            onClick = { requestPermissionOrOpenDialog() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("یادآور جدید", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))

        // ---- لیست یادآورها -------------------------------------------------------
        if (reminders.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.NotificationsActive,
                title = "هنوز یادآوری نداری",
                subtitle = "با دکمه «یادآور جدید» اولین یادآوری پرداختت را بساز"
            )
            Spacer(Modifier.height(80.dp))
        } else {
            reminders.forEach { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggle = { vm.setReminderEnabled(reminder, it) },
                    onEdit = { editing = reminder },
                    onDelete = { deleteTarget = reminder }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    // ---- دیالوگ افزودن/ویرایش ---------------------------------------------------
    if (showAddDialog || editing != null) {
        ReminderDialog(
            initial = editing,
            onDismiss = { showAddDialog = false; editing = null },
            onSave = { title, amount, day ->
                vm.saveReminder(editing?.id, title, amount, day)
                showAddDialog = false
                editing = null
                Toast.makeText(context, "یادآور ذخیره شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ---- تأیید حذف --------------------------------------------------------------
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف یادآور") },
            text = { Text("یادآور «" + target.title + "» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteReminder(target)
                    deleteTarget = null
                }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("انصراف") }
            }
        )
    }
}

private fun openAppSettings(context: android.content.Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // مسیر جایگزین وجود ندارد
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (reminder.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (reminder.amount > 0)
                        "هر ماه " + Fmt.fa(reminder.dayOfMonth) +
                            " — " + Fmt.money(reminder.amount) + " تومان"
                    else
                        "هر ماه " + Fmt.fa(reminder.dayOfMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = reminder.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ReminderDialog(
    initial: ReminderEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, day: Int) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountText by remember {
        mutableStateOf(if ((initial?.amount ?: 0L) > 0) Fmt.amountInput(initial!!.amount.toString()) else "")
    }
    var day by remember { mutableStateOf(initial?.dayOfMonth ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "یادآور جدید" else "ویرایش یادآور") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان یادآور") },
                    placeholder = { Text("مثلاً: اجاره خانه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = Fmt.amountInput(it) },
                    label = { Text("مبلغ (تومان — اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "روز یادآوری در هر ماه",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                // انتخاب روز: شبکه ۱..۳۱
                val days = (1..31).toList()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    days.chunked(8).forEach { chunk ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            chunk.forEach { d ->
                                val selected = d == day
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { day = d },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        Fmt.fa(d),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "اگر ماه آن روز را نداشته باشد، آخرین روز ماه یادآوری می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalTitle = title.trim()
                    if (finalTitle.isNotEmpty()) {
                        onSave(finalTitle, Fmt.parseAmount(amountText), day)
                    }
                },
                enabled = title.trim().isNotEmpty()
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
