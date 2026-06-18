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
        WHERE entrepriseId = :entrepriseId OR entrepriseId = ''
        ORDER BY dueDate DESC
        """
    )
    fun observeInvoices(entrepriseId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun observePayments(): Flow<List<PaymentEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE entrepriseId = :entrepriseId OR entrepriseId = ''
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

    @Query("DELETE FROM invoices WHERE id = :invoiceId")
    suspend fun deleteInvoiceById(invoiceId: String)

    @Query("SELECT * FROM entreprises WHERE id = :id LIMIT 1")
    suspend fun findEntrepriseById(id: String): EntrepriseEntity?

    @Query(
        """
        SELECT * FROM invoices
        WHERE entrepriseId = :entrepriseId OR entrepriseId = ''
        """
    )
    suspend fun getInvoicesForBackup(entrepriseId: String): List<InvoiceEntity>

    @Query("SELECT * FROM payments WHERE invoiceId IN (:invoiceIds)")
    suspend fun getPaymentsForInvoices(invoiceIds: List<String>): List<PaymentEntity>

    @Query(
        """
        SELECT * FROM expenses
        WHERE entrepriseId = :entrepriseId OR entrepriseId = ''
        """
    )
    suspend fun getExpensesForBackup(entrepriseId: String): List<ExpenseEntity>

    @Query("SELECT * FROM users WHERE entrepriseId = :entrepriseId")
    suspend fun getUsersForBackup(entrepriseId: String): List<UserEntity>
}
