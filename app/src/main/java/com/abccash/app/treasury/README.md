# Application de Trésorerie et Recouvrement

## 📱 Description

Application Android native en Kotlin avec Jetpack Compose pour le suivi de trésorerie et de recouvrement. L'application gère deux rôles d'utilisateurs : **ADMIN** et **STAFF**.

## 🏗️ Architecture

**MVVM (Model-View-ViewModel)**
- **Model** : Entités de données (Invoice, Payment, Expense)
- **View** : Composables UI (Screens)
- **ViewModel** : TreasuryViewModel pour la logique métier

## 📂 Structure des fichiers

```
treasury/
├── data/
│   ├── UserRole.kt              # Enum des rôles (ADMIN, STAFF)
│   ├── InvoiceStatus.kt         # Statuts des factures
│   ├── PaymentMethod.kt         # Modes de règlement
│   ├── Invoice.kt               # Entité Facture
│   ├── Payment.kt               # Entité Paiement
│   └── Expense.kt               # Entité Dépense
├── viewmodel/
│   └── TreasuryViewModel.kt     # ViewModel principal
├── ui/
│   ├── InvoicesListScreen.kt    # Écran 1 - Liste des factures
│   ├── PaymentEntryScreen.kt    # Écran 2 - Saisie de paiement
│   ├── ExpensesManagementScreen.kt  # Écran 3 - Gestion dépenses (ADMIN)
│   └── TreasuryBalanceScreen.kt # Écran 4 - Solde trésorerie (ADMIN)
├── TreasuryApp.kt               # Navigation et composition principale
└── TreasuryMainActivity.kt      # Point d'entrée de l'application
```

## 🎯 Fonctionnalités par écran

### 📋 Écran 1 : Liste des Factures (Tous les utilisateurs)
- Barre de recherche par client ou numéro de facture
- Filtres rapides : Toutes, Dues, Partielles, Soldées
- Cartes affichant :
  - Nom du client et numéro de facture
  - Reste à payer avec badge coloré de statut
  - Barre de progression du paiement
  - Date d'échéance
- Bouton flottant pour ajouter une facture

### 💰 Écran 2 : Saisie de Paiement (Tous les utilisateurs)
- Résumé de la facture (total, payé, reste)
- Formulaire de saisie :
  - Montant de l'avance
  - Date de paiement (DatePicker)
  - Mode de règlement (Espèces, Virement, Chèque)
- Historique des versements
- Calcul automatique du statut après paiement

### 💸 Écran 3 : Gestion des Dépenses (ADMIN uniquement)
- **Contrôle d'accès** : Message "Accès refusé" pour STAFF
- Formulaire d'ajout :
  - Libellé de la dépense
  - Montant
  - Date
  - Case "Dépense récurrente"
  - Switch "Déjà payée" / "À venir"
- Liste des dépenses du mois avec indicateurs visuels

### 📊 Écran 4 : Solde de Trésorerie (ADMIN uniquement)
- **Contrôle d'accès** : Message "Accès refusé" pour STAFF
- Sélecteur de mois (navigation gauche/droite)
- 3 blocs principaux :
  1. **Total Encaissé** (vert)
  2. **Total Dépenses** (rouge)
  3. **Solde du Mois** (vert si positif, rouge si négatif)
- Solde prévisionnel incluant factures dues et dépenses à venir
- Détails du calcul

## 🔐 Gestion des rôles

### ADMIN
- Accès à tous les écrans
- Peut gérer les dépenses
- Peut consulter la trésorerie

### STAFF
- Accès aux factures uniquement
- Peut enregistrer des paiements
- **Bloqué** sur les écrans Dépenses et Trésorerie

## 🎨 Design

- **Material Design 3** avec Jetpack Compose
- Couleurs adaptées au contexte financier
- Formatage des montants en Dinars Tunisiens (DT) avec millimes
- Interface intuitive et professionnelle

## 🚀 Utilisation

### Changer le rôle de l'utilisateur

```kotlin
val viewModel: TreasuryViewModel = viewModel()
viewModel.setUserRole(UserRole.ADMIN)  // ou UserRole.STAFF
```

### Ajouter une facture

```kotlin
viewModel.addInvoice(
    invoiceNumber = "FAC-2026-001",
    clientName = "Société ABC",
    totalAmount = 15000.0,
    dueDate = LocalDate.now().plusDays(30)
)
```

### Enregistrer un paiement

```kotlin
viewModel.addPayment(
    invoiceId = "invoice-id",
    amount = 5000.0,
    date = LocalDate.now(),
    method = PaymentMethod.TRANSFER
)
```

### Ajouter une dépense (ADMIN uniquement)

```kotlin
viewModel.addExpense(
    label = "Loyer bureau",
    amount = 3500.0,
    date = LocalDate.now(),
    isRecurring = true,
    isPaid = true
)
```

## 📱 Navigation

La navigation utilise **Jetpack Navigation Compose** avec une **BottomNavigationBar** qui s'adapte au rôle :

- **STAFF** : Voit uniquement l'onglet "Factures"
- **ADMIN** : Voit "Factures", "Dépenses" et "Trésorerie"

## 🔧 Dépendances requises

```gradle
dependencies {
    // Jetpack Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.0")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")
    
    // Activity Compose
    implementation("androidx.activity:activity-compose:1.7.0")
}
```

## 💡 Points clés

1. **Sécurité** : Contrôle d'accès strict sur les écrans ADMIN
2. **UX** : Formatage professionnel des montants
3. **Réactivité** : StateFlow pour la gestion d'état
4. **Navigation** : BottomBar adaptatif selon le rôle
5. **Calculs** : Automatiques pour les soldes et prévisions

## 📝 Notes

- Les montants sont formatés en Dinars Tunisiens (DT) avec 3 décimales pour les millimes
- Les dates utilisent le format français (dd/MM/yyyy)
- L'application inclut des données d'exemple pour la démonstration
- Le ViewModel gère automatiquement les calculs de solde et prévisions
