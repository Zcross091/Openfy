### 🎵 Openfy v1.3.3 - Unthrottled 320kbps Audio Engine & Playback Freeze Auto-Recovery

**Openfy** brings Spotify's mobile interface together with high-fidelity, ad-free background music streaming.

This release eliminates playback freezing (such as the 1-minute / 1:09 buffer freeze caused by YouTube CDN rate-limiting down to 1.5 KB/s) by introducing **JioSaavn HQ 320k** direct streaming (as pioneered by Spotube) alongside an intelligent **Stall Detection Watchdog** that auto-recovers playback seamlessly if network buffering ever stalls.

---

### 🚀 Key Improvements & Fixes in v1.3.3

#### 1. ⚡ Ultra-Fast JioSaavn HQ 320kbps CD-Quality Audio Engine
* **Bypasses YouTube 1MB Cutoffs & Throttling**: YouTube Google Video CDN limits certain unauthenticated or topic streams to ~1MB or throttles un-ciphered clients down to 1.53 KB/s. JioSaavn HQ provides unthrottled, direct 320kbps CD-quality AAC streams with full HTTP Range seek support from start to finish (100% complete playback).
* **Automatic Integration with Auto Fallback**: When set to Auto (default), Openfy searches and plays crystal-clear 320kbps / 160kbps AAC audio with zero ads and zero buffering drops, while maintaining the complete multi-client RVX fallback chain (`JioSaavn HQ` -> `iOS Direct` -> `Android VR` -> `Apple visionOS` -> `NewPipe Extractor` -> `TV Cobalt`).

#### 2. 🛡️ Intelligent Stall Detection Watchdog
* **Auto-Recovers Stalled Streams**: If ExoPlayer is actively playing but gets stuck buffering or progress stops advancing for >4 seconds (e.g., due to a CDN connection drop or speed throttle), the watchdog auto-triggers an engine failover.
* **Seamless Timestamp Preservation**: When failing over or when the user manually taps the Source Badge to switch engines, playback instantly resumes at the exact timestamp without resetting the track back to 0:00!

#### 3. 🔍 Verified 1MB CDN Boundary Check
* **Pre-empts 1MB CDN Limits**: YouTube Google Video CDN streams are now verified beyond byte 1,048,576 before being selected, ensuring 1MB-capped topic streams are avoided before they can stall playback.

#### 4. 🎛️ Interactive Source Switcher & Settings
* Users can freely select `JioSaavn HQ (320k)`, `iOS Direct`, `Android VR`, or `NewPipe Extractor` directly from the Now Playing sheet or Settings screen.

---

### 🔒 Verification & Checksums
* **Android APK**: `Openfy.apk` / `Openfy-v1.3.3.apk` (see `SHA256SUMS.txt`)
* **iOS IPA**: `Openfy.ipa` / `Openfy-v1.3.3.ipa` (see `SHA256SUMS-ios.txt`)

#### 📥 Download:
- **Android Users**: Download **`Openfy.apk`** below to install directly (in-place update supported).
- **iOS Users**: Download **`Openfy.ipa`** below to sideload via AltStore / SideStore / TrollStore.
