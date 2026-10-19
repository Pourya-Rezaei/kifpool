package com.kifpool.app.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kifpool.app.WalletViewModel
import com.kifpool.app.data.DebtEntity
import com.kifpool.app.data.DebtType
import com.kifpool.app.logic.Jalali
import com.kifpool.app.ui.components.ConfirmDialog
import com.kifpool.app.ui.components.EmptyState
import com.kifpool.app.ui.theme.LocalAppColors
import com.kifpool.app.util.Fmt
import java.time.LocalDate

/**
 * دفتر طلب و بدهی (نسخهٔ ۱٫۶):
 * «به من بدهکاره» یا «من بدهکارم» با سررسید شمسی، تسویه و یادآور.
 */
@Composable
fun DebtsScreen(vm: WalletViewModel) {
    val debts by vm.debts.collectAsState()
    val appColors = LocalAppColors.current

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DebtEntity?>(null) }
    var toDelete by remember { mutableStateOf<DebtEntity?>(null) }
    var reminderFor by remember { mutableStateOf<DebtEntity?>(null) }
    var filter by remember { mutableIntStateOf(FILTER_ALL) }

    val open = debts.filter { !it.settled }
    val totalCredit = open.filter { it.type == DebtType.CREDIT }.sumOf { it.amount }
    val totalDebt = open.filter { it.type == DebtType.DEBT }.sumOf { it.amount }

    val filtered = when (filter) {
        FILTER_CREDIT -> debts.filter { it.type == DebtType.CREDIT && !it.settled }
        FILTER_DEBT -> debts.filter { it.type == DebtType.DEBT && !it.settled }
        FILTER_SETTLED -> debts.filter { it.settled }
        else -> debts
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("دفتر حساب", style = MaterialTheme.typography.headlineSmall)
        Text(
            "طلب‌ها و بدهی‌هایت را با سررسید و یادآور پیگیری کن",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ---------- خلاصه ----------
        Spacer(Modifier.height(14.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Handshake,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "جمع تسویه‌نشده‌ها",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCell(
                        title = "مطالبات من",
                        value = Fmt.money(totalCredit),
                        color = appColors.income,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCell(
                        title = "بدهی من",
                        value = Fmt.money(totalDebt),
                        color = appColors.expense,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                val net = totalCredit - totalDebt
                Text(
                    text = when {
                        net > 0 -> "در نهایت " + Fmt.money(net) + " تومان طلب داری"
                        net < 0 -> "در نهایت " + Fmt.money(-net) + " تومان بدهکاری"
                        else -> "جمع مطالبات و بدهی‌ها برابر است"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------- فیلترها ----------
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("همه", filter == FILTER_ALL, Modifier.weight(1f)) { filter = FILTER_ALL }
            FilterChip("طلب‌ها", filter == FILTER_CREDIT, Modifier.weight(1f)) { filter = FILTER_CREDIT }
            FilterChip("بدهی‌ها", filter == FILTER_DEBT, Modifier.weight(1f)) { filter = FILTER_DEBT }
            FilterChip("تسویه‌شده", filter == FILTER_SETTLED, Modifier.weight(1f)) { filter = FILTER_SETTLED }
        }

        // ---------- دکمه افزودن ----------
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdd = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "ثبت طلب یا بدهی جدید",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // ---------- لیست ----------
        if (filtered.isEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "موردی ثبت نشده",
                    subtitle = "قرض‌ها، وام‌ها و حساب‌های دوستانت را اینجا ثبت کن تا یادت نرود"
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp)) {
                    filtered.forEach { debt ->
                        DebtRow(
                            debt = debt,
                            onSettle = { vm.setDebtSettled(debt, !debt.settled) },
                            onEdit = { editing = debt },
                            onReminder = { reminderFor = debt },
                            onDelete = { toDelete = debt }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(96.dp))
    }

    // ---------- دیالوگ‌ها ----------
    if (showAdd) {
        DebtDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { person, amount, type, note, dueDate ->
                vm.saveDebt(null, person, amount, type, note, dueDate)
                showAdd = false
            }
        )
    }

    editing?.let { debt ->
        DebtDialog(
            initial = debt,
            onDismiss = { editing = null },
            onSave = { person, amount, type, note, dueDate ->
                vm.saveDebt(debt.id, person, amount, type, note, dueDate)
                editing = null
            }
        )
    }

    toDelete?.let { debt ->
        ConfirmDialog(
            title = "حذف " + (if (debt.type == DebtType.CREDIT) "طلب" else "بدهی"),
            text = "ثبت مربوط به «" + debt.person + "» برای همیشه حذف می‌شود. ادامه می‌دهی؟",
            confirmLabel = "حذف",
            onConfirm = {
                vm.deleteDebt(debt)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }

    reminderFor?.let { debt ->
        ReminderForDebtDialog(
            debt = debt,
            onDismiss = { reminderFor = null },
            onConfirm = { day ->
                vm.createReminderForDebt(debt, day)
                reminderFor = null
            }
        )
    }
}

private const val FILTER_ALL = 0
private const val FILTER_CREDIT = 1
private const val FILTER_DEBT = 2
private const val FILTER_SETTLED = 3

@Composable
private fun SummaryCell(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DebtRow(
    debt: DebtEntity,
    onSettle: () -> Unit,
    onEdit: () -> Unit,
    onReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val appColors = LocalAppColors.current
    val isCredit = debt.type == DebtType.CREDIT
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        // آواتار حرف اول نام
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    (if (isCredit) appColors.income else appColors.expense).copy(alpha = 0.16f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = debt.person.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = if (isCredit) appColors.income else appColors.expense
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = debt.person + "  " + (if (isCredit) "(به تو بدهکاره)" else "(تو بدهکاری)"),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (debt.settled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
            if (debt.note.isNotBlank()) {
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (debt.dueDate > 0) {
                Text(
                    text = "سررسید: " + Fmt.dateShort(Jalali.dateOf(debt.dueDate)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = Fmt.money(debt.amount),
            style = MaterialTheme.typography.titleSmall,
            color = if (debt.settled) MaterialTheme.colorScheme.onSurfaceVariant
                    else if (isCredit) appColors.income else appColors.expense
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onSettle) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = if (debt.settled) "لغو تسویه" else "تسویه شد",
                tint = if (debt.settled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Rounded.Edit,
                contentDescription = "ویرایش",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onReminder) {
            Icon(
                Icons.Rounded.Alarm,
                contentDescription = "ساخت یادآور",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "حذف",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** دیالوگ ثبت/ویرایش طلب و بدهی. */
@Composable
private fun DebtDialog(
    initial: DebtEntity?,
    onDismiss: () -> Unit,
    onSave: (person: String, amount: Long, type: Int, note: String, dueDate: Long) -> Unit
) {
    var person by remember { mutableStateOf(initial?.person ?: "") }
    var amountText by remember {
        mutableStateOf(Fmt.amountInput((initial?.amount ?: 0L).toString()))
    }
    var type by remember { mutableIntStateOf(initial?.type ?: DebtType.CREDIT) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var dueDate by remember { mutableStateOf(initial?.dueDate ?: 0L) }
    var showDate by remember { mutableStateOf(false) }

    val amount = Fmt.parseAmount(amountText)
    val valid = person.isNotBlank() && amount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "ثبت طلب یا بدهی" else "ویرایش ثبت") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeCell(
                        label = "به من بدهکاره",
                        selected = type == DebtType.CREDIT,
                        modifier = Modifier.weight(1f)
                    ) { type = DebtType.CREDIT }
                    TypeCell(
                        label = "من بدهکارم",
                        selected = type == DebtType.DEBT,
                        modifier = Modifier.weight(1f)
                    ) { type = DebtType.DEBT }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it.take(40) },
                    label = { Text("نام طرف حساب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = Fmt.amountInput(it) },
                    label = { Text("مبلغ (تومان)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(80) },
                    label = { Text("یادداشت (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDate = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (dueDate > 0)
                                "سررسید: " + Fmt.dateShort(Jalali.dateOf(dueDate))
                            else
                                "تعیین سررسید (اختیاری)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.weight(1f))
                        if (dueDate > 0) {
                            Text(
                                "حذف",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { dueDate = 0L }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(person, amount, type, note, dueDate) }
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )

    if (showDate) {
        JalaliDatePickerDialog(
            initial = if (dueDate > 0) Jalali.dateOf(dueDate) else LocalDate.now(),
            onDismiss = { showDate = false },
            onSelected = { picked ->
                dueDate = Jalali.startOfDayMillis(picked)
                showDate = false
            }
        )
    }
}

/** دیالوگ ساخت یادآور ماهانه از روی بدهی/طلب. */
@Composable
private fun ReminderForDebtDialog(
    debt: DebtEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dayText by remember {
        mutableStateOf(
            Fmt.fa(
                if (debt.dueDate > 0) Jalali.toJalali(Jalali.dateOf(debt.dueDate)).day
                else Jalali.toJalali(LocalDate.now()).day
            ).toString()
        )
    }
    val day = (Fmt.digitsOnly(dayText).toIntOrNull() ?: 1).coerceIn(1, 31)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("یادآور ماهانه") },
        text = {
            Column {
                Text(
                    "هر ماه روز " + Fmt.fa(day) + " یادآوری برای «" + debt.person + "» ساخته می‌شود",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = Fmt.amountInput(it) },
                    label = { Text("روز ماه (۱ تا ۳۱)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "یادآور در صفحهٔ «یادآورها» قابل مدیریت است",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(day) }) { Text("ساخت یادآور") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun TypeCell(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center
        )
    }
}
