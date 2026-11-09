package com.ppp65isking.ytmusicfortv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ppp65isking.ytmusicfortv.playback.PlayerManager
import com.ppp65isking.ytmusicfortv.playback.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen now playing, Apple Music style: huge artwork, big
 * title/artist, and a translucent bottom transport overlay with the
 * progress bar, times, and controls (previous = restart, play/pause,
 * heart, lyrics). The TV Back button closes it too (BackHandler in
 * HomeScreen).
 *
 * Transport model (leanback-to-compose-tv-migration): the D-pad keys
 * live on the screen container, not on the buttons, so they work while
 * the transport is hidden — Center/Enter = play/pause, Left/Right =
 * seek ∓10s, Up/Down = show/hide the transport. The transport hides
 * 5s after the last key press and reappears on any key. Keys only
 * steer the transport while focus is on a transport control; when
 * focus is on Close, the keys pass through so the D-pad can still
 * leave the screen.
 *
 * The progress bar is DISPLAY-ONLY: seeking an HLS audio stream by
 * dragging on a D-pad is awkward. Position is polled from ExoPlayer
 * every 500ms while this screen is showing.
 */
@Composable
fun NowPlayingScreen(
    playerManager: PlayerManager,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerState by playerManager.state.collectAsState()
    val libVersion by playerManager.libraryStore.version.collectAsState()
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var lyricsNote by remember { mutableStateOf(false) }
    val playFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val hasTrack = playerState.title.isNotEmpty()
    val liked = hasTrack && remember(libVersion, playerState.videoId) {
        playerManager.libraryStore.isLiked(playerState.videoId)
    }

    // Which transport control has focus, so the transport keys know
    // whether Center/Left/Right belong to the transport or to the
    // focused button itself.
    var focusedControl by remember { mutableStateOf("play") }

    // Transport auto-hide: visible on entry, hidden 5s after the last
    // key press. The overlay stays composed (alpha 0) so focus is never
    // lost and the keys keep working while it is hidden.
    var controlsVisible by remember { mutableStateOf(true) }
    var hideJob by remember { mutableStateOf<Job?>(null) }
    fun scheduleAutoHide() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(5000)
            controlsVisible = false
        }
    }
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
    )

    LaunchedEffect(Unit) { scheduleAutoHide() }

    // Poll position ~2x a second while this screen is up. Keyed on the
    // track so a new song restarts the loop from 0.
    LaunchedEffect(playerState.videoId) {
        while (true) {
            positionMs = playerManager.currentPositionMs()
            durationMs = playerManager.durationMs()
            delay(500)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val inTransport = focusedControl in setOf("prev", "play", "heart", "lyrics")
                var handled = false
                var hidByToggle = false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (focusedControl == "play") {
                            playerManager.togglePlayPause()
                            handled = true
                        }
                    }
                    Key.MediaPlayPause, Key.Spacebar -> {
                        playerManager.togglePlayPause()
                        handled = true
                    }
                    Key.DirectionLeft, Key.MediaRewind -> {
                        if (focusedControl == "play") {
                            playerManager.seekBy(-10_000)
                            handled = true
                        }
                    }
                    Key.DirectionRight, Key.MediaFastForward -> {
                        if (focusedControl == "play") {
                            playerManager.seekBy(10_000)
                            handled = true
                        }
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (inTransport) {
                            controlsVisible = !controlsVisible
                            if (controlsVisible) scheduleAutoHide() else hideJob?.cancel()
                            hidByToggle = true
                            handled = true
                        }
                    }
                }
                // Any other key press re-shows the transport and restarts
                // the 5s timer — except a toggle that just hid it.
                if (!hidByToggle) {
                    if (!controlsVisible) controlsVisible = true
                    scheduleAutoHide()
                }
                handled
            },
    ) {
        GlassBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 64.dp, end = 64.dp, top = 48.dp, bottom = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Close, top-right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                GlassIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Close",
                    onClick = onClose,
                    size = 56.dp,
                    iconSize = 26.dp,
                    onFocusChanged = { if (it) focusedControl = "close" },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Huge artwork.
            ArtworkTile(
                index = 0,
                modifier = Modifier
                    .size(300.dp)
                    .shadow(24.dp, RoundedCornerShape(16.dp)),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = if (hasTrack) playerState.title else "Nothing playing yet",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 48.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    playerState.isLoading -> "Loading…"
                    hasTrack -> playerState.artist
                    else -> "Tap the test card to play Bohemian Rhapsody"
                },
                style = MaterialTheme.typography.titleLarge,
                fontSize = 26.sp,
                color = Color(0xFFC4B5FD),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            playerState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = Color(0xFFFCA5A5),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Translucent bottom transport overlay: progress + times +
        // controls in one glass bar. Always composed — alpha 0 when
        // hidden — so focus and the transport keys survive auto-hide.
        val fraction = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 64.dp, end = 64.dp, bottom = 48.dp)
                .alpha(controlsAlpha)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.04f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(colors = Glass.BorderBright),
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Progress bar (display-only) + times.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFA78BFA), Color(0xFF7C3AED)),
                            ),
                        ),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatMs(positionMs),
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                )
                Text(
                    text = formatMs(durationMs),
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Controls: previous (restart), play/pause, heart, lyrics.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                GlassIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = "Restart track",
                    onClick = { playerManager.restart() },
                    enabled = hasTrack,
                    size = 64.dp,
                    iconSize = 32.dp,
                    onFocusChanged = { if (it) focusedControl = "prev" },
                )
                GlassIconButton(
                    icon = if (playerState.isPlaying) Icons.Filled.Pause
                    else Icons.Filled.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    onClick = { playerManager.togglePlayPause() },
                    enabled = hasTrack,
                    size = 88.dp,
                    iconSize = 44.dp,
                    selected = hasTrack,
                    lens = hasTrack,
                    modifier = Modifier.focusRequester(playFocusRequester),
                    onFocusChanged = { if (it) focusedControl = "play" },
                )
                GlassIconButton(
                    icon = if (liked) Icons.Filled.Favorite
                    else Icons.Filled.FavoriteBorder,
                    contentDescription = if (liked) "Unlike" else "Like",
                    onClick = {
                        playerManager.libraryStore.toggleLiked(
                            Track(
                                playerState.videoId,
                                playerState.title,
                                playerState.artist,
                            ),
                        )
                    },
                    enabled = hasTrack,
                    size = 64.dp,
                    iconSize = 30.dp,
                    selected = liked,
                    lens = liked,
                    onFocusChanged = { if (it) focusedControl = "heart" },
                )
                GlassCard(
                    onClick = { lyricsNote = true },
                    modifier = Modifier.height(64.dp),
                    onFocusChanged = { if (it) focusedControl = "lyrics" },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Lyrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 24.sp,
                            color = Color.White,
                        )
                    }
                }
            }
            if (lyricsNote) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Lyrics coming soon",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                )
            }
        }
    }

    // Initial focus on Play, claimed with retry.
    LaunchedEffect(Unit) {
        repeat(10) {
            if (playFocusRequester.requestFocus()) return@LaunchedEffect
            delay(100)
        }
    }
}

/** mm:ss; "--:--" when the length is unknown (live/HLS not ready yet). */
private fun formatMs(ms: Long): String {
    if (ms < 0) return "--:--"
    val totalSeconds = ms / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
