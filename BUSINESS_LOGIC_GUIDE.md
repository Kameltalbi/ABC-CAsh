# ABC-Cash - Guide d'utilisation de la logique métier

## 📦 Contenu copié depuis MoneyOne

Toute la logique métier de gestion de transactions, catégories, comptes et récurrences a été copiée de MoneyOne vers ABC-Cash.

### Structure des fichiers

```
app/src/main/java/com/abccash/app/
├── data/
│   ├── entity/
│   │   ├── Transaction.kt          # Entité transaction
│   │   ├── Category.kt             # Entité catégorie
│   │   ├── RecurringTransaction.kt # Entité récurrence
│   │   ├── Account.kt              # Entité compte
│   │   ├── Budget.kt               # Entité budget
│   │   ├── SavingsGoal.kt          # Entité objectif d'épargne
│   │   ├── TransactionType.kt      # Enum type (INCOME/EXPENSE/TRANSFER)
│   │   └── Frequency.kt            # Enum fréquence (DAILY/WEEKLY/MONTHLY/YEARLY)
│   ├── dao/
│   │   ├── TransactionDao.kt       # DAO transactions
│   │   ├── CategoryDao.kt          # DAO catégories
│   │   ├── RecurringDao.kt         # DAO récurrences
│   │   ├── AccountDao.kt           # DAO comptes
│   │   ├── BudgetDao.kt            # DAO budgets
│   │   └── SavingsGoalDao.kt       # DAO objectifs
│   ├── repository/
│   │   ├── TransactionRepository.kt
│   │   ├── CategoryRepository.kt
│   │   ├── RecurringTransactionRepository.kt
│   │   ├── AccountRepository.kt
│   │   ├── BudgetRepository.kt
│   │   └── SavingsGoalRepository.kt
│   ├── ABCCashDatabase.kt          # Configuration Room Database
│   ├── RecurringGenerator.kt       # Générateur de transactions récurrentes
│   └── Converters.kt               # Convertisseurs Room
├── ui/
│   ├── viewmodel/
│   │   ├── TransactionViewModel.kt # ViewModel pour transactions
│   │   ├── MainViewModel.kt        # ViewModel principal
│   │   └── CategoryViewModel.kt    # ViewModel catégories
│   └── util/
│       ├── DateUtils.kt            # Utilitaires dates
│       └── CurrencyFormatter.kt    # Formatage devises
└── ABCCashApp.kt                   # Application class

```

## 🚀 Configuration

### 1. Déclarer l'Application dans AndroidManifest.xml

```xml
<application
    android:name=".ABCCashApp"
    ...>
```

### 2. Utiliser les ViewModels dans vos écrans

```kotlin
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ABCCashApp
                TransactionViewModel(
                    app.transactionRepository,
                    app.accountRepository,
                    app.categoryRepository,
                    app.recurringRepository,
                    app
                )
            }
        }
    )
) {
    // Votre UI ici
}
```

## 💡 Exemples d'utilisation

### Créer une transaction simple

```kotlin
viewModel.updateName("Courses")
viewModel.updateAmount("50.00")
viewModel.updateType(TransactionType.EXPENSE)
viewModel.updateCategoryId(categoryId)
viewModel.saveTransaction {
    // Transaction sauvegardée
}
```

### Créer une transaction récurrente

```kotlin
viewModel.updateName("Loyer")
viewModel.updateAmount("800.00")
viewModel.updateType(TransactionType.EXPENSE)
viewModel.updateFrequency(Frequency.MONTHLY)
viewModel.updateFrequencyInterval(1) // Tous les 1 mois
viewModel.saveTransaction {
    // Transaction récurrente créée
}
```

### Modifier une transaction récurrente

#### Modifier uniquement une occurrence
```kotlin
viewModel.setRecurringEditMode(RecurringEditMode.SINGLE)
viewModel.modifySingleOccurrence {
    // Occurrence modifiée
}
```

#### Modifier toutes les occurrences futures
```kotlin
viewModel.setRecurringEditMode(RecurringEditMode.FUTURE)
viewModel.modifyFutureOccurrences {
    // Occurrences futures modifiées
}
```

#### Modifier toute la série
```kotlin
viewModel.setRecurringEditMode(RecurringEditMode.ALL)
viewModel.modifyEntireSeries {
    // Toute la série modifiée
}
```

### Récupérer les transactions

```kotlin
// Dans votre ViewModel ou composable
val transactions by viewModel.transactions.collectAsState()

// Transactions filtrées par mois
val monthTransactions by viewModel.monthTransactions.collectAsState()
```

### Créer des catégories

```kotlin
val category = Category(
    name = "Alimentation",
    icon = "restaurant",
    color = 0xFF4CAF50.toInt(),
    type = TransactionType.EXPENSE,
    userId = "user_id"
)
categoryRepository.insert(category)
```

## 🔄 Génération automatique des récurrences

Le `RecurringGenerator` génère automatiquement les occurrences des transactions récurrentes.

```kotlin
// Dans votre Application class (déjà configuré dans ABCCashApp)
val recurringGenerator = RecurringGenerator(transactionRepository, recurringRepository)

// Générer les occurrences pour le mois en cours
viewModelScope.launch {
    val currentMonth = YearMonth.now()
    recurringGenerator.generateUpToMonth(recurringTransaction, currentMonth)
}
```

## 📊 Calculs de solde

```kotlin
// Solde total
val balance = income - expenses

// Solde à une date donnée
val balanceAtDate = transactionRepository.getTotalIncome(userId, accountId, 0L, dateMillis)
    .combine(transactionRepository.getTotalExpenses(userId, accountId, 0L, dateMillis)) 
    { income, expenses -> income - expenses }
```

## 🎨 Formatage des devises

```kotlin
// Initialiser dans Application.onCreate() (déjà fait dans ABCCashApp)
CurrencyFormatter.init(context)

// Formater un montant
val formatted = CurrencyFormatter.format(123.45) // "123,45 €"

// Formater avec signe
val signed = CurrencyFormatter.formatSigned(-50.0) // "-50,00 €"

// Format compact
val compact = CurrencyFormatter.formatCompact(1500.0) // "1.5k"
```

## 📅 Utilitaires de dates

```kotlin
// Convertir LocalDate en epoch millis
val millis = DateUtils.toEpochMillis(LocalDate.now())

// Convertir epoch millis en LocalDate
val date = DateUtils.fromEpochMillis(millis)

// Formater une date
val formatted = DateUtils.formatDate(LocalDate.now()) // "02 mars 2026"

// Début et fin de mois
val monthStart = DateUtils.monthStart(YearMonth.now())
val monthEnd = DateUtils.monthEnd(YearMonth.now())
```

## 🔐 Support multi-utilisateurs

Toutes les entités ont un champ `userId` pour supporter plusieurs utilisateurs.

```kotlin
// Récupérer les transactions d'un utilisateur
transactionRepository.getAllTransactions(userId, accountId)

// Créer une transaction pour un utilisateur
val transaction = Transaction(
    name = "Test",
    amount = 100.0,
    type = TransactionType.EXPENSE,
    userId = "user_123",
    ...
)
```

## ⚠️ Notes importantes

1. **Migrations de base de données** : La base de données est en version 10 avec toutes les migrations configurées.

2. **Transactions récurrentes** : Les occurrences sont générées automatiquement. Ne pas les créer manuellement.

3. **Soft delete** : Les transactions ont un flag `isDeleted` pour la suppression logique.

4. **Modifications** : Les occurrences modifiées ont un flag `isModified` pour les distinguer.

5. **Transferts** : Les transactions de type `TRANSFER` ont un `destinationAccountId`.

## 🎯 Prochaines étapes

1. Créer vos écrans UI en Compose
2. Utiliser les ViewModels fournis
3. Personnaliser les catégories par défaut
4. Ajouter vos propres fonctionnalités

Toute la logique métier est prête à l'emploi ! 🚀
