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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ppp65isking.ytmusicfortv.innertube.InnerTubeClient
import com.ppp65isking.ytmusicfortv.innertube.Renderers
import com.ppp65isking.ytmusicfortv.innertube.Song
import com.ppp65isking.ytmusicfortv.innertube.StreamResolver
import com.ppp65isking.ytmusicfortv.playback.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real search: types into a text field (focusing it opens the TV system
 * keyboard), searches InnerTube on submit, and shows the songs as glass
 * rows. Tapping a row resolves it and plays it — the same flow as the
 * test card. Search runs against the WEB_REMIX client; results are
 * parsed by [Renderers.parseSearchSongs].
 */
@Composable
fun SearchScreen(
    innerTube: InnerTubeClient,
    streamResolver: StreamResolver,
    playerManager: PlayerManager,
    navPosition: NavPosition,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var resolvingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val fieldFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }
    // The search field exits to the nav: Left in rail mode, Up in
    // top-bar mode (it's the topmost element on this screen).
    val fieldExitModifier = if (navPosition == NavPosition.LEFT) {
        Modifier.navExitOnLeft()
    } else {
        Modifier.navExitOnUp()
    }

    fun doSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        scope.launch {
            searching = true
            searchError = null
            try {
                val response = withContext(Dispatchers.IO) {
                    innerTube.search(q, InnerTubeClient.SearchParams.SONGS)
                }
                results = Renderers.parseSearchSongs(response)
                searched = true
            } catch (e: Exception) {
                searchError = e.message ?: "Search failed"
            } finally {
                searching = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 48.dp, end = 48.dp, top = 48.dp, bottom = 48.dp),
    ) {
        // Search field + button.
        Row(
            modifier = Modifier.then(fieldExitModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(fieldFocusRequester),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 24.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                cursorBrush = SolidColor(Color(0xFFA78BFA)),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
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
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(Color(0xFFC4B5FD)),
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search songs, artists…",
                                    color = Color(0xFF8A7FB8),
                                    fontSize = 24.sp,
                                )
                            }
                            innerTextField()
                        }
                    }
                },
            )
            GlassIconButton(
                icon = Icons.Filled.Search,
                contentDescription = "Search",
                onClick = { doSearch() },
                size = 56.dp,
                iconSize = 26.dp,
                selected = searching,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            searching && results.isEmpty() -> {
                // First search: skeleton rows in the exact TrackRow
                // geometry, so the layout is already shaped when the
                // results land and focus has somewhere stable to go.
                Text(
                    text = "Searching…",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(5) { SkeletonRow() }
                }
            }
            searching -> {
                // Re-search with old results on screen: keep them (and
                // the focus on them) and show the spinner state above.
                Text(
                    text = "Searching…",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                SearchResultsList(
                    results = results,
                    resolvingId = resolvingId,
                    navPosition = navPosition,
                    firstResultFocusRequester = firstResultFocusRequester,
                    onPlay = { song ->
                        scope.launch {
                            resolvingId = song.videoId
                            try {
                                resolveAndPlay(
                                    song.videoId,
                                    song.title,
                                    song.artist,
                                    streamResolver,
                                    playerManager,
                                )
                            } catch (e: Exception) {
                                searchError = e.message ?: "Couldn't play that song"
                            } finally {
                                resolvingId = null
                            }
                        }
                    },
                )
            }
            searchError != null -> {
                Text(
                    text = "Search failed: $searchError",
                    color = Color(0xFFFCA5A5),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 24.sp,
                )
            }
            searched && results.isEmpty() -> {
                Text(
                    text = "No results for \"$query\"",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try a different song or artist.",
                    color = Color(0xFF8A7FB8),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 24.sp,
                )
            }
            searched -> {
                SearchResultsList(
                    results = results,
                    resolvingId = resolvingId,
                    navPosition = navPosition,
                    firstResultFocusRequester = firstResultFocusRequester,
                    onPlay = { song ->
                        scope.launch {
                            resolvingId = song.videoId
                            try {
                                resolveAndPlay(
                                    song.videoId,
                                    song.title,
                                    song.artist,
                                    streamResolver,
                                    playerManager,
                                )
                            } catch (e: Exception) {
                                searchError = e.message ?: "Couldn't play that song"
                            } finally {
                                resolvingId = null
                            }
                        }
                    },
                )
            }
            else -> {
                Text(
                    text = "Search YouTube Music",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Type a song or artist above, then press Search.",
                    color = Color(0xFF8A7FB8),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 24.sp,
                )
            }
        }
    }

    // Open on the field so the TV keyboard is one tap away. Retry: the
    // field composes with the screen, but the retry costs nothing.
    LaunchedEffect(Unit) {
        repeat(10) {
            if (fieldFocusRequester.requestFocus()) return@LaunchedEffect
            delay(100)
        }
    }

    // When results land, move focus to the first one (with retry — the
    // list composes after the data does).
    LaunchedEffect(searched, results) {
        if (searched && results.isNotEmpty()) {
            repeat(10) {
                if (firstResultFocusRequester.requestFocus()) return@LaunchedEffect
                delay(100)
            }
        }
    }
}

/** The results list, shared by the searching-with-old-results state. */
@Composable
private fun SearchResultsList(
    results: List<Song>,
    resolvingId: String?,
    navPosition: NavPosition,
    firstResultFocusRequester: FocusRequester,
    onPlay: (Song) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(results, key = { _, song -> song.videoId }) { index, song ->
            TrackRow(
                title = song.title,
                artist = song.artist,
                subtitle = song.durationText,
                index = index,
                resolving = resolvingId == song.videoId,
                onClick = { onPlay(song) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (navPosition == NavPosition.LEFT) {
                            Modifier.navExitOnLeft()
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (index == 0) {
                            Modifier.focusRequester(firstResultFocusRequester)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

/**
 * Focus-safe loading placeholder: the exact TrackRow geometry (16dp
 * padding + 56dp artwork + two text lines) as plain grey blocks — NOT
 * focusable, so the D-pad can never land on a skeleton.
 */
@Composable
private fun SkeletonRow() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.02f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = Glass.BorderDim),
                shape = RoundedCornerShape(24.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                )
            }
        }
    }
}
