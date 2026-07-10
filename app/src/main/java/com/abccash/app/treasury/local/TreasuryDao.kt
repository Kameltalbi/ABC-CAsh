package com.abccash.app.treasury.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TreasuryDao {
    @Query(
        """
        SELECT * FROM invoices
        WHERE entrepriseId = :entrepriseId
        ORDER BY dueDate DESC
        """
    )
    fun observeInvoices(entrepriseId: String): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT p.* FROM payments p
        INNER JOIN invoices i ON p.invoiceId = i.id
        WHERE i.entrepriseId = :entrepriseId
        ORDER BY p.date DESC
        """
    )
    fun observePayments(entrepriseId: String): Flow<List<PaymentEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE entrepriseId = :entrepriseId
        ORDER BY date DESC
        """
    )
    fun observeExpenses(entrepriseId: String): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM users
        WHERE entrepriseId = :entrepriseId
        ORDER BY dateInscription DESC
        """
    )
    fun observeUsers(entrepriseId: String): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("SELECT * FROM users ORDER BY dateInscription ASC LIMIT 1")
    suspend fun findFirstUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntreprise(entreprise: EntrepriseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE lower(email) = lower(:email) LIMIT 1")
    suspend fun findUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE telephone = :telephone LIMIT 1")
    suspend fun findUserByTelephone(telephone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun findUserById(userId: String): UserEntity?

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("SELECT invoiceNumber FROM invoices WHERE entrepriseId = :entrepriseId AND documentStatus = 'VALIDATED'")
    suspend fun getValidatedInvoiceNumbers(entrepriseId: String): List<String>

    @Query(
        """
        SELECT * FROM quotes
        WHERE entrepriseId = :entrepriseId
        ORDER BY issueDate DESC
        """
    )
    fun observeQuotes(entrepriseId: String): Flow<List<QuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuote(quote: QuoteEntity)

    @Query("SELECT quoteNumber FROM quotes WHERE entrepriseId = :entrepriseId AND status != 'DRAFT'")
    suspend fun getIssuedQuoteNumbers(entrepriseId: String): List<String>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun findQuoteById(id: String): QuoteEntity?

    @Query(
        """
        SELECT * FROM quotes
        WHERE entrepriseId = :entrepriseId
          AND lower(trim(quoteNumber)) = lower(trim(:quoteNumber))
        LIMIT 1
        """
    )
    suspend fun findQuoteByNumber(entrepriseId: String, quoteNumber: String): QuoteEntity?

    @Query("DELETE FROM quotes WHERE id = :quoteId")
    suspend fun deleteQuoteById(quoteId: String)

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun findInvoiceById(id: String): InvoiceEntity?

    @Query("DELETE FROM payments WHERE invoiceId = :invoiceId")
    suspend fun deletePaymentsForInvoice(invoiceId: String)

    @Query("DELETE FROM payments WHERE invoiceId NOT IN (SELECT id FROM invoices)")
    suspend fun deleteOrphanPayments()

    @Query("DELETE FROM invoices WHERE id = :invoiceId")
    suspend fun deleteInvoiceById(invoiceId: String)

    @Query(
        """
        SELECT * FROM invoices
        WHERE entrepriseId = :entrepriseId
          AND lower(trim(invoiceNumber)) = lower(trim(:invoiceNumber))
        LIMIT 1
        """
    )
    suspend fun findInvoiceByNumber(entrepriseId: String, invoiceNumber: String): InvoiceEntity?

    @Query("SELECT * FROM entreprises WHERE id = :id LIMIT 1")
    fun observeEntreprise(id: String): Flow<EntrepriseEntity?>

    @Query("SELECT * FROM entreprises WHERE id = :id LIMIT 1")
    suspend fun findEntrepriseById(id: String): EntrepriseEntity?

    @Query(
        """
        SELECT * FROM invoices
        WHERE entrepriseId = :entrepriseId
        """
    )
    suspend fun getInvoicesForBackup(entrepriseId: String): List<InvoiceEntity>

    @Query("SELECT * FROM payments WHERE invoiceId IN (:invoiceIds)")
    suspend fun getPaymentsForInvoices(invoiceIds: List<String>): List<PaymentEntity>

    @Query(
        """
        SELECT * FROM expenses
        WHERE entrepriseId = :entrepriseId
        """
    )
    suspend fun getExpensesForBackup(entrepriseId: String): List<ExpenseEntity>

    @Query("SELECT * FROM users WHERE entrepriseId = :entrepriseId")
    suspend fun getUsersForBackup(entrepriseId: String): List<UserEntity>

    @Query(
        """
        SELECT * FROM bank_accounts
        WHERE entrepriseId = :entrepriseId
        ORDER BY isDefault DESC, name COLLATE NOCASE ASC
        """
    )
    fun observeBankAccounts(entrepriseId: String): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun findBankAccountById(id: String): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE entrepriseId = :entrepriseId AND isDefault = 1 AND kind = 'BANK' LIMIT 1")
    suspend fun findDefaultBankAccount(entrepriseId: String): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE entrepriseId = :entrepriseId AND isDefault = 1 AND kind = :kind LIMIT 1")
    suspend fun findDefaultAccountByKind(entrepriseId: String, kind: String): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBankAccount(account: BankAccountEntity)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deleteBankAccountById(id: String)

    @Query("UPDATE bank_accounts SET isDefault = 0 WHERE entrepriseId = :entrepriseId AND kind = :kind")
    suspend fun clearDefaultAccountsForKind(entrepriseId: String, kind: String)

    @Deprecated("Use clearDefaultAccountsForKind")
    @Query("UPDATE bank_accounts SET isDefault = 0 WHERE entrepriseId = :entrepriseId")
    suspend fun clearDefaultBankAccounts(entrepriseId: String)

    @Query(
        """
        SELECT * FROM bank_accounts
        WHERE entrepriseId = :entrepriseId
        """
    )
    suspend fun getBankAccountsForBackup(entrepriseId: String): List<BankAccountEntity>

    @Query(
        """
        SELECT * FROM contacts
        WHERE entrepriseId = :entrepriseId
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeContacts(entrepriseId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun findContactById(id: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: String)

    @Query(
        """
        SELECT * FROM contacts
        WHERE entrepriseId = :entrepriseId
        """
    )
    suspend fun getContactsForBackup(entrepriseId: String): List<ContactEntity>

    @Query("DELETE FROM payments WHERE invoiceId IN (SELECT id FROM invoices WHERE entrepriseId = :entrepriseId)")
    suspend fun deletePaymentsForEntreprise(entrepriseId: String)

    @Query("DELETE FROM invoices WHERE entrepriseId = :entrepriseId")
    suspend fun deleteInvoicesForEntreprise(entrepriseId: String)

    @Query("DELETE FROM expenses WHERE entrepriseId = :entrepriseId")
    suspend fun deleteExpensesForEntreprise(entrepriseId: String)

    @Query("DELETE FROM quotes WHERE entrepriseId = :entrepriseId")
    suspend fun deleteQuotesForEntreprise(entrepriseId: String)

    @Query("DELETE FROM bank_accounts WHERE entrepriseId = :entrepriseId")
    suspend fun deleteBankAccountsForEntreprise(entrepriseId: String)

    @Query("DELETE FROM contacts WHERE entrepriseId = :entrepriseId")
    suspend fun deleteContactsForEntreprise(entrepriseId: String)

    @Query("DELETE FROM users WHERE entrepriseId = :entrepriseId")
    suspend fun deleteUsersForEntreprise(entrepriseId: String)

    @Query("DELETE FROM entreprises WHERE id = :entrepriseId")
    suspend fun deleteEntrepriseById(entrepriseId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBalanceCorrection(correction: BalanceCorrectionEntity)

    @Query("""
        SELECT * FROM balance_corrections
        WHERE entrepriseId = :entrepriseId
        ORDER BY correctionDate DESC, createdAt DESC
    """)
    fun observeBalanceCorrections(entrepriseId: String): Flow<List<BalanceCorrectionEntity>>

    @Query("""
        SELECT * FROM balance_corrections
        WHERE entrepriseId = :entrepriseId AND type = 'INITIAL'
        ORDER BY createdAt ASC LIMIT 1
    """)
    suspend fun findInitialBalance(entrepriseId: String): BalanceCorrectionEntity?

    @Query("""
        SELECT * FROM balance_corrections
        WHERE entrepriseId = :entrepriseId
        ORDER BY correctionDate DESC, createdAt DESC LIMIT 1
    """)
    suspend fun findLatestCorrection(entrepriseId: String): BalanceCorrectionEntity?

    @Query("DELETE FROM balance_corrections WHERE entrepriseId = :entrepriseId")
    suspend fun deleteCorrectionsForEntreprise(entrepriseId: String)
}
