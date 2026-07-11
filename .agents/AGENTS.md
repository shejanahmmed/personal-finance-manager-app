# FinanceBuddy Rules

## Gemini Added Memories

### Project Setup
- Project "FinanceBuddy" (com.shejan.financebuddy) is an Android Studio project using Jetpack Compose and Kotlin DSL.
- FinanceBuddy targets API 24 (Android 7.0 Nougat) as the minimum SDK.
- The FinanceBuddy project path contains whitespace, which may cause issues with NDK tools.
- FinanceBuddy (com.shejan.financebuddy) is an Android project initialized with the 'Empty Activity' template using Jetpack Compose and Kotlin DSL, targeting API 24 with ~99.2% device compatibility.

### App Concept & Purpose
- FinanceBuddy is a **personal finance manager app specifically designed for Bangladesh**.
- The app targets Bangladeshi users, supporting all major local banks and mobile banking services.

### Planned Features (NOT yet implemented)
- **Bangladesh Bank & Mobile Banking Support**: Include all major Bangladeshi banks (e.g., Dutch-Bangla Bank, BRAC Bank, Islami Bank, Sonali Bank, City Bank, Eastern Bank, Mutual Trust Bank, etc.) and mobile banking platforms (e.g., bKash, Nagad, Rocket, Upay, SureCash, MyCash, etc.).
- **Income / Balance Input**: Users can manually input their income or current balance for each account/wallet.
- **Expense Tracking**: Users can log their daily expenses.
- **Money Transfer Tracking**: Users can log transfers between their accounts (e.g., bank to bKash, bank to bank).
- **Transaction History**: Users can view a full history of income, expenses, and transfers.
- **Financial Overview / Dashboard**: A summary screen showing total balance, income, and expenses.

### Supported Financial Institutions (Exact List)

#### Everyday Digital Banks
- BRAC Bank PLC
- The City Bank PLC
- Eastern Bank PLC (EBL)
- Dutch-Bangla Bank PLC (DBBL)
- Prime Bank PLC
- Mutual Trust Bank PLC
- Islami Bank Bangladesh PLC (IBBL)
- Al-Arafah Islami Bank PLC
- Shahjalal Islami Bank PLC

#### Mobile Financial Services (MFS)
- bKash
- Nagad
- Rocket
- Upay
- CellFin (IBBL)
- Ok Wallet
- MyCash

### Design Notes
- The app should feel local and familiar to Bangladeshi users.
- Support for BDT (Bangladeshi Taka ৳) as the default currency.

---

## App Design & UI/UX Requirements (NOT yet implemented)

### Design Philosophy
- **Professional & Modern** but **Minimal** — clean, uncluttered UI with high-quality visual polish.
- Premium feel: use curated color palettes, smooth animations, modern typography.
- All data stored **locally on the device** (no cloud/server) for better security and privacy.

### Onboarding
- **Starter / Splash Page**: Shown only the first time the user opens the app.
  - Welcome screen with app branding, tagline, and a "Get Started" CTA.
  - Could include a brief onboarding walkthrough (2–3 slides) explaining core features.

### App Navigation Structure
- **Bottom Navigation Bar** with primary pages:
  1. 🏠 **Home** — Main dashboard
  2. 📊 **Budget** — Budget planning and limits per category
  3. 🎯 **Goals** — Savings goals tracking
- **Hamburger (☰) Menu** — Top-left on the Home page, slides out a drawer with:
  - ⚙️ Settings
  - 💱 Currency (default BDT ৳)
  - 📈 Statistics
  - 💼 Investment Tracker
  - (and other options as needed)

### Home Page Layout

#### Top Bar
- **Top-Left**: ☰ Hamburger menu icon (opens side drawer)
- **Top-Right**: 🔔 Notification icon (shows user alerts & reminders)

#### FAB / Quick Action Button
- **Floating `+` Button**: Opens a modal/sheet to quickly add:
  - ➕ Income
  - ➖ Expense
  - 🔄 Transfer (between accounts)

#### Home Page Content Sections (top to bottom)
1. **Balance Overview Card** — Total balance across all accounts/wallets
2. **Income vs Expense Summary** — Quick snapshot (this month)
3. **Expense Graph** — Visual chart (bar/line) of spending over time
4. **Last Recorded Overview** — Most recent transactions listed
5. **Balance Trend** — A trend line showing balance change over weeks/months
6. **Upcoming Planned Payments** — Scheduled/recurring payments due soon

### Data Storage
- All financial data (transactions, accounts, goals, budgets) saved locally on-device.
- Use **Room Database** (SQLite via Jetpack Room) for structured local storage.
- No internet connection required for core functionality.
