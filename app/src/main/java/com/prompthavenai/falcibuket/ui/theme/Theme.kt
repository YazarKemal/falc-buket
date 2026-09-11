package com.prompthavenai.falcibuket.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF2A1A05),
    secondary = Rose,
    onSecondary = Color(0xFF2A0F18),
    background = NightBg,
    onBackground = TextCream,
    surface = SurfacePlum,
    onSurface = TextCream,
    surfaceVariant = SurfacePlum,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF4A3866)
)

@Composable
fun FalcibuketTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
