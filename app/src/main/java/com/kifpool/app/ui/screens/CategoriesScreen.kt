package com.kifpool.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kifpool.app.WalletViewModel
import com.kifpool.app.data.AppData
import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.TxType
import com.kifpool.app.ui.components.CategoryAvatar
import com.kifpool.app.ui.components.ConfirmDialog
import com.kifpool.app.ui.components.ProgressBar
import com.kifpool.app.ui.components.SectionTitle
import com.kifpool.app.util.Fmt
import com.kifpool.app.util.Presets
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(vm: WalletViewModel) {
    val categories by vm.categories.collectAsState()
    val data by vm.data.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var budgetFor by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("دسته‌بندی‌ها", style = MaterialTheme.typography.headlineSmall)
        Text(
            "با آیکون «بودجه» می‌توانی برای هر دستهٔ هزینه سقف ماهانه تعیین کنی",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ---------- add button ----------
        Spacer(Modifier.height(16.dp))
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
                    "دسته‌بندی جدید",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        val expenses = categories.filter { it.type == TxType.EXPENSE }
        val incomes = categories.filter { it.type == TxType.INCOME }

        SectionTitle("هزینه‌ها (" + Fmt.fa(expenses.size) + ")")
        CategoryCard(expenses, data, onBudget = { budgetFor = it }) { toDelete = it }

        SectionTitle("درآمدها (" + Fmt.fa(incomes.size) + ")")
        CategoryCard(incomes, null, onBudget = null) { toDelete = it }

        Spacer(Modifier.height(96.dp))
    }

    if (showAdd) {
        CategoryDialog(
            onDismiss = { showAdd = false },
            onSave = { name, type, icon, color ->
                vm.addCategory(name, type, icon, color)
                showAdd = false
            }
        )
    }

    budgetFor?.let { cat ->
        BudgetDialog(
            category = cat,
            onDismiss = { budgetFor = null },
            onSave = { amount ->
                vm.setCategoryBudget(cat, amount)
                budgetFor = null
            },
            onRemove = {
                vm.setCategoryBudget(cat, 0)
                budgetFor = null
            }
        )
    }

    toDelete?.let { cat ->
        ConfirmDialog(
            title = "حذف دسته‌بندی",
            text = "«" + cat.name + "» حذف شود؟ این کار فقط وقتی ممکن است که دسته‌بندی تراکنشی نداشته باشد.",
            confirmLabel = "حذف",
            onConfirm = {
                toDelete = null
                scope.launch {
                    val ok = vm.deleteCategory(cat)
                    if (!ok) {
                        Toast.makeText(
                            context,
                            "این دسته‌بندی تراکنش دارد و قابل حذف نیست",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun CategoryCard(
    cats: List<CategoryEntity>,
    data: AppData?,
    onBudget: ((CategoryEntity) -> Unit)?,
    onDelete: (CategoryEntity) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            if (cats.isEmpty()) {
                Text(
                    "موردی نیست",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
            cats.forEach { cat ->
                val spent = data?.spentInCategory(cat.id) ?: 0L
                val hasBudget = cat.budgetAmount > 0
                val overBudget = hasBudget && spent > cat.budgetAmount
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(cat)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                cat.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            if (hasBudget) {
                                Text(
                                    text = Fmt.money(spent) + " از " + Fmt.money(cat.budgetAmount) + " تومان",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (overBudget) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (onBudget != null) {
                            IconButton(onClick = { onBudget(cat) }) {
                                Icon(
                                    Icons.Rounded.Savings,
                                    contentDescription = "بودجه " + cat.name,
                                    tint = if (hasBudget) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (cat.isDefault) {
                            Text(
                                "پیش‌فرض",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(onClick = { onDelete(cat) }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "حذف " + cat.name,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (hasBudget) {
                        Spacer(Modifier.height(2.dp))
                        val fraction = spent.toFloat() / cat.budgetAmount
                        ProgressBar(
                            fraction = fraction,
                            color = when {
                                overBudget -> MaterialTheme.colorScheme.error
                                fraction >= 0.8f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.padding(start = 54.dp, end = 8.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var iconKey by remember { mutableStateOf("restaurant") }
    var colorHex by remember { mutableStateOf(Presets.PALETTE.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("دسته‌بندی جدید", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دسته‌بندی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypeToggle("هزینه", TxType.EXPENSE, type, Modifier.weight(1f)) { type = it }
                    TypeToggle("درآمد", TxType.INCOME, type, Modifier.weight(1f)) { type = it }
                }

                Spacer(Modifier.height(14.dp))
                Text("آیکون", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                val iconKeys = Presets.CATEGORY_ICONS.keys.toList()
                iconKeys.chunked(6).forEach { rowKeys ->
                    Row {
                        rowKeys.forEach { key ->
                            IconCell(
                                icon = Presets.iconOf(key),
                                selected = iconKey == key,
                                modifier = Modifier.weight(1f)
                            ) { iconKey = key }
                        }
                        repeat(6 - rowKeys.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("رنگ", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Presets.PALETTE.chunked(6).forEach { rowColors ->
                    Row {
                        rowColors.forEach { hex ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                ColorDot(hex, colorHex == hex) { colorHex = hex }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) onSave(name, type, iconKey, colorHex)
                        }
                    ) { Text("افزودن") }
                }
            }
        }
    }
}

@Composable
private fun TypeToggle(
    label: String,
    value: Int,
    current: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val selected = value == current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.clickable { onSelect(value) }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** دیالوگ تعیین بودجهٔ ماهانهٔ یک دستهٔ هزینه. */
@Composable
private fun BudgetDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    onRemove: () -> Unit
) {
    var text by remember { mutableStateOf(Fmt.amountInput(category.budgetAmount.toString())) }
    val amount = Fmt.parseAmount(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بودجهٔ ماهانه «" + category.name + "»") },
        text = {
            Column {
                Text(
                    "سقف هزینهٔ این دسته در هر ماه شمسی",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = Fmt.amountInput(it) },
                    label = { Text("بودجه (تومان)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "وقتی مصرف از ۸۰٪ بودجه رد شود، در خانه هشدار می‌بینی",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount > 0,
                onClick = { onSave(amount) }
            ) { Text("ذخیره") }
        },
        dismissButton = {
            Row {
                if (category.budgetAmount > 0) {
                    TextButton(onClick = onRemove) {
                        Text("حذف بودجه", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )
}

@Composable
private fun IconCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer
                )
                .border(
                    BorderStroke(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(5.dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(Presets.colorOf(hex)))
            .border(
                BorderStroke(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                ),
                CircleShape
            )
            .clickable(onClick = onClick)
    )
}
