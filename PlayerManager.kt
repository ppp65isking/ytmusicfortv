package com.ppp65isking.ytmusicfortv.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ppp65isking.ytmusicfortv.innertube.StreamResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Owns the single ExoPlayer instance. Audio-only: no video surface, no
 * PlayerView — the TV UI is pure Compose.
 *
 * Playback order: prefer the HLS manifest (ExoPlayer handles the adaptive
 * audio rendition), fall back to the direct itag URL.
 */
class PlayerManager(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = error.message ?: "Playback error",
            )
        }
    }

    init {
        player.addListener(listener)
    }

    fun playStream(stream: StreamResult, title: String, artist: String) {
        val url = stream.hlsUrl ?: stream.directUrl
        if (url == null) {
            _state.value = _state.value.copy(error = "No playable stream URL")
            return
        }
        _state.value = PlayerUiState(title = title, artist = artist, isLoading = true)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
        _state.value = _state.value.copy(isLoading = false)
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stop() {
        player.stop()
        _state.value = PlayerUiState()
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}
