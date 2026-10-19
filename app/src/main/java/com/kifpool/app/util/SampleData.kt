package com.kifpool.app.util

import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.logic.Jalali
import java.time.LocalDate

/** Deterministic realistic sample data (no Random) so screenshots look consistent. */
object SampleData {

    private val CURRENT_MONTH: List<Pair<Int, Triple<String, Long, String>>> = listOf(
        1 to Triple("خوراک", 420_000L, "سوپرمارکت محله"),
        1 to Triple("قبض‌ها", 1_180_000L, "قبض برق"),
        2 to Triple("حمل‌ونقل", 96_000L, "تاکسی اینترنتی"),
        3 to Triple("خوراک", 780_000L, "میوه و سبزیجات"),
        4 to Triple("سرگرمی", 350_000L, "کافه با دوستان"),
        5 to Triple("خرید", 2_450_000L, "کفش"),
        6 to Triple("خوراک", 1_950_000L, "خرید هفتگی فروشگاه"),
        7 to Triple("حمل‌ونقل", 210_000L, "بنزین"),
        8 to Triple("سلامت", 640_000L, "داروخانه"),
        9 to Triple("خوراک", 135_000L, "نان"),
        10 to Triple("سرگرمی", 900_000L, "سینما"),
        11 to Triple("قبض‌ها", 890_000L, "اینترنت"),
        12 to Triple("پوشاک", 1_680_000L, "تیشرت"),
        13 to Triple("خوراک", 560_000L, "قنادی"),
        14 to Triple("خانه", 3_200_000L, "لوازم خانه"),
        15 to Triple("حمل‌ونقل", 85_000L, "مترو"),
        16 to Triple("خوراک", 2_150_000L, "خرید هفتگی فروشگاه"),
        17 to Triple("آموزش", 1_400_000L, "دوره آنلاین"),
        18 to Triple("سرگرمی", 480_000L, "اشتراک موسیقی"),
        19 to Triple("خوراک", 690_000L, "رستوران با خانواده"),
        20 to Triple("سلامت", 2_800_000L, "دندانپزشک"),
        21 to Triple("حمل‌ونقل", 120_000L, "اسنپ"),
        22 to Triple("خوراک", 745_000L, "سوپرمارکت"),
        23 to Triple("پوشاک", 980_000L, "شلوار"),
        24 to Triple("سرگرمی", 320_000L, "بولینگ"),
        25 to Triple("قبض‌ها", 320_000L, "قبض آب"),
        26 to Triple("خوراک", 1_120_000L, "خرید هفتگی فروشگاه"),
        27 to Triple("حمل‌ونقل", 150_000L, "تاکسی"),
        28 to Triple("خوراک", 430_000L, "پیتزا")
    )

    private val PREV_MONTH: List<Pair<Int, Triple<String, Long, String>>> = listOf(
        1 to Triple("خوراک", 390_000L, "سوپرمارکت"),
        2 to Triple("قبض‌ها", 1_050_000L, "قبض برق"),
        4 to Triple("حمل‌ونقل", 240_000L, "بنزین"),
        5 to Triple("خوراک", 1_780_000L, "خرید هفتگی فروشگاه"),
        7 to Triple("سرگرمی", 760_000L, "سینما"),
        9 to Triple("خرید", 3_100_000L, "هدفون"),
        11 to Triple("خوراک", 620_000L, "رستوران"),
        13 to Triple("قبض‌ها", 850_000L, "اینترنت"),
        15 to Triple("سلامت", 1_240_000L, "داروخانه"),
        17 to Triple("خوراک", 2_030_000L, "خرید هفتگی فروشگاه"),
        19 to Triple("پوشاک", 2_450_000L, "کاپشن"),
        21 to Triple("حمل‌ونقل", 190_000L, "بنزین"),
        23 to Triple("سرگرمی", 540_000L, "شهربازی با بچه‌ها"),
        25 to Triple("قبض‌ها", 310_000L, "قبض آب"),
        27 to Triple("خوراک", 980_000L, "ماهی و گوشت"),
        29 to Triple("خانه", 1_850_000L, "حساب مشترک خانه")
    )

    /**
     * Builds sample transactions for the current and previous Jalali months.
     * [catId] maps a default-category name to its freshly inserted DB id.
     */
    fun build(today: LocalDate, catId: (String) -> Long): List<TransactionEntity> {
        val result = mutableListOf<TransactionEntity>()
        val mk = Jalali.currentMonthKey(today)
        val pm = mk.previous()
        val todayDay = Jalali.toJalali(today).day

        fun add(jy: Int, jm: Int, day: Int, name: String, amount: Long, note: String) {
            val date = Jalali.toGregorian(jy, jm, day)
            result.add(
                TransactionEntity(
                    amount = amount,
                    categoryId = catId(name),
                    date = Jalali.startOfDayMillis(date),
                    note = note
                )
            )
        }

        // Incomes: salary in both months + small extras
        add(mk.year, mk.month, 1, "حقوق", 28_500_000L, "حقوق ماهانه")
        add(pm.year, pm.month, 1, "حقوق", 27_800_000L, "حقوق ماهانه")
        add(pm.year, pm.month, 12, "هدیه", 850_000L, "هدیه تولد")
        add(pm.year, pm.month, 21, "فروش", 2_300_000L, "فروش گوشی قدیمی")
        if (todayDay >= 14) add(mk.year, mk.month, 14, "هدیه", 500_000L, "هدیه")
        if (todayDay >= 20) add(mk.year, mk.month, 20, "فروش", 1_150_000L, "فروش کتاب")

        CURRENT_MONTH.forEach { (day, t) ->
            if (day <= todayDay) add(mk.year, mk.month, day, t.first, t.second, t.third)
        }
        PREV_MONTH.forEach { (day, t) ->
            add(pm.year, pm.month, day, t.first, t.second, t.third)
        }
        return result
    }
}
