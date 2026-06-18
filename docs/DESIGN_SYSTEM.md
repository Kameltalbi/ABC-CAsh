# 🎨 Design System - ABC Cash

## Palette de Couleurs "Bleu nuit + blanc"

**Rendu** : Sérieux, financier, professionnel, proche des banques modernes

---

## 🎯 Couleurs Principales

| Nom | Hex | Usage | Exemple |
|-----|-----|-------|---------|
| **Primary** | `#0F172A` | Couleur principale - Bleu nuit | Boutons, titres, navigation |
| **Secondary** | `#2563EB` | Couleur secondaire - Bleu royal | Liens, accents, icônes |
| **Success** | `#16A34A` | Succès - Vert | Revenus, paiements reçus, validations |
| **Warning** | `#F59E0B` | Alerte - Orange | Échéances proches, avertissements |
| **Error** | `#DC2626` | Erreur - Rouge | Dépenses, retards, erreurs |

---

## 🖼️ Fonds et Surfaces

| Nom | Hex | Usage |
|-----|-----|-------|
| **Background** | `#F8FAFC` | Fond principal de l'application |
| **Surface** | `#FFFFFF` | Fond des cartes et composants |
| **SurfaceVariant** | `#F1F5F9` | Fond alternatif pour différenciation |

---

## 💼 Couleurs Métier

### Transactions
| Type | Couleur | Hex | Usage |
|------|---------|-----|-------|
| **Revenus** | Vert | `#16A34A` | Encaissements, factures payées |
| **Dépenses** | Rouge | `#DC2626` | Sorties d'argent, factures à payer |
| **En retard** | Orange | `#F59E0B` | Factures échues, alertes |
| **En attente** | Bleu | `#2563EB` | Factures à venir, prévisions |

### Backgrounds Colorés
| Type | Hex | Usage |
|------|-----|-------|
| **SuccessBackground** | `#DCFCE7` | Fond vert clair pour zones de succès |
| **WarningBackground** | `#FEF3C7` | Fond orange clair pour alertes |
| **ErrorBackground** | `#FEE2E2` | Fond rouge clair pour erreurs |
| **InfoBackground** | `#DEEBFF` | Fond bleu clair pour informations |

---

## 📝 Texte

| Niveau | Hex | Usage |
|--------|-----|-------|
| **TextPrimary** | `#0F172A` | Titres, texte principal |
| **TextSecondary** | `#64748B` | Sous-titres, descriptions |
| **TextTertiary** | `#94A3B8` | Texte secondaire, labels |

---

## 🔲 Bordures

| Type | Hex | Usage |
|------|-----|-------|
| **Border** | `#E2E8F0` | Bordures par défaut |
| **BorderFocus** | `#2563EB` | Bordures en focus |

---

## 📊 Catégories

| Catégorie | Hex | Emoji |
|-----------|-----|-------|
| **Revenus** | `#DCFCE7` | 💰 |
| **Transport** | `#DEEBFF` | 🚗 |
| **Santé** | `#FEE2E2` | 🏥 |
| **Logement** | `#FED7AA` | 🏠 |
| **Alimentation** | `#FEF3C7` | 🍴 |
| **Shopping** | `#FCE7F3` | 🛒 |
| **Défaut** | `#F1F5F9` | 📁 |

---

## 🎭 Utilisation dans les Composants

### Boutons

```kotlin
// Bouton principal
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = AppColors.Primary,
        contentColor = Color.White
    )
)

// Bouton succès
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = AppColors.Success,
        contentColor = Color.White
    )
)

// Bouton danger
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = AppColors.Error,
        contentColor = Color.White
    )
)
```

### Cartes

```kotlin
Surface(
    color = AppColors.Surface,
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 2.dp
) {
    // Contenu
}
```

### Texte

```kotlin
// Titre
Text(
    text = "Titre",
    color = AppColors.TextPrimary,
    fontWeight = FontWeight.Bold
)

// Sous-titre
Text(
    text = "Sous-titre",
    color = AppColors.TextSecondary
)

// Label
Text(
    text = "Label",
    color = AppColors.TextTertiary
)
```

---

## 🌓 Mode Sombre

Le mode sombre utilise les mêmes couleurs de base avec des ajustements :
- Background : `#0A0F1C`
- Surface : `#10182A`
- SurfaceVariant : `#1B2640`

---

## 📐 Espacements

| Taille | dp | Usage |
|--------|-----|-------|
| **XS** | 4dp | Espacement minimal |
| **S** | 8dp | Espacement petit |
| **M** | 12dp | Espacement moyen |
| **L** | 16dp | Espacement standard |
| **XL** | 20dp | Espacement large |
| **XXL** | 24dp | Espacement très large |

---

## 🔤 Typographie

| Style | Taille | Poids | Usage |
|-------|--------|-------|-------|
| **Display** | 32sp | Bold | Titres principaux |
| **Headline** | 24sp | Bold | Titres de section |
| **Title** | 20sp | SemiBold | Titres de carte |
| **Body** | 16sp | Regular | Texte principal |
| **Label** | 14sp | Medium | Labels, boutons |
| **Caption** | 12sp | Regular | Texte secondaire |
| **Overline** | 10sp | Medium | Badges, tags |

---

## 🎯 Coins Arrondis

| Taille | dp | Usage |
|--------|-----|-------|
| **Small** | 8dp | Petits composants |
| **Medium** | 12dp | Boutons, chips |
| **Large** | 16dp | Cartes standard |
| **XLarge** | 20dp | Grandes cartes |
| **XXLarge** | 24dp | Cartes premium |

---

## 🌟 Ombres

| Niveau | Élévation | Usage |
|--------|-----------|-------|
| **None** | 0dp | Éléments plats |
| **Low** | 1dp | Éléments légèrement surélevés |
| **Medium** | 2dp | Cartes standard |
| **High** | 4dp | Cartes importantes |
| **VeryHigh** | 8dp | Modals, dialogs |

---

## ✨ Animations

| Type | Durée | Courbe |
|------|-------|--------|
| **Fast** | 150ms | EaseOut |
| **Normal** | 300ms | EaseInOut |
| **Slow** | 500ms | EaseIn |

---

## 🎨 Gradients (KPI Cards)

```kotlin
// Gradient Vert (Revenus)
Brush.linearGradient(
    colors = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
)

// Gradient Rouge (Dépenses)
Brush.linearGradient(
    colors = listOf(Color(0xFFFF512F), Color(0xFFF09819))
)

// Gradient Bleu (Solde)
Brush.linearGradient(
    colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
)

// Gradient Orange (Alertes)
Brush.linearGradient(
    colors = listOf(Color(0xFFFFA726), Color(0xFFFB8C00))
)
```

---

## 📱 Responsive Design

### Breakpoints

| Taille | Largeur | Usage |
|--------|---------|-------|
| **Compact** | < 600dp | Smartphones |
| **Medium** | 600-840dp | Tablettes portrait |
| **Expanded** | > 840dp | Tablettes paysage, desktop |

---

## ♿ Accessibilité

### Contraste Minimum

- **Texte normal** : 4.5:1
- **Texte large** : 3:1
- **Composants UI** : 3:1

### Tailles Tactiles

- **Minimum** : 48dp x 48dp
- **Recommandé** : 56dp x 56dp

---

## 🔍 Exemples d'Application

### Écran Factures
- **Background** : `#F8FAFC`
- **Cartes** : `#FFFFFF` avec ombre 2dp
- **Titre** : `#0F172A` Bold 24sp
- **Montants** : Vert `#16A34A` ou Rouge `#DC2626`
- **Badges** : Fond coloré avec texte assorti

### Écran Trésorerie
- **KPI Cards** : Gradients avec coins 24dp
- **Graphiques** : Couleurs métier
- **Alertes** : Fond `#FEF3C7` avec texte `#F59E0B`

---

**Version** : 1.0  
**Dernière mise à jour** : Juin 2026  
**Application** : ABC Cash - Pilotage Financier
