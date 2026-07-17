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
    suspend fun syncPreviousSms(context: Context, database: FinanceDatabase, daysLimit: Int? = 30): Int = withContext(Dispatchers.IO) {
        if (!isReadSmsPermissionGranted(context)) {
            Log.w(TAG, "READ_SMS permission not granted. Aborting sync.")
            return@withContext 0
        }

        val pendingSmsDao = database.pendingSmsDao()
        val accountDao = database.accountDao()
        val senderMappingDao = database.smsSenderMappingDao()

        val accounts = accountDao.getAllAccountsOnce()
        val mappings = senderMappingDao.getAllMappingsOnce()
        val mappingMap = mappings.associateBy { it.senderAddress.lowercase().trim() }

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")

        // Build selection dynamically based on daysLimit
        val selection: String?
        val selectionArgs: Array<String>?
        if (daysLimit != null && daysLimit > 0) {
            val limitMillis = System.currentTimeMillis() - (daysLimit.toLong() * 24 * 60 * 60 * 1000)
            selection = "date >= ?"
            selectionArgs = arrayOf(limitMillis.toString())
        } else {
            selection = null
            selectionArgs = null
        }

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

                    val senderLower = sender.lowercase().trim()
                    val mapping = mappingMap[senderLower]

                    val parsed = if (mapping != null) {
                        val matchedAccount = accounts.find { it.id == mapping.accountId }
                        if (matchedAccount != null) {
                            SmsParser.parse(
                                sender = sender,
                                body = body,
                                resolvedAccountName = matchedAccount.name,
                                bankIndicator = matchedAccount.name
                            )
                        } else {
                            SmsParser.parse(sender, body)
                        }
                    } else {
                        SmsParser.parse(sender, body)
                    }

                    if (parsed == null) continue

                    // Check for duplicates in pending table
                    val existsInPending = pendingSmsDao.isSmsExists(body)
                    if (existsInPending) continue

                    // Match account name
                    val matchedAccount = if (mapping != null) {
                        accounts.find { it.id == mapping.accountId }
                    } else {
                        accounts.firstOrNull { acc ->
                            acc.name.equals(parsed.detectedAccountName, ignoreCase = true)
                        }
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

    /**
     * Scans the system SMS inbox for potential transaction messages from unknown, non-whitelisted,
     * and currently unmapped senders.
     */
    suspend fun findPotentialUnknownSenders(context: Context, database: FinanceDatabase): List<PotentialSender> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PotentialSender>()
        if (!isReadSmsPermissionGranted(context)) return@withContext result

        val mappings = database.smsSenderMappingDao().getAllMappingsOnce()
        val mappedAddresses = mappings.map { it.senderAddress.lowercase().trim() }.toSet()

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")

        // Search for keywords that suggest transaction messages
        val selection = "body LIKE '%Tk%' OR body LIKE '%BDT%' OR body LIKE '%৳%' OR body LIKE '%received%' OR body LIKE '%sent%' OR body LIKE '%paid%'"
        
        val seenSenders = mutableSetOf<String>()

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "date DESC"
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow("address")
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")

                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIdx) ?: continue
                    val senderLower = sender.lowercase().trim()

                    // Skip standard whitelisted senders
                    if (SmsParser.resolveAccount(sender) != null) continue
                    // Skip already mapped senders
                    if (mappedAddresses.contains(senderLower)) continue
                    // Skip duplicate senders in the scanned list
                    if (seenSenders.contains(senderLower)) continue

                    val body = cursor.getString(bodyIdx) ?: continue
                    val date = cursor.getLong(dateIdx)

                    seenSenders.add(senderLower)
                    result.add(PotentialSender(
                        senderAddress = sender,
                        latestMessage = body,
                        timestamp = date
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding potential senders: ${e.message}", e)
        }

        return@withContext result
    }

    /**
     * Sycns past SMS history from a specific sender address using the mapped account's context.
     */
    suspend fun syncPreviousSmsForSender(context: Context, database: FinanceDatabase, senderAddress: String, accountId: Int): Int = withContext(Dispatchers.IO) {
        if (!isReadSmsPermissionGranted(context)) return@withContext 0

        val pendingSmsDao = database.pendingSmsDao()
        val accountDao = database.accountDao()
        val accounts = accountDao.getAllAccountsOnce()
        val matchedAccount = accounts.find { it.id == accountId } ?: return@withContext 0

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")

        val selection = "address = ?"
        val selectionArgs = arrayOf(senderAddress)

        var importedCount = 0

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date DESC"
            )?.use { cursor ->
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")

                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyIdx) ?: continue
                    val smsDate = cursor.getLong(dateIdx)

                    // Attempt to parse SMS with mapping context
                    val parsed = SmsParser.parse(
                        sender = senderAddress,
                        body = body,
                        resolvedAccountName = matchedAccount.name,
                        bankIndicator = matchedAccount.name
                    ) ?: continue

                    // Check for duplicates in pending table
                    val existsInPending = pendingSmsDao.isSmsExists(body)
                    if (existsInPending) continue

                    val pending = PendingSmsTransactionEntity(
                        rawSmsBody          = body,
                        senderAddress       = senderAddress,
                        amount              = parsed.amount,
                        type                = parsed.type,
                        category            = parsed.category,
                        note                = parsed.note,
                        detectedAccountName = parsed.detectedAccountName,
                        fromAccountId       = matchedAccount.id,
                        toAccountId         = null,
                        timestamp           = parsed.timestamp ?: smsDate,
                        receivedAt          = smsDate
                    )

                    pendingSmsDao.insertPending(pending)
                    importedCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing SMS for specific sender: ${e.message}", e)
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

data class PotentialSender(
    val senderAddress: String,
    val latestMessage: String,
    val timestamp: Long
)
