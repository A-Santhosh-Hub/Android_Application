# SanDen — Stage 1 Manual Test Checklist

This checklist provides step-by-step instructions for verifying all features, privacy guarantees, and storage mechanics of SanDen Stage 1.

---

## 1. Fresh Installation & Permission Flow

- [ ] **First Launch**: Install and open the debug APK on an Android 11+ device.
  - *Expected*: The friendly Onboarding screen appears explaining the purpose of All Files Access.
- [ ] **Permission Denied / Back Pressed**: Tap "Grant File Access", but return from Settings without enabling the toggle.
  - *Expected*: App remains on the Onboarding screen and displays a clear message inviting the user to try again.
- [ ] **Permission Granted**: Tap "Grant File Access" and toggle "Allow management of all files" ON. Return to the app.
  - *Expected*: App automatically detects the granted state and transitions smoothly to the Home screen.
- [ ] **Folder Auto-Creation**:
  - Open any file manager (or adb shell) and navigate to `/storage/emulated/0/`.
  - *Expected*: A folder named `SanDen` exists: `/storage/emulated/0/SanDen/`.
  - *Expected*: A hidden file named `.nomedia` exists directly inside `/storage/emulated/0/SanDen/.nomedia`.

---

## 2. Core Privacy & Drop Folder Verification (Mode A)

- [ ] **Step 2.1 — Prepare Test Files**:
  Gather the following sample files on your PC or in your Downloads folder:
  1. `test_photo.jpg` (or `.heic`, `.png`)
  2. `test_audio.mp3` (or `.flac`, `.opus`)
  3. `test_video.mkv` (or `.mp4`)
  4. `test_document.pdf` (or `.docx`, `.epub`)
  5. A nested directory: `NestedFolder/sub_photo.webp` and `NestedFolder/archive.zip`

- [ ] **Step 2.2 — Drop Files via File Manager or PC (USB)**:
  - Copy all test files and `NestedFolder/` into `/storage/emulated/0/SanDen/`.
  
- [ ] **Step 2.3 — Verify SanDen App Indexing**:
  - Open (or bring to foreground) SanDen.
  - *Expected*: The category cards immediately show:
    - **Images**: 2 files (including the nested one)
    - **Audio**: 1 file
    - **Videos**: 1 file
    - **Documents**: 1 file
    - **Others**: 1 file (`archive.zip`)
  - Tap **Images**: Both `test_photo.jpg` and `sub_photo.webp` appear with thumbnails.
  - Tap **Browse All Folders**: `NestedFolder` is visible as a directory; tapping it enters the folder and displays breadcrumbs (`SanDen Root > NestedFolder`).

- [ ] **Step 2.4 — Verify Privacy (Media Invisibility Guarantee)**:
  - Open the device's system **Gallery** or **Google Photos**.
    - *Expected*: `test_photo.jpg` and `sub_photo.webp` do **NOT** appear anywhere in the photo feed or albums.
  - Open the default **Music Player** (or Spotify local files / YouTube Music).
    - *Expected*: `test_audio.mp3` is **NOT** listed in songs or library.
  - Open the default **Video Player** (or VLC / MX Player).
    - *Expected*: `test_video.mkv` is **NOT** listed in video library.

---

## 3. Storage Mode B: Private Import Mode

- [ ] **Step 3.1 — Switch Storage Mode**:
  - Go to **Settings** in SanDen.
  - Select **Mode B: Private Import**. Confirm the switch in the warning dialog.
- [ ] **Step 3.2 — Drop New Files into `/SanDen/`**:
  - Copy a new file `private_doc.pdf` into `/storage/emulated/0/SanDen/`.
- [ ] **Step 3.3 — Trigger Sync / Resume**:
  - Resume the app or tap "Rescan Vault Now" in Settings.
  - *Expected*: `private_doc.pdf` is safely moved into app internal storage (`/data/user/0/com.sanden/files/vault/`).
  - *Expected*: `private_doc.pdf` is **completely removed** from `/storage/emulated/0/SanDen/`.
  - *Expected*: The file is now **100% invisible** to all third-party file managers and PC USB browsing.
  - *Expected*: In SanDen, `private_doc.pdf` appears under **Documents** and can be viewed normally.

---

## 4. File Operations & Viewers

- [ ] **Multi-Selection & Batch Delete**:
  - Open a category or folder. Long-press any file to enter selection mode.
  - Select 2 items and tap the Trash icon. Confirm deletion.
  - *Expected*: Items are deleted from disk and immediately disappear from the UI.
- [ ] **File Rename & Collision Handling**:
  - Long-press a single file and tap Edit/Rename. Change the name.
  - *Expected*: File is updated on disk and reflected in the database.
  - Import a file with an identical name.
  - *Expected*: SanDen auto-renames it to `name (1).ext` without overwriting or error.
- [ ] **Export to Downloads**:
  - Select any file in the vault and tap the **Export** icon.
  - *Expected*: File is exported into `/storage/emulated/0/Download/`.
- [ ] **Create Folder & Move**:
  - In "Browse All Folders", tap the New Folder icon and create `Classified`.
  - Select a file, tap Move, and select `Classified`.
  - *Expected*: File moves into the new folder and database updates without rescan needed.
- [ ] **Image Viewer**:
  - Tap any photo: Opens full-screen dark viewer.
  - Pinch-to-zoom smoothly up to 5x.
  - Swipe left/right between images.
  - Tap once to hide/show action bars.
- [ ] **Video Player**:
  - Tap any video: Opens Media3 ExoPlayer viewer.
  - Play, pause, scrub timeline, and verify audio/video sync.
- [ ] **Audio Player**:
  - Tap any audio track: Opens dedicated audio player with waveform icon.
  - Scrub slider, press play/pause, test 10s skip buttons.

---

## 5. Stress & Edge Case Scenarios

- [ ] **Large File (> 1GB)**:
  - Copy a 1GB+ video or zip file into `/SanDen/`.
  - *Expected*: Indexing finishes smoothly without `OutOfMemoryError` (uses streaming 8KB buffers).
- [ ] **Special Characters & Unicode**:
  - Add files named `レポート_2026.pdf`, `photo 📸.heic`, `data[v1]#test&final.csv`.
  - *Expected*: All indexed, displayed, and opened without encoding errors.
- [ ] **Sudden Kill / Interruption**:
  - During a large file move or import, kill the app from Recent Apps.
  - *Expected*: No half-deleted or corrupt files; source file remains intact if destination verification had not completed.
