package com.kifpool.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object TxType {
    const val EXPENSE = 0
    const val INCOME = 1
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: Int,          // TxType.EXPENSE / TxType.INCOME
    val icon: String,       // icon key, see Presets.CATEGORY_ICONS
    val color: String,      // hex like "#F97316"
    val isDefault: Boolean = false,
    val budgetAmount: Long = 0   // 0 = بدون بودجه (نسخهٔ ۱٫۶)
)

@Entity(
    tableName = "transactions",
    indices = [Index("categoryId"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,       // Toman
    val categoryId: Long,
    val date: Long,         // epoch millis at start of the Jalali day
    val note: String = ""
)

object DebtType {
    const val CREDIT = 0   // «به من بدهکاره» — طلب من
    const val DEBT = 1     // «من بدهکارم» — بدهی من
}

/** دفتر طلب و بدهی (نسخهٔ ۱٫۶). */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,             // نام طرف حساب
    val amount: Long,               // تومان
    val type: Int,                  // DebtType.CREDIT / DebtType.DEBT
    val note: String = "",
    val createdDate: Long,          // epoch millis روز ثبت
    val dueDate: Long = 0,          // 0 = بدون سررسید
    val settled: Boolean = false    // تسویه شده؟
)
