package com.abccash.app.treasury.data

enum class DocumentPdfTemplate {
    CLASSIC_BLUE,
    MODERN_GREEN,
    ELEGANT_SLATE;

    companion object {
        fun fromName(value: String?): DocumentPdfTemplate =
            entries.find { it.name == value } ?: CLASSIC_BLUE
    }
}
