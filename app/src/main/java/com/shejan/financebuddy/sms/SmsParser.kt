package com.shejan.financebuddy.sms

import android.util.Log

/**
 * Pure-Kotlin, on-device SMS parser for Bangladeshi banks and MFS providers.
 *
 * Design principles:
 *  - No network calls, no reflection, no dynamic code.
 *  - Returns null for any SMS that doesn't match a known pattern (safe fail).
 *  - Each bank/MFS block is isolated so patterns never cross-contaminate.
 *
 * Sender whitelist strategy:
 *  The caller (SmsReceiver) passes the sender address. We check it against
 *  a whitelist of known alphanumeric sender IDs before even attempting regex.
 *  This prevents spoofing by random numbers pretending to be a bank.
 */
object SmsParser {

    private const val TAG = "SmsParser"

    // ─── Amount regex helpers ─────────────────────────────────────────────────

    // Matches: Tk 1,234.56 / Tk1234.56 / BDT 1,234.56 / Taka 1,234 / ৳1,234.56
    private val AMOUNT_REGEX = Regex(
        """(?:Tk\.?|BDT\.?|Taka|৳)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private fun parseAmount(raw: String): Double? =
        raw.replace(",", "").toDoubleOrNull()

    // ─── Sender whitelist ─────────────────────────────────────────────────────

    /**
     * Maps known sender IDs (case-insensitive) to their canonical account names.
     * Add new IDs here as needed.
     */
    private val SENDER_ACCOUNT_MAP: Map<String, String> = mapOf(
        // bKash
        "bkash"         to "bKash",
        "bkashsms"      to "bKash",

        // Nagad
        "nagad"         to "Nagad",
        "nagadsms"      to "Nagad",

        // Rocket (DBBL MFS)
        "rocket"        to "Rocket",
        "dbblrocket"    to "Rocket",

        // Upay
        "upay"          to "Upay",
        "upaysms"       to "Upay",

        // CellFin (IBBL)
        "cellfin"       to "CellFin (IBBL)",
        "ibblcellfin"   to "CellFin (IBBL)",

        // Ok Wallet
        "okwallet"      to "Ok Wallet",
        "okcash"        to "Ok Wallet",

        // MyCash
        "mycash"        to "MyCash",

        // BRAC Bank
        "bracbank"      to "BRAC Bank PLC",
        "brac"          to "BRAC Bank PLC",
        "bracbanksms"   to "BRAC Bank PLC",

        // City Bank
        "citybank"      to "The City Bank PLC",
        "thecitybank"   to "The City Bank PLC",
        "citybanksms"   to "The City Bank PLC",

        // EBL
        "ebl"           to "Eastern Bank PLC (EBL)",
        "easternbank"   to "Eastern Bank PLC (EBL)",
        "eblsms"        to "Eastern Bank PLC (EBL)",

        // DBBL (Dutch-Bangla)
        "dbbl"          to "Dutch-Bangla Bank PLC (DBBL)",
        "dutchbangla"   to "Dutch-Bangla Bank PLC (DBBL)",
        "dbblsms"       to "Dutch-Bangla Bank PLC (DBBL)",
        "dutchbanglabank" to "Dutch-Bangla Bank PLC (DBBL)",

        // Prime Bank
        "primebank"     to "Prime Bank PLC",
        "primebanksms"  to "Prime Bank PLC",

        // Mutual Trust Bank
        "mtb"           to "Mutual Trust Bank PLC",
        "mutualtrustbank" to "Mutual Trust Bank PLC",
        "mtbsms"        to "Mutual Trust Bank PLC",

        // Islami Bank
        "ibbl"          to "Islami Bank Bangladesh PLC (IBBL)",
        "islamibank"    to "Islami Bank Bangladesh PLC (IBBL)",
        "ibblsms"       to "Islami Bank Bangladesh PLC (IBBL)",

        // Al-Arafah Islami Bank
        "alarafah"      to "Al-Arafah Islami Bank PLC",
        "alarafahbank"  to "Al-Arafah Islami Bank PLC",

        // Shahjalal Islami Bank
        "sjibl"         to "Shahjalal Islami Bank PLC",
        "shahjalal"     to "Shahjalal Islami Bank PLC",
        "sjibsms"       to "Shahjalal Islami Bank PLC"
    )

    /**
     * Returns the canonical account name if [sender] is on the whitelist, null otherwise.
     * A numeric-only sender (e.g. "+8801XXXXXXX") is never whitelisted.
     */
    fun resolveAccount(sender: String): String? {
        // Numeric-only senders are rejected — real bank shortcodes are alphanumeric
        if (sender.trimStart('+').all { it.isDigit() }) return null
        return SENDER_ACCOUNT_MAP[sender.lowercase().trim()]
    }

    /**
     * Attempts to parse a financial transaction from an SMS.
     *
     * @param sender              The originating address.
     * @param body                The full SMS message text.
     * @param resolvedAccountName Optional pre-resolved account name (from custom mappings).
     * @param bankIndicator       Optional bank/MFS indicator string (to route parsing patterns).
     * @return                    A [ParsedSmsData] on success, or null if the SMS is not a recognisable transaction.
     */
    fun parse(
        sender: String,
        body: String,
        resolvedAccountName: String? = null,
        bankIndicator: String? = null
    ): ParsedSmsData? {
        val accountName = resolvedAccountName ?: resolveAccount(sender) ?: return null
        val lookupString = bankIndicator?.lowercase() ?: sender.lowercase().trim()

        return try {
            when {
                isBkash(lookupString)      -> parseBkash(body, accountName)
                isNagad(lookupString)      -> parseNagad(body, accountName)
                isRocket(lookupString)     -> parseRocket(body, accountName)
                isUpay(lookupString)       -> parseUpay(body, accountName)
                isCellfin(lookupString)    -> parseCellfin(body, accountName)
                isOkWallet(lookupString)   -> parseOkWallet(body, accountName)
                isMyCash(lookupString)     -> parseMyCash(body, accountName)
                isBracBank(lookupString)   -> parseBracBank(body, accountName)
                isCityBank(lookupString)   -> parseCityBank(body, accountName)
                isEbl(lookupString)        -> parseEbl(body, accountName)
                isDbbl(lookupString)       -> parseDbbl(body, accountName)
                isPrimeBank(lookupString)  -> parsePrimeBank(body, accountName)
                isMtb(lookupString)        -> parseMtb(body, accountName)
                isIbbl(lookupString)       -> parseIbbl(body, accountName)
                isAlArafah(lookupString)   -> parseAlArafah(body, accountName)
                isSjibl(lookupString)      -> parseSjibl(body, accountName)
                else                      -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Parse error for sender=$sender: ${e.message}")
            null
        }
    }

    // ─── Sender group checks ──────────────────────────────────────────────────

    private fun isBkash(s: String)     = s.contains("bkash")
    private fun isNagad(s: String)     = s.contains("nagad")
    private fun isRocket(s: String)    = s.contains("rocket") || s.contains("dbblrocket")
    private fun isUpay(s: String)      = s.contains("upay")
    private fun isCellfin(s: String)   = s.contains("cellfin")
    private fun isOkWallet(s: String)  = s.contains("okwallet") || s.contains("okcash")
    private fun isMyCash(s: String)    = s.contains("mycash")
    private fun isBracBank(s: String)  = s.contains("brac")
    private fun isCityBank(s: String)  = s.contains("citybank") || s.contains("thecitybank")
    private fun isEbl(s: String)       = s.contains("ebl") || s.contains("easternbank")
    private fun isDbbl(s: String)      = s.contains("dbbl") || s.contains("dutchbangla")
    private fun isPrimeBank(s: String) = s.contains("primebank")
    private fun isMtb(s: String)       = s.contains("mtb") || s.contains("mutualtrustbank")
    private fun isIbbl(s: String)      = s.contains("ibbl") || s.contains("islamibank")
    private fun isAlArafah(s: String)  = s.contains("alarafah")
    private fun isSjibl(s: String)     = s.contains("sjibl") || s.contains("shahjalal")

    // ─── bKash ───────────────────────────────────────────────────────────────
    // Examples:
    //   "bKash account 01XXXXXXXXX Tk 500.00 paid to 01XXXXXXXXX Ref ABCDEF."
    //   "You have received Tk 1,000.00 from 01XXXXXXXXX at your bKash account."
    //   "Your bKash account Tk 200.00 has been charged. Balance: Tk 4,800.00."

    private fun parseBkash(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("received") || b.contains("credited") || b.contains("added") || b.contains("cash in") -> {
                val note = extractBkashSender(body) ?: "bKash Credit"
                ParsedSmsData(amount, "INCOME", "Mobile Banking", note, accountName)
            }
            b.contains("paid") || b.contains("sent") || b.contains("charged") ||
            b.contains("deducted") || b.contains("payment") || b.contains("cash out") -> {
                val note = extractBkashRecipient(body) ?: "bKash Payment"
                ParsedSmsData(amount, "EXPENSE", "Mobile Banking", note, accountName)
            }
            else -> null
        }
    }

    private fun extractBkashSender(body: String): String? =
        Regex("""from\s+([0-9]{11})""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)

    private fun extractBkashRecipient(body: String): String? =
        Regex("""(?:paid to|sent to|to)\s+([^\s.]+)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)

    // ─── Nagad ───────────────────────────────────────────────────────────────
    // Examples:
    //   "Tk.500 has been sent from your Nagad account to 01XXXXXXXXX. Charge Tk 5.00. Balance Tk 495.00."
    //   "Your Nagad account has received Tk.1,000.00 from 01XXXXXXXXX."

    private fun parseNagad(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("received") || b.contains("credited") || b.contains("cash in") -> {
                val note = Regex("""from\s+([0-9]{11})""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1) ?: "Nagad Credit"
                ParsedSmsData(amount, "INCOME", "Mobile Banking", note, accountName)
            }
            b.contains("sent") || b.contains("paid") || b.contains("payment") ||
            b.contains("charged") || b.contains("cash out") -> {
                val note = Regex("""to\s+([0-9]{11}|[A-Za-z\s]+)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.trim() ?: "Nagad Payment"
                ParsedSmsData(amount, "EXPENSE", "Mobile Banking", note, accountName)
            }
            else -> null
        }
    }

    // ─── Rocket ──────────────────────────────────────────────────────────────
    // Example:
    //   "BDT 500.00 has been debited from your Rocket A/C. Ref: XXXXX."
    //   "BDT 1000.00 credited to your Rocket A/C from 01XXXXXXXXX."

    private fun parseRocket(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("credited") || b.contains("received") -> {
                val note = Regex("""from\s+([0-9]{11}|[A-Za-z\s]+)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.trim() ?: "Rocket Credit"
                ParsedSmsData(amount, "INCOME", "Mobile Banking", note, accountName)
            }
            b.contains("debited") || b.contains("charged") || b.contains("payment") -> {
                val note = Regex("""(?:to|ref[:\s]+)\s*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1) ?: "Rocket Payment"
                ParsedSmsData(amount, "EXPENSE", "Mobile Banking", note, accountName)
            }
            else -> null
        }
    }

    // ─── Upay ────────────────────────────────────────────────────────────────
    private fun parseUpay(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("received") || b.contains("credit") || b.contains("added") ->
                ParsedSmsData(amount, "INCOME", "Mobile Banking", "Upay Credit", accountName)
            b.contains("debited") || b.contains("paid") || b.contains("sent") ->
                ParsedSmsData(amount, "EXPENSE", "Mobile Banking", "Upay Payment", accountName)
            else -> null
        }
    }

    // ─── CellFin (IBBL) ──────────────────────────────────────────────────────
    private fun parseCellfin(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("received") || b.contains("credited") ->
                ParsedSmsData(amount, "INCOME", "Mobile Banking", "CellFin Credit", accountName)
            b.contains("debited") || b.contains("paid") ->
                ParsedSmsData(amount, "EXPENSE", "Mobile Banking", "CellFin Payment", accountName)
            else -> null
        }
    }

    // ─── Ok Wallet ───────────────────────────────────────────────────────────
    private fun parseOkWallet(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("received") || b.contains("credit") ->
                ParsedSmsData(amount, "INCOME", "Mobile Banking", "Ok Wallet Credit", accountName)
            b.contains("debit") || b.contains("paid") || b.contains("sent") ->
                ParsedSmsData(amount, "EXPENSE", "Mobile Banking", "Ok Wallet Payment", accountName)
            else -> null
        }
    }

    // ─── MyCash ──────────────────────────────────────────────────────────────
    private fun parseMyCash(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("received") || b.contains("credited") ->
                ParsedSmsData(amount, "INCOME", "Mobile Banking", "MyCash Credit", accountName)
            b.contains("debited") || b.contains("paid") ->
                ParsedSmsData(amount, "EXPENSE", "Mobile Banking", "MyCash Payment", accountName)
            else -> null
        }
    }

    // ─── BRAC Bank ───────────────────────────────────────────────────────────
    // Example:
    //   "Your A/C XXXX credited BDT 5,000.00 on 13-Jul-26. Avl Bal BDT 10,000.00."
    //   "Your A/C XXXX debited BDT 500.00 at Merchant XYZ on 13-Jul-26."

    private fun parseBracBank(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        val merchantNote = Regex("""at\s+([A-Za-z0-9\s]+?)(?:\s+on|\.|,|$)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.trim()

        return when {
            b.contains("credited") || b.contains("credit") ->
                ParsedSmsData(amount, "INCOME", "Banking", merchantNote ?: "BRAC Bank Credit", accountName)
            b.contains("debited") || b.contains("debit") || b.contains("charged") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", merchantNote ?: "BRAC Bank Debit", accountName)
            else -> null
        }
    }

    // ─── City Bank ───────────────────────────────────────────────────────────
    // Example:
    //   "City Bank Txn: BDT 1,200.00 debited from A/C XXXX. Merchant: Shajahan Restaurant."
    //   "City Bank Txn: BDT 50,000.00 credited to A/C XXXX. Ref: Salary."

    private fun parseCityBank(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        val merchantNote = Regex("""(?:merchant|ref)[:\s]+([A-Za-z0-9\s]+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.trim()

        return when {
            b.contains("credited") || b.contains("credit") ->
                ParsedSmsData(amount, "INCOME", "Banking", merchantNote ?: "City Bank Credit", accountName)
            b.contains("debited") || b.contains("debit") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", merchantNote ?: "City Bank Debit", accountName)
            else -> null
        }
    }

    // ─── EBL ─────────────────────────────────────────────────────────────────
    // Example:
    //   "Your EBL A/C XXXX Cr BDT 10,000.00. Balance BDT 25,000.00."
    //   "Your EBL A/C XXXX Dr BDT 800.00 at POS XYZ. Balance BDT 24,200.00."

    private fun parseEbl(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        val merchantNote = Regex("""(?:at pos|at)\s+([A-Za-z0-9\s]+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.trim()

        return when {
            b.contains(" cr ") || b.contains("credited") ->
                ParsedSmsData(amount, "INCOME", "Banking", merchantNote ?: "EBL Credit", accountName)
            b.contains(" dr ") || b.contains("debited") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", merchantNote ?: "EBL Debit", accountName)
            else -> null
        }
    }

    // ─── DBBL (Dutch-Bangla) ─────────────────────────────────────────────────
    // Example:
    //   "Debit A/C No XXXX BDT 500.00 at Shop Name 13-Jul-26. Bal BDT 4,500.00."
    //   "Credit A/C No XXXX BDT 5,000.00. Bal BDT 9,500.00."

    private fun parseDbbl(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        val merchantNote = Regex("""at\s+([A-Za-z0-9\s]+?)\s+\d{2}""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.trim()

        return when {
            b.startsWith("credit") || b.contains("credited") ->
                ParsedSmsData(amount, "INCOME", "Banking", merchantNote ?: "DBBL Credit", accountName)
            b.startsWith("debit") || b.contains("debited") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", merchantNote ?: "DBBL Debit", accountName)
            else -> null
        }
    }

    // ─── Prime Bank ──────────────────────────────────────────────────────────
    private fun parsePrimeBank(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("credited") || b.contains("credit") ->
                ParsedSmsData(amount, "INCOME", "Banking", "Prime Bank Credit", accountName)
            b.contains("debited") || b.contains("debit") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", "Prime Bank Debit", accountName)
            else -> null
        }
    }

    // ─── Mutual Trust Bank ───────────────────────────────────────────────────
    private fun parseMtb(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("credited") || b.contains("credit") ->
                ParsedSmsData(amount, "INCOME", "Banking", "MTB Credit", accountName)
            b.contains("debited") || b.contains("debit") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", "MTB Debit", accountName)
            else -> null
        }
    }

    // ─── IBBL ────────────────────────────────────────────────────────────────
    // Example:
    //   "Taka 5,000.00 has been credited to your IBBL A/C."
    //   "Taka 500.00 has been debited from your IBBL A/C."

    private fun parseIbbl(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("credited") || b.contains("received") ->
                ParsedSmsData(amount, "INCOME", "Banking", "IBBL Credit", accountName)
            b.contains("debited") || b.contains("charged") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", "IBBL Debit", accountName)
            else -> null
        }
    }

    // ─── Al-Arafah Islami Bank ───────────────────────────────────────────────
    private fun parseAlArafah(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("credited") || b.contains("credit") ->
                ParsedSmsData(amount, "INCOME", "Banking", "Al-Arafah Bank Credit", accountName)
            b.contains("debited") || b.contains("debit") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", "Al-Arafah Bank Debit", accountName)
            else -> null
        }
    }

    // ─── Shahjalal Islami Bank ───────────────────────────────────────────────
    private fun parseSjibl(body: String, accountName: String): ParsedSmsData? {
        val b = body.lowercase()
        val amountStr = AMOUNT_REGEX.find(body)?.groupValues?.get(1) ?: return null
        val amount = parseAmount(amountStr) ?: return null

        return when {
            b.contains("credited") || b.contains("credit") ->
                ParsedSmsData(amount, "INCOME", "Banking", "Shahjalal Bank Credit", accountName)
            b.contains("debited") || b.contains("debit") ->
                ParsedSmsData(amount, "EXPENSE", "Banking", "Shahjalal Bank Debit", accountName)
            else -> null
        }
    }
}
