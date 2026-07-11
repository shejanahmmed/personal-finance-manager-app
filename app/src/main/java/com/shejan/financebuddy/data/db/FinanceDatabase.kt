package com.shejan.financebuddy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AccountEntity::class, TransactionEntity::class], version = 1, exportSchema = false)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao

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
                        // Seed default Bangladeshi accounts/MFS into database on first creation
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('BRAC Bank PLC', 'BANK', 0.0, '#0096FF')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Dutch-Bangla Bank (DBBL)', 'BANK', 0.0, '#7C5CFC')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('bKash', 'MFS', 0.0, '#FF5C7C')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Nagad', 'MFS', 0.0, '#FFBD2E')")
                        db.execSQL("INSERT INTO accounts (name, type, balance, colorHex) VALUES ('Rocket', 'MFS', 0.0, '#00D4AA')")
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
