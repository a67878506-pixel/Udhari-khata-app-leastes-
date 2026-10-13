package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NavyPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = BlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = AmberWarning,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = RedDenaDark,
    onError = Color.White,
    errorContainer = RedDenaContainerDark,
    onErrorContainer = OnRedDenaContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = NavyOnPrimary,
    primaryContainer = NavyContainer,
    onPrimaryContainer = OnNavyContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = OnBlueSecondaryContainer,
    tertiary = AmberWarning,
    onTertiary = Color.White,
    background = NeutralSurface,
    onBackground = NeutralOnSurface,
    surface = Color.White,
    onSurface = NeutralOnSurface,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
    error = RedDena,
    onError = Color.White,
    errorContainer = RedDenaContainer,
    onErrorContainer = OnRedDenaContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val financialColors = if (darkTheme) {
        FinancialColors(
            lena = GreenLenaDark,
            lenaContainer = GreenLenaContainerDark,
            onLenaContainer = OnGreenLenaContainerDark,
            dena = RedDenaDark,
            denaContainer = RedDenaContainerDark,
            onDenaContainer = OnRedDenaContainerDark,
            cardBorder = DarkOutline
        )
    } else {
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

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
