# Text to All Text | VS Code Edition 🚀

**Text to All Text** is a powerful, browser-based multi-utility workspace designed with a Visual Studio Code-inspired interface. Built by the **SanStudio Developer Team**, this all-in-one tool operates entirely on the client side. It combines a code editor, media player, document processor, and dozens of developer utilities into a single, seamless, and responsive web application.

Install it as a Progressive Web App (PWA) to use it offline on your desktop or mobile device.

---

## ✨ Core Features

### 💻 Development & Code Tools

* **Multi-Tab Editor:** Edit HTML, CSS, JS, Python, C, Markdown, and JSON with real-time formatting.
* **Project Workspace:** Drag and drop entire folders to explore files in a tree view.
* **Live Split Preview:** Preview HTML/Tailwind/Markdown code instantly. Toggle between Desktop and Mobile viewport sizes.
* **Format & Clean:** Prettier-style formatting, JSON validation, Base64/URL encoding and decoding, and text case conversion.
* **Web Asset & Source Extractor:** Extract raw HTML source code, images, fonts, scripts, and CSS from any website URL (uses CORS proxies).

### 📄 Document & Office Suite

* **PDF Utilities:** View, merge, split, and add watermarks to PDF files using `pdf-lib`.
* **PDF Generator:** Convert active text or code into a downloadable PDF.
* **Digital Signature Pad:** Draw and export signatures as PNGs.
* **Word Processor:** A4-sized rich text editor with full formatting controls and HTML export.
* **Excel/CSV Editor:** Lightweight spreadsheet grid for data entry and CSV exporting.

### 🛠️ Productivity Utilities

* **Keep Notes:** Google Keep-style notes system with color coding and pinning (saves to LocalStorage).
* **To-Do & Alarms:** Task manager with custom audio alarms and browser push notifications.
* **WhatsApp Contact Links:** Generate direct WhatsApp, Telegram, and Call links from raw lists of phone numbers.
* **IN Number Formatter:** Automatically clean, filter, and format Indian phone numbers.
* **QR Code Generator:** Create highly customizable QR codes (colors, center logos) and bulk generate from phone numbers.

### 🎵 Media Tools

* **Mini Media Player:** Picture-in-picture style player for local video/audio and YouTube links. Includes an audio visualizer.
* **YouTube Downloader:** Extract thumbnails in various resolutions (Max, HD, SD) directly from YouTube URLs.

---

## 🧰 Tech Stack & Libraries

This project is built using vanilla web technologies and relies on powerful CDN-based libraries to keep everything running locally in the browser:

* **Frontend:** HTML5, Vanilla JavaScript, [Tailwind CSS](https://tailwindcss.com/) (via CDN).
* **Icons & Fonts:** FontAwesome 6, Google Fonts (Inter, Fira Code).
* **Markdown Parsing:** [Marked.js](https://marked.js.org/)
* **File Compression:** [JSZip](https://stuk.github.io/jszip/) (Download active workspaces as `.zip`).
* **PDF Manipulation:** [PDF-Lib](https://pdf-lib.js.org/) & [jsPDF](https://parall.ax/products/jspdf).
* **QR Codes:** [qr-code-styling](https://qr-code-styling.com/).
* **Media:** YouTube IFrame API.

---

## ⌨️ Keyboard Shortcuts

Navigate and control the workspace just like you would in a native IDE:

| Shortcut | Action |
| --- | --- |
| `Ctrl + N` | Open a New Tab |
| `Ctrl + O` | Open File (Prompt) |
| `Ctrl + S` | Save / Download Active File |
| `Ctrl + B` | Toggle Sidebar Visibility |
| `Ctrl + Enter` | Format Active Code |
| `Alt + Z` | Toggle Word Wrap |
| `Alt + P` | Toggle Split Live Preview |
| `Alt + W` | Close Active Tab |
| `Ctrl + Shift + E` | Focus Explorer Sidebar |
| `Ctrl + Shift + F` | Focus Tools Sidebar |
| `Alt + 1...9` | Switch between open tabs |

---

## 🚀 Installation & Usage

Because this tool is completely client-side, installation is incredibly simple:

1. **Clone or Download** the repository.
2. Ensure `manifest.json` and the main `index.html` file are in the same directory.
3. Open `index.html` in any modern web browser (Chrome, Edge, Firefox, Safari).
4. **(Optional) Install as PWA:** Click the "Install App" button in the header (or use your browser's install prompt) to add the app to your Desktop or Mobile home screen for a native, offline-capable experience.

---

## 📱 Mobile Responsiveness

The UI is heavily optimized for mobile devices:

* Sidebar converts into a full-screen sliding overlay.
* A bottom navigation bar replaces the desktop activity bar.
* Tools and generators are accessible via a swipe-up bottom drawer.
* Touch-friendly tabs and responsive editor font sizing.

---

## 👨‍💻 Credits

**Developed By:** SanStudio Developer Team (San Developers)

**Theme Inspiration:** Visual Studio Code (Dark+, Light+, Monokai)
