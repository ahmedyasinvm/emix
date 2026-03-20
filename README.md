# 🏦 Emix - Premium EMI Collection Manager

![Version](https://img.shields.io/badge/Version-4.6.5-gold) ![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-green) ![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Hilt-blue)

**Emix** is a professional-grade EMI loan collection tool built for field agents and small lenders. It features a complete **Finance Premium Dark Theme** (Emerald & Gold), real-time analytics, cloud backup via Google Drive, and smart receipt generation — all offline-first.

## 📥 Download

Get the latest APK from the **[Releases Page](https://github.com/ahmedyasinvm/emix/releases)**.

---

## ✨ What's New in v4.6.5 (The Premium Overhaul)

- **Finance Premium Dark Theme** — 60+ new design tokens, glassmorphism cards with gradient borders, and animated screen transitions.
- **Custom Date Range Analytics** — Dynamically filter charts, collection stats, and period summaries using the new `Custom` date picker.
- **Enhanced Statement Sharing** — Modern bottom sheet UI to share Full (All Loans) or Itemized (Specific Loan) statements with custom date bounds.
- **Premium Receipt Overhaul** — Generated statements now feature stunning gradient headers, faint watermarks, and beautiful tabular rows to match the app's aesthetic.
- **Improved Logical Validation** — Smart UI bounds prevent irrational transactions, over-budget down payments, and negative accounting loops.
- **Settings Enhancements** — Grouped glass card sections, Privacy Policy integration, and clearer Cloud Backup states.

*(Includes all robust features from v3.2 like the Restore-First Safety Protocol and CRUD confirmations).*

---

## ✨ Core Features

### 📊 Dashboard
- Real-time stats: Today's Collection, Pending Dues, Active Loans
- **Today's Due** filter — one tap to see only customers due today
- **Today's Progress** quick action with collected amount and due count
- Sort by: Urgent First, Highest Debt, Name A-Z
- Glassmorphic dark UI with animated scale transitions

### 👤 Customer Management
- Add, **Edit**, and **Delete** customers with confirmation
- Master Ledger per customer — full chronological payment history
- FIFO total payment across all active loans
- Clean Circular avatars with deterministic gradients and urgency indicators

### 💳 Loan Management
- Add multiple loans per customer (item, price, down payment, schedule)
- Per-loan repayment **progress bar**
- Automatic **"Settled"** badge on fully paid loans
- Safe input validation against excess down-payments

### 💰 Smart Payment Processing
- Cash / GPay toggle
- Pre-filled default amount stepper
- **Over-budget warning** when amount > remaining balance

### 🧾 Receipt & Statement Generation
- Premium branded receipt (PNG) — shareable via WhatsApp/Telegram
- Combined or itemized customer statements with beautiful header gradients, watermarks, and alternate row colors.
- Custom Date selection directly from the Share bottom sheet.

### ☁️ Cloud Backup & Restore (Google Drive)
- One-tap backup to `Emix_Backups/` folder in Drive
- **Restore-First protocol** — protects against accidental overwrite
- Daily auto-backup via WorkManager
- Local JSON backup/restore + Excel export

### 📈 Analytics
- Collections bar chart with **Last 7 / 30 / 90 Days** and **Custom Range** selector
- Cash vs GPay payment split pie chart
- Overdue customers list with last payment date

---

## 🛠️ Technical Stack

| Layer        | Technology                   |
| ------------ | ---------------------------- |
| Language     | Kotlin 2.0                   |
| UI           | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture    |
| DI           | Dagger Hilt                  |
| Database     | Room (SQLite)                |
| Async        | Coroutines + Flow            |
| Cloud        | Google Drive API v3          |
| Charts       | MPAndroidChart               |
| Export       | Apache POI (Excel)           |
| Background   | WorkManager                  |

---

## 📱 Screenshots

> *Screenshots are currently reflecting earlier versions. Premium Dark Mode UI screenshots coming soon!*

|                                                Dashboard                                                |                                                Smart Pay                                                |                                                Analytics                                                |
| :-----------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------: |
| <img src="https://github.com/user-attachments/assets/c35371fb-4cab-4977-95bb-58a11620366b" width="260"> | <img src="https://github.com/user-attachments/assets/faf5efcd-6a18-4256-982b-ffa66f8865f8" width="260"> | <img src="https://github.com/user-attachments/assets/10582d2f-8edb-48ff-94b9-0886d39e18a4" width="260"> |

---

## 👤 Author

Developed by **Ahmed Yasin**

- **GitHub:** [github.com/ahmedyasinvm](https://github.com/ahmedyasinvm)
- **Site:** [ahmedyasinvm.site](https://ahmedyasinvm.site)
