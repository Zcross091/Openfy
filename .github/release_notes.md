### 🎵 Openfy v1.3.4 - Spotify Desktop Edition (Windows & macOS) & Multi-Platform Release

**Openfy** brings the iconic Spotify desktop experience together with high-fidelity, ad-free background music streaming and unthrottled audio resolvers.

This release introduces the official **Windows & macOS Desktop Editions** featuring the full **Spotify Desktop UI** alongside Android and iOS cross-platform support.

---

### 🚀 Key Features & Highlights in v1.3.4

#### 1. 🖥️ Official Spotify Desktop Client (macOS & Windows)
* **Identical Spotify Desktop UI**: Authentic 3-panel Spotify desktop layout:
  - **Left Sidebar**: Home, Search pills, and Your Library with pinned Liked Songs and personal playlists.
  - **Center Stage**: Detailed Spotify track table with `#`, `Title`, `Album`, `Date Added`, and `Duration` columns, 56dp Spotify-green play button, and interactive hover states.
  - **Right Collapsible Drawer**: Now Playing view and synchronized karaoke lyrics.
  - **Bottom Player Bar**: Full-width persistent player bar with track scrubber, volume slider, mute toggle, and interactive audio engine switcher.
* **Apple Silicon (M1/M2/M3/M4) & Intel macOS Support**: Universal macOS `.app` bundle, `.dmg` disk image, and `.pkg` installer with ad-hoc code signature for Gatekeeper.
* **Native Desktop Persistence**: Uses system preferences (`~/Library/Preferences/` on macOS and Windows Registry on Windows) for instantaneous load times and zero-configuration settings persistence.
* **Native Audio Pipeline**: Direct streaming audio engine supporting HTTP/HTTPS streaming with hardware-accelerated audio decoding.

#### 2. ⚡ Unthrottled 320kbps CD-Quality Audio Fallback Engines
* **JioSaavn HQ (320k)**: Direct 320kbps CD-quality AAC streams with full HTTP Range seek support from start to finish (100% complete playback, no 1MB cutoffs or YouTube speed throttles).
* **Multi-Engine Fallback Cascade**: Seamlessly falls over between `JioSaavn HQ` ➔ `iOS Direct` ➔ `Android VR` ➔ `Apple visionOS` ➔ `NewPipe Extractor` ➔ `TV Cobalt`.
* **Interactive Engine Switcher**: Tap the engine badge in the player bar or use Settings to switch resolvers anytime with exact timestamp preservation.

#### 3. 🛡️ Intelligent Stall Detection Watchdog
* **Auto-Recovers Stalled Streams**: If playback ever stalls buffering for >4 seconds due to network throttling, the watchdog auto-recovers to the next resolver instantly.

#### 4. 🎤 Synchronized Real-Time Karaoke Lyrics
* Real-time synchronized lyrics with animated syllable tracking and one-tap jump-to-timestamp seeking.

---

### 🔒 Verification & Checksums
* **macOS DMG Installer**: `Openfy.dmg` / `Openfy-v1.3.4.dmg` (see `SHA256SUMS-macos.txt`)
* **macOS PKG Installer**: `Openfy.pkg` / `Openfy-v1.3.4.pkg`
* **macOS Portable ZIP**: `Openfy-macos.zip` / `Openfy-v1.3.4-macos.zip`
* **Windows Portable ZIP**: `Openfy-windows-x64.zip` / `Openfy-v1.3.4-windows-x64.zip` (see `SHA256SUMS-windows.txt`)
* **Windows MSI Installer**: `Openfy.msi` / `Openfy-v1.3.4.msi`
* **Windows EXE Installer**: `Openfy-Setup.exe` / `Openfy-v1.3.4-Setup.exe`
* **Android APK**: `Openfy.apk` / `Openfy-v1.3.4.apk` (see `SHA256SUMS.txt`)
* **iOS IPA**: `Openfy.ipa` / `Openfy-v1.3.4.ipa` (see `SHA256SUMS-ios.txt`)

#### 📥 Download:
- **Mac Users (DMG)**: Download **`Openfy.dmg`** below, double-click to open, and drag Openfy into your Applications folder.
- **Mac Users (Portable)**: Download **`Openfy-macos.zip`** below and extract `Openfy.app`.
- **Windows Users (Portable)**: Download **`Openfy-windows-x64.zip`** below, extract, and double-click `Openfy.exe`.
- **Windows Users (Installer)**: Download **`Openfy.msi`** or **`Openfy-Setup.exe`** below to install Openfy into your Windows Start Menu.
- **Android Users**: Download **`Openfy.apk`** below to install directly (in-place update supported).
- **iOS Users**: Download **`Openfy.ipa`** below to sideload via AltStore / SideStore / TrollStore.
