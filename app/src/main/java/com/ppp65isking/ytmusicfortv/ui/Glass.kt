package com.ppp65isking.ytmusicfortv.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
 * Focus utilities from the TV skills (androidtv-compose-focus).
 *
 * The rail-reachability trap: Compose focus search does NOT cross from a
 * lazy list's first item into a sibling container, and `focusProperties
 * { exit = ... }` is not delivered through a lazy list. So HomeScreen
 * provides the nav rail's [FocusRequester] through this CompositionLocal,
 * and the leftmost content items handle Left (or Up, for the top bar)
 * explicitly, consuming the key before focus search ever sees it.
 */
val LocalNavFocusRequester = compositionLocalOf<FocusRequester?> { null }

/** Left from a leftmost content item reaches the nav rail (LEFT mode). */
@Composable
fun Modifier.navExitOnLeft(): Modifier {
    val navRequester = LocalNavFocusRequester.current ?: return this
    return onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
            navRequester.requestFocus()
            true
        } else {
            false
        }
    }
}

/** Up from the topmost content item reaches the top nav bar (TOP mode). */
@Composable
fun Modifier.navExitOnUp(): Modifier {
    val navRequester = LocalNavFocusRequester.current ?: return this
    return onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
            navRequester.requestFocus()
            true
        } else {
            false
        }
    }
}

/**
 * The select keys every OEM remote may send. `clickable` alone is not
 * enough — some remotes never fire it, so the hand-rolled glass surfaces
 * answer these keys themselves (see the onKeyEvent in [GlassCard],
 * [GlassIconButton] and [VerseGlassPill]).
 */
private val SelectKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * Handles a select key press. Placed AFTER `clickable` in the modifier
 * chain, so it runs before clickable's own key handling (post-phase
 * key events run inner-to-outer) and consumes the event — one click,
 * never two. Both KeyDown and KeyUp are consumed for select keys, so
 * clickable never sees a stray KeyUp either.
 */
private fun Modifier.selectKeys(onClick: () -> Unit, enabled: Boolean): Modifier =
    onKeyEvent { event ->
        if (event.key in SelectKeys) {
            if (event.type == KeyEventType.KeyDown && enabled) onClick()
            true
        } else {
            false
        }
    }

/**
 * The canvas: near-black with a deep purple tint, plus three large soft
 * glow orbs (purple top-left, magenta right, blue bottom) drawn as radial
 * gradients. The glass surfaces sit over this so they have something to
 * catch light from. A faint top-to-bottom dark gradient keeps text legible.
 *
 * Performance: the orbs are rendered ONCE into an offscreen bitmap the
 * first time the size is known, then the bitmap is drawn. The old version
 * redrew three huge radial gradients in drawBehind on EVERY recomposition
 * — on a Chromecast that was a visible stutter every time anything on
 * screen changed.
 */
@Composable
fun GlassBackground() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bitmap = remember(size) {
        if (size.width <= 0 || size.height <= 0) null
        else renderGlassBackground(size.width, size.height)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0912))
            .onSizeChanged { size = it },
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

/** Draws the three glow orbs + legibility wash once, into a bitmap. */
private fun renderGlassBackground(width: Int, height: Int): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)
    val w = width.toFloat()
    val h = height.toFloat()
    // Purple orb, top-left.
    canvas.drawCircle(
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
    canvas.drawCircle(
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
    canvas.drawCircle(
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
    canvas.drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B0912).copy(alpha = 0.15f),
                Color.Transparent,
                Color(0xFF0B0912).copy(alpha = 0.35f),
            ),
        ),
        size = Size(w, h),
    )
    return bitmap
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
 * A circular glass icon button.
 *
 * Focus is UNMISSABLE on purpose (10-foot viewing): 3dp bright ring,
 * 1.15x scale, a brighter fill and a drop shadow so the focused button
 * lifts off the screen. Selected stays a purple glass fill. `lens` is
 * the Verse Garden active-item treatment — a brighter vertical glass
 * fill with a white rim and a soft purple glow behind it (used for the
 * play button and the liked heart in the bottom bar).
 *
 * Hand-rolled on foundation clickable (focusable on TV via D-pad) so the
 * look does not depend on any tv-material button shape API. Animations
 * are scale-only — no layout changes on focus, so focus moves stay cheap.
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
    lens: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    // Focus contract (m3-tv): 1.1x or more, bright ring, lift — never
    // colour alone. Animation is 150-200ms ease-out; longer makes D-pad
    // navigation feel unresponsive.
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
    )
    val shadowElevation = when {
        focused -> 12.dp
        lens -> 8.dp
        else -> 0.dp
    }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = shadowElevation,
                shape = CircleShape,
                clip = false,
                spotColor = if (lens) Color(0xFFA78BFA).copy(alpha = 0.45f)
                else Color.Black,
            )
            .clip(CircleShape)
            .background(
                when {
                    lens -> Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.14f),
                        ),
                    )
                    selected -> Brush.linearGradient(
                        listOf(
                            Color(0xFFA78BFA).copy(alpha = 0.75f),
                            Color(0xFF7C3AED).copy(alpha = 0.65f),
                        ),
                    )
                    focused -> Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.08f),
                        ),
                    )
                    else -> Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.04f),
                        ),
                    )
                },
            )
            .border(
                width = if (focused) 3.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = when {
                        lens -> listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.3f),
                        )
                        selected -> Glass.PurpleBorder
                        focused -> listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.4f),
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
            .selectKeys(onClick, enabled)
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
                if (selected || focused || lens) Color.White
                else Color.White.copy(alpha = 0.75f),
            ),
        )
    }
}

/**
 * A rounded glass card.
 *
 * Same unmissable-focus treatment as [GlassIconButton]: 3dp bright ring
 * (white into purple), 1.1x scale, a clearly brighter fill and a drop
 * shadow when focused. Used for the test-playback hero card, the rail
 * cards and the track rows. Same hand-rolled focus approach.
 */
@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.1f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
    )
    Column(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (focused) 16.dp else 0.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (focused) 0.16f else 0.10f),
                        Color.White.copy(alpha = if (focused) 0.08f else 0.04f),
                    ),
                ),
            )
            .border(
                width = if (focused) 3.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (focused) {
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFFA78BFA).copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.3f),
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
            .selectKeys(onClick, enabled)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            },
        content = {
            SpecularEdge()
            content()
        },
    )
}

/**
 * The now-playing bottom bar, in our own Verse Garden Bible app's
 * liquid-glass language (styles.css: .bottom-nav-glass, .nav-lens,
 * .nav-glow, Android section ~line 722).
 *
 * Translated to Compose: a floating 28dp pill; a thin 135-degree
 * gradient fill over a purple tint (rgba(88, 70, 140, 0.18)); a 1px
 * border that is bright at the top; a top specular hairline; a faint
 * diagonal sheen band; and a soft radial specular highlight that
 * drifts slowly across the top of the pill (Verse Garden moves its
 * sheen with pointer/tilt — a TV has no pointer, so it drifts on its
 * own). The drop shadow (14dp, 20dp on focus) lifts it off the content.
 * No blur anywhere (API 31+, banned) — the tint + gradients do the work.
 */
@Composable
fun VerseGlassPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    // Focus on a full-width bar: a 1.1x scale would push it past the
    // overscan margins and clip, so the bar scales only 1.02x — but it
    // still carries the other two contract signals loudly: a 3dp bright
    // ring and a 20dp lift shadow, plus a brighter fill.
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.02f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
    )
    // Drifting sheen: only this pill animates — nothing else on screen.
    val sheenX by rememberInfiniteTransition().animateFloat(
        initialValue = 0.32f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Row(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (focused) 20.dp else 14.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
            )
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF58468C).copy(alpha = 0.18f))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.05f),
                    ),
                ),
            )
            .drawBehind {
                // Diagonal sheen band (the static base under VG's ::before).
                drawRect(
                    brush = Brush.linearGradient(
                        0.0f to Color.Transparent,
                        0.46f to Color.White.copy(alpha = 0.10f),
                        0.54f to Color.White.copy(alpha = 0.06f),
                        1.0f to Color.Transparent,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                    ),
                )
                // Drifting radial specular highlight, top-center.
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.26f),
                        0.45f to Color.White.copy(alpha = 0.08f),
                        0.7f to Color.Transparent,
                        center = Offset(size.width * sheenX, size.height * 0.28f),
                        radius = size.width * 0.22f,
                    ),
                    center = Offset(size.width * sheenX, size.height * 0.28f),
                    radius = size.width * 0.22f,
                )
                // Top specular hairline (inset 0 1px 0 white 0.45).
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 1f,
                )
            }
            .border(
                width = if (focused) 3.dp else 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (focused) {
                        listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.3f),
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f),
                        )
                    },
                ),
                shape = RoundedCornerShape(28.dp),
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .selectKeys(onClick, enabled)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}
