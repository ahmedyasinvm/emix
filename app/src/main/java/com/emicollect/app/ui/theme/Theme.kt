package com.emicollect.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════
// EMIX v4.0 — Premium Color Schemes
// ═══════════════════════════════════════════════════════════════════

private val PremiumDarkScheme = darkColorScheme(
    // Primary emerald
    primary              = EmeraldLight,
    onPrimary            = GunmetalDark,
    primaryContainer     = EmeraldPrimary,
    onPrimaryContainer   = TextWhite,

    // Secondary gold
    secondary            = GoldAccent,
    onSecondary          = GunmetalDark,
    secondaryContainer   = GoldDeep.copy(alpha = 0.3f),
    onSecondaryContainer = GoldLight,

    // Tertiary — info accent
    tertiary             = InfoBlue,
    onTertiary           = TextWhite,
    tertiaryContainer    = InfoBlueTint,
    onTertiaryContainer  = InfoBlueSoft,

    // Background / Surface hierarchy
    background           = GunmetalMid,
    onBackground         = TextWhite,
    surface              = GunmetalLight,
    onSurface            = TextWhite,
    surfaceVariant       = GunmetalElevated,
    onSurfaceVariant     = SlateLight,

    // Outline
    outline              = SlateSubtle,
    outlineVariant       = SlateSubtle.copy(alpha = 0.5f),

    // Inverse
    inverseSurface       = SlateWhite,
    inverseOnSurface     = GunmetalDark,
    inversePrimary       = EmeraldPrimary,

    // Error
    error                = ErrorRed,
    onError              = TextWhite,
    errorContainer       = ErrorRedTint,
    onErrorContainer     = ErrorRedSoft
)

private val PremiumLightScheme = lightColorScheme(
    // Primary emerald
    primary              = EmeraldPrimary,
    onPrimary            = TextWhite,
    primaryContainer     = EmeraldTint,
    onPrimaryContainer   = EmeraldDeep,

    // Secondary gold
    secondary            = GoldDeep,
    onSecondary          = TextWhite,
    secondaryContainer   = GoldTint,
    onSecondaryContainer = GoldDeep,

    // Tertiary — info
    tertiary             = InfoBlue,
    onTertiary           = TextWhite,
    tertiaryContainer    = InfoBlueTint,
    onTertiaryContainer  = InfoBlue,

    // Background / Surface hierarchy
    background           = LightBackground,
    onBackground         = TextDark,
    surface              = LightSurface,
    onSurface            = TextDark,
    surfaceVariant       = LightSurfaceVar,
    onSurfaceVariant     = SlateMuted,

    // Outline
    outline              = LightBorder,
    outlineVariant       = SlateWhite,

    // Inverse
    inverseSurface       = GunmetalMid,
    inverseOnSurface     = TextWhite,
    inversePrimary       = EmeraldLight,

    // Error
    error                = ErrorRed,
    onError              = TextWhite,
    errorContainer       = Color(0xFFFEE2E2),
    onErrorContainer     = Color(0xFFB91C1C)
)

@Composable
fun EMICollectAppTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) PremiumDarkScheme else PremiumLightScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar
            window.statusBarColor = colorScheme.background.toArgb()
            // Navigation bar
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            // Icon appearance
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
