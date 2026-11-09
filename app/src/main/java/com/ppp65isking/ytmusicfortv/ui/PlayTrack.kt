package com.ppp65isking.ytmusicfortv.ui

import com.ppp65isking.ytmusicfortv.innertube.StreamResolver
import com.ppp65isking.ytmusicfortv.playback.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The one play flow, shared by the test card, Search and Library:
 * resolve the stream on IO (StreamResolver is blocking), then hand it
 * to PlayerManager — which also records it in Recently played.
 */
suspend fun resolveAndPlay(
    videoId: String,
    title: String,
    artist: String,
    streamResolver: StreamResolver,
    playerManager: PlayerManager,
) {
    val stream = withContext(Dispatchers.IO) { streamResolver.resolve(videoId) }
    playerManager.playStream(stream, title, artist, videoId)
}
