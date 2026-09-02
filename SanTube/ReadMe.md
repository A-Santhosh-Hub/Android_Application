
 # SanTube
 # SanTube – Just Download

 # https://drive.google.com/file/d/1x5VWtlueRhYVsiGZgypKSJeOrcbJZPtL/view?usp=sharing

 A Brave-inspired, user-first Android application designed for students to download, manage, and share educational content offline.
 > **Learn Offline, Share Knowledge**  
 > A premium, high-performance Android application built with Kotlin and Jetpack Compose for downloading, managing, and viewing tutorial videos and audio offline.
​
 ---

 ## 🚀 Key Features

 - **Multi-Platform Support**: Effortlessly download videos from YouTube, Instagram, and Facebook.
 - **Social Quick Links**: Instantly launch YouTube, Instagram, or Facebook from the Home screen to find educational content.
 - **Image Post & Carousel Download**: Save high-quality images and multi-image posts from Instagram and Facebook as organized ZIP galleries.
 - **Video & Audio Download (YouTube)**: Choose between high-quality video or extract audio directly as MP3 for YouTube links.
 - **Settings & Customization**: Manage Quick Link visibility and app preferences in the dedicated Settings tab.
 - **Brave-Inspired UI**: Clean, modern interface with dark and light mode support.
 - **Download Management**: Pause, resume, and cancel active downloads.
 - **Village Sharing**: Share downloaded lessons with peers offline via Android's native sharing.
 - **Learning History**: Keep track of your downloaded tutorials and educational content.
 - **Auto-Syncing Counts**: Accurate item counts across all screens (Home, Downloads, Share, History).
 ### 📥 Powerful Multi-Platform Downloader
 * **Universal Link Detection**: Download high-quality videos and audio from YouTube, Instagram, Facebook, and web links.
 * **Format & Quality Selector**: Choose between multiple video resolutions (1080p, 720p, 360p) or high-bitrate MP3 audio options (~320kbps, ~128kbps).
 * **WorkManager Integration**: Background download task queue with pause, resume, cancel, and progress notifications.

 ## 🛠 Technology Stack
 ### 🎬 Instagram-Style Reels Player
 * **Immersive Vertical Feed**: Swipe through your downloaded video tutorials and audio lessons in a full-screen vertical pager (`VerticalPager`).
 * **Advanced Gesture Controls**:
   * ⚡ **Long-Press 2X Playback**: Press and hold anywhere on screen to speed up playback to 2X.
   * ⏩ **Double-Tap Skip**: Double-tap right to skip +10s forward; double-tap left to skip -10s backward.
   * 🎚️ **Interactive Seeker Bar**: Bottom progress bar for scrubbing to any timestamp.
 * **Music Reels UI**: Dedicated animated UI with rotating album artwork for MP3 audio files.

 - **UI Framework**: Jetpack Compose
 - **Language**: Kotlin
 - **Architecture**: MVVM (Model-View-ViewModel) / Functional UI State
 - **Video Engine**: [youtubedl-android](https://github.com/junkfood02/youtubedl-android)
 - **Utilities**: FFmpeg, Aria2c
 - **Storage**: External Storage (Downloads/SanTube directory)
 ### 🤝 Offline Peer Sharing
 * **Village Sharing**: Share downloaded educational content with peers offline without wasting mobile data.

 ## 📦 Setup and Installation
 ### 🎨 Premium Modern UI/UX
 * **Dark Charcoal Aesthetics**: Sleek dark mode design system inspired by modern production apps.
 * **Quick Links**: Quick shortcuts for major platforms (YouTube, Instagram, Facebook).
 * **6-Item Navigation Bar**: Single-line bottom bar featuring **Home**, **Reels**, **Downloads**, **Share**, **History**, and **Profile**.
​
 ---
​
 ## 🛠️ Tech Stack & Architecture
​
 * **Language**: Kotlin 2.2
 * **UI Framework**: Jetpack Compose with Material 3
 * **Media Playback**: `androidx.media3` (ExoPlayer & Media3 UI)
 * **Download Engines**:
   * [`youtubedl-android`](https://github.com/junkfood02/youtubedl-android)
   * FFmpeg & Aria2c integration
 * **Background Tasks**: AndroidX `WorkManager` & `CoroutineWorker`
 * **Local Storage & Database**: Room Persistence Library (`2.7.0-alpha11` with KSP2)
 * **Image Loading**: Coil (`coil-compose`)
 * **Networking**: OkHttp 4.12
 * **Asynchronous Execution**: Kotlin Coroutines & `StateFlow`
​
 ---
​
 ## 📱 Screenshots & UI Layout
​
 ```
 ┌──────────────────────────────────────┐
 │  SanTube                             │
 │  Just Download          [☀️]  [⚙️]   │
 │                                      │
 │ ┌──────────────────────────────────┐ │
 │ │ 🔗 Paste YouTube / IG / FB link  │ │
 │ │                                  │ │
 │ │        [  Get Video  ]           │ │
 │ └──────────────────────────────────┘ │
 │                                      │
 │ ┌──────────────┐  ┌──────────────┐ │
 │ │ 📁 (42)      │  │ 🔗 (42)      │ │
 │ │ My Downloads │  │ Share Videos │ │
 │ └──────────────┘  └──────────────┘ │
 │                                      │
 │ ┌──────────────────────────────────┐ │
 │ │ Quick Links 🚀                   │ │
 │ │   (▶️)          (📷)       (🌐)  │ │
 │ │  YouTube     Instagram  Facebook │ │
 │ └──────────────────────────────────┘ │
 │                                      │
 │ [Home] [Reels] [Downloads] [Share]   │
 └──────────────────────────────────────┘
 ```
​
 ---
​
 ## 🚀 Getting Started

 ### Prerequisites
 - Android Studio Iguana or newer
 - Android SDK 26+ (Android 8.0+)
 - Internet connection for initial video engine updates
 * **Android Studio**: Ladybug / 2024.1+ (or latest Canary)
 * **JDK**: Java 11 or JDK 17
 * **Android SDK**: `compileSdk = 36` (Android 16 / Android 14+ compatible)
 * **Minimum Android Version**: Android 7.0 (API level 24+)

 ### Building the Project
 1. Clone the repository or open the project folder in Android Studio.
 2. Allow Gradle to sync and download dependencies.
 3. Build and run the `app` module on an emulator or physical device.
 ### Installation & Build

 ## 📂 Project Structure
 1. **Clone the Repository**:
    ```bash
    git clone https://github.com/SanStudio-Hub/SanTube.git
    cd SanTube
    ```

 - `MainActivity.kt`: The main entry point containing the Compose UI logic and navigation.
 - `Lesson`: Data model representing a downloaded file.
 - `DownloadTask`: Data model representing an active download process.
 - `downloadVideoFunctional`: Core logic for fetching video metadata and handling the download process.
 2. **Open in Android Studio**:
    Open Android Studio and select **Open an Existing Project**, then select the project folder.

 ## 🤝 Contributing
 3. **Build the Project**:
    Run a Gradle build from terminal or Android Studio:
    ```bash
    ./gradlew :app:assembleDebug
    ```

 This project is developed by **SanStudio**. Feel free to explore and suggest improvements!
 4. **Run on Device / Emulator**:
    Connect an Android device with USB Debugging enabled, or launch an Emulator, then click **Run** (`Shift + F10`).

 ---
 *Learn Offline, Share Knowledge.*
​
 ## ⚙️ Permissions Required
​
 The application requires the following permissions declared in `AndroidManifest.xml`:
 * `INTERNET` - To fetch video details and download files.
 * `POST_NOTIFICATIONS` - For download progress notifications (Android 13+).
 * `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` / `READ_MEDIA_IMAGES` - Media permissions for Android 13+.
 * `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` - File permissions for legacy Android versions.
 * `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC` - For background download execution.
​
 ---
​
 ## 👨‍💻 Developer & Credits
​
 * **Developed by**: [SanStudio](https://sanstudio-hub.github.io/in/)
 * **License**: Open-source educational software.
​
 ---
​
 *SanTube – Dedicated to providing students with the best offline learning experience. Download once, learn anywhere.*


 --------


 Full focus working on SunTube
