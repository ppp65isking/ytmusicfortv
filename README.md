# YTMusic for TV

A real Google TV app for YouTube Music — personal use only, sideloaded,
never the Play Store. No patched APKs, no modifying YouTube's APK.

> **Placeholders for Anthony to rename:** the app name is currently
> **"YTMusic for TV"** and the package id is
> **`com.ppp65isking.ytmusicfortv`**. Both are flagged in the code.

## Status — Phase 1 + 2 (scaffold)

- **Phase 1 — playback PROVED** (see [phase1-report.md](phase1-report.md)).
  The app calls YouTube Music's InnerTube API directly from the device,
  no backend. The player client is **VISIONOS** (id 101, v1.02) — the only
  client returning plain stream URLs + `hlsManifestUrl` as of Nov 2026
  (IOS/ANDROID now return SABR-only formats). Direct itag 251 (Opus
  ~148 kbps) and HLS (itag 140 AAC) were both fetched and decoded cleanly.
  `visitorData` is required on every player call.
- **Phase 2 — scaffold** (this repo): native Kotlin Android TV app,
  Jetpack Compose for TV, Media3 playback, GitHub Actions debug-APK build.

## Target device

**Chromecast with Google TV 4K** (Android TV / Google TV OS). Sideload via
adb over Wi-Fi, Downloader, or Send Files to TV. The device has ~8 GB
usable storage — keep the APK lean (no unused deps).

## Architecture

```
app/src/main/java/com/ppp65isking/ytmusicfortv/
  MainActivity.kt          entry point, sets the Compose content
  YtmTvApp.kt              singletons (OkHttp, AuthStore, InnerTube, Player)
  innertube/               ONE place for everything YouTube-shaped
    Clients.kt             verified InnerTube client defs (VISIONOS etc.)
    InnerTubeClient.kt     POST helper, visitorData harvesting, search
    StreamResolver.kt      player endpoint -> HLS URL / direct itag URL
    Renderers.kt           search/browse JSON -> Song (null-safe)
    AuthStore.kt           visitorData + (Phase 3) browser.json headers
  playback/                ONE place for everything playback-shaped
    PlayerManager.kt       single ExoPlayer, HLS-first, StateFlow UI state
    MusicService.kt        MediaSessionService for background play
  ui/
    HomeScreen.kt          sidebar nav + content rows + now-playing bar
    theme/Theme.kt         dark purple theme (Anthony's favourite colour)
```

When YouTube changes something, touch `innertube/` only. When playback
changes, touch `playback/` only.

## Build

First build happens in GitHub Actions (this VM has no JDK/Android SDK):

1. Push to `main` (needs Anthony's approval — never push unapproved).
2. Workflow **Build Android APK** runs `./gradlew assembleDebug`.
3. Download the `ytm-tv-apk` artifact.

Local build (with Android Studio / SDK installed):

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Stack: AGP 9.1.0, Gradle 9.3.1, JDK 17, compile/target SDK 36, minSdk 24,
Compose for TV (tv-material 1.1.0, tv-foundation 1.0.0), Media3 1.11.1,
OkHttp 4.12.0, kotlinx-serialization-json 1.9.0. Leanback is deprecated —
not used. AGP 9.1 bundles Kotlin 2.2.10: do **not** apply
`org.jetbrains.kotlin.android` (incompatible, build fails).

## Sideload test (Chromecast with Google TV 4K)

1. Enable Developer options (Settings → System → About → click Build
   repeatedly), turn on USB debugging / ADB over network.
2. `adb connect <tv-ip>:5555`, then `adb install app-debug.apk`
   (or use Downloader / Send Files to TV).
3. Open the app, press the **"Bohemian Rhapsody — Queen"** test card.
   It resolves the real stream via InnerTube and plays it — that is the
   end-to-end proof on the device. If it plays, the last mile is done.

## Roadmap

- **Phase 3:** search/browse UI (real rows from `Renderers`), queue,
  sign-in via `browser.json` paste (library/playlists/liked), background
  play polish.
- **Phase 4:** lyrics, custom theme, voice search, liquid-glass nav
  (glass on the navigation layer only, never content).
- **Phase 5:** nav-bar position setting (left sidebar ↔ top bar).

## Docs

- [research-build-plan.md](research-build-plan.md) — original build plan
  (note: its IOS-first dual-client assumption was disproved by Phase 1).
- [phase1-report.md](phase1-report.md) — playback proof, fixtures, client
  versions, and what still needs proving on-device.
