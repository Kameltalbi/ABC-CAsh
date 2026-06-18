package com.abccash.app.treasury.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class InvoiceImportParserTest {

    @Test
    fun parseCsv_withEnglishHeaders() {
        val csv = """
            invoiceNumber,clientName,totalAmount,dueDate
            FAC-001,Client ABC,1500.000,30/06/2026
        """.trimIndent()
        val result = InvoiceImportParser.parse("test.csv", ByteArrayInputStream(csv.toByteArray()), null)
        assertEquals(1, result.invoices.size)
        assertEquals("FAC-001", result.invoices.first().invoiceNumber)
    }

    @Test
    fun parseCsv_withFrenchHeaders() {
        val csv = """
            N° Facture;Client;Montant;Date échéance
            FAC-002;Société XYZ;2 500,500;15/07/2026
        """.trimIndent()
        val result = InvoiceImportParser.parse("test.csv", ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)), null)
        assertEquals(1, result.invoices.size)
        assertEquals("Société XYZ", result.invoices.first().clientName)
    }

    @Test
    fun parseXlsx_detectedByMagicBytes_evenWithoutExtension() {
        val csv = "not xlsx"
        val result = InvoiceImportParser.parse(
            fileName = "document",
            inputStream = ByteArrayInputStream(csv.toByteArray()),
            mimeType = null
        )
        assertTrue(result.invoices.isEmpty())
    }

    @Test
    fun parseCsv_usesDueDateColumnWhenInvoiceDateAlsoPresent() {
        val csv = """
            N° Facture;Client;Montant;Date;Date échéance
            FAC-001;Client A;1000;01/01/2026;30/06/2026
            FAC-002;Client B;2000;01/01/2026;15/08/2026
        """.trimIndent()
        val result = InvoiceImportParser.parse("test.csv", ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)), null)
        assertEquals(2, result.invoices.size)
        assertEquals(6, result.invoices[0].dueDate.monthValue)
        assertEquals(8, result.invoices[1].dueDate.monthValue)
    }

    @Test
    fun parseCsv_unknownHeaders_returnsHelpfulError() {
        val csv = """
            colonne1,colonne2,colonne3,colonne4
            a,b,c,d
        """.trimIndent()
        val result = InvoiceImportParser.parse("test.csv", ByteArrayInputStream(csv.toByteArray()), null)
        assertTrue(result.invoices.isEmpty())
        assertTrue(result.errorMessage!!.contains("Colonnes non reconnues"))
    }
}
