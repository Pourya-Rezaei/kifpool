package com.kifpool.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kifpool.app.logic.Jalali
import com.kifpool.app.util.Fmt
import java.time.LocalDate

/**
 * Compact Jalali (Shamsi) date picker with a month grid and Today shortcut.
 */
@Composable
fun JalaliDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onSelected: (LocalDate) -> Unit
) {
    val initialJ = Jalali.toJalali(initial)
    var jy by remember { mutableIntStateOf(initialJ.year) }
    var jm by remember { mutableIntStateOf(initialJ.month) }
    var selected by remember { mutableStateOf(initial) }

    val length = Jalali.monthLength(jy, jm)
    val offset = Jalali.weekdayIndex(Jalali.toGregorian(jy, jm, 1))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                // month navigation
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (jm == 1) { jm = 12; jy -= 1 } else jm -= 1
                    }) {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "ماه قبل",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        Fmt.fa(Jalali.MONTH_NAMES[jm - 1] + " " + jy),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (jm == 12) { jm = 1; jy += 1 } else jm += 1
                    }) {
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = "ماه بعد",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { wd ->
                        Text(
                            wd,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                val cells: List<Int?> = List(offset) { null } + (1..length).toList()
                cells.chunked(7).forEach { week ->
                    Row {
                        week.forEach { day ->
                            Box(
                                Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val d = Jalali.toGregorian(jy, jm, day)
                                    val isSel = d == selected
                                    val isToday = d == LocalDate.now()
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSel) MaterialTheme.colorScheme.primary
                                                else Color.Transparent,
                                                CircleShape
                                            )
                                            .border(
                                                width = if (isToday && !isSel) 1.dp else 0.dp,
                                                color = if (isToday && !isSel) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                                } else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selected = d },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            Fmt.fa(day),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { selected = LocalDate.now() }) { Text("امروز") }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = { onSelected(selected) }) { Text("تأیید") }
                }
            }
        }
    }
}
