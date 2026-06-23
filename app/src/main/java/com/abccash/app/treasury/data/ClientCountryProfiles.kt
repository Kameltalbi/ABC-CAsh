package com.abccash.app.treasury.data

enum class TaxIdType {
    EU_VAT,
    FR_SIRET,
    FR_SIREN,
    TN_MATRICULE,
    TN_TVA,
    GENERIC_TAX_ID,
    COMPANY_REGISTRATION,
    NONE
}

enum class TaxIdValidationStatus {
    UNVERIFIED,
    VALID_FORMAT,
    INVALID_FORMAT
}

data class ClientCountryOption(
    val code: String,
    val labelFr: String,
    val labelEn: String
)

data class TaxIdTypeOption(
    val type: TaxIdType,
    val labelFr: String,
    val labelEn: String,
    val hintFr: String,
    val hintEn: String,
    val placeholderFr: String,
    val placeholderEn: String
)

object ClientCountryProfiles {

    val countries: List<ClientCountryOption> = listOf(
        ClientCountryOption("FR", "France", "France"),
        ClientCountryOption("TN", "Tunisie", "Tunisia"),
        ClientCountryOption("BE", "Belgique", "Belgium"),
        ClientCountryOption("DE", "Allemagne", "Germany"),
        ClientCountryOption("ES", "Espagne", "Spain"),
        ClientCountryOption("IT", "Italie", "Italy"),
        ClientCountryOption("NL", "Pays-Bas", "Netherlands"),
        ClientCountryOption("GB", "Royaume-Uni", "United Kingdom"),
        ClientCountryOption("MA", "Maroc", "Morocco"),
        ClientCountryOption("DZ", "Algérie", "Algeria"),
        ClientCountryOption("SN", "Sénégal", "Senegal"),
        ClientCountryOption("CI", "Côte d'Ivoire", "Ivory Coast"),
        ClientCountryOption("US", "États-Unis", "United States"),
        ClientCountryOption("OTHER", "Autre pays", "Other country")
    )

    fun countryLabel(code: String, french: Boolean = true): String {
        val option = countries.find { it.code == code } ?: countries.last()
        return if (french) option.labelFr else option.labelEn
    }

    fun taxIdOptionsForCountry(countryCode: String): List<TaxIdTypeOption> {
        val euVat = TaxIdTypeOption(
            type = TaxIdType.EU_VAT,
            labelFr = "N° de TVA intracommunautaire",
            labelEn = "EU VAT number",
            hintFr = "Ex. FR12345678901 — obligatoire pour facturer une entreprise UE.",
            hintEn = "E.g. FR12345678901 — required for EU B2B invoicing.",
            placeholderFr = "FR__ _________",
            placeholderEn = "FR__ _________"
        )
        val generic = TaxIdTypeOption(
            type = TaxIdType.GENERIC_TAX_ID,
            labelFr = "Identifiant fiscal",
            labelEn = "Tax identifier",
            hintFr = "Numéro fiscal local ou international.",
            hintEn = "Local or international tax identifier.",
            placeholderFr = "Ex. 123456789",
            placeholderEn = "E.g. 123456789"
        )
        val companyReg = TaxIdTypeOption(
            type = TaxIdType.COMPANY_REGISTRATION,
            labelFr = "N° d'immatriculation (RC, ICE…)",
            labelEn = "Company registration (RC, ICE…)",
            hintFr = "Registre du commerce ou identifiant légal de l'entreprise.",
            hintEn = "Trade register or legal company identifier.",
            placeholderFr = "Ex. RC 12345",
            placeholderEn = "E.g. RC 12345"
        )
        val none = TaxIdTypeOption(
            type = TaxIdType.NONE,
            labelFr = "Pas d'identifiant fiscal",
            labelEn = "No tax identifier",
            hintFr = "Client particulier ou identifiant non connu.",
            hintEn = "Individual client or unknown identifier.",
            placeholderFr = "",
            placeholderEn = ""
        )

        return when (countryCode) {
            "FR" -> listOf(
                TaxIdTypeOption(
                    type = TaxIdType.FR_SIRET,
                    labelFr = "SIRET",
                    labelEn = "SIRET",
                    hintFr = "14 chiffres — identifiant d'établissement en France.",
                    hintEn = "14 digits — French establishment ID.",
                    placeholderFr = "123 456 789 00012",
                    placeholderEn = "123 456 789 00012"
                ),
                TaxIdTypeOption(
                    type = TaxIdType.FR_SIREN,
                    labelFr = "SIREN",
                    labelEn = "SIREN",
                    hintFr = "9 chiffres — identifiant de l'entreprise.",
                    hintEn = "9 digits — French company ID.",
                    placeholderFr = "123 456 789",
                    placeholderEn = "123 456 789"
                ),
                euVat.copy(
                    labelFr = "N° de TVA intracommunautaire",
                    hintFr = "Commence par FR suivi de 11 caractères."
                ),
                none
            )
            "TN" -> listOf(
                TaxIdTypeOption(
                    type = TaxIdType.TN_MATRICULE,
                    labelFr = "Matricule fiscal",
                    labelEn = "Tax registration number",
                    hintFr = "Saisie libre (ex. 06114615/P/A/M/000).",
                    hintEn = "Free entry (e.g. 06114615/P/A/M/000).",
                    placeholderFr = "06114615PAM000",
                    placeholderEn = "06114615PAM000"
                ),
                TaxIdTypeOption(
                    type = TaxIdType.TN_TVA,
                    labelFr = "N° de TVA",
                    labelEn = "VAT number",
                    hintFr = "Saisie libre — même identifiant que le matricule fiscal.",
                    hintEn = "Free entry — same ID as the tax registration number.",
                    placeholderFr = "06114615/P/A/M/000",
                    placeholderEn = "06114615/P/A/M/000"
                ),
                companyReg,
                none
            )
            "BE", "DE", "ES", "IT", "NL", "GB" -> listOf(euVat, companyReg, generic, none)
            "MA", "DZ", "SN", "CI" -> listOf(companyReg, generic, none)
            "US" -> listOf(
                TaxIdTypeOption(
                    type = TaxIdType.GENERIC_TAX_ID,
                    labelFr = "EIN / Tax ID",
                    labelEn = "EIN / Tax ID",
                    hintFr = "Employer Identification Number (9 chiffres).",
                    hintEn = "Employer Identification Number (9 digits).",
                    placeholderFr = "12-3456789",
                    placeholderEn = "12-3456789"
                ),
                none
            )
            else -> listOf(generic, companyReg, euVat, none)
        }
    }

    fun defaultTaxIdType(countryCode: String): TaxIdType =
        taxIdOptionsForCountry(countryCode).first().type

    fun isEuCountry(countryCode: String): Boolean =
        countryCode in setOf("FR", "BE", "DE", "ES", "IT", "NL", "GB")
}
