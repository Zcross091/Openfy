# Openfy 🎵 (Android)

<p align="center">
  <img src="https://raw.githubusercontent.com/Zcross091/Openfy/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Openfy Logo" width="128"/>
</p>

<p align="center">
  <strong>A music streaming application for Android mobile phones, looking just like Spotify with the power of RVX YouTube Music under the hood.</strong>
</p>

<p align="center">
  <a href="https://github.com/Zcross091/Openfy/actions/workflows/build.yml">
    <img src="https://github.com/Zcross091/Openfy/actions/workflows/build.yml/badge.svg" alt="Build Status"/>
  </a>
  <a href="https://github.com/Zcross091/Openfy/releases/tag/latest">
    <img src="https://img.shields.io/github/v/release/Zcross091/Openfy?label=Latest%20APK&color=1DB954&logo=android" alt="Latest Release"/>
  </a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Audio-AndroidX%20Media3%20ExoPlayer-FF0000?logo=youtube&logoColor=white" alt="ExoPlayer"/>
  <img src="https://img.shields.io/badge/Audio-Ad--Free%20Opus%20160k-7c3aed" alt="Ad-Free"/>
</p>

<p align="center">
  <a href="https://github.com/Zcross091/Openfy/releases/latest/download/Openfy.apk">
    <img src="https://img.shields.io/badge/Download-Openfy.apk%20(Direct)-1DB954?style=for-the-badge&logo=android&logoColor=white" alt="Download APK"/>
  </a>
</p>

---

## 📥 Download & Install

You can download the latest automated build directly from GitHub Releases:
* **Direct Download**: [**Download Openfy.apk**](https://github.com/Zcross091/Openfy/releases/latest/download/Openfy.apk)
* **Latest Release Hub**: [GitHub Releases (Tag: `latest`)](https://github.com/Zcross091/Openfy/releases/tag/latest)

> **In-Place APK Updates Supported**: Every build is signed with a persistent cryptographic release certificate (`SHA-256: 74:E6:5E:24:...`). You can install new APK versions directly on top of previous versions without uninstalling or losing your cached data.

---

## 🌟 Highlights

**Openfy** is a pure, native **Android application** that merges Spotify's mobile user interface with the unrestricted, ad-free audio streaming engine of RVX (ReVanced Extended) YouTube Music.

* **🎨 1:1 Spotify Mobile Interface**: Built entirely with **Jetpack Compose** and Material 3. Exact Spotify dark palette (`#121212`, card `#181818`, hover `#282828`, green accent `#1DB954`), mobile bottom navigation bar (`Home`, `Search`, `Your Library`, `Active Account`), persistent floating mini-player, and full-screen now-playing sheet.
* **⚡ RVX YouTube Music Streaming Engine**: High-fidelity Opus (160 kbps) and AAC (128 kbps) audio extraction using YouTube Music's InnerTube API. No ads, no song skips limits, and no Spotify Premium or YouTube Premium required.
* **🎧 Background Playback & Lock Screen Controls**: Powered by **AndroidX Media3 (ExoPlayer)** and a native `MediaSessionService`. Keeps playing seamlessly in the background with lock-screen controls, Android system notification controls (MediaStyle), and audio focus handling.
* **🔄 Single-Active-Account Paradigm**: Log in from your **Spotify** or **YouTube Music** account (one at a time):
  * **Logged into Spotify**: Access your Spotify Liked Songs and playlists. Tapping the **Heart** icon while listening immediately saves the song directly to your Spotify library (`PUT /v1/me/tracks`)!
  * **Logged into YouTube**: Access your YouTube Music playlists and Liked Music (`LM`). Tapping the **Heart** icon likes the song on YouTube Music!
  * **Guest / Free Mode**: Search, browse global top charts, and stream full songs without logging into anything.
* **🎤 Synchronized Karaoke Lyrics**: Fullscreen real-time synced lyrics with line-by-line autoscroll and click-to-seek, powered by LRCLIB.

---

## 🏗️ Architecture

```mermaid
flowchart TD
    subgraph UI["Jetpack Compose UI (com.openfy.music.ui)"]
        Nav[Bottom Navigation Bar: Home, Search, Library, Account]
        MiniPlayer[Floating Bottom Mini-Player]
        NowPlaying[Now Playing Screen: Album Art, Scrubber, Controls]
        LyricsView[Synced Karaoke Lyrics Screen]
        AccountModal[Single-Account Switcher Dialog]
    end

    subgraph Core["Playback & Services (com.openfy.music)"]
        MusicService[MusicPlaybackService: MediaSessionService]
        ExoPlayer[AndroidX Media3 ExoPlayer]
        PlaybackMgr[PlaybackManager Singleton StateFlow]
        AccountMgr[AccountManager: Spotify vs YouTube Library]
    end

    subgraph Data["Remote APIs & Extractors (com.openfy.music.data)"]
        YTEngine[YouTubeAudioResolver: InnerTube Extractor]
        SpotifyClient[SpotifyClient: OAuth PKCE & Library Sync]
        LyricsClient[LyricsClient: LRCLIB Synced Lyrics]
    end

    UI --> PlaybackMgr
    UI --> AccountMgr
    PlaybackMgr --> MusicService
    MusicService --> ExoPlayer
    PlaybackMgr --> YTEngine
    AccountMgr --> SpotifyClient
    AccountMgr --> YTEngine
    NowPlaying --> LyricsView
    LyricsView --> LyricsClient
```

---

## 🛠️ Building the APK

### Prerequisites
* **Android SDK**: API 35 (installed)
* **JDK**: Java 21 or Java 17
* **Gradle**: 8.12 (via Gradle Wrapper included)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📂 Project Structure

```
Openfy/
├── .gitignore
├── README.md
├── build.gradle.kts                      # Root Gradle build configuration
├── settings.gradle.kts                   # Project module dependencies
├── gradle.properties                     # JVM & AndroidX settings
├── gradlew / gradlew.bat                 # Gradle wrapper scripts
├── gradle/
│   ├── libs.versions.toml                # Android version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts                  # App module Gradle configuration
    ├── proguard-rules.pro
    └── src/
        └── main/
            ├── AndroidManifest.xml       # Foreground media service & permissions
            ├── res/                      # Drawables, colors, strings, themes
            └── java/com/openfy/music/
                ├── MainActivity.kt       # Root Compose activity
                ├── OpenfyApplication.kt  # Android application class
                ├── data/
                │   ├── AccountManager.kt # Single-active-account & like sync
                │   ├── model/            # Track, Album, Artist, Playlist, Lyrics
                │   └── remote/
                │       ├── YouTubeAudioResolver.kt # RVX InnerTube audio extractor
                │       ├── SpotifyClient.kt        # Spotify Web API PKCE & library
                │       └── LyricsClient.kt         # Synced lyrics fetcher
                ├── player/
                │   └── PlaybackManager.kt# ExoPlayer state & queue manager
                ├── service/
                │   └── MusicService.kt   # AndroidX MediaSessionService
                └── ui/
                    ├── components/       # MiniPlayer, NowPlaying, Nav, Lyrics
                    ├── screens/          # HomeScreen, SearchScreen, LikedSongsScreen
                    └── theme/            # Spotify dark theme palette & typography
```

---

## 📜 License

MIT License. Built for educational and personal research purposes.
