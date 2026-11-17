### 🎵 Openfy v1.3.0 - Playback Freeze Fix & Spotify Auth Overhaul

**Openfy** brings Spotify's mobile interface together with high-fidelity, ad-free background music streaming.

This release directly addresses and eliminates the playback buffering freeze, overhauls Spotify account connection with reverse-engineered TOTP token support, and delivers a 100% seamless Spotify-first experience.

---

### 🚀 Key Improvements & Fixes in v1.3.0

#### 1. 🎧 Fixed Audio Playback 0:00 Freeze (NewPipeExtractor Engine)
* **Root Cause Fixed**: YouTube's Google Video CDN previously rejected chunk range requests ($>256\text{ KB}$) with `HTTP 403 Forbidden` on standard mobile clients, causing ExoPlayer to stall at `0:00 / 0:00` with an infinite loading spinner.
* **Battle-Tested Stream Extraction**: Integrated `NewPipeExtractor` with dynamic cipher deobfuscation. Audio streams now stream with standard `HTTP 206 Partial Content` ranges for uninterrupted playback of any track.
* **Proper User-Agent Headers**: Configured ExoPlayer's `DefaultHttpDataSource` with clean mobile Chrome headers to prevent CDN rate drops.

#### 2. 🔑 Reverse-Engineered Spotify TOTP `/api/token` Authentication
* **TOTP Handshake**: Reversed Spotify Web Player's modern token authentication algorithm (SHA1 TOTP period 30s) directly in Kotlin.
* **Reliable Library Sync**: Replaces deprecated endpoints with `/api/token`, enabling instant exchange of session cookies (`sp_dc`) for private Web Player Bearer Access Tokens.
* **Anonymous Browsing Fallback**: Automatically generates anonymous tokens for search and metadata browsing even when no account is connected.

#### 3. 🌐 Overhauled Spotify In-App Web Login & 1-Tap Connect
* **Automated Cookie Capture**: Added background polling that monitors `.spotify.com`, `accounts.spotify.com`, and `open.spotify.com` to capture sessions automatically as soon as login completes.
* **"I'm Logged In - Connect Openfy" Button**: Added a prominent 1-tap button at the bottom of the dialog. If you see "Logged in as [Name]", tapping this button captures the session immediately.
* **Eliminated Popup Blank Screen**: Set `setSupportMultipleWindows(false)` and transparent background to prevent blank WebViews on Spotify redirects.
* **Dual Login Options**: Switch seamlessly between **In-App Login** and **Quick Cookie (sp_dc)** paste.

#### 4. 🎯 100% Spotify-First Architecture
* **Removed Google WebView Block**: Completely removed Google Sign-in WebView popups that triggered Google's *"This browser or app may not be secure"* error.
* **Invisible Audio Backend**: The RVX YouTube audio engine runs invisibly under the hood with zero login friction.

---

### 🔒 Verification & Checksums
* **Android APK**: `Openfy.apk` / `Openfy-v1.3.0.apk` (see `SHA256SUMS.txt`)
* **iOS IPA**: `Openfy.ipa` / `Openfy-v1.3.0.ipa` (see `SHA256SUMS-ios.txt`)

#### 📥 Download:
- **Android Users**: Download **`Openfy.apk`** below to install directly (in-place update supported).
- **iOS Users**: Download **`Openfy.ipa`** below to sideload via AltStore / SideStore / TrollStore.
