package com.ppp65isking.ytmusicfortv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ppp65isking.ytmusicfortv.innertube.StreamResolver
import com.ppp65isking.ytmusicfortv.playback.PlayerManager
import com.ppp65isking.ytmusicfortv.playback.Track
import kotlinx.coroutines.launch

/**
 * The LOCAL library (no account sign-in yet): Recently played (last 20,
 * recorded on every play) and Liked songs (the heart on the bottom bar
 * and the full-screen view). Tapping any row plays it. Empty shelves
 * get a friendly note, never an error.
 */
@Composable
fun LibraryScreen(
    playerManager: PlayerManager,
    streamResolver: StreamResolver,
    navPosition: NavPosition,
    modifier: Modifier = Modifier,
) {
    val libVersion by playerManager.libraryStore.version.collectAsState()
    val recent = remember(libVersion) { playerManager.libraryStore.recent() }
    val liked = remember(libVersion) { playerManager.libraryStore.liked() }
    var resolvingId by remember { mutableStateOf<String?>(null) }
    var playError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun play(track: Track) {
        scope.launch {
            resolvingId = track.videoId
            playError = null
            try {
                resolveAndPlay(
                    track.videoId,
                    track.title,
                    track.artist,
                    streamResolver,
                    playerManager,
                )
            } catch (e: Exception) {
                playError = e.message ?: "Couldn't play that song"
            } finally {
                resolvingId = null
            }
        }
    }

    // Full-width rows: Left reaches the rail in rail mode.
    val rowExitModifier = if (navPosition == NavPosition.LEFT) {
        Modifier.navExitOnLeft()
    } else {
        Modifier
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 48.dp, end = 48.dp, top = 48.dp, bottom = 48.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Recently played",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 26.sp,
                color = Color.White,
            )
        }
        if (recent.isEmpty()) {
            item {
                Text(
                    text = "Nothing here yet — play a song and it'll show up here.",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 24.sp,
                )
            }
        } else {
            items(recent, key = { it.videoId }) { track ->
                TrackRow(
                    title = track.title,
                    artist = track.artist,
                    subtitle = null,
                    index = recent.indexOf(track),
                    resolving = resolvingId == track.videoId,
                    onClick = { play(track) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(rowExitModifier),
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Liked songs",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 26.sp,
                color = Color.White,
            )
        }
        if (liked.isEmpty()) {
            item {
                Text(
                    text = "No liked songs yet — tap the heart on anything playing.",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 24.sp,
                )
            }
        } else {
            items(liked, key = { it.videoId }) { track ->
                TrackRow(
                    title = track.title,
                    artist = track.artist,
                    subtitle = null,
                    index = liked.indexOf(track),
                    resolving = resolvingId == track.videoId,
                    onClick = { play(track) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(rowExitModifier),
                )
            }
        }
        playError?.let { error ->
            item {
                Text(
                    text = error,
                    color = Color(0xFFFCA5A5),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 24.sp,
                )
            }
        }
    }
}
