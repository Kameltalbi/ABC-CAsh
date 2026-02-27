package com.abccash.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abccash.app.data.local.entity.BalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(balance: BalanceEntity): Long

    @Query("SELECT * FROM balances WHERE currency = :currency LIMIT 1")
    suspend fun getByCurrency(currency: String): BalanceEntity?

    @Query("SELECT * FROM balances WHERE currency = :currency LIMIT 1")
    fun observeByCurrency(currency: String): Flow<BalanceEntity?>

    @Query("SELECT * FROM balances ORDER BY id ASC")
    suspend fun getAll(): List<BalanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BalanceEntity>)

    @Query("DELETE FROM balances")
    suspend fun deleteAll()
}
