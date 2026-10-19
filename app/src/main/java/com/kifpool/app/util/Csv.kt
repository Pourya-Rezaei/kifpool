package com.kifpool.app.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.data.TxType
import com.kifpool.app.logic.Jalali
import java.io.File
import java.time.LocalDate
import java.util.Locale

object Csv {

    /** Builds a comma-separated CSV (Excel-friendly, Persian headers). */
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
                cat?.name ?: "-",
                dateStr,
                escape(tx.note)
            )
            sb.append(row.joinToString(",")).append('\n')
        }
        return sb.toString()
    }

    private fun escape(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s

    /** Writes the CSV (UTF-8 with BOM so Excel shows Persian correctly). */
    fun write(context: Context, content: String): File {
        val dir = context.getExternalFilesDir("exports") ?: context.filesDir
        val j = Jalali.toJalali(LocalDate.now())
        val file = File(dir, "kifpool_" + j.year + "_" + j.month + ".csv")
        file.writeText("\uFEFF" + content, Charsets.UTF_8)
        return file
    }

    /** Opens the Android share sheet for the CSV file. */
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "خروجی تراکنش‌های کیف پول من")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "ارسال فایل CSV").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "فایل ذخیره شد: " + file.absolutePath, Toast.LENGTH_LONG).show()
        }
    }
}
