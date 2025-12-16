# Android_Application


##SanStock


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

