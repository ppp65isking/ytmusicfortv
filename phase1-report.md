# YTM TV App — Phase 1 Report (playback proof)

**Date:** 2026-11-08. **Song probed:** Queen — "Bohemian Rhapsody" (`BSTsnWoslP4`).
**Verdict: PLAYBACK PROVED** on two independent paths (direct URL + HLS).
Fixtures: `fixtures/` in this directory. Probes: `probe_phase1.py`,
`probe_phase1b.py`, `probe_phase1c.py`, `verify_playback.py`,
`verify_playback2.py`. Lab venv: `.venv/` (ytmusicapi 1.12.3, yt-dlp 2026.10.26).

## What was proved

| Check | Result |
|---|---|
| ytmusicapi search (WEB_REMIX, unauthenticated) | 20 results, first = Queen "Bohemian Rhapsody" |
| Player call, VISIONOS client | `playabilityStatus: OK`, 5 audio formats with **plain `url`**, `hlsManifestUrl` present, `expiresInSeconds: 21540` (~6 h) |
| Direct itag 251 (Opus ~148 kbps) | Range fetch HTTP 206 at **1.18 MB/s** (no `n`-param throttling); ffprobe: **opus, 48000 Hz, stereo**; ffmpeg decoded first 2 s, rc=0 |
| HLS (itag 140 AAC rendition) | Master manifest + variant playlist fetch fine; first segment HTTP 200 (114,517 B); ffprobe: **aac, 44100 Hz, stereo**; ffmpeg decode rc=0 |
| yt-dlp 2026.10.26 cross-check | 42 formats, audio itags 139/249/250/140/251 all with plain HTTPS URLs; uses visionos player client |

## Verified client table (live from yt-dlp `_base.py`, 2026-11-08)

| clientName | id | clientVersion | User-Agent |
|---|---|---|---|
| WEB_REMIX | 67 | 1.20260707.12.00 | browser UA |
| ANDROID | 3 | 21.26.364 | `com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip` |
| IOS | 5 | 21.26.4 | `com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)` |
| **VISIONOS (player client)** | **101** | **1.02** | `Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15` (deviceModel `RealityDevice17,1`, osName `visionOS`, osVersion `26.5.23O471`) |
| ANDROID_VR | 28 | 1.65.10 | **DEAD — all formats 403 since 2026-08-17 (yt-dlp note)** |
| TVHTML5 | 7 | 7.20260707.07.00 | Cobalt UA |

## Corrections to the research plan (the plan's assumptions that turned out wrong)

1. **IOS/ANDROID are NOT usable for playback anymore.** Both return
   `playabilityStatus: OK` but `adaptiveFormats` with **no `url` and no
   `signatureCipher`** — SABR-only (`serverAbrStreamingUrl`). The plan's
   "IOS first, ANDROID fallback" is dead as of Nov 2026. **VISIONOS is the
   player client.** It returns plain URLs + `hlsManifestUrl` on both
   `music.youtube.com` and `www.youtube.com` (host does not matter).
2. **`visitorData` is REQUIRED.** A player call with no visitorData returns
   HTTP 200 with `playabilityStatus: LOGIN_REQUIRED`, reason "Sign in to
   confirm you're not a bot". Fix: take `responseContext.visitorData` from
   any prior response and send it as `context.client.visitorData` AND the
   `X-Goog-Visitor-Id` header. The Kotlin app must bootstrap visitorData
   (e.g. from a first search/browse response) and reuse it per session.
3. **TVHTML5 player is UNPLAYABLE** (`"The page needs to be reloaded."`).
   Not a fallback.
4. **ANDROID_VR is dead** (403 for all formats since 2026-08-17). Remove
   from the fallback chain. **There is currently NO proven fallback client
   after VISIONOS** — if VISIONOS goes SABR-only, the plain-URL path dies.
5. **No API key needed.** ytmusicapi's `WEB_REMIX` key is literally
   `<redacted>` in its source; requests work with no `key` param at all.
6. **WEB_REMIX clientVersion is date-generated** by ytmusicapi:
   `1.YYYYMMDD.01.00` (today: `1.20261108.01.00`). The Kotlin app should
   generate it the same way, not hardcode a stale version.
7. **Search-filter `params`** (from ytmusicapi 1.12.3
   `parsers/search.py:get_search_params`, spelling-check on):
   songs `EgWKAQIIAWoMEA4QChADEAQQCRAF`, videos `…IQ…`, albums `…IY…`,
   artists `…Ig…`, podcasts `…JQ…`, episodes `…JI…` (same prefix/suffix);
   playlists `Eg-KAQwIABAAGAAgACgBMABqChAEEAMQCRAFEAo%3D`.
8. **Lab-only sandbox quirk:** this VM's egress proxy drops requests with
   ytmusicapi's default UA (Firefox 88, 2021). Modern Chrome / iOS / Android
   UAs pass. Irrelevant on a home network — the TV app never hits this proxy.
   (yt-dlp needed `--no-check-certificates` for the same proxy reason.)
9. Stream URLs expire in ~6 h (`expiresInSeconds: 21540`) — resolve fresh
   on every play, never cache URLs. HLS master manifest is `playlist_type/DVR`
   with `itag/0` — ExoPlayer handles it, but do **not** feed the raw master
   manifest to a plain ffmpeg `-t` test (it hangs); pick an audio rendition.

## Player request that works (Kotlin reference)

- `POST https://music.youtube.com/youtubei/v1/player?prettyPrint=false`
- Headers: `Content-Type: application/json`,
  `User-Agent: <VISIONOS UA>`, `X-YouTube-Client-Name: 101`,
  `X-YouTube-Client-Version: 1.02`, `X-Goog-Api-Format-Version: 2`,
  `X-Goog-Visitor-Id: <visitorData>`
- Body: `{"context": {"client": {clientName: "VISIONOS",
  clientVersion: "1.02", deviceMake: "Apple", deviceModel: "RealityDevice17,1",
  userAgent: "<VISIONOS UA>", osName: "visionOS", osVersion: "26.5.23O471",
  hl: "en", gl: "US", visitorData: "<visitorData>"}}, "videoId": "<id>",
  "contentCheckOk": true, "racyCheckOk": true}` — anonymous, no auth headers.
- Pick audio: itag **251** (Opus ~148 kbps) first, fallback **140** (AAC 128k).
  Or hand `streamingData.hlsManifestUrl` to ExoPlayer (HLS module).

## Still to prove on-device (Anthony's Chromecast with Google TV 4K)

- The VISIONOS HLS/direct URLs play in **ExoPlayer on the actual TV**
  (ffmpeg proves the bytes; ExoPlayer on-device is the last mile).
- Whether a residential IP behaves the same (this probe ran from a
  datacenter egress IP; visitorData fixed the bot-check here).
