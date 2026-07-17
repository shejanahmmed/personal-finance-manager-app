package com.shejan.financebuddy.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.data.db.PendingSmsTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives incoming SMS messages and, if they match a known Bangladeshi bank/MFS pattern,
 * inserts a pending transaction into the encrypted Room database for user review.
 *
 * Security notes:
 *  - android:exported="false" in the manifest — only the Android system can trigger this receiver.
 *  - android:permission="android.permission.BROADCAST_SMS" — only system can send these broadcasts.
 *  - Sender whitelist enforced inside [SmsParser.resolveAccount] before any regex is run.
 *  - No data ever leaves the device; everything goes into the SQLCipher-encrypted Room DB.
 *  - [rawSmsBody] is stored only in the pending table and is deleted when the user acts on the entry.
 */
class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"

    // A coroutine scope tied to the application process, not to an Activity lifecycle.
    // SupervisorJob ensures that a failure in one child doesn't cancel the others.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        // Reconstruct full message body (multi-part SMS arrives in chunks)
        val byOriginator = mutableMapOf<String, StringBuilder>()
        for (sms in messages) {
            byOriginator
                .getOrPut(sms.originatingAddress ?: continue) { StringBuilder() }
                .append(sms.messageBody ?: "")
        }

        val db = FinanceDatabase.getDatabase(context.applicationContext)
        val pendingSmsDao = db.pendingSmsDao()
        val accountDao = db.accountDao()

        for ((sender, bodyBuilder) in byOriginator) {
            val body = bodyBuilder.toString().trim()
            if (body.isBlank()) continue

            scope.launch {
                val mapping = db.smsSenderMappingDao().getMappingForSenderOnce(sender)
                val accounts = accountDao.getAllAccountsOnce()

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

                if (parsed == null) return@launch

                Log.d(TAG, "Parsed SMS from $sender: amount=${parsed.amount} type=${parsed.type}")

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
                    timestamp           = parsed.timestamp ?: System.currentTimeMillis(),
                    receivedAt          = System.currentTimeMillis()
                )

                pendingSmsDao.insertPending(pending)
                Log.d(TAG, "Inserted pending transaction for ${parsed.detectedAccountName}")
            }
        }
    }
}
