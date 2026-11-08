package com.ppp65isking.ytmusicfortv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ppp65isking.ytmusicfortv.ui.HomeScreen
import com.ppp65isking.ytmusicfortv.ui.theme.YtmTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as YtmTvApp
        setContent {
            YtmTvTheme {
                HomeScreen(
                    playerManager = app.playerManager,
                    streamResolver = app.streamResolver,
                )
            }
        }
    }
}
