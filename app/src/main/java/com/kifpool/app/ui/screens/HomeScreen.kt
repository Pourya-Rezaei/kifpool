package com.kifpool.app.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kifpool.app.WalletViewModel
import com.kifpool.app.data.GoalProgress
import com.kifpool.app.logic.RecurringEngine
import com.kifpool.app.ui.components.EmptyState
import com.kifpool.app.ui.components.MiniStat
import com.kifpool.app.ui.components.MonthSwitcher
import com.kifpool.app.ui.components.ProgressBar
import com.kifpool.app.ui.components.SectionTitle
import com.kifpool.app.ui.components.TxRow
import com.kifpool.app.ui.theme.LocalAppColors
import com.kifpool.app.util.Fmt
import com.kifpool.app.util.Presets
import java.time.LocalDate

@Composable
fun HomeScreen(
    vm: WalletViewModel,
    onEdit: (Long) -> Unit,
    onOpenRecurring: () -> Unit = {},
    onOpenGoals: () -> Unit = {}
) {
    val data by vm.data.collectAsState()
    val month by vm.selectedMonth.collectAsState()
    val appColors = LocalAppColors.current
    val recurring by vm.recurring.collectAsState()
    val goals by vm.goals.collectAsState()
    val savedByGoal by vm.savedByGoal.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("کیف پول من", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            MonthSwitcher(
                label = Fmt.monthLabel(month),
                onPrev = vm::prevMonth,
                onNext = vm::nextMonth
            )
        }

        val d = data
        if (d == null) {
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        // ---------- dashboard card ----------
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "هزینه ${Fmt.monthLabel(month)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        Fmt.money(d.monthExpense),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "تومان",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (d.monthlyBudget > 0) {
                    Spacer(Modifier.height(16.dp))
                    val barColor =
                        if (d.budgetExceeded) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    ProgressBar(
                        fraction = d.budgetFraction,
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (d.budgetExceeded) {
                            Fmt.money(-d.budgetRemaining) + " تومان از بودجه عبور کرده‌ای"
                        } else {
                            Fmt.money(d.budgetRemaining) + " تومان از بودجه " + Fmt.money(d.monthlyBudget) + " تومانی باقی مانده"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (d.weeklyGoal > 0) {
                    Spacer(Modifier.height(16.dp))
                    val weekColor =
                        if (d.week.exceeded) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.tertiary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = weekColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "هدف هفته: " + Fmt.money(d.week.spent) + " از " + Fmt.money(d.week.goal) + " تومان",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            Fmt.percent(d.week.fraction),
                            style = MaterialTheme.typography.labelMedium,
                            color = weekColor
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(
                        fraction = d.week.fraction,
                        color = weekColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
        }

        // ---------- mini stats ----------
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(
                title = "درآمد این ماه",
                value = Fmt.money(d.monthIncome),
                modifier = Modifier.weight(1f),
                valueColor = appColors.income
            )
            MiniStat(
                title = "مانده ماه",
                value = Fmt.money(d.balance),
                modifier = Modifier.weight(1f),
                valueColor = if (d.balance >= 0) appColors.income else appColors.expense
            )
            MiniStat(
                title = "میانگین روزانه",
                value = Fmt.money(d.dailyAverage),
                modifier = Modifier.weight(1f)
            )
        }

        // ---------- دستیار مالی: سررسیدهای نزدیک + اهداف پس‌انداز ----------
        val enabledRecs = recurring.filter { it.enabled }
        val upcoming = enabledRecs
            .mapNotNull { rec -> RecurringEngine.upcomingDate(rec, LocalDate.now())?.let { rec to it } }
            .sortedBy { it.second }
            .take(2)
        if (upcoming.isNotEmpty() || goals.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "دستیار مالی",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    upcoming.forEach { (rec, date) ->
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onOpenRecurring() }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                rec.title,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                Fmt.money(rec.amount) + " تومان — " + Fmt.dateShort(date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    goals.take(2).forEach { goal ->
                        val progress = GoalProgress(
                            saved = savedByGoal[goal.id] ?: 0L,
                            target = goal.targetAmount
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onOpenGoals() }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Presets.iconOf(goal.icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                goal.title,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                Fmt.percent(progress.fraction),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        ProgressBar(
                            fraction = progress.fraction,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { if (goals.isNotEmpty()) onOpenGoals() else onOpenRecurring() }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            if (goals.isEmpty()) "مدیریت تکرارشوها"
                            else "اهداف و تکرارشوها",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ---------- category budget alerts ----------
        if (d.budgetAlerts.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "هشدار بودجهٔ دسته‌ها",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    d.budgetAlerts.forEach { alert ->
                        val over = alert.total > alert.category.budgetAmount
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                alert.category.name,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                Fmt.money(alert.total) + " از " + Fmt.money(alert.category.budgetAmount) + " تومان",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (over) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        ProgressBar(
                            fraction = alert.total.toFloat() / alert.category.budgetAmount,
                            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }

        // ---------- recent transactions ----------
        SectionTitle("آخرین تراکنش‌ها")
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp)) {
                if (d.recentTxs.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "هنوز تراکنشی نیست",
                        subtitle = "با دکمه «ثبت تراکنش» اولین هزینه یا درآمد این ماه را اضافه کن"
                    )
                } else {
                    val catById = d.categories.associateBy { it.id }
                    d.recentTxs.forEach { tx ->
                        TxRow(tx, catById[tx.categoryId]) { onEdit(tx.id) }
                    }
                    if (d.monthTxs.size > d.recentTxs.size) {
                        Text(
                            "+" + Fmt.fa(d.monthTxs.size - d.recentTxs.size) + " تراکنش دیگر در این ماه",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}
