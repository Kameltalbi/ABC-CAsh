package com.abccash.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Palette de couleurs ABC Cash
 * Rendu : Sérieux, financier, professionnel, proche des banques modernes
 */
object AppColors {
    // Couleurs principales — alignées sur le logo ABC Cash
    val Primary = Color(0xFF00B982)        // Vert ABC Cash
    val PrimaryDark = Color(0xFF00956E)      // Vert foncé (pressed)
    val Secondary = Color(0xFF2563EB)      // Bleu royal - accents
    val Success = Color(0xFF16A34A)        // Vert - Succès/Revenus
    val Warning = Color(0xFFF59E0B)        // Orange - Alerte/Échéances
    val Error = Color(0xFFDC2626)          // Rouge - Erreur/Dépenses
    
    // Fonds et surfaces
    val Background = Color(0xFFF4FBF8)     // Fond légèrement vert, plus chaleureux
    val Surface = Color.White              // Surface des cartes
    val SurfaceVariant = Color(0xFFE8F5F0) // Surface variante
    
    // Couleurs métier
    val IncomeGreen = Color(0xFF16A34A)    // Revenus/Encaissements
    val ExpenseRed = Color(0xFFDC2626)     // Dépenses/Sorties
    val OverdueOrange = Color(0xFFF59E0B)  // Retards/Alertes
    val PendingBlue = Color(0xFF2563EB)    // En attente
    
    // Backgrounds colorés
    val SuccessBackground = Color(0xFFDCFCE7)  // Fond vert clair
    val WarningBackground = Color(0xFFFEF3C7)  // Fond orange clair
    val ErrorBackground = Color(0xFFFEE2E2)    // Fond rouge clair
    val InfoBackground = Color(0xFFDEEBFF)     // Fond bleu clair
    
    // Catégories
    val CategoryIncome = Color(0xFFDCFCE7)
    val CategoryTransport = Color(0xFFDEEBFF)
    val CategoryHealth = Color(0xFFFEE2E2)
    val CategoryHousing = Color(0xFFFED7AA)
    val CategoryFood = Color(0xFFFEF3C7)
    val CategoryShopping = Color(0xFFFCE7F3)
    val CategoryDefault = Color(0xFFF1F5F9)
    
    // Texte
    val TextPrimary = Color(0xFF1E3A4A)
    val TextSecondary = Color(0xFF64748B)
    val TextTertiary = Color(0xFF94A3B8)
    
    // Bordures
    val Border = Color(0xFFE2E8F0)
    val BorderFocus = Color(0xFF2563EB)
    
    // Couleurs pour les thèmes clairs
    val LightBackground = Color(0xFFF8FAFC)
    val LightSurface = Color.White
    val LightSurfaceVariant = Color(0xFFF1F5F9)
    
    // Couleurs pour les cartes
    val CardBackground = Color.White
    val SelectedCardBackground = Color(0xFFDEEBFF)
    val InfoCardBackground = Color(0xFFF1F5F9)
    
    // Couleurs pour les indicateurs de catégories
    val RevenueDot = Color(0xFF16A34A)  // Vert pour revenus
    val ExpenseDot = Color(0xFFDC2626)  // Rouge pour dépenses
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
            primary = AppColors.Secondary,
            onPrimary = Color.White,
            secondary = AppColors.Primary,
            background = Color(0xFF0A0F1C),
            surface = Color(0xFF10182A),
            surfaceVariant = Color(0xFF1B2640),
            error = AppColors.Error,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00B982),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD1FAE8),
            onPrimaryContainer = Color(0xFF065F46),
            secondary = AppColors.Secondary,
            tertiary = AppColors.Success,
            background = AppColors.Background,
            surface = AppColors.Surface,
            surfaceVariant = AppColors.SurfaceVariant,
            error = AppColors.Error,
            onError = Color.White,
            outline = AppColors.Border,
            onSurface = AppColors.TextPrimary,
            onSurfaceVariant = AppColors.TextSecondary
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
        AppPalette.MIDNIGHT -> Color(0xFF00B982)
    }
}
