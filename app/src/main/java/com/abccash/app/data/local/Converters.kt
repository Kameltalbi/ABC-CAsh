package com.abccash.app.data.local

import androidx.room.TypeConverter
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.data.local.entity.RecurrenceRule
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun fromCurrency(value: CurrencyCode?): String? = value?.name

    @TypeConverter
    fun toCurrency(value: String?): CurrencyCode? = value?.let(CurrencyCode::valueOf)

    @TypeConverter
    fun fromType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toType(value: String?): TransactionType? = value?.let(TransactionType::valueOf)

    @TypeConverter
    fun fromStatus(value: TransactionStatus?): String? = value?.name

    @TypeConverter
    fun toStatus(value: String?): TransactionStatus? = value?.let(TransactionStatus::valueOf)

    @TypeConverter
    fun fromRecurrence(value: RecurrenceRule?): String? = value?.name

    @TypeConverter
    fun toRecurrence(value: String?): RecurrenceRule? = value?.let(RecurrenceRule::valueOf)
}
