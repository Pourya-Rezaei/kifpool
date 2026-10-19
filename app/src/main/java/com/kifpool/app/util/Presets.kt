package com.kifpool.app.util

import android.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalMovies
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlife
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.TxType

object Presets {

    val CATEGORY_ICONS: Map<String, ImageVector> = mapOf(
        "restaurant" to Icons.Rounded.Restaurant,
        "fastfood" to Icons.Rounded.Fastfood,
        "cafe" to Icons.Rounded.LocalCafe,
        "bus" to Icons.Rounded.DirectionsBus,
        "gas" to Icons.Rounded.LocalGasStation,
        "flight" to Icons.Rounded.Flight,
        "cart" to Icons.Rounded.ShoppingCart,
        "clothes" to Icons.Rounded.Checkroom,
        "phone" to Icons.Rounded.PhoneIphone,
        "computer" to Icons.Rounded.Computer,
        "home" to Icons.Rounded.Home,
        "bolt" to Icons.Rounded.Bolt,
        "water" to Icons.Rounded.WaterDrop,
        "health" to Icons.Rounded.HealthAndSafety,
        "fitness" to Icons.Rounded.FitnessCenter,
        "school" to Icons.Rounded.School,
        "movie" to Icons.Rounded.LocalMovies,
        "music" to Icons.Rounded.MusicNote,
        "game" to Icons.Rounded.SportsEsports,
        "nightlife" to Icons.Rounded.Nightlife,
        "child" to Icons.Rounded.ChildCare,
        "pet" to Icons.Rounded.Pets,
        "gift" to Icons.Rounded.Redeem,
        "receipt" to Icons.Rounded.Receipt,
        "money" to Icons.Rounded.Payments,
        "savings" to Icons.Rounded.Savings,
        "card" to Icons.Rounded.CreditCard,
        "work" to Icons.Rounded.Work,
        "other" to Icons.Rounded.MoreHoriz
    )

    fun iconOf(key: String): ImageVector =
        CATEGORY_ICONS[key] ?: CATEGORY_ICONS.getValue("other")

    val PALETTE = listOf(
        "#F97316", "#FBBF24", "#FACC15", "#4ADE80", "#34D399", "#2DD4BF",
        "#38BDF8", "#60A5FA", "#818CF8", "#A78BFA", "#F472B6", "#FB7185"
    )

    /** Parses a "#RRGGBB" hex string into an ARGB int with a safe fallback. */
    fun colorOf(hex: String): Int = try {
        Color.parseColor(hex)
    } catch (e: Exception) {
        Color.parseColor("#2DD4BF")
    }

    val DEFAULT_CATEGORIES = listOf(
        CategoryEntity(name = "خوراک", type = TxType.EXPENSE, icon = "restaurant", color = "#F97316", isDefault = true),
        CategoryEntity(name = "حمل‌ونقل", type = TxType.EXPENSE, icon = "bus", color = "#38BDF8", isDefault = true),
        CategoryEntity(name = "خرید", type = TxType.EXPENSE, icon = "cart", color = "#F472B6", isDefault = true),
        CategoryEntity(name = "سرگرمی", type = TxType.EXPENSE, icon = "game", color = "#A78BFA", isDefault = true),
        CategoryEntity(name = "قبض‌ها", type = TxType.EXPENSE, icon = "receipt", color = "#FBBF24", isDefault = true),
        CategoryEntity(name = "سلامت", type = TxType.EXPENSE, icon = "health", color = "#34D399", isDefault = true),
        CategoryEntity(name = "آموزش", type = TxType.EXPENSE, icon = "school", color = "#60A5FA", isDefault = true),
        CategoryEntity(name = "خانه", type = TxType.EXPENSE, icon = "home", color = "#FB7185", isDefault = true),
        CategoryEntity(name = "پوشاک", type = TxType.EXPENSE, icon = "clothes", color = "#818CF8", isDefault = true),
        CategoryEntity(name = "سایر هزینه", type = TxType.EXPENSE, icon = "other", color = "#94A3B8", isDefault = true),
        CategoryEntity(name = "حقوق", type = TxType.INCOME, icon = "money", color = "#34D399", isDefault = true),
        CategoryEntity(name = "هدیه", type = TxType.INCOME, icon = "gift", color = "#F472B6", isDefault = true),
        CategoryEntity(name = "فروش", type = TxType.INCOME, icon = "savings", color = "#60A5FA", isDefault = true),
        CategoryEntity(name = "سایر درآمد", type = TxType.INCOME, icon = "other", color = "#94A3B8", isDefault = true)
    )
}
