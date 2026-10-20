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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kifpool.app.WalletViewModel
import com.kifpool.app.data.GoalProgress
import com.kifpool.app.data.SavingsGoalEntity
import com.kifpool.app.ui.components.AmountDialog
import com.kifpool.app.ui.components.ConfirmDialog
import com.kifpool.app.ui.components.EmptyState
import com.kifpool.app.ui.components.ProgressBar
import com.kifpool.app.util.Fmt
import com.kifpool.app.util.Presets

/** آیکون‌های پیشنهادی برای اهداف — زیرمجموعهٔ آیکون‌های موجود (DRY) */
private val GOAL_ICONS = listOf(
    "savings", "flight", "home", "phone", "computer", "school", "gift", "health"
)

/**
 * صفحهٔ اهداف پس‌انداز (نسخهٔ ۱٫۸).
 *
 * هر هدف: عنوان + مبلغ هدف + آیکون/رنگ. پس‌انداز فعلی از جمع واریزی‌ها و
 * برداشت‌ها محاسبه می‌شود (منبع واحد حقیقت) و با نوار پیشرفت نمایش داده می‌شود.
 */
@Composable
fun GoalsScreen(
    vm: WalletViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val goals by vm.goals.collectAsState()
    val savedByGoal by vm.savedByGoal.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var depositTarget by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var withdrawTarget by remember { mutableStateOf<SavingsGoalEntity?>(null) }

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
                Text("اهداف پس‌انداز", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "قدم‌قدم تا رویاهایت",
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
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("هدف جدید", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))

        if (goals.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Savings,
                title = "هنوز هدفی نساخته‌ای",
                subtitle = "برای سفر، گوشی جدید یا هر رویای دیگرت یک هدف بساز و قدم‌به‌قدم پُرش کن"
            )
            Spacer(Modifier.height(80.dp))
        } else {
            goals.forEach { goal ->
                GoalCard(
                    goal = goal,
                    progress = GoalProgress(saved = savedByGoal[goal.id] ?: 0L, target = goal.targetAmount),
                    onEdit = { editing = goal },
                    onDeposit = { depositTarget = goal },
                    onWithdraw = { withdrawTarget = goal },
                    onDelete = { deleteTarget = goal }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    // ---- دیالوگ افزودن/ویرایش هدف ---------------------------------------------
    if (showAddDialog || editing != null) {
        GoalDialog(
            initial = editing,
            onDismiss = { showAddDialog = false; editing = null },
            onSave = { title, target, icon, color ->
                vm.saveGoal(editing?.id, title, target, icon, color)
                showAddDialog = false
                editing = null
            }
        )
    }

    // ---- واریز / برداشت ---------------------------------------------------------
    depositTarget?.let { goal ->
        AmountDialog(
            title = "واریز به «" + goal.title + "»",
            initial = 0L,
            onConfirm = { amount ->
                vm.contribute(goal, amount, deposit = true)
                depositTarget = null
            },
            onDismiss = { depositTarget = null }
        )
    }
    withdrawTarget?.let { goal ->
        AmountDialog(
            title = "برداشت از «" + goal.title + "»",
            initial = 0L,
            onConfirm = { amount ->
                vm.contribute(goal, amount, deposit = false)
                withdrawTarget = null
            },
            onDismiss = { withdrawTarget = null }
        )
    }

    // ---- تأیید حذف --------------------------------------------------------------
    deleteTarget?.let { goal ->
        ConfirmDialog(
            title = "حذف هدف",
            text = "«" + goal.title + "» با همهٔ واریزی‌هایش حذف شود؟ این کار برگشت‌پذیر نیست.",
            confirmLabel = "حذف",
            onConfirm = {
                vm.deleteGoal(goal)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ---------------------------------------------------------------------------
// کارت هدف
// ---------------------------------------------------------------------------

@Composable
private fun GoalCard(
    goal: SavingsGoalEntity,
    progress: GoalProgress,
    onEdit: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = parseGoalColor(goal.color)
    val achieved = progress.achieved

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Presets.iconOf(goal.icon),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (achieved) "هدف محقق شد! 🎉"
                        else Fmt.money(progress.saved) + " از " + Fmt.money(goal.targetAmount) + " تومان",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (achieved) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    Fmt.percent(progress.fraction),
                    style = MaterialTheme.typography.titleSmall,
                    color = accent
                )
            }

            Spacer(Modifier.height(10.dp))
            ProgressBar(
                fraction = progress.fraction,
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            if (!achieved) {
                Spacer(Modifier.height(6.dp))
                Text(
                    Fmt.money(progress.remaining) + " تومان مانده",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDeposit,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "واریز",
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onWithdraw,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Rounded.Remove,
                        contentDescription = "برداشت",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** تبدیل رنگ hex به Color با برگشت امن به رنگ پیش‌فرض برند */
private fun parseGoalColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF2DD4BF)
}

// ---------------------------------------------------------------------------
// دیالوگ افزودن/ویرایش هدف
// ---------------------------------------------------------------------------

@Composable
private fun GoalDialog(
    initial: SavingsGoalEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, target: Long, icon: String, color: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var targetText by remember {
        mutableStateOf(if ((initial?.targetAmount ?: 0L) > 0) Fmt.amountInput(initial!!.targetAmount.toString()) else "")
    }
    var icon by remember { mutableStateOf(initial?.icon ?: "savings") }
    var color by remember { mutableStateOf(initial?.color ?: Presets.PALETTE.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "هدف جدید" else "ویرایش هدف") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("نام هدف") },
                    placeholder = { Text("مثلاً: سفر شمال") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = Fmt.amountInput(it) },
                    label = { Text("مبلغ هدف (تومان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text("آیکون", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val accent = parseGoalColor(color)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GOAL_ICONS.take(4).forEach { key ->
                        IconChoice(icon = key, selected = icon == key, color = accent, onSelect = { icon = key })
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GOAL_ICONS.drop(4).forEach { key ->
                        IconChoice(icon = key, selected = icon == key, color = accent, onSelect = { icon = key })
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("رنگ", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Presets.PALETTE.take(6).forEach { hex ->
                        ColorChoice(hex = hex, selected = color == hex, onSelect = { color = hex })
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Presets.PALETTE.drop(6).forEach { hex ->
                        ColorChoice(hex = hex, selected = color == hex, onSelect = { color = hex })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = Fmt.parseAmount(targetText)
                    if (title.trim().isNotEmpty() && target > 0) {
                        onSave(title.trim(), target, icon, color)
                    }
                },
                enabled = title.trim().isNotEmpty() && Fmt.parseAmount(targetText) > 0
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun IconChoice(icon: String, selected: Boolean, color: Color, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (selected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Presets.iconOf(icon),
            contentDescription = null,
            tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ColorChoice(hex: String, selected: Boolean, onSelect: () -> Unit) {
    val c = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF2DD4BF)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(c)
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
