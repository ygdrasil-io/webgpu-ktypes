# ✅ Phase 3 : Checklist Complète - Processing

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 3 - Processing  
**Durée totale** : 8-12 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique)  
**Statut Global** : [ ] 0% | [ ] 25% | [ ] 50% | [ ] 75% | [x] 100%

---

## 📊 SOMMAIRE DE LA PHASE 3

| Sous-Phase | Durée | Fichiers | Statut | Progression |
|------------|-------|----------|--------|-------------|
| **3.0 - Constant Evaluator** | 2-3 semaines | 5 fichiers | [x] | 100% |
| **3.1 - Typifier** | 2-3 semaines | 4 fichiers | [x] | 100% |
| **3.2 - Layouter** | 2-3 semaines | 5 fichiers | [x] | 100% |
| **3.3 - Namer** | 2-3 semaines | 4 fichiers | [x] | 100% |
| **3.4 - Validator** | 3-4 semaines | 15+ fichiers | [x] | 100% |

**Total Phase 3** : **8-12 semaines** | **~33 fichiers** | **Progression Globale : 100%**

---

## 🎯 CHECKLIST GLOBALE PHASE 3

### ⬜ Sous-Phase 3.0 : Évaluateur de Constantes (2-3 semaines)

**Fichier** : `00_constant-evaluator.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [x] `ConstValue.kt` - Valeurs constantes (ScalarValue, VectorValue, MatrixValue, ArrayValue)
- [x] `ConstantEvaluator.kt` - Classe principale avec tryEvaluate()
- [x] `ConstantEvaluationError.kt` - Erreurs d'évaluation
- [x] `ConstantExpressionChecker.kt` - Vérification des expressions constantes

#### ConstValue
- [x] `ScalarValue` (Bool, Sint, Uint, Float, AbstractInt)
- [x] `VectorValue` (list of ScalarValue, width)
- [x] `MatrixValue` (list of VectorValue, columns, rows)
- [x] `ArrayValue` (list of ConstValue)
- [x] `SplatValue` (value, size)
- [x] Méthodes utilitaires (getElement, getVectorSize, getScalarKind)

#### Évaluation des Expressions
- [x] `tryEvaluateLiteral()` - Littéraux
- [x] `tryEvaluateIdent()` - Identifiants (constantes globales)
- [x] `tryEvaluateUnary()` - Opérateurs unaires (+, -, !, ~)
- [x] `tryEvaluateBinary()` - Opérateurs binaires (+, -, *, /, %, ==, !=, <, >, <=, >=, &&, ||, &, |, ^, <<, >>)
- [x] `tryEvaluateSelect()` - Opérateur ternaire select
- [x] `tryEvaluateCompose()` - Construction de vecteurs/matrices
- [x] `tryEvaluateSplat()` - Splat de valeurs scalaires
- [x] `tryEvaluateSwizzle()` - Swizzle de vecteurs
- [x] `tryEvaluateAccess()` - Accès aux membres/indices
- [x] `tryEvaluateAccessIndex()` - Accès aux indices constants
- [x] `tryEvaluateArrayLength()` - Longueur de tableau
- [x] `tryEvaluateAs()` - Cast/bitcast
- [x] `tryEvaluateMath()` - Fonctions mathématiques (abs, min, max, clamp, etc.)
- [x] `tryEvaluateDerivative()` - Dérivées (dpdx, dpdy, fwidth)

#### Évaluation par Type
- [x] Évaluation des scalaires (i32, u32, f32, bool)
- [x] Évaluation des vecteurs (vec2, vec3, vec4)
- [x] Évaluation des matrices (mat2x2, mat2x3, mat2x4, mat3x2, mat3x3, mat3x4, mat4x2, mat4x3, mat4x4)
- [x] Évaluation des tableaux
- [x] Évaluation des structs

#### Opérations Supportées
- [x] Opérations arithmétiques (+, -, *, /, %)
- [x] Opérations logiques (&&, ||, !)
- [x] Opérations de comparaison (==, !=, <, >, <=, >=)
- [x] Opérations bits (&, |, ^, ~, <<, >>)
- [x] Constructeurs (vec2, vec3, vec4, mat2x2, etc.)
- [x] Conversions (i32(), u32(), f32(), etc.)
- [x] Fonctions mathématiques (abs, min, max, clamp, floor, ceil, round, etc.)
- [x] Fonctions trigonométriques (sin, cos, tan, etc.)
- [x] Fonctions exponentielles (exp, log, pow, etc.)

#### Gestion des Erreurs
- [x] Division par zéro
- [x] Overflow/Underflow
- [x] Opérations invalides (ex: f32 % f32)
- [x] Accès hors limites
- [x] Types incompatibles

#### Intégration
- [x] Intégration avec le parser WGSL
- [x] Intégration avec le typifier
- [x] Utilisation dans le validator
- [x] Utilisation dans les backends

#### Tests
- [x] Tests pour toutes les opérations scalaires
- [x] Tests pour toutes les opérations vectorielles
- [x] Tests pour toutes les opérations matricielles
- [x] Tests pour les constructeurs
- [x] Tests pour les conversions
- [x] Tests pour les fonctions mathématiques
- [x] Tests pour les erreurs

---

### ⬜ Sous-Phase 3.1 : Typifier (2-3 semaines)

**Fichier** : `01_typifier.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [x] `TypeResolution.kt` - Résolution de type
- [x] `Typifier.kt` - Classe principale
- [x] `TypeError.kt` - Erreurs de typage
- [x] `TypeCompatibility.kt` - Vérification de compatibilité

#### TypeResolution
- [x] `TypeResolution` (ty: Handle<Type>, span: Span)
- [x] `TypeResolution.error()` - Résolution en erreur
- [x] `TypeResolution.unresolved()` - Non résolu

#### Fonctionnalités Typifier
- [x] Assignation des types explicites
- [x] Inférence des types pour les littéraux
- [x] Inférence des types pour les opérations binaires
- [x] Inférence des types pour les opérations unaires
- [x] Inférence des types pour les constructeurs
- [x] Inférence des types pour les accès (membre, index)
- [x] Inférence des types pour les appels de fonction
- [x] Inférence des types pour les expressions select
- [x] Inférence des types pour les casts/bitcasts
- [x] Vérification de compatibilité des types

#### Inférence par Expression
- [x] `Literal` - Type basé sur la valeur
- [x] `Ident` - Type de la déclaration
- [x] `Unary` - Même type que l'opérande
- [x] `Binary` - Type basé sur les opérandes et l'opérateur
- [x] `Compose` - Type vectoriel/matriciel
- [x] `Splat` - Type vectoriel
- [x] `Swizzle` - Même type que la source
- [x] `Access` - Type du membre/élément
- [x] `AccessIndex` - Type du membre/élément
- [x] `Select` - Type basé sur les branches
- [x] `As` - Type de la cible
- [x] `Call` - Type du retour de la fonction
- [x] `Math` - Type basé sur l'opération

#### Vérification de Type
- [x] Vérification que les opérandes sont compatibles
- [x] Vérification que les opérations sont autorisées
- [x] Vérification des conversions implicites
- [x] Vérification des casts explicites

#### Tests
- [x] Tests pour l'inférence des littéraux
- [x] Tests pour l'inférence des opérations
- [x] Tests pour la compatibilité des types
- [x] Tests pour les erreurs de typage

---

### ⬜ Sous-Phase 3.2 : Layouter (2-3 semaines)

**Fichier** : `02_layouter.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [x] `Alignment.kt` - Classe Alignment
- [x] `TypeLayout.kt` - Layout de type
- [x] `StructMemberLayout.kt` - Layout des membres de struct
- [x] `Layouter.kt` - Classe principale
- [x] `LayoutError.kt` - Erreurs de layout

#### Alignment
- [x] Classe Alignment (value class)
- [x] Constantes (ONE, TWO, FOUR, EIGHT, SIXTEEN)
- [x] MIN_UNIFORM (16)
- [x] `fromWidth()` - Créer à partir d'une largeur
- [x] `new()` - Créer s'il s'agit d'une puissance de 2
- [x] `isPowerOfTwo` - Extension Int
- [x] `isAligned()` - Vérifier l'alignement
- [x] `roundUp()` - Arrondir à l'alignement supérieur
- [x] `times()` - Multiplier par un scalaire
- [x] `max()` - Maximum de deux alignments
- [x] Implémentation de Comparable

#### TypeLayout
- [x] Data class TypeLayout (size, alignment)
- [x] `toStride()` - Calculer le stride

#### Layouter
- [x] `update()` - Mettre à jour tous les layouts
- [x] `updateNew()` - Mettre à jour les nouveaux types uniquement
- [x] `get()` - Récupérer le layout d'un type
- [x] `getStructMembers()` - Récupérer les layouts des membres
- [x] `clear()` - Effacer tous les layouts
- [x] Calcul pour Scalar
- [x] Calcul pour Vector
- [x] Calcul pour Matrix
- [x] Calcul pour CooperativeMatrix
- [x] Calcul pour Pointer/ValuePointer
- [x] Calcul pour Array
- [x] Calcul pour Struct (avec offsets)
- [x] Calcul pour Image/Sampler/AccelerationStructure/RayQuery/BindingArray

#### LayoutError
- [x] TooLarge
- [x] InvalidArrayElementType
- [x] InvalidStructMemberType
- [x] NonPowerOfTwoWidth
- [x] `message()` - Message d'erreur

#### Tests
- [x] Tests pour Alignment
- [x] Tests pour les types scalaires
- [x] Tests pour les types vectoriels
- [x] Tests pour les types matrices
- [x] Tests pour les structs
- [x] Tests pour les arrays
- [x] Tests pour les erreurs

---

### ⬜ Sous-Phase 3.3 : Namer (2-3 semaines)

**Fichier** : `03_namer.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [x] `NameKey.kt` - Clés d'identification
- [x] `KeywordSet.kt` - Ensembles de mots-clés
- [x] `CaseInsensitiveKeywordSet.kt` - Mots-clés insensibles à la casse
- [x] `Namer.kt` - Classe principale

#### NameKey
- [x] Type(handle)
- [x] StructMember(type, index)
- [x] Function(handle)
- [x] FunctionArgument(function, index)
- [x] FunctionLocal(function, variable)
- [x] GlobalVariable(handle)
- [x] Constant(handle)
- [x] Override(handle)
- [x] EntryPoint(index)
- [x] EntryPointArgument(ep, index)
- [x] EntryPointLocal(ep, variable)
- [x] ExternalTextureGlobalVariable(global, key)
- [x] ExternalTextureFunctionArgument(function, index, key)
- [x] FunctionOobLocal(function, type)
- [x] EntryPointOobLocal(ep, type)
- [x] ExternalTextureNameKey (Plane, Params)

#### KeywordSet
- [x] Classe KeywordSet
- [x] WGSL_KEYWORDS
- [x] MSL_KEYWORDS
- [x] HLSL_KEYWORDS
- [x] GLSL_KEYWORDS
- [x] WGSL_BUILTIN_IDENTIFIERS

#### Namer
- [x] `reset()` - Réinitialiser pour un nouveau module
- [x] `call()` - Générer un nom unique
- [x] `callOr()` - Générer un nom avec fallback
- [x] `sanitize()` - Nettoyer un nom
- [x] `ensureUniqueness()` - Garantir l'unicité
- [x] `namespace()` - Créer un namespace local
- [x] `nameTypes()` - Nommer les types
- [x] `nameEntryPoints()` - Nommer les entry points
- [x] `nameFunctions()` - Nommer les fonctions
- [x] `nameGlobalVariables()` - Nommer les variables globales
- [x] `nameConstants()` - Nommer les constantes
- [x] `nameOverrides()` - Nommer les overrides

#### Sanitization
- [x] Supprimer les caractères non alphanumériques (sauf _)
- [x] Remplacer les caractères spéciaux (: < > ,) par _
- [x] Convertir __ en _
- [x] Supprimer les _ en double
- [x] Ajouter préfixe si commence par un chiffre
- [x] Utiliser "unnamed" si vide
- [x] Éviter les conflits avec les mots-clés
- [x] Vérifier les préfixes réservés

#### Tests
- [x] Tests pour sanitize()
- [x] Tests pour call()
- [x] Tests pour callOr()
- [x] Tests pour les mots-clés
- [x] Tests pour les préfixes réservés
- [x] Tests d'intégration

---

### ⬜ Sous-Phase 3.4 : Validator (3-4 semaines)

**Fichier** : `04_validator.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structure des Fichiers
- [x] `ValidationFlags.kt` - Flags de validation
- [x] `Capabilities.kt` - Capacités supportées
- [x] `ShaderStages.kt` - Étapes de shader
- [x] `SubgroupOperationSet.kt` - Opérations de subgroup
- [x] `ValidationError.kt` - Erreurs de validation
- [x] `Validator.kt` - Classe principale
- [x] `ModuleInfo.kt` - Informations du module
- [x] `type/TypeError.kt` - Erreurs de type
- [x] `function/FunctionError.kt` - Erreurs de fonction
- [x] `function/LocalVariableError.kt` - Erreurs de variable locale
- [x] `interface/EntryPointError.kt` - Erreurs d'entry point
- [x] `interface/GlobalVariableError.kt` - Erreurs de variable globale
- [x] `interface/VaryingError.kt` - Erreurs de varying
- [x] `expression/ExpressionError.kt` - Erreurs d'expression
- [x] `expression/ConstExpressionError.kt` - Erreurs d'expression constante
- [x] `compose/ComposeError.kt` - Erreurs de composition

#### ValidationFlags
- [x] EXPRESSIONS
- [x] BLOCKS
- [x] CONTROL_FLOW_UNIFORMITY
- [x] STRUCT_LAYOUTS
- [x] CONSTANTS
- [x] BINDINGS
- [x] ALL, NONE
- [x] `or()`, `and()`

#### Capabilities
- [x] IMMEDIATES
- [x] FLOAT64
- [x] PRIMITIVE_INDEX
- [x] TEXTURE_AND_SAMPLER_BINDING_ARRAY
- [x] BUFFER_BINDING_ARRAY
- [x] STORAGE_TEXTURE_BINDING_ARRAY
- [x] STORAGE_BUFFER_BINDING_ARRAY
- [x] CLIP_DISTANCES
- [x] CULL_DISTANCE
- [x] STORAGE_TEXTURE_16BIT_NORM_FORMATS
- [x] MULTIVIEW
- [x] EARLY_DEPTH_TEST
- [x] MULTISAMPLED_SHADING
- [x] RAY_QUERY
- [x] RAY_HIT_VERTEX_POSITION
- [x] DUAL_SOURCE_BLENDING
- [x] CUBE_ARRAY_TEXTURES
- [x] SHADER_INT64
- [x] SHADER_INT64_ATOMIC_MIN_MAX
- [x] SHADER_INT64_ATOMIC_ALL_OPS
- [x] SHADER_FLOAT32_ATOMIC
- [x] TEXTURE_ATOMIC
- [x] TEXTURE_INT64_ATOMIC
- [x] SHADER_FLOAT16
- [x] SHADER_FLOAT16_IN_FLOAT32
- [x] TEXTURE_EXTERNAL
- [x] SHADER_BARYCENTRICS
- [x] MESH_SHADER
- [x] MESH_SHADER_POINT_TOPOLOGY
- [x] TEXTURE_AND_SAMPLER_BINDING_ARRAY_NON_UNIFORM_INDEXING
- [x] BUFFER_BINDING_ARRAY_NON_UNIFORM_INDEXING
- [x] STORAGE_TEXTURE_BINDING_ARRAY_NON_UNIFORM_INDEXING
- [x] STORAGE_BUFFER_BINDING_ARRAY_NON_UNIFORM_INDEXING
- [x] COOPERATIVE_MATRIX
- [x] PER_VERTEX
- [x] DRAW_INDEX
- [x] ACCELERATION_STRUCTURE_BINDING_ARRAY
- [x] MEMORY_DECORATION_COHERENT
- [x] MEMORY_DECORATION_VOLATILE
- [x] SUBGROUP
- [x] SUBGROUP_BARRIER
- [x] SUBGROUP_VERTEX_STAGE
- [x] RAY_TRACING_PIPELINE

#### Validator
- [x] `validate()` - Valider un module
- [x] `validateQuiet()` - Valider sans exception
- [x] `validateThrow()` - Valider avec exception
- [x] `validateTypes()` - Valider les types
- [x] `validateConstants()` - Valider les constantes
- [x] `validateOverrides()` - Valider les overrides
- [x] `validateFunctions()` - Valider les fonctions
- [x] `validateEntryPoints()` - Valider les entry points
- [x] `validateGlobalVariables()` - Valider les variables globales
- [x] `validateExpressions()` - Valider les expressions
- [x] `validateStatements()` - Valider les statements

#### Validation des Types
- [x] Scalar (width valide)
- [x] Vector (width scalaire valide)
- [x] Matrix (dimensions valides)
- [x] Struct (offsets alignés, span correct)
- [x] Array (taille positive)
- [x] Pointer
- [x] Image/Sampler

#### Validation des Constantes
- [x] Initialiseur est une expression constante
- [x] Type correspond
- [x] Type est constructible

#### Validation des Overrides
- [x] Type est scalaire
- [x] ID est unique
- [x] Initialiseur est valide

#### Validation des Fonctions
- [x] Arguments ont des types valides
- [x] Type de retour est valide
- [x] Variables locales ont des types valides
- [x] Initialiseurs des variables locales sont valides
- [x] Corps de la fonction est valide

#### Validation des Entry Points
- [x] Nom n'est pas vide
- [x] Stage est supportée
- [x] Pas de binding sur les arguments
- [x] early_depth_test est supporté
- [x] workgroup_size est valide
- [x] Bindings sont uniques

#### Validation des Variables Globales
- [x] Type est valide
- [x] Binding est valide pour VariableClass
- [x] Binding est unique
- [x] Initialiseur est une expression constante

#### Tests
- [x] Tests pour un module vide
- [x] Tests pour un module valide
- [x] Tests pour chaque type d'erreur
- [x] Tests pour ValidationFlags
- [x] Tests pour Capabilities

---

## 📊 RÉCAPITULATIF PAR DOSSIER

### `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/`

#### Constant Evaluator
- [x] ConstValue.kt
- [x] ConstantEvaluator.kt
- [x] ConstantEvaluationError.kt
- [x] ConstantExpressionChecker.kt

#### Typifier
- [x] TypeResolution.kt
- [x] Typifier.kt
- [x] TypeError.kt
- [x] TypeCompatibility.kt

#### Layouter
- [x] Alignment.kt
- [x] TypeLayout.kt
- [x] StructMemberLayout.kt
- [x] Layouter.kt
- [x] LayoutError.kt

#### Namer
- [x] NameKey.kt
- [x] KeywordSet.kt
- [x] CaseInsensitiveKeywordSet.kt
- [x] Namer.kt

### `wgsl:core/src/main/kotlin/dev/gfxrs/naga/valid/`

#### Root
- [x] ValidationFlags.kt
- [x] Capabilities.kt
- [x] ShaderStages.kt
- [x] SubgroupOperationSet.kt
- [x] ValidationError.kt
- [x] Validator.kt
- [x] ModuleInfo.kt

#### type/
- [x] TypeError.kt
- [x] TypeFlags.kt

#### function/
- [x] FunctionError.kt
- [x] LocalVariableError.kt
- [x] CallError.kt
- [x] SubgroupError.kt

#### interface/
- [x] EntryPointError.kt
- [x] GlobalVariableError.kt
- [x] VaryingError.kt

#### expression/
- [x] ExpressionError.kt
- [x] ConstExpressionError.kt
- [x] LiteralError.kt

#### compose/
- [x] ComposeError.kt

#### handles/
- [x] InvalidHandleError.kt

#### immediates/
- [x] ImmediateSlots.kt

---

## 🎯 CHECKLIST DES TESTS

### Tests Constant Evaluator
- [x] `ConstantEvaluatorTest.kt`
  - [x] Tests pour les scalaires
  - [x] Tests pour les vecteurs
  - [x] Tests pour les matrices
  - [x] Tests pour les tableaux
  - [x] Tests pour les opérations arithmétiques
  - [x] Tests pour les opérations logiques
  - [x] Tests pour les opérations bits
  - [x] Tests pour les constructeurs
  - [x] Tests pour les conversions
  - [x] Tests pour les fonctions mathématiques
  - [x] Tests pour les erreurs

### Tests Typifier
- [x] `TypifierTest.kt`
  - [x] Tests pour l'inférence des littéraux
  - [x] Tests pour l'inférence des opérations
  - [x] Tests pour la compatibilité des types
  - [x] Tests pour les erreurs

### Tests Layouter
- [x] `AlignmentTest.kt`
- [x] `LayouterTest.kt`
  - [x] Tests pour tous les types
  - [x] Tests pour les structs
  - [x] Tests pour les erreurs

### Tests Namer
- [x] `NamerTest.kt`
  - [x] Tests pour sanitize()
  - [x] Tests pour call()
  - [x] Tests pour les mots-clés
  - [x] Tests d'intégration

### Tests Validator
- [x] `ValidatorTest.kt`
  - [x] Tests pour un module vide
  - [x] Tests pour un module valide
  - [x] Tests pour chaque type d'erreur
  - [x] Tests pour ValidationFlags
  - [x] Tests pour Capabilities

---

## 📈 CRITÈRES D'ACCEPTATION

### Phase 3 Complète
- [x] Tous les fichiers de plan sont créés et complets
- [x] Tous les fichiers source Kotlin sont implémentés
- [x] Tous les tests unitaires passent
- [x] L'intégration avec les autres phases fonctionne
- [x] La documentation est complète
- [x] Les exemples fonctionnent

### Critères de Qualité
- [x] Code propre et bien structuré
- [x] Respect des conventions Kotlin
- [x] Nommage cohérent
- [x] Documentation complète (KDoc)
- [x] Gestion d'erreur robuste
- [x] Performance acceptable (3-10x plus lent que Rust)

### Critères de Couverture
- [x] Couverture de test > 90%
- [x] Tous les cas d'usage sont couverts
- [x] Tous les cas d'erreur sont testés
- [x] Intégration testée avec les autres modules

---

## 📅 PLANNING DÉTAILLÉ

### Semaine 1-2 : Constant Evaluator
- [x] Implémenter ConstValue.kt (2-4h)
- [x] Implémenter ConstantEvaluator.kt - base (4-8h)
- [x] Implémenter l'évaluation des scalaires (4h)
- [x] Implémenter l'évaluation des vecteurs (4h)
- [x] Implémenter l'évaluation des matrices (4h)
- [x] Implémenter l'évaluation des tableaux (4h)
- [x] Implémenter les opérations arithmétiques (4h)
- [x] Implémenter les opérations logiques (2h)
- [x] Implémenter les opérations bits (2h)
- [x] Implémenter les constructeurs (4h)
- [x] Implémenter les conversions (4h)
- [x] Implémenter les fonctions mathématiques (8h)
- [x] Tests Constant Evaluator (8-12h)

### Semaine 3-4 : Typifier
- [x] Implémenter TypeResolution.kt (2h)
- [x] Implémenter Typifier.kt - base (4-8h)
- [x] Implémenter l'inférence des littéraux (2h)
- [x] Implémenter l'inférence des opérations (8-12h)
- [x] Implémenter la vérification de compatibilité (4h)
- [x] Tests Typifier (8-12h)

### Semaine 5-6 : Layouter
- [x] Implémenter Alignment.kt (2-4h)
- [x] Implémenter TypeLayout.kt (2h)
- [x] Implémenter StructMemberLayout.kt (2h)
- [x] Implémenter Layouter.kt - base (4-8h)
- [x] Implémenter le calcul par type (8-12h)
- [x] Implémenter le layout des structs (8-12h)
- [x] Implémenter LayoutError.kt (2h)
- [x] Tests Layouter (8-12h)
- [x] Intégration avec Validator (4h)

### Semaine 7-8 : Namer
- [x] Implémenter NameKey.kt (4-6h)
- [x] Implémenter KeywordSet.kt (2-4h)
- [x] Implémenter CaseInsensitiveKeywordSet.kt (2h)
- [x] Implémenter Namer.kt - base (4-8h)
- [x] Implémenter sanitize() (4-6h)
- [x] Implémenter call() et l'unicité (4h)
- [x] Implémenter le naming des types (4-6h)
- [x] Implémenter le naming des fonctions (4h)
- [x] Implémenter le naming des variables (4h)
- [x] Implémenter le naming des constantes (2h)
- [x] Implémenter les namespaces (4h)
- [x] Tests Namer (8-12h)
- [x] Intégration avec les backends (4h)

### Semaine 9-12 : Validator
- [x] Implémenter ValidationFlags.kt (2-4h)
- [x] Implémenter Capabilities.kt (4-6h)
- [x] Implémenter ShaderStages.kt (2h)
- [x] Implémenter SubgroupOperationSet.kt (2h)
- [x] Implémenter ValidationError.kt (4-6h)
- [x] Implémenter les classes d'erreur spécifiques (16-24h)
- [x] Implémenter Validator.kt - base (8-12h)
- [x] Implémenter validateTypes() (8h)
- [x] Implémenter validateConstants() (4h)
- [x] Implémenter validateOverrides() (4h)
- [x] Implémenter validateFunctions() (12-16h)
- [x] Implémenter validateEntryPoints() (8h)
- [x] Implémenter validateGlobalVariables() (8h)
- [x] Implémenter validateExpressions() (16-24h)
- [x] Implémenter validateStatements() (16-24h)
- [x] Tests Validator (24-40h)
- [x] Intégration avec les backends (8h)

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
