package com.kifpool.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kifpool.app.WalletViewModel
import com.kifpool.app.ui.components.BarChart
import com.kifpool.app.ui.components.CategoryAvatar
import com.kifpool.app.ui.components.DonutChart
import com.kifpool.app.ui.components.DonutSlice
import com.kifpool.app.ui.components.EmptyState
import com.kifpool.app.ui.components.MiniStat
import com.kifpool.app.ui.components.MonthSwitcher
import com.kifpool.app.ui.components.SectionTitle
import com.kifpool.app.ui.theme.LocalAppColors
import com.kifpool.app.util.Fmt
import com.kifpool.app.util.Presets
import androidx.compose.ui.graphics.Color

@Composable
fun StatsScreen(vm: WalletViewModel) {
    val data by vm.data.collectAsState()
    val month by vm.selectedMonth.collectAsState()
    val appColors = LocalAppColors.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("آمار", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            MonthSwitcher(
                label = Fmt.monthLabel(month),
                onPrev = vm::prevMonth,
                onNext = vm::nextMonth
            )
        }

        val d = data ?: run {
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        // ---------- summary ----------
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(
                title = "هزینه",
                value = Fmt.money(d.monthExpense),
                modifier = Modifier.weight(1f),
                valueColor = appColors.expense
            )
            MiniStat(
                title = "درآمد",
                value = Fmt.money(d.monthIncome),
                modifier = Modifier.weight(1f),
                valueColor = appColors.income
            )
            MiniStat(
                title = "مانده",
                value = Fmt.money(d.balance),
                modifier = Modifier.weight(1f),
                valueColor = if (d.balance >= 0) appColors.income else appColors.expense
            )
        }

        // ---------- monthly comparison ----------
        SectionTitle("مقایسه با ماه گذشته")
        MonthComparisonCard(d = d, appColors = appColors)

        // ---------- donut ----------
        SectionTitle("سهم دسته‌بندی‌ها (هزینه)")
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                if (d.categoryStats.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Insights,
                        title = "داده‌ای برای این ماه نیست",
                        subtitle = "وقتی هزینه ثبت کنی، نمودار دایره‌ای دسته‌بندی‌ها اینجا نمایش داده می‌شود"
                    )
                } else {
                    val total = d.categoryStats.sumOf { it.total }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            slices = d.categoryStats.map {
                                DonutSlice(Color(Presets.colorOf(it.category.color)), it.total.toFloat())
                            },
                            modifier = Modifier
                                .size(150.dp)
                                .aspectRatio(1f),
                            centerContent = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "کل هزینه",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        Fmt.money(total),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            d.categoryStats.take(5).forEach { stat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .background(
                                                Color(Presets.colorOf(stat.category.color)),
                                                CircleShape
                                            )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stat.category.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Text(
                                        Fmt.percent(stat.fraction),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (d.categoryStats.size > 5) {
                                Text(
                                    "و " + Fmt.fa(d.categoryStats.size - 5) + " دسته‌بندی دیگر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // detailed rows
                    d.categoryStats.forEach { stat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            CategoryAvatar(stat.category, size = 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stat.category.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    Fmt.percent(stat.fraction) + " از کل هزینه‌ها",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                Fmt.money(stat.total),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // ---------- daily bars ----------
        SectionTitle("روند روزانه (هزینه)")
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                BarChart(
                    values = d.dailyExpense,
                    barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    highlightColor = MaterialTheme.colorScheme.primary,
                    gridColor = MaterialTheme.colorScheme.outlineVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun MonthComparisonCard(
    d: com.kifpool.app.data.AppData,
    appColors: com.kifpool.app.ui.theme.AppColors
) {
    val (curIncome, curExpense) = remember(d) { d.totalsFor(d.month) }
    val (prevIncome, prevExpense) = remember(d) { d.totalsFor(d.month.previous()) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            ComparisonRow(
                title = "هزینه",
                current = curExpense,
                previous = prevExpense,
                valueColor = appColors.expense,
                lessIsBetter = true
            )
            Spacer(Modifier.height(12.dp))
            ComparisonRow(
                title = "درآمد",
                current = curIncome,
                previous = prevIncome,
                valueColor = appColors.income,
                lessIsBetter = false
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    title: String,
    current: Long,
    previous: Long,
    valueColor: androidx.compose.ui.graphics.Color,
    lessIsBetter: Boolean
) {
    val diff = current - previous
    val percent = if (previous > 0) diff.toFloat() / previous else null
    val increased = diff > 0
    val unchanged = diff == 0L

    // رنگ تغییر: برای هزینه کاهش خوب است، برای درآمد افزایش خوب است
    val goodChange = if (lessIsBetter) diff < 0 else diff > 0
    val trendColor = when {
        unchanged -> MaterialTheme.colorScheme.onSurfaceVariant
        goodChange -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    val trendIcon = when {
        unchanged -> Icons.Rounded.TrendingFlat
        increased -> Icons.Rounded.TrendingUp
        else -> Icons.Rounded.TrendingDown
    }
    val trendText = when {
        unchanged -> "بدون تغییر"
        else -> {
            val sign = if (increased) "+" else "−"
            sign + (percent?.let { Fmt.percent(kotlin.math.abs(it)) } ?: "—")
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Fmt.money(current) + " تومان",
                style = MaterialTheme.typography.titleSmall,
                color = valueColor
            )
            Text(
                "ماه گذشته: " + Fmt.money(previous) + " تومان",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(92.dp)) {
            Icon(
                trendIcon,
                contentDescription = null,
                tint = trendColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                trendText,
                style = MaterialTheme.typography.labelSmall,
                color = trendColor,
                maxLines = 2
            )
        }
    }
}
