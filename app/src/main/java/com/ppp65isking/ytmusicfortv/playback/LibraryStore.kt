package com.ppp65isking.ytmusicfortv.playback

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** One playable track, wherever it came from (search, library, test card). */
data class Track(
    val videoId: String,
    val title: String,
    val artist: String,
)

/**
 * The LOCAL library (no account sign-in yet — the app has no auth).
 *
 * Backed by SharedPreferences, same pattern as AuthStore. Holds the last
 * 20 played tracks (most recent first, de-duplicated) and the liked
 * tracks. Stored as JSON arrays so titles with odd characters survive.
 *
 * [version] is a process-wide flow bumped on every mutation: the UI
 * collects it and re-reads the lists. It lives in the companion object
 * so the instance PlayerManager owns and the instances the UI builds
 * from LocalContext all see the same ticks.
 */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("ytm_library", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    val version: StateFlow<Int> get() = versionFlow

    fun recent(): List<Track> = decode(prefs.getString(KEY_RECENT, null))

    fun liked(): List<Track> = decode(prefs.getString(KEY_LIKED, null))

    fun isLiked(videoId: String): Boolean =
        liked().any { it.videoId == videoId }

    /** Record a play: newest first, de-duplicated, capped at 20. */
    fun recordPlayed(track: Track) {
        val updated = (listOf(track) + recent().filter { it.videoId != track.videoId })
            .take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT, encode(updated)).apply()
        bump()
    }

    /** Toggle the heart. Returns the new liked state. */
    fun toggleLiked(track: Track): Boolean {
        val current = liked()
        val updated = if (current.any { it.videoId == track.videoId }) {
            current.filter { it.videoId != track.videoId }
        } else {
            listOf(track) + current
        }
        prefs.edit().putString(KEY_LIKED, encode(updated)).apply()
        bump()
        return updated.any { it.videoId == track.videoId }
    }

    fun clearRecent() {
        prefs.edit().putString(KEY_RECENT, encode(emptyList())).apply()
        bump()
    }

    fun clearLiked() {
        prefs.edit().putString(KEY_LIKED, encode(emptyList())).apply()
        bump()
    }

    private fun encode(tracks: List<Track>): String =
        buildJsonArray {
            tracks.forEach { track ->
                addJsonObject {
                    put("videoId", track.videoId)
                    put("title", track.title)
                    put("artist", track.artist)
                }
            }
        }.toString()

    private fun decode(raw: String?): List<Track> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
                val obj = element.jsonObject
                val id = obj["videoId"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                Track(
                    videoId = id,
                    title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    artist = obj["artist"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun bump() {
        versionFlow.value = versionFlow.value + 1
    }

    companion object {
        private const val KEY_RECENT = "recent_tracks"
        private const val KEY_LIKED = "liked_tracks"
        private const val MAX_RECENT = 20
        private val versionFlow = MutableStateFlow(0)
    }
}
