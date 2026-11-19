### 🎵 Openfy v1.3.5 - Screen-Off Play, Auto-Fallbacks, Session Persistence & Connection Keep-Alive

**Openfy v1.3.5** delivers essential stability, power management, and playback persistence improvements across Windows, macOS, Android, and iOS.

---

### 🚀 Key Improvements in v1.3.5

#### 1. 🌙 Screen-Off Play & Background Power Management
* **Windows Modern Standby & Lock Screen Support**: Integrates Win32 `SetThreadExecutionState` (`ES_CONTINUOUS | ES_SYSTEM_REQUIRED | ES_AWAYMODE_REQUIRED`). Windows will no longer suspend audio streaming or drop network connections when your display turns off, screen locks, or laptop lid closes.
* **macOS Sleep Prevention**: Integrates power assertion management via `caffeinate` to prevent system and network sleep while music is playing.
* **Android High-Performance Wi-Fi Lock**: Acquires `WIFI_MODE_FULL_HIGH_PERF` alongside network `WakeLock` to prevent Android Doze mode from throttling streaming packets when the screen is off.

#### 2. ⚡ Intelligent Stall Detection Watchdog & Seamless Auto-Fallback
* **Active Freeze Watchdog**: Continuously monitors the audio decoders. If an audio stream freezes or buffers for >4 seconds due to network throttling, the watchdog automatically recovers by switching to the next resolver in the cascade.
* **Exact Timestamp Preservation**: Auto-fallback and manual engine switching now resume from the **exact millisecond** where playback was interrupted, eliminating restarts from the beginning.
* **Fixed Playback Cutoffs**: Removed phantom synthetic progress timers that could cause premature track skips when streaming jitter occurred.

#### 3. 💾 Full Playback Session Persistence & Instant Resume
* **Automatic State Saving**: The app continuously saves your active track, millisecond position, up to 50 queue items, volume level, shuffle state, repeat mode, and preferred resolver to disk.
* **Instant Startup Resume**: On app launch (Windows, macOS, Android), your previous listening session is immediately ready on screen so you can resume listening with a single tap or spacebar press.

#### 4. 🌐 Connection Keep-Alive & Robust HTTP Streaming
* **Persistent Connection Pooling**: Configured HTTP keep-alive (`Connection: keep-alive`, `Keep-Alive: timeout=60, max=100`) and pooled connections to CDNs.
* **Exponential Backoff Retry**: Automatic request retries on socket resets or transient network drops.
* **JioSaavn HQ 320k Integration**: Tier 1 unthrottled CD-quality direct AAC streams now available natively across all desktop and mobile platforms with on-device decryption.

---

### 🔒 Verification & Checksums
* **Windows Setup Installer**: `Openfy-Setup.exe` (see `SHA256SUMS-windows.txt`)
* **Windows MSI Installer**: `Openfy.msi`
* **Windows Portable ZIP**: `Openfy-windows-x64.zip`
* **macOS DMG Installer**: `Openfy.dmg` (see `SHA256SUMS-macos.txt`)
* **macOS PKG Installer**: `Openfy.pkg`
* **macOS Portable ZIP**: `Openfy-macos.zip`
* **Android APK**: `Openfy.apk` (see `SHA256SUMS.txt`)
* **iOS IPA**: `Openfy.ipa` (see `SHA256SUMS-ios.txt`)
