package com.ppp65isking.ytmusicfortv.playback

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ppp65isking.ytmusicfortv.YtmTvApp

/**
 * MediaSessionService so audio keeps playing when the user leaves the app
 * (background play is a Phase-3 feature; the service exists now so the
 * player wiring is real, not a stub).
 *
 * The Application owns the ExoPlayer lifecycle — this service only owns
 * the MediaSession and must NOT release the player.
 */
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = (application as YtmTvApp).playerManager.player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
