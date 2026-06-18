package com.abccash.app.treasury.local

import androidx.room.TypeConverter
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import java.time.LocalDate
import java.time.LocalDateTime

class TreasuryConverters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun fromUserRole(value: UserRole?): String? = value?.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole? = value?.let(UserRole::valueOf)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let(PaymentMethod::valueOf)

    @TypeConverter
    fun fromExpenseRecurrence(value: ExpenseRecurrence?): String? = value?.name

    @TypeConverter
    fun toExpenseRecurrence(value: String?): ExpenseRecurrence? = value?.let(ExpenseRecurrence::valueOf)

    @TypeConverter
    fun fromUserPermissions(value: Set<UserPermission>?): String? = value?.joinToString(",") { it.name }

    @TypeConverter
    fun toUserPermissions(value: String?): Set<UserPermission> {
        return value
            ?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { runCatching { UserPermission.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()
    }
}
