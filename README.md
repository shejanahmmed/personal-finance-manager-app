<p align="center">
  <img src="app/src/main/res/drawable/financebuddy.png" alt="FinanceBuddy Logo" width="120" />
</p>

<h1 align="center">FinanceBuddy</h1>

<p align="center">
  <strong>A premium, offline-first personal finance manager tailored for Bangladesh.</strong>
</p>

---

**FinanceBuddy** is a modern, premium, and offline-first personal finance manager designed specifically for Bangladeshi users. Built with modern Android development patterns, the application allows users to track incomes, daily expenses, inter-account transfers, category budgets, savings goals, bank loans, and personal lending across local banks and mobile financial services (MFS) — with complete security, hardware-backed encryption, PIN lock, and total offline privacy.

---

## Technical Architecture

FinanceBuddy is engineered using Clean Architecture principles, leveraging declarative UI patterns and a highly reactive unidirectional data flow.

### Architecture Pillars

*   **Zero-Cloud Offline Privacy**: All transaction records, budget configurations, savings targets, and loan data are persisted locally on the device with no external server sync, ensuring complete financial privacy.
*   **Hardware-Backed Security**: The local SQLite database is fully encrypted at rest. Passphrases are generated cryptographically and secured inside the device hardware Keystore.
*   **PIN Lock**: App-level PIN authentication guards access on every cold start. Setup and reset flows are built-in.
*   **Automated SMS Parsing**: Fully on-device parsing of transaction SMS notifications from Bangladeshi banks and MFS providers, using regex engines with whitelist sender filters.
*   **Persistent Schema Migrations**: Avoids destructive database drops by utilizing version-controlled migration scripts for schema updates (v1 to v14).
*   **Single-Activity Scaffolding**: Uses Jetpack Compose Navigation (NavHost) to manage state transitions across all screens.
*   **Reactive Flow Channels**: Employs Room Database queries that expose asynchronous stream values (Flow), which are collected as state within UI composables to instantly reflect changes.
*   **Custom Graphics Rendering**: Utilizes lower-level Canvas APIs for custom rendering of data charts, arc overviews, doughnut rings, and progress arcs.

---

## Core Security and Encryption

To protect sensitive financial information, FinanceBuddy implements a multi-layered local security model:

```
                  +------------------------------------------+
                  |          Jetpack Room Database           |
                  +--------------------+---------------------+
                                       | SQLCipher SupportFactory
                  +--------------------v---------------------+
                  |         SQLCipher Engine (AES-256)       |
                  |     (Encrypts database file at rest)     |
                  +--------------------+---------------------+
                                       | 256-bit passphrase
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

1.  **Database-Level Encryption**: The entire database is encrypted using SQLCipher (AES-256-CBC). Unencrypted database files cannot be read even on rooted devices.
2.  **Key Management**: A cryptographically secure 256-bit random passphrase is generated on the first run using SecureRandom.
3.  **Hardware Storage**: The passphrase is stored in EncryptedSharedPreferences, encrypted with a Master Key generated inside the Android Keystore (utilizing a hardware-backed Trusted Execution Environment / TEE if available).
4.  **PIN Lock Screen**: On every cold start, the user must authenticate with a self-defined numeric PIN before accessing any financial data. PIN hashes are stored securely in EncryptedSharedPreferences.
5.  **Secure SMS Pipeline**:
    *   Local Processing Only: SMS messages are parsed in-memory without any network activity.
    *   Sender Whitelisting: Generic numbers are automatically rejected. Only whitelisted shortcodes (e.g., bKash, Nagad, BRACBANK) are processed.
    *   Security Restrictions: The SmsReceiver is configured with android:exported="false" and guarded with the BROADCAST_SMS permission, ensuring only the OS can trigger it.

---

## Core Technologies

*   **UI Framework**: Jetpack Compose (Declarative UI) with custom Material 3 design tokens.
*   **Security and Crypto**: SQLCipher for Android (android-database-sqlcipher), Android Jetpack Security (security-crypto).
*   **Local Persistence Layer**:
    *   Room Database: Relational SQLite engine wrapper for transactions, accounts, budgets, savings goals, loans, and payees.
    *   Preferences DataStore: Jetpack DataStore key-value store for lightweight configuration states (onboarding, SMS setup, PIN, notifications).
*   **Asynchronous Concurrency**: Kotlin Coroutines and Reactive Flow for non-blocking I/O.
*   **Code Generation**: Kotlin Symbol Processing (KSP) for compile-time database mapping and query validation.
*   **PDF Generation**: Android PdfDocument API for generating formatted financial reports exportable to device storage.
*   **Typography Assets**: Bundled custom Outfit Sans typeface weights.

---

## Database Architecture

The local SQLite schema operates with relational constraints to guarantee transaction and balance consistency. The schema has evolved through **14 versioned migrations**.

### Schema Version History

| Version | Change |
|---------|--------|
| v1 → v2 | Initial schema — accounts + transactions |
| v2 → v3 | Added budgets table |
| v3 → v4 | Added goals table |
| v4 → v5 | Added pending_sms_transactions table |
| v5 → v6 | Added account profile fields (subtype, managed, holder, number) |
| v6 → v7 | Added payees and payee_accounts tables |
| v7 → v8 | Added showAs alias column to accounts |
| v8 → v9 | Added nickname column to payee_accounts |
| v9 → v10 | Added sms_sender_mappings table |
| v10 → v11 | Added loans table |
| v11 → v12 | Added repaidAmount and accountId to loans |
| v12 → v13 | Added loanType and lenderName to loans |
| v13 → v14 | Added isLent flag to loans (lent-to tracking) |

### Self-Synchronizing Balances
The database design delegates transaction-balance math to database-level transactions using Room @Transaction:
- **Income Insertion**: Increases target account balance.
- **Expense Insertion**: Decreases target account balance.
- **Transfer Insertion**: Atomically transfers value between source and destination accounts.
- **Bank Loan Added**: Credits the linked account balance and logs an INCOME/Loan transaction.
- **Personal Loan Lent**: Debits the linked account balance and logs an EXPENSE transaction.
- **Repayment (Borrowed)**: Debits the linked account and logs an EXPENSE transaction.
- **Repayment Received (Lent)**: Credits the linked account and logs an INCOME transaction.
- **Deletion Reversal**: Automatically restores previous balances on transaction removal.

---

## Project Structure

```
app/src/main/java/com/shejan/financebuddy/
+-- data/
|   +-- db/
|   |   +-- AccountEntity.kt              # Account database model
|   |   +-- TransactionEntity.kt          # Transaction database model
|   |   +-- BudgetEntity.kt               # Budget limit database model
|   |   +-- GoalEntity.kt                 # Savings Goal database model
|   |   +-- LoanEntity.kt                 # Loan model (bank + borrowed + lent)
|   |   +-- PendingSmsTransactionEntity.kt# Temporary SMS transaction model
|   |   +-- PayeeEntity.kt                # Recipient Profile model
|   |   +-- PayeeAccountEntity.kt         # Recipient Bank Account model
|   |   +-- SmsSenderMappingEntity.kt     # Custom SMS sender mapping model
|   |   +-- AccountDao.kt                 # Queries for wallets/institutions
|   |   +-- TransactionDao.kt             # Atomic balance-adjusting transaction queries
|   |   +-- BudgetDao.kt                  # Category-based budget constraint queries
|   |   +-- GoalDao.kt                    # Savings goal deposit and CRUD queries
|   |   +-- LoanDao.kt                    # Loan query constraints
|   |   +-- PendingSmsDao.kt              # CRUD for Transaction Inbox
|   |   +-- PayeeDao.kt                   # CRUD for Recipient Profiles
|   |   +-- SmsSenderMappingDao.kt        # CRUD for custom sender mappings
|   |   +-- DatabaseMigrations.kt         # Version-controlled migrations (v1 to v14)
|   |   +-- DatabaseKeyManager.kt         # Keystore-backed encryption key manager
|   |   +-- FinanceDatabase.kt            # Encrypted Room database configuration
|   +-- PreferencesManager.kt             # DataStore (Onboarding, SMS, PIN, Notifications)
+-- sms/
|   +-- SmsParser.kt                      # On-device regex SMS parsing engine
|   +-- ParsedSmsData.kt                  # Parsed SMS transaction parameter model
|   +-- SmsReceiver.kt                    # Secured BroadcastReceiver for incoming SMS
|   +-- SmsSyncHelper.kt                  # ContentProvider scanner for SMS history sync
|   +-- SmsPermissionHandler.kt           # Setup dialog and permission requester
+-- ui/
|   +-- home/
|   |   +-- components/Charts.kt          # Custom Canvas Bar and Line charts
|   |   +-- HomeScreen.kt                 # Dashboard with balance, charts, recent transactions
|   |   +-- AddTransactionSheet.kt        # Sliding modal transaction form sheet
|   +-- accounts/
|   |   +-- BankAccountsScreen.kt         # Full account management CRUD and detail views
|   +-- budget/
|   |   +-- BudgetScreen.kt               # Budgeting interface, Canvas arc, and CRUD sheets
|   +-- goals/
|   |   +-- GoalsScreen.kt                # Savings goal progress rings and deposit forms
|   +-- history/
|   |   +-- HistoryScreen.kt              # Full transaction history with filters and search
|   +-- loans/
|   |   +-- LoansScreen.kt                # Bank loans, borrowed loans, and lent loans
|   +-- reports/
|   |   +-- ReportsScreen.kt              # Financial reports with charts and PDF export
|   |   +-- PdfReportGenerator.kt         # PDF document builder
|   +-- notifications/
|   |   +-- AppNotification.kt            # Notification data model
|   |   +-- NotificationHelper.kt         # Notification read/write and scheduling helpers
|   |   +-- NotificationsBottomSheet.kt   # In-app notification centre bottom sheet
|   +-- security/
|   |   +-- LockScreen.kt                 # PIN entry screen shown on cold start
|   |   +-- PinSetupDialog.kt             # PIN creation and change dialog
|   +-- settings/
|   |   +-- SettingsScreen.kt             # App settings (PIN, SMS, currency, profile, data)
|   +-- payees/                           # Payee profile management composables
|   +-- profile/
|   |   +-- ProfileDialog.kt              # User profile dialog (name, display)
|   +-- pending/
|   |   +-- PendingTransactionsScreen.kt  # Transaction Inbox and SMS sender mapping
|   +-- onboarding/
|   |   +-- OnboardingPage.kt             # Pager metadata model
|   |   +-- OnboardingScreen.kt           # Interactive onboarding walk-through
|   +-- theme/
|       +-- Color.kt                      # Dark fintech color palettes
|       +-- Type.kt                       # Custom Outfit font definitions
|       +-- Theme.kt                      # Edge-to-edge system window theme hooks
+-- MainActivity.kt                       # Root navigation host and App entry point
```

---

## Features

### 1. Seeding of Bangladeshi Institutions
Upon initialization, the database seeds default local financial institutions:
- **Banks**: BRAC Bank PLC, The City Bank PLC, Eastern Bank PLC (EBL), Dutch-Bangla Bank PLC (DBBL), Prime Bank PLC, Mutual Trust Bank PLC, Islami Bank Bangladesh PLC (IBBL), Al-Arafah Islami Bank PLC, Shahjalal Islami Bank PLC.
- **MFS**: bKash, Nagad, Rocket, Upay, CellFin (IBBL), Ok Wallet, MyCash.

### 2. Transaction Inbox and SMS Auto-Detection
FinanceBuddy automates expense tracking through local SMS interceptors:
- **On-Device Regex Parser**: Extracts transaction type, amounts, account keywords, and reference notes from bank SMS. Supports routing mapped custom or unknown senders dynamically to specific bank/MFS sub-parsers.
- **Flexible Configurable Sync Timeframes**: At setup, or during manual scans, users can choose to scan inbox history for the past 1 month, 3 months, 6 months, 1 year, or all messages.
- **Custom SMS Sender Mappings**: Users can map numeric or unknown SMS senders to specific local accounts via the SMS Sender Configurations sheet. When an unknown transaction is received, the app prompts linking, saves it, and runs a retroactive history scan.
- **Inbox Review Queue**: SMS transactions are placed in the Transaction Inbox where users review, edit, confirm, or dismiss them before balances adjust.
- **Manual Historical Scan**: Scan triggers available in the empty state and top header to re-scan messages at any time.

### 3. High-Performance Custom Charts
Bespoke charts designed with native Compose Canvas drawing APIs:
- **Weekly Expenses Bar Chart**: Automatically sums and visualizes daily expense totals for the last 7 calendar days.
- **Balance Trend Bezier Chart**: Computes running balances dynamically by subtracting daily net-change from the total balance going backward. Plots a smooth curved line.

### 4. Dynamic Budgeting Dashboard
- **Monthly Limit Constraints**: Set monthly spending limits per category.
- **Visual Warning Metrics**: Visualizes total budget usage with a custom Canvas arc meter that dynamically updates color states as categories approach thresholds.
- **Spent-vs-Limit Trackers**: Highlights remaining balance versus spent totals per category.

### 5. Interactive Savings Goals
- **Animated Circular Progress**: Renders a custom 360-degree Canvas progress ring around goal indicators.
- **Secure Deposits**: Add manual savings increments directly to goals.
- **Goal Personalization**: Complete custom emoji grid picker and dynamic color-coding.
- **Deadline Metrics**: Automatically computes and highlights remaining days/months before targets.

### 6. Account Management
- **Full CRUD**: Create, view, edit, and delete bank/MFS accounts.
- **Account Subtypes**: Mark accounts as Savings, Current, Student, etc.
- **Managed Accounts**: Flag accounts managed on behalf of others with a holder name.
- **Show As Alias**: Set a custom display nickname separate from the institution name.
- **Masked Account Numbers**: Numbers are masked (e.g. 1234) in card views for visual privacy.
- **Compact Cards**: 100dp height cards with border strokes and proportional typography.

### 7. Comprehensive Loan Management
Three distinct loan categories tracked separately with a unified summary overview:

#### Bank Loans
- Linking a bank loan to an account auto-credits its balance with an INCOME/Loan transaction.
- Repayment modals auto-fill the monthly EMI (or remaining balance) and deduct from the linked account.
- Active loan breakdown (Repaid vs Principal vs Interest) rendered as a Canvas doughnut chart.

#### Borrowed from Friend / Family
- Log money borrowed from a person with their name, amount, optional interest, and duration.
- Record partial or full repayments; each repayment debits the linked account and logs an EXPENSE.
- "I Owe" total shown prominently in the loan summary card.

#### Lent to Friend / Family
- Log money lent to others — track who owes you, how much, and for how long.
- Record repayments received; each receipt credits the linked account and logs an INCOME.
- "Owed to Me" total shown with a distinct teal accent in the loan summary card.
- Summary overview clearly separates liabilities (what you owe) from assets (what others owe you).

### 8. Transaction History
- **Full Searchable Log**: Filterable list of all confirmed transactions across all accounts.
- **Filter by Type**: Filter by Income, Expense, or Transfer.
- **Filter by Account**: Narrow history to a single account.
- **Date Range Filters**: View transactions within custom date ranges.
- **Inline Delete**: Delete transactions with automatic balance reversal.

### 9. Financial Reports and PDF Export
- **Period Reports**: Generate income/expense summaries for custom date ranges.
- **Category Breakdown**: Visual charts showing spending distribution across categories.
- **Net Worth Snapshot**: Total assets vs total liabilities view.
- **PDF Export**: One-tap export to a formatted PDF document saved to device storage.

### 10. In-App Notification Centre
- **Smart Alerts**: Budget overruns, goal deadlines, and loan reminders surfaced as in-app notifications.
- **Notification Bell**: Bell icon in the home screen top bar with an unread badge counter.
- **Notification History**: Full list of past alerts accessible via the bottom sheet notification centre.
- **Mark as Read / Clear**: Mark individual or all notifications as read.

### 11. PIN Lock and Security
- **Cold Start PIN Gate**: Every fresh app open requires PIN entry before any data is accessible.
- **PIN Setup**: Users set a PIN during or after onboarding.
- **PIN Change**: Change PIN at any time from Settings.

### 12. Settings
- **SMS Configuration**: Toggle SMS auto-detection, configure scan timeframes, and manage sender mappings.
- **PIN Management**: Set or change the app lock PIN.
- **Currency**: Default BDT (taka symbol) with display formatting.
- **Profile**: Set a user name displayed in the home screen greeting.

### 13. Streamlined Transfer Sheet and Autocomplete
- **Vertical Input Flow**: Stacked From and To account selectors for clean, professional styling.
- **Payee-Free Other Transfers**: Direct inputs for Recipient Name and Account/Mobile Number.
- **Recipient Profiles (Payees)**: Full profiles with multiple bank/MFS accounts, custom nicknames, and account-specific details.
- **Intelligent Autocomplete**: Autocomplete popup for recipient name; selecting a suggestion auto-populates all details.

### 14. UI/UX Polish
- **Ambient Gradient Backgrounds**: Consistent dark fintech aesthetic with a subtle ambient glow gradient tied to each screen accent color.
- **Drawer Navigation**: Slide-out hamburger drawer with close-on-outside-tap and close-on-X-tap behavior.
- **Header Uniformity**: Matching gradient header backgrounds across all screens.
- **Redesigned Inbox Actions**: Action buttons (Dismiss, Edit, Confirm) as 46dp square boxes with clear icons and labels.
- **Tightened Bottom Navigation**: Optimized icon-to-label spacing for a cohesive professional layout.

---

## Build and Setup

### Prerequisites
- JDK 17+
- Android SDK 35+ (min API 24 / Android 7.0)
- Android Studio Ladybug or later

### Compilation Steps

1. Clone the repository to your local path.
2. Initialize build via Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run compilation check:
   ```bash
   ./gradlew compileDebugKotlin
   ```

> **Current database schema version: v14** (14 incremental migrations, no destructive resets).
