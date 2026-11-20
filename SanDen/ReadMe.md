# 🛡️ SanDen — Private Vault + Phone ↔ PC Continuity Hub

**SanDen** is a free, open-source Android app that does two things:

1. **Private file vault** – drop any file into a hidden folder; it is organised into Images / Audio / Video / Documents / Others and **never appears in Gallery, Google Photos or music/video apps** (`.nomedia`, or fully private storage).
2. **Phone ↔ PC Continuity Hub** – a private, zero-cloud alternative to "messaging yourself on WhatsApp". Over your own Wi-Fi (or a USB cable) you can:
   - 💬 chat text / links / code snippets between phone and PC
   - 📋 sync the clipboard (copy on phone → paste on PC, and back)
   - 📁 send images, videos, music, APKs, any file (multi-GB, streamed) in both directions
   - 🖱️ click **Open** on the PC to open anything received from the phone

No account, no internet, no cloud. Everything stays on your network.

> Built with Kotlin, Jetpack Compose (Material 3, "Liquid Glass" UI), Hilt, Room, Media3, Coil. The PC companion is a single Python file (no pip packages).

---

## 🚀 Quick start (3 steps)

### 1. Run the Android app
1. Install **[Android Studio](https://developer.android.com/studio)** (latest stable).
2. Download this repo as ZIP (**Code → Download ZIP**) and unzip it.
3. In Android Studio: **File → Open →** select the unzipped folder.
   Gradle downloads everything automatically (first sync takes a few minutes; Android Studio creates `local.properties` for you).
4. Plug in your phone (USB debugging on) or start an emulator, then press ▶ **Run**.
5. On first launch grant **"All files access"** when asked (needed to create the vault folder).

### 2. Run the PC companion
Requires **Python 3.8+** (Windows / macOS / Linux, `tkinter` included with the standard installer).

```bash
cd pc-companion
python sanden_pc.py          # or double-click Run-SanDen-PC.bat on Windows
```

### 3. Connect
- On the phone open **Phone ↔ Laptop Sync** → the hub starts and shows `http://<phone-ip>:54321`.
- On the PC type that URL into the app and press **Connect Wi-Fi**.
- No common Wi-Fi / router blocks devices? Plug in USB and press **USB Bridge (ADB)** (needs `adb` from Android SDK platform-tools; found automatically).

Both devices must be on the same network for Wi-Fi mode.

---

## 📂 Where do files go?

| Direction | Saved to |
|---|---|
| PC → Phone | Your **SanDen vault** (`/storage/emulated/0/SanDen/`, or app-private storage in *Private Import* mode) |
| Phone → PC | `~/Downloads/SanDen/` (button **📂 Saved Folder** opens it) |

---

## 🔒 Storage modes (Settings)

| Mode | Behaviour |
|---|---|
| **Drop Folder** (default) | Files live in `/storage/emulated/0/SanDen/` with a `.nomedia` file. Visible to file managers, hidden from media apps. |
| **Private Import** | The folder is an inbox; files are moved (verified with CRC32) into app-private storage and vanish from every other app. |

---

## 🏗️ Architecture

```
app/src/main/java/com/sanden/
├── data/        storage engine, Room DB, scanner, LAN hub (HTTP + WebSocket + mDNS)
├── domain/      models, VaultRepository, FileTransferProvider, use cases
├── service/     foreground service for long file operations
└── ui/          Compose screens: home, category, browser, viewers, connect, settings
pc-companion/    sanden_pc.py  (Tkinter desktop client)
```

The phone runs a tiny embedded server (`LanTransferHub`):

| Endpoint | Purpose |
|---|---|
| `GET /api/info` | device info |
| `GET/POST /api/messages` | chat + clipboard stream |
| `POST /api/upload` | stream a file into the vault |
| `GET /api/vault/files`, `/api/vault/download?id=` | browse / download vault files |
| `GET /api/messages/download?id=` | download a file shared in chat |
| `WS /ws` | live push updates |

Default port **54321** (falls back to a free port if busy; the real URL is shown in the app).

---

## ⚠️ Security notes (please read)

- The hub currently has **no authentication or encryption** — use it only on **trusted networks** (home Wi-Fi, USB). PIN pairing + TLS is the top roadmap item.
- The app uses `MANAGE_EXTERNAL_STORAGE`, so it is meant for **sideloading / GitHub releases**, not Google Play.
- Files in *Drop Folder* mode are still visible to file managers (only hidden from media apps).

---

## 🗺️ Roadmap

See [ROADMAP.md](ROADMAP.md) — upcoming: PIN pairing + TLS, biometric app lock, **tablet / large-screen layout**, Windows `.exe` build, screen mirroring, folder transfer, QR connect.

## 🧪 Tests

```bash
./gradlew testDebugUnitTest        # Windows: gradlew.bat testDebugUnitTest
./gradlew assembleDebug            # APK → app/build/outputs/apk/debug/
```
See [TEST_CHECKLIST.md](TEST_CHECKLIST.md) for the manual test list.

## 🤝 Contributing

Issues and pull requests are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md).

## 📄 License

[MIT](LICENSE)
