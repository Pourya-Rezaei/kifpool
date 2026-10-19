package com.kifpool.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ---------- semantic app colors (income / expense) ------------------------
data class AppColors(
    val income: Color,
    val expense: Color,
    val incomeContainer: Color,
    val expenseContainer: Color
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        income = Color(0xFF34D399),
        expense = Color(0xFFFB7185),
        incomeContainer = Color(0xFF10382C),
        expenseContainer = Color(0xFF3B1B26)
    )
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF032723),
    primaryContainer = Color(0xFF0F4A43),
    onPrimaryContainer = Color(0xFF9FF2E6),
    secondary = Color(0xFFB0BFD6),
    onSecondary = Color(0xFF0B1220),
    secondaryContainer = Color(0xFF223047),
    onSecondaryContainer = Color(0xFFC7D4E8),
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF12142E),
    tertiaryContainer = Color(0xFF33366B),
    onTertiaryContainer = Color(0xFFC5C9F8),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE7EBF5),
    surface = Color(0xFF111731),
    onSurface = Color(0xFFE7EBF5),
    surfaceVariant = Color(0xFF1B2444),
    onSurfaceVariant = Color(0xFF9CA7C4),
    outline = Color(0xFF2B3557),
    outlineVariant = Color(0xFF222B49),
    error = Color(0xFFF87171),
    onError = Color(0xFF2B0A0A),
    errorContainer = Color(0xFF4A1414),
    onErrorContainer = Color(0xFFFCA5A5),
    inverseSurface = Color(0xFFE7EBF5),
    inverseOnSurface = Color(0xFF111731),
    surfaceDim = Color(0xFF0B1020),
    surfaceBright = Color(0xFF1E2748),
    surfaceContainerLowest = Color(0xFF0A0F1E),
    surfaceContainerLow = Color(0xFF0E1428),
    surfaceContainer = Color(0xFF121833),
    surfaceContainerHigh = Color(0xFF161E3B),
    surfaceContainerHighest = Color(0xFF1B2444)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F7F0),
    onPrimaryContainer = Color(0xFF063B36),
    secondary = Color(0xFF47566E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEE6F2),
    onSecondaryContainer = Color(0xFF1B2440),
    tertiary = Color(0xFF6366F1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E2FC),
    onTertiaryContainer = Color(0xFF1E1B4B),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF121A2C),
    surface = Color.White,
    onSurface = Color(0xFF121A2C),
    surfaceVariant = Color(0xFFE7ECF6),
    onSurfaceVariant = Color(0xFF57627A),
    outline = Color(0xFFC3CBDD),
    outlineVariant = Color(0xFFE0E5F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFDE4E4),
    onErrorContainer = Color(0xFF7F1D1D),
    inverseSurface = Color(0xFF121A2C),
    inverseOnSurface = Color(0xFFF4F6FB),
    surfaceDim = Color(0xFFDDE1EC),
    surfaceBright = Color(0xFFF4F6FB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainer = Color(0xFFF2F4FA),
    surfaceContainerHigh = Color(0xFFECEFF7),
    surfaceContainerHighest = Color(0xFFE6EAF3)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KifPoolTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val appColors = if (dark) {
        AppColors(
            income = Color(0xFF34D399),
            expense = Color(0xFFFB7185),
            incomeContainer = Color(0xFF10382C),
            expenseContainer = Color(0xFF3B1B26)
        )
    } else {
        AppColors(
            income = Color(0xFF059669),
            expense = Color(0xFFE11D48),
            incomeContainer = Color(0xFFD8F3E7),
            expenseContainer = Color(0xFFFCE1E8)
        )
    }
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
