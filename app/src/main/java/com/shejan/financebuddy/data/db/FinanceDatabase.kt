package com.shejan.financebuddy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AccountEntity::class, TransactionEntity::class, BudgetEntity::class, GoalEntity::class], version = 4, exportSchema = false)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_buddy_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDatabase(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try {
                            val count = db.compileStatement("SELECT COUNT(*) FROM accounts").simpleQueryForLong()
                            if (count == 0L) {
                                seedDatabase(db)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    private fun seedDatabase(db: SupportSQLiteDatabase) {
                        // Seed default Bangladeshi accounts/MFS into database
                        // Everyday Digital Banks
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('BRAC Bank PLC', 'BANK', 0.0, '#0096FF')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('The City Bank PLC', 'BANK', 0.0, '#007A33')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Eastern Bank PLC (EBL)', 'BANK', 0.0, '#003366')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Dutch-Bangla Bank PLC (DBBL)', 'BANK', 0.0, '#7C5CFC')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Prime Bank PLC', 'BANK', 0.0, '#FF5722')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Mutual Trust Bank PLC', 'BANK', 0.0, '#0C2340')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Islami Bank Bangladesh PLC (IBBL)', 'BANK', 0.0, '#1B5E20')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Al-Arafah Islami Bank PLC', 'BANK', 0.0, '#2E7D32')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Shahjalal Islami Bank PLC', 'BANK', 0.0, '#008080')")

                        // Mobile Financial Services (MFS)
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('bKash', 'MFS', 0.0, '#FF5C7C')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Nagad', 'MFS', 0.0, '#FFBD2E')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Rocket', 'MFS', 0.0, '#00D4AA')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Upay', 'MFS', 0.0, '#FFB300')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('CellFin (IBBL)', 'MFS', 0.0, '#4CAF50')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Ok Wallet', 'MFS', 0.0, '#FF5722')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('MyCash', 'MFS', 0.0, '#3F51B5')")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
