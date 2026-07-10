package com.abccash.app.treasury.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.io.File

@Database(
    entities = [
        EntrepriseEntity::class,
        UserEntity::class,
        InvoiceEntity::class,
        QuoteEntity::class,
        PaymentEntity::class,
        BankAccountEntity::class,
        ContactEntity::class,
        ExpenseEntity::class,
        BalanceCorrectionEntity::class
    ],
    version = 23,
    exportSchema = false
)
@TypeConverters(TreasuryConverters::class)
abstract class TreasuryDatabase : RoomDatabase() {
    abstract fun treasuryDao(): TreasuryDao

    companion object {
        private const val TAG = "TreasuryDatabase"
        private const val DB_NAME = "cashtrack_treasury.db"

        @Volatile
        private var INSTANCE: TreasuryDatabase? = null

        fun getInstance(context: Context): TreasuryDatabase {
            val appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: openDatabase(appContext).also { INSTANCE = it }
            }
        }

        private fun openDatabase(context: Context): TreasuryDatabase {
            return try {
                buildDatabase(context)
            } catch (error: Exception) {
                Log.e(TAG, "Database open failed, archiving corrupt database", error)
                synchronized(this) { INSTANCE = null }
                archiveCorruptDatabase(context)
                buildDatabase(context)
            }
        }

        private fun archiveCorruptDatabase(context: Context) {
            val timestamp = System.currentTimeMillis()
            listOf(DB_NAME, "$DB_NAME-shm", "$DB_NAME-wal").forEach { name ->
                val file = context.getDatabasePath(name)
                if (!file.exists()) return@forEach
                val archived = File(file.parent, "$name.corrupt.$timestamp")
                if (!file.renameTo(archived)) {
                    Log.w(TAG, "Could not archive $name, deleting instead")
                    file.delete()
                }
            }
        }

        private fun buildDatabase(context: Context): TreasuryDatabase =
            Room.databaseBuilder(context, TreasuryDatabase::class.java, DB_NAME)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23
                )
                .build()
    }
}
