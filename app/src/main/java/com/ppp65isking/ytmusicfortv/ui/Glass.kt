package com.ppp65isking.ytmusicfortv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid-glass building blocks.
 *
 * Real liquid glass (Apple, WWDC 2025) is a refractive, specular material:
 * bright top-edge highlights, a thin light border, concentric rounded
 * shapes and depth from layered translucency. Compose for TV cannot blur
 * or refract (Modifier.blur needs API 31+ and is banned here), so every
 * effect below is faked with translucent fills, gradient borders and soft
 * radial glow orbs behind the content. Nothing here is a flat solid colour.
 *
 * Only foundation/ui APIs are used on purpose — no tv-material component
 * is needed to draw glass, which keeps the look identical on every API
 * level the app supports (minSdk 24).
 */
object Glass {
    /** 1px light border — bright at the top, fading down (light from above). */
    val BorderBright = listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.08f),
    )
    val BorderDim = listOf(
        Color.White.copy(alpha = 0.14f),
        Color.White.copy(alpha = 0.05f),
    )

    /** Selected-item purple glass border. */
    val PurpleBorder = listOf(
        Color(0xFFE9D5FF).copy(alpha = 0.9f),
        Color(0xFF8B5CF6).copy(alpha = 0.55f),
    )

    val PurpleLight = Color(0xFFA78BFA)
}

/**
 * The canvas: near-black with a deep purple tint, plus three large soft
 * glow orbs (purple top-left, magenta right, blue bottom) drawn as radial
 * gradients. The glass surfaces sit over this so they have something to
 * catch light from. A faint top-to-bottom dark gradient keeps text legible.
 */
@Composable
fun GlassBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0912))
            .drawBehind {
                val w = size.width
                val h = size.height
                // Purple orb, top-left.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.10f, h * 0.05f),
                        radius = w * 0.55f,
                    ),
                    center = Offset(w * 0.10f, h * 0.05f),
                    radius = w * 0.55f,
                )
                // Magenta orb, right.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD946EF).copy(alpha = 0.32f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.92f, h * 0.30f),
                        radius = w * 0.42f,
                    ),
                    center = Offset(w * 0.92f, h * 0.30f),
                    radius = w * 0.42f,
                )
                // Blue orb, bottom-left.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2563EB).copy(alpha = 0.30f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.22f, h * 1.02f),
                        radius = w * 0.50f,
                    ),
                    center = Offset(w * 0.22f, h * 1.02f),
                    radius = w * 0.50f,
                )
                // Gentle dark wash so text stays readable over the orbs.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0912).copy(alpha = 0.15f),
                            Color.Transparent,
                            Color(0xFF0B0912).copy(alpha = 0.35f),
                        ),
                    ),
                )
            },
    )
}

/**
 * The specular top-edge highlight: a 1dp line that fades in from the left,
 * peaks bright, then fades out — light catching the top rim of a glass
 * surface. Place it as the first child inside a glass container.
 */
@Composable
fun SpecularEdge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

/** Gradient artwork squares (placeholder art until real thumbnails land). */
private val ARTWORK_GRADIENTS = listOf(
    listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)),
    listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6)),
    listOf(Color(0xFFEC4899), Color(0xFFF59E0B)),
    listOf(Color(0xFF10B981), Color(0xFF3B82F6)),
    listOf(Color(0xFFF97316), Color(0xFFEC4899)),
    listOf(Color(0xFF6366F1), Color(0xFF22D3EE)),
)

/**
 * A rounded gradient square with a music-note glyph — the artwork-forward
 * look from Apple Music / Spotify. The hue cycles per item so rails read
 * as a shelf of albums instead of identical grey tiles.
 */
@Composable
fun ArtworkTile(index: Int, modifier: Modifier = Modifier) {
    val gradient = ARTWORK_GRADIENTS[index % ARTWORK_GRADIENTS.size]
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors = gradient))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = Glass.BorderBright),
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.85f)),
        )
    }
}

/**
 * A circular glass icon button: translucent fill, 1px light border, and a
 * focus state that scales up slightly with a bright ring. The selected
 * state is a purple glass fill with a lavender border. No ripple — the
 * focus glow is the feedback.
 *
 * Hand-rolled on foundation clickable (focusable on TV via D-pad) so the
 * look does not depend on any tv-material button shape API.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 26.dp,
    selected: Boolean = false,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.12f else 1f)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFA78BFA).copy(alpha = 0.55f),
                            Color(0xFF7C3AED).copy(alpha = 0.55f),
                        ),
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.04f),
                        ),
                    )
                },
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = when {
                        selected -> Glass.PurpleBorder
                        focused -> listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.35f),
                        )
                        else -> Glass.BorderDim
                    },
                ),
                shape = CircleShape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(
                if (selected || focused) Color.White
                else Color.White.copy(alpha = 0.75f),
            ),
        )
    }
}

/**
 * A rounded glass card: translucent fill, light border, specular top edge,
 * and a focus state that scales ~1.05 with a bright ring. Used for the
 * test-playback hero card and the rail cards. Same hand-rolled focus
 * approach as [GlassIconButton].
 */
@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f)
    Column(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (focused) {
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color(0xFFA78BFA).copy(alpha = 0.7f),
                            Color.White.copy(alpha = 0.25f),
                        )
                    } else {
                        Glass.BorderBright
                    },
                ),
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .onFocusChanged { focused = it.isFocused },
        content = {
            SpecularEdge()
            content()
        },
    )
}
