package com.kifpool.app.util

import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست CSV: escape کامل فیلدها + خنثی‌سازی فرمول‌های اکسل. */
class CsvTest {

    private fun cat(id: Long, name: String, type: Int) =
        CategoryEntity(id = id, name = name, type = type, icon = "other", color = "#000000")

    @Test
    fun `every field is always quoted`() {
        val csv = Csv.build(
            listOf(TransactionEntity(amount = 10_000L, categoryId = 1L, date = 0L, note = "ساده")),
            listOf(cat(1L, "خوراک", TxType.EXPENSE))
        )
        val lines = csv.trim().split('\n')
        assertEquals(2, lines.size)
        assertTrue("خط داده باید کاملاً نقل‌قولی باشد: $lines", lines[1].startsWith("\""))
        assertTrue(lines[1].endsWith("\""))
        assertEquals("۵ فیلد باید داشته باشیم", 5, lines[1].split("\",\"").size)
    }

    @Test
    fun `comma quote and newline in note are escaped`() {
        val tricky = "الف,ب \"کوتیشن\"\nخط بعدی"
        val csv = Csv.build(
            listOf(TransactionEntity(amount = 5_000L, categoryId = 1L, date = 0L, note = tricky)),
            listOf(cat(1L, "خوراک", TxType.EXPENSE))
        )
        // نیولاین داخل نقل‌قول می‌ماند (استاندارد RFC 4180) و " به "" دوگان می‌شود
        assertTrue(
            csv.contains("\"الف,ب \"\"کوتیشن\"\"\nخط بعدی\"")
        )
        // رکورد بعد از header دقیقاً یک رکورد است — پارس RFC-سازگار یک ردیف می‌دهد
        val fields = parseRfc4180(csv)
        assertEquals(2, fields.size) // header + 1 ردیف
        assertEquals(tricky, fields[1][4]) // متن سالم برگشت
    }

    /** پارس‌کنندهٔ سادهٔ RFC 4180 برای اعتبارسنجی خروجی CSV خودمان. */
    private fun parseRfc4180(text: String): List<List<String>> {
        val rows = mutableListOf(mutableListOf(""))
        var i = 0
        var inQuotes = false
        while (i < text.length) {
            val c = text[i]
            val row = rows.last()
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    row[row.size - 1] += '"'; i += 2
                }
                c == '"' -> { inQuotes = !inQuotes; i++ }
                !inQuotes && c == ',' -> { row.add(""); i++ }
                !inQuotes && c == '\n' -> { if (i + 1 < text.length) rows.add(mutableListOf("")); i++ }
                else -> { row[row.size - 1] += c; i++ }
            }
        }
        return rows
    }

    @Test
    fun `category name with comma no longer breaks the file`() {
        // باگ نسخه‌های قبل: فقط note escape می‌شد
        val csv = Csv.build(
            listOf(TransactionEntity(amount = 7_000L, categoryId = 1L, date = 0L)),
            listOf(cat(1L, "خانه, ماشین", TxType.EXPENSE))
        )
        val fields = parseRfc4180(csv)
        assertEquals(2, fields.size)
        assertEquals("خانه, ماشین", fields[1][2])
    }

    @Test
    fun `formula injection is neutralized`() {
        for (prefix in listOf("=", "+", "-", "@")) {
            val note = prefix + "HYPERLINK(\"http://evil\")"
            val csv = Csv.build(
                listOf(TransactionEntity(amount = 1_000L, categoryId = 1L, date = 0L, note = note)),
                listOf(cat(1L, "خوراک", TxType.EXPENSE))
            )
            val dataLine = csv.trim().split('\n')[1]
            // فیلد یادداشت باید با آپستروف خنثی‌ساز شروع شود تا اکسل فرمول نسازد
            assertTrue(
                "پیشوند $prefix باید خنثی شود — خط: $dataLine",
                dataLine.contains("\"'" + prefix)
            )
        }
    }

    @Test
    fun `persian headers are present`() {
        val csv = Csv.build(emptyList(), emptyList())
        assertEquals("نوع,مبلغ (تومان),دسته‌بندی,تاریخ,توضیحات", csv.trim())
    }

    @Test
    fun `unknown category renders dash`() {
        val csv = Csv.build(
            listOf(TransactionEntity(amount = 3_000L, categoryId = 99L, date = 0L)),
            emptyList()
        )
        assertTrue(csv.contains("بدون دسته"))
    }

    @Test
    fun `encodeField double escaping works standalone`() {
        assertEquals("\"a\"\"b\"", Csv.encodeField("a\"b"))
        assertEquals("\"plain\"", Csv.encodeField("plain"))
        assertFalse(Csv.encodeField("=cmd").startsWith("\"=")) // خنثی‌سازی شده
    }
}
