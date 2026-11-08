package com.ppp65isking.ytmusicfortv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ppp65isking.ytmusicfortv.innertube.StreamResolver
import com.ppp65isking.ytmusicfortv.playback.PlayerManager
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            // --- Left sidebar nav (a setting will switch this to a top bar) ---
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .padding(start = 32.dp, top = 32.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "YTMusic for TV",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFA78BFA),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                NAV_ITEMS.forEach { item ->
                    Button(
                        onClick = { selectedNav = item },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = item)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (selectedNav == "Home") "Home" else "“$selectedNav” — coming soon",
                    color = Color.Gray,
                )
            }

            // --- Content rows ---
            Surface(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 32.dp, horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    item {
                        Column {
                            Text(
                                text = "Test playback",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
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
                                    .width(320.dp)
                                    .focusRequester(firstCardFocusRequester),
                                colors = CardDefaults.colors(
                                    containerColor = Color(0xFF6D28D9),
                                ),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
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
                                    )
                                    resolveError?.let { error ->
                                        Text(text = "Error: $error", color = Color(0xFFFCA5A5))
                                    }
                                }
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
            }
        }

        // --- Now-playing bar (current song + playback controls) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = when {
                    playerState.title.isNotEmpty() ->
                        "${playerState.title} — ${playerState.artist}"
                    else -> "Nothing playing yet"
                },
                modifier = Modifier.weight(1f),
            )
            if (playerState.isLoading) {
                Text(text = "Loading…", color = Color.Gray)
            }
            Button(
                onClick = { playerManager.togglePlayPause() },
                enabled = playerState.title.isNotEmpty(),
            ) {
                Text(text = if (playerState.isPlaying) "Pause" else "Play")
            }
        }
    }

    LaunchedEffect(Unit) {
        firstCardFocusRequester.requestFocus()
    }
}

@Composable
private fun ContentRow(title: String, items: List<String>) {
    var isRowFocused by remember { mutableStateOf(false) }
    Column {
        Text(
            text = title,
            color = if (isRowFocused) Color.White else Color.Gray,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.onFocusChanged { isRowFocused = it.hasFocus },
        ) {
            itemsIndexed(items) { _, item ->
                Card(
                    onClick = { },
                    modifier = Modifier.width(200.dp),
                    colors = CardDefaults.colors(containerColor = Color.DarkGray),
                ) {
                    Text(text = item, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
