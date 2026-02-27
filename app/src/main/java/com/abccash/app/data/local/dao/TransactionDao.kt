package com.abccash.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.abccash.app.data.local.entity.TransactionEntity
import com.abccash.app.domain.model.MonthlyForecastRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE id = :seriesRootId OR parentRecurringId = :seriesRootId")
    suspend fun deleteSeriesByRootId(seriesRootId: Long)

    @Query(
        """
        SELECT * FROM transactions
        WHERE currency = :currency
        ORDER BY date ASC, id ASC
    """
    )
    fun observeByCurrency(currency: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE currency = :currency
        ORDER BY date ASC, id ASC
    """
    )
    suspend fun getByCurrency(currency: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date ASC, id ASC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        """
        UPDATE transactions
        SET status = 'VALIDEE', updatedAt = :updatedAt
        WHERE id = :id
    """
    )
    suspend fun validateTransaction(id: Long, updatedAt: String)

    @Query(
        """
        UPDATE transactions
        SET status = 'EN_RETARD', updatedAt = :updatedAt
        WHERE status = 'PLANIFIEE' AND date < :today
    """
    )
    suspend fun markOverdue(today: String, updatedAt: String)

    @Query(
        """
        UPDATE transactions
        SET amountMinor = :amountMinor,
            date = CASE
                WHEN :deltaDays = 0 THEN date
                ELSE date(date, (CASE WHEN :deltaDays > 0 THEN '+' ELSE '' END) || :deltaDays || ' days')
            END,
            updatedAt = :updatedAt
        WHERE id = :seriesRootId OR parentRecurringId = :seriesRootId
    """
    )
    suspend fun editRecurringSeriesAmountAndDateShift(
        seriesRootId: Long,
        amountMinor: Long,
        deltaDays: Int,
        updatedAt: String
    )

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query(
        """
        SELECT
            strftime('%Y-%m', date) AS yearMonth,
            SUM(CASE WHEN type = 'INCOME' THEN amountMinor ELSE 0 END) AS incomeMinor,
            SUM(CASE WHEN type = 'EXPENSE' THEN amountMinor ELSE 0 END) AS expenseMinor,
            SUM(CASE WHEN type = 'INCOME' THEN amountMinor ELSE -amountMinor END) AS netMinor
        FROM transactions
        WHERE currency = :currency
          AND date >= :startDate
          AND date <= :endDate
        GROUP BY strftime('%Y-%m', date)
        ORDER BY yearMonth ASC
    """
    )
    suspend fun getMonthlyForecast(
        currency: String,
        startDate: String,
        endDate: String
    ): List<MonthlyForecastRow>
}
