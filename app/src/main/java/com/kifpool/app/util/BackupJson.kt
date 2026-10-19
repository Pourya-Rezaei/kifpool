package com.kifpool.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.ReminderEntity
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.logic.Jalali
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * پشتیبان‌گیری و بازیابی کامل داده‌ها در قالب JSON (کاملاً آفلاین، بدون وابستگی خارجی).
 *
 * ساختار فایل:
 * {
 *   "app": "kifpool", "backupVersion": 1, "exportedAt": "1404/07/27",
 *   "categories": [...], "transactions": [...], "reminders": [...]
 * }
 */
object BackupJson {

    data class Backup(
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val reminders: List<ReminderEntity>
    )

    // ---- export ---------------------------------------------------------------

    fun build(txs: List<TransactionEntity>, cats: List<CategoryEntity>, reminders: List<ReminderEntity>): String {
        val root = JSONObject()
        root.put("app", "kifpool")
        root.put("backupVersion", 1)
        val j = Jalali.toJalali(LocalDate.now())
        root.put("exportedAt", Fmt.fa(j.year.toString() + "/" + j.month + "/" + j.day))

        val catsArr = JSONArray()
        for (c in cats) {
            catsArr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("type", c.type)
                    .put("icon", c.icon)
                    .put("color", c.color)
                    .put("isDefault", c.isDefault)
            )
        }

        val txsArr = JSONArray()
        for (t in txs) {
            txsArr.put(
                JSONObject()
                    .put("amount", t.amount)
                    .put("categoryId", t.categoryId)
                    .put("date", t.date)
                    .put("note", t.note)
            )
        }

        val remArr = JSONArray()
        for (r in reminders) {
            remArr.put(
                JSONObject()
                    .put("title", r.title)
                    .put("amount", r.amount)
                    .put("dayOfMonth", r.dayOfMonth)
                    .put("timeMinutes", r.timeMinutes)
                    .put("enabled", r.enabled)
            )
        }

        root.put("categories", catsArr)
        root.put("transactions", txsArr)
        root.put("reminders", remArr)
        return root.toString(2)
    }

    fun write(context: Context, content: String): File {
        val dir = context.getExternalFilesDir("exports") ?: context.filesDir
        val j = Jalali.toJalali(LocalDate.now())
        val file = File(dir, "kifpool_backup_" + j.year + "_" + j.month + ".json")
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "پشتیبان کامل کیف پول من")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "ارسال فایل پشتیبان").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context, "فایل ذخیره شد: " + file.absolutePath, android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---- import ---------------------------------------------------------------

    /** خواندن متن فایل پشتیبان از Uri انتخاب‌شده توسط کاربر. */
    fun read(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    } catch (e: Exception) {
        null
    }

    /** تجزیه و اعتبارسنجی فایل پشتیبان؛ در خطا null برمی‌گرداند. */
    fun parse(text: String): Backup? {
        return try {
            val root = JSONObject(text)
            if (root.optString("app") != "kifpool") return null

            val cats = mutableListOf<CategoryEntity>()
            val catsArr = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catsArr.length()) {
                val o = catsArr.getJSONObject(i)
                cats.add(
                    CategoryEntity(
                        id = o.optLong("id", 0L),
                        name = o.optString("name"),
                        type = o.optInt("type", 0),
                        icon = o.optString("icon", "wallet"),
                        color = o.optString("color", "#F97316"),
                        isDefault = o.optBoolean("isDefault", false)
                    )
                )
            }

            val txs = mutableListOf<TransactionEntity>()
            val txsArr = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txsArr.length()) {
                val o = txsArr.getJSONObject(i)
                txs.add(
                    TransactionEntity(
                        amount = o.optLong("amount", 0L),
                        categoryId = o.optLong("categoryId", 0L),
                        date = o.optLong("date", 0L),
                        note = o.optString("note", "")
                    )
                )
            }

            val rems = mutableListOf<ReminderEntity>()
            val remArr = root.optJSONArray("reminders") ?: JSONArray()
            for (i in 0 until remArr.length()) {
                val o = remArr.getJSONObject(i)
                rems.add(
                    ReminderEntity(
                        title = o.optString("title"),
                        amount = o.optLong("amount", 0L),
                        dayOfMonth = o.optInt("dayOfMonth", 1).coerceIn(1, 31),
                        timeMinutes = o.optInt("timeMinutes", 8 * 60 + 30),
                        enabled = o.optBoolean("enabled", true)
                    )
                )
            }

            Backup(cats, txs, rems)
        } catch (e: Exception) {
            null
        }
    }
}
