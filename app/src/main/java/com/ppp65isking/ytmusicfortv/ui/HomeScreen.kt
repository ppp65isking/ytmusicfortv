package com.ppp65isking.ytmusicfortv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.ExperimentalFocusRestorerApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ppp65isking.ytmusicfortv.innertube.AuthStore
import com.ppp65isking.ytmusicfortv.innertube.InnerTubeClient
import com.ppp65isking.ytmusicfortv.innertube.StreamResolver
import com.ppp65isking.ytmusicfortv.playback.PlayerManager
import com.ppp65isking.ytmusicfortv.playback.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context.applicationContext) }
    val navPosition by settingsStore.navPosition.collectAsState()
    // Search needs its own InnerTube client (MainActivity only passes the
    // player pieces). It shares the session through AuthStore, so search
    // calls carry the same visitorData the player flow harvested.
    val innerTube = remember {
        InnerTubeClient(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build(),
            AuthStore(context.applicationContext),
        )
    }
    var selectedNav by remember { mutableStateOf("Home") }
    var showNowPlaying by remember { mutableStateOf(false) }
    // The nav rail's FocusRequester, shared through a CompositionLocal so
    // the leftmost content items can send Left straight back to the rail
    // (see navExitOnLeft in Glass.kt — focus search can't cross from a
    // lazy list into a sibling container on its own).
    val navFocusRequester = remember { FocusRequester() }

    // TV Back closes the full-screen player instead of leaving the app.
    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassBackground()
        CompositionLocalProvider(LocalNavFocusRequester provides navFocusRequester) {
            // When the full-screen player is up, the home content is NOT
            // composed at all — no competing focus targets for the D-pad
            // to walk into from the player (skill rule 6).
            if (showNowPlaying) {
                NowPlayingScreen(
                    playerManager = playerManager,
                    onClose = { showNowPlaying = false },
                )
            } else if (navPosition == NavPosition.TOP) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(
                        selectedNav = selectedNav,
                        onSelect = { selectedNav = it },
                    )
                    MainContent(
                        selectedNav = selectedNav,
                        navPosition = navPosition,
                        showNowPlaying = showNowPlaying,
                        playerManager = playerManager,
                        streamResolver = streamResolver,
                        innerTube = innerTube,
                        settingsStore = settingsStore,
                        onOpenNowPlaying = { showNowPlaying = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavRail(
                        selectedNav = selectedNav,
                        onSelect = { selectedNav = it },
                    )
                    MainContent(
                        selectedNav = selectedNav,
                        navPosition = navPosition,
                        showNowPlaying = showNowPlaying,
                        playerManager = playerManager,
                        streamResolver = streamResolver,
                        innerTube = innerTube,
                        settingsStore = settingsStore,
                        onOpenNowPlaying = { showNowPlaying = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Purple glass circle with a music note — the app's brand mark. */
@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
}

/** Left icon rail: brand mark + a floating glass panel of icon buttons. */
@Composable
private fun NavRail(
    selectedNav: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The rail's FocusRequester lives on the SELECTED item, so Left from
    // content lands on the section you're actually in.
    val navRequester = LocalNavFocusRequester.current
    Column(
        modifier = modifier
            .width(160.dp)
            .fillMaxHeight()
            .padding(start = 48.dp, top = 48.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark()
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .width(128.dp)
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
                    onClick = { onSelect(item) },
                    modifier = if (selectedNav == item && navRequester != null) {
                        Modifier.focusRequester(navRequester)
                    } else {
                        Modifier
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

/** Top nav bar (Settings → Navigation → Top bar): same icons, horizontal. */
@Composable
private fun TopBar(
    selectedNav: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navRequester = LocalNavFocusRequester.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 48.dp, top = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrandMark()
        Row(
            modifier = Modifier
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
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NAV_ITEMS.forEach { item ->
                RailItem(
                    label = item,
                    icon = navIcon(item),
                    selected = selectedNav == item,
                    onClick = { onSelect(item) },
                    modifier = if (selectedNav == item && navRequester != null) {
                        Modifier.focusRequester(navRequester)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

/**
 * The screen body under/beside the nav: Home rows, Search, Library or
 * Settings, plus the floating now-playing bar at the bottom.
 */
@Composable
private fun MainContent(
    selectedNav: String,
    navPosition: NavPosition,
    showNowPlaying: Boolean,
    playerManager: PlayerManager,
    streamResolver: StreamResolver,
    innerTube: InnerTubeClient,
    settingsStore: SettingsStore,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var resolving by remember { mutableStateOf(false) }
    var resolveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val firstCardFocusRequester = remember { FocusRequester() }
    // The hero card exits to the nav: Left in rail mode, Up in top-bar
    // mode (the topmost content item, so Up has nowhere else to go).
    val heroExitModifier = if (navPosition == NavPosition.LEFT) {
        Modifier.navExitOnLeft()
    } else {
        Modifier.navExitOnUp()
    }

    Column(modifier = modifier.fillMaxHeight()) {
        when (selectedNav) {
            "Home" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 48.dp,
                        end = 48.dp,
                        top = 48.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(40.dp),
                ) {
                    item {
                        // Hero test card — scaled to the screen: a little
                        // over half the content width, so it feels right
                        // on a big TV instead of cramped or tiny.
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val heroWidth = (maxWidth * 0.55f).coerceIn(480.dp, 680.dp)
                            GlassCard(
                                onClick = {
                                    scope.launch {
                                        resolving = true
                                        resolveError = null
                                        try {
                                            resolveAndPlay(
                                                TEST_VIDEO_ID,
                                                "Bohemian Rhapsody",
                                                "Queen",
                                                streamResolver,
                                                playerManager,
                                            )
                                        } catch (e: Exception) {
                                            resolveError = e.message ?: "Resolve failed"
                                        } finally {
                                            resolving = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(heroWidth)
                                    .then(heroExitModifier)
                                    .focusRequester(firstCardFocusRequester),
                            ) {
                                Row(
                                    modifier = Modifier.padding(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                ) {
                                    ArtworkTile(
                                        index = 0,
                                        modifier = Modifier.size(120.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Test playback",
                                            color = Glass.PurpleLight,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = 24.sp,
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = when {
                                                resolving -> "Resolving stream…"
                                                else -> "Bohemian Rhapsody — Queen"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 48.sp,
                                        )
                                        Text(
                                            text = "Resolves the real stream via InnerTube, then plays it",
                                            color = Color(0xFFD8B4FE),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 24.sp,
                                        )
                                        resolveError?.let { error ->
                                            Text(
                                                text = "Error: $error",
                                                color = Color(0xFFFCA5A5),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = 24.sp,
                                            )
                                        }
                                    }
                                    GlassIconButton(
                                        icon = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        onClick = {},
                                        enabled = false,
                                        size = 56.dp,
                                        iconSize = 28.dp,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        ContentRow(
                            title = "Trending",
                            items = TRENDING,
                            navPosition = navPosition,
                        )
                    }
                    item {
                        ContentRow(
                            title = "Your mixes",
                            items = YOUR_MIXES,
                            navPosition = navPosition,
                        )
                    }
                }
            }
            "Search" -> SearchScreen(
                innerTube = innerTube,
                streamResolver = streamResolver,
                playerManager = playerManager,
                navPosition = navPosition,
                modifier = Modifier.weight(1f),
            )
            "Library" -> LibraryScreen(
                playerManager = playerManager,
                streamResolver = streamResolver,
                navPosition = navPosition,
                modifier = Modifier.weight(1f),
            )
            "Settings" -> SettingsScreen(
                settingsStore = settingsStore,
                playerManager = playerManager,
                navPosition = navPosition,
                modifier = Modifier.weight(1f),
            )
        }

        NowPlayingBar(
            playerManager = playerManager,
            onOpenFullScreen = onOpenNowPlaying,
        )
    }

    // Initial focus, claimed with RETRY: the first attempt can land before
    // the lazy list has composed. Also re-runs when the full-screen
    // player closes, so focus is restored to the hero card (the skill's
    // "restore focus to its prior position" rule, with the hero as the
    // home default).
    LaunchedEffect(selectedNav, showNowPlaying) {
        if (selectedNav == "Home" && !showNowPlaying) {
            repeat(10) {
                if (firstCardFocusRequester.requestFocus()) return@LaunchedEffect
                delay(100)
            }
        }
    }
}

/**
 * One rail entry: a circular glass icon button with its label underneath.
 * The label is always laid out (transparent when hidden) so the layout
 * never shifts — and it stays visible in purple while selected, so you
 * can always tell where you are.
 */
@Composable
private fun RailItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassIconButton(
            icon = icon,
            contentDescription = label,
            onClick = onClick,
            selected = selected,
            onFocusChanged = { focused = it },
        )
        Text(
            text = label,
            // 24sp is the 10-foot metadata floor — 13sp was unreadable
            // from the couch. The rail is wide enough now to fit it.
            fontSize = 24.sp,
            color = when {
                selected -> Glass.PurpleLight
                focused -> Color.White
                else -> Color.Transparent
            },
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * An artwork-forward row, Apple Music style: big square artwork on top,
 * title + artist below. Clean hierarchy, generous type and spacing for
 * the couch.
 */
@OptIn(ExperimentalFocusRestorerApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun ContentRow(
    title: String,
    items: List<String>,
    navPosition: NavPosition,
) {
    var isRowFocused by remember { mutableStateOf(false) }
    Column {
        Text(
            text = title,
            color = if (isRowFocused) Color.White else Color(0xFFC4B5FD),
            style = MaterialTheme.typography.titleLarge,
            fontSize = 26.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .onFocusChanged { isRowFocused = it.hasFocus }
                // Each row remembers its own last-focused card, so Down
                // from a scrolled-right card lands on the card under it,
                // not "whatever sits at X=800dp in the next row".
                .focusRestorer(),
        ) {
            itemsIndexed(items) { index, item ->
                val parts = item.split(" — ")
                // Rail reachability: the leftmost card sends Left straight
                // to the rail — focus search can't cross from a LazyRow
                // into the sibling rail container on its own.
                val exitModifier = if (index == 0 && navPosition == NavPosition.LEFT) {
                    Modifier.navExitOnLeft()
                } else {
                    Modifier
                }
                GlassCard(
                    onClick = { },
                    modifier = Modifier
                        .width(260.dp)
                        .then(exitModifier),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ArtworkTile(
                            index = index,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = parts[0],
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 24.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (parts.size > 1) {
                            Text(
                                text = parts[1],
                                color = Color(0xFFC4B5FD),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 24.sp,
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

/**
 * The floating now-playing bar, in Verse Garden's liquid-glass pill
 * language (see [VerseGlassPill]): tinted gradient, specular highlight,
 * drifting sheen. Tapping the bar opens the full-screen player; the
 * heart likes the current track; play/pause is the raised lens button.
 *
 * Player state is collected HERE, not at the top of HomeScreen — a
 * play/pause flip recomposes this bar only, never the rails or rows.
 */
@Composable
private fun NowPlayingBar(
    playerManager: PlayerManager,
    onOpenFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerState by playerManager.state.collectAsState()
    val libVersion by playerManager.libraryStore.version.collectAsState()
    val hasTrack = playerState.title.isNotEmpty()
    val liked = hasTrack && remember(libVersion, playerState.videoId) {
        playerManager.libraryStore.isLiked(playerState.videoId)
    }
    VerseGlassPill(
        onClick = onOpenFullScreen,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 48.dp, bottom = 48.dp),
    ) {
        if (hasTrack) {
            ArtworkTile(index = 0, modifier = Modifier.size(52.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.4f)),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (hasTrack) playerState.title else "Nothing playing yet",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 24.sp,
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
                fontSize = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GlassIconButton(
            icon = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (liked) "Unlike" else "Like",
            onClick = {
                playerManager.libraryStore.toggleLiked(
                    Track(playerState.videoId, playerState.title, playerState.artist),
                )
            },
            enabled = hasTrack,
            size = 48.dp,
            iconSize = 24.dp,
            selected = liked,
            lens = liked,
        )
        GlassIconButton(
            icon = if (playerState.isPlaying) Icons.Filled.Pause
            else Icons.Filled.PlayArrow,
            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
            onClick = { playerManager.togglePlayPause() },
            enabled = hasTrack,
            size = 56.dp,
            iconSize = 28.dp,
            selected = hasTrack,
            lens = hasTrack,
        )
    }
}
