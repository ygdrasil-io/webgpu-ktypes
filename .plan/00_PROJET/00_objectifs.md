# 🎯 Objectifs du Projet : Naga → Kotlin

**Projet** : WebGPU-KTypes Shader Transpiler  
**Code** : `naga-kt`  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

---

## 📋 CONTEXTE

Naga est le traducteur et validateur de shaders utilisé par **wgpu** (implémentation Rust de WebGPU).  
Le projet **WebGPU-KTypes** vise à fournir une implémentation Kotlin de Naga pour :

1. **Intégration native** avec les écosystèmes Kotlin/Java/Android
2. **Interopérabilité** avec les APIs WebGPU existantes
3. **Alternative** à l'utilisation de FFI (JNI) pour appeler Naga Rust depuis Kotlin

---

## ✅ OBJECTIFS PRINCIPAUX (MUST HAVE)

### 1. Core IR Fonctionnel
- [ ] Implémenter la **Représentation Intermédiaire (IR)** complète
- [ ] Supporter tous les types WGSL : scalaires, vecteurs, matrices, structs, arrays, pointers
- [ ] Supporter toutes les expressions WGSL (60+ variants)
- [ ] Supporter toutes les instructions WGSL (25+ variants)
- [ ] Gérer les Modules, Functions, EntryPoints, GlobalVariables

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs`

### 2. Parser WGSL → IR
- [ ] Parser **100% de la syntaxe WGSL** (spécification officielle)
- [ ] Générer un **IR équivalent** à celui de Naga Rust
- [ ] Gérer les **erreur de parsing** avec des messages clairs
- [ ] Supporter les **directives diagnostiques** (`@diagnostic`)
- [ ] Supporter les **doc comments**

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/`

### 3. Backend MSL (Metal Shading Language)
- [ ] Générer du **MSL valide** pour Metal (Apple platforms)
- [ ] Supporter tous les **shader stages** (vertex, fragment, compute)
- [ ] Gérer les **conversions de types** Kotlin → MSL
- [ ] Optimiser le **code généré** (minimal, lisible)

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/msl/`

### 4. Backend HLSL (High-Level Shading Language)
- [ ] Générer du **HLSL valide** pour DirectX 11/12
- [ ] Supporter **Shader Model 5.0+**
- [ ] Gérer les **spécificités HLSL** (registers, semantics)
- [ ] Optimiser pour **DXC/FXC**

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/`

### 5. Validation Sémantique
- [ ] Valider la **cohérence des types** dans le module IR
- [ ] Valider les **expressions** (opérandes compatibles)
- [ ] Valider les **statements** (control flow valide)
- [ ] Valider les **entry points** (IO valide, capabilities)
- [ ] Intégrer les **golden files Rust** pour validation

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/`

---

## 🟡 OBJECTIFS SECONDAIRES (SHOULD HAVE)

### 1. Backend GLSL
- [ ] Générer du **GLSL 330+/ES 300+**
- [ ] Gérer les **profiles** (core, es, compatibility)
- [ ] Supporter les **extensions GLSL**

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/`

### 2. Backend WGSL
- [ ] Générer du **WGSL valide** (round-trip)
- [ ] Formater le **code source** (indentation, style)
- [ ] Préserver les **commentaires** si possible

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/`

### 3. Traitement IR Avancé
- [ ] **Constant Evaluator** : Évaluer les expressions constantes à la "compilation"
- [ ] **Typifier** : Inférence de types pour les expressions
- [ ] **Layouter** : Calcul du layout mémoire (alignement, padding)
- [ ] **Namer** : Génération automatique de noms pour les variables temporaires

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/`

### 4. Validation avec Outils Natifs
- [ ] **spirv-val** : Validation SPIR-V (si backend SPIR-V implémenté)
- [ ] **glslangValidator** : Validation GLSL
- [ ] **Metal compiler** : Validation MSL (via `metal` CLI)
- [ ] **DXC/FXC** : Validation HLSL

---

## ⚪ OBJECTIFS FUTURS (NICE TO HAVE)

### 1. Frontend GLSL
- [ ] Parser GLSL 440+ et Vulkan semantics
- [ ] Gérer le **préprocesseur GLSL**
- [ ] Supporter les **versions multiples** (120, 330, 450, etc.)

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/glsl/`

### 2. Frontend/Backend SPIR-V
- [ ] Parser le **format binaire SPIR-V**
- [ ] Générer du **SPIR-V binaire valide**
- [ ] Supporter **Vulkan 1.3+**

**Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/spv/` et `/Users/chaos/RustroverProjects/wgpu/naga/src/back/spv/`

### 3. Support Android
- [ ] Compiler avec **Kotlin Native**
- [ ] Intégration avec **Android NDK**
- [ ] Optimiser pour **mobile** (mémoire, performance)

### 4. Outil CLI
- [ ] Commande `naga-kt` pour conversion depuis la ligne de commande
- [ ] Supporter tous les **formats d'entrée/sortie**
- [ ] Options de **formattage et optimisation**

---

## ❌ NON-SCOPE (EXCLU)

Les éléments suivants **ne seront PAS implémentés** dans ce projet :

1. **Frontend/Backend DOT** (GraphViz) - Peu utile pour WebGPU
2. **Frontend/Backend SPIR-V binaire** - Sauf si demande explicite
3. **Support WebAssembly** (Kotlin/JS) - Futur lointain
4. **Intégration avec Deno** - Spécifique à l'écosystème Rust
5. **Support des anciennes versions de WGSL** - Se concentrer sur la spécification actuelle
6. **Optimisations agressives** - Se contenter d'un code correct et lisible
7. **Support temps réel** - C'est un outil de build-time, pas de runtime

---

## 🎯 CRITÈRES D'ACCEPTATION (MVP)

Pour que le **MVP (Minimum Viable Product)** soit considéré comme complet :

- [ ] **Core IR** : 100% des structures de base implémentées
- [ ] **WGSL Parser** : 100% de la syntaxe WGSL supportée
- [ ] **MSL Backend** : Génération de MSL valide et compilable
- [ ] **HLSL Backend** : Génération de HLSL valide et compilable
- [ ] **Validation** : Pipeline de validation fonctionnel avec golden files
- [ ] **Tests** : > 80% de couverture pour le core IR et WGSL parser
- [ ] **Documentation** : README complet avec exemples d'utilisation

---

## 📊 METRIQUES DE SUCCÈS

| Métrique | Cible MVP | Cible V1.0 | Cible Ideal |
|----------|-----------|-------------|-------------|
| Couverture de test | > 70% | > 80% | > 90% |
| Performance (WGSL→IR) | < 100ms/shader | < 50ms/shader | < 20ms/shader |
| Taille du code | < 20k lignes | < 25k lignes | < 30k lignes |
| Dépendances externes | 2 (kotlinx) | 3 | 4 |
| Compatibilité WGSL | 95% | 98% | 100% |
| Validation native | 80% | 90% | 100% |

---

## 🔗 DÉPENDANCES AU PROJET WGPU/NAGA RUST

Ce projet **dépend** des éléments suivants depuis le dépôt Naga Rust :

1. **Spécification WGSL** : Doit suivre la même spécification
2. **Golden Files** : Utilisés pour validation (fichiers dans `tests/in/` et `tests/out/`)
3. **Algorithmes** : Certains algorithmes peuvent être portés ou adaptés

**Ce projet est INDEPENDANT** des éléments suivants :
- Implémentation Rust de Naga
- Format binaire de Naga
- APIs Rust spécifiques

---

## 📝 NOTES SUPPLÉMENTAIRES

1. **Compatibilité** : Le IR Kotlin doit être **compatible** avec le IR Rust pour faciliter la validation
2. **Extensibilité** : L'architecture doit permettre d'ajouter facilement de nouveaux frontends/backends
3. **Maintenabilité** : Le code doit être **bien documenté** et **facile à comprendre**
4. **Performance** : Les optimisations doivent être **progressives** (d'abord correct, puis rapide)

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Valider ces objectifs avec les stakeholders
2. [ ] Prioriser les objectifs secondaires
3. [ ] Définir les critères d'acceptation détaillés pour chaque composant
4. [ ] Passer à la phase de conception technique (fichier `01_contraintes.md`)
