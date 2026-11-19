### 🎵 Openfy v1.3.2 - RVX Dynamic Source Switching & Multi-Client Fallback Engine

**Openfy** brings Spotify's mobile interface together with high-fidelity, ad-free background music streaming.

This release introduces full **RVX (ReVanced Extended) client fallback switching**, resolving playback stalls by automatically switching between streaming sources (`Android VR`, `iOS Direct`, `Apple visionOS`, `NewPipe Extractor`, and `TV Cobalt`).

---

### 🚀 Key Improvements & Fixes in v1.3.2

#### 1. 🔄 RVX Dynamic Client Fallback & Automatic Source Switching
* **Auto-Failover on Playback Error**: When ExoPlayer encounters a playback error (such as a 403 Forbidden or network CDN drop from YouTube), the app does not stop. It automatically marks the failing client and fails over to the next engine in the prioritized chain (`iOS Direct` -> `Android VR (Meta Quest)` -> `Apple visionOS` -> `NewPipe Extractor` -> `TV Cobalt`).
* **Active Stream Pre-Validation**: Candidate streams are validated via ultra-fast 0-1 byte Range checks (200-206) before passing to ExoPlayer, preventing infinite buffering spinners on stale or blocked CDN links.

#### 2. 🥽 Added ReVanced Extended Default Client (Android VR)
* **Android VR Client Spoofing**: Added `ANDROID_VR` (Meta Quest 3 / Client 28) to `clients.json` and stream resolvers. This is the #1 recommended client in RVX because it provides unthrottled, unciphered direct audio streams without complex JavaScript signature transforms or DRM requirements.
* **Apple visionOS Client (Client 101)**: Integrated as an additional high-stability direct stream fallback.

#### 3. 🎛️ Interactive Source Switcher in Now Playing & Settings
* **One-Tap Switch Engine**: Replaced the static badge in the Now Playing sheet with an interactive Source Badge. Tapping the badge instantly switches between available streaming engines on the fly.
* **Wired Settings Preference**: Connected the **Streaming Client** setting in Settings to the resolver engine so users can lock their preferred default source.

---

### 🔒 Verification & Checksums
* **Android APK**: `Openfy.apk` / `Openfy-v1.3.2.apk` (see `SHA256SUMS.txt`)
* **iOS IPA**: `Openfy.ipa` / `Openfy-v1.3.2.ipa` (see `SHA256SUMS-ios.txt`)

#### 📥 Download:
- **Android Users**: Download **`Openfy.apk`** below to install directly (in-place update supported).
- **iOS Users**: Download **`Openfy.ipa`** below to sideload via AltStore / SideStore / TrollStore.
