# SanTube – Just Download

A native Android app (Kotlin + Jetpack Compose) that downloads videos and music from YouTube,
Instagram, Facebook, Reddit and most other websites, then plays everything you saved offline in a
Reels-style feed.

`com.santube` · version 1.1 · Android 7.0 (API 24) and newer · target Android 16 (API 36)

---

## Contents

- [Run it in 5 minutes](#run-it-in-5-minutes)
- [Features](#features)
- [How it works](#how-it-works)
- [Drawbacks and limitations](#drawbacks-and-limitations)
- [Build an APK for phones](#build-an-apk-for-phones)
- [Optional API keys (Magic feed)](#optional-api-keys-magic-feed)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [Credits](#credits)

---

## Run it in 5 minutes

You need **Android Studio** (a recent version) and an internet connection for the first build.
No account, server or API key is needed.

1. On this GitHub page click **Code → Download ZIP**, then extract the ZIP.
2. In Android Studio choose **File → Open** and pick the extracted folder (the one with
   `settings.gradle.kts` in it).
3. Wait for **Gradle sync** to finish. Android Studio downloads Gradle, the Android libraries and
   the download engine by itself - the first sync takes a few minutes. If it asks for an Android
   SDK or a JDK, accept its suggestion (the project uses Java 21, which Android Studio ships with).
4. Connect a phone with USB debugging on (or start an emulator) and press **Run ▶**.

The repository holds only the source code (about 8 MB). Everything else - build output, Gradle
caches, the SDK - is downloaded or rebuilt on your computer, which is why it is not in the repo.

> The debug build Android Studio runs contains the engine for all four CPU types, so it is about
> 330 MB. For a small APK to share, see [Build an APK for phones](#build-an-apk-for-phones).

---

## Features

### Download from a link
- **Share or paste a link** from YouTube, Instagram, Facebook, Reddit or almost any web page.
  SanTube also offers a link you have just copied ("Link you copied" card).
- **"Download video as" sheet** with the video's thumbnail and title, then:
  - **Music** - *Fast* (the site's own M4A audio, no conversion) or *Classic MP3* at 70K, 128K,
    160K or 320K.
  - **Video** - every quality the site offers, from 144p up to 1080p and higher, with file sizes.
  - **Bundle** - video, music and thumbnail together in one folder.
- **Fast MP3 conversion** - the audio is cut into pieces that are encoded on several CPU cores at
  once and joined on exact MP3 frame boundaries, so the joins cannot be heard. The result is
  byte-identical to a normal single-pass encode. A 1 h 35 min track converts in about 40 s
  instead of 70 s. Title, artist, date, cover art and chapters are written into the MP3.
- **Background downloads** that keep going when the app is closed, with **Pause / Resume /
  Cancel** in the app and in the notification. Resume continues from the same byte. A lost
  connection waits and continues by itself.
- **Notifications** with progress, speed and time left; the finished notification shows the
  video's thumbnail and plays the file when tapped.
- Finished files appear straight away in Gallery, WhatsApp, file managers and music apps.

### Watch and manage offline
- **Reels** - a full-screen vertical feed of everything you downloaded (video, audio, images):
  background play, long-press for 2× speed, double-tap to skip ±10 s, seek bar, platform filter,
  full screen in landscape. Files the phone's own decoder cannot play are played with libVLC.
- **Downloads** - running and finished downloads, search, multi-select, rename, delete.
- **Share** - a list with thumbnails: open the file's folder in the file manager, play it inside
  the app, or send it with Android's share sheet.
- **History** - search, filters and a detailed info view for every download (format, size,
  resolution, codecs, source link).
- **Private Safe** - a password-protected place for videos you want to keep private.
- **Hide videos / audio** - keep downloads out of Gallery and music apps (they still play in SanTube).

### Discover
- **Magic** - a personal video feed from free sources (PeerTube, Internet Archive, Wikimedia
  Commons, media.ccc.de, Mastodon, Bluesky, Vimeo, Dailymotion, Pixabay, YouTube) and from
  websites you add yourself. *SanAlgorithm* learns what you like **on the phone only**;
  *SanViral* puts the most-watched videos first.
- **SanTube Browser** - browse any site and tap Download on the video or audio it plays, with a
  built-in ad and tracker blocker (*Shields*) and private browsing.

### Comfort
- **One window only** - sharing several links from different apps never opens extra SanTube
  windows; each link goes to the window that is already open.
- **Back goes Home first** - Back on any tab returns to Home; Back on Home leaves the app.
- **Adjustable bottom bar** - Settings → Bottom bar: change its size (80-170 %), the order of the
  tabs (Home always first) and which tabs are shown.
- Works in the phone's **small floating window**: the download sheet's list scrolls and the
  Download button always stays visible.
- Red-and-white neon "glass" design, dark mode.

---

## How it works

```
 link (share / paste / copied)
        │
        ▼
 PlatformRouter            YouTube / Instagram / Facebook / Reddit / direct file / any web page
        │
        ▼
 yt-dlp analysis           title, thumbnail, duration, every available format
        │
        ▼
 Download sheet            the user picks a format (or a bundle)
        │
        ▼
 DownloadWorker            WorkManager job - survives the app being closed
        ├─ YouTube         yt-dlp's own downloader (fastest for YouTube)
        ├─ other sites     aria2c, 16 connections
        ├─ FFmpeg          joins video + audio streams
        └─ FastMp3Converter  parallel MP3 encoding, tags, cover, chapters
        │
        ▼
 StorageRepository         Download/SanTube folders + Android's MediaStore
        │
        ▼
 Room database             the one library that Home, Reels, Downloads, Share and History all read
```

The download engine is [yt-dlp](https://github.com/yt-dlp/yt-dlp), run inside the app through
[youtubedl-android](https://github.com/junkfood02/youtubedl-android), which ships a real Python
runtime, FFmpeg and aria2c as native libraries. There is no SanTube server. The APK carries a
recent yt-dlp (`app/src/main/res/raw/ytdlp`), SanTube updates it from GitHub at most once a day,
and when a known site suddenly fails it updates yt-dlp and tries once more.

---

## Drawbacks and limitations

- **Large APK.** Python, FFmpeg and libVLC are about 75 MB per CPU type: an APK for one CPU type
  is ~86 MB, the all-in-one debug build ~330 MB.
- **Sites change.** Downloading works only as long as yt-dlp understands the site. YouTube and
  Instagram change often; a new yt-dlp usually fixes it, which SanTube fetches by itself - but
  there can be days when a site fails until yt-dlp catches up.
- **No login.** SanTube does not sign in to sites, so private posts, age-restricted or members-only
  videos fail. Instagram may also refuse a phone that downloads a lot in a short time
  ("login required").
- **MP3 still takes time.** Converting to MP3 is faster than before but not instant (about 40 s for
  95 minutes on a mid-range phone). *Fast* (M4A) needs no conversion at all. To make the parallel
  joins exact, LAME's bit reservoir is switched off - at 320K the difference is negligible.
- **No MP3 for some sites.** Instagram and similar sites have no separate audio stream; music is
  taken from the video.
- **Magic needs keys for some sources.** YouTube, Dailymotion and Pixabay in Magic need free API
  keys (see below); without them those sources are simply skipped.
- **First start is slower.** The Python engine is unpacked on the first launch (a few seconds).
- **Signed with the debug key.** Release builds are signed with the computer's Android Studio debug
  key so they install over earlier builds. For Google Play you need your own release key - and
  Google Play does not allow YouTube downloaders anyway.
- **Lint.** Android's `lintVital` task crashes on this project (a lint tool bug), so release builds
  skip it (see the command below).
- **Tested phones.** Mostly tested on two vivo phones (arm64). The 32-bit build and other brands
  are less tested.
- **Use it responsibly.** Download only what you have the right to download, and respect the
  sites' terms and the creators' copyright.

---

## Build an APK for phones

A release APK for one CPU type is about a quarter of the debug build's size. From the project
folder (Windows; on macOS/Linux use `./gradlew`):

```bat
gradlew.bat :app:assembleRelease -PsantubeAbi=arm64-v8a -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease
```

- `arm64-v8a` - almost every phone from the last 7-8 years. Use `armeabi-v7a` for old 32-bit
  phones. Leave `-PsantubeAbi` out for all four types.
- The APK is written to `app/build/outputs/apk/release/`.
- Unit tests: `gradlew.bat :app:testDebugUnitTest`

---

## Optional API keys (Magic feed)

Create a file named `local.properties` in the project folder (Android Studio usually creates it
with your SDK path - just add lines to it):

```properties
YOUTUBE_API_KEY=your-youtube-data-api-v3-key
DAILYMOTION_API_KEY=...
DAILYMOTION_API_SECRET=...
PIXABAY_API_KEY=...
```

All four are optional. `local.properties` is in `.gitignore` - never commit your keys.

---

## Project structure

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt          the app shell: tabs, Home, bottom bar, share/Back handling
├── DownloadViewModel.kt     link analysis, the download list, settings
├── DownloadWorker.kt        one download: yt-dlp / aria2c / FFmpeg, then saving the file
├── DownloadNotifications.kt every download notification
├── FormatOptions.kt         the Music / Video choices in the download sheet
├── FormatPicker.kt          yt-dlp's format list → rows
├── MagicViewModel.kt        the Magic feed
├── data/
│   ├── YtDlpEngine.kt       starts and updates yt-dlp, FFmpeg, aria2c
│   ├── FastMp3Converter.kt  parallel, seamless MP3 encoding
│   ├── StorageRepository.kt folders, file names, MediaStore
│   ├── LessonRepository.kt  the Room library
│   ├── SanAlgorithm.kt / SanViral.kt   Magic's on-device ranking
│   ├── AdBlocker.kt / BrowserShields.kt  the Browser's blocker
│   └── providers/           Magic's sources (PeerTube, Internet Archive, YouTube, ...)
└── ui/                      Reels, Downloads, Share, History, Settings, Magic, Browser, players
app/src/test/                JVM unit tests (format picker, MP3 joining, bottom bar, ranking ...)
app/src/androidTest/         on-device UI tests
```

---

## Tech stack

| Area | Used |
|---|---|
| Language / UI | Kotlin 2.2, Jetpack Compose (BOM 2026.02.01), Material 3 |
| Download engine | youtubedl-android 0.18.1 (yt-dlp + Python), FFmpeg 7.1, aria2c |
| Playback | AndroidX Media3 1.4.1 (ExoPlayer, MediaSession), libVLC 3.7.7 |
| Background work | WorkManager 2.9 |
| Storage | Room 2.7, SharedPreferences, MediaStore |
| Network / images | OkHttp, Coil |
| Build | Android Gradle Plugin 9.2.1, Gradle 9.6, R8, Java 21 |

---

## Credits

- Developed by [SanStudio](https://sanstudio-hub.github.io/in/) · [Santhosh](https://a-santhosh-hub.github.io/in/)
- Built on [yt-dlp](https://github.com/yt-dlp/yt-dlp),
  [youtubedl-android](https://github.com/junkfood02/youtubedl-android), FFmpeg, aria2c,
  LAME and [libVLC](https://www.videolan.org/vlc/libvlc.html).
