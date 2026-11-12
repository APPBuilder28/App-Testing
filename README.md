# NextVu Player

**NextVu Player** is a futuristic, media-only IPTV playback client designed for **Android Mobile, Android TV, and Amazon Fire Stick** (touch + full D-Pad remote focus navigation).

> **Legal Notice:** NextVu Player is strictly a media playback client. It does **NOT** host, sell, promote, or supply any streams, channels, movies, or copyrighted content. Users must connect only to Xtream API sources they legally subscribe to or own.

---

## Tech Stack (Native Android Implementation)

Built natively for Android using:
- **Language & UI:** Kotlin + Jetpack Compose (Material 3 Dark Cinematic Theme `#14141C` / `#00E5FF` / `#7B2CFF`, Inter font)
- **Media Engine:** Jetpack Media3 ExoPlayer (`media3-exoplayer`, `media3-exoplayer-hls`, `media3-ui`)
- **Encrypted Credential Storage:** Android Keystore hardware-backed **AES-256-GCM** (`SecureSourceStorage`)
- **Per-Source Local Database:** Room SQLite (`NextVuDatabase`) isolating watch history, playback progress, offline catalog cache, and unified watchlist strictly per `sourceId`
- **Settings Persistence:** Jetpack DataStore Preferences
- **Networking:** OkHttp + resilient JSON parsing for dynamic Xtream Codes API (`player_api.php`)

---

## Core Features

1. **Multi-Xtream Source Manager**
   - Add multiple Xtream API profiles (`Source Name`, `Server URL`, `Username`, `Password` with show/hide toggle).
   - **Test Connection** validation before saving with animated green-check success or horizontal shake error animation.
   - Tap source card to confirm switching active source; long-press (or options button) to Rename, Edit Credentials, or Delete.
   - Global **Quick-Source-Switch Popup** triggered by tapping the Active-Source Name in the top bar on any screen.
2. **Per-Source Isolated Persistence & Offline Mode**
   - Playback progress, Continue Watching, Watch History, and Unified Favorites/Watchlist are stored separately per `sourceId`.
   - Pull-to-refresh on Live TV, Movies, and Series tabs, 15s timeout & HTTP 429 rate-limit handling, and an automatic Offline Mode indicator.
3. **Media3 ExoPlayer 4K / HEVC / HDR / AC3 Engine**
   - HEVC (H.265) hardware decoding probe with automatic software fallback & warning banner.
   - Full 4K (2160p) UHD support with high-speed internet warning popup before loading 4K streams.
   - HDR10 / HDR10+ / HLG / Dolby Vision badge detection.
   - AC3 / E-AC3 passthrough probe with automatic PCM decoding fallback.
   - Toggleable real-time bitrate HUD, resolution selector (`Auto / 720p / 1080p / 2160p`), audio & subtitle track switchers.
4. **Live TV + EPG Catch-Up, Movies & Series**
   - Live TV collapsible category sidebar, search/favorites/EPG filters, slide-in EPG timeline & Catch-Up replay (`tv_archive`).
   - Movies & Series poster grids with Genre, Year, Rating, Sort, and Favorites filters + resume playback & Auto-Next Episode.
5. **Settings & Multi-Language UI**
   - Video quality presets, live-preview subtitle customization, 4-digit Parental Lock PIN, bitrate HUD toggle, Auto-Next Episode toggle, Screenshot Blocking (`FLAG_SECURE`), English/Español/Français/Deutsch localization, per-source or all-sources history clear, and source reset.

---

## Build & Installation

1. Open the project in Android Studio (or run Gradle directly):
   ```bash
   gradle :app:assembleDebug
   ```
2. Install on Android Mobile, Android TV, or Fire Stick (`LEANBACK_LAUNCHER` and `LAUNCHER` are both configured in `AndroidManifest.xml`).
3. Launch the app, add your legal Xtream Codes server credentials, tap **Test Connection**, and save.
