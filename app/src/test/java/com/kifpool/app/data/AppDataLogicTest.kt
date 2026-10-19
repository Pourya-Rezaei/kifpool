package com.kifpool.app.data

import com.kifpool.app.logic.Jalali
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * تست منطق مالی نسخهٔ ۱٫۶: هشدار بودجهٔ دسته‌بندی (آستانهٔ ۸۰٪)
 * و جمع طلب/بدهی دفتر حساب. همهٔ داده‌ها با تاریخ قطعی ساخته می‌شوند.
 */
class AppDataLogicTest {

    private val month = Jalali.MonthKey(1403, 3) // خرداد ۱۴۰۳
    private val today: LocalDate = Jalali.toGregorian(1403, 3, 15) // ۱۵ خرداد

    private fun dayMillis(jDay: Int): Long =
        Jalali.startOfDayMillis(Jalali.toGregorian(month.year, month.month, jDay))

    private fun cat(id: Long, name: String, type: Int = TxType.EXPENSE, budget: Long = 0L) =
        CategoryEntity(id = id, name = name, type = type, icon = "other", color = "#000000", budgetAmount = budget)

    private fun tx(id: Long, categoryId: Long, amount: Long, day: Int) =
        TransactionEntity(id = id, amount = amount, categoryId = categoryId, date = dayMillis(day))

    // ---- بودجهٔ دسته‌بندی ------------------------------------------------------

    @Test
    fun `spentInCategory sums only that category in the month`() {
        val data = AppData(
            transactions = listOf(
                tx(1, 1, 100_000, 5),
                tx(2, 1, 50_000, 7),
                tx(3, 2, 999_000, 6)
            ),
            categories = listOf(cat(1, "خوراک"), cat(2, "پوشاک")),
            month = month, monthlyBudget = 0, weeklyGoal = 0, today = today
        )
        assertEquals(150_000L, data.spentInCategory(1))
        assertEquals(999_000L, data.spentInCategory(2))
        assertEquals(0L, data.spentInCategory(3))
    }

    @Test
    fun `budget alert fires at and above 80 percent`() {
        val data = AppData(
            transactions = listOf(tx(1, 1, 800_000, 5)),
            categories = listOf(cat(1, "خوراک", budget = 1_000_000)),
            month = month, monthlyBudget = 0, weeklyGoal = 0, today = today
        )
        val alerts = data.budgetAlerts
        assertEquals(1, alerts.size)
        assertEquals("خوراک", alerts[0].category.name)
    }

    @Test
    fun `below 80 percent there is no alert`() {
        val data = AppData(
            transactions = listOf(tx(1, 1, 799_999, 5)),
            categories = listOf(cat(1, "خوراک", budget = 1_000_000)),
            month = month, monthlyBudget = 0, weeklyGoal = 0, today = today
        )
        assertTrue(data.budgetAlerts.isEmpty())
    }

    @Test
    fun `categories without budget never alert`() {
        val data = AppData(
            transactions = listOf(tx(1, 1, 5_000_000, 5)),
            categories = listOf(cat(1, "خوراک", budget = 0)),
            month = month, monthlyBudget = 0, weeklyGoal = 0, today = today
        )
        assertTrue("بودجهٔ صفر یعنی بدون بودجه", data.budgetAlerts.isEmpty())
    }

    @Test
    fun `transactions outside the month do not count`() {
        val lastDayPrev = dayMillis(1) - 1 // آخرین روز ماه قبل
        val data = AppData(
            transactions = listOf(
                TransactionEntity(id = 1, amount = 900_000, categoryId = 1, date = lastDayPrev)
            ),
            categories = listOf(cat(1, "خوراک", budget = 1_000_000)),
            month = month, monthlyBudget = 0, weeklyGoal = 0, today = today
        )
        assertTrue(data.budgetAlerts.isEmpty())
        assertEquals(0L, data.spentInCategory(1))
    }

    // ---- دفتر طلب و بدهی -------------------------------------------------------

    @Test
    fun `debtTotals separates open credit and debt`() {
        val debts = listOf(
            DebtEntity(id = 1, person = "علی", amount = 500_000, type = DebtType.CREDIT, createdDate = 0),
            DebtEntity(id = 2, person = "رضا", amount = 300_000, type = DebtType.DEBT, createdDate = 0),
            DebtEntity(id = 3, person = "حسن", amount = 700_000, type = DebtType.CREDIT, createdDate = 0, settled = true)
        )
        val (credit, debt) = AppData(emptyList(), emptyList(), month, 0, 0, today).debtTotals(debts)
        assertEquals(500_000L, credit) // تسویه‌شده حساب نمی‌شود
        assertEquals(300_000L, debt)
    }

    @Test
    fun `debtTotals of settled everything is zero`() {
        val debts = listOf(
            DebtEntity(id = 1, person = "علی", amount = 900_000, type = DebtType.DEBT, createdDate = 0, settled = true)
        )
        val (credit, debt) = AppData(emptyList(), emptyList(), month, 0, 0, today).debtTotals(debts)
        assertEquals(0L, credit)
        assertEquals(0L, debt)
    }

    @Test
    fun `monthly totals for arbitrary month key`() {
        val data = AppData(
            transactions = listOf(
                tx(1, 1, 100_000, 3),
                TransactionEntity(id = 2, amount = 400_000, categoryId = 2, date = dayMillis(1) - 1) // ماه قبل
            ),
            categories = listOf(cat(1, "خوراک"), cat(2, "حقوق", type = TxType.INCOME)),
            month = month, monthlyBudget = 0, weeklyGoal = 0, today = today
        )
        val (income, expense) = data.totalsFor(month)
        assertEquals(0L, income) // حقوق در ماه قبل است
        assertEquals(100_000L, expense)
    }

    @Test
    fun `budget exceeded flags correctly`() {
        val data = AppData(
            transactions = listOf(tx(1, 1, 1_200_000, 3)),
            categories = listOf(cat(1, "خوراک")),
            month = month, monthlyBudget = 1_000_000, weeklyGoal = 0, today = today
        )
        assertTrue(data.budgetExceeded)
        assertEquals(-200_000L, data.budgetRemaining)
        assertFalse(AppData(emptyList(), emptyList(), month, 0, 0, today).budgetExceeded)
    }
}
