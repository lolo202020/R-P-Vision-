package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = Color(0xFF451A03),
    primaryContainer = AmberPrimaryDark,
    onPrimaryContainer = AmberContainer,
    secondary = SlateSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = IncomeGreenLight,
    onTertiary = Color(0xFF064E3B),
    error = ExpenseRedLight,
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = ExpenseRedContainer,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.White,
    primaryContainer = AmberContainer,
    onPrimaryContainer = OnAmberContainer,
    secondary = SlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = OnSlateContainer,
    tertiary = IncomeGreen,
    onTertiary = Color.White,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedContainer,
    onErrorContainer = OnExpenseRedContainer,
    background = NeutralBackground,
    onBackground = TextPrimary,
    surface = NeutralSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NeutralOutline
)

@Composable
fun SiteKhataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set default false to preserve distinctive brand theme
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

