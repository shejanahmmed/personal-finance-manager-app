package com.shejan.financebuddy.data

import android.content.Context
import com.shejan.financebuddy.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/**
 * Handles full app data export (backup) and import (restore) using JSON serialization.
 *
 * Why JSON instead of raw DB copy?
 * The SQLCipher database encryption key is hardware-bound (Android Keystore).
 * Copying the raw .db file to another device will fail because that device's
 * Keystore cannot decrypt it. JSON export decouples the data from the encryption
 * layer, allowing seamless cross-device restoration.
 */
object BackupManager {

    private const val BACKUP_VERSION = 1
    private const val KEY_VERSION = "backup_version"
    private const val KEY_ACCOUNTS = "accounts"
    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_BUDGETS = "budgets"
    private const val KEY_GOALS = "goals"
    private const val KEY_LOANS = "loans"
    private const val KEY_PENDING_SMS = "pending_sms_transactions"
    private const val KEY_PAYEES = "payees"
    private const val KEY_PAYEE_ACCOUNTS = "payee_accounts"
    private const val KEY_SMS_MAPPINGS = "sms_sender_mappings"
    private const val KEY_PREFERENCES = "preferences"

    // ─── EXPORT ─────────────────────────────────────────────────

    /**
     * Queries all Room tables and DataStore preferences, serializes them to JSON,
     * and writes the result to [outputStream].
     */
    suspend fun exportData(
        context: Context,
        outputStream: OutputStream,
        database: FinanceDatabase,
        preferencesManager: PreferencesManager
    ) = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put(KEY_VERSION, BACKUP_VERSION)
        root.put("exported_at", System.currentTimeMillis())

        // Accounts
        val accounts = database.accountDao().getAllAccounts().first()
        root.put(KEY_ACCOUNTS, JSONArray().apply {
            accounts.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("type", a.type)
                    put("balance", a.balance)
                    put("colorHex", a.colorHex)
                    put("accountSubtype", a.accountSubtype)
                    put("isManaged", a.isManaged)
                    put("holderName", a.holderName)
                    put("accountNumber", a.accountNumber)
                    put("showAs", a.showAs)
                })
            }
        })

        // Transactions
        val transactions = database.transactionDao().getAllTransactions().first()
        root.put(KEY_TRANSACTIONS, JSONArray().apply {
            transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("amount", t.amount)
                    put("type", t.type)
                    put("category", t.category)
                    put("timestamp", t.timestamp)
                    put("fromAccountId", t.fromAccountId)
                    put("toAccountId", t.toAccountId ?: JSONObject.NULL)
                    put("note", t.note)
                })
            }
        })

        // Budgets
        val budgets = database.budgetDao().getAllBudgets().first()
        root.put(KEY_BUDGETS, JSONArray().apply {
            budgets.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("category", b.category)
                    put("limitAmount", b.limitAmount)
                    put("colorHex", b.colorHex)
                })
            }
        })

        // Goals
        val goals = database.goalDao().getAllGoals().first()
        root.put(KEY_GOALS, JSONArray().apply {
            goals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("title", g.title)
                    put("targetAmount", g.targetAmount)
                    put("savedAmount", g.savedAmount)
                    put("colorHex", g.colorHex)
                    put("emoji", g.emoji)
                    put("deadline", g.deadline ?: JSONObject.NULL)
                    put("createdAt", g.createdAt)
                })
            }
        })

        // Loans
        val loans = database.loanDao().getAllLoans().first()
        root.put(KEY_LOANS, JSONArray().apply {
            loans.forEach { l ->
                put(JSONObject().apply {
                    put("id", l.id)
                    put("bankName", l.bankName)
                    put("loanAmount", l.loanAmount)
                    put("durationMonths", l.durationMonths)
                    put("interestRate", l.interestRate)
                    put("repaidAmount", l.repaidAmount)
                    put("accountId", l.accountId)
                    put("loanType", l.loanType)
                    put("lenderName", l.lenderName)
                    put("isLent", l.isLent)
                    put("createdAt", l.createdAt)
                })
            }
        })

        // Pending SMS Transactions
        val pendingSms = database.pendingSmsDao().getAllPending().first()
        root.put(KEY_PENDING_SMS, JSONArray().apply {
            pendingSms.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("rawSmsBody", p.rawSmsBody)
                    put("senderAddress", p.senderAddress)
                    put("amount", p.amount)
                    put("type", p.type)
                    put("category", p.category)
                    put("note", p.note)
                    put("detectedAccountName", p.detectedAccountName)
                    put("fromAccountId", p.fromAccountId)
                    put("toAccountId", p.toAccountId ?: JSONObject.NULL)
                    put("timestamp", p.timestamp)
                    put("receivedAt", p.receivedAt)
                })
            }
        })

        // Payees
        val payees = database.payeeDao().getAllPayees().first()
        root.put(KEY_PAYEES, JSONArray().apply {
            payees.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("uniqueId", p.uniqueId)
                    put("createdAt", p.createdAt)
                })
            }
        })

        // Payee Accounts
        val payeeAccounts = database.payeeDao().getAllPayeeAccounts().first()
        root.put(KEY_PAYEE_ACCOUNTS, JSONArray().apply {
            payeeAccounts.forEach { pa ->
                put(JSONObject().apply {
                    put("id", pa.id)
                    put("payeeId", pa.payeeId)
                    put("bankName", pa.bankName)
                    put("accountNumber", pa.accountNumber)
                    put("recipientName", pa.recipientName)
                    put("type", pa.type)
                    put("nickname", pa.nickname)
                })
            }
        })

        // SMS Sender Mappings
        val mappings = database.smsSenderMappingDao().getAllMappingsFlow().first()
        root.put(KEY_SMS_MAPPINGS, JSONArray().apply {
            mappings.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("senderAddress", m.senderAddress)
                    put("accountId", m.accountId)
                })
            }
        })

        // DataStore Preferences (non-sensitive only)
        root.put(KEY_PREFERENCES, JSONObject().apply {
            put("theme_mode", preferencesManager.themeMode.first())
            put("sms_sync_choice", preferencesManager.smsSyncChoice.first())
            put("profile_name", preferencesManager.profileName.first())
            put("hide_card_balances", preferencesManager.hideCardBalances.first())
            put("onboarding_completed", true) // always mark as completed in restore
        })

        // Write JSON to output
        val jsonBytes = root.toString(2).toByteArray(Charsets.UTF_8)
        outputStream.write(jsonBytes)
        outputStream.flush()
    }

    // ─── IMPORT ─────────────────────────────────────────────────

    /**
     * Reads JSON from [inputStream], clears all existing Room data,
     * inserts the backup records, and restores DataStore preferences.
     *
     * App lock is intentionally reset for security.
     */
    suspend fun importData(
        context: Context,
        inputStream: InputStream,
        database: FinanceDatabase,
        preferencesManager: PreferencesManager
    ) = withContext(Dispatchers.IO) {
        val jsonString = inputStream.bufferedReader(Charsets.UTF_8).readText()
        val root = JSONObject(jsonString)

        // Version check for forward compatibility
        val version = root.optInt(KEY_VERSION, 1)
        if (version > BACKUP_VERSION) {
            throw IllegalStateException("Backup file version ($version) is newer than this app supports ($BACKUP_VERSION). Please update FinanceBuddy.")
        }

        val accountDao = database.accountDao()
        val transactionDao = database.transactionDao()
        val budgetDao = database.budgetDao()
        val goalDao = database.goalDao()
        val loanDao = database.loanDao()
        val pendingSmsDao = database.pendingSmsDao()
        val payeeDao = database.payeeDao()
        val smsMappingDao = database.smsSenderMappingDao()

        // ── Clear all existing data ─────────────────────────────
        // Delete in reverse-dependency order
        smsMappingDao.deleteAll()
        payeeDao.deleteAllPayeeAccounts()
        payeeDao.deleteAllPayees()
        pendingSmsDao.clearAll()
        loanDao.deleteAll()
        goalDao.deleteAll()
        budgetDao.deleteAll()
        transactionDao.deleteAll()
        accountDao.deleteAll()

        // ── Insert accounts ─────────────────────────────────────
        val accountsArray = root.optJSONArray(KEY_ACCOUNTS) ?: JSONArray()
        for (i in 0 until accountsArray.length()) {
            val a = accountsArray.getJSONObject(i)
            accountDao.insertAccount(
                AccountEntity(
                    id = a.getInt("id"),
                    name = a.getString("name"),
                    type = a.getString("type"),
                    balance = a.getDouble("balance"),
                    colorHex = a.getString("colorHex"),
                    accountSubtype = a.optString("accountSubtype", ""),
                    isManaged = a.optBoolean("isManaged", false),
                    holderName = a.optString("holderName", ""),
                    accountNumber = a.optString("accountNumber", ""),
                    showAs = a.optString("showAs", "")
                )
            )
        }

        // ── Insert transactions ─────────────────────────────────
        val txArray = root.optJSONArray(KEY_TRANSACTIONS) ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val t = txArray.getJSONObject(i)
            transactionDao.insertTransactionDirect(
                TransactionEntity(
                    id = t.getInt("id"),
                    amount = t.getDouble("amount"),
                    type = t.getString("type"),
                    category = t.getString("category"),
                    timestamp = t.getLong("timestamp"),
                    fromAccountId = t.getInt("fromAccountId"),
                    toAccountId = if (t.isNull("toAccountId")) null else t.getInt("toAccountId"),
                    note = t.optString("note", "")
                )
            )
        }

        // ── Insert budgets ──────────────────────────────────────
        val budgetArray = root.optJSONArray(KEY_BUDGETS) ?: JSONArray()
        for (i in 0 until budgetArray.length()) {
            val b = budgetArray.getJSONObject(i)
            budgetDao.insertBudget(
                BudgetEntity(
                    id = b.getInt("id"),
                    category = b.getString("category"),
                    limitAmount = b.getDouble("limitAmount"),
                    colorHex = b.getString("colorHex")
                )
            )
        }

        // ── Insert goals ────────────────────────────────────────
        val goalArray = root.optJSONArray(KEY_GOALS) ?: JSONArray()
        for (i in 0 until goalArray.length()) {
            val g = goalArray.getJSONObject(i)
            goalDao.insertGoal(
                GoalEntity(
                    id = g.getInt("id"),
                    title = g.getString("title"),
                    targetAmount = g.getDouble("targetAmount"),
                    savedAmount = g.getDouble("savedAmount"),
                    colorHex = g.getString("colorHex"),
                    emoji = g.getString("emoji"),
                    deadline = if (g.isNull("deadline")) null else g.getLong("deadline"),
                    createdAt = g.getLong("createdAt")
                )
            )
        }

        // ── Insert loans ────────────────────────────────────────
        val loanArray = root.optJSONArray(KEY_LOANS) ?: JSONArray()
        for (i in 0 until loanArray.length()) {
            val l = loanArray.getJSONObject(i)
            loanDao.insertLoan(
                LoanEntity(
                    id = l.getInt("id"),
                    bankName = l.getString("bankName"),
                    loanAmount = l.getDouble("loanAmount"),
                    durationMonths = l.getInt("durationMonths"),
                    interestRate = l.getDouble("interestRate"),
                    repaidAmount = l.optDouble("repaidAmount", 0.0),
                    accountId = l.optInt("accountId", 0),
                    loanType = l.optString("loanType", "BANK"),
                    lenderName = l.optString("lenderName", ""),
                    isLent = l.optBoolean("isLent", false),
                    createdAt = l.getLong("createdAt")
                )
            )
        }

        // ── Insert pending SMS transactions ─────────────────────
        val pendingArray = root.optJSONArray(KEY_PENDING_SMS) ?: JSONArray()
        for (i in 0 until pendingArray.length()) {
            val p = pendingArray.getJSONObject(i)
            pendingSmsDao.insertPending(
                PendingSmsTransactionEntity(
                    id = p.getInt("id"),
                    rawSmsBody = p.getString("rawSmsBody"),
                    senderAddress = p.getString("senderAddress"),
                    amount = p.getDouble("amount"),
                    type = p.getString("type"),
                    category = p.getString("category"),
                    note = p.getString("note"),
                    detectedAccountName = p.getString("detectedAccountName"),
                    fromAccountId = p.getInt("fromAccountId"),
                    toAccountId = if (p.isNull("toAccountId")) null else p.getInt("toAccountId"),
                    timestamp = p.getLong("timestamp"),
                    receivedAt = p.getLong("receivedAt")
                )
            )
        }

        // ── Insert payees ───────────────────────────────────────
        val payeeArray = root.optJSONArray(KEY_PAYEES) ?: JSONArray()
        for (i in 0 until payeeArray.length()) {
            val p = payeeArray.getJSONObject(i)
            payeeDao.insertPayee(
                PayeeEntity(
                    id = p.getInt("id"),
                    name = p.getString("name"),
                    uniqueId = p.getString("uniqueId"),
                    createdAt = p.getLong("createdAt")
                )
            )
        }

        // ── Insert payee accounts ───────────────────────────────
        val paArray = root.optJSONArray(KEY_PAYEE_ACCOUNTS) ?: JSONArray()
        for (i in 0 until paArray.length()) {
            val pa = paArray.getJSONObject(i)
            payeeDao.insertPayeeAccount(
                PayeeAccountEntity(
                    id = pa.getInt("id"),
                    payeeId = pa.getInt("payeeId"),
                    bankName = pa.getString("bankName"),
                    accountNumber = pa.getString("accountNumber"),
                    recipientName = pa.getString("recipientName"),
                    type = pa.getString("type"),
                    nickname = pa.optString("nickname", "")
                )
            )
        }

        // ── Insert SMS sender mappings ──────────────────────────
        val mappingArray = root.optJSONArray(KEY_SMS_MAPPINGS) ?: JSONArray()
        for (i in 0 until mappingArray.length()) {
            val m = mappingArray.getJSONObject(i)
            smsMappingDao.insertMapping(
                SmsSenderMappingEntity(
                    id = m.getInt("id"),
                    senderAddress = m.getString("senderAddress"),
                    accountId = m.getInt("accountId")
                )
            )
        }

        // ── Restore DataStore preferences ───────────────────────
        val prefs = root.optJSONObject(KEY_PREFERENCES)
        if (prefs != null) {
            preferencesManager.setThemeMode(prefs.optString("theme_mode", "SYSTEM"))
            preferencesManager.setSmsSyncChoice(prefs.optString("sms_sync_choice", "DISABLED"))
            preferencesManager.setProfileName(prefs.optString("profile_name", "User"))
            preferencesManager.setHideCardBalances(prefs.optBoolean("hide_card_balances", false))
            preferencesManager.setOnboardingCompleted()
            // Intentionally reset app lock for security
            preferencesManager.setAppLockEnabled(false)
            preferencesManager.setAppLockPin("")
        }
    }
}
