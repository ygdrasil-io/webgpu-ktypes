# 📋 Processus de PR - Port Naga → Kotlin

Ce document décrit le **processus standardisé** pour contribuer au port de Naga vers Kotlin.
Toutes les PRs doivent suivre ces conventions pour maintenir la cohérence du projet.

---

## 🔄 Workflow Standard

### 1️⃣ **Création de branche**

Pour chaque phase ou feature significative, créer une branche dédiée :

```bash
# Depuis la branche principale du port
git checkout feature/naga-port

# Créer une branche pour la phase N
git checkout -b feature/naga-phase{N}

# Ou pour une sous-tâche
 git checkout -b feature/naga-phase{N}-{sous-tache}
```

**Nomenclature des branches** :
- `feature/naga-phase{N}` - Phase principale (ex: `feature/naga-phase1`)
- `feature/naga-phase{N}-{composant}` - Sous-composant (ex: `feature/naga-phase2-lexer`)
- `fix/naga-{problème}` - Corrections (ex: `fix/naga-arena-iterator`)

---

### 2️⃣ **Conventions de commits**

#### Format des messages

```
<icône> [Phase N] <description concise>

<corps optionnel>

Refs: #<issue>, <fichier Rust source>
```

**Icônes standardisées** :

| Icône | Utilisation | Exemple |
|-------|-------------|---------|
| `✅` | Ajout/complétion | `✅ [Phase 1] Implémenter Span et Diagnostic` |
| `🔧` | Correction technique | `🔧 [Phase 1] Corriger iterator() dans Arena` |
| `⚠️` | Travail en cours | `⚠️ [Phase 2] Lexer WGSL (partiel)` |
| `📝` | Documentation | `📝 [Phase 1] Documenter Arena system` |
| `🧪` | Tests | `🧪 [Phase 1] Ajouter tests Span` |
| `🗑️` | Suppression | `🗑️ Supprimer code obsolète` |

#### Bonnes pratiques

- **Commits atomiques** : 1 commit = 1 changement logique
- **Messages en français** (langue du projet)
- **Référencer la source Rust** quand applicable :
  ```
  Refs: /Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/lexer.rs
  ```
- **Lier aux issues/PRs** si existantes

---

## 📝 Template de Pull Request

### ⚠️ **NE PAS MODIFIER LE TITRE/TEMPLATE SANS RAISON**

Copier-coller ce template pour chaque PR et **remplir toutes les sections**.

---

```markdown
### ✅ Phase {N}: {Titre de la phase}

---

## 📋 Résumé

[Description concise de ce que cette PR accompli]

**Phase** : [N]
**Statut** : ✅ Complète | ⚠️ Partielle | 🔧 En cours
**Module** : [wgsl:core | wgsl:msl | wgsl:hlsl | etc.]

---

## 🎯 Objectifs atteints

- [x] Objectif 1
- [x] Objectif 2
- [ ] Objectif 3 (si partiel)

---

## 📦 Changements

### ✅ Fichiers créés
| Fichier | Description | Lignes |
|--------|-------------|--------|
| `wgsl/core/src/.../Nouveau.kt` | Description | +XXX |

### 🔧 Fichiers modifiés
| Fichier | Changement | Justification |
|--------|------------|---------------|
| `Arena.kt` | `iterator()` → `MutableIterator<T>` | Compatibilité MutableCollection |

### 🗑️ Fichiers supprimés
| Fichier | Raison |
|--------|--------|
| `Ancien.kt` | Remplacé par Nouveau.kt |

---

## 🔧 Détails techniques

### Corrections de compilation

[Lister les corrections avec explications]

```kotlin
// Avant (erreur)
fun iterator(): Iterator<T> = data.iterator()

// Après (correction)
override fun iterator(): MutableIterator<T> = data.iterator()
```

### Décisions d'architecture

[Expliquer les choix techniques importants]

---

## 📊 Métriques

| Métrique | Valeur |
|----------|--------|
| Fichiers modifiés | X |
| Fichiers créés | Y |
| Fichiers supprimés | Z |
| Lignes ajoutées | +A |
| Lignes supprimées | -B |
| Tests ajoutés | C |
| Tests passants | D |

---

## ✅ Vérification

### Compilation
```bash
./gradlew compileKotlinJvm  # ✅ SUCCESS | ❌ FAILED
```

### Tests
```bash
# Module spécifique
./gradlew :wgsl:core:jvmTest      # ✅ 36/36 passants

# Tous les modules
./gradlew jvmTest                 # ✅ XXX/XXX passants
```

### Validation manuelle
- [ ] Fonctionnalité X testée manuellement
- [ ] Compatible avec les golden files Rust

---

## 📚 Documentation mise à jour

- [x] `.plan/SUMMARY.md`
- [x] `.plan/01_FONDATIONS/99_checklist.md`
- [ ] Autre : _______________

---

## 🎯 Prochaine étape

[Indiquer ce qui reste à faire]

**Priorité suivante** : [Phase N+1 | Sous-tâche]
**Fichier à consulter** : `/Users/chaos/RustroverProjects/wgpu/naga/src/...`

---

## 🔗 Références

- **Source Rust** : [chemin absolu]
- **Documentation Naga** : [lien]
- **Issue liée** : #[numéro]
- **PR dépendante** : #[numéro]
```

---

## 📋 Checklist Pré-PR

Avant de créer une PR, vérifier :

### ✅ Code
- [ ] `./gradlew compileKotlinJvm` passe
- [ ] `./gradlew compileKotlinJs` passe (si applicable)
- [ ] `./gradlew compileKotlinNative` passe (si applicable)
- [ ] `./gradlew updateKotlinAbi` lancé et changes commités (si ABI modifiée)
- [ ] Pas d'avertissements du compilateur
- [ ] Pas de `TODO` ou `FIXME` non justifiés

### ✅ Tests
- [ ] `./gradlew jvmTest` passe
- [ ] Nouveaux tests ajoutés pour les nouvelles fonctionnalités
- [ ] Tests existants mis à jour si nécessaire
- [ ] Couverture de test > 80% pour les nouveaux modules

### ✅ Documentation
- [ ] Fichiers documentés avec KDoc (`/** ... */`)
- [ ] `.plan/SUMMARY.md` mis à jour
- [ ] Checklist de phase mise à jour
- [ ] Références Rust documentées

### ✅ Conventions
- [ ] Nomenclature Kotlin respectée
- [ ] Imports triés et groupés
- [ ] Pas de code commenté
- [ ] Pas de `println`/debug dans le code final

---

## 🛠️ Commandes Utiles

### Création de PR
```bash
# Pousser la branche
git push fork feature/naga-phase{N}

# Créer PR avec gh CLI
gh pr create \
  --repo ygdrasil-io/webgpu-ktypes \
  --base feature/naga-port \
  --head feature/naga-phase{N} \
  --title "✅ [Phase N] Description" \
  --body-file .plan/pr_body_{N}.md
```

### Vérification locale
```bash
# Compilation complète
./gradlew clean build

# Mise à jour du référentiel ABI (OBLIGATOIRE si signatures modifiées)
./gradlew updateKotlinAbi
# ⚠️  Si des fichiers sont modifiés par cette commande, les commiter :
#     git add .
#     git commit -m "🔧 [Phase N] Mettre à jour référentiel ABI"

# Tests avec couverture
./gradlew jvmTest --info

# Vérifier les dépendances
./gradlew dependencies
```

### Mise à jour du plan
```bash
# Marquer une tâche comme complète dans SUMMARY.md
sed -i 's/- \[ \]/- [x]/g' .plan/SUMMARY.md
# Puis commiter
 git add .plan/SUMMARY.md
 git commit -m "📝 [Phase N] Mettre à jour le plan"
```

---

## 📊 Phases et Branches

| Phase | Branche | PR Target | Durée estimée |
|-------|---------|-----------|---------------|
| Phase 0 | `feature/naga-port` | - | 1 semaine |
| Phase 1 | `feature/naga-phase1` | `feature/naga-port` | 4-6 semaines |
| Phase 2 | `feature/naga-phase2` | `feature/naga-port` | 6-8 semaines |
| Phase 3 | `feature/naga-phase3` | `feature/naga-port` | 4-6 semaines |
| ... | ... | ... | ... |

**Règle** : Toutes les PRs de phase doivent **targeter** `feature/naga-port`.
Une fois toutes les phases complètes, une PR finale sera créée vers `master`.

---

## 🚨 Règles Importantes

### ❌ À NE PAS FAIRE
- Ne **pas** commiter directement sur `feature/naga-port`
- Ne **pas** merger une PR sans review
- Ne **pas** merger une PR sans tests passants
- Ne **pas** utiliser `force push` sur les branches partagées
- Ne **pas** laisser de code commenté (`// ...`)

### ✅ À TOUJOURS FAIRE
- **Rebaser** sur `feature/naga-port` avant de créer une PR
- **Squasher** les commits mineures (`fix typo`, etc.)
- **Vérifier** que la CI passe avant de merger
- **Documenter** toutes les décisions non-triviales
- **Lier** aux fichiers sources Rust

---

## 📞 Support

En cas de doute sur le processus :
1. Consulter ce document
2. Vérifier les PRs existantes pour des exemples
3. Demander clarification dans le chat de l'équipe

---

*Dernière mise à jour : {date}*
*Version : 1.0*
