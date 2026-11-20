import json
import os
import subprocess
import threading
import time
import tkinter as tk
from tkinter import filedialog, messagebox, scrolledtext
import urllib.parse
import urllib.request

DEFAULT_PORT = 54321
def find_adb():
    """Locate adb: PATH first, then ANDROID_HOME / default SDK locations."""
    import shutil
    found = shutil.which("adb")
    if found:
        return found
    candidates = [
        os.path.join(os.environ.get("ANDROID_HOME", ""), "platform-tools", "adb.exe"),
        os.path.join(os.environ.get("ANDROID_SDK_ROOT", ""), "platform-tools", "adb.exe"),
        os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe"),
        os.path.expanduser("~/Library/Android/sdk/platform-tools/adb"),
        os.path.expanduser("~/Android/Sdk/platform-tools/adb"),
    ]
    for c in candidates:
        if c and os.path.exists(c):
            return c
    return "adb"


ADB_PATH = find_adb()
SAVE_DIR = os.path.join(os.path.expanduser("~"), "Downloads", "SanDen")


def open_path(path):
    """Open a file/folder with the OS default app (Windows, macOS, Linux)."""
    import sys
    if sys.platform.startswith("win"):
        os.startfile(path)
    elif sys.platform == "darwin":
        subprocess.Popen(["open", path])
    else:
        subprocess.Popen(["xdg-open", path])


class SanDenPCApp:
    def __init__(self, root: tk.Tk):
        os.makedirs(SAVE_DIR, exist_ok=True)
        self.root = root
        self.root.title("SanDen PC Continuity Hub")
        self.root.geometry("800x640")
        self.root.configure(bg="#060913")

        self.base_url = tk.StringVar(value=f"http://192.168.0.100:{DEFAULT_PORT}")
        self.status_text = tk.StringVar(value="Not Connected")
        self.seen_ids = set()
        self.polling = True

        self._build_ui()
        self._start_polling()

    def _build_ui(self):
        # Top Header
        header = tk.Frame(self.root, bg="#0F1629", pady=10, padx=14)
        header.pack(fill=tk.X)

        tk.Label(
            header, text="ðŸ›¡ï¸ SanDen Continuity Hub (PC)",
            fg="#F1F5F9", bg="#0F1629", font=("Segoe UI", 13, "bold")
        ).pack(side=tk.LEFT)

        tk.Button(
            header, text="ðŸ“‚ Saved Folder", command=self.open_save_folder,
            bg="#1E2638", fg="#F1F5F9", relief=tk.FLAT, padx=10, pady=4, font=("Segoe UI", 9)
        ).pack(side=tk.RIGHT, padx=(8, 0))

        self.status_lbl = tk.Label(
            header, textvariable=self.status_text,
            fg="#4DB6AC", bg="#18223A", padx=10, pady=4, font=("Segoe UI", 9, "bold")
        )
        self.status_lbl.pack(side=tk.RIGHT)

        # Connection Bar
        conn_bar = tk.Frame(self.root, bg="#0B1220", pady=8, padx=14)
        conn_bar.pack(fill=tk.X)

        tk.Label(conn_bar, text="Phone URL:", fg="#94A3B8", bg="#0B1220", font=("Segoe UI", 9)).pack(side=tk.LEFT)
        self.url_entry = tk.Entry(
            conn_bar, textvariable=self.base_url, bg="#18223A", fg="#F1F5F9",
            insertbackground="#F1F5F9", relief=tk.FLAT, font=("Consolas", 10), width=28
        )
        self.url_entry.pack(side=tk.LEFT, padx=8, ipady=4)

        tk.Button(
            conn_bar, text="Connect Wi-Fi", command=self.check_connection,
            bg="#82B1FF", fg="#06132B", relief=tk.FLAT, padx=10, pady=4, font=("Segoe UI", 9, "bold")
        ).pack(side=tk.LEFT, padx=4)

        tk.Button(
            conn_bar, text="USB Bridge (ADB)", command=self.connect_usb_bridge,
            bg="#4DB6AC", fg="#00201C", relief=tk.FLAT, padx=10, pady=4, font=("Segoe UI", 9, "bold")
        ).pack(side=tk.LEFT, padx=4)

        tk.Button(
            conn_bar, text="ðŸ“‹ Send Clipboard", command=self.send_clipboard,
            bg="#1E2638", fg="#F1F5F9", relief=tk.FLAT, padx=10, pady=4, font=("Segoe UI", 9)
        ).pack(side=tk.RIGHT)

        # Save path hint
        tk.Label(
            self.root,
            text=f"Received mobile files auto-save to: {SAVE_DIR}",
            fg="#64748B", bg="#060913", font=("Segoe UI", 8), anchor="w", padx=16
        ).pack(fill=tk.X, pady=(4, 0))

        # Chat Stream
        self.chat_box = scrolledtext.ScrolledText(
            self.root, bg="#0B101D", fg="#F1F5F9", font=("Segoe UI", 10),
            relief=tk.FLAT, padx=14, pady=12, wrap=tk.WORD, state=tk.DISABLED
        )
        self.chat_box.pack(fill=tk.BOTH, expand=True, padx=14, pady=8)
        self.chat_box.tag_config("PHONE", foreground="#82B1FF")
        self.chat_box.tag_config("LAPTOP", foreground="#4DB6AC")

        # Bottom Composer
        composer = tk.Frame(self.root, bg="#0F1629", pady=10, padx=14)
        composer.pack(fill=tk.X)

        tk.Button(
            composer, text="ðŸ“Ž Send File(s)", command=self.send_files,
            bg="#1E2638", fg="#F1F5F9", relief=tk.FLAT, padx=12, pady=6, font=("Segoe UI", 9, "bold")
        ).pack(side=tk.LEFT, padx=(0, 8))

        self.msg_entry = tk.Entry(
            composer, bg="#18223A", fg="#F1F5F9", insertbackground="#F1F5F9",
            relief=tk.FLAT, font=("Segoe UI", 10)
        )
        self.msg_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, ipady=7, padx=(0, 8))
        self.msg_entry.bind("<Return>", lambda _e: self.send_text())

        tk.Button(
            composer, text="Send", command=self.send_text,
            bg="#82B1FF", fg="#06132B", relief=tk.FLAT, padx=16, pady=6, font=("Segoe UI", 9, "bold")
        ).pack(side=tk.RIGHT)

    def open_save_folder(self):
        os.makedirs(SAVE_DIR, exist_ok=True)
        open_path(SAVE_DIR)

    def connect_usb_bridge(self):
        def worker():
            try:
                adb = ADB_PATH if os.path.exists(ADB_PATH) else "adb"
                subprocess.run([adb, "forward", f"tcp:{DEFAULT_PORT}", f"tcp:{DEFAULT_PORT}"], check=True)
                self.root.after(0, lambda: self.base_url.set(f"http://127.0.0.1:{DEFAULT_PORT}"))
                self.check_connection()
            except Exception as exc:
                self.root.after(0, lambda: messagebox.showerror("USB Bridge", f"ADB forward failed: {exc}"))
        threading.Thread(target=worker, daemon=True).start()

    def check_connection(self):
        def worker():
            url = self.base_url.get().rstrip("/")
            try:
                with urllib.request.urlopen(f"{url}/api/info", timeout=3) as resp:
                    info = json.loads(resp.read().decode("utf-8"))
                name = info.get("deviceName", "Phone")
                self.root.after(0, lambda: self.status_text.set(f"Connected: {name}"))
                self.fetch_messages()
            except Exception:
                self.root.after(0, lambda: self.status_text.set("Offline / Check Phone"))
        threading.Thread(target=worker, daemon=True).start()

    def fetch_messages(self):
        url = self.base_url.get().rstrip("/")
        try:
            with urllib.request.urlopen(f"{url}/api/messages", timeout=3) as resp:
                msgs = json.loads(resp.read().decode("utf-8"))
            self.root.after(0, lambda: self._render_new_messages(msgs))
            self.root.after(0, lambda: self.status_text.set("Connected (Live)"))
        except Exception:
            pass

    def _download_file(self, msg_id: int, file_name: str, open_after: bool = False):
        url = self.base_url.get().rstrip("/")
        safe_name = os.path.basename(file_name) or f"sanden_{msg_id}"
        dest_path = os.path.join(SAVE_DIR, safe_name)

        def worker():
            try:
                if not os.path.exists(dest_path):
                    with urllib.request.urlopen(f"{url}/api/messages/download?id={msg_id}", timeout=60) as resp:
                        with open(dest_path, "wb") as out:
                            while True:
                                chunk = resp.read(8192)
                                if not chunk:
                                    break
                                out.write(chunk)
                if open_after:
                    self.root.after(0, lambda: open_path(dest_path))
            except Exception as exc:
                if open_after:
                    self.root.after(0, lambda: messagebox.showerror("Open Failed", str(exc)))

        threading.Thread(target=worker, daemon=True).start()

    def _render_new_messages(self, msgs):
        new_items = [m for m in msgs if m.get("id") not in self.seen_ids]
        if not new_items:
            return
        self.chat_box.configure(state=tk.NORMAL)
        for m in new_items:
            mid = m.get("id")
            self.seen_ids.add(mid)
            sender = m.get("sender", "PHONE")
            mtype = m.get("type", "TEXT")
            ts = time.strftime("%H:%M", time.localtime(m.get("timestamp", 0) / 1000.0))
            self.chat_box.insert(tk.END, f"[{sender} â€¢ {ts}] ", sender)

            if mtype == "FILE":
                fname = m.get("fileName", "file")
                fsize = m.get("fileSize", 0)
                self.chat_box.insert(tk.END, f"ðŸ“ {fname} ({fsize} B)  ")

                if sender == "PHONE":
                    # Auto-download in background & add clickable Open button
                    self._download_file(mid, fname, open_after=False)
                    btn = tk.Button(
                        self.chat_box, text="Open",
                        command=lambda i=mid, n=fname: self._download_file(i, n, open_after=True),
                        bg="#82B1FF", fg="#06132B", relief=tk.FLAT,
                        padx=8, pady=1, font=("Segoe UI", 8, "bold"), cursor="hand2"
                    )
                    self.chat_box.window_create(tk.END, window=btn)
                self.chat_box.insert(tk.END, "\n")
            elif mtype == "CLIPBOARD":
                text = m.get("textContent", "")
                self.chat_box.insert(tk.END, f"ðŸ“‹ {text}\n")
                if sender == "PHONE" and text:
                    self.root.clipboard_clear()
                    self.root.clipboard_append(text)
            else:
                self.chat_box.insert(tk.END, f"{m.get('textContent', '')}\n")

        self.chat_box.configure(state=tk.DISABLED)
        self.chat_box.see(tk.END)

    def send_text(self, mtype="TEXT", override=None):
        text = override if override is not None else self.msg_entry.get().strip()
        if not text:
            return
        if override is None:
            self.msg_entry.delete(0, tk.END)
        url = self.base_url.get().rstrip("/")

        def worker():
            try:
                payload = json.dumps({"sender": "LAPTOP", "type": mtype, "textContent": text}).encode("utf-8")
                req = urllib.request.Request(
                    f"{url}/api/messages", data=payload,
                    headers={"Content-Type": "application/json"}, method="POST"
                )
                with urllib.request.urlopen(req, timeout=4):
                    pass
                self.fetch_messages()
            except Exception as exc:
                self.root.after(0, lambda: messagebox.showerror("Send Failed", str(exc)))
        threading.Thread(target=worker, daemon=True).start()

    def send_clipboard(self):
        try:
            text = self.root.clipboard_get()
            if text:
                self.send_text(mtype="CLIPBOARD", override=text)
        except Exception:
            messagebox.showinfo("Clipboard", "Clipboard is empty or not text.")

    def send_files(self):
        paths = filedialog.askopenfilenames(title="Select files to send to SanDen Vault")
        if not paths:
            return
        url = self.base_url.get().rstrip("/")

        def worker():
            for p in paths:
                try:
                    name = os.path.basename(p)
                    with open(p, "rb") as f:
                        data = f.read()
                    req = urllib.request.Request(
                        f"{url}/api/upload", data=data,
                        headers={"X-File-Name": urllib.parse.quote(name)}, method="POST"
                    )
                    with urllib.request.urlopen(req, timeout=60):
                        pass
                except Exception as exc:
                    self.root.after(0, lambda e=exc: messagebox.showerror("Upload Failed", str(e)))
            self.fetch_messages()
        threading.Thread(target=worker, daemon=True).start()

    def _start_polling(self):
        def loop():
            while self.polling:
                self.fetch_messages()
                time.sleep(2.0)
        threading.Thread(target=loop, daemon=True).start()


if __name__ == "__main__":
    app_root = tk.Tk()
    app = SanDenPCApp(app_root)

    def on_close():
        app.polling = False
        app_root.destroy()

    app_root.protocol("WM_DELETE_WINDOW", on_close)
    try:
        app_root.mainloop()
    except KeyboardInterrupt:
        on_close()

