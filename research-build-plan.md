# YTM TV App — Research & Build Plan

**Project:** a real Google TV / Android TV music app for personal use only (sideloaded, never Play Store), playing YouTube Music via the unofficial community approach.
**Status:** RESEARCH ONLY — no code written, no repo created. Awaiting Anthony's feature list.
**Researched:** 2026-11-08. InnerTube constants rotate — re-verify client versions against `yt-dlp` at build time.

---

## 1. TL;DR — recommended architecture

**Build a native Kotlin Android TV app that calls YouTube Music's InnerTube API directly from the device (Option A). No backend server.**

- The Python library `ytmusicapi` **cannot run inside an Android app** — it is Python. It is used as (a) the living spec for request/response shapes, and (b) a laptop prototyping harness to verify calls and save test fixtures before porting them to Kotlin.
- This is exactly the pattern proven by the open-source YTM clients **InnerTune**, **OuterTune**, and **Metrolist**: Kotlin + OkHttp + direct InnerTube calls.
- **UI:** Jetpack **Compose for TV** (not Leanback — Leanback is deprecated). **Playback:** Jetpack **Media3 / ExoPlayer**.
- **Dual-client trick (the key decision):** use `WEB_REMIX` for search/browse metadata, but a **mobile client (`IOS` first, `ANDROID` fallback)** for the `player` endpoint, because mobile clients return plain playable stream URLs while web clients return ciphered/SABR formats.
- **Auth:** browser-cookie headers only. OAuth is broken upstream since 2025-08-29 — do not build on it.
- **Distribution:** sideloaded APK only (debug APK from GitHub Actions is enough to start; one signing key reused for updates).

---

## 2. Architecture options

### Option A — Kotlin app calls InnerTube directly (RECOMMENDED)

| | |
|---|---|
| **How** | Kotlin + OkHttp/Retrofit. Port the InnerTube calls to Kotlin, following the InnerTune/OuterTune pattern. `ytmusicapi` source is the spec; a laptop Python harness verifies shapes first. |
| **Pros** | No server to host, pay for, or maintain. No extra network hop. No cookies living on a server. Self-contained app. Proven by InnerTune (~6.1k stars), OuterTune (~5.4k), Metrolist (~14k). |
| **Cons** | Must reimplement request/response parsing in Kotlin. Fragile JSON trees live in the app. Stream-resolution complexity (client fallback, `n`-param) lives in the app. |
| **Cost** | $0. One GitHub repo, one Actions workflow. |

### Option B — Python backend running ytmusicapi + thin TV client (NOT recommended)

| | |
|---|---|
| **How** | A Python service (FastAPI/Flask + `ytmusicapi`) somewhere always-on; the TV app calls it over HTTP. |
| **Pros** | `ytmusicapi` handles all parsing/auth/stream logic. TV app stays thin. YouTube breakage fixed in one place (update the library). |
| **Cons** | Needs an always-on server (a spare computer, Raspberry Pi, or paid cloud host). The user's Google session cookies live on a server (worse security). Extra failure point + latency. Same cookie-expiry problem as Option A. Overkill for one user on one TV. |
| **Cost** | Hosting + maintenance, forever. |

### Verdict

**Option A.** For one user on one TV, a server is pure overhead. The hard parts (parsing, stream resolution) are isolated in a single `innertube` module inside the app, with lenient JSON parsing (`ignoreUnknownKeys`, tree-walking over `JsonElement` instead of full data classes).

### Hybrid prototyping workflow (do this regardless)

1. On a laptop: `pip install ytmusicapi==1.12.3`, run unauthenticated search/browse/player probes, save the raw JSON as fixtures.
2. Generate `browser.json` via `ytmusicapi browser` (Anthony's step — his cookies), probe library/liked/playlists, save fixtures.
3. Port each call to Kotlin against the fixtures. The Kotlin client must produce identical results before the UI is built on top of it.

---

## 3. InnerTube API reference (YouTube Music)

InnerTube (`youtubei`) is YouTube's private internal API — no public docs, everything reverse-engineered. All calls are `POST` with a JSON body `{"context": {...}, <endpoint fields>}`.

### 3.1 Transport

- **Base URL (Music):** `https://music.youtube.com/youtubei/v1/<endpoint>?key=<apiKey>&prettyPrint=false`
- **API key:** a public, non-rotating key baked into the web client. It is **optional** — yt-dlp and NewPipe send no key at all; the request is authenticated by the `context.client` block + headers. **Do not invent a key.** At build time, read the `WEB_REMIX` key from `ytmusicapi`'s source if a key is wanted, or omit it.
- **Host for Music:** `music.youtube.com`. (Other hosts: `www.youtube.com`, `youtubei.googleapis.com`.)

### 3.2 The `context` block (minimum viable)

```json
{
  "context": {
    "client": {
      "clientName": "WEB_REMIX",
      "clientVersion": "1.20260114.03.00",
      "hl": "en",
      "gl": "US",
      "visitorData": "<from a previous responseContext>",
      "userAgent": "<must match the client>"
    },
    "user": { "lockedSafetyMode": false }
  }
}
```

`visitorData` is echoed back from `responseContext.visitorData` on the first response — reuse one per session. It is a base64url protobuf; it can also be generated locally, but prefer the server-issued value.

### 3.3 Required headers

| Header | Value |
|---|---|
| `Content-Type` | `application/json` |
| `X-YouTube-Client-Name` | numeric client id (see table) |
| `X-YouTube-Client-Version` | client version string |
| `User-Agent` | must match the client (esp. ANDROID/IOS) |
| `X-Goog-Visitor-Id` | `<visitorData>` when you have one |
| `X-Goog-Api-Format-Version` | `2` (mobile clients) |
| `Origin` / `Referer` | `https://music.youtube.com` (web clients) |
| Authed only: `Authorization` | `SAPISIDHASH <ts>_<sha1>` (see §4) |
| Authed only: `X-Goog-AuthUser` | `0` |
| Authed only: `Cookie` | session cookies incl. `__Secure-3PAPISID` |

### 3.4 Client table (as of Jan–Jun 2026 — RE-VERIFY from yt-dlp `_base.py` at build time)

| clientName | id | clientVersion (≈2026) | Stream URLs? | po_token? | Notes |
|---|---|---|---|---|---|
| **WEB_REMIX** (Music web) | 67 | `1.20260114.03.00` | ciphered / SABR | required (GVS) | Use for **metadata only** (search/browse). |
| **WEB** | 1 | `2.20260114.08.00` | ciphered | required | General YouTube web. |
| **ANDROID** | 3 | `21.02.35`–`21.03.36` | **plain `url`** | not required w/ player-token | Use for **player**. Rejects OAuth Bearer (400) — call anonymously. |
| **IOS** | 5 | `20.49.6`–`21.03.2` | **plain `url` + `hlsManifestUrl`** | not required w/ player-token | **Best player client** — hand `hlsManifestUrl` straight to ExoPlayer. |
| **ANDROID_VR** | 28 | `1.65.10` | plain `url` | none | yt-dlp's primary default. ⚠ `>1.65` may be forced onto SABR. |
| **TVHTML5** | 7 | `7.20260114.12.00` | partial (ciphered) | recommended | Living-room client; used for authed browse. |
| **ANDROID_MUSIC** | 21 | — | — | — | Native Android YTM client id. |
| **IOS_MUSIC** | 26 | — | — | — | Native iOS YTM client id. |
| **TVHTML5_SIMPLY_EMBEDDED_PLAYER** | 85 | `2.0` | — | — | Age-gate bypass for embeddable videos. |

### 3.5 Endpoints

All `POST https://music.youtube.com/youtubei/v1/<endpoint>?prettyPrint=false`.

| Endpoint | Purpose | Key body fields | Response highlights |
|---|---|---|---|
| **`search`** | Search songs/artists/albums/playlists | `query`, `params` (base64-protobuf filter, optional) | `contents.tabbedSearchResultsRenderer.tabs[].tabRenderer.content.sectionListRenderer.contents[]` → `musicShelfRenderer` / `musicCardShelfRenderer` |
| **`browse`** | Home, library, playlists, artist, album | `browseId`, `params`, `continuation` | `contents.singleColumnBrowseResultsRenderer.tabs[].tabRenderer.content.sectionListRenderer.contents[]` → `musicShelfRenderer` / `gridRenderer` / `musicCarouselShelfRenderer` |
| **`player`** | Stream URLs + metadata | `videoId`, `contentCheckOk: true`, `racyCheckOk: true`, `playbackContext.contentPlaybackContext.signatureTimestamp` (only if deciphering) | `playabilityStatus`, `videoDetails`, `streamingData` |
| **`next`** | Queue / radio / up-next | `videoId`, `playlistId` | `contents.singleColumnWatchNextResults.playlist.playlistPanelRenderer.contents[]` → `playlistPanelVideoRenderer` |
| **`music/get_search_suggestions`** | Search autocomplete | `input` | suggestions list |
| **`music/get_queue`** | Playback queue info | `videoIds`, `playlistId` | queue contents |

**Pagination:** find `continuationItemRenderer.continuationEndpoint.continuationCommand.token` in a response, then re-POST the **same endpoint** with body `{"context": {...}, "continuation": "<token>"}`. Items arrive under `onResponseReceivedActions[].appendContinuationItemsAction.continuationItems`.

**Response parsing:** responses are trees of `renderer` objects — walk them by key name. Text fields are `{"simpleText": "…"}` or `{"runs": [{"text": "…"}]}` (concatenate runs). Thumbnails live in `thumbnail.thumbnails[]` (pick by width). **Do not model the whole tree as data classes** — use `JsonElement` tree-walking with null-safe access; the shape changes 2–4×/year.

### 3.6 Browse IDs (YouTube Music)

| browseId | Meaning |
|---|---|
| `FEmusic_home` | Personalized home / mixes |
| `FEmusic_library_landing` | Library landing |
| `FEmusic_liked_videos` | Liked songs |
| `FEmusic_library_privately_owned_playlists` | User's playlists |
| `VL{playlistId}` | Specific playlist's tracks (prefix playlist id with `VL`) |
| `UC…` | Artist / channel page |
| `MPRE…` | Album page |
| `FEmusic_charts` / `VLOLAK5uy_…` | Charts (verify id at build time) |

### 3.7 Player / stream URLs — the hard part

`player.streamingData` contains:
- **`formats`** — progressive (muxed audio+video), e.g. itag 18/22. Not what we want for music.
- **`adaptiveFormats`** — separate audio-only / video-only. Audio itags (verify against yt-dlp at build time): **251 (Opus ~160 kbps — preferred)**, 171 (Opus ~128k), **140 (AAC 128k — safe fallback)**, 249/250 (Opus low). Pick 251 first, fall back to 140.
- **`hlsManifestUrl` / `dashManifestUrl`** — server-built manifests. IOS client returns `hlsManifestUrl`; hand it directly to ExoPlayer (HLS support via `media3-exoplayer-hls`).
- **URL expiry:** stream URLs expire in ~**6 hours**. Fetch fresh on every play. **Never cache stream URLs** — cache only `videoId`s.

**Which URL you get depends on the client you pretend to be:**
- **ANDROID / IOS / ANDROID_VR → plain `url`** (directly playable, no deciphering).
- **WEB / WEB_REMIX / TVHTML5 → `signatureCipher`** (`url=…&s=<cipher>&sp=sig`) — the `s` must be transformed by a function extracted from the player's `base.js`, and **SABR**: web clients increasingly get `serverAbrStreamingUrl` only, with `adaptiveFormats` arriving **without a `url`** at all. **Do not use web clients for playback.**
- **`n` (throttling) param:** present on essentially all stream URLs. Without transforming it, downloads are throttled to ~50 KB/s. The transform needs the `nsig` function from `base.js` (needs a JS engine). **Mitigation:** prefer the **IOS `hlsManifestUrl`** path — HLS segments are server-signed and the `n` problem is largely sidestepped (verify on-device; if throttled, the manifest URL's own `n` may still need solving). For audio-only (160 kbps ≈ 20 KB/s), throttling is also less likely to starve playback than for video — **test this, don't assume it**.
- **po_token / BotGuard:** YouTube gates many requests behind a Proof-of-Origin token. As of yt-dlp 2026.06, **ANDROID / IOS need no po_token** (`not_required_with_player_token`), ANDROID_VR needs none at all; **WEB / WEB_REMIX / MWEB require it**. This is another reason to use mobile clients for `player`. **Do not implement BotGuard** — if YouTube ever requires po_token for mobile clients, that is a project-killer for a thin client (would need a token-minter service — out of scope).
- **Critical gotcha:** always call `player` **anonymously**, even when logged in. The ANDROID/IOS `player` endpoint rejects `Authorization: Bearer` with `400 INVALID_ARGUMENT`. Personalization lives on `browse`/`next` only — playback is effectively always anonymous. There is **no known authenticated-playback recipe**.
- **`playabilityStatus.status`:** `OK` / `LOGIN_REQUIRED` / `AGE_VERIFICATION_REQUIRED` / `UNPLAYABLE` / `LIVE_STREAM_OFFLINE` + `.reason`. Handle `UNPLAYABLE` by trying the next fallback client.

**Fallback order for stream resolution (try in order, stop at first playable):**
1. `IOS` → `streamingData.hlsManifestUrl` (or best audio `adaptiveFormats[].url`)
2. `ANDROID` → best audio `adaptiveFormats[].url` (itag 251 → 140)
3. `ANDROID_VR` → same as ANDROID

### 3.8 What the official YouTube Data API v3 can/can't do (for comparison)

- Free tier: 10,000 quota units/day. Covers search/playlists/videos metadata.
- **Cannot do:** audio-only streaming, library/liked songs, playback control, YTM-specific features. That is why the unofficial route is needed at all.

---

## 4. Auth — current state (verified 2026-11-08)

### 4.1 What works unauthenticated

Search, browse (home/charts/explore), and `player` all work anonymously. **Library, liked songs, user playlists, and any playlist write fail without auth.**

### 4.2 OAuth is BROKEN — do not build on it

- Since **2025-08-29**, the YTM inner API rejects OAuth tokens with **HTTP 400 "Request contains an invalid argument"** on **all** requests, including `search`. (`sigma67/ytmusicapi` issue **#813**; also #921, #682.) Still broken as of Aug 2026 on ytmusicapi 1.12.2.
- Root cause per maintainers: a token from a "TV and Limited Input devices" client is only accepted under the `TVHTML5` client context, while ytmusicapi hardcodes `WEB_REMIX`.
- The same OAuth token **still works** with the official YouTube Data API v3 — just not with InnerTube. Irrelevant for this app, but worth knowing.
- ytmusicapi's own test suite skips OAuth tests (`@pytest.mark.skip(reason = "oauth is currently not working, see #813")`).

### 4.3 Browser-headers (cookie) auth — the only working path

`ytmusicapi` supports three auth modes; only one works:

1. **Browser headers (WORKS):** raw request headers copied from a logged-in browser session on `music.youtube.com`. Requires the `__Secure-3PAPISID` cookie. Generate with the ytmusicapi CLI:
   ```
   pip install ytmusicapi
   ytmusicapi browser --file browser.json
   ```
   The CLI walks you through copying request headers from DevTools (filter for a `browse?...` POST, Copy → Copy as fetch / copy request headers). The resulting `browser.json` holds headers incl. the `Cookie` string. **Treat it like a password.**
2. **OAuth (BROKEN, see above).**
3. **Full custom OAuth headers** — for downstream clients supplying their own complete header set. Not applicable.

**SAPISIDHASH** (the `Authorization` header for cookie auth):
```
Authorization: SAPISIDHASH <unix_timestamp>_<sha1("<timestamp> <SAPISID> https://music.youtube.com")>
```
where `<SAPISID>` is the value of the `__Secure-3PAPISID` cookie. Also send `X-Goog-AuthUser: 0`, `X-Origin: https://music.youtube.com`, and the `Cookie` header. **Verify the exact origin string in the hash against `ytmusicapi`'s `auth.py` at build time** (web uses `https://www.youtube.com`; music uses the music origin).

### 4.4 Cookie expiry — the silent-failure trap

- Session cookies rotate. Reports vary: one project sees cookies fail after ~4 days; another claims ~2 years if the session stays alive and the user doesn't log out in that browser. **Plan for weeks, hope for longer.**
- **A lapsed session does NOT return 401.** It returns **HTTP 200 with a logged-out payload**, which unwraps to an **empty library**. The app must treat "authed library suddenly empty" as **auth failure**, never as "user deleted everything": **never wipe the cached library on an empty authed response** — show a "sign in again" prompt and keep the cache.
- Mitigation: use a persistent cookie jar / `requests`-style session so `Set-Cookie` rotations on ordinary responses are absorbed (a static header dict ages out faster).

### 4.5 Getting headers into a TV app

Typing on a TV is painful, so plan the UX:

| Method | How | Verdict |
|---|---|---|
| **Paste `browser.json` / cookie string** via the Google TV phone-app remote keyboard into a Settings screen | User copies headers on their laptop/phone, pastes into the app | **v1 choice — simplest, proven by the ytmusicapi CLI flow** |
| In-app WebView sign-in to `music.youtube.com`, app captures cookies | Best UX, but Google login inside a TV WebView may hit bot checks | Stretch goal — test it; fall back to paste |
| QR code → phone page → send headers to TV over LAN | Nice, but a whole second app surface | Later, if wanted |

**Anthony's step (his tap, his account):** he generates `browser.json` on his own computer with `ytmusicapi browser` and pastes it into the app. Cosmo never sees his Google password and never handles his raw cookies beyond the app's own storage.

---

## 5. Android TV app skeleton

### 5.1 UI — Compose for TV (Leanback is deprecated)

- **Do NOT build on Leanback.** AOSP's `androidx.leanback` fragment toolkit (`BrowseSupportFragment`, `RowsSupportFragment`, `PlaybackSupportFragment`) is officially deprecated in favor of Compose for TV. Google's own guidance for new apps in 2026: **use Compose for TV**.
- **Dependencies (verified stable versions, Nov 2026):**
  - `androidx.tv:tv-material:1.1.0` (stable May 6, 2026) — TV-adapted Material 3 components (`Card`, `Button`, `Surface`, `Text` under `androidx.tv.material3`), visible focus states, scale-on-focus.
  - `androidx.tv:tv-foundation:1.0.0` (stable May 6, 2026) — TV-aware layout/focus primitives.
  - Regular Compose foundation APIs (`LazyColumn`, `LazyRow`) are used as-is; focus is handled by the Compose focus system (`Modifier.focusable()`, `onKeyEvent` for `KEYCODE_DPAD_CENTER`, `KEYCODE_MEDIA_PLAY_PAUSE`, etc.).
- **Layout pattern:** home screen = vertical list of content rows (LazyColumn of LazyRows of focusable cards), exactly like the codezup walkthrough and the InnerTune/OuterTune home screens. Detail screen for album/artist/playlist; now-playing bar + full player screen.
- **D-pad rules:** every interactive element focusable; visible focus state always; no touch-only gestures; DPAD_CENTER = select; BACK = navigate up; media keys = play/pause/next/prev.

### 5.2 Playback — Media3 / ExoPlayer

- **Media3 latest stable: `1.11.1`** (Oct 26, 2026); `1.12.0-alpha02` in alpha.
- Dependencies: `androidx.media3:media3-exoplayer:1.11.1`, `media3-session:1.11.1` (MediaSession + background playback), `media3-ui:1.11.1`, `media3-exoplayer-hls:1.11.1` (for the IOS HLS path), `media3-exoplayer-dash:1.11.1` (if using `dashManifestUrl`).
- ⚠ **Verify at build time:** Media3's docs still list `media3-ui-leanback`, but a 2026 doc claims Media3 1.10.0 shipped **no** `ui_leanback` artifact. We don't need it (Compose for TV, not Leanback) — just don't depend on it.
- **Background audio:** `MediaSessionService` + `MediaSession` (foreground service, media notification, transport controls on the TV home screen). Audio-only: ExoPlayer with audio renderers only; no SurfaceView needed for audio (use a `PlayerView` host or no view at all for the audio path).
- **Queue:** ExoPlayer playlist = list of `MediaItem`s built from resolved stream URLs (resolve each URL fresh at play time; see §3.7).

### 5.3 AndroidManifest (TV)

```xml
<!-- Launcher: Leanback launcher category so the app shows on the TV home screen -->
<activity android:name=".MainActivity"
    android:banner="@drawable/banner"
    android:logo="@drawable/banner"
    android:screenOrientation="landscape">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
    </intent-filter>
</activity>

<uses-feature android:name="android.software.leanback" android:required="false" />
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```
- Banner: 320×180 px, xhdpi. `android.software.leanback` with `required="false"` keeps the APK installable on non-TV devices too (useful for testing on a phone).
- Background playback on Android 14+ needs the `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission + foreground-service type `mediaPlayback` on the service.

### 5.4 Gradle / SDK levels (verified Nov 2026)

| Item | Version |
|---|---|
| Android Gradle Plugin | **9.1.0** (Mar 2026, stable) — 9.4.0 exists (Sep 2026, supports API 37) but pin 9.1.0 for stability unless a feature is needed |
| Gradle | **9.3.1** (pairs with AGP 9.1.0) |
| JDK | **17** (Temurin; AGP 9.x requires 17+) |
| Kotlin | KGP **2.2.10+** (AGP 9.0+ requires it; Kotlin is built-in to AGP 9.0+, the old `kotlin-android` plugin is gone) |
| compileSdk / targetSdk | **36** (Android 16) |
| minSdk | **24** (Android 7.0 — matches OuterTune 0.10.1 and 2026 guidance; covers Chromecast with Google TV and all Google TV Streamer/TV sets) |
| Build tools | `36.0.0`; SDK packages `platforms;android-36`, `build-tools;36.0.0`, `platform-tools` |
| Networking | OkHttp **4.12.0** + Retrofit **2.11.0** (or Ktor); kotlinx.serialization with `ignoreUnknownKeys = true` |
| Images | Coil (Compose) for artwork loading |

### 5.5 Suggested module layout

```
app/
  innertube/        ← THE fragile module. All InnerTube calls, client contexts,
                      SAPISIDHASH, stream resolution + client fallback. NOTHING
                      else in the app knows YouTube exists.
    InnerTubeClient.kt    (OkHttp, POST youtubei/v1/*)
    Clients.kt            (clientName/id/version table — ONE place to bump versions)
    AuthStore.kt          (browser.json headers, encrypted storage)
    StreamResolver.kt     (player → best audio URL, IOS→ANDROID→ANDROID_VR)
    Renderers.kt          (JsonElement tree-walking → Song/Album/Artist/Playlist)
  data/             ← Repository: caching (Room), queue, liked/playlists
  playback/         ← MediaSessionService, ExoPlayer wrapper, queue management
  ui/               ← Compose for TV screens: Home, Search, Library, Playlist,
                      Artist/Album detail, Player, Settings (incl. auth paste)
```

**Isolation rule:** if YouTube changes something, the fix is inside `innertube/` only. UI and playback never touch raw InnerTube JSON.

---

## 6. Build + install pipeline

Same pattern as the Verse Garden APK builds (GitHub Actions "Build Android APK").

### 6.1 GitHub Actions workflow (`.github/workflows/build-apk.yml`)

```yaml
name: Build Android TV APK
on:
  push: { branches: [main] }
  pull_request:
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "17" }
      - name: Set up Android SDK
        uses: android-actions/setup-android@v3
        with:
          packages: platform-tools platforms;android-36 build-tools;36.0.0
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: ytm-tv-apk
          path: app/build/outputs/apk/debug/*.apk
```

- **Debug APK needs no signing** (auto debug keystore) — enough to start sideloading and testing.
- **Release signing:** create a keystore once (e.g. `keytool -genkeypair`), store it + passwords as GitHub repo secrets, wire a `signingConfig` that reads them. **Reuse the same key for every release build** — Android refuses to install an update signed with a different key over the old one. (Same rule as Verse Garden.)
- Deliver the APK via Google Drive (proven path for Anthony) or a GitHub Release.

### 6.2 Sideloading onto a Google TV

1. **Enable Developer options:** Settings → System → About → click **Android TV OS build** 7 times ("You are now a developer!").
2. **Allow unknown sources** for the installer app (per-app permission since Android 8 — grant it to whichever app opens the APK).
3. **Get the APK onto the TV** (easiest first):
   - **Downloader** (free in Play Store on the TV): type the APK's direct URL or short code; it downloads and opens the installer.
   - **Send Files to TV / SATV / LocalSend:** phone ↔ TV over home Wi-Fi; install on both, send the APK, open with a file manager on the TV.
   - **USB drive** (FAT32) + a file manager (e.g. Material Files). Chromecast with Google TV needs a USB-C hub with power delivery.
   - **ADB (most reliable, shows full errors):** enable USB debugging / Wireless debugging in Developer options, then from a computer with platform-tools:
     ```
     adb connect <TV-IP>:5555        # or: adb pair <TV-IP>:<port> <code> for wireless debugging
     adb install app-debug.apk
     adb install-multiple base.apk split_config.arm64_v8a.apk   # only if the build is split; prefer a single universal APK
     ```
     Note: retail Android TV devices may need a one-time USB connection + `adb tcpip 5555` before wireless adb works.
   - Build a **single universal APK** (no ABI splits) — a pure-Kotlin app has no native code, so 32-bit vs 64-bit TV is a non-issue.
4. **Future watch item:** Google has begun requiring sideloaded apps on certified devices to come from **verified developers** — enforcement started 2026-09-30 in Brazil, Indonesia, Singapore, Thailand, expanding worldwide. **US timing is unverified.** If it reaches the US, sideloading may need a verified-developer registration. Not a blocker today; revisit if it lands.

---

## 7. Top pitfalls + how to isolate them

| # | Pitfall | Likelihood / impact | Mitigation |
|---|---|---|---|
| 1 | **InnerTube response shapes change** (2–4×/year) | Medium / High | Lenient parsing (`ignoreUnknownKeys`, `JsonElement` tree-walking). All parsing in `innertube/`. Monitor `ytmusicapi`, `yt-dlp`, InnerTune/Metrolist repos. |
| 2 | **Client versions rotate** (stale WEB_REMIX version gets soft-blocked) | High / Medium | Versions in ONE config file (`Clients.kt`). Bump from yt-dlp `_base.py` at build time and on breakage. |
| 3 | **Stream resolution breaks** (cipher/SABR/po_token enforcement moves to mobile clients) | Medium / **Project-killing** | Fallback chain IOS → ANDROID → ANDROID_VR. Do NOT implement BotGuard/deciphering in v1. If mobile clients go SABR-only, the plain-URL path dies — that's the known long-term risk. |
| 4 | **`n`-param throttling** (streams crawl at ~50 KB/s) | Medium / Low for audio | Prefer IOS HLS path (server-signed segments). Audio-only needs ~20 KB/s, so throttling may not starve playback — **test, don't assume**. |
| 5 | **Stream URLs expire (~6 h)** | Certain / Low | Resolve fresh on every play. Never persist stream URLs. |
| 6 | **Cookie auth silently lapses** (HTTP 200 + empty library, not 401) | High over time / Medium | Detect empty-authed-library as auth failure; never wipe cache; prompt re-auth. Use a persistent cookie jar so rotations are absorbed. |
| 7 | **OAuth is broken** | Certain / — | Don't build login on OAuth. Browser headers only. |
| 8 | **No authenticated playback exists** | Certain / Low | `player` is always anonymous. Liked/library features come from `browse` only. Don't plan a feature around authed streams. |
| 9 | **ToS / distribution** | Certain / — | Unofficial API = against YouTube's ToS. **Never Play Store. Personal sideload only. Don't distribute, don't charge.** If reusing InnerTune/OuterTune code, it's **GPL-3.0** — the app inherits the license. |
| 10 | **Reference code is aging** | Medium / Medium | InnerTune last updated Nov 2025; OuterTune removed YTM entirely in v0.11 (0.10.1, Dec 2025, is the last YTM version). Metrolist (~14k stars) is the most active YTM client in 2026 — prefer it as the live reference. |
| 11 | **Google sideload verification** | Low now, rising / High | Watch the developer-verification rollout; US timing unverified as of Nov 2026. |
| 12 | **IP reputation / rate limits** | Low on home Wi-Fi / Medium | Residential IP is fine anonymously. Reuse one `visitorData` per session; don't hammer the API. |

---

## 8. Reference implementations (read, don't blindly copy)

| Project | What it proves | Status (Nov 2026) | License |
|---|---|---|---|
| **z-huang/InnerTune** | Kotlin InnerTube client + YTM player (the pattern to follow) | ~6.1k stars; last updated Nov 13, 2025 | GPL-3.0 |
| **OuterTune/OuterTune** | Fork of InnerTune, local files + YTM | ~5.4k stars; **YTM removed in v0.11** — 0.10.1 (Dec 2025) is last YTM build | GPL-3.0 |
| **MetrolistGroup/Metrolist** | YTM client for Android | ~14k stars; most active YTM client in 2026 | (check repo) |
| **sigma67/ytmusicapi** | Python InnerTube client — **the spec** for request/response shapes + auth | 1.12.3 (PyPI, Sep 16, 2026); actively maintained | (check repo) |
| **yt-dlp** (`yt_dlp/extractor/youtube/_base.py`) | **Authoritative live client table**, po_token policy, sig/n deciphering | Daily-updated | Unlicense |
| **LuanRT/YouTube.js** | Most complete InnerTube impl (TS): session, OAuth TV flow, decipher, SABR | Active | (check repo) |
| **TeamNewPipe/NewPipeExtractor** | Java extractor; clean sig/nsig regexes, `PoTokenProvider` interface | Active | GPL-3.0 |
| **vfsfitvnm/ViMusic** (RiMusic) | Android YTM streaming app | (check repo) | GPL-3.0 |
| **IarChep/MeeTube** (`docs/INNERTUBE_API.md`) | Best single engineering reference doc for InnerTube (compiled 2026-06-29) | — | — |

---

## 9. Prep checklist (in order)

**Anthony's steps (his taps):**
- [ ] Tell Cosmo the feature list — home? search? library? playlists? liked songs? queue/radio? lyrics? downloads? (STILL WAITING)
- [ ] Confirm the TV model + Android version (Chromecast with Google TV? Google TV Streamer? Sony/TCL/Hisense TV?) — decides minSdk testing
- [ ] Create the GitHub repo (Cosmo's token cannot create repos — Anthony creates, Cosmo builds + PRs)
- [ ] Pick an app name + package id (e.g. `com.anthony.ytmtv` — anything, it's sideload-only)
- [ ] Later, when auth is needed: run `ytmusicapi browser` on his own computer and paste `browser.json` into the app (his Google account, his cookies)

**Builder prep (before writing app code):**
- [ ] Laptop harness: `pip install ytmusicapi==1.12.3`; unauthenticated probes — `search()`, `get_home()`, `get_song()`/`get_streaming_data()`; save raw JSON fixtures
- [ ] Verify the **IOS `hlsManifestUrl` path actually plays** in ExoPlayer and isn't throttled — this is the load-bearing assumption; prove it FIRST, before building on it
- [ ] Verify itag picks (251 Opus → 140 AAC) against yt-dlp's format table
- [ ] Pull current client versions from yt-dlp `_base.py` (WEB_REMIX, ANDROID, IOS, ANDROID_VR) — do not ship the Jun-2026 snapshots in §3.4
- [ ] Read the `WEB_REMIX` API key + YTM search-filter `params` values from `ytmusicapi` source (do not invent them)
- [ ] Confirm `SAPISIDHASH` origin string from `ytmusicapi` `auth.py`
- [ ] Scaffold: AGP 9.1.0 + Gradle 9.3.1 + JDK 17, compile/target 36, minSdk 24; Compose for TV 1.1.0/1.0.0; Media3 1.11.1; OkHttp 4.12.0 + Retrofit 2.11.0
- [ ] GitHub Actions workflow (mirror Verse Garden pattern: JDK 17, `platforms;android-36`, `assembleDebug`, artifact upload)
- [ ] Signing keystore for release builds (same reuse rule as Verse Garden — one key, kept in repo secrets)
- [ ] First sideload test: `adb install` a hello-world TV APK on Anthony's actual TV before any YTM code exists
- [ ] Build order (proposed, pending Anthony's feature list): **Phase 1** — scaffold + unauthenticated search/browse + play one song; **Phase 2** — queue, home feed, playlists; **Phase 3** — browser-header auth + library/liked; **Phase 4** — polish (lyrics, now-playing UI, settings)

---

## 10. Open questions for Anthony

1. **Feature list** — what must v1 do? (Home? Search? Library? Playlists? Liked? Radio/queue? Lyrics?)
2. **TV model** — what device is this going on?
3. **Repo name + app name** — what do we call it?
4. **Auth now or later?** — v1 can ship unauthenticated (search + play anything public); library/liked needs the browser-headers step. Recommend: auth in Phase 3, not Phase 1.

---

## 11. Sources

- InnerTube engineering reference: https://github.com/iarchep/meetube/blob/HEAD/docs/INNERTUBE_API.md (compiled 2026-06-29; client versions as of Jan–Jun 2026)
- ytmusicapi auth + OAuth breakage: https://github.com/ichieh42/ytmusicapi/blob/HEAD/skills/SKILL.md ; issue #813: https://github.com/sigma67/ytmusicapi/issues/813 ; ytmusicapi PyPI 1.12.3: https://pypi.org/project/ytmusicapi/
- SABR / streaming internals: https://github.com/animeshkundu/youtube-audio/blob/HEAD/docs/research/04-youtube-streaming-internals.md
- InnerTube-in-Kotlin reference (Stash design, InnerTube endpoints + browse IDs): https://github.com/rawnaldclark/stash/blob/HEAD/docs/superpowers/specs/2026-03-25-stash-music-app-design.md
- CLIBeats ADR-005 (OkHttp 4.12.0 + Retrofit 2.11.0, stream-URL expiry ~6h, lenient JSON parsing): https://github.com/omprakash-p06/clibeats/blob/HEAD/docs/adr/ADR-005-provider-integration-innertube.md
- Stream-resolution fallback (dual-client, plain-`url` only): https://deepwiki.com/feralbureau/bedrock-api/5.1-youtube-provider
- Media3 1.11.1 stable (Oct 26, 2026): https://developer.android.com/jetpack/androidx/releases/media3
- Compose for TV, tv-material 1.1.0 / tv-foundation 1.0.0 (May 6, 2026): https://developer.android.com/jetpack/androidx/releases/tv
- Leanback deprecated / Compose for TV recommended; Media3 1.10.0 has no `ui_leanback`: https://github.com/brettpetch/silo-android/blob/HEAD/docs/media3/06-android-tv-and-track-selection.md
- AGP 9.4.0 release notes (Sep 2026; max API 37, Gradle 9.6.0, JDK 17, build-tools 36.0.0): https://developer.android.com/build/releases/agp-9-4-0-release-notes
- GitHub Actions Android build pattern (JDK 17 Temurin, `gradle/actions/setup-gradle@v4`, `android-actions/setup-android@v3`, `platforms;android-36`): https://dev.to/ronynn/automating-android-apk-builds-with-github-actions-the-sane-way-1h95 and https://syedovaiss.medium.com/android-ci-cd-from-pull-requests-to-production-releases-690cdc02b2a1
- Sideloading (Downloader, Send Files to TV, USB, adb; developer-verification rollout): https://livenettv.tools/install-apps-on-android-tv/ ; https://phonetotv.app/install-apk-on-android-tv ; https://h5.2113.net/guides/install-apk-android-tv.html
- InnerTune / OuterTune / Metrolist: https://github.com/z-huang/InnerTune ; https://github.com/OuterTune/OuterTune ; OuterTune 0.10.1 status: https://h5.2113.net/apps/outertune.html

## 12. Things NOT verified — do not treat as fact

- The exact current `WEB_REMIX` / `ANDROID` / `IOS` client versions (Jun-2026 snapshots in §3.4 — re-pull from yt-dlp at build time).
- The public `WEB_REMIX` API key value (redacted in sources I read — take it from `ytmusicapi` source at build time, or omit the key).
- YTM search-filter `params` base64 values (songs/albums/artists/playlists) — read from `ytmusicapi` source at build time.
- Whether the IOS `hlsManifestUrl` path plays unthrottled on a real 2026 device (the load-bearing assumption — must be proven on-device in prep).
- Whether `media3-ui-leanback` exists in Media3 1.11.x (irrelevant — we don't need it — but don't depend on it).
- US timing of Google's sideload developer-verification rollout (only the Sep-2026 start in BR/ID/SG/TH is documented).
- Whether Anthony's TV is 32- or 64-bit (irrelevant for a pure-Kotlin app, but confirm via `adb shell getprop ro.product.cpu.abilist` at install time).
