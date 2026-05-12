# ✅ Phase 3 : Checklist Complète - Processing

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 3 - Processing  
**Durée totale** : 8-12 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique)  
**Statut Global** : [ ] 0% | [ ] 25% | [ ] 50% | [ ] 75% | [ ] 100%

---

## 📊 SOMMAIRE DE LA PHASE 3

| Sous-Phase | Durée | Fichiers | Statut | Progression |
|------------|-------|----------|--------|-------------|
| **3.0 - Constant Evaluator** | 2-3 semaines | 5 fichiers | [ ] | 0% |
| **3.1 - Typifier** | 2-3 semaines | 4 fichiers | [ ] | 0% |
| **3.2 - Layouter** | 2-3 semaines | 5 fichiers | [ ] | 0% |
| **3.3 - Namer** | 2-3 semaines | 4 fichiers | [ ] | 0% |
| **3.4 - Validator** | 3-4 semaines | 15+ fichiers | [ ] | 0% |

**Total Phase 3** : **8-12 semaines** | **~33 fichiers** | **Progression Globale : 0%**

---

## 🎯 CHECKLIST GLOBALE PHASE 3

### ⬜ Sous-Phase 3.0 : Évaluateur de Constantes (2-3 semaines)

**Fichier** : `00_constant-evaluator.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [ ] `ConstValue.kt` - Valeurs constantes (ScalarValue, VectorValue, MatrixValue, ArrayValue)
- [ ] `ConstantEvaluator.kt` - Classe principale avec tryEvaluate()
- [ ] `ConstantEvaluationError.kt` - Erreurs d'évaluation
- [ ] `ConstantExpressionChecker.kt` - Vérification des expressions constantes

#### ConstValue
- [ ] `ScalarValue` (Bool, Sint, Uint, Float, AbstractInt)
- [ ] `VectorValue` (list of ScalarValue, width)
- [ ] `MatrixValue` (list of VectorValue, columns, rows)
- [ ] `ArrayValue` (list of ConstValue)
- [ ] `SplatValue` (value, size)
- [ ] Méthodes utilitaires (getElement, getVectorSize, getScalarKind)

#### Évaluation des Expressions
- [ ] `tryEvaluateLiteral()` - Littéraux
- [ ] `tryEvaluateIdent()` - Identifiants (constantes globales)
- [ ] `tryEvaluateUnary()` - Opérateurs unaires (+, -, !, ~)
- [ ] `tryEvaluateBinary()` - Opérateurs binaires (+, -, *, /, %, ==, !=, <, >, <=, >=, &&, ||, &, |, ^, <<, >>)
- [ ] `tryEvaluateSelect()` - Opérateur ternaire select
- [ ] `tryEvaluateCompose()` - Construction de vecteurs/matrices
- [ ] `tryEvaluateSplat()` - Splat de valeurs scalaires
- [ ] `tryEvaluateSwizzle()` - Swizzle de vecteurs
- [ ] `tryEvaluateAccess()` - Accès aux membres/indices
- [ ] `tryEvaluateAccessIndex()` - Accès aux indices constants
- [ ] `tryEvaluateArrayLength()` - Longueur de tableau
- [ ] `tryEvaluateAs()` - Cast/bitcast
- [ ] `tryEvaluateMath()` - Fonctions mathématiques (abs, min, max, clamp, etc.)
- [ ] `tryEvaluateDerivative()` - Dérivées (dpdx, dpdy, fwidth)

#### Évaluation par Type
- [ ] Évaluation des scalaires (i32, u32, f32, bool)
- [ ] Évaluation des vecteurs (vec2, vec3, vec4)
- [ ] Évaluation des matrices (mat2x2, mat2x3, mat2x4, mat3x2, mat3x3, mat3x4, mat4x2, mat4x3, mat4x4)
- [ ] Évaluation des tableaux
- [ ] Évaluation des structs

#### Opérations Supportées
- [ ] Opérations arithmétiques (+, -, *, /, %)
- [ ] Opérations logiques (&&, ||, !)
- [ ] Opérations de comparaison (==, !=, <, >, <=, >=)
- [ ] Opérations bits (&, |, ^, ~, <<, >>)
- [ ] Constructeurs (vec2, vec3, vec4, mat2x2, etc.)
- [ ] Conversions (i32(), u32(), f32(), etc.)
- [ ] Fonctions mathématiques (abs, min, max, clamp, floor, ceil, round, etc.)
- [ ] Fonctions trigonométriques (sin, cos, tan, etc.)
- [ ] Fonctions exponentielles (exp, log, pow, etc.)

#### Gestion des Erreurs
- [ ] Division par zéro
- [ ] Overflow/Underflow
- [ ] Opérations invalides (ex: f32 % f32)
- [ ] Accès hors limites
- [ ] Types incompatibles

#### Intégration
- [ ] Intégration avec le parser WGSL
- [ ] Intégration avec le typifier
- [ ] Utilisation dans le validator
- [ ] Utilisation dans les backends

#### Tests
- [ ] Tests pour toutes les opérations scalaires
- [ ] Tests pour toutes les opérations vectorielles
- [ ] Tests pour toutes les opérations matricielles
- [ ] Tests pour les constructeurs
- [ ] Tests pour les conversions
- [ ] Tests pour les fonctions mathématiques
- [ ] Tests pour les erreurs

---

### ⬜ Sous-Phase 3.1 : Typifier (2-3 semaines)

**Fichier** : `01_typifier.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [ ] `TypeResolution.kt` - Résolution de type
- [ ] `Typifier.kt` - Classe principale
- [ ] `TypeError.kt` - Erreurs de typage
- [ ] `TypeCompatibility.kt` - Vérification de compatibilité

#### TypeResolution
- [ ] `TypeResolution` (ty: Handle<Type>, span: Span)
- [ ] `TypeResolution.error()` - Résolution en erreur
- [ ] `TypeResolution.unresolved()` - Non résolu

#### Fonctionnalités Typifier
- [ ] Assignation des types explicites
- [ ] Inférence des types pour les littéraux
- [ ] Inférence des types pour les opérations binaires
- [ ] Inférence des types pour les opérations unaires
- [ ] Inférence des types pour les constructeurs
- [ ] Inférence des types pour les accès (membre, index)
- [ ] Inférence des types pour les appels de fonction
- [ ] Inférence des types pour les expressions select
- [ ] Inférence des types pour les casts/bitcasts
- [ ] Vérification de compatibilité des types

#### Inférence par Expression
- [ ] `Literal` - Type basé sur la valeur
- [ ] `Ident` - Type de la déclaration
- [ ] `Unary` - Même type que l'opérande
- [ ] `Binary` - Type basé sur les opérandes et l'opérateur
- [ ] `Compose` - Type vectoriel/matriciel
- [ ] `Splat` - Type vectoriel
- [ ] `Swizzle` - Même type que la source
- [ ] `Access` - Type du membre/élément
- [ ] `AccessIndex` - Type du membre/élément
- [ ] `Select` - Type basé sur les branches
- [ ] `As` - Type de la cible
- [ ] `Call` - Type du retour de la fonction
- [ ] `Math` - Type basé sur l'opération

#### Vérification de Type
- [ ] Vérification que les opérandes sont compatibles
- [ ] Vérification que les opérations sont autorisées
- [ ] Vérification des conversions implicites
- [ ] Vérification des casts explicites

#### Tests
- [ ] Tests pour l'inférence des littéraux
- [ ] Tests pour l'inférence des opérations
- [ ] Tests pour la compatibilité des types
- [ ] Tests pour les erreurs de typage

---

### ⬜ Sous-Phase 3.2 : Layouter (2-3 semaines)

**Fichier** : `02_layouter.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [ ] `Alignment.kt` - Classe Alignment
- [ ] `TypeLayout.kt` - Layout de type
- [ ] `StructMemberLayout.kt` - Layout des membres de struct
- [ ] `Layouter.kt` - Classe principale
- [ ] `LayoutError.kt` - Erreurs de layout

#### Alignment
- [ ] Classe Alignment (value class)
- [ ] Constantes (ONE, TWO, FOUR, EIGHT, SIXTEEN)
- [ ] MIN_UNIFORM (16)
- [ ] `fromWidth()` - Créer à partir d'une largeur
- [ ] `new()` - Créer s'il s'agit d'une puissance de 2
- [ ] `isPowerOfTwo` - Extension Int
- [ ] `isAligned()` - Vérifier l'alignement
- [ ] `roundUp()` - Arrondir à l'alignement supérieur
- [ ] `times()` - Multiplier par un scalaire
- [ ] `max()` - Maximum de deux alignments
- [ ] Implémentation de Comparable

#### TypeLayout
- [ ] Data class TypeLayout (size, alignment)
- [ ] `toStride()` - Calculer le stride

#### Layouter
- [ ] `update()` - Mettre à jour tous les layouts
- [ ] `updateNew()` - Mettre à jour les nouveaux types uniquement
- [ ] `get()` - Récupérer le layout d'un type
- [ ] `getStructMembers()` - Récupérer les layouts des membres
- [ ] `clear()` - Effacer tous les layouts
- [ ] Calcul pour Scalar
- [ ] Calcul pour Vector
- [ ] Calcul pour Matrix
- [ ] Calcul pour CooperativeMatrix
- [ ] Calcul pour Pointer/ValuePointer
- [ ] Calcul pour Array
- [ ] Calcul pour Struct (avec offsets)
- [ ] Calcul pour Image/Sampler/AccelerationStructure/RayQuery/BindingArray

#### LayoutError
- [ ] TooLarge
- [ ] InvalidArrayElementType
- [ ] InvalidStructMemberType
- [ ] NonPowerOfTwoWidth
- [ ] `message()` - Message d'erreur

#### Tests
- [ ] Tests pour Alignment
- [ ] Tests pour les types scalaires
- [ ] Tests pour les types vectoriels
- [ ] Tests pour les types matrices
- [ ] Tests pour les structs
- [ ] Tests pour les arrays
- [ ] Tests pour les erreurs

---

### ⬜ Sous-Phase 3.3 : Namer (2-3 semaines)

**Fichier** : `03_namer.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [ ] `NameKey.kt` - Clés d'identification
- [ ] `KeywordSet.kt` - Ensembles de mots-clés
- [ ] `CaseInsensitiveKeywordSet.kt` - Mots-clés insensibles à la casse
- [ ] `Namer.kt` - Classe principale

#### NameKey
- [ ] Type(handle)
- [ ] StructMember(type, index)
- [ ] Function(handle)
- [ ] FunctionArgument(function, index)
- [ ] FunctionLocal(function, variable)
- [ ] GlobalVariable(handle)
- [ ] Constant(handle)
- [ ] Override(handle)
- [ ] EntryPoint(index)
- [ ] EntryPointArgument(ep, index)
- [ ] EntryPointLocal(ep, variable)
- [ ] ExternalTextureGlobalVariable(global, key)
- [ ] ExternalTextureFunctionArgument(function, index, key)
- [ ] FunctionOobLocal(function, type)
- [ ] EntryPointOobLocal(ep, type)
- [ ] ExternalTextureNameKey (Plane, Params)

#### KeywordSet
- [ ] Classe KeywordSet
- [ ] WGSL_KEYWORDS
- [ ] MSL_KEYWORDS
- [ ] HLSL_KEYWORDS
- [ ] GLSL_KEYWORDS
- [ ] WGSL_BUILTIN_IDENTIFIERS

#### Namer
- [ ] `reset()` - Réinitialiser pour un nouveau module
- [ ] `call()` - Générer un nom unique
- [ ] `callOr()` - Générer un nom avec fallback
- [ ] `sanitize()` - Nettoyer un nom
- [ ] `ensureUniqueness()` - Garantir l'unicité
- [ ] `namespace()` - Créer un namespace local
- [ ] `nameTypes()` - Nommer les types
- [ ] `nameEntryPoints()` - Nommer les entry points
- [ ] `nameFunctions()` - Nommer les fonctions
- [ ] `nameGlobalVariables()` - Nommer les variables globales
- [ ] `nameConstants()` - Nommer les constantes
- [ ] `nameOverrides()` - Nommer les overrides

#### Sanitization
- [ ] Supprimer les caractères non alphanumériques (sauf _)
- [ ] Remplacer les caractères spéciaux (: < > ,) par _
- [ ] Convertir __ en _
- [ ] Supprimer les _ en double
- [ ] Ajouter préfixe si commence par un chiffre
- [ ] Utiliser "unnamed" si vide
- [ ] Éviter les conflits avec les mots-clés
- [ ] Vérifier les préfixes réservés

#### Tests
- [ ] Tests pour sanitize()
- [ ] Tests pour call()
- [ ] Tests pour callOr()
- [ ] Tests pour les mots-clés
- [ ] Tests pour les préfixes réservés
- [ ] Tests d'intégration

---

### ⬜ Sous-Phase 3.4 : Validator (3-4 semaines)

**Fichier** : `04_validator.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [ ] `ValidationFlags.kt` - Flags de validation
- [ ] `Capabilities.kt` - Capacités supportées
- [ ] `ShaderStages.kt` - Étapes de shader
- [ ] `SubgroupOperationSet.kt` - Opérations de subgroup
- [ ] `ValidationError.kt` - Erreurs de validation
- [ ] `Validator.kt` - Classe principale
- [ ] `ModuleInfo.kt` - Informations du module
- [ ] `type/TypeError.kt` - Erreurs de type
- [ ] `function/FunctionError.kt` - Erreurs de fonction
- [ ] `function/LocalVariableError.kt` - Erreurs de variable locale
- [ ] `interface/EntryPointError.kt` - Erreurs d'entry point
- [ ] `interface/GlobalVariableError.kt` - Erreurs de variable globale
- [ ] `interface/VaryingError.kt` - Erreurs de varying
- [ ] `expression/ExpressionError.kt` - Erreurs d'expression
- [ ] `expression/ConstExpressionError.kt` - Erreurs d'expression constante
- [ ] `compose/ComposeError.kt` - Erreurs de composition

#### ValidationFlags
- [ ] EXPRESSIONS
- [ ] BLOCKS
- [ ] CONTROL_FLOW_UNIFORMITY
- [ ] STRUCT_LAYOUTS
- [ ] CONSTANTS
- [ ] BINDINGS
- [ ] ALL, NONE
- [ ] `or()`, `and()`

#### Capabilities
- [ ] IMMEDIATES
- [ ] FLOAT64
- [ ] PRIMITIVE_INDEX
- [ ] TEXTURE_AND_SAMPLER_BINDING_ARRAY
- [ ] BUFFER_BINDING_ARRAY
- [ ] STORAGE_TEXTURE_BINDING_ARRAY
- [ ] STORAGE_BUFFER_BINDING_ARRAY
- [ ] CLIP_DISTANCES
- [ ] CULL_DISTANCE
- [ ] STORAGE_TEXTURE_16BIT_NORM_FORMATS
- [ ] MULTIVIEW
- [ ] EARLY_DEPTH_TEST
- [ ] MULTISAMPLED_SHADING
- [ ] RAY_QUERY
- [ ] RAY_HIT_VERTEX_POSITION
- [ ] DUAL_SOURCE_BLENDING
- [ ] CUBE_ARRAY_TEXTURES
- [ ] SHADER_INT64
- [ ] SHADER_INT64_ATOMIC_MIN_MAX
- [ ] SHADER_INT64_ATOMIC_ALL_OPS
- [ ] SHADER_FLOAT32_ATOMIC
- [ ] TEXTURE_ATOMIC
- [ ] TEXTURE_INT64_ATOMIC
- [ ] SHADER_FLOAT16
- [ ] SHADER_FLOAT16_IN_FLOAT32
- [ ] TEXTURE_EXTERNAL
- [ ] SHADER_BARYCENTRICS
- [ ] MESH_SHADER
- [ ] MESH_SHADER_POINT_TOPOLOGY
- [ ] TEXTURE_AND_SAMPLER_BINDING_ARRAY_NON_UNIFORM_INDEXING
- [ ] BUFFER_BINDING_ARRAY_NON_UNIFORM_INDEXING
- [ ] STORAGE_TEXTURE_BINDING_ARRAY_NON_UNIFORM_INDEXING
- [ ] STORAGE_BUFFER_BINDING_ARRAY_NON_UNIFORM_INDEXING
- [ ] COOPERATIVE_MATRIX
- [ ] PER_VERTEX
- [ ] DRAW_INDEX
- [ ] ACCELERATION_STRUCTURE_BINDING_ARRAY
- [ ] MEMORY_DECORATION_COHERENT
- [ ] MEMORY_DECORATION_VOLATILE
- [ ] SUBGROUP
- [ ] SUBGROUP_BARRIER
- [ ] SUBGROUP_VERTEX_STAGE
- [ ] RAY_TRACING_PIPELINE

#### Validator
- [ ] `validate()` - Valider un module
- [ ] `validateQuiet()` - Valider sans exception
- [ ] `validateThrow()` - Valider avec exception
- [ ] `validateTypes()` - Valider les types
- [ ] `validateConstants()` - Valider les constantes
- [ ] `validateOverrides()` - Valider les overrides
- [ ] `validateFunctions()` - Valider les fonctions
- [ ] `validateEntryPoints()` - Valider les entry points
- [ ] `validateGlobalVariables()` - Valider les variables globales
- [ ] `validateExpressions()` - Valider les expressions
- [ ] `validateStatements()` - Valider les statements

#### Validation des Types
- [ ] Scalar (width valide)
- [ ] Vector (width scalaire valide)
- [ ] Matrix (dimensions valides)
- [ ] Struct (offsets alignés, span correct)
- [ ] Array (taille positive)
- [ ] Pointer
- [ ] Image/Sampler

#### Validation des Constantes
- [ ] Initialiseur est une expression constante
- [ ] Type correspond
- [ ] Type est constructible

#### Validation des Overrides
- [ ] Type est scalaire
- [ ] ID est unique
- [ ] Initialiseur est valide

#### Validation des Fonctions
- [ ] Arguments ont des types valides
- [ ] Type de retour est valide
- [ ] Variables locales ont des types valides
- [ ] Initialiseurs des variables locales sont valides
- [ ] Corps de la fonction est valide

#### Validation des Entry Points
- [ ] Nom n'est pas vide
- [ ] Stage est supportée
- [ ] Pas de binding sur les arguments
- [ ] early_depth_test est supporté
- [ ] workgroup_size est valide
- [ ] Bindings sont uniques

#### Validation des Variables Globales
- [ ] Type est valide
- [ ] Binding est valide pour VariableClass
- [ ] Binding est unique
- [ ] Initialiseur est une expression constante

#### Tests
- [ ] Tests pour un module vide
- [ ] Tests pour un module valide
- [ ] Tests pour chaque type d'erreur
- [ ] Tests pour ValidationFlags
- [ ] Tests pour Capabilities

---

## 📊 RÉCAPITULATIF PAR DOSSIER

### `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/`

#### Constant Evaluator
- [ ] ConstValue.kt
- [ ] ConstantEvaluator.kt
- [ ] ConstantEvaluationError.kt
- [ ] ConstantExpressionChecker.kt

#### Typifier
- [ ] TypeResolution.kt
- [ ] Typifier.kt
- [ ] TypeError.kt
- [ ] TypeCompatibility.kt

#### Layouter
- [ ] Alignment.kt
- [ ] TypeLayout.kt
- [ ] StructMemberLayout.kt
- [ ] Layouter.kt
- [ ] LayoutError.kt

#### Namer
- [ ] NameKey.kt
- [ ] KeywordSet.kt
- [ ] CaseInsensitiveKeywordSet.kt
- [ ] Namer.kt

### `wgsl:core/src/main/kotlin/dev/gfxrs/naga/valid/`

#### Root
- [ ] ValidationFlags.kt
- [ ] Capabilities.kt
- [ ] ShaderStages.kt
- [ ] SubgroupOperationSet.kt
- [ ] ValidationError.kt
- [ ] Validator.kt
- [ ] ModuleInfo.kt

#### type/
- [ ] TypeError.kt
- [ ] TypeFlags.kt

#### function/
- [ ] FunctionError.kt
- [ ] LocalVariableError.kt
- [ ] CallError.kt
- [ ] SubgroupError.kt

#### interface/
- [ ] EntryPointError.kt
- [ ] GlobalVariableError.kt
- [ ] VaryingError.kt

#### expression/
- [ ] ExpressionError.kt
- [ ] ConstExpressionError.kt
- [ ] LiteralError.kt

#### compose/
- [ ] ComposeError.kt

#### handles/
- [ ] InvalidHandleError.kt

#### immediates/
- [ ] ImmediateSlots.kt

---

## 🎯 CHECKLIST DES TESTS

### Tests Constant Evaluator
- [ ] `ConstantEvaluatorTest.kt`
  - [ ] Tests pour les scalaires
  - [ ] Tests pour les vecteurs
  - [ ] Tests pour les matrices
  - [ ] Tests pour les tableaux
  - [ ] Tests pour les opérations arithmétiques
  - [ ] Tests pour les opérations logiques
  - [ ] Tests pour les opérations bits
  - [ ] Tests pour les constructeurs
  - [ ] Tests pour les conversions
  - [ ] Tests pour les fonctions mathématiques
  - [ ] Tests pour les erreurs

### Tests Typifier
- [ ] `TypifierTest.kt`
  - [ ] Tests pour l'inférence des littéraux
  - [ ] Tests pour l'inférence des opérations
  - [ ] Tests pour la compatibilité des types
  - [ ] Tests pour les erreurs

### Tests Layouter
- [ ] `AlignmentTest.kt`
- [ ] `LayouterTest.kt`
  - [ ] Tests pour tous les types
  - [ ] Tests pour les structs
  - [ ] Tests pour les erreurs

### Tests Namer
- [ ] `NamerTest.kt`
  - [ ] Tests pour sanitize()
  - [ ] Tests pour call()
  - [ ] Tests pour les mots-clés
  - [ ] Tests d'intégration

### Tests Validator
- [ ] `ValidatorTest.kt`
  - [ ] Tests pour un module vide
  - [ ] Tests pour un module valide
  - [ ] Tests pour chaque type d'erreur
  - [ ] Tests pour ValidationFlags
  - [ ] Tests pour Capabilities

---

## 📈 CRITÈRES D'ACCEPTATION

### Phase 3 Complète
- [ ] Tous les fichiers de plan sont créés et complets
- [ ] Tous les fichiers source Kotlin sont implémentés
- [ ] Tous les tests unitaires passent
- [ ] L'intégration avec les autres phases fonctionne
- [ ] La documentation est complète
- [ ] Les exemples fonctionnent

### Critères de Qualité
- [ ] Code propre et bien structuré
- [ ] Respect des conventions Kotlin
- [ ] Nommage cohérent
- [ ] Documentation complète (KDoc)
- [ ] Gestion d'erreur robuste
- [ ] Performance acceptable (3-10x plus lent que Rust)

### Critères de Couverture
- [ ] Couverture de test > 90%
- [ ] Tous les cas d'usage sont couverts
- [ ] Tous les cas d'erreur sont testés
- [ ] Intégration testée avec les autres modules

---

## 📅 PLANNING DÉTAILLÉ

### Semaine 1-2 : Constant Evaluator
- [ ] Implémenter ConstValue.kt (2-4h)
- [ ] Implémenter ConstantEvaluator.kt - base (4-8h)
- [ ] Implémenter l'évaluation des scalaires (4h)
- [ ] Implémenter l'évaluation des vecteurs (4h)
- [ ] Implémenter l'évaluation des matrices (4h)
- [ ] Implémenter l'évaluation des tableaux (4h)
- [ ] Implémenter les opérations arithmétiques (4h)
- [ ] Implémenter les opérations logiques (2h)
- [ ] Implémenter les opérations bits (2h)
- [ ] Implémenter les constructeurs (4h)
- [ ] Implémenter les conversions (4h)
- [ ] Implémenter les fonctions mathématiques (8h)
- [ ] Tests Constant Evaluator (8-12h)

### Semaine 3-4 : Typifier
- [ ] Implémenter TypeResolution.kt (2h)
- [ ] Implémenter Typifier.kt - base (4-8h)
- [ ] Implémenter l'inférence des littéraux (2h)
- [ ] Implémenter l'inférence des opérations (8-12h)
- [ ] Implémenter la vérification de compatibilité (4h)
- [ ] Tests Typifier (8-12h)

### Semaine 5-6 : Layouter
- [ ] Implémenter Alignment.kt (2-4h)
- [ ] Implémenter TypeLayout.kt (2h)
- [ ] Implémenter StructMemberLayout.kt (2h)
- [ ] Implémenter Layouter.kt - base (4-8h)
- [ ] Implémenter le calcul par type (8-12h)
- [ ] Implémenter le layout des structs (8-12h)
- [ ] Implémenter LayoutError.kt (2h)
- [ ] Tests Layouter (8-12h)
- [ ] Intégration avec Validator (4h)

### Semaine 7-8 : Namer
- [ ] Implémenter NameKey.kt (4-6h)
- [ ] Implémenter KeywordSet.kt (2-4h)
- [ ] Implémenter CaseInsensitiveKeywordSet.kt (2h)
- [ ] Implémenter Namer.kt - base (4-8h)
- [ ] Implémenter sanitize() (4-6h)
- [ ] Implémenter call() et l'unicité (4h)
- [ ] Implémenter le naming des types (4-6h)
- [ ] Implémenter le naming des fonctions (4h)
- [ ] Implémenter le naming des variables (4h)
- [ ] Implémenter le naming des constantes (2h)
- [ ] Implémenter les namespaces (4h)
- [ ] Tests Namer (8-12h)
- [ ] Intégration avec les backends (4h)

### Semaine 9-12 : Validator
- [ ] Implémenter ValidationFlags.kt (2-4h)
- [ ] Implémenter Capabilities.kt (4-6h)
- [ ] Implémenter ShaderStages.kt (2h)
- [ ] Implémenter SubgroupOperationSet.kt (2h)
- [ ] Implémenter ValidationError.kt (4-6h)
- [ ] Implémenter les classes d'erreur spécifiques (16-24h)
- [ ] Implémenter Validator.kt - base (8-12h)
- [ ] Implémenter validateTypes() (8h)
- [ ] Implémenter validateConstants() (4h)
- [ ] Implémenter validateOverrides() (4h)
- [ ] Implémenter validateFunctions() (12-16h)
- [ ] Implémenter validateEntryPoints() (8h)
- [ ] Implémenter validateGlobalVariables() (8h)
- [ ] Implémenter validateExpressions() (16-24h)
- [ ] Implémenter validateStatements() (16-24h)
- [ ] Tests Validator (24-40h)
- [ ] Intégration avec les backends (8h)

---

## 🔄 DÉPENDANCES ENTRE SOUS-PHASES

```
Phase 3.0 : Constant Evaluator
    ↓
Phase 3.1 : Typifier (dépend des types et expressions)
    ↓
Phase 3.2 : Layouter (dépend des types)
    ↓
Phase 3.3 : Namer (dépend du module complet)
    ↓
Phase 3.4 : Validator (dépend de tout)
```

**Note** : Les sous-phases peuvent être implémentées en parallèle dans une certaine mesure, mais le Validator dépend de toutes les autres.

---

## 🎯 LIVRABLES DE LA PHASE 3

1. **Module `wgsl:core (proc)`** : Contient ConstantEvaluator, Typifier, Layouter, Namer
2. **Module `wgsl:core (valid)`** : Contient Validator et toutes les classes d'erreur
3. **Tests unitaires** : Couverture > 90% pour tous les modules
4. **Documentation** : Documentation complète pour toutes les APIs publiques
5. **Exemples** : Exemples d'utilisation de chaque composant

---

## 📝 NOTES

1. **Priorisation** : Commencer par le Constant Evaluator et le Layouter, car ils sont nécessaires pour le Namer et le Validator.

2. **Parallélisation** : Le Typifier et le Layouter peuvent être implémentés en parallèle après le Constant Evaluator.

3. **Complexité** : Le Validator est la sous-phase la plus complexe. Prévoir plus de temps pour cette partie.

4. **Tests** : Les tests doivent être écrits en parallèle avec l'implémentation pour garantir la qualité.

5. **Revues de code** : Prévoir des revues de code régulières pour maintenir la qualité.

6. **Intégration continue** : Intégrer chaque composant dès qu'il est prêt pour détecter les problèmes tôt.
