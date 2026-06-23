package com.abccash.app.treasury.data

enum class OtherTaxMode {
    PERCENTAGE,
    ABSOLUTE;

    companion object {
        fun fromName(value: String?): OtherTaxMode =
            entries.find { it.name == value } ?: PERCENTAGE
    }
}
