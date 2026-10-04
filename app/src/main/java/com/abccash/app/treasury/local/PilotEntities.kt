package com.abccash.app.treasury.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abccash.app.treasury.data.PilotCategory
import com.abccash.app.treasury.data.PilotEntry
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.PilotImportRecord
import com.abccash.app.treasury.data.PilotMonthlyTarget
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "pilot_categories",
    indices = [Index(value = ["entrepriseId", "type", "name"], unique = true)]
)
data class PilotCategoryEntity(
    @PrimaryKey val id: String,
    val entrepriseId: String,
    val type: PilotEntryType,
    val name: String,
    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    @ColumnInfo(defaultValue = "0")
    val colorIndex: Int = 0
)

@Entity(
    tableName = "pilot_entries",
    foreignKeys = [
        ForeignKey(
            entity = PilotCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["entrepriseId"]),
        Index(value = ["categoryId"]),
        Index(value = ["importId"])
    ]
)
data class PilotEntryEntity(
    @PrimaryKey val id: String,
    val entrepriseId: String,
    val type: PilotEntryType,
    val date: LocalDate,
    val categoryId: String?,
    val amount: Double,
    @ColumnInfo(defaultValue = "''")
    val note: String = "",
    @ColumnInfo(defaultValue = "0")
    val recurring: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val importId: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    @ColumnInfo(defaultValue = "0")
    val recurrenceMonths: Int = 0,
    val treasuryDate: LocalDate? = null,
    val bankAccountId: String? = null,
    val counterAccountId: String? = null
)

@Entity(
    tableName = "pilot_monthly_targets",
    indices = [Index(value = ["entrepriseId", "year", "month"], unique = true)]
)
data class PilotMonthlyTargetEntity(
    @PrimaryKey val id: String,
    val entrepriseId: String,
    val year: Int,
    val month: Int,
    val salesTarget: Double,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Entity(
    tableName = "pilot_imports",
    indices = [Index(value = ["entrepriseId"])]
)
data class PilotImportEntity(
    @PrimaryKey val id: String,
    val entrepriseId: String,
    val type: PilotEntryType,
    val filename: String,
    val importedRows: Int,
    val ignoredRows: Int,
    val createdAt: LocalDateTime
)

fun PilotCategoryEntity.toDomain() = PilotCategory(
    id = id,
    entrepriseId = entrepriseId,
    type = type,
    name = name,
    active = active,
    colorIndex = colorIndex,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PilotCategory.toEntity() = PilotCategoryEntity(
    id = id,
    entrepriseId = entrepriseId,
    type = type,
    name = name,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
    colorIndex = colorIndex
)

fun PilotEntryEntity.toDomain() = PilotEntry(
    id = id,
    entrepriseId = entrepriseId,
    type = type,
    date = date,
    categoryId = categoryId,
    amount = amount,
    note = note,
    recurring = recurring,
    recurrenceMonths = recurrenceMonths,
    treasuryDate = treasuryDate,
    bankAccountId = bankAccountId,
    counterAccountId = counterAccountId,
    importId = importId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PilotEntry.toEntity() = PilotEntryEntity(
    id = id,
    entrepriseId = entrepriseId,
    type = type,
    date = date,
    categoryId = categoryId,
    amount = amount,
    note = note,
    recurring = recurring,
    importId = importId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    recurrenceMonths = recurrenceMonths,
    treasuryDate = treasuryDate,
    bankAccountId = bankAccountId,
    counterAccountId = counterAccountId
)

fun PilotMonthlyTargetEntity.toDomain() = PilotMonthlyTarget(
    id = id,
    entrepriseId = entrepriseId,
    year = year,
    month = month,
    salesTarget = salesTarget,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PilotMonthlyTarget.toEntity() = PilotMonthlyTargetEntity(
    id = id,
    entrepriseId = entrepriseId,
    year = year,
    month = month,
    salesTarget = salesTarget,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PilotImportEntity.toDomain() = PilotImportRecord(
    id = id,
    entrepriseId = entrepriseId,
    type = type,
    filename = filename,
    importedRows = importedRows,
    ignoredRows = ignoredRows,
    createdAt = createdAt
)

fun PilotImportRecord.toEntity() = PilotImportEntity(
    id = id,
    entrepriseId = entrepriseId,
    type = type,
    filename = filename,
    importedRows = importedRows,
    ignoredRows = ignoredRows,
    createdAt = createdAt
)
