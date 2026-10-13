package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Primary Brand Colors (Deep Professional Navy Blue)
// ==========================================
val NavyPrimary = Color(0xFF1E3A8A)           // Rich Navy Blue - trust & stability
val NavyPrimaryLight = Color(0xFF2563EB)      // Vibrant Royal Blue
val NavyPrimaryDark = Color(0xFF0F172A)       // Deep Midnight
val NavyOnPrimary = Color(0xFFFFFFFF)
val NavyContainer = Color(0xFFDBEAFE)         // Soft Ice Blue container
val OnNavyContainer = Color(0xFF1E3A8A)

// Secondary Accents
val BlueSecondary = Color(0xFF0284C7)        // Ocean Blue accent
val BlueOnSecondary = Color(0xFFFFFFFF)
val BlueSecondaryContainer = Color(0xFFE0F2FE)
val OnBlueSecondaryContainer = Color(0xFF0369A1)

// ==========================================
// Financial Semantic Colors (Standard Indian Ledger)
// ==========================================
// Lena (Credit / You will get / Maine Diye) - Positive Vibrant Green
val GreenLena = Color(0xFF16A34A)
val GreenLenaDark = Color(0xFF22C55E)
val GreenLenaContainer = Color(0xFFDCFCE7)
val OnGreenLenaContainer = Color(0xFF14532D)
val GreenLenaContainerDark = Color(0xFF064E3B)
val OnGreenLenaContainerDark = Color(0xFFA7F3D0)

// Dena (Debit / You will give / Maine Liye) - Debt / Caution Red
val RedDena = Color(0xFFDC2626)
val RedDenaDark = Color(0xFFEF4444)
val RedDenaContainer = Color(0xFFFEE2E2)
val OnRedDenaContainer = Color(0xFF7F1D1D)
val RedDenaContainerDark = Color(0xFF450A0A)
val OnRedDenaContainerDark = Color(0xFFFECACA)

// Neutral & Status
val AmberWarning = Color(0xFFD97706)
val AmberWarningContainer = Color(0xFFFEF3C7)
val OnAmberWarningContainer = Color(0xFF78350F)

// Light Theme Neutrals
val NeutralSurface = Color(0xFFF8FAFC)
val NeutralOnSurface = Color(0xFF0F172A)
val NeutralSurfaceVariant = Color(0xFFF1F5F9)
val NeutralOnSurfaceVariant = Color(0xFF475569)
val NeutralOutline = Color(0xFFCBD5E1)
val NeutralOutlineVariant = Color(0xFFE2E8F0)

// Dark Theme Neutrals
val DarkBackground = Color(0xFF090E17)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)
val DarkOutlineVariant = Color(0xFF1E293B)

@Immutable
data class FinancialColors(
    val lena: Color,
    val lenaContainer: Color,
    val onLenaContainer: Color,
    val dena: Color,
    val denaContainer: Color,
    val onDenaContainer: Color,
    val cardBorder: Color
)

val LocalFinancialColors = staticCompositionLocalOf {
    FinancialColors(
        lena = GreenLena,
        lenaContainer = GreenLenaContainer,
        onLenaContainer = OnGreenLenaContainer,
        dena = RedDena,
        denaContainer = RedDenaContainer,
        onDenaContainer = OnRedDenaContainer,
        cardBorder = NeutralOutlineVariant
    )
}
