package com.shejan.financebuddy.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All Room schema migrations for FinanceBuddy.
 *
 * Version history:
 *  v1 → v2 : Initial schema — accounts + transactions tables
 *  v2 → v3 : Added budgets table
 *  v3 → v4 : Added goals table
 *  v4 → v5 : Added pending_sms_transactions table (SMS auto-detection feature)
 *
 * Rule: NEVER use fallbackToDestructiveMigration in production.
 * Every schema change must have a corresponding Migration here.
 */
object DatabaseMigrations {

    // ─────────────────────────────────────────────────────────
    // v1 → v2 : Create accounts + transactions tables
    // ─────────────────────────────────────────────────────────
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // accounts table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `accounts` (
                    `id`        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name`      TEXT    NOT NULL,
                    `type`      TEXT    NOT NULL,
                    `balance`   REAL    NOT NULL,
                    `colorHex`  TEXT    NOT NULL
                )
                """.trimIndent()
            )

            // transactions table with FK constraint and index
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transactions` (
                    `id`            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `amount`        REAL    NOT NULL,
                    `type`          TEXT    NOT NULL,
                    `category`      TEXT    NOT NULL,
                    `timestamp`     INTEGER NOT NULL,
                    `fromAccountId` INTEGER NOT NULL,
                    `toAccountId`   INTEGER,
                    `note`          TEXT    NOT NULL DEFAULT '',
                    FOREIGN KEY(`fromAccountId`) REFERENCES `accounts`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_fromAccountId` ON `transactions` (`fromAccountId`)"
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // v2 → v3 : Add budgets table
    // ─────────────────────────────────────────────────────────
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `budgets` (
                    `id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `category`    TEXT    NOT NULL,
                    `limitAmount` REAL    NOT NULL,
                    `colorHex`    TEXT    NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // v3 → v4 : Add goals table
    // ─────────────────────────────────────────────────────────
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `goals` (
                    `id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title`        TEXT    NOT NULL,
                    `targetAmount` REAL    NOT NULL,
                    `savedAmount`  REAL    NOT NULL DEFAULT 0.0,
                    `colorHex`     TEXT    NOT NULL,
                    `emoji`        TEXT    NOT NULL,
                    `deadline`     INTEGER,
                    `createdAt`    INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // v4 → v5 : Add pending_sms_transactions table
    // ─────────────────────────────────────────────────────────
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pending_sms_transactions` (
                    `id`                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `rawSmsBody`          TEXT    NOT NULL,
                    `senderAddress`       TEXT    NOT NULL,
                    `amount`              REAL    NOT NULL,
                    `type`                TEXT    NOT NULL,
                    `category`            TEXT    NOT NULL,
                    `note`                TEXT    NOT NULL,
                    `detectedAccountName` TEXT    NOT NULL,
                    `fromAccountId`       INTEGER NOT NULL,
                    `toAccountId`         INTEGER,
                    `timestamp`           INTEGER NOT NULL,
                    `receivedAt`          INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // v5 → v6 : Add extra account profile fields
    // ─────────────────────────────────────────────────────────
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `accountSubtype` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `isManaged`      INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `holderName`     TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `accountNumber`  TEXT NOT NULL DEFAULT ''")
        }
    }

    // ─────────────────────────────────────────────────────────
    // v6 → v7 : Add payees & payee_accounts tables
    // ─────────────────────────────────────────────────────────
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `payees` (
                    `id`        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name`      TEXT    NOT NULL,
                    `uniqueId`  TEXT    NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `payee_accounts` (
                    `id`            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `payeeId`       INTEGER NOT NULL,
                    `bankName`      TEXT    NOT NULL,
                    `accountNumber` TEXT    NOT NULL,
                    `recipientName` TEXT    NOT NULL,
                    `type`          TEXT    NOT NULL,
                    FOREIGN KEY(`payeeId`) REFERENCES `payees`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_payee_accounts_payeeId` ON `payee_accounts` (`payeeId`)"
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    // v7 → v8 : Add showAs column to accounts table
    // ─────────────────────────────────────────────────────────
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `showAs` TEXT NOT NULL DEFAULT ''")
        }
    }

    // ─────────────────────────────────────────────────────────
    // v8 → v9 : Add nickname column to payee_accounts table
    // ─────────────────────────────────────────────────────────
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `payee_accounts` ADD COLUMN `nickname` TEXT NOT NULL DEFAULT ''")
        }
    }

    // ─────────────────────────────────────────────────────────
    // v9 → v10 : Create sms_sender_mappings table
    // ─────────────────────────────────────────────────────────
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `sms_sender_mappings` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `senderAddress` TEXT NOT NULL,
                    `accountId` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    // ─────────────────────────────────────────────────────────
    // v10 → v11 : Create loans table
    // ─────────────────────────────────────────────────────────
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `loans` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `bankName` TEXT NOT NULL,
                    `loanAmount` REAL NOT NULL,
                    `durationMonths` INTEGER NOT NULL,
                    `interestRate` REAL NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    /** Convenience list — pass this to addMigrations() */
    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
}


