### 🎵 Openfy v1.2.0 - Cross-Platform Release (Android & iOS)

**Openfy** is an ad-free music streaming client bringing Spotify's sleek interface together with RVX YouTube Music's high-fidelity audio engine.

Starting with **v1.2.0**, Openfy is a **universal cross-platform app** running on both **Android** and **iOS** powered by **Compose Multiplatform (CMP)** and **Kotlin Multiplatform (KMP)**!

---

### 🍏 What's New for iOS
* **Compose Multiplatform UI**: 1:1 Spotify mobile dark interface running natively on iPhone and iPad.
* **Native Apple Audio Engine (`AVPlayer`)**: Streams high-efficiency AAC 128 kbps (itag 140) with native hardware acceleration and battery optimization.
* **Background Playback**: `AVAudioSessionCategoryPlayback` ensures music keeps playing with the screen locked or app in background.
* **Lock Screen & Dynamic Island**: `MPNowPlayingInfoCenter` integration showing track titles, artist, album art, and progress scrubber.
* **AirPods & Control Center**: Full `MPRemoteCommandCenter` support for headphone clicks and Control Center scrubbers.
* **Sideloadable IPA Package**: Download `Openfy.ipa` below and sideload with AltStore, SideStore, TrollStore, or Sideloadly!

---

### 🤖 What's New for Android
* **Opus 160k Streaming**: Full-fidelity Opus extraction via InnerTube with ExoPlayer.
* **Spotify Library Sync**: Fast 1-click `sp_dc` cookie input to sync Liked Songs and playlists.
* **Playlist Management & Direct Heart**: Add-to-Playlist sheet, '+ New Playlist' creation, and instant Spotify API library write (`PUT /v1/me/tracks`).
* **LRCLIB Synced Karaoke Lyrics**: Fullscreen real-time synced lyrics with line-by-line autoscroll and tap-to-seek.
* **Persistent Keystore Signature**: Direct in-place updates supported across all releases!

---

### 🔒 Verification & Checksums
* **Android APK**: `Openfy.apk` / `Openfy-v1.2.0.apk` (see `SHA256SUMS.txt`)
* **iOS IPA**: `Openfy.ipa` / `Openfy-v1.2.0.ipa` (see `SHA256SUMS-ios.txt`)

#### 📥 Download:
- **Android Users**: Download **`Openfy.apk`** below to install directly.
- **iOS Users**: Download **`Openfy.ipa`** below to sideload via AltStore / SideStore / TrollStore.
