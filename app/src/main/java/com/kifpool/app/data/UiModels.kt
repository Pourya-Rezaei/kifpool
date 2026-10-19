package com.kifpool.app.data

import com.kifpool.app.logic.Jalali
import java.time.LocalDate

data class CategoryStat(
    val category: CategoryEntity,
    val total: Long,
    val fraction: Float
)

data class WeekGoal(
    val spent: Long,
    val goal: Long
) {
    val fraction: Float
        get() = if (goal > 0) (spent.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val exceeded: Boolean
        get() = goal > 0 && spent > goal
}

/** Everything the UI needs, derived once per emission. */
data class AppData(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val month: Jalali.MonthKey,
    val monthlyBudget: Long,
    val weeklyGoal: Long,
    val today: LocalDate
) {
    private val catById = categories.associateBy { it.id }

    val monthTxs: List<TransactionEntity> by lazy {
        val start = Jalali.monthStartMillis(month)
        val end = Jalali.monthEndExclusiveMillis(month)
        transactions.filter { it.date in start until end }
    }

    val monthExpense: Long by lazy { sumByType(TxType.EXPENSE) }
    val monthIncome: Long by lazy { sumByType(TxType.INCOME) }
    val balance: Long get() = monthIncome - monthExpense

    private fun sumByType(type: Int): Long = monthTxs.sumOf { tx ->
        if (catById[tx.categoryId]?.type == type) tx.amount else 0L
    }

    /** Expense totals per category for the selected month, sorted desc. */
    val categoryStats: List<CategoryStat> by lazy {
        val expenseTxs = monthTxs.filter { catById[it.categoryId]?.type == TxType.EXPENSE }
        val byCat = expenseTxs.groupBy { it.categoryId }.mapNotNull { (id, list) ->
            val cat = catById[id] ?: return@mapNotNull null
            CategoryStat(cat, list.sumOf { it.amount }, 0f)
        }.sortedByDescending { it.total }
        val grand = byCat.sumOf { it.total }
        if (grand > 0) byCat.map { it.copy(fraction = it.total.toFloat() / grand) } else byCat
    }

    /** Daily expense totals for every day of the selected Jalali month. */
    val dailyExpense: List<Float> by lazy {
        val len = Jalali.monthLength(month.year, month.month)
        val arr = FloatArray(len)
        for (tx in monthTxs) {
            if (catById[tx.categoryId]?.type != TxType.EXPENSE) continue
            val j = Jalali.toJalali(Jalali.dateOf(tx.date))
            if (j.year == month.year && j.month == month.month && j.day in 1..len) {
                arr[j.day - 1] += tx.amount
            }
        }
        arr.toList()
    }

    val budgetFraction: Float
        get() = if (monthlyBudget > 0) (monthExpense.toFloat() / monthlyBudget).coerceIn(0f, 1f) else 0f
    val budgetExceeded: Boolean
        get() = monthlyBudget > 0 && monthExpense > monthlyBudget
    val budgetRemaining: Long
        get() = monthlyBudget - monthExpense

    /** Daily spending average (only meaningful when viewing the current month). */
    val dailyAverage: Long by lazy {
        val days = if (month == Jalali.currentMonthKey(today)) {
            Jalali.toJalali(today).day
        } else {
            Jalali.monthLength(month.year, month.month)
        }
        if (days > 0) monthExpense / days else 0L
    }

    /** Spending of the current Iranian week (Saturday..today). */
    val week: WeekGoal by lazy {
        val start = Jalali.weekStart(today)
        val s = Jalali.startOfDayMillis(start)
        val e = Jalali.startOfDayMillis(start.plusDays(7))
        val spent = transactions
            .filter { it.date in s until e }
            .sumOf { tx -> if (catById[tx.categoryId]?.type == TxType.EXPENSE) tx.amount else 0L }
        WeekGoal(spent, weeklyGoal)
    }

    val recentTxs: List<TransactionEntity> get() = monthTxs.take(8)

    // ---- بودجهٔ دسته‌بندی‌ها (نسخهٔ ۱٫۶) ------------------------------------

    /** هزینه‌کرد ماهِ انتخاب‌شده برای یک دستهٔ خاص. */
    fun spentInCategory(categoryId: Long): Long =
        monthTxs.filter { it.categoryId == categoryId }.sumOf { it.amount }

    /**
     * دسته‌هایی که بودجه دارند و باید هشدار بدهند:
     * مصرف از ۸۰٪ گذشته باشد (حتی اگر بودجه هنوز تمام نشده).
     */
    val budgetAlerts: List<CategoryStat> by lazy {
        categoryStats.filter { stat ->
            val budget = stat.category.budgetAmount
            budget > 0 && stat.total >= budget * 80 / 100
        }
    }

    /** جمع بدهی‌ها/طلب‌های تسویه‌نشده از دفتر حساب. */
    fun debtTotals(debts: List<DebtEntity>): Pair<Long, Long> {
        val open = debts.filter { !it.settled }
        val credit = open.filter { it.type == DebtType.CREDIT }.sumOf { it.amount }
        val debt = open.filter { it.type == DebtType.DEBT }.sumOf { it.amount }
        return credit to debt
    }

    /** جمع درآمد و هزینه هر ماه شمسی دلخواه — برای مقایسه ماهانه. */
    fun totalsFor(key: Jalali.MonthKey): Pair<Long, Long> {
        val start = Jalali.monthStartMillis(key)
        val end = Jalali.monthEndExclusiveMillis(key)
        var income = 0L
        var expense = 0L
        for (tx in transactions) {
            if (tx.date !in start until end) continue
            when (catById[tx.categoryId]?.type) {
                TxType.INCOME -> income += tx.amount
                TxType.EXPENSE -> expense += tx.amount
            }
        }
        return income to expense
    }
}
