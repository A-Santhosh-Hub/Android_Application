# SanBar – Music & Phone Stats Widgets

Two home-screen widgets for Android, made to look clean on any wallpaper:

- **SanBar** – a full-width music bar: cover, song, ⏮ ⏯ ⏭, phone battery and Bluetooth headphone battery.
- **SanStats** – CPU, RAM and temperature rings.

Both share one look: your colour, an optional RGB border effect, see-through glass or a fully transparent background.

`com.sanstudio.sanbar` · version 1.0 · Android 12 (API 31) and newer · target Android 16 (API 36) · APK 1.6 MB

---

## Contents

- [Download](#download)
- [Features](#features)
- [Permissions](#permissions)
- [Build from source](#build-from-source)
- [How it works](#how-it-works)
- [Limitations](#limitations)
- [Project structure](#project-structure)
- [Credits](#credits)

---

## Download

A ready-made APK is in this repository: **[`release/SanBar-v1.0.apk`](release/SanBar-v1.0.apk)**.

1. Download the APK on your phone and open it (allow "install unknown apps" for your browser or file manager if Android asks).
2. Open **SanBar** and allow **Music access** and **Nearby devices** (see [Permissions](#permissions)).
3. Tap **Add SanBar** and **Add SanStats**, or long-press the home screen → **Widgets** → SanBar.

---

## Features

### SanBar – music bar
- Works with **any music app** (YouTube, Spotify, SanTube, the phone's own player...): cover, title, artist and app name.
- **Previous / Play-Pause / Next** buttons; tap the song to open the app that is playing.
- **1 row** = slim bar, **2 rows** = big card with a running time (`1:23 ━━━── 3:45`) and bigger buttons.
- **Phone battery ring** – turns red at 15 % and below.
- **Charging animation** – a spinning comet around the battery ring: **yellow** for normal charging, **red and faster** for fast charging.
- **Headphone battery ring** – for Bluetooth headphones/earbuds that report their battery.
- **Visualizer** behind the song:
  - **Animated** – bars dance while music plays (no permission, almost no battery).
  - **Real (beat)** – bars follow the actual music (see [Permissions](#permissions)).

### SanStats – phone stats
- **CPU** – current load, plus the fastest core's GHz on the 2-row size.
- **RAM** – used %, plus used / total GB on the 2-row size.
- **TEMP** – battery temperature; the ring shows how close the phone is to slowing down from heat and turns green → yellow → orange → red (Cool / Warm / Hot / Very hot / Critical).
- Refreshes every 4 seconds, only while the screen is on.

### Customize (in the SanBar app)
- **Colour** – 9 presets, a hue slider for any colour, or **Album colour** (follows the song cover).
- **Glass darkness** – from clear to solid.
- **RGB effect** – Rainbow spin, Colour cycle or Breathing, normal or fast. These run inside the home screen itself, so SanBar uses almost no battery for them.
- **Transparent background** – separately for the music bar and SanStats: no box, only the controls/rings with soft text shadows.

---

## Permissions

| Permission | Why |
|---|---|
| **Notification access** ("Music access") | Android requires it before an app may read other apps' music sessions. SanBar turns **every notification type off** for itself (`disabled_filter_types`) and never reads notifications. It also keeps the widgets live without a "running" notification. |
| **Nearby devices** (`BLUETOOTH_CONNECT`) | Name and battery of the connected headphones. |
| **Microphone** (`RECORD_AUDIO`) – optional | Only for **Visualizer → Real (beat)**, asked when you pick it. Android's `Visualizer` API reads the sound going **to the speaker/headphones**, not the microphone, but Android files it under the microphone permission. It runs only while music plays and the screen is on. |

SanBar has **no internet permission at all** – no ads, no analytics, no account; nothing leaves the phone.

---

## Build from source

You need **Android Studio** (a recent version). No API key or account is needed.

1. Click **Code → Download ZIP** and extract it, or clone the repository.
2. In Android Studio choose **File → Open** and pick the folder with `settings.gradle.kts`.
3. Wait for **Gradle sync** (the first sync downloads Gradle and the Android libraries).
4. Connect a phone with USB debugging on and press **Run ▶**.

Release APK (R8-shrunk, about 1.6 MB) from the project folder on Windows (`./gradlew` on macOS/Linux):

```bat
gradlew.bat :app:assembleRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease
```

The APK is written to `app/build/outputs/apk/release/`. It is signed with your computer's Android Studio debug key; create a real release key before publishing to the Play Store.

Built with AGP 9.2.1, Gradle 9.6.0, Kotlin 2.2.10 and the Java 21 that ships with Android Studio.

---

## How it works

| Feature | Android API |
|---|---|
| Song, cover, controls | `MediaSessionManager.getActiveSessions` + `MediaController.TransportControls` (needs a granted `NotificationListenerService`) |
| Always-live updates | The granted notification listener stays bound by Android, so it can watch music, battery and Bluetooth without a foreground service |
| Phone battery & charging | `ACTION_BATTERY_CHANGED`, `BatteryManager`; fast charge = vivo's `engine` battery extra **or** ≥ 15 W measured from current × voltage |
| Headphone battery | A2DP / HEADSET profile proxies + `BluetoothDevice.getBatteryLevel()` (hidden API, reached by reflection) and the `BATTERY_LEVEL_CHANGED` broadcast |
| CPU load | Average of each core's current ÷ max speed (`/sys/devices/system/cpu/*/cpufreq`) – `/proc/stat` is blocked for apps on Android 8+ |
| RAM | `ActivityManager.getMemoryInfo` |
| Temperature | Battery temperature + `PowerManager.getCurrentThermalStatus` / `getThermalHeadroom` |
| Colours | `RemoteViews.setColorStateList` on tint methods that Android allows in widgets (`setProgressTintList`, `setIndeterminateTintList`, `setImageTintList`, `setBackgroundTintList`) |
| RGB effects, charging comet, animated bars | Indeterminate `ProgressBar`s with rotate / animation-list drawables – they animate inside the launcher, so the app does no work per frame |
| Real visualizer | `android.media.audiofx.Visualizer` on the output mix; only the 24 bar heights are sent with `partiallyUpdateAppWidget` about 15 times a second. Each widget uses one layout (slim or tall, chosen by its height) so partial updates merge correctly |

The vivo-specific details (the `engine` fast-charge extra, which tint methods are allowed, and why GPU usage is unavailable) were found by reverse-engineering the phone's own system apps and framework with REA.

---

## Limitations

- **No GPU usage.** MediaTek GPU files are blocked for apps, and vivo's own route (`vivo_daemon.service`) is hidden from normal apps. Root would be needed.
- **CPU % is a load estimate** from core speeds, not exact `/proc/stat` usage (blocked for apps).
- **Detailed CPU/GPU/skin temperatures** need system permissions; SanStats shows battery temperature and Android's thermal status instead.
- **Left/right earbud battery separately** works only for the phone maker's own apps; SanBar shows one value.
- **Fast-charge detection** uses vivo's `engine` extra; on other phones it relies on the ≥ 15 W measurement.
- **Real visualizer** may fall back to animated bars on phones that hide the output audio (some offloaded music playback); this happens automatically.
- Tested on an iQOO phone (vivo OriginOS, Android 16). Other launchers may round or pad widgets differently.

---

## Project structure

```
app/src/main/java/com/sanstudio/sanbar/
├── MainActivity.kt          Setup + Customize screen (Jetpack Compose)
├── SanBarWidget.kt          Music bar widget provider (button actions)
├── StatsWidget.kt           SanStats widget provider
├── WidgetRenderer.kt        Builds the music bar RemoteViews, colours, effects, spectrum frames
├── StatsRenderer.kt         Builds the SanStats RemoteViews
├── MediaListenerService.kt  Always-on engine: music sessions, battery/Bluetooth events, refresh ticks
├── MediaRepo.kt             Now playing, controls, cover + album colour
├── BatteryRepo.kt           Phone battery, charging speed, headphone battery
├── StatsRepo.kt             CPU, RAM, temperature
├── SpectrumEngine.kt        Real visualizer (FFT → 24 bars)
└── Prefs.kt                 Customize settings
app/src/main/res/
├── layout/                  widget_slim / widget_tall, stats_slim / stats_tall, fx_layer, viz_bars
├── drawable/                rings, RGB effects, charging comet, visualizer bars, icons
└── xml/                     widget provider info
release/SanBar-v1.0.apk      Ready-made APK
```

---

## Credits

- Developed by [SanStudio](https://sanstudio-hub.github.io/in/) · [Santhosh](https://a-santhosh-hub.github.io/in/)
- Built with Kotlin, Jetpack Compose and Android `RemoteViews` app widgets.
- Icons drawn from Material Design icon shapes.
