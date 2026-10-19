package com.kifpool.app.util

import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.DebtEntity
import com.kifpool.app.data.DebtType
import com.kifpool.app.data.ReminderEntity
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست پشتیبان JSON: رفت‌وبرگشت کامل، سازگاری رو به عقب با پشتیبان‌های
 * قدیمی، و مقاومت در برابر فایل خراب/مخرب.
 */
class BackupJsonTest {

    // ---- helpers -------------------------------------------------------------

    private fun sampleCategories() = listOf(
        CategoryEntity(id = 1L, name = "خوراک", type = TxType.EXPENSE, icon = "restaurant", color = "#F97316", isDefault = true, budgetAmount = 2_000_000L),
        CategoryEntity(id = 2L, name = "حقوق", type = TxType.INCOME, icon = "work", color = "#2DD4BF")
    )

    private fun sampleTxs() = listOf(
        TransactionEntity(amount = 150_000L, categoryId = 1L, date = 1_700_000_000_000L, note = "ناهار، با «ویرگول» و \"کوتیشن\""),
        TransactionEntity(amount = 20_000_000L, categoryId = 2L, date = 1_700_100_000_000L)
    )

    private fun sampleReminders() = listOf(
        ReminderEntity(title = "اجاره", amount = 8_000_000L, dayOfMonth = 5, timeMinutes = 510, enabled = true)
    )

    private fun sampleDebts() = listOf(
        DebtEntity(person = "علی", amount = 500_000L, type = DebtType.CREDIT, note = "قرض", createdDate = 1_700_000_000_000L, dueDate = 1_710_000_000_000L),
        DebtEntity(person = "رضا", amount = 200_000L, type = DebtType.DEBT, createdDate = 1_700_000_000_000L, settled = true)
    )

    // ---- round trip ----------------------------------------------------------

    @Test
    fun `full round trip preserves everything`() {
        val json = BackupJson.build(sampleTxs(), sampleCategories(), sampleReminders(), sampleDebts())
        val backup = BackupJson.parse(json)
        assertNotNull(backup)

        backup!!
        assertEquals(2, backup.categories.size)
        assertEquals(2, backup.transactions.size)
        assertEquals(1, backup.reminders.size)
        assertEquals(2, backup.debts.size)

        val cat = backup.categories[0]
        assertEquals("خوراک", cat.name)
        assertEquals(2_000_000L, cat.budgetAmount) // بودجهٔ دسته حفظ شود
        assertTrue(cat.isDefault)

        val tx = backup.transactions[0]
        assertEquals(150_000L, tx.amount)
        assertEquals("ناهار، با «ویرگول» و \"کوتیشن\"", tx.note) // متن‌های خاص سالم

        val rem = backup.reminders[0]
        assertEquals("اجاره", rem.title)
        assertEquals(5, rem.dayOfMonth)
        assertEquals(510, rem.timeMinutes)

        val debt = backup.debts[0]
        assertEquals("علی", debt.person)
        assertEquals(DebtType.CREDIT, debt.type)
        assertEquals(1_710_000_000_000L, debt.dueDate)
        assertEquals(false, debt.settled)
    }

    @Test
    fun `old v1 backup without debts key still parses`() {
        // پشتیبان نسخه‌های ≤ ۱٫۵٫۲ — بدون debts و بدون budgetAmount
        val oldJson = """
            {
              "app": "kifpool",
              "backupVersion": 1,
              "categories": [
                {"id": 1, "name": "خوراک", "type": 0, "icon": "restaurant", "color": "#F97316", "isDefault": true}
              ],
              "transactions": [
                {"amount": 250000, "categoryId": 1, "date": 1700000000000, "note": "قبلی"}
              ],
              "reminders": []
            }
        """.trimIndent()
        val backup = BackupJson.parse(oldJson)
        assertNotNull(backup)
        backup!!
        assertEquals(1, backup.categories.size)
        assertEquals(0L, backup.categories[0].budgetAmount) // پیش‌فرض بدون بودجه
        assertEquals(0, backup.debts.size)                  // آرایهٔ غایب => خالی
        assertEquals(1, backup.transactions.size)
    }

    // ---- validation ----------------------------------------------------------

    @Test
    fun `wrong app tag is rejected`() {
        val json = """{"app": "other-app", "categories": [], "transactions": [], "reminders": []}"""
        assertNull(BackupJson.parse(json))
    }

    @Test
    fun `corrupt json is rejected without crashing`() {
        assertNull(BackupJson.parse("not json at all"))
        assertNull(BackupJson.parse(""))
        assertNull(BackupJson.parse("null"))
        assertNull(BackupJson.parse("""{"app": 123}"""))
    }

    @Test
    fun `negative or zero amounts are dropped`() {
        val json = """
            {
              "app": "kifpool",
              "transactions": [
                {"amount": -5, "categoryId": 1, "date": 1000, "note": "منفی"},
                {"amount": 0, "categoryId": 1, "date": 1000},
                {"amount": 700, "categoryId": 1, "date": 1000}
              ],
              "debts": [
                {"person": "x", "amount": -1, "type": 0, "createdDate": 0},
                {"person": "x", "amount": 999, "type": 0, "createdDate": 0}
              ]
            }
        """.trimIndent()
        val backup = BackupJson.parse(json)!!
        assertEquals(1, backup.transactions.size) // فقط مثبت می‌ماند
        assertEquals(700L, backup.transactions[0].amount)
        assertEquals(1, backup.debts.size)
        assertEquals(999L, backup.debts[0].amount)
    }

    @Test
    fun `nameless categories and personless debts are dropped`() {
        val json = """
            {
              "app": "kifpool",
              "categories": [
                {"id": 1, "name": "   ", "type": 0},
                {"id": 2, "name": "معتبر", "type": 0}
              ],
              "debts": [
                {"person": "", "amount": 100, "type": 0, "createdDate": 0},
                {"person": "علی", "amount": 100, "type": 0, "createdDate": 0}
              ]
            }
        """.trimIndent()
        val backup = BackupJson.parse(json)!!
        assertEquals(1, backup.categories.size)
        assertEquals("معتبر", backup.categories[0].name)
        assertEquals(1, backup.debts.size)
        assertEquals("علی", backup.debts[0].person)
    }

    @Test
    fun `oversized strings are bounded`() {
        val longName = "الف".repeat(500)
        val json = """
            {
              "app": "kifpool",
              "categories": [{"id": 1, "name": "$longName", "type": 0}]
            }
        """.trimIndent()
        val backup = BackupJson.parse(json)!!
        assertTrue(backup.categories[0].name.length <= BackupJson.MAX_NAME)
    }

    @Test
    fun `field domains are coerced`() {
        val json = """
            {
              "app": "kifpool",
              "reminders": [
                {"title": "قبض", "dayOfMonth": 99, "timeMinutes": 99999, "enabled": true},
                {"title": "قرض", "dayOfMonth": -3, "timeMinutes": -5}
              ],
              "transactions": [
                {"amount": 500, "categoryId": -10, "date": -99}
              ]
            }
        """.trimIndent()
        val backup = BackupJson.parse(json)!!
        val (a, b) = backup.reminders
        assertEquals(31, a.dayOfMonth)
        assertEquals(24 * 60 - 1, a.timeMinutes)
        assertEquals(1, b.dayOfMonth)
        assertEquals(0, b.timeMinutes)
        val tx = backup.transactions[0]
        assertEquals(0L, tx.categoryId)
        assertEquals(0L, tx.date)
    }

    @Test
    fun `debt type is normalized to known constants`() {
        val json = """
            {
              "app": "kifpool",
              "debts": [
                {"person": "الف", "amount": 100, "type": 7, "createdDate": 0},
                {"person": "ب", "amount": 100, "type": 0, "createdDate": 0}
              ]
            }
        """.trimIndent()
        val backup = BackupJson.parse(json)!!
        assertEquals(DebtType.DEBT, backup.debts[0].type)   // مقدار ناشناخته => بدهی
        assertEquals(DebtType.CREDIT, backup.debts[1].type) // صفر = طلب
    }

    @Test
    fun `build output includes all arrays`() {
        val json = BackupJson.build(sampleTxs(), sampleCategories(), sampleReminders(), sampleDebts())
        assertTrue(json.contains("\"categories\""))
        assertTrue(json.contains("\"transactions\""))
        assertTrue(json.contains("\"reminders\""))
        assertTrue(json.contains("\"debts\""))
        assertTrue(json.contains("\"backupVersion\""))
    }
}
