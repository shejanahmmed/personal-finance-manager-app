package com.shejan.financebuddy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [AccountEntity::class, TransactionEntity::class, BudgetEntity::class, GoalEntity::class, PendingSmsTransactionEntity::class, PayeeEntity::class, PayeeAccountEntity::class, SmsSenderMappingEntity::class, LoanEntity::class], version = 12, exportSchema = false)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun pendingSmsDao(): PendingSmsDao
    abstract fun payeeDao(): PayeeDao
    abstract fun smsSenderMappingDao(): SmsSenderMappingDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                // Retrieve (or generate on first launch) the AES-256 passphrase
                // from Android Keystore-backed EncryptedSharedPreferences.
                val passphrase: ByteArray = DatabaseKeyManager.getOrCreatePassphrase(context)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "financebuddy_encrypted.db"
                )
                .openHelperFactory(factory)
                .addMigrations(*DatabaseMigrations.ALL)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
