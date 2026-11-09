package com.ppp65isking.ytmusicfortv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * One track row, shared by Search results and Library lists: gradient
 * artwork tile, title + artist (plus an optional subtitle such as the
 * duration), and a "Loading…" state while its stream resolves. Focus
 * comes free from [GlassCard] — 3dp bright ring, scale, shadow.
 */
@Composable
fun TrackRow(
    title: String,
    artist: String,
    subtitle: String?,
    index: Int,
    resolving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ArtworkTile(
                index = index,
                modifier = Modifier.size(56.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = buildList {
                    if (artist.isNotEmpty()) add(artist)
                    if (!subtitle.isNullOrEmpty()) add(subtitle)
                }.joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        color = Color(0xFFC4B5FD),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (resolving) {
                Text(
                    text = "Loading…",
                    color = Color(0xFF9B8FC4),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 24.sp,
                )
            }
        }
    }
}
