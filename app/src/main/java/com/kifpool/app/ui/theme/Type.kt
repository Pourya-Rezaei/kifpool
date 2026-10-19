package com.kifpool.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kifpool.app.R

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Vazir, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = Vazir, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = Vazir, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Vazir, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Vazir, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Vazir, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Vazir, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = Vazir, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = Vazir, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = Vazir),
    bodyMedium = base.bodyMedium.copy(fontFamily = Vazir),
    bodySmall = base.bodySmall.copy(fontFamily = Vazir),
    labelLarge = base.labelLarge.copy(fontFamily = Vazir, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = Vazir),
    labelSmall = base.labelSmall.copy(fontFamily = Vazir)
)
