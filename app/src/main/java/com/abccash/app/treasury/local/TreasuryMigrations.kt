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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `invoices` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'OTHER'")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'OTHER'")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `entreprises` ADD COLUMN `email` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `entreprises` ADD COLUMN `telephone` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `entreprises` ADD COLUMN `adresse` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `invoices` ADD COLUMN `categoryLabel` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `categoryLabel` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `paymentMethod` TEXT")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bank_accounts` (
                `id` TEXT NOT NULL,
                `entrepriseId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `bankName` TEXT NOT NULL DEFAULT '',
                `ibanLast4` TEXT NOT NULL DEFAULT '',
                `openingBalance` REAL NOT NULL DEFAULT 0,
                `alertLowBalance` REAL,
                `isDefault` INTEGER NOT NULL DEFAULT 0,
                `source` TEXT NOT NULL DEFAULT 'MANUAL',
                `createdDate` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_bank_accounts_entrepriseId` ON `bank_accounts` (`entrepriseId`)"
        )
        db.execSQL("ALTER TABLE `payments` ADD COLUMN `bankAccountId` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `bankAccountId` TEXT")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `contacts` (
                `id` TEXT NOT NULL,
                `entrepriseId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `email` TEXT NOT NULL DEFAULT '',
                `phone` TEXT NOT NULL DEFAULT '',
                `address` TEXT NOT NULL DEFAULT '',
                `notes` TEXT NOT NULL DEFAULT '',
                `createdDate` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_contacts_entrepriseId_type` ON `contacts` (`entrepriseId`, `type`)"
        )
        db.execSQL("ALTER TABLE `invoices` ADD COLUMN `clientContactId` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `supplierContactId` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `note` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `receiptImagePath` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `isExpenseNote` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `invoices` ADD COLUMN `documentStatus` TEXT NOT NULL DEFAULT 'VALIDATED'"
        )
        db.execSQL("ALTER TABLE `invoices` ADD COLUMN `amountExclTax` REAL")
        db.execSQL("ALTER TABLE `invoices` ADD COLUMN `tvaRate` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `invoices` ADD COLUMN `otherTaxRate` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `invoices` ADD COLUMN `otherTaxLabel` TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            "UPDATE `invoices` SET `amountExclTax` = `totalAmount` WHERE `amountExclTax` IS NULL"
        )
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `invoices` ADD COLUMN `lineItemsJson` TEXT NOT NULL DEFAULT '[]'"
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createQuotesTable(db)
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `quotes`")
        createQuotesTable(db)
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `products` (
                `id` TEXT NOT NULL,
                `entrepriseId` TEXT NOT NULL DEFAULT '',
                `name` TEXT NOT NULL,
                `unitPriceExclTax` REAL NOT NULL,
                `kind` TEXT NOT NULL DEFAULT 'SERVICE',
                `unit` TEXT NOT NULL DEFAULT 'PIECE',
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `createdDate` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_products_entrepriseId` ON `products` (`entrepriseId`)"
        )
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addTextColumnIfMissing("contacts", "countryCode", "NOT NULL DEFAULT ''")
        db.addTextColumnIfMissing("contacts", "legalName", "NOT NULL DEFAULT ''")
        db.addTextColumnIfMissing("contacts", "taxIdType", "DEFAULT NULL")
        db.addTextColumnIfMissing("contacts", "taxIdValue", "NOT NULL DEFAULT ''")
        db.addTextColumnIfMissing("contacts", "taxIdValidationStatus", "NOT NULL DEFAULT 'UNVERIFIED'")
        db.addTextColumnIfMissing("contacts", "addressLine1", "NOT NULL DEFAULT ''")
        db.addTextColumnIfMissing("contacts", "addressLine2", "NOT NULL DEFAULT ''")
        db.addTextColumnIfMissing("contacts", "postalCode", "NOT NULL DEFAULT ''")
        db.addTextColumnIfMissing("contacts", "city", "NOT NULL DEFAULT ''")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addTextColumnIfMissing("invoices", "otherTaxMode", "NOT NULL DEFAULT 'PERCENTAGE'")
        db.addTextColumnIfMissing("quotes", "otherTaxMode", "NOT NULL DEFAULT 'PERCENTAGE'")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addTextColumnIfMissing("products", "category", "NOT NULL DEFAULT 'OTHER'")
        db.addTextColumnIfMissing("products", "categoryLabel", "NOT NULL DEFAULT ''")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addTextColumnIfMissing("bank_accounts", "kind", "NOT NULL DEFAULT 'BANK'")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recurring templates must stay unpaid; older builds could mark the whole series as paid.
        db.execSQL(
            "UPDATE expenses SET isPaid = 0 WHERE isRecurring = 1 AND isPaid = 1"
        )
    }
}

private fun createQuotesTable(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `quotes` (
            `id` TEXT NOT NULL,
            `quoteNumber` TEXT NOT NULL,
            `clientName` TEXT NOT NULL,
            `clientContactId` TEXT DEFAULT NULL,
            `totalAmount` REAL NOT NULL,
            `issueDate` TEXT NOT NULL,
            `validUntil` TEXT NOT NULL,
            `createdDate` TEXT NOT NULL,
            `entrepriseId` TEXT NOT NULL DEFAULT '',
            `category` TEXT NOT NULL DEFAULT 'OTHER',
            `categoryLabel` TEXT NOT NULL DEFAULT '',
            `status` TEXT NOT NULL DEFAULT 'DRAFT',
            `amountExclTax` REAL,
            `tvaRate` REAL NOT NULL DEFAULT 0,
            `otherTaxRate` REAL NOT NULL DEFAULT 0,
            `otherTaxLabel` TEXT NOT NULL DEFAULT '',
            `lineItemsJson` TEXT NOT NULL DEFAULT '[]',
            `convertedInvoiceId` TEXT DEFAULT NULL,
            `notes` TEXT NOT NULL DEFAULT '',
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_quotes_entrepriseId` ON `quotes` (`entrepriseId`)"
    )
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

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0 && cursor.getString(nameIndex) == column) return true
        }
    }
    return false
}

private fun SupportSQLiteDatabase.addTextColumnIfMissing(
    table: String,
    column: String,
    definition: String
) {
    if (!hasColumn(table, column)) {
        execSQL("ALTER TABLE `$table` ADD COLUMN `$column` TEXT $definition")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `products`")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `balance_corrections` (
                `id` TEXT NOT NULL,
                `entrepriseId` TEXT NOT NULL,
                `bankAccountId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `oldBalance` REAL NOT NULL,
                `newBalance` REAL NOT NULL,
                `correctionDate` TEXT NOT NULL,
                `motif` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `userName` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_balance_corrections_entrepriseId` ON `balance_corrections` (`entrepriseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_balance_corrections_bankAccountId` ON `balance_corrections` (`bankAccountId`)")
    }
}

/** Supprime les paiements orphelins (facture supprimée sans cascade). */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM payments WHERE invoiceId NOT IN (SELECT id FROM invoices)")
    }
}
