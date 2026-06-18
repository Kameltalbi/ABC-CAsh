package com.abccash.app.treasury.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `date` TEXT NOT NULL,
                `isRecurring` INTEGER NOT NULL,
                `recurrence` TEXT,
                `isPaid` INTEGER NOT NULL,
                `createdDate` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `recurrenceEndDate` TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `invoices` ADD COLUMN `entrepriseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `entrepriseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_entrepriseId` ON `invoices` (`entrepriseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_entrepriseId` ON `expenses` (`entrepriseId`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val entrepriseId = resolveDefaultEntrepriseId(db) ?: return
        db.execSQL(
            "UPDATE invoices SET entrepriseId = '$entrepriseId' WHERE entrepriseId = ''"
        )
        db.execSQL(
            "UPDATE expenses SET entrepriseId = '$entrepriseId' WHERE entrepriseId = ''"
        )
    }
}

private fun resolveDefaultEntrepriseId(db: SupportSQLiteDatabase): String? {
    db.query("SELECT id FROM entreprises LIMIT 1").use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(0)
        }
    }
    db.query("SELECT entrepriseId FROM users WHERE entrepriseId != '' LIMIT 1").use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(0)
        }
    }
    return null
}
