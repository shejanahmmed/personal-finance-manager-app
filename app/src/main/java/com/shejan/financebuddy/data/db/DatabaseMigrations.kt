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

    /** Convenience list — pass this to addMigrations() */
    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
