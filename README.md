# Android_Application List 

================================

📞 San Dial – Simple Android Phone Dialer  2

San Dial is a lightweight, clean, and modern Android phone dialer app focused on simplicity, speed, and usability.
It provides an easy-to-use dial pad, call history, contacts access, and a Favorites contacts feature for quick calling.

Developed by SanStudio
🌐 https://sanstudio.neocities.org/

✨ Features
🔢 Dialer

Clean numeric dial pad (0–9, *, #)

Live number preview while typing

Backspace & clear input

Uses Android system dial intent (ACTION_DIAL)

Smooth touch feedback & ripple effects

📜 Recents / Call Log

View recent calls (incoming, outgoing, missed)

Tap to call again quickly

Long-press options (call, add to favorites)

👤 Contacts

Access phone contacts

Search contacts instantly

Tap contact to dial

Add/remove contacts from favorites

⭐ Favorites

Save frequently used contacts

Add from Contacts, Recents, or Dialer

Persistent storage (saved even after app restart)

Clean list / grid UI

One-tap calling

Remove or edit favorite contacts

🌙 Dark Mode

Supports Light & Dark themes

Can follow system theme

🎨 UI / UX Design

Minimal & modern 2025-style UI

Rounded buttons and cards

Calm accent color for call actions

Bottom navigation for easy access

Optimized for both phones & tablets

🛠 Tech Stack

Language: Kotlin

IDE: Android Studio

UI: XML Layouts

Architecture: MVVM

Local Storage: Room / SharedPreferences

Permissions:

Contacts

Call Log (optional)

Phone (optional for direct calling)

📂 Project Structure
SanDial/
│
├── ui/
│   ├── dialer/
│   ├── recents/
│   ├── contacts/
│   ├── favorites/
│   └── settings/
│
├── viewmodel/
│
├── data/
│   ├── model/
│   ├── dao/
│   └── repository/
│
├── utils/
│
└── MainActivity.kt

🔐 Permissions Handling

App works even if permissions are denied

Friendly permission prompts

Graceful fallback UI for:

No contacts permission

No call log permission

🚀 Getting Started
Prerequisites

Android Studio (latest recommended)

Android device or emulator (API 23+)

Steps

Clone the repository:

git clone https://github.com/yourusername/san-dial.git


Open the project in Android Studio

Sync Gradle files

Run the app on a device or emulator

📌 Branding

Splash Screen:

App name: San Dial

Text: Developed by SanStudio

About Screen:

Clickable link to
👉 https://sanstudio.neocities.org/

Optional footer branding inside app

📷 Screens (Optional)

Add screenshots here later:

Dialer Screen

Recents

Contacts

Favorites

Dark Mode

🔮 Future Enhancements

Direct calling (ACTION_CALL)

Speed dial

Call blocking

Contact avatars sync

Gesture-based dial actions

Widget support

👨‍💻 Developed By

SanStudio
🌐 https://sanstudio.neocities.org/

📄 License

This project is open for learning and personal use.
For commercial use, please contact SanStudio.

=======================================================================


##SanStock 4


# 🏗️ SiteManager – Construction Stock Manager (Web → Android)

**SiteManager** is a modern **Construction Stock Management system** designed to track materials, stock movements (IN / OUT), and generate detailed reports.  
The project started as a **Web App (HTML, CSS, JavaScript)** and is being upgraded into a **full-featured Android App (2025-ready)**.

This tool is built for **construction site supervisors, storekeepers, and small contractors** who need a simple yet powerful way to manage daily stock without complex software.

---

## 🚀 Features Overview

### 📊 Dashboard
- Total materials count
- Current total stock
- Today’s IN / OUT quantities
- Monthly IN / OUT summary
- Recent transactions list
- Filters by material and transaction type

### 📦 Materials Master
- Add / Edit / Delete materials
- Material type, name, unit, rate
- Automatic stock calculation
- Centralized material management

### ⬆️ Stock IN (Material Received)
- Record incoming materials
- Supplier/source & notes
- Automatic stock increase
- Edit & delete with stock correction

### ⬇️ Stock OUT (Material Issued)
- Issue materials to workers/sites
- Shows available stock before issuing
- Prevents negative stock
- Edit & delete with stock correction

### 🧾 Transaction History
- Full searchable transaction list
- Filter by IN / OUT
- Edit & delete transactions
- Real-time stock recalculation

### 📈 Reports
- **Daily Report**
- **Monthly Summary**
- **Material-wise Report**
- Opening / Closing stock calculation
- Export as **PDF / Excel (Web)**

### 📧 Gmail Sync (Android Upgrade)
- First-time Gmail selection
- Sync stock reports to selected email
- Manual “Sync to Gmail” from reports
- Optional daily reminder via notification

---

## 🛠️ Tech Stack

### 🌐 Web Version
- **HTML5**
- **CSS3 (Tailwind CSS)**
- **Vanilla JavaScript**
- **LocalStorage** for data persistence
- **jsPDF & SheetJS** for report export

### 📱 Android Version (In Progress)
- **Kotlin**
- **Jetpack Compose**
- **Material 3 (Material You)**
- **MVVM Architecture**
- **Room Database**
- **Coroutines & Flow**
- **WorkManager** (optional auto-sync)
- **Gmail Intent-based Sync**

---

## 🗂️ Project Structure (Web)

