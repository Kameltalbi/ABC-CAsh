package com.abccash.app.treasury.entitlement

import com.abccash.app.BuildConfig
import com.abccash.app.treasury.data.SubscriptionPlan

/**
 * Phase commerciale de l'application.
 *
 * [LAUNCH_FREE] : phase actuelle — 100 % gratuit (objectif ~5 000 téléchargements).
 * [FREEMIUM] : futur Free + Pro (non activé tant que [BuildConfig.LAUNCH_FREE_PHASE] est true).
 */
enum class CommercialPhase {
    LAUNCH_FREE,
    FREEMIUM
}

/**
 * Capacités envisagées pour un futur plan Pro.
 * Catalogue d'architecture uniquement : aucun gate métier actif pendant [CommercialPhase.LAUNCH_FREE].
 * Ne pas brancher ces flags pour retirer une fonction déjà proposée aux premiers utilisateurs.
 */
enum class FutureProCapability {
    ADVANCED_MULTI_COMPANY,
    CLOUD_BACKUP,
    MULTI_DEVICE_SYNC,
    ADVANCED_EXPORTS,
    PDF_REPORTS,
    ADVANCED_ANALYTICS,
    /** Prévisions intelligentes / ABC Predict */
    SMART_FORECASTS
}

/**
 * Point unique pour la politique d'accès commerciale.
 * La logique Pilotage / Trésorerie / Dashboard ne doit pas dépendre des détails billing.
 */
object FeatureAccess {
    val phase: CommercialPhase
        get() = if (BuildConfig.LAUNCH_FREE_PHASE) {
            CommercialPhase.LAUNCH_FREE
        } else {
            CommercialPhase.FREEMIUM
        }

    val isLaunchFreePhase: Boolean
        get() = phase == CommercialPhase.LAUNCH_FREE

    /** Paywall, achats Play et quotas : désactivés en phase de lancement. */
    fun enforcesPaidLimits(): Boolean = phase == CommercialPhase.FREEMIUM

    fun allowsBillingPurchases(): Boolean = enforcesPaidLimits()

    fun allowsOcr(plan: SubscriptionPlan): Boolean =
        isLaunchFreePhase || plan == SubscriptionPlan.PRO

    /**
     * Gate futur pour les capacités Pro listées ci-dessus.
     * En lancement : ne retire rien (toujours true) pour ne pas casser l'existant
     * lorsque ces capacités seront branchées progressivement.
     */
    fun isFutureProCapabilityUnlocked(
        capability: FutureProCapability,
        plan: SubscriptionPlan
    ): Boolean {
        if (isLaunchFreePhase) return true
        return plan == SubscriptionPlan.PRO
    }
}
