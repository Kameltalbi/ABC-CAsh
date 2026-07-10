package com.abccash.app.treasury.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class BankStatementImportParserTest {

    @Test
    fun parse_skipsOpeningBalanceRow() {
        val csv = """
            Date opération;Description;Débit;Crédit
            01/07/2026;SOLDE AU 01/07/2026;;152315,08
            05/07/2026;VIR CLIENT ABC;;500,00
            10/07/2026;PAIEMENT FOURNISSEUR;200,00;
        """.trimIndent()

        val result = BankStatementImportParser.parse(
            "releve-juillet.csv",
            ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        )

        assertEquals(null, result.errorMessage)
        assertEquals(1, result.skippedSummaryRows)
        assertEquals(2, result.entries.size)
        assertEquals(500.0, result.totalCredit, 0.001)
        assertEquals(200.0, result.totalDebit, 0.001)
        assertTrue(result.entries.none { it.label.contains("SOLDE", ignoreCase = true) })
    }

    @Test
    fun parse_skipsOpeningBalanceDebitRow() {
        val csv = """
            Date opération;Description;Débit;Crédit
            01/07/2026;SOLDE REPORT;152315,08;
            08/07/2026;LOYER BUREAU;1200,00;
        """.trimIndent()

        val result = BankStatementImportParser.parse(
            "releve.csv",
            ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        )

        assertEquals(1, result.skippedSummaryRows)
        assertEquals(1, result.entries.size)
        assertEquals(1200.0, result.totalDebit, 0.001)
        assertEquals(0.0, result.totalCredit, 0.001)
    }

    @Test
    fun parse_amountColumn_usesSignWhenSensEmpty() {
        val csv = """
            Date;Libellé;Montant;Sens
            10/07/2026;VIR CLIENT;500,00;
            11/07/2026;PAIEMENT FOURNISSEUR;-200,00;
        """.trimIndent()

        val result = BankStatementImportParser.parse(
            "releve.csv",
            ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        )

        assertEquals(2, result.entries.size)
        assertEquals(500.0, result.totalCredit, 0.001)
        assertEquals(200.0, result.totalDebit, 0.001)
    }

    @Test
    fun isNonTransactionalLabel_detectsBalanceKeywords() {
        assertTrue(BankStatementImportParser.isNonTransactionalLabel("SOLDE AU 01/07/2026"))
        assertTrue(BankStatementImportParser.isNonTransactionalLabel("Report"))
        assertTrue(BankStatementImportParser.isNonTransactionalLabel("TOTAL MOIS"))
        assertTrue(!BankStatementImportParser.isNonTransactionalLabel("VIR SEPA CLIENT ABC"))
        assertTrue(BankStatementImportParser.isNonTransactionalLabel("TOTAL DEBITS MOIS"))
    }
}
