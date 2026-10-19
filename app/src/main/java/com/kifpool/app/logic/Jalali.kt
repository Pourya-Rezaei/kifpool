package com.kifpool.app.logic

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Persian (Jalali/Shamsi) calendar utilities.
 *
 * Core conversion is the well-known "jalaali" algorithm (Behrooz's astrometric
 * table for years 1178-1633 AP), ported from jalaali-js. Day-number helpers use
 * java.time epoch days which are fully reliable.
 */
object Jalali {

    data class Date(val year: Int, val month: Int, val day: Int)

    data class MonthKey(val year: Int, val month: Int) {
        fun next(): MonthKey = if (month == 12) MonthKey(year + 1, 1) else MonthKey(year, month + 1)
        fun previous(): MonthKey = if (month == 1) MonthKey(year - 1, 12) else MonthKey(year, month - 1)
    }

    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private fun div(a: Int, b: Int): Int = a.floorDiv(b)
    private fun mod(a: Int, b: Int): Int = a.mod(b)

    /** Returns [leap, gy, march] for the given Jalali year. */
    private fun jalCal(jy: Int): IntArray {
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        require(jy >= jp && jy < breaks[bl - 1]) { "Invalid Jalali year $jy" }
        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return intArrayOf(leap, gy, march)
    }

    private fun g2d(date: LocalDate): Long = date.toEpochDay()
    private fun d2g(jdn: Long): LocalDate = LocalDate.ofEpochDay(jdn)

    private fun j2d(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy)
        return g2d(LocalDate.of(r[1], 3, r[2])) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Long): Date {
        val gy = d2g(jdn).year
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(LocalDate.of(gy, 3, r[2]))
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return Date(jy, 1 + div(k.toInt(), 31), mod(k.toInt(), 31) + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r[0] == 1) k += 1
        }
        return Date(jy, 7 + div(k.toInt(), 30), mod(k.toInt(), 30) + 1)
    }

    fun isLeap(jy: Int): Boolean = jalCal(jy)[0] == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    fun toJalali(date: LocalDate): Date = d2j(date.toEpochDay())

    fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate = d2g(j2d(jy, jm, jd))

    fun currentMonthKey(today: LocalDate = LocalDate.now()): MonthKey {
        val j = toJalali(today)
        return MonthKey(j.year, j.month)
    }

    fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun dateOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun monthStartMillis(key: MonthKey, zone: ZoneId = ZoneId.systemDefault()): Long =
        startOfDayMillis(toGregorian(key.year, key.month, 1), zone)

    fun monthEndExclusiveMillis(key: MonthKey, zone: ZoneId = ZoneId.systemDefault()): Long =
        startOfDayMillis(toGregorian(key.next().year, key.next().month, 1), zone)

    /** Index of weekday in the Iranian week: Saturday = 0 ... Friday = 6. */
    fun weekdayIndex(date: LocalDate): Int = (date.dayOfWeek.value + 1) % 7

    /** Saturday that starts the (Iranian) week containing [today]. */
    fun weekStart(today: LocalDate = LocalDate.now()): LocalDate =
        today.minusDays(weekdayIndex(today).toLong())
}
