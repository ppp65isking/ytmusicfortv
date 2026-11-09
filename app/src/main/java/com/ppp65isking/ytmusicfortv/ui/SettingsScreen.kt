package com.ppp65isking.ytmusicfortv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ppp65isking.ytmusicfortv.playback.PlayerManager

/**
 * Settings: nav position (left rail vs top bar — the original feature
 * list), About, clear-your-data rows, and an honest sign-in row.
 * Sign-in itself is NOT built here — it lands in Phase 3.
 */
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    playerManager: PlayerManager,
    navPosition: NavPosition,
    modifier: Modifier = Modifier,
) {
    val navPositionState by settingsStore.navPosition.collectAsState()
    var feedback by remember { mutableStateOf<String?>(null) }
    // Full-width cards: Left reaches the rail in rail mode.
    val cardExitModifier = if (navPosition == NavPosition.LEFT) {
        Modifier.navExitOnLeft()
    } else {
        Modifier
    }
    // In top-bar mode, Up from the first card reaches the bar.
    val firstCardExitModifier = if (navPosition == NavPosition.TOP) {
        Modifier.navExitOnUp()
    } else {
        cardExitModifier
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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 28.sp,
                color = Color.White,
            )
        }

        item {
            Text(
                text = "Navigation",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 24.sp,
                color = Color(0xFFC4B5FD),
            )
        }
        item {
            GlassCard(
                onClick = { settingsStore.setNavPosition(NavPosition.LEFT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(firstCardExitModifier),
            ) {
                SettingsRowContent(
                    title = "Left rail",
                    subtitle = "Icons down the left side (current)",
                    checked = navPositionState == NavPosition.LEFT,
                )
            }
        }
        item {
            GlassCard(
                onClick = { settingsStore.setNavPosition(NavPosition.TOP) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(cardExitModifier),
            ) {
                SettingsRowContent(
                    title = "Top bar",
                    subtitle = "Icons across the top instead",
                    checked = navPositionState == NavPosition.TOP,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your data",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 24.sp,
                color = Color(0xFFC4B5FD),
            )
        }
        item {
            GlassCard(
                onClick = {
                    playerManager.libraryStore.clearRecent()
                    feedback = "Recently played cleared"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(cardExitModifier),
            ) {
                SettingsRowContent(
                    title = "Clear recently played",
                    subtitle = "Empties the Recently played shelf",
                    checked = false,
                )
            }
        }
        item {
            GlassCard(
                onClick = {
                    playerManager.libraryStore.clearLiked()
                    feedback = "Liked songs cleared"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(cardExitModifier),
            ) {
                SettingsRowContent(
                    title = "Clear liked songs",
                    subtitle = "Removes every heart",
                    checked = false,
                )
            }
        }
        feedback?.let { message ->
            item {
                Text(
                    text = message,
                    color = Color(0xFFA78BFA),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 24.sp,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 24.sp,
                color = Color(0xFFC4B5FD),
            )
        }
        item {
            // Not a button on purpose: nothing to do here until Phase 3.
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
                    )
                    .padding(20.dp),
            ) {
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 24.sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Coming in Phase 3 — your account library needs it.",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 24.sp,
                color = Color(0xFFC4B5FD),
            )
        }
        item {
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
                    )
                    .padding(20.dp),
            ) {
                Text(
                    text = "YTMusic for TV",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 24.sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version 0.1.0",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personal use only — not affiliated with YouTube or Google.",
                    color = Color(0xFFC4B5FD),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                )
            }
        }
    }
}

/** A settings row: title + subtitle, with a check mark when selected. */
@Composable
private fun SettingsRowContent(
    title: String,
    subtitle: String,
    checked: Boolean,
) {
    Row(
        modifier = Modifier.padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 24.sp,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color(0xFFC4B5FD),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 24.sp,
            )
        }
        if (checked) {
            Image(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(Color(0xFFA78BFA)),
            )
        }
    }
}
