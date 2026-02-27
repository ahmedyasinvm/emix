# 🏦 Emix - Premium EMI Collection Manager

![Version](https://img.shields.io/badge/Version-3.2-gold) ![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-green) ![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Hilt-blue)

**Emix** is a professional-grade EMI loan collection tool built for field agents and small lenders. It features a **Finance Premium Dark Theme** (Emerald & Gold), real-time analytics, cloud backup via Google Drive, and smart receipt generation — all offline-first.

## 📥 Download

Get the latest APK from the **[Releases Page](https://github.com/ahmedyasinvm/emix/releases)**.

---

## ✨ What's New in v3.2

- **Restore-First Safety Protocol** — After sign-in, if a cloud backup exists and the local database is empty, the app prompts to restore before doing any backup (prevents data loss)
- **Edit & Delete Customers/Loans** — Full CRUD from the Customer Detail screen with confirmation dialogs
- **Loan Progress Bar** — Each loan card now shows a repayment progress bar and a **"Settled"** badge when fully paid
- **Over-Budget Payment Warning** — SmartPaymentDialog warns and relabels the button when the entered amount exceeds the remaining balance
- **Today's Due Filter** — Dashboard quick action to instantly filter only overdue/due-today customers
- **Analytics Date Range** — Switch the collections chart between **Last 7 Days**, **30 Days**, and **90 Days**
- **Professional Codebase** — Removed informal Toast/emoji feedback; all UI events go through `UiEvent` SharedFlow

---

## ✨ Core Features

### 📊 Dashboard

- Real-time stats: Today's Collection, Pending Dues, Active Loans
- **Today's Due** filter — one tap to see only customers due today
- **Today's Progress** quick action with collected amount and due count
- Sort by: Urgent First, Highest Debt, Name A-Z
- Glassmorphic dark UI with Emerald & Gold palette

### 👤 Customer Management

- Add, **Edit**, and **Delete** customers with confirmation
- Master Ledger per customer — full chronological payment history
- FIFO total payment across all active loans

### 💳 Loan Management

- Add multiple loans per customer (item, price, down payment, schedule)
- Per-loan repayment **progress bar**
- Automatic **"Settled"** badge on fully paid loans
- **Delete Loan** with single confirmation

### 💰 Smart Payment Processing

- Cash / GPay toggle
- Pre-filled default amount stepper
- **Over-budget warning** when amount > remaining balance
- Edit past transactions inline

### 🧾 Receipt & Statement Generation

- Instant branded receipt (JPEG) — shareable via WhatsApp/Telegram
- Combined customer statement with full ledger table

### ☁️ Cloud Backup & Restore (Google Drive)

- One-tap backup to `Emix_Backups/` folder in Drive
- **Restore-First protocol** — protects against accidental overwrite
- Daily auto-backup via WorkManager
- Local JSON backup/restore + Excel export

### 📈 Analytics

- Collections bar chart with **Week / 30 Days / 90 Days** range selector
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

|                                                Dashboard                                                |                                                Smart Pay                                                |                                                Analytics                                                |
| :-----------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------: |
| <img src="https://github.com/user-attachments/assets/c35371fb-4cab-4977-95bb-58a11620366b" width="260"> | <img src="https://github.com/user-attachments/assets/faf5efcd-6a18-4256-982b-ffa66f8865f8" width="260"> | <img src="https://github.com/user-attachments/assets/10582d2f-8edb-48ff-94b9-0886d39e18a4" width="260"> |

---

## 👤 Author

Developed by **Ahmed Yasin**

- **GitHub:** [github.com/ahmedyasinvm](https://github.com/ahmedyasinvm)
- **Site:** [ahmedyasinvm.site](https://ahmedyasinvm.site)
