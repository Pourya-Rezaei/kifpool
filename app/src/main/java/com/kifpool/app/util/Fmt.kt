package com.kifpool.app.util

import com.kifpool.app.logic.Jalali
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/** Formatting helpers: Persian digits, money, dates. */
object Fmt {

    private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun fa(s: String): String = buildString {
        for (c in s) append(if (c in '0'..'9') faDigits[c - '0'] else c)
    }

    fun fa(n: Int): String = fa(n.toString())
    fun fa(n: Long): String = fa(n.toString())

    /** Groups digits by 3 with the given separator. */
    private fun group(s: String, sep: Char): String {
        val sb = StringBuilder()
        val len = s.length
        for (i in s.indices) {
            sb.append(s[i])
            val rem = len - i - 1
            if (rem > 0 && rem % 3 == 0) sb.append(sep)
        }
        return sb.toString()
    }

    /** 1250000 -> ۱٬۲۵۰٬۰۰۰ */
    fun money(n: Long): String = fa(group(n.toString(), '٬'))

    /** For CSV files: 1250000 -> 1,250,000 (ASCII comma — Excel-friendly) */
    fun moneyLatin(n: Long): String = group(n.toString(), ',')

    fun percent(f: Float): String = fa("${(f * 100).roundToInt()}٪")

    /** Normalizes Persian/Arabic digits to Latin and strips everything else. */
    fun digitsOnly(raw: String): String = buildString {
        for (c in raw) {
            when (c) {
                in '۰'..'۹' -> append('0' + (c - '۰'))
                in '٠'..'٩' -> append('0' + (c - '٠'))
                in '0'..'9' -> append(c)
            }
        }
    }

    /** Live-formats the amount input field (grouped + Persian digits). */
    fun amountInput(raw: String): String {
        val d = digitsOnly(raw).trimStart('0')
        if (d.isEmpty()) return ""
        val capped = if (d.length > 15) d.substring(0, 15) else d
        return fa(group(capped, '٬'))
    }

    fun parseAmount(display: String): Long = digitsOnly(display).toLongOrNull() ?: 0L

    fun dateFull(date: LocalDate): String {
        val j = Jalali.toJalali(date)
        return fa("${j.day} ${Jalali.MONTH_NAMES[j.month - 1]} ${j.year}")
    }

    fun dateShort(date: LocalDate): String {
        val j = Jalali.toJalali(date)
        return fa("${j.day} ${Jalali.MONTH_NAMES[j.month - 1]}")
    }

    fun dateSlash(date: LocalDate): String {
        val j = Jalali.toJalali(date)
        return fa(String.format(Locale.US, "%04d/%02d/%02d", j.year, j.month, j.day))
    }

    fun monthLabel(key: Jalali.MonthKey): String =
        fa("${Jalali.MONTH_NAMES[key.month - 1]} ${key.year}")
}
