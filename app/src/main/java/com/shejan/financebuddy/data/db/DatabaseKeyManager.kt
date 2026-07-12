package com.shejan.financebuddy.data.db

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Manages the SQLCipher database passphrase using Android Keystore + EncryptedSharedPreferences.
 *
 * Security model:
 *  - A random 32-byte (256-bit) passphrase is generated on first launch via [SecureRandom].
 *  - The passphrase is stored in [EncryptedSharedPreferences], which uses a hardware-backed
 *    AES-256-GCM key held inside the Android Keystore. The passphrase never leaves secure storage.
 *  - On every subsequent launch, the passphrase is read from EncryptedSharedPreferences and
 *    handed to SQLCipher's SupportFactory — the DB file remains AES-256-CBC encrypted at rest.
 *
 * Thread safety: Call [getOrCreatePassphrase] from a background thread (e.g. during DB init).
 */
object DatabaseKeyManager {

    private const val PREFS_FILE_NAME = "finance_buddy_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_BYTE_LENGTH = 32 // 256-bit

    /**
     * Returns the SQLCipher passphrase as a [ByteArray].
     * Generates and persists a new passphrase if one does not already exist.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = buildEncryptedPrefs(context)
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        return if (existing != null) {
            hexToBytes(existing)
        } else {
            val newPassphrase = generatePassphrase()
            prefs.edit()
                .putString(KEY_DB_PASSPHRASE, bytesToHex(newPassphrase))
                .apply()
            newPassphrase
        }
    }

    // ─── Private helpers ──────────────────────────────────────

    private fun buildEncryptedPrefs(context: Context): android.content.SharedPreferences {
        // MasterKey uses AES-256-GCM key stored in Android Keystore hardware security module.
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun generatePassphrase(): ByteArray {
        val bytes = ByteArray(PASSPHRASE_BYTE_LENGTH)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
