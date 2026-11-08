package com.ppp65isking.ytmusicfortv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ppp65isking.ytmusicfortv.innertube.StreamResolver
import com.ppp65isking.ytmusicfortv.playback.PlayerManager
import com.ppp65isking.ytmusicfortv.playback.PlayerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Phase 1 test song: Queen — Bohemian Rhapsody (videoId from the probes). */
private const val TEST_VIDEO_ID = "BSTsnWoslP4"

private val NAV_ITEMS = listOf("Home", "Search", "Library", "Settings")

// Placeholder rows until search/browse lands (Phase 3).
private val TRENDING = listOf(
    "Bohemian Rhapsody — Queen",
    "Blinding Lights — The Weeknd",
    "Levitating — Dua Lipa",
    "As It Was — Harry Styles",
)
private val YOUR_MIXES = listOf(
    "Mix 1 — For you",
    "Mix 2 — Rock",
    "Mix 3 — Chill",
    "Mix 4 — Workout",
)

private fun navIcon(label: String): ImageVector = when (label) {
    "Home" -> Icons.Filled.Home
    "Search" -> Icons.Filled.Search
    "Library" -> Icons.Filled.LibraryMusic
    else -> Icons.Filled.Settings
}

@Composable
fun HomeScreen(
    playerManager: PlayerManager,
    streamResolver: StreamResolver,
) {
    var selectedNav by remember { mutableStateOf("Home") }
    var resolving by remember { mutableStateOf(false) }
    var resolveError by remember { mutableStateOf<String?>(null) }
    val playerState by playerManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val firstCardFocusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassBackground()

        Row(modifier = Modifier.fillMaxSize()) {
            // --- Left icon rail: icons only, labels appear on focus/select ---
            Column(
                modifier = Modifier
                    .width(104.dp)
                    .fillMaxHeight()
                    .padding(top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Brand mark: purple glass circle, music note.
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFA78BFA), Color(0xFF7C3AED)),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(colors = Glass.BorderBright),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "YTMusic for TV",
                        modifier = Modifier.size(26.dp),
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                // Floating glass rail panel.
                Column(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.03f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(colors = Glass.BorderDim),
                            shape = RoundedCornerShape(28.dp),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpecularEdge()
                    Spacer(modifier = Modifier.height(2.dp))
                    NAV_ITEMS.forEach { item ->
                        RailItem(
                            label = item,
                            icon = navIcon(item),
                            selected = selectedNav == item,
                            onClick = { selectedNav = item },
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // --- Content + floating now-playing pill ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                if (selectedNav == "Home") {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 32.dp,
                            top = 32.dp,
                            bottom = 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        item {
                            // Hero test card — same tap behavior as before.
                            GlassCard(
                                onClick = {
                                    scope.launch {
                                        resolving = true
                                        resolveError = null
                                        try {
                                            val stream = withContext(Dispatchers.IO) {
                                                streamResolver.resolve(TEST_VIDEO_ID)
                                            }
                                            playerManager.playStream(
                                                stream,
                                                "Bohemian Rhapsody",
                                                "Queen",
                                            )
                                        } catch (e: Exception) {
                                            resolveError = e.message ?: "Resolve failed"
                                        } finally {
                                            resolving = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(560.dp)
                                    .focusRequester(firstCardFocusRequester),
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    ArtworkTile(
                                        index = 0,
                                        modifier = Modifier.size(76.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Test playback",
                                            color = Glass.PurpleLight,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = when {
                                                resolving -> "Resolving stream…"
                                                else -> "Bohemian Rhapsody — Queen"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = "Resolves the real stream via InnerTube, then plays it",
                                            color = Color(0xFFD8B4FE),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        resolveError?.let { error ->
                                            Text(
                                                text = "Error: $error",
                                                color = Color(0xFFFCA5A5),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                    GlassIconButton(
                                        icon = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        onClick = {},
                                        enabled = false,
                                        size = 48.dp,
                                        iconSize = 24.dp,
                                    )
                                }
                            }
                        }
                        item {
                            ContentRow(title = "Trending", items = TRENDING)
                        }
                        item {
                            ContentRow(title = "Your mixes", items = YOUR_MIXES)
                        }
                    }
                } else {
                    // Search / Library / Settings are not built yet — show an
                    // intentional empty state instead of an error-looking note.
                    ComingSoonSection(
                        section = selectedNav,
                        modifier = Modifier.weight(1f),
                    )
                }

                NowPlayingBar(
                    playerState = playerState,
                    playerManager = playerManager,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        firstCardFocusRequester.requestFocus()
    }
}

/**
 * One rail entry: a circular glass icon button with its label underneath.
 * The label is only visible when the item is focused or selected — the
 * rail is icons-only, as requested. The text still occupies its space
 * when transparent so the layout never shifts.
 */
@Composable
private fun RailItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassIconButton(
            icon = icon,
            contentDescription = label,
            onClick = onClick,
            selected = selected,
            onFocusChanged = { focused = it },
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (focused || selected) Color.White else Color.Transparent,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ContentRow(title: String, items: List<String>) {
    var isRowFocused by remember { mutableStateOf(false) }
    Column {
        Text(
            text = title,
            color = if (isRowFocused) Color.White else Color(0xFFC4B5FD),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.onFocusChanged { isRowFocused = it.hasFocus },
        ) {
            itemsIndexed(items) { index, item ->
                val parts = item.split(" — ")
                GlassCard(
                    onClick = { },
                    modifier = Modifier.width(220.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ArtworkTile(
                            index = index,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = parts[0],
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (parts.size > 1) {
                            Text(
                                text = parts[1],
                                color = Color(0xFFC4B5FD),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoonSection(section: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlassIconButton(
            icon = navIcon(section),
            contentDescription = section,
            onClick = {},
            size = 88.dp,
            iconSize = 40.dp,
            enabled = false,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = section,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Coming soon — $section lands in Phase 3",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFC4B5FD),
        )
    }
}

/**
 * Floating glass pill at the bottom: mini artwork, track title/artist
 * (or a "Nothing playing yet" hint), a "Loading…" state, and a
 * play/pause button. Wiring to PlayerManager is unchanged.
 */
@Composable
private fun NowPlayingBar(
    playerState: PlayerUiState,
    playerManager: PlayerManager,
    modifier: Modifier = Modifier,
) {
    val hasTrack = playerState.title.isNotEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 32.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(colors = Glass.BorderBright),
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (hasTrack) {
                ArtworkTile(index = 0, modifier = Modifier.size(48.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(colors = Glass.BorderDim),
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.4f)),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasTrack) playerState.title else "Nothing playing yet",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        playerState.isLoading -> "Loading…"
                        hasTrack -> playerState.artist
                        else -> "Tap the test card to play Bohemian Rhapsody"
                    },
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GlassIconButton(
                icon = if (playerState.isPlaying) Icons.Filled.Pause
                else Icons.Filled.PlayArrow,
                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                onClick = { playerManager.togglePlayPause() },
                enabled = hasTrack,
                size = 48.dp,
                iconSize = 24.dp,
                selected = hasTrack,
            )
        }
    }
}
