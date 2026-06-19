package com.abccash.app.locale

import androidx.annotation.StringRes
import com.abccash.app.R

enum class AppLanguage(
    val tag: String?,
    @StringRes val labelRes: Int
) {
    SYSTEM(null, R.string.language_system_default),
    ENGLISH("en", R.string.language_en),
    FRENCH("fr", R.string.language_fr),
    SPANISH("es", R.string.language_es),
    GERMAN("de", R.string.language_de),
    PORTUGUESE("pt", R.string.language_pt),
    ITALIAN("it", R.string.language_it),
    SWEDISH("sv", R.string.language_sv),
    NORWEGIAN("nb", R.string.language_nb),
    DANISH("da", R.string.language_da),
    TURKISH("tr", R.string.language_tr);

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.find { it.tag == tag } ?: SYSTEM
    }
}
