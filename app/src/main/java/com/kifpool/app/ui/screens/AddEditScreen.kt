package com.kifpool.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kifpool.app.WalletViewModel
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.data.TxType
import com.kifpool.app.logic.Jalali
import com.kifpool.app.ui.components.ConfirmDialog
import com.kifpool.app.ui.theme.LocalAppColors
import com.kifpool.app.util.Fmt
import com.kifpool.app.util.Presets
import java.time.LocalDate

@Composable
fun AddEditScreen(vm: WalletViewModel, txId: Long?, onBack: () -> Unit) {
    val categories by vm.categories.collectAsState()
    val appColors = LocalAppColors.current

    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var loadedTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var ready by remember { mutableStateOf(txId == null) }

    LaunchedEffect(txId) {
        if (txId != null) {
            val tx = vm.getTransaction(txId)
            if (tx != null) {
                loadedTx = tx
                amount = Fmt.amountInput(tx.amount.toString())
                note = tx.note
                date = Jalali.dateOf(tx.date)
                categoryId = tx.categoryId
            }
            ready = true
        }
    }
    LaunchedEffect(categories, loadedTx) {
        val tx = loadedTx
        if (tx != null) {
            categories.find { it.id == tx.categoryId }?.let { type = it.type }
        }
    }

    val cats = categories.filter { it.type == type }
    val valid = Fmt.parseAmount(amount) > 0 && categoryId != null

    Column(Modifier.fillMaxSize()) {
        // ---------- header ----------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                if (txId == null) "ثبت تراکنش" else "ویرایش تراکنش",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.weight(1f))
            if (txId != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "حذف تراکنش",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---------- type toggle ----------
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeChip(
                    label = "هزینه",
                    selected = type == TxType.EXPENSE,
                    selectedColor = appColors.expense,
                    modifier = Modifier.weight(1f)
                ) {
                    type = TxType.EXPENSE
                    categoryId = null
                }
                TypeChip(
                    label = "درآمد",
                    selected = type == TxType.INCOME,
                    selectedColor = appColors.income,
                    modifier = Modifier.weight(1f)
                ) {
                    type = TxType.INCOME
                    categoryId = null
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("مبلغ (تومان)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = Fmt.amountInput(it) },
                placeholder = { Text("۰", style = MaterialTheme.typography.headlineSmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Start),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            cats.chunked(4).forEach { rowCats ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowCats.forEach { cat ->
                        CategoryCell(
                            cat = cat,
                            selected = categoryId == cat.id,
                            modifier = Modifier.weight(1f)
                        ) { categoryId = cat.id }
                    }
                    repeat(4 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))
            Text("تاریخ", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(Fmt.dateFull(date), style = MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("یادداشت (اختیاری)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(26.dp))
            Button(
                onClick = {
                    val cat = categoryId ?: return@Button
                    vm.saveTransaction(
                        id = txId,
                        amount = Fmt.parseAmount(amount),
                        categoryId = cat,
                        dateMillis = Jalali.startOfDayMillis(date),
                        note = note.trim()
                    )
                    onBack()
                },
                enabled = valid && ready,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("ذخیره", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showPicker) {
        JalaliDatePickerDialog(
            initial = date,
            onDismiss = { showPicker = false },
            onSelected = {
                date = it
                showPicker = false
            }
        )
    }

    val txToDelete = loadedTx
    if (confirmDelete && txToDelete != null) {
        ConfirmDialog(
            title = "حذف تراکنش",
            text = "این تراکنش برای همیشه حذف می‌شود. مطمئنی؟",
            confirmLabel = "حذف",
            onConfirm = {
                confirmDelete = false
                vm.deleteTransaction(txToDelete)
                onBack()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) {
                if (selectedColor.luminance() > 0.5f) MaterialTheme.colorScheme.onBackground
                else Color.White
            } else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun CategoryCell(
    cat: com.kifpool.app.data.CategoryEntity,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            com.kifpool.app.ui.components.CategoryAvatar(cat = cat, size = 36.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                cat.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}
