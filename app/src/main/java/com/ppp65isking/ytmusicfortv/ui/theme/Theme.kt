package com.ppp65isking.ytmusicfortv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

// Anthony's favourite colour: purple. Dark surfaces, purple accent.
// Liquid-glass treatment (refraction / specular sheen on the nav layer only)
// is a Phase-4 direction — Compose for TV has no real liquid glass, so the
// scaffold uses solid dark surfaces and the glass look lands later.
private val YtmDarkColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    secondary = Color(0xFFA78BFA),
    background = Color(0xFF0F0B1E),
    surface = Color(0xFF1A1430),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
)

@Composable
fun YtmTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YtmDarkColors,
        content = content,
    )
}
