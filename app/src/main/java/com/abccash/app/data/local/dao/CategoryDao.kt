package com.abccash.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abccash.app.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Query("SELECT * FROM categories ORDER BY type ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories WHERE name = :name AND type = :type")
    suspend fun deleteByNameAndType(name: String, type: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
