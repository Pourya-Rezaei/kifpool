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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kifpool.app.WalletViewModel
import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.RecurFreq
import com.kifpool.app.data.RecurringTransactionEntity
import com.kifpool.app.data.TxType
import com.kifpool.app.logic.RecurringEngine
import com.kifpool.app.ui.components.EmptyState
import com.kifpool.app.util.Fmt
import com.kifpool.app.util.Presets
import java.time.LocalDate

/**
 * صفحهٔ تراکنش‌های تکراری (نسخهٔ ۱٫۸) — مدیریت پرداخت/درآمدهای دوره‌ای.
 *
 * هر آیتم دو حالت دارد:
 *  - ثبت خودکار: در هر سررسید، تراکنش واقعی ساخته می‌شود (با جبران عقب‌افتادگی)
 *  - فقط یادآور: در روز سررسید اعلان می‌آید و ثبت با خود کاربر است
 */
@Composable
fun RecurringScreen(
    vm: WalletViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val recurring by vm.recurring.collectAsState()
    val categories by vm.categories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var editing by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(Modifier.weight(1f)) {
                Text("تراکنش‌های تکراری", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "ثبت خودکار اجاره، قسط، اشتراک و حقوق",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Rounded.Repeat, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("تکرارشوی جدید", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))

        if (recurring.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Repeat,
                title = "هنوز تکرارشویی نداری",
                subtitle = "پرداخت‌های دوره‌ای مثل اجاره و قسط را بسپار به دستیار مالی‌ات"
            )
            Spacer(Modifier.height(80.dp))
        } else {
            val catById = categories.associateBy { it.id }
            val today = LocalDate.now()
            recurring.forEach { rec ->
                RecurringCard(
                    rec = rec,
                    category = catById[rec.categoryId],
                    today = today,
                    onToggle = { vm.setRecurringEnabled(rec, it) },
                    onEdit = { editing = rec },
                    onDelete = { deleteTarget = rec }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    // ---- دیالوگ افزودن/ویرایش ---------------------------------------------------
    if (showAddDialog || editing != null) {
        RecurringDialog(
            initial = editing,
            categories = categories,
            onDismiss = { showAddDialog = false; editing = null },
            onSave = { title, amount, type, categoryId, frequency, day, weekday, monthOfYear, autoAdd ->
                vm.saveRecurring(
                    id = editing?.id,
                    title = title,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    frequency = frequency,
                    dayOfMonth = day,
                    weekday = weekday,
                    monthOfYear = monthOfYear,
                    autoAdd = autoAdd,
                    note = ""
                )
                showAddDialog = false
                editing = null
            }
        )
    }

    // ---- تأیید حذف --------------------------------------------------------------
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف تکرارشو") },
            text = { Text("«" + target.title + "» حذف شود؟ تراکنش‌های قبلاً ثبت‌شده باقی می‌مانند.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteRecurring(target)
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

// ---------------------------------------------------------------------------
// کارت آیتم
// ---------------------------------------------------------------------------

@Composable
private fun RecurringCard(
    rec: RecurringTransactionEntity,
    category: CategoryEntity?,
    today: LocalDate,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val appColors = com.kifpool.app.ui.theme.LocalAppColors.current
    val pending = RecurringEngine.pendingDate(rec, today)

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
                    .background(
                        if (rec.type == TxType.INCOME) appColors.incomeContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category?.let { Presets.iconOf(it.icon) } ?: Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = if (rec.type == TxType.INCOME) appColors.income
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    rec.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (rec.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    RecurringEngine.frequencyLabel(rec) + " — " + Fmt.money(rec.amount) + " تومان",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        pending != null -> "سررسید: " + Fmt.dateShort(pending) +
                            (if (rec.autoAdd) " (ثبت خودکار)" else " (فقط یادآوری)")
                        else -> {
                            val next = RecurringEngine.upcomingDate(rec, today)
                            if (next != null) "سررسید بعدی: " + Fmt.dateShort(next)
                            else if (rec.autoAdd) "ثبت خودکار" else "فقط یادآوری"
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = if (pending != null) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = rec.enabled, onCheckedChange = onToggle)
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

// ---------------------------------------------------------------------------
// چیپ انتخاب عمومی — برای روز/روز هفته/ماه/دسته (DRY)
// ---------------------------------------------------------------------------

@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** شبکهٔ چیپ‌ها با چیدمان ردیفی ثابت (ایندکس‌ها مستقل از نام‌های تکراری) */
@Composable
private fun ChipGrid(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.withIndex().toList().chunked(4).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chunk.forEach { (globalIndex, label) ->
                    SelectionChip(
                        label = label,
                        selected = globalIndex == selectedIndex,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(globalIndex) }
                    )
                }
                repeat(4 - chunk.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// دیالوگ افزودن/ویرایش تکرارشو
// ---------------------------------------------------------------------------

@Composable
private fun RecurringDialog(
    initial: RecurringTransactionEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String, amount: Long, type: Int, categoryId: Long,
        frequency: Int, dayOfMonth: Int, weekday: Int, monthOfYear: Int,
        autoAdd: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountText by remember {
        mutableStateOf(if ((initial?.amount ?: 0L) > 0) Fmt.amountInput(initial!!.amount.toString()) else "")
    }
    var type by remember { mutableStateOf(initial?.type ?: TxType.EXPENSE) }
    var categoryId by remember {
        mutableStateOf(initial?.categoryId ?: categories.firstOrNull { it.type == (initial?.type ?: TxType.EXPENSE) }?.id ?: 0L)
    }
    var frequency by remember { mutableStateOf(initial?.frequency ?: RecurFreq.MONTHLY) }
    var day by remember { mutableStateOf(initial?.dayOfMonth ?: 1) }
    var weekday by remember { mutableStateOf(initial?.weekday ?: 0) }
    var monthOfYear by remember { mutableStateOf(initial?.monthOfYear ?: 1) }
    var autoAdd by remember { mutableStateOf(initial?.autoAdd ?: true) }

    val typeCats = categories.filter { it.type == type }
    // اگر نوع عوض شد و دستهٔ فعلی از نوع دیگر است، اولین دستهٔ هم‌نوع انتخاب شود
    val effectiveCategoryId =
        if (typeCats.any { it.id == categoryId }) categoryId
        else typeCats.firstOrNull()?.id ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "تکرارشوی جدید" else "ویرایش تکرارشو") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") },
                    placeholder = { Text("مثلاً: اجاره خانه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = Fmt.amountInput(it) },
                    label = { Text("مبلغ (تومان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                // نوع: هزینه / درآمد
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionChip(
                        label = "هزینه",
                        selected = type == TxType.EXPENSE,
                        modifier = Modifier.weight(1f),
                        onClick = { type = TxType.EXPENSE }
                    )
                    SelectionChip(
                        label = "درآمد",
                        selected = type == TxType.INCOME,
                        modifier = Modifier.weight(1f),
                        onClick = { type = TxType.INCOME }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("دسته", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                if (typeCats.isEmpty()) {
                    Text(
                        "دسته‌ای از این نوع نداری — از تب «دسته‌ها» اضافه کن",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ChipGrid(
                        items = typeCats.map { it.name },
                        selectedIndex = typeCats.indexOfFirst { it.id == effectiveCategoryId },
                        onSelect = { idx -> typeCats.getOrNull(idx)?.let { categoryId = it.id } }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("تناوب", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionChip(
                        label = "ماهانه",
                        selected = frequency == RecurFreq.MONTHLY,
                        modifier = Modifier.weight(1f),
                        onClick = { frequency = RecurFreq.MONTHLY }
                    )
                    SelectionChip(
                        label = "هفتگی",
                        selected = frequency == RecurFreq.WEEKLY,
                        modifier = Modifier.weight(1f),
                        onClick = { frequency = RecurFreq.WEEKLY }
                    )
                    SelectionChip(
                        label = "سالانه",
                        selected = frequency == RecurFreq.YEARLY,
                        modifier = Modifier.weight(1f),
                        onClick = { frequency = RecurFreq.YEARLY }
                    )
                }

                Spacer(Modifier.height(12.dp))
                when (frequency) {
                    RecurFreq.WEEKLY -> {
                        Text("روز هفته", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        ChipGrid(
                            items = RecurringEngine.WEEKDAY_NAMES,
                            selectedIndex = weekday,
                            onSelect = { weekday = it }
                        )
                    }

                    RecurFreq.YEARLY -> {
                        Text("ماه و روز هر سال", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        ChipGrid(
                            items = com.kifpool.app.logic.Jalali.MONTH_NAMES,
                            selectedIndex = monthOfYear - 1,
                            onSelect = { monthOfYear = it + 1 }
                        )
                        Spacer(Modifier.height(8.dp))
                        DayGrid(day = day, onDay = { day = it })
                    }

                    else -> {
                        Text("روز ماه", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        DayGrid(day = day, onDay = { day = it })
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ثبت خودکار", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "خاموش = فقط اعلان یادآوری در روز سررسید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoAdd, onCheckedChange = { autoAdd = it })
                }

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "اگر ماه آن روز را نداشته باشد، آخرین روز ماه ملاک است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = Fmt.parseAmount(amountText)
                    if (title.trim().isNotEmpty() && amount > 0 && effectiveCategoryId > 0) {
                        onSave(
                            title.trim(), amount, type, effectiveCategoryId,
                            frequency, day, weekday, monthOfYear, autoAdd
                        )
                    }
                },
                enabled = title.trim().isNotEmpty() &&
                    Fmt.parseAmount(amountText) > 0 && effectiveCategoryId > 0
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

/** شبکهٔ انتخاب روز ماه ۱..۳۱ (الگوی RemindersScreen) */
@Composable
private fun DayGrid(day: Int, onDay: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..31).toList().chunked(8).forEach { chunk ->
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
                            .clickable { onDay(d) },
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
}
