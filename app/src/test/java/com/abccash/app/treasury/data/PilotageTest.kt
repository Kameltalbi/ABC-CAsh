package com.abccash.app.treasury.data

import com.abccash.app.treasury.importer.PilotDuplicateMode
import com.abccash.app.treasury.importer.PilotImportAnalysis
import com.abccash.app.treasury.importer.PilotImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream

class PilotageTest {
    private val orgA = "org-a"
    private val orgB = "org-b"
    private val october = YearMonth.of(2026, 10)

    @Test
    fun totauxVentesChargesResultatCouvertureEtPointZero() {
        val entries = listOf(
            sale(orgA, LocalDate.of(2026, 10, 3), "digital", 25_000.0),
            sale(orgA, LocalDate.of(2026, 10, 8), "conseil", 20_000.0),
            expense(orgA, LocalDate.of(2026, 10, 1), "salaires", 38_200.0)
        )
        val figures = PilotCalculations.monthFigures(entries, orgA, october)
        assertEquals(45_000.0, figures.sales, 0.001)
        assertEquals(38_200.0, figures.charges, 0.001)
        assertEquals(6_800.0, figures.result, 0.001)
        assertEquals(38_200.0, figures.zeroPoint, 0.001)
        assertEquals(117.8, PilotMoney.percent(figures.coverage!!), 0.001)
        assertEquals(PilotCoverageState.ABOVE, PilotCalculations.coverageState(figures))
        assertEquals(6_800.0, PilotCalculations.gapToZero(figures), 0.001)
    }

    @Test
    fun couvertureNulleSiAucuneCharge() {
        val figures = PilotCalculations.monthFigures(
            listOf(sale(orgA, LocalDate.of(2026, 10, 3), "digital", 1_000.0)),
            orgA,
            october
        )
        assertEquals(0.0, figures.charges, 0.001)
        assertNull(figures.coverage)
        assertEquals(PilotCoverageState.NO_CHARGES, PilotCalculations.coverageState(figures))
    }

    @Test
    fun couvertureAnnuelleEstLeRatioDesTotaux() {
        val entries = listOf(
            sale(orgA, LocalDate.of(2026, 1, 10), "digital", 100.0),
            expense(orgA, LocalDate.of(2026, 1, 10), "loyer", 50.0),
            expense(orgA, LocalDate.of(2026, 2, 10), "loyer", 50.0)
        )
        val year = PilotCalculations.yearFigures(entries, orgA, 2026)
        assertEquals(100.0, year.sales, 0.001)
        assertEquals(100.0, year.charges, 0.001)
        assertEquals(0.0, year.result, 0.001)
        assertEquals(100.0, PilotMoney.percent(year.coverage!!), 0.001)
        assertEquals(12, year.months.size)
        assertEquals(0.0, year.months[2].sales, 0.001)
    }

    @Test
    fun changementDeCategorieDeplaceLeMontant() {
        val digital = sale(orgA, LocalDate.of(2026, 10, 3), "digital", 2_500.0)
        val conseil = digital.copy(categoryId = "conseil", amount = 4_000.0, id = "other")
        val categories = listOf(
            category(orgA, PilotEntryType.SALE, "digital", "Digital"),
            category(orgA, PilotEntryType.SALE, "conseil", "Conseil")
        )
        val before = PilotCalculations.salesByCategory(listOf(digital), categories, orgA, october)
        assertEquals("Digital", before.single().name)
        val moved = digital.copy(categoryId = "conseil")
        val after = PilotCalculations.salesByCategory(listOf(moved, conseil), categories, orgA, october)
        assertEquals(1, after.size)
        assertEquals("Conseil", after.single().name)
        assertEquals(6_500.0, after.single().amount, 0.001)
    }

    @Test
    fun modificationEtSuppressionRecalculentLesTotaux() {
        val first = sale(orgA, LocalDate.of(2026, 10, 3), "digital", 2_500.0)
        val second = sale(orgA, LocalDate.of(2026, 10, 5), "digital", 1_000.0, id = "b")
        val modified = listOf(first.copy(amount = 3_000.0), second)
        assertEquals(4_000.0, PilotCalculations.monthFigures(modified, orgA, october).sales, 0.001)
        val deleted = modified.filter { it.id != second.id }
        assertEquals(3_000.0, PilotCalculations.monthFigures(deleted, orgA, october).sales, 0.001)
    }

    @Test
    fun lesDonneesDuneAutreEntrepriseSontIgnorees() {
        val entries = listOf(
            sale(orgA, LocalDate.of(2026, 10, 3), "digital", 2_500.0),
            sale(orgB, LocalDate.of(2026, 10, 3), "digital", 9_000.0, id = "other-org")
        )
        val figures = PilotCalculations.monthFigures(entries, orgA, october)
        assertEquals(2_500.0, figures.sales, 0.001)
        assertEquals(9_000.0, PilotCalculations.monthFigures(entries, orgB, october).sales, 0.001)
    }

    @Test
    fun montantNulOuNegatifEstRefuse() {
        assertTrue(!PilotMoney.isPositive(0.0))
        assertTrue(!PilotMoney.isPositive(-10.0))
        assertEquals(2500.0, PilotMoney.parse("2 500,000")!!, 0.001)
        assertEquals(2500.5, PilotMoney.parse("2.500,50")!!, 0.001)
        assertNull(PilotMoney.parse("abc"))
    }

    @Test
    fun importCsvDetecteLesColonnesEtLesMois() {
        val csv = """
            Date facture;Activité;Montant HT;Commentaire
            03/01/2026;Digital;2500;Janvier
            05/10/2026;Conseil;4000;Octobre
            08/10/2026;;1700;
            abc;Digital;xx;
        """.trimIndent()
        val table = PilotImportParser.read("ventes.csv", ByteArrayInputStream(csv.toByteArray()))
        assertNull(table.errorMessage)
        val mapping = PilotImportParser.suggestMapping(table.headers)
        assertTrue(mapping.isComplete)
        val lines = PilotImportParser.parseLines(table, mapping)
        val preview = PilotImportAnalysis.preview(
            lines = lines,
            entries = emptyList(),
            categories = emptyList(),
            entrepriseId = orgA,
            type = PilotEntryType.SALE,
            duplicateMode = PilotDuplicateMode.IGNORE,
            reviewedIndexes = emptySet()
        )
        assertEquals(2, preview.validLines.size)
        assertEquals(2, preview.invalidCount)
        assertEquals(6_500.0, preview.total, 0.001)
        assertEquals(YearMonth.of(2026, 1), preview.periodStart)
        assertEquals(YearMonth.of(2026, 10), preview.periodEnd)
        assertEquals(listOf("Digital", "Conseil"), preview.newCategoryNames)
    }

    @Test
    fun importExcelEtDoublons() {
        val sheet = """
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="inlineStr"><is><t>Date</t></is></c>
                  <c r="B1" t="inlineStr"><is><t>Catégorie</t></is></c>
                  <c r="C1" t="inlineStr"><is><t>Montant</t></is></c>
                </row>
                <row r="2">
                  <c r="A2" t="inlineStr"><is><t>03/10/2026</t></is></c>
                  <c r="B2" t="inlineStr"><is><t>Digital</t></is></c>
                  <c r="C2"><v>2500</v></c>
                </row>
                <row r="3">
                  <c r="A3" t="inlineStr"><is><t>03/10/2026</t></is></c>
                  <c r="B3" t="inlineStr"><is><t>Digital</t></is></c>
                  <c r="C3"><v>2500</v></c>
                </row>
              </sheetData>
            </worksheet>
        """.trimIndent()
        val table = PilotImportParser.read("ventes.xlsx", ByteArrayInputStream(xlsx(sheet)))
        assertNull(table.errorMessage)
        val lines = PilotImportParser.parseLines(table, PilotImportParser.suggestMapping(table.headers))
        val existing = listOf(sale(orgA, LocalDate.of(2026, 10, 3), "digital", 2_500.0))
        val categories = listOf(category(orgA, PilotEntryType.SALE, "digital", "Digital"))
        val preview = PilotImportAnalysis.preview(
            lines, existing, categories, orgA, PilotEntryType.SALE, PilotDuplicateMode.IGNORE, emptySet()
        )
        assertEquals(2, preview.duplicateIndexes.size)
        assertTrue(preview.linesToImport.isEmpty())
        val forced = PilotImportAnalysis.preview(
            lines, existing, categories, orgA, PilotEntryType.SALE, PilotDuplicateMode.IMPORT, emptySet()
        )
        assertEquals(2, forced.linesToImport.size)
    }

    @Test
    fun fichierVideEtXlsSontRejetes() {
        val empty = PilotImportParser.read("vide.csv", ByteArrayInputStream(ByteArray(0)))
        assertTrue(empty.errorMessage != null)
        val xls = PilotImportParser.read("ancien.xls", ByteArrayInputStream("not".toByteArray(StandardCharsets.UTF_8)))
        assertTrue(xls.errorMessage!!.contains(".xls"))
    }

    @Test
    fun importCsvMultiMoisSansFichierEmbarque() {
        // Pas de fichier privé dans assets : un nouvel utilisateur importe uniquement son propre CSV.
        val csv = """
            Date facture;Activité;Montant HT
            15/01/2026;Abonnements;100
            10/02/2026;Formation;200
            05/03/2026;Événement;50
        """.trimIndent()
        val table = PilotImportParser.read("ventes.csv", ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        assertNull(table.errorMessage)
        val lines = PilotImportParser.parseLines(table, PilotImportParser.suggestMapping(table.headers))
        assertEquals(3, lines.size)
        assertTrue(lines.all { it.isValid })
        assertEquals(setOf(1, 2, 3), lines.map { YearMonth.from(it.date).monthValue }.toSet())
    }

    @Test
    fun chaqueRubriqueRecoitUneCouleurDifferente() {
        assertEquals(0, PilotCalculations.nextRubriqueColor(emptyList()))
        assertEquals(2, PilotCalculations.nextRubriqueColor(listOf(0, 1, 4)))
        val first = category(orgA, PilotEntryType.SALE, "marketing", "Marketing").copy(colorIndex = 0)
        val second = category(orgA, PilotEntryType.SALE, "conseil", "Conseil").copy(colorIndex = 0)
        val expense = category(orgA, PilotEntryType.EXPENSE, "loyer", "Loyer").copy(colorIndex = 0)
        val colors = PilotCalculations.rubriqueColorIndexes(listOf(first, second, expense))
        assertTrue(colors.getValue(first.id) != colors.getValue(second.id))
        assertEquals(0, colors.getValue(expense.id))
    }

    @Test
    fun chargesRecurrentesNeSontPasDupliqueesSiDejaPresentes() {
        val previous = expense(orgA, LocalDate.of(2026, 9, 5), "loyer", 2_500.0, recurring = true)
        val already = expense(orgA, LocalDate.of(2026, 10, 5), "loyer", 2_500.0, recurring = true, id = "already")
        val pending = PilotCalculations.recurringToCopy(listOf(previous, already), orgA, october)
        assertTrue(pending.isEmpty())
        val copies = PilotCalculations.recurringToCopy(listOf(previous), orgA, october)
        assertEquals(1, copies.size)
        val shifted = PilotCalculations.shiftedRecurringCopy(copies.single(), october)
        assertEquals(LocalDate.of(2026, 10, 5), shifted.date)
        assertTrue(shifted.id != previous.id)
    }

    @Test
    fun recurrenceSuitLIntervalleChoisi() {
        val quarterly = expense(orgA, LocalDate.of(2026, 1, 10), "loyer", 1_200.0, recurrenceMonths = 3, id = "trim")
        assertTrue(PilotCalculations.recurringToCopy(listOf(quarterly), orgA, YearMonth.of(2026, 2)).isEmpty())
        assertEquals(1, PilotCalculations.recurringToCopy(listOf(quarterly), orgA, YearMonth.of(2026, 4)).size)
        val everyTwo = expense(orgA, LocalDate.of(2026, 8, 4), "logiciel", 600.0, recurrenceMonths = 2, id = "deux")
        val everyFour = expense(orgA, LocalDate.of(2026, 6, 11), "transport", 500.0, recurrenceMonths = 4, id = "quatre")
        val semester = expense(orgA, LocalDate.of(2026, 4, 3), "assurance", 800.0, recurrenceMonths = 6, id = "sem")
        val annual = expense(orgA, LocalDate.of(2025, 10, 9), "licence", 900.0, recurrenceMonths = 12, id = "an")
        assertEquals(1, PilotCalculations.recurringToCopy(listOf(everyTwo), orgA, october).size)
        assertEquals(1, PilotCalculations.recurringToCopy(listOf(everyFour), orgA, october).size)
        assertEquals(1, PilotCalculations.recurringToCopy(listOf(semester), orgA, october).size)
        assertEquals(1, PilotCalculations.recurringToCopy(listOf(annual), orgA, october).size)
        val copied = PilotCalculations.shiftedRecurringCopy(quarterly, YearMonth.of(2026, 4))
        assertEquals(3, copied.recurrenceMonths)
        assertEquals(LocalDate.of(2026, 4, 10), copied.date)
        val sale = sale(orgA, LocalDate.of(2026, 7, 8), "conseil", 1_500.0).copy(recurring = true, recurrenceMonths = 3)
        assertTrue(PilotCalculations.recurringToCopy(listOf(sale), orgA, YearMonth.of(2026, 8)).isEmpty())
        assertEquals(1, PilotCalculations.recurringToCopy(listOf(sale), orgA, october).size)
    }

    @Test
    fun venteNouvelleCompteDansLeMoisEtDansLaTresorerieALaDateDEncaissement() {
        val sale = movement(
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 10, 4),
            amount = 10_000.0,
            categoryId = "abonnements",
            treasuryDate = LocalDate.of(2026, 11, 15),
            bankAccountId = "biat"
        )
        val figures = PilotCalculations.monthFigures(listOf(sale), orgA, october)
        val cash = PilotCash.items(sale)

        assertEquals(10_000.0, figures.sales, 0.001)
        assertEquals(10_000.0, figures.result, 0.001)
        assertEquals(1, cash.size)
        assertEquals(LocalDate.of(2026, 11, 15), cash.first().date)
        assertEquals(10_000.0, cash.first().signedAmount, 0.001)
        assertEquals("biat", cash.first().bankAccountId)
        assertEquals(CashSourceType.PILOTAGE, cash.first().sourceType)
    }

    @Test
    fun venteHistoriqueGardeLeResultatSansCreerDeCash() {
        val historical = movement(
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 1, 12),
            amount = 4_200.0,
            categoryId = "magazine"
        )
        val figures = PilotCalculations.monthFigures(listOf(historical), orgA, YearMonth.of(2026, 1))

        assertNull(historical.treasuryDate)
        assertNull(historical.bankAccountId)
        assertEquals(4_200.0, figures.sales, 0.001)
        assertEquals(4_200.0, figures.result, 0.001)
        // Sans compte, toujours hors cash (même si la date d'encaissement retombe sur la date de saisie).
        assertTrue(PilotCash.items(historical).isEmpty())
    }

    @Test
    fun venteSansDateEncaissementUtiliseLaDateDeSaisiePourLeCash() {
        val sale = movement(
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 3, 18),
            amount = 1_500.0,
            categoryId = "digital",
            bankAccountId = "biat"
        )
        assertNull(sale.treasuryDate)
        assertEquals(LocalDate.of(2026, 3, 18), PilotRules.effectiveCollectionDate(sale))
        val filled = PilotRules.withDefaultCollection(sale)
        assertEquals(LocalDate.of(2026, 3, 18), filled.treasuryDate)
        val cash = PilotCash.items(sale)
        assertEquals(1, cash.size)
        assertEquals(LocalDate.of(2026, 3, 18), cash.first().date)
        assertEquals(1_500.0, cash.first().signedAmount, 0.001)
    }

    @Test
    fun depenseReduitLeResultatEtLeCompteALaMemeDate() {
        val expense = movement(
            type = PilotEntryType.EXPENSE,
            date = LocalDate.of(2026, 10, 10),
            amount = 2_500.0,
            categoryId = "loyer",
            bankAccountId = "biat"
        )
        val figures = PilotCalculations.monthFigures(listOf(expense), orgA, october)
        val cash = PilotCash.items(expense)

        assertEquals(2_500.0, figures.charges, 0.001)
        assertEquals(-2_500.0, figures.result, 0.001)
        assertEquals(-2_500.0, cash.first().signedAmount, 0.001)
        assertEquals(LocalDate.of(2026, 10, 10), cash.first().date)
        assertEquals("biat", cash.first().bankAccountId)
    }

    @Test
    fun autresMouvementsEtTransfertNeTouchentPasLeResultat() {
        val credit = movement(
            type = PilotEntryType.OTHER_INFLOW,
            date = LocalDate.of(2026, 10, 6),
            amount = 50_000.0,
            categoryId = "credit",
            bankAccountId = "biat"
        )
        val outflow = movement(
            type = PilotEntryType.OTHER_OUTFLOW,
            date = LocalDate.of(2026, 10, 8),
            amount = 5_000.0,
            categoryId = "retrait",
            bankAccountId = "biat"
        )
        val transfer = movement(
            type = PilotEntryType.TRANSFER,
            date = LocalDate.of(2026, 10, 9),
            amount = 10_000.0,
            bankAccountId = "biat",
            counterAccountId = "stb"
        )
        val entries = listOf(credit, outflow, transfer)
        val figures = PilotCalculations.monthFigures(entries, orgA, october)

        assertEquals(0.0, figures.sales, 0.001)
        assertEquals(0.0, figures.charges, 0.001)
        assertEquals(0.0, figures.result, 0.001)
        assertEquals(50_000.0, PilotCash.signedOn(PilotCash.items(credit), "biat"), 0.001)
        assertEquals(-5_000.0, PilotCash.signedOn(PilotCash.items(outflow), "biat"), 0.001)
        assertEquals(-10_000.0, PilotCash.signedOn(PilotCash.items(transfer), "biat"), 0.001)
        assertEquals(10_000.0, PilotCash.signedOn(PilotCash.items(transfer), "stb"), 0.001)
        assertEquals(0.0, PilotMoney.sum(PilotCash.items(transfer).map { it.signedAmount }), 0.001)
    }

    @Test
    fun modifierUneLigneRecalculeLeCashSansLeDupliquer() {
        val original = movement(
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 10, 4),
            amount = 10_000.0,
            categoryId = "abonnements",
            treasuryDate = LocalDate.of(2026, 11, 15),
            bankAccountId = "biat",
            id = "sale-1"
        )
        val edited = original.copy(
            amount = 8_000.0,
            treasuryDate = LocalDate.of(2026, 12, 1),
            bankAccountId = "stb"
        )
        val cash = PilotCash.items(edited)

        assertEquals(1, cash.size)
        assertEquals("sale-1", cash.first().sourceId)
        assertEquals(8_000.0, cash.first().signedAmount, 0.001)
        assertEquals("stb", cash.first().bankAccountId)
        assertEquals(LocalDate.of(2026, 12, 1), cash.first().date)
        assertEquals(0.0, PilotCash.signedOn(cash, "biat"), 0.001)
    }

    @Test
    fun supprimerUneLigneRetireSonCash() {
        val sale = movement(
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 10, 4),
            amount = 10_000.0,
            categoryId = "abonnements",
            treasuryDate = LocalDate.of(2026, 11, 15),
            bankAccountId = "biat",
            id = "sale-1"
        )
        val remaining = listOf(sale).filterNot { it.id == "sale-1" }

        assertTrue(PilotCash.items(remaining).isEmpty())
    }

    @Test
    fun refuseMontantNulNegatifTransfertIdentiqueEtCompteDuneAutreEntreprise() {
        val base = movement(
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 10, 4),
            amount = 10.0,
            categoryId = "abonnements",
            treasuryDate = LocalDate.of(2026, 11, 15),
            bankAccountId = "biat"
        )
        assertEquals(PilotCodes.AMOUNT, PilotRules.validate(base.copy(amount = 0.0), "org-a", PilotEntryType.SALE, "org-a", strictCash = true))
        assertEquals(PilotCodes.AMOUNT, PilotRules.validate(base.copy(amount = -10.0), "org-a", PilotEntryType.SALE, "org-a", strictCash = true))
        assertEquals(
            PilotCodes.TRANSFER,
            PilotRules.validate(
                movement(
                    type = PilotEntryType.TRANSFER,
                    date = LocalDate.of(2026, 10, 9),
                    amount = 10.0,
                    bankAccountId = "biat",
                    counterAccountId = "biat"
                ),
                sourceEntrepriseId = orgA,
                destinationEntrepriseId = orgA
            )
        )
        assertEquals(
            PilotCodes.TENANT,
            PilotRules.validate(
                base,
                categoryEntrepriseId = orgA,
                categoryType = PilotEntryType.SALE,
                sourceEntrepriseId = orgB,
                strictCash = true
            )
        )
        assertNull(PilotEntryType.fromStored("INCONNU"))
        assertEquals(PilotEntryType.OTHER_INFLOW, PilotEntryType.fromStored("OTHER_INFLOW"))
    }

    @Test
    fun unSeulCompteEstProposeEtLeCompteParDefautSinon() {
        val biat = BankAccount(id = "biat", entrepriseId = orgA, name = "BIAT", isDefault = true)
        val stb = BankAccount(id = "stb", entrepriseId = orgA, name = "STB")
        val other = BankAccount(id = "uib", entrepriseId = orgB, name = "UIB", isDefault = true)

        assertEquals("biat", PilotRules.suggestedAccount(listOf(biat, other), orgA)?.id)
        assertEquals("biat", PilotRules.suggestedAccount(listOf(biat, stb, other), orgA)?.id)
        assertEquals("stb", PilotRules.suggestedAccount(listOf(stb, other), orgA)?.id)
        val bh = BankAccount(id = "bh", entrepriseId = orgA, name = "BH")
        assertNull(PilotRules.suggestedAccount(listOf(stb, bh, other), orgA))
    }

    @Test
    fun uneVenteHistoriqueSansEncaissementResteHorsCash() {
        val historical = movement(
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 1, 12),
            amount = 4_200.0,
            categoryId = "magazine"
        )
        assertEquals(
            false,
            PilotRules.requiresStrictCash(historical, PilotEntryType.SALE, null, null)
        )
        assertNull(
            PilotRules.validate(
                historical,
                categoryEntrepriseId = orgA,
                categoryType = PilotEntryType.SALE,
                strictCash = false
            )
        )
        assertTrue(PilotCash.items(historical).isEmpty())
    }

    @Test
    fun uneNouvelleVenteSansComptePossibleSiAucunCompteNExiste() {
        val sale = movement(
            type = PilotEntryType.SALE,
            date = LocalDate.of(2026, 10, 4),
            amount = 10_000.0,
            categoryId = "abonnements",
            treasuryDate = LocalDate.of(2026, 11, 15)
        )
        assertNull(
            PilotRules.validate(
                sale,
                categoryEntrepriseId = orgA,
                categoryType = PilotEntryType.SALE,
                strictCash = true,
                requireAccount = false
            )
        )
        assertEquals(
            PilotCodes.ACCOUNT,
            PilotRules.validate(
                sale,
                categoryEntrepriseId = orgA,
                categoryType = PilotEntryType.SALE,
                strictCash = true,
                requireAccount = true
            )
        )
    }

    @Test
    fun leCashLegacyResteDistinctDuneLignePilotage() {
        val legacy = PilotCash.legacyPayment(orgA, "pay-1", LocalDate.of(2026, 1, 20), 4_200.0, "biat", "Facture")
        val pilot = PilotCash.items(
            movement(
                type = PilotEntryType.SALE,
                date = LocalDate.of(2026, 1, 12),
                amount = 4_200.0,
                categoryId = "magazine"
            )
        )

        assertEquals(CashSourceType.LEGACY_PAYMENT, legacy.sourceType)
        assertEquals("pay-1", legacy.sourceId)
        assertTrue(pilot.isEmpty())
    }

    private fun movement(
        type: PilotEntryType,
        date: LocalDate,
        amount: Double,
        categoryId: String? = null,
        treasuryDate: LocalDate? = null,
        bankAccountId: String? = null,
        counterAccountId: String? = null,
        id: String = "$type-$date-$amount"
    ) = PilotEntry(
        id = id,
        entrepriseId = orgA,
        type = type,
        date = date,
        categoryId = categoryId,
        amount = amount,
        treasuryDate = treasuryDate,
        bankAccountId = bankAccountId,
        counterAccountId = counterAccountId
    )

    private fun sale(
        org: String,
        date: LocalDate,
        categoryId: String,
        amount: Double,
        id: String = "sale-$categoryId-$date"
    ) = PilotEntry(
        id = id,
        entrepriseId = org,
        type = PilotEntryType.SALE,
        date = date,
        categoryId = categoryId,
        amount = amount
    )

    private fun expense(
        org: String,
        date: LocalDate,
        categoryId: String,
        amount: Double,
        recurring: Boolean = false,
        recurrenceMonths: Int = 0,
        id: String = "expense-$categoryId-$date"
    ) = PilotEntry(
        id = id,
        entrepriseId = org,
        type = PilotEntryType.EXPENSE,
        date = date,
        categoryId = categoryId,
        amount = amount,
        recurring = recurring || recurrenceMonths > 0,
        recurrenceMonths = recurrenceMonths
    )

    private fun category(org: String, type: PilotEntryType, id: String, name: String) = PilotCategory(
        id = id,
        entrepriseId = org,
        type = type,
        name = name
    )

    private fun xlsx(sheetXml: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}
