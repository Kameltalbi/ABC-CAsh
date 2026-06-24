package com.abccash.app.treasury.local

import androidx.room.TypeConverter
import com.abccash.app.treasury.data.BankAccountSource
import com.abccash.app.treasury.data.ContactType
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.InvoiceDocumentStatus
import com.abccash.app.treasury.data.ProductKind
import com.abccash.app.treasury.data.ProductUnit
import com.abccash.app.treasury.data.TaxIdType
import com.abccash.app.treasury.data.TaxIdValidationStatus
import com.abccash.app.treasury.data.TreasuryAccountKind
import com.abccash.app.treasury.data.OtherTaxMode
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.QuoteStatus
import com.abccash.app.treasury.data.RevenueCategory
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
    fun fromBankAccountSource(value: BankAccountSource?): String? = value?.name

    @TypeConverter
    fun toBankAccountSource(value: String?): BankAccountSource? =
        value?.let { runCatching { BankAccountSource.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let(PaymentMethod::valueOf)

    @TypeConverter
    fun fromExpenseRecurrence(value: ExpenseRecurrence?): String? = value?.name

    @TypeConverter
    fun toExpenseRecurrence(value: String?): ExpenseRecurrence? = value?.let(ExpenseRecurrence::valueOf)

    @TypeConverter
    fun fromRevenueCategory(value: RevenueCategory?): String? = value?.name

    @TypeConverter
    fun toRevenueCategory(value: String?): RevenueCategory? =
        value?.let { runCatching { RevenueCategory.valueOf(it) }.getOrNull() } ?: RevenueCategory.OTHER

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun toExpenseCategory(value: String?): ExpenseCategory? =
        value?.let { runCatching { ExpenseCategory.valueOf(it) }.getOrNull() } ?: ExpenseCategory.OTHER

    @TypeConverter
    fun fromContactType(value: ContactType?): String? = value?.name

    @TypeConverter
    fun toContactType(value: String?): ContactType? =
        value?.let { runCatching { ContactType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromInvoiceDocumentStatus(value: InvoiceDocumentStatus?): String? = value?.name

    @TypeConverter
    fun toInvoiceDocumentStatus(value: String?): InvoiceDocumentStatus? =
        value?.let { runCatching { InvoiceDocumentStatus.valueOf(it) }.getOrNull() }
            ?: InvoiceDocumentStatus.VALIDATED

    @TypeConverter
    fun fromQuoteStatus(value: QuoteStatus?): String? = value?.name

    @TypeConverter
    fun toQuoteStatus(value: String?): QuoteStatus? =
        value?.let { runCatching { QuoteStatus.valueOf(it) }.getOrNull() } ?: QuoteStatus.DRAFT

    @TypeConverter
    fun fromProductKind(value: ProductKind?): String? = value?.name

    @TypeConverter
    fun toProductKind(value: String?): ProductKind? =
        value?.let { runCatching { ProductKind.valueOf(it) }.getOrNull() } ?: ProductKind.SERVICE

    @TypeConverter
    fun fromProductUnit(value: ProductUnit?): String? = value?.name

    @TypeConverter
    fun toProductUnit(value: String?): ProductUnit? =
        value?.let { runCatching { ProductUnit.valueOf(it) }.getOrNull() } ?: ProductUnit.PIECE

    @TypeConverter
    fun fromTaxIdType(value: TaxIdType?): String? = value?.name

    @TypeConverter
    fun toTaxIdType(value: String?): TaxIdType? =
        value?.let { runCatching { TaxIdType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromTaxIdValidationStatus(value: TaxIdValidationStatus?): String? = value?.name

    @TypeConverter
    fun toTaxIdValidationStatus(value: String?): TaxIdValidationStatus? =
        value?.let { runCatching { TaxIdValidationStatus.valueOf(it) }.getOrNull() }
            ?: TaxIdValidationStatus.UNVERIFIED

    @TypeConverter
    fun fromOtherTaxMode(value: OtherTaxMode?): String? = value?.name

    @TypeConverter
    fun toOtherTaxMode(value: String?): OtherTaxMode? =
        value?.let { runCatching { OtherTaxMode.valueOf(it) }.getOrNull() } ?: OtherTaxMode.PERCENTAGE

    @TypeConverter
    fun fromTreasuryAccountKind(value: TreasuryAccountKind?): String? = value?.name

    @TypeConverter
    fun toTreasuryAccountKind(value: String?): TreasuryAccountKind? =
        value?.let { runCatching { TreasuryAccountKind.valueOf(it) }.getOrNull() }
            ?: TreasuryAccountKind.BANK

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
