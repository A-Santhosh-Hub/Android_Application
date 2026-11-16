# 🛡️ SanDen — Private File Vault for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg?style=flat&logo=android)](https://android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12.01-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-indigo.svg?style=flat)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-amber.svg?style=flat)](LICENSE)

**SanDen** is a modern, high-performance private file vault for Android built with Jetpack Compose and Clean Architecture. On first launch, it automatically creates a dedicated vault folder on your device. Simply drag, drop, or paste files and folders into this directory via any file manager or your PC over USB. SanDen immediately indexes them and organizes them into **Images**, **Audio**, **Videos**, **Documents**, and **Others** — ensuring **none** of these files ever show up in your Gallery, Google Photos, music players, or media apps.

---

## ✨ Key Features

- **🔒 Total Media Invisibility (`.nomedia`)**: Plants an active `.nomedia` file at the vault root to completely exclude all vault files and subdirectories from Android `MediaStore`. No media scanner or gallery app will index them.
- **🔄 Dual-Mode Storage Engine (`VaultStorage`)**:
  - **Mode A: Drop Folder (Default)**: Direct in-place indexing in `/storage/emulated/0/SanDen/`. Files remain accessible to third-party file managers and USB connections without duplicating storage.
  - **Mode B: Private Import**: The drop folder acts as an external inbox. Newly detected files are moved into app-private internal storage (`context.filesDir/vault/`), rendering them 100% invisible to all other apps and file managers.
- **🛡️ Safe Move & Integrity Guarantee**: Atomic `File.renameTo` for same-volume moves, with streaming 8KB buffer copy + CRC32 checksum verification fallback. **The source file is never deleted unless the destination is verified bit-for-bit.**
- **📁 Full Recursive Directory Browser**: Nested folder navigation with breadcrumb history, folder creation, renaming, and batch delete/export.
- **👁️ Built-in Viewers**:
  - **Image Viewer**: Fullscreen viewer with pinch-to-zoom, pan, share, export, and delete.
  - **Video Player**: Seamless Media3 ExoPlayer with gesture controls and aspect ratio preservation.
  - **Audio Player**: Integrated audio playback engine with mini-player overlay.
- **⚡ Reactive Live Updates**: File changes are monitored using recursive debounced file observers (`FileObserverManager`) and indexed into a Room SQLite database with instant UI updates via Kotlin Flow.
- **🎨 Modern Material 3 UI**: Clean glassmorphism styling, dark mode support, fluid animations, and custom theme tokens.

---

## 🏛 Architecture Overview

SanDen is engineered using **Clean Architecture** and **Unidirectional Data Flow (MVI/MVVM)**:

```
┌────────────────────────────────────────────────────────┐
│                        UI Layer                        │
│   Jetpack Compose + Material 3 (Android 15 / OriginOS) │
│   ViewModels + StateFlow + Kotlin Coroutines           │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                      Domain Layer                      │
│   Use Cases + Models (VaultFile, FileCategory, etc.)   │
│   Contracts: VaultRepository, FileTransferProvider     │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                       Data Layer                       │
│   Storage Engine: DropFolderStorage / PrivateImport    │
│   Room Database: SanDenDatabase + VaultFileDao         │
│   Sync & Scanning: FileScanner + ScanDiff + Observer   │
└────────────────────────────────────────────────────────┘
```

### Module Structure

```
com.sanden
├── data
│   ├── db            # Room Database, VaultFileEntity, TypeConverters, VaultFileDao
│   ├── repository    # VaultRepositoryImpl (Room + Scanner + Observer coordination)
│   ├── scanner       # FileScanner (recursive), ScanDiff, CategoryDetector, FileObserverManager
│   └── storage       # VaultStorage, DropFolderStorage, PrivateImportStorage, SafeFileOperations
├── di                # Hilt Dependency Injection Modules (DatabaseModule, StorageModule)
├── domain
│   ├── model         # VaultFile, FileCategory, StorageMode, FileOperation, ScanResult
│   ├── repository    # VaultRepository & FileTransferProvider (Stage 2 contract)
│   └── usecase       # 11 Granular Single-Responsibility Use Cases
├── service           # FileOperationService (Foreground service with notifications)
└── ui
    ├── browser       # FolderBrowserScreen & FolderBrowserViewModel
    ├── category      # CategoryScreen & CategoryViewModel
    ├── components    # FileCard, CategoryCard, BreadcrumbBar, EmptyState, SortMenu, etc.
    ├── home          # HomeScreen & HomeViewModel
    ├── navigation    # SanDenNavGraph & Screen routes
    ├── onboarding    # OnboardingScreen & OnboardingViewModel (Permission handling)
    ├── settings      # SettingsScreen & SettingsViewModel
    ├── theme         # Material 3 Color palette, Typography, Shapes, Theme
    └── viewer        # ImageViewerScreen, VideoPlayerScreen, AudioPlayerScreen
```

---

## 🛠 Tech Stack

| Technology | Purpose |
| :--- | :--- |
| **Kotlin 2.0.21** | Primary language with Coroutines & StateFlow |
| **Jetpack Compose** | Modern declarative UI toolkit (Compose BOM `2024.12.01`) |
| **Material 3 (M3)** | Contemporary Material You design system |
| **Hilt (Dagger) 2.53.1** | Dependency injection |
| **Room 2.6.1 + KSP** | Local SQLite caching and indexed metadata querying |
| **Media3 ExoPlayer 1.5.1** | High-performance audio & video playback engine |
| **Coil 2.7.0** | Asynchronous image loading and video thumbnail generation |
| **Navigation Compose** | In-app type-safe screen navigation |
| **Gradle 8.9 + AGP 8.7.3** | Build automation with Version Catalog (`libs.versions.toml`) |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Android Studio Ladybug (2024.2.1+) or Koala
- **JDK**: Java 17 or Java 21 (bundled automatically with Android Studio)
- **Android Device / Emulator**: Running Android 8.0 (API 26) or higher

### Opening in Android Studio

1. **Download & Extract**: Download or clone this repository and extract the files:
   ```bash
   git clone https://github.com/your-username/SanDen.git
   ```
2. **Open in Android Studio**:
   - Open Android Studio.
   - Select **File → Open...** and choose the `SanDen` root folder.
3. **Automatic Dependency Sync**:
   - Android Studio will detect the included Gradle Wrapper (`gradlew`) and automatically download Gradle 8.9 and all dependencies.
4. **Run the Project**:
   - Connect your Android device via USB (with USB Debugging enabled) or start an emulator.
   - Click the green **Run (▶)** button or press `Shift + F10`.

### Building from Command Line

```bash
# On Windows
gradlew.bat assembleDebug

# On macOS / Linux
./gradlew assembleDebug
```

The resulting APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

To install directly to a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Permissions & Storage Access

SanDen requests `MANAGE_EXTERNAL_STORAGE` (`ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`) on Android 11+ (API 30+).

- **Why it is needed**: The core functionality of SanDen requires creating a public `/storage/emulated/0/SanDen/` drop folder, placing a `.nomedia` file to keep files hidden from the system gallery, indexing user-dropped folders, and recursively observing file system events.
- **Sideloading Note**: Because Google Play Store policies restrict `MANAGE_EXTERNAL_STORAGE` to file managers and backup tools, SanDen is designed as an independent privacy vault distributed directly via GitHub Releases and APK sideloading.

---

## 🧪 Testing

The project includes unit test suites covering:
- **CategoryDetectorTest**: Tests MIME type detection and extension mappings.
- **ScanDiffTest**: Tests addition, removal, and modification diff calculation algorithms.
- **SafeFileOperationsTest**: Validates streaming copies, CRC32 checksums, collision handling (`name (1).ext`), and source preservation upon failure.

Run the unit tests via Gradle:
```bash
gradlew.bat testDebugUnitTest
```

---

## 🗺️ Roadmap (Upcoming Stages)

- **Stage 2: Local Network File Transfer**: Peer-to-peer Wi-Fi sharing between phone and PC via local HTTP/WebSocket daemon with zero cloud dependency.
- **Stage 3: Vault Security**: Biometric fingerprint / PIN authentication and optional SQLCipher database encryption.
- **Stage 4: Cloud Backup & Sync**: Optional end-to-end encrypted remote sync.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
