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
    val isDefault: Boolean = false
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
