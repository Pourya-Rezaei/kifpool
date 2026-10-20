package com.kifpool.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kifpool.app.WalletViewModel
import com.kifpool.app.security.PinPolicy
import com.kifpool.app.ui.components.AmountDialog
import com.kifpool.app.ui.components.ConfirmDialog
import com.kifpool.app.ui.components.SectionTitle
import com.kifpool.app.util.BackupJson
import com.kifpool.app.util.Biometrics
import com.kifpool.app.util.Csv
import com.kifpool.app.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    vm: WalletViewModel,
    onOpenReminders: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenGoals: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dark by vm.darkMode.collectAsState()
    val budget by vm.monthlyBudget.collectAsState()
    val goal by vm.weeklyGoal.collectAsState()
    val lockEnabled by vm.lockEnabled.collectAsState()
    val biometricEnabled by vm.biometricEnabled.collectAsState()
    val securityQuestion by vm.securityQuestion.collectAsState()

    val canBiometric = remember { Biometrics.canUseBiometric(context) }

    var editBudget by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf(false) }
    var confirmSample by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var confirmRemoveLock by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    // انتخاب فایل پشتیبان برای بازیابی
    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = vm.restoreBackup(context, uri)
                Toast.makeText(
                    context,
                    if (ok) "بازیابی با موفقیت انجام شد" else "فایل پشتیبان معتبر نیست",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall)
        Text(
            "بودجه، امنیت، یادآورها و مدیریت داده‌های تو",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionTitle("بودجه و اهداف")
        SettingRow(
            icon = Icons.Rounded.Savings,
            title = "بودجه ماهانه",
            subtitle = Fmt.money(budget) + " تومان",
            onClick = { editBudget = true }
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            icon = Icons.Rounded.Flag,
            title = "هدف هفتگی هزینه",
            subtitle = Fmt.money(goal) + " تومان",
            onClick = { editGoal = true }
        )

        SectionTitle("دستیار مالی")
        SettingRow(
            icon = Icons.Rounded.Repeat,
            title = "تراکنش‌های تکراری",
            subtitle = "ثبت خودکار اجاره، قسط، اشتراک و حقوق‌های دوره‌ای",
            onClick = onOpenRecurring
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            icon = Icons.Rounded.Savings,
            title = "اهداف پس‌انداز",
            subtitle = "پس‌انداز قدم‌به‌قدم برای سفر، گوشی و رویاهایت",
            onClick = onOpenGoals
        )

        SectionTitle("یادآورها")
        SettingRow(
            icon = Icons.Rounded.Alarm,
            title = "یادآورهای پرداخت",
            subtitle = "یادآوری اجاره، قبض، قسط و هر پرداخت ماهانه",
            onClick = onOpenReminders
        )

        SectionTitle("امنیت")
        SwitchRow(
            icon = Icons.Rounded.Lock,
            title = "قفل برنامه",
            subtitle = if (lockEnabled) "ورود با PIN و اثر انگشت فعال است"
            else "ورود سریع بدون رمز",
            checked = lockEnabled,
            onCheckedChange = { enabled ->
                if (enabled) showPinDialog = true else confirmRemoveLock = true
            }
        )
        if (lockEnabled) {
            Spacer(Modifier.height(10.dp))
            SettingRow(
                icon = Icons.Rounded.Password,
                title = "تغییر رمز PIN",
                subtitle = "یک رمز جدید برای ورود تعیین کن",
                onClick = { showPinDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            SettingRow(
                icon = Icons.Rounded.HelpOutline,
                title = "سوال امنیتی بازیابی",
                subtitle = securityQuestion.ifBlank {
                    "هنوز تعیین نشده — برای فراموشی رمز، آن را از «تغییر رمز PIN» تنظیم کن"
                },
                onClick = { showPinDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            SwitchRow(
                icon = Icons.Rounded.Fingerprint,
                title = "ورود با اثر انگشت",
                subtitle = if (canBiometric) "باز کردن سریع قفل با حسگر اثر انگشت"
                else "حسگر اثر انگشت روی این گوشی در دسترس نیست",
                checked = biometricEnabled && canBiometric,
                enabled = canBiometric,
                onCheckedChange = { vm.setBiometricEnabled(it) }
            )
        }

        SectionTitle("ظاهر")
        SwitchRow(
            icon = Icons.Rounded.WbSunny,
            title = "حالت شب",
            subtitle = "تم تیره مدرن برای چشم‌های راحت‌تر",
            checked = dark,
            onCheckedChange = { vm.setDarkMode(it) }
        )

        SectionTitle("داده‌ها")
        SettingRow(
            icon = Icons.Rounded.FileDownload,
            title = "خروجی CSV",
            subtitle = "اشتراک‌گذاری همه تراکنش‌ها در قالب فایل اکسل",
            onClick = {
                scope.launch {
                    val (txs, cats) = vm.snapshot()
                    if (txs.isEmpty()) {
                        Toast.makeText(
                            context,
                            "هنوز تراکنشی برای خروجی وجود ندارد",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val file = withContext(Dispatchers.IO) {
                            Csv.write(context, Csv.build(txs, cats))
                        }
                        Csv.share(context, file)
                    }
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            icon = Icons.Rounded.Backup,
            title = "پشتیبان‌گیری JSON",
            subtitle = "ذخیره کامل تراکنش‌ها، دسته‌ها و یادآورها در یک فایل",
            onClick = {
                scope.launch {
                    val backup = vm.fullBackup()
                    if (backup.transactions.isEmpty() && backup.categories.isEmpty() && backup.reminders.isEmpty()) {
                        Toast.makeText(context, "داده‌ای برای پشتیبان‌گیری وجود ندارد", Toast.LENGTH_SHORT).show()
                    } else {
                        val file = vm.writeBackupFile(context, backup)
                        BackupJson.share(context, file)
                    }
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            icon = Icons.Rounded.FileUpload,
            title = "بازیابی از فایل پشتیبان",
            subtitle = "بازگرداندن داده‌ها از فایل JSON قبلی",
            onClick = { confirmRestore = true }
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            icon = Icons.Rounded.Refresh,
            title = "بارگذاری داده نمونه",
            subtitle = "پاک کردن داده‌ها و ساخت نمونه‌ی آزمایشی",
            onClick = { confirmSample = true }
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            icon = Icons.Rounded.Delete,
            title = "پاک کردن همه داده‌ها",
            subtitle = "حذف کامل تراکنش‌ها و دسته‌بندی‌ها",
            danger = true,
            onClick = { confirmClear = true }
        )

        SectionTitle("درباره")
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("کیف پول من", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                // نسخه به‌صورت پویا از PackageManager خوانده می‌شود تا هرگز کهنه نشود
                val versionName = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                            .versionName ?: "?"
                    } catch (e: Exception) {
                        "?"
                    }
                }
                Text(
                    "نسخه " + Fmt.fa(versionName).replace('.', '٫'),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "دستیار مالی شخصی: دفتر خرج و درآمد، بودجه، طلب و بدهی، اهداف پس‌انداز و تراکنش‌های دوره‌ای — کاملاً آفلاین",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "داده‌ها فقط روی گوشی خودت ذخیره می‌شوند (Jetpack Compose + Room)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    if (editBudget) {
        AmountDialog(
            title = "بودجه ماهانه (تومان)",
            initial = budget,
            onConfirm = { vm.setBudget(it); editBudget = false },
            onDismiss = { editBudget = false }
        )
    }
    if (editGoal) {
        AmountDialog(
            title = "هدف هفتگی هزینه (تومان)",
            initial = goal,
            onConfirm = { vm.setWeeklyGoal(it); editGoal = false },
            onDismiss = { editGoal = false }
        )
    }
    if (confirmSample) {
        ConfirmDialog(
            title = "بارگذاری داده نمونه",
            text = "همه داده‌های فعلی پاک می‌شوند و داده‌های نمونه ساخته می‌شود. ادامه می‌دهی؟",
            confirmLabel = "بارگذاری",
            onConfirm = { vm.reloadSample(); confirmSample = false },
            onDismiss = { confirmSample = false }
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "پاک کردن همه داده‌ها",
            text = "همه تراکنش‌ها و دسته‌بندی‌ها برای همیشه حذف می‌شوند. این کار قابل بازگشت نیست!",
            confirmLabel = "پاک کن",
            onConfirm = { vm.clearAll(); confirmClear = false },
            onDismiss = { confirmClear = false }
        )
    }
    if (confirmRemoveLock) {
        ConfirmDialog(
            title = "برداشتن قفل",
            text = "رمز PIN و اثر انگشت حذف می‌شود و ورود به برنامه آزاد خواهد بود. ادامه می‌دهی؟",
            confirmLabel = "برداشتن قفل",
            onConfirm = { vm.removeLock(); confirmRemoveLock = false },
            onDismiss = { confirmRemoveLock = false }
        )
    }
    if (confirmRestore) {
        ConfirmDialog(
            title = "بازیابی از فایل پشتیبان",
            text = "همه داده‌های فعلی پاک شده و داده‌های فایل پشتیبان جایگزین می‌شوند. ادامه می‌دهی؟",
            confirmLabel = "انتخاب فایل",
            onConfirm = {
                confirmRestore = false
                try {
                    restorePicker.launch("application/json")
                } catch (e: Exception) {
                    restorePicker.launch("*/*")
                }
            },
            onDismiss = { confirmRestore = false }
        )
    }
    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onSave = { pin, question, answer ->
                vm.setPin(pin)
                if (question.isNotBlank() && answer.isNotBlank()) {
                    vm.setSecurityQuestion(question, answer)
                }
                showPinDialog = false
                Toast.makeText(context, "قفل فعال شد", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (pin: String, question: String, answer: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    // قواعد PIN از PinPolicy (تک مرجع) — با تکرار قبلی در LockScreen یکسان است
    val valid = PinPolicy.isValid(pin) && pin == pin2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعیین رمز PIN") },
        text = {
            Column {
                Text(
                    "رمز ۴ تا ۶ رقمی برای ورود به برنامه",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c in '0'..'9' }.take(6) },
                    label = { Text("رمز جدید") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin2,
                    onValueChange = { pin2 = it.filter { c -> c in '0'..'9' }.take(6) },
                    label = { Text("تکرار رمز") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pin2.isNotEmpty() && pin != pin2) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "رمزها یکسان نیستند",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "سوال امنیتی (پیشنهاد می‌شود) — اگر رمز را فراموش کردی با پاسخ آن وارد شوی",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it.take(60) },
                    label = { Text("مثلاً: نام بهترین دوستت؟") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it.take(40) },
                    label = { Text("پاسخ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(pin, question, answer) },
                enabled = valid
            ) { Text("فعال‌سازی قفل") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                Modifier
                    .size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
