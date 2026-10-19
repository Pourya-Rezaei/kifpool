package com.kifpool.app.util

import android.content.Context
import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.data.TxType
import com.kifpool.app.logic.Jalali
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * ساخت خروجی CSV سازگار با Excel.
 *
 * امنیت:
 *  - همهٔ فیلدها همیشه داخل " " قرار می‌گیرند (قبل از این فقط توضیحات
 *    escape می‌شد و دسته‌ای با نام حاوی ویرگول، فایل را خراب می‌کرد).
 *  - محافظت CSV Formula Injection: فیلدهایی که با = + - @ TAB یا CR شروع
 *    شوند در Excel به‌عنوان فرمول اجرا می‌شوند؛ با پیشوند ' خنثی می‌شوند.
 */
object Csv {

    fun build(txs: List<TransactionEntity>, cats: List<CategoryEntity>): String {
        val nameById = cats.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("نوع,مبلغ (تومان),دسته‌بندی,تاریخ,توضیحات\n")
        for (tx in txs) {
            val cat = nameById[tx.categoryId]
            val type = if (cat?.type == TxType.INCOME) "درآمد" else "هزینه"
            val j = Jalali.toJalali(Jalali.dateOf(tx.date))
            val dateStr = String.format(Locale.US, "%04d/%02d/%02d", j.year, j.month, j.day)
            val row = listOf(
                type,
                Fmt.moneyLatin(tx.amount),
                cat?.name ?: "بدون دسته",
                dateStr,
                tx.note
            )
            sb.append(row.joinToString(",") { encodeField(it) }).append('\n')
        }
        return sb.toString()
    }

    /**
     * کدگذاری امن یک فیلد CSV: همیشه درون نقل‌قول + جاگذاری " دوتایی
     * + خنثی‌سازی فرمول‌های اکسل (= + - @ Tab CR).
     */
    fun encodeField(raw: String): String {
        val safe = if (raw.isNotEmpty() && raw[0] in "=+-@\t\r") "'$raw" else raw
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    /** نوشتن CSV با BOM تا Excel فارسی را درست نشان دهد. */
    fun write(context: Context, content: String): File {
        val name = FileExport.exportFileName(prefix = "kifpool", ext = "csv")
        return FileExport.writeToExports(context, name, "\uFEFF" + content)
    }

    /** باز کردن صفحهٔ اشتراک برای فایل CSV. */
    fun share(context: Context, file: File) = FileExport.share(
        context, file,
        mime = "text/csv",
        subject = "خروجی تراکنش‌های کیف پول من",
        chooserTitle = "ارسال فایل CSV"
    )
}
