package com.abccash.app.treasury.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        EntrepriseEntity::class,
        UserEntity::class,
        InvoiceEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(TreasuryConverters::class)
abstract class TreasuryDatabase : RoomDatabase() {
    abstract fun treasuryDao(): TreasuryDao

    companion object {
        @Volatile
        private var INSTANCE: TreasuryDatabase? = null

        fun getInstance(context: Context): TreasuryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?:                 Room.databaseBuilder(
                    context.applicationContext,
                    TreasuryDatabase::class.java,
                    "cashtrack_treasury.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
