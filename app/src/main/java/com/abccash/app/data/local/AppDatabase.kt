package com.abccash.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abccash.app.data.local.dao.BalanceDao
import com.abccash.app.data.local.dao.CategoryDao
import com.abccash.app.data.local.dao.TransactionDao
import com.abccash.app.data.local.entity.BalanceEntity
import com.abccash.app.data.local.entity.CategoryEntity
import com.abccash.app.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, BalanceEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun balanceDao(): BalanceDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abc_cash.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
