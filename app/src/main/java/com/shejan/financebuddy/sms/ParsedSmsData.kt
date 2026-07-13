package com.shejan.financebuddy.sms

/**
 * The result of a successful SMS parse.
 * All on-device — no network calls, no third-party APIs.
 */
data class ParsedSmsData(
    val amount: Double,

    /** "INCOME" or "EXPENSE". TRANSFER is determined later if the user selects a toAccount. */
    val type: String,

    /** Suggested category for the transaction (e.g. "Mobile Banking", "Salary"). */
    val category: String,

    /** Merchant name, recipient, or reference note extracted from the SMS. */
    val note: String,

    /** The canonical account name as stored in the DB (e.g. "bKash", "BRAC Bank PLC"). */
    val detectedAccountName: String,

    /** Timestamp if parseable from the SMS, otherwise the caller provides System.currentTimeMillis(). */
    val timestamp: Long? = null
)
