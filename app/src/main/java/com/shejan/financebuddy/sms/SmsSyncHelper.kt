package com.shejan.financebuddy.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.data.db.PendingSmsTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsSyncHelper {
    private const val TAG = "SmsSyncHelper"

    /**
     * Scans the system SMS inbox for transaction messages from the last 30 days.
     * Filters, parses, and inserts them into the pending database.
     * 
     * @return The number of newly imported transactions.
     */
    suspend fun syncPreviousSms(context: Context, database: FinanceDatabase): Int = withContext(Dispatchers.IO) {
        if (!isReadSmsPermissionGranted(context)) {
            Log.w(TAG, "READ_SMS permission not granted. Aborting sync.")
            return@withContext 0
        }

        val pendingSmsDao = database.pendingSmsDao()
        val accountDao = database.accountDao()
        val accounts = accountDao.getAllAccountsOnce()

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")

        // Query the last 30 days of SMS messages
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val selection = "date >= ?"
        val selectionArgs = arrayOf(thirtyDaysAgo.toString())

        var importedCount = 0

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date DESC"
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow("address")
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")

                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIdx) ?: continue
                    val body = cursor.getString(bodyIdx) ?: continue
                    val smsDate = cursor.getLong(dateIdx)

                    // Attempt to parse SMS
                    val parsed = SmsParser.parse(sender, body) ?: continue

                    // Check for duplicates in pending table
                    val existsInPending = pendingSmsDao.isSmsExists(body)
                    if (existsInPending) continue

                    // Match account name
                    val matchedAccount = accounts.firstOrNull { acc ->
                        acc.name.equals(parsed.detectedAccountName, ignoreCase = true)
                    }

                    val pending = PendingSmsTransactionEntity(
                        rawSmsBody          = body,
                        senderAddress       = sender,
                        amount              = parsed.amount,
                        type                = parsed.type,
                        category            = parsed.category,
                        note                = parsed.note,
                        detectedAccountName = parsed.detectedAccountName,
                        fromAccountId       = matchedAccount?.id ?: -1,
                        toAccountId         = null,
                        timestamp           = parsed.timestamp ?: smsDate,
                        receivedAt          = smsDate
                    )

                    pendingSmsDao.insertPending(pending)
                    importedCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying system SMS provider: ${e.message}", e)
        }

        return@withContext importedCount
    }

    /** Returns true if READ_SMS permission is currently granted. */
    fun isReadSmsPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
