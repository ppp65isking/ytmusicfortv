package com.ppp65isking.ytmusicfortv.innertube

import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** A resolved, playable audio stream. URLs expire in ~6h — resolve fresh on
 *  every play, never cache them. */
data class StreamResult(
    /** HLS master manifest — hand to ExoPlayer (preferred). */
    val hlsUrl: String?,
    /** Best direct audio URL (itag 251 Opus, else 140 AAC, else best bitrate). */
    val directUrl: String?,
    val itag: Int?,
    val expiresInSeconds: Long?,
)

/**
 * Player endpoint via the VISIONOS client (the only client returning plain
 * URLs as of Nov 2026 — see Clients.kt / phase1-report.md).
 *
 * Phase 1 proof: itag 251 (Opus ~148 kbps) range-fetched at 1.18 MB/s and
 * decoded cleanly; HLS master + itag-140 AAC rendition likewise.
 */
class StreamResolver(private val client: InnerTubeClient) {

    fun resolve(videoId: String): StreamResult {
        val player = client.post("player", Clients.VISIONOS) {
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }
        val status = player["playabilityStatus"]
            ?.jsonObject?.get("status")
            ?.jsonPrimitive?.contentOrNull
        require(status == "OK") { "player playabilityStatus=$status for $videoId" }
        val streamingData = player["streamingData"]?.jsonObject
            ?: error("player response has no streamingData for $videoId")
        val hls = streamingData["hlsManifestUrl"]?.jsonPrimitive?.contentOrNull
        val audio = streamingData["adaptiveFormats"]?.jsonArray.orEmpty()
            .mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .filter { format ->
                (format["mimeType"]?.jsonPrimitive?.contentOrNull ?: "")
                    .startsWith("audio/")
            }
            .filter { it.containsKey("url") }
        val best = audio.firstOrNull { it["itag"]?.jsonPrimitive?.intOrNull == 251 }
            ?: audio.firstOrNull { it["itag"]?.jsonPrimitive?.intOrNull == 140 }
            ?: audio.maxByOrNull { it["bitrate"]?.jsonPrimitive?.longOrNull ?: 0L }
        return StreamResult(
            hlsUrl = hls,
            directUrl = best?.get("url")?.jsonPrimitive?.contentOrNull,
            itag = best?.get("itag")?.jsonPrimitive?.intOrNull,
            expiresInSeconds = streamingData["expiresInSeconds"]
                ?.jsonPrimitive?.longOrNull,
        )
    }
}
