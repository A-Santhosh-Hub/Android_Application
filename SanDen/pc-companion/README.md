# SanDen PC Companion

Single-file desktop client for the SanDen Android app. Python 3.8+ only, **no extra packages** (uses `tkinter` + standard library).

```bash
python sanden_pc.py        # Windows: double-click Run-SanDen-PC.bat
```

1. Open **Phone ↔ Laptop Sync** on the phone and note the URL (`http://<ip>:54321`).
2. Paste it into the app → **Connect Wi-Fi**. Or plug in USB → **USB Bridge (ADB)**.
3. Chat, sync clipboard, send files. Received files are saved to `~/Downloads/SanDen/`; click **Open** next to a file in the chat.
