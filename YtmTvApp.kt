package com.ppp65isking.ytmusicfortv

import android.app.Application
import com.ppp65isking.ytmusicfortv.innertube.AuthStore
import com.ppp65isking.ytmusicfortv.innertube.InnerTubeClient
import com.ppp65isking.ytmusicfortv.innertube.StreamResolver
import com.ppp65isking.ytmusicfortv.playback.PlayerManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Holds the app's singletons. The fragile parts are isolated by design:
 * everything YouTube-shaped lives in `innertube/` (one module to touch when
 * the API changes), everything playback-shaped in `playback/`.
 */
class YtmTvApp : Application() {

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val authStore: AuthStore by lazy { AuthStore(this) }

    val innerTube: InnerTubeClient by lazy { InnerTubeClient(okHttp, authStore) }

    val streamResolver: StreamResolver by lazy { StreamResolver(innerTube) }

    val playerManager: PlayerManager by lazy { PlayerManager(this) }
}
