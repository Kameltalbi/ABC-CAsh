# ABC Cash - Guide d'architecture

## 📦 Description

Application Android native en Kotlin avec Jetpack Compose pour la gestion de trésorerie et recouvrement (factures, paiements, dépenses, prévisions).

## 🏗️ Architecture

**MVVM (Model-View-ViewModel) avec Room + Sync serveur**

- **Model** : Entités de données (Invoice, Payment, Expense, User, Entreprise)
- **View** : Composables UI (Screens)
- **ViewModel** : TreasuryViewModel pour la logique métier
- **Repository** : TreasuryRepository pour l'accès aux données
- **Local** : Room Database (TreasuryDatabase)
- **Remote** : TreasuryApiClient + TreasurySyncService

## 📂 Structure des fichiers

```
app/src/main/java/com/abccash/app/treasury/
├── data/                           # Entités de domaine
│   ├── Invoice.kt                  # Facture
│   ├── Payment.kt                  # Paiement
│   ├── Expense.kt                  # Dépense
│   ├── User.kt                     # Utilisateur
│   ├── Entreprise.kt               # Entreprise
│   ├── UserRole.kt                 # Rôles (ADMIN, STAFF)
│   ├── UserPermission.kt           # Permissions
│   └── ...
├── local/                          # Persistance locale
│   ├── TreasuryDatabase.kt         # Room Database (version 8)
│   ├── TreasuryDao.kt              # DAO
│   ├── TreasuryEntities.kt         # Entités Room
│   └── TreasuryMigrations.kt       # Migrations
├── remote/                         # API serveur
│   ├── TreasuryApiClient.kt        # Client Ktor
│   ├── TreasurySyncService.kt      # Sync pull/push
│   └── ApiDtos.kt                  # DTOs API
├── repository/                     # Couche repository
│   └── TreasuryRepository.kt       # Logique d'accès aux données
├── viewmodel/                      # ViewModels
│   ├── TreasuryViewModel.kt        # ViewModel principal
│   └── LoginViewModel.kt           # ViewModel login
├── ui/                             # Écrans Compose
│   ├── LoginScreen.kt
│   ├── InvoicesListScreen.kt
│   ├── PaymentEntryScreen.kt
│   ├── ExpensesManagementScreen.kt
│   ├── TreasuryBalanceScreen.kt
│   └── ...
├── datastore/                      # Préférences
│   ├── UserPreferences.kt          # Session utilisateur
│   └── AppSettings.kt              # Configuration app
└── TreasuryApp.kt                  # Navigation principale
```

## 🔐 Gestion des rôles et permissions

### Rôles
- **ADMIN** : Accès complet (factures, dépenses, trésorerie, utilisateurs)
- **STAFF** : Accès limité (factures, paiements uniquement)

### Permissions
Les permissions sont définies dans `UserPermission.kt` et vérifiées via `hasPermission()`.

## 🔄 Synchronisation

La sync se fait via `TreasurySyncService` :
- **Pull** : Récupération des données serveur
- **Push** : Envoi des modifications locales
- **Auto-sync** : Lancée après chaque modification et au retour de l'app

## � Build

### Configuration locale
Ajouter dans `local.properties` (gitignore) :
```properties
ABC_CASH_RELEASE_STORE_FILE=abc-cash-release.keystore
ABC_CASH_RELEASE_STORE_PASSWORD=votre_mot_de_passe
ABC_CASH_RELEASE_KEY_ALIAS=abc_cash
ABC_CASH_RELEASE_KEY_PASSWORD=votre_mot_de_passe
```

### Commandes
```bash
./gradlew assembleDebug      # Build debug
./gradlew assembleRelease    # Build release (avec minification)
./gradlew test               # Tests unitaires
```

## ⚠️ Notes importantes

1. **Base de données** : Version 8 avec migrations configurées
2. **API** : HTTPS activé (configurable via UserPreferences)
3. **Sécurité** : Mots de passe hashés avec PasswordHasher
4. **Validation** : Validations côté repository (montants > 0, champs obligatoires)
5. **Minification** : Activée en release (R8/ProGuard)
