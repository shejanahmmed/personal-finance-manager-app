<p align="center">
  <img src="app/src/main/res/drawable/financebuddy.png" alt="FinanceBuddy Logo" width="120" />
</p>

<h1 align="center">FinanceBuddy</h1>

<p align="center">
  <strong>A premium, offline-first personal finance & investment manager tailored for Bangladesh.</strong>
</p>

---

**FinanceBuddy** is a state-of-the-art, modern, and offline-first personal finance and investment tracking application engineered specifically for Bangladeshi users. Built with modern Android development standards, the application enables users to seamlessly manage incomes, expenses, inter-account transfers, category budgets, savings targets, bank loans, personal lending/borrowing, interactive visual statistics, and an end-to-end investment portfolio tracker (FDR, Sanchayapatra, Stock Market, Gold, Real Estate, Crypto) — all guarded with hardware-backed AES-256 encryption, app-lock protection, and complete offline privacy.

---

## Technical Architecture

FinanceBuddy follows Clean Architecture principles, combining declarative Jetpack Compose UI patterns with a reactive, unidirectional data flow architecture.

```
+-------------------------------------------------------------------------+
|                            Jetpack Compose UI                           |
|  (HomeScreen, InvestmentsScreen, StatisticsScreen, Reports, Loans, etc.)|
+------------------------------------+------------------------------------+
                                     | Reactive State (Flow / State)
+------------------------------------v------------------------------------+
|                         ViewModel / DAO Layer                           |
|       (Room Asynchronous Stream Queries & Preferences DataStore)        |
+------------------------------------+------------------------------------+
                                     | Encrypted SQLCipher Bridge
+------------------------------------v------------------------------------+
|                    SQLCipher Database (AES-256)                         |
|   (16 Incremental Migrations, Hardware Keystore-backed Master Key)     |
+-------------------------------------------------------------------------+
```

### Architecture Pillars

* **Zero-Cloud Offline Privacy**: All financial records, investment portfolios, loan states, and auto-parsed SMS entries reside 100% locally on the user's device with zero external server dependencies.
* **Hardware-Backed Cryptographic Security**: SQLite database files are encrypted at rest via SQLCipher (AES-256-CBC). Master encryption keys are generated cryptographically and isolated inside the device's hardware Trusted Execution Environment (TEE).
* **Biometric & PIN Security Gate**: Cold starts and app resumes enforce PIN authentication with customizable auto-lock timeouts (Immediate, 1 min, 3 min, 5 min).
* **On-Device Intelligent SMS Parser**: Automatic extraction of transaction parameters (Amount, Sender, Account, Reference) from bank SMS shortcodes without sending data outside the device.
* **Non-Destructive Schema Evolution**: 16 version-controlled incremental database migrations guarantee data persistence without destructive database resets across updates.
* **Custom Compose Canvas Visualizations**: Native low-level Compose Canvas graphics engine for interactive grouped bar charts, Bezier curve trend graphs, donut pie distributions, and progress arcs.

---

## Security & Encryption Model

To ensure absolute confidentiality of personal financial data, FinanceBuddy implements a multi-layered local security paradigm:

```
                  +------------------------------------------+
                  |          Jetpack Room Database           |
                  +--------------------+---------------------+
                                       | SQLCipher SupportFactory
                  +--------------------v---------------------+
                  |         SQLCipher Engine (AES-256)       |
                  |     (Encrypts database file at rest)     |
                  +--------------------+---------------------+
                                       | 256-bit Passphrase
                  +--------------------v---------------------+
                  |        EncryptedSharedPreferences        |
                  |   (Stores passphrase encrypted with AES)  |
                  +--------------------+---------------------+
                                       | Hardware Master Key
                  +--------------------v---------------------+
                  |         Android Keystore System          |
                  |    (Key isolated inside hardware TEE)    |
                  +------------------------------------------+
```

1. **Database Encryption at Rest**: Encrypted using SQLCipher (AES-256-CBC). Database files cannot be read even on rooted devices.
2. **Passphrase Key Isolation**: Generated via `SecureRandom` on first launch and encrypted inside `EncryptedSharedPreferences` backed by the Android Keystore.
3. **Automated PIN Authentication**: Cold start and background resume gate guarding financial views.
4. **Secure SMS Processing**: Receiver restricted via `android:exported="false"` and protected by `BROADCAST_SMS` permission.

---

## Tech Stack & Core Libraries

* **UI Framework**: Jetpack Compose (Material 3), Navigation Compose, Custom Outfit Sans Typography.
* **Database & Persistence**: Room Database (SQLite), SQLCipher (`android-database-sqlcipher`), EncryptedSharedPreferences (`androidx.security.crypto`), Preferences DataStore.
* **Concurrency & Streams**: Kotlin Coroutines & Reactive `Flow`.
* **Graphics & Rendering**: Native Jetpack Compose Canvas APIs for bespoke charts and analytics.
* **Document Engine**: Android `PdfDocument` for client-side PDF statement generation.
* **Build System & Tooling**: Kotlin Symbol Processing (KSP), Gradle Kotlin DSL.

---

## Database Schema & Migration History

The database currently operates at **Schema Version 16** with atomic balance calculation and strict foreign key integrity.

### Schema Version Log

| Version | Migration Highlights |
| :--- | :--- |
| **v1 → v2** | Initial schema — `accounts` and `transactions` tables |
| **v2 → v3** | Added `budgets` table for spending limit tracking |
| **v3 → v4** | Added `goals` table for savings targets |
| **v4 → v5** | Added `pending_sms_transactions` table for SMS inbox |
| **v5 → v6** | Added account metadata columns (subtype, managed, holder, number) |
| **v6 → v7** | Added `payees` and `payee_accounts` recipient profile tables |
| **v7 → v8** | Added `showAs` alias column to accounts |
| **v8 → v9** | Added `nickname` column to payee accounts |
| **v9 → v10** | Added `sms_sender_mappings` custom shortcode table |
| **v10 → v11** | Added `loans` table for credit and lending tracking |
| **v11 → v12** | Added `repaidAmount` and `accountId` columns to loans |
| **v12 → v13** | Added `loanType` and `lenderName` columns to loans |
| **v13 → v14** | Added `isLent` flag to distinguish borrowed vs lent loans |
| **v14 → v15** | Added `status` state column to `pending_sms_transactions` |
| **v15 → v16** | Added `investments` table for full portfolio management |

---

## Key Feature Modules

### 1. 📈 Investment Tracker Portfolio
* **Multi-Asset Support**: Track Sanchayapatra (National Savings Certificates), Fixed Deposits (FDR/DPS), Stock Market (DSE), Mutual Funds, Gold & Precious Metals, Real Estate, Crypto, and Foreign Exchange.
* **Real-time Portfolio Metrics**: Net Portfolio Value, Total Invested Principal, Total Profit/Loss, and overall ROI percentage (`+X.X%`).
* **Visual Asset Allocation Bar**: Multi-segmented color bar showing category allocation percentages.
* **Quick Market Value Updates**: Update current values on-the-fly as assets appreciate or fluctuate.
* **Log Return / Dividend Payout**: Record dividends or interest payouts directly into any linked Bank/MFS account (auto-creates an `INCOME` record).
* **Filter Chips**: Filter by `All`, `Active Only`, `FDR`, `Sanchayapatra`, `Stocks`, `Gold`, `Real Estate`, `Crypto`, `Other`.

### 2. 📊 Interactive Visual Financial Statistics
* **6-Month Income vs Expense Bar Chart**: Side-by-side grouped animated bar comparison of monthly inflows vs outflows with Y-axis labels.
* **30-Day Balance Trend Bezier Curve**: Smooth line graph plotting running account balances over the past 30 days with glowing endpoint indicators.
* **Donut Category Expense Distribution**: Segmented pie donut chart with percentage legends.
* **KPI Metrics Dashboard**: Total Income, Total Expense, Net Savings, and Savings Rate %.
* **Dynamic Period Selectors**: This Week, This Month, This Year, All Time.

### 3. 🏦 Bangladeshi Financial Institutions Integration
* **Pre-seeded Banks**: BRAC Bank PLC, City Bank PLC, Eastern Bank PLC (EBL), Dutch-Bangla Bank PLC (DBBL), Prime Bank PLC, Mutual Trust Bank PLC, Islami Bank Bangladesh PLC (IBBL), Al-Arafah Islami Bank PLC, Shahjalal Islami Bank PLC.
* **Pre-seeded MFS Platforms**: bKash, Nagad, Rocket, Upay, CellFin (IBBL), Ok Wallet, MyCash.

### 4. 📲 Transaction Inbox & Automatic SMS Detection
* **On-Device Parsing**: Intercepts bank SMS alerts and extracts amount, type (Income/Expense), sender, and notes.
* **Custom Sender Shortcode Mapping**: Map unknown numeric or branded shortcodes directly to specific accounts.
* **Inbox Queue**: Confirm, edit, or dismiss auto-parsed transactions before updating account balances.

### 5. 💰 Budgeting & Category Limits
* **Spending Thresholds**: Set monthly limits per expense category.
* **Visual Usage Arc**: Dynamic Canvas arc meters changing color (Teal → Yellow → Red) as limits are approached.

### 6. 🎯 Savings Goals & Deposit Tracking
* **360-Degree Canvas Rings**: Animated circular progress indicators around custom goal icons.
* **Incremental Deposit Form**: Add manual savings amounts directly toward goal targets.

### 7. 🤝 Unified Loan & Lending Manager
* **Bank Loans**: Auto-credits account balance upon creation; EMI repayment logging.
* **Borrowed Money (I Owe)**: Log money borrowed from friends/family with partial/full repayment flows.
* **Lent Money (Owed to Me)**: Track personal funds lent out; record repayments received directly to accounts.

### 8. 📄 Financial Reports & Client-Side PDF Export
* **Custom Period Statements**: Generate summaries for custom date ranges.
* **One-Tap PDF Export**: Generate clean formatted PDF statements saved directly to device storage.

---

## Project Structure Overview

```
app/src/main/java/com/shejan/financebuddy/
├── data/
│   ├── db/
│   │   ├── AccountEntity.kt              # Account model
│   │   ├── TransactionEntity.kt          # Transaction model
│   │   ├── BudgetEntity.kt               # Budget limit model
│   │   ├── GoalEntity.kt                 # Savings Goal model
│   │   ├── LoanEntity.kt                 # Loan model (Bank, Borrowed, Lent)
│   │   ├── InvestmentEntity.kt           # Investment portfolio model
│   │   ├── PendingSmsTransactionEntity.kt# Temporary SMS model
│   │   ├── PayeeEntity.kt                # Recipient profile model
│   │   ├── PayeeAccountEntity.kt         # Recipient account model
│   │   ├── SmsSenderMappingEntity.kt     # SMS mapping model
│   │   ├── AccountDao.kt                 # Queries for accounts
│   │   ├── TransactionDao.kt             # Queries for transactions
│   │   ├── BudgetDao.kt                  # Queries for budgets
│   │   ├── GoalDao.kt                    # Queries for goals
│   │   ├── LoanDao.kt                    # Queries for loans
│   │   ├── InvestmentDao.kt              # Queries for investments
│   │   ├── PendingSmsDao.kt              # Queries for SMS inbox
│   │   ├── DatabaseMigrations.kt         # Versioned migrations (v1 to v16)
│   │   ├── DatabaseKeyManager.kt         # Keystore encryption manager
│   │   └── FinanceDatabase.kt            # Encrypted Room DB
│   └── PreferencesManager.kt             # Jetpack DataStore manager
├── sms/
│   ├── SmsParser.kt                      # On-device regex SMS engine
│   ├── SmsReceiver.kt                    # BroadcastReceiver for SMS
│   └── SmsSyncHelper.kt                  # SMS history scanner
├── ui/
│   ├── home/
│   │   ├── HomeScreen.kt                 # Main dashboard
│   │   └── components/Charts.kt          # Canvas bar and line charts
│   ├── investments/
│   │   └── InvestmentsScreen.kt          # Full investment portfolio manager
│   ├── statistics/
│   │   └── StatisticsScreen.kt           # Visual charts & financial analytics
│   ├── accounts/
│   │   └── BankAccountsScreen.kt         # Account CRUD & details
│   ├── budget/
│   │   └── BudgetScreen.kt               # Category budget dashboard
│   ├── goals/
│   │   └── GoalsScreen.kt                # Savings target progress
│   ├── loans/
│   │   └── LoansScreen.kt                # Bank loans & personal lending/borrowing
│   ├── history/
│   │   └── HistoryScreen.kt              # Searchable transaction log
│   ├── reports/
│   │   ├── ReportsScreen.kt              # Period summaries
│   │   └── PdfReportGenerator.kt         # Client-side PDF builder
│   ├── settings/
│   │   └── SettingsScreen.kt             # App configuration & security
│   └── theme/                            # Dark fintech tokens & Outfit font
└── MainActivity.kt                       # Single-activity navigation host
```

---

## Build & Installation

### Requirements
* **JDK**: 17+
* **Android SDK**: 35 (Min SDK: 24 / Android 7.0 Nougat)
* **IDE**: Android Studio Ladybug or later

### Building from Source

```bash
# Clone the repository
git clone https://github.com/shejanahmmed/FinanceBuddy.git
cd FinanceBuddy

# Compile debug APK
./gradlew assembleDebug

# Run Kotlin compilation verification
./gradlew compileDebugKotlin
```

---

<p align="center">
  Crafted with ❤️ for Bangladesh 🇧🇩
</p>
