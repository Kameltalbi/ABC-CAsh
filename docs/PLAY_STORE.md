# Publication Play Store — ABC Cash

## Générer l'AAB (Android App Bundle)

```bash
./gradlew bundleRelease
```

Fichier produit :

`app/build/outputs/bundle/release/app-release.aab`

## Signature release

- Keystore : `app/cashtrack-release.keystore` (local, non versionné)
- Alias : `cashtrack`
- Mot de passe : voir configuration dans `app/build.gradle.kts`

**Important :** conservez le keystore et ses mots de passe. Sans eux, vous ne pourrez pas publier de mises à jour sur le même listing Play Store.

## APK release (installation directe)

```bash
./gradlew assembleRelease
```

Fichier : `app/build/outputs/apk/release/app-release.apk`

## Checklist Play Console

1. Créer une application sur [Google Play Console](https://play.google.com/console)
2. Téléverser l'AAB (`app-release.aab`)
3. Renseigner la fiche store (titre, description, captures d'écran)
4. Publier la [politique de confidentialité](PRIVACY_POLICY.md) sur une URL publique (GitHub Pages, site web) et indiquer l'URL dans la console
5. Déclarer le type d'application : gestion financière / productivité
6. Classifier le contenu (questionnaire IARC)
7. Créer une piste de test interne avant la production

## Identifiants application

| Champ | Valeur |
|-------|--------|
| applicationId | `com.abccash.app` |
| versionName | voir `app/build.gradle.kts` |
| minSdk | 26 |
| targetSdk | 35 |

## Notes

- L'app ne requiert pas de compte Google pour fonctionner
- Pas de collecte de données analytics intégrée
- Les sauvegardes JSON/CSV sont initiées manuellement par l'utilisateur
