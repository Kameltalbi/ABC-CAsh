package com.abccash.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object AppColors {
    val IncomeGreen = Color(0xFF1B8F3A)
    val ExpenseRed = Color(0xFFB3261E)
    val OverdueOrange = Color(0xFFE65100)
    
    val LightBackground = Color(0xFFF8FAFC)
    val LightSurface = Color.White
    val LightSurfaceVariant = Color(0xFFF1F5F9)
    val CardBackground = Color(0xFFF8F8F8)
    val SelectedCardBackground = Color(0xFFE8F0FF)
    val InfoCardBackground = Color(0xFFF4F7FF)
    
    val CategoryIncome = Color(0xFFE9F7EF)
    val CategoryTransport = Color(0xFFEAF2FF)
    val CategoryHealth = Color(0xFFFFEDEE)
    val CategoryHousing = Color(0xFFFFF1E8)
    val CategoryFood = Color(0xFFFFF3E6)
    val CategoryShopping = Color(0xFFFFEAF5)
    val CategoryDefault = Color(0xFFEDF3F7)
    
    val RevenueDot = Color(0xFF1DB954)
    val ExpenseDot = Color(0xFFE53935)
}

enum class AppPalette(val label: String) {
    SUNSET("Sunset"),
    OCEAN("Ocean"),
    FOREST("Forest"),
    VIOLET("Violet"),
    ROSE("Rose"),
    MIDNIGHT("Midnight")
}

fun appColorScheme(darkMode: Boolean, palette: AppPalette) = when (palette) {
    AppPalette.SUNSET -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFFFF8A50),
            onPrimary = Color(0xFF1A1A1A),
            secondary = Color(0xFFFFB74D),
            background = Color(0xFF111318),
            surface = Color(0xFF1A1D24),
            surfaceVariant = Color(0xFF252A34)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFFF5A00),
            onPrimary = Color.White,
            secondary = Color(0xFFFF9800),
            background = AppColors.LightBackground,
            surface = AppColors.LightSurface,
            surfaceVariant = AppColors.LightSurfaceVariant
        )
    }
    AppPalette.OCEAN -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF0B2530),
            secondary = Color(0xFF90CAF9),
            background = Color(0xFF0F172A),
            surface = Color(0xFF111827),
            surfaceVariant = Color(0xFF1F2937)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0D47A1),
            onPrimary = Color.White,
            secondary = Color(0xFF0288D1),
            background = Color(0xFFF6FAFF),
            surface = AppColors.LightSurface,
            surfaceVariant = Color(0xFFEFF6FF)
        )
    }
    AppPalette.FOREST -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFF66BB6A),
            onPrimary = Color(0xFF0E2A10),
            secondary = Color(0xFFA5D6A7),
            background = Color(0xFF111712),
            surface = Color(0xFF18211A),
            surfaceVariant = Color(0xFF243126)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1B5E20),
            onPrimary = Color.White,
            secondary = Color(0xFF43A047),
            background = Color(0xFFF7FCF7),
            surface = AppColors.LightSurface,
            surfaceVariant = Color(0xFFECF7ED)
        )
    }
    AppPalette.VIOLET -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFFB388FF),
            onPrimary = Color(0xFF1D1038),
            secondary = Color(0xFF7C4DFF),
            background = Color(0xFF120F1F),
            surface = Color(0xFF1B172B),
            surfaceVariant = Color(0xFF2A2342)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF7C3AED),
            onPrimary = Color.White,
            secondary = Color(0xFF6D28D9),
            background = AppColors.LightBackground,
            surface = AppColors.LightSurface,
            surfaceVariant = AppColors.LightSurfaceVariant
        )
    }
    AppPalette.ROSE -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFFFF7AA2),
            onPrimary = Color(0xFF3A0F1D),
            secondary = Color(0xFFFF4D8D),
            background = Color(0xFF1A1015),
            surface = Color(0xFF24141C),
            surfaceVariant = Color(0xFF34212B)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFE91E63),
            onPrimary = Color.White,
            secondary = Color(0xFFFF5C8A),
            background = Color(0xFFFFF7FA),
            surface = AppColors.LightSurface,
            surfaceVariant = Color(0xFFFFEAF1)
        )
    }
    AppPalette.MIDNIGHT -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF052335),
            secondary = Color(0xFF0EA5E9),
            background = Color(0xFF0A0F1C),
            surface = Color(0xFF10182A),
            surfaceVariant = Color(0xFF1B2640)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0F172A),
            onPrimary = Color.White,
            secondary = Color(0xFF0284C7),
            background = Color(0xFFF6FAFF),
            surface = AppColors.LightSurface,
            surfaceVariant = Color(0xFFE8F1FF)
        )
    }
}

fun palettePreviewColor(palette: AppPalette): Color {
    return when (palette) {
        AppPalette.SUNSET -> Color(0xFFFF5A00)
        AppPalette.OCEAN -> Color(0xFF0D47A1)
        AppPalette.FOREST -> Color(0xFF1B5E20)
        AppPalette.VIOLET -> Color(0xFF7C3AED)
        AppPalette.ROSE -> Color(0xFFE91E63)
        AppPalette.MIDNIGHT -> Color(0xFF0F172A)
    }
}
