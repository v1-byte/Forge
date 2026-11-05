package com.v1byte.forge.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ForgeBackground = Color(0xFF08080A)
val ForgeSurface = Color(0xFF111113)
val ForgePanel = Color(0xFF1A1A1E)
val ForgePanelRaised = Color(0xFF222225)
val ForgeBorder = Color(0xFF1E1E20)
val ForgeText = Color(0xFFFAFAFA)
val ForgeTextSecondary = Color(0xFFE4E4E7)
val ForgeMuted = Color(0xFFA1A1AA)
val ForgeMutedSecondary = Color(0xFF71717B)
val ForgeOrange = Color(0xFFFF6A00)
val ForgePink = Color(0xFFFF3D9A)
val ForgeGreen = Color(0xFF22C55E)
val ForgeYellow = Color(0xFFFACC15)
val ForgeRed = Color(0xFFEF4444)

private val ForgeColorScheme = darkColorScheme(
    primary = ForgeOrange,
    onPrimary = Color.White,
    secondary = ForgePink,
    background = ForgeBackground,
    onBackground = ForgeText,
    surface = ForgeSurface,
    onSurface = ForgeText,
    surfaceVariant = ForgePanel,
    onSurfaceVariant = ForgeTextSecondary,
    outline = ForgeBorder,
    error = ForgeRed,
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ForgeColorScheme,
        content = content,
    )
}
