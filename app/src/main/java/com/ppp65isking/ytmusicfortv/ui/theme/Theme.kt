package com.ppp65isking.ytmusicfortv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

// Anthony's favourite colour: purple. Near-black canvas with a deep purple
// tint — content glows, chrome recedes.
//
// Liquid glass lands here: translucent surfaces, a bright specular top-edge
// highlight and layered glow orbs behind the content, all drawn with
// gradients (see ui/Glass.kt). Compose for TV has no real refraction or
// blur, so the look is faked with translucent fills, 1px light borders and
// radial glows — never flat solid colours.
private val YtmDarkColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1E1033),
    tertiary = Color(0xFFD946EF),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF0B0912),
    onBackground = Color(0xFFF5F3FF),
    surface = Color(0xFF17112A),
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = Color(0xFF221A3D),
    onSurfaceVariant = Color(0xFFC4B5FD),
    outline = Color(0xFFFFFFFF).copy(alpha = 0.14f),
)

@Composable
fun YtmTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YtmDarkColors,
        content = content,
    )
}
