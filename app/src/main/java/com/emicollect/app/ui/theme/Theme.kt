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

@Composable
fun EMICollectAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PremiumScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar matches the dark background
            window.statusBarColor = GunmetalDark.toArgb()
            // Ensure icons are light (for dark background)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
