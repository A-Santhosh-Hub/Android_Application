# Edu Video Downloader

A Brave-inspired, user-first Android application designed for students to download, manage, and share educational content offline.

## 🚀 Key Features

- **Multi-Platform Support**: Effortlessly download videos from YouTube, Instagram, and Facebook.
- **Brave-Inspired UI**: Clean, modern interface with dark and light mode support.
- **Download Management**: Pause, resume, and cancel active downloads.
- **Village Sharing**: Share downloaded lessons with peers offline via Android's native sharing.
- **Learning History**: Keep track of your downloaded tutorials and educational content.
- **Auto-Syncing Counts**: Accurate item counts across all screens (Home, Downloads, Share, History).

## 🛠 Technology Stack

- **UI Framework**: Jetpack Compose
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel) / Functional UI State
- **Video Engine**: [youtubedl-android](https://github.com/junkfood02/youtubedl-android)
- **Utilities**: FFmpeg, Aria2c
- **Storage**: External Storage (Downloads/SanTube directory)

## 📦 Setup and Installation
![App Screenshot](https://github.com/A-Santhosh-Hub/Android_Application/blob/main/Edu%20Video%20Downloader/Screenshot/1.jpeg)
![App Screenshot](https://github.com/A-Santhosh-Hub/Android_Application/blob/main/Edu%20Video%20Downloader/Screenshot/2.jpeg)
### Prerequisites
- Android Studio Iguana or newer
- Android SDK 26+ (Android 8.0+)
- Internet connection for initial video engine updates

### Building the Project
1. Clone the repository or open the project folder in Android Studio.
2. Allow Gradle to sync and download dependencies.
3. Build and run the `app` module on an emulator or physical device.

## 📂 Project Structure

- `MainActivity.kt`: The main entry point containing the Compose UI logic and navigation.
- `Lesson`: Data model representing a downloaded file.
- `DownloadTask`: Data model representing an active download process.
- `downloadVideoFunctional`: Core logic for fetching video metadata and handling the download process.

## 🤝 Contributing

This project is developed by **SanStudio**. Feel free to explore and suggest improvements!
https://sanstudio-hub.github.io/in/
---
*Learn Offline, Share Knowledge.*
