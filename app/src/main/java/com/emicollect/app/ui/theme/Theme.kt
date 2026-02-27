package com.emicollect.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PremiumScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = GunmetalDark,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = TextWhite,
    secondary = GoldAccent,
    onSecondary = GunmetalDark,
    background = GunmetalDark,
    onBackground = TextWhite,
    surface = GunmetalLight,
    onSurface = TextWhite,
    error = ErrorRed,
    onError = TextWhite
)

private val LightScheme = androidx.compose.material3.lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = TextWhite,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = GunmetalDark,
    secondary = GoldAccent,
    onSecondary = GunmetalDark,
    background = androidx.compose.ui.graphics.Color(0xFFF1F5F9), // Slate 100
    onBackground = GunmetalDark,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = GunmetalDark,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun EMICollectAppTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) PremiumScheme else LightScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar matches the background
            window.statusBarColor = colorScheme.background.toArgb()
            // Icons are light if background is dark, dark if background is light
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
