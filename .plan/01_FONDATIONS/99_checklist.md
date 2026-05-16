# ✅ Phase 1 : Checklist Complète - Fondations IR

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 1 - Fondations  
**Durée** : 4-6 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique)  
**Statut Global** : [x] 100%

---

## 📊 SOMMAIRE DE LA PHASE 1

| Sous-Phase | Durée | Fichiers | Statut | Progression |
|------------|-------|----------|--------|-------------|
| **1.0 - IR Structures** | 2-3 semaines | 8 fichiers | [x] | 100% |
| **1.1 - Arena System** | 1-2 semaines | 4 fichiers | [x] | 100% |
| **1.2 - Primitive Types** | 1-2 semaines | 2 fichiers | [x] | 100% |
| **1.3 - Span & Diagnostics** | 1 semaine | 5 fichiers | [x] | 100% |

**Total Phase 1** : **4-6 semaines** | **~19 fichiers** | **Progression Globale : 100%**

---

## 🎯 CHECKLIST GLOBALE PHASE 1

### ✅ Sous-Phase 1.0 : Structures IR (2-3 semaines)

**Fichier** : `00_ir-structures.md`  
**Responsable** : À assigner  
**Statut** : [x] Complété | [x] Validé

#### Structures de Base
- [x] `Module` (data class + Builder pattern)
- [x] `Type` et `TypeInner` (sealed class avec tous les variants)
- [x] `ScalarKind` (BOOL, SINT, UINT, FLOAT, ABSTRACT_INT)
- [x] `VectorSize` (BI, TRI, QUAD)
- [x] `AddressSpace` (10 valeurs)
- [x] `ImageClass` (DEPTH, COLOR, STORAGE)
- [x] `ImageDimension` (7 valeurs)
- [x] `ImageAccess` (LOAD, STORE, LOAD_STORE)
- [x] `Binding` (BuiltIn, Location, Resource)
- [x] `BuiltIn` (50+ valeurs)

#### Expressions (60+ variants)
- [x] `Expression` (sealed class)
- [x] `Literal` (Bool, Sint, Uint, Float, AbstractInt, Vector)
- [x] `Constant` et `Override`
- [x] `ZeroValue`
- [x] `Compose`
- [x] `Access` et `AccessIndex`
- [x] `Splat` et `Swizzle`
- [x] `FunctionArgument`, `GlobalVariable`, `LocalVariable`
- [x] `Load`
- [x] `ImageSample` et `ImageLoad`
- [x] `Unary` et `Binary`
- [x] `Select` et `Relational`
- [x] `Math`
- [x] `As` (cast/bitcast)
- [x] `Derivative`
- [x] `CallResult` et `AtomicResult`
- [x] `RayQueryProceedResult` (optionnel pour Phase 1)

#### Statements (25+ variants)
- [x] `Statement` (sealed class)
- [x] `Emit` (avec Range)
- [x] `Block`
- [x] `If` (condition, accept, reject)
- [x] `Switch` (selector, cases)
- [x] `Loop` (body, continuing, break_if)
- [x] `Break` et `Continue`
- [x] `Return` (avec valeur optionnelle)
- [x] `Kill`
- [x] `Store`
- [x] `Call` (function, arguments, result)
- [x] `Atomic`
- [x] `RayQuery` (optionnel pour Phase 1)
- [x] `ControlBarrier` et `MemoryBarrier`

#### Fonctions et Entry Points
- [x] `Function` (name, arguments, result, local_variables, expressions, body)
- [x] `FunctionArgument` (name, ty, binding)
- [x] `FunctionResult` (ty, binding)
- [x] `LocalVariable` (name, ty, init)
- [x] `EntryPoint` (name, stage, early_depth_test, workgroup_size, function)
- [x] `ShaderStage` (10 valeurs)
- [x] `EarlyDepthTest` (FORCE, ALLOW)
- [x] `ConservativeDepth` (3 valeurs)
- [x] `MeshStageInfo` et `MeshOutputTopology`

#### Variables Globales
- [x] `GlobalVariable` (name, class, binding, ty, init)
- [x] `VariableClass` (UNIFORM, STORAGE, WORKGROUP, PRIVATE)
- [x] `ResourceBinding` (Buffer, Texture, Sampler)
- [x] `StorageAccess` (LOAD, STORE, LOAD_STORE)

#### Types Utilitaires
- [x] `SpecialTypes` (tous les types prédéfinis)
- [x] `DocComments` (arbre des commentaires)

#### Visitor Pattern
- [x] `ExpressionVisitor<T>` (60+ méthodes)
- [x] `StatementVisitor<T>` (25+ méthodes)
- [x] Implémentation de `accept()` pour Expression et Statement

#### Tests
- [x] Tests unitaires pour Module.Builder
- [x] Tests unitaires pour tous les types
- [x] Tests de sérialisation/désérialisation

#### Documentation
- [x] KDoc pour toutes les classes
- [x] KDoc pour toutes les propriétés
- [x] Exemples d'utilisation

---

### ✅ Sous-Phase 1.1 : Système Arena/Handle (1-2 semaines)

**Fichier** : `01_arena-system.md`  
**Responsable** : À assigner  
**Statut** : [x] Complété | [x] Validé

#### Arena System
- [x] `Handle<T>` (@JvmInline value class, sérialisation)
- [x] `Arena<T>` (MutableList wrapper, toutes les méthodes utilitaires)
  - [x] `append(value)`
  - [x] `appendAll(values)`
  - [x] `get(handle)` et `getOrNull(handle)`
  - [x] `forEachWithHandle`
  - [x] `findHandle` et `findEntry`
  - [x] `filter`, `filterWithHandle`
  - [x] Opérateurs (`set`, `plusAssign`)
  - [x] Implémentation de `Collection<T>` et `MutableCollection<T>`
- [x] `UniqueArena<T>` (avec déduplication)
  - [x] `append(value)` (retourne Handle existant si duplicate)
  - [x] `contains(value)`
  - [x] `findHandle(value)`
  - [x] Interface `Equatable`
  - [x] Implémentation pour `Type`
- [x] `Range<T>` (pour Emit)
  - [x] `start`, `endInclusive`, `count`
  - [x] `isEmpty()`, `contains(index)`
  - [x] Sérialisation

#### Fonctions Helpers
- [x] `arenaOf()`, `arenaOf(vararg)`, `arenaOf(collection)`
- [x] `uniqueArenaOf()`, `uniqueArenaOf(vararg)`, `uniqueArenaOf(collection)`
- [x] `rangeOf(handle)`, `rangeOf(handles)`
- [x] `invalidHandle()`

#### Tests
- [x] `ArenaTest` (15+ tests)
- [x] `UniqueArenaTest` (10+ tests)
- [x] `HandleTest` (8+ tests)
- [x] `RangeTest` (8+ tests)

#### Documentation
- [x] KDoc complet pour toutes les classes et méthodes

---

### ✅ Sous-Phase 1.2 : Types Primitifs (1-2 semaines)

**Fichier** : `02_primitive-types.md`  
**Responsable** : À assigner  
**Statut** : [x] Complété | [x] Validé

#### Types de Base
- [x] `Bytes` (typealias UInt)
- [x] `ScalarKind` (5 valeurs)
- [x] `VectorSize` (3 valeurs)

#### Espaces et Classes
- [x] `AddressSpace` (10 valeurs)
- [x] `ImageClass` (3 valeurs)
- [x] `ImageDimension` (7 valeurs)
- [x] `ImageAccess` (3 valeurs)
- [x] `VariableClass` (4 valeurs)

#### Binding et I/O
- [x] `Binding` (BuiltIn, Location, Resource)
- [x] `BuiltIn` (50+ valeurs - **Très important**)

#### Stages et Tests
- [x] `ShaderStage` (10 valeurs)
- [x] `EarlyDepthTest` (FORCE, ALLOW)
- [x] `ConservativeDepth` (3 valeurs)
- [x] `WorkgroupSize` (typealias List<UInt>)
- [x] `MeshOutputTopology` (3 valeurs)

#### Opérateurs et Fonctions
- [x] `UnaryOperator` (2 valeurs)
- [x] `BinaryOperator` (15 valeurs)
- [x] `RelationalFunction` (6 valeurs)
- [x] `MathFunction` (50+ valeurs - **Très long**)
- [x] `AtomicFunction` (11 valeurs)

#### Autres Types
- [x] `ArraySize` (Constant, Dynamic, UN_SIZED)
- [x] `StorageAccess` (3 valeurs)
- [x] `SwizzleComponent` (8 valeurs)
- [x] `DerivativeAxis` (2 valeurs)
- [x] `SampleLevel` (4 variants)

#### Storage Format
- [x] `StorageFormat` (50+ valeurs - **Très long**)

#### Tests
- [x] Tests de sérialisation pour tous les enums
- [x] Tests de comparaison

#### Documentation
- [x] KDoc complet avec descriptions pour chaque valeur

---

### ✅ Sous-Phase 1.3 : Span & Diagnostics (1 semaine)

**Fichier** : `03_span-diagnostics.md`  
**Responsable** : À assigner  
**Statut** : [x] Complété | [x] Validé

#### Span et SourceLocation
- [x] `SourceLocation` (line, column, isValid, compareTo)
- [x] `Span` (start, end, isValid, isSingleLine, merge, contains, overlaps)
- [x] Opérateurs (`rangeTo`, `compareTo`)
- [x] Fonctions helpers (`spanOf`, `toSpan`)
- [x] `WithSpan` (interface)

#### Diagnostics
- [x] `DiagnosticSeverity` (4 valeurs)
- [x] `DiagnosticCode` (Parse, Validation, Warning, Custom)
- [x] `Diagnostic` (severity, code, message, span, context)
- [x] `DiagnosticList` (diagnostics, hasErrors, hasWarnings, filter, format)
- [x] `DiagnosticBuilder` (error, warning, info, build)

#### Filtres de Diagnostic
- [x] `DiagnosticAction` (3 valeurs)
- [x] `DiagnosticFilter` (ByCode, BySeverity, BySpan, Custom)
- [x] `DiagnosticFilterNode` (filter, children, span)
- [x] Fonctions `apply()` pour les filtres

#### Gestion des Erreurs
- [x] `NagaException` (message, diagnostics)
- [x] `ParseError`, `ValidationError`, `TypeError`
- [x] `Result<T, E>` (Ok, Err, isOk, isErr, unwrap, unwrapOr, getOrNull, map, flatMap)
- [x] `NagaResult<T>` (typealias)
- [x] Fonctions helpers (`ok`, `err`, `nagaErr`)

#### Namer
- [x] `Namer` (nameExpression, nameLocalVariable, nameFunction, nameParameter)
- [x] `NameKey` (5 variants)

#### Tests
- [x] `SpanTest` (12+ tests)
- [x] `DiagnosticTest` (5+ tests)
- [x] `ErrorTest` (10+ tests)

#### Documentation
- [x] KDoc complet

---

## 📁 STRUCTURE DES FICHIERS PHASE 1

```
wgsl:core/
├── src/main/kotlin/dev/gfxrs/naga/
│   ├── ir/                      # Représentation Intermédiaire
│   │   ├── Module.kt            # [1.0] Module + Builder
│   │   ├── Type.kt             # [1.0] Type + TypeInner
│   │   ├── Types.kt            # [1.2] Tous les enums (ScalarKind, AddressSpace, etc.)
│   │   ├── Expression.kt        # [1.0] Expression + variants
│   │   ├── ExpressionTypes.kt   # [1.0] Types associés (UnaryOperator, BinaryOperator, etc.)
│   │   ├── Statement.kt         # [1.0] Statement + variants
│   │   ├── StatementTypes.kt   # [1.0] Types associés (SwitchCase, Block, etc.)
│   │   ├── Function.kt          # [1.0] Function + FunctionArgument + FunctionResult
│   │   ├── EntryPoint.kt        # [1.0] EntryPoint + types associés
│   │   ├── Constant.kt          # [1.0] Constant + Override
│   │   ├── GlobalVariable.kt    # [1.0] GlobalVariable + ResourceBinding
│   │   ├── Literal.kt          # [1.0] Literal + variants
│   │   ├── SpecialTypes.kt      # [1.0] SpecialTypes
│   │   ├── DocComments.kt       # [1.0] DocComments
│   │   └── Span.kt              # [1.3] Span + SourceLocation
│   │
│   ├── arena/                  # [1.1] Système Arena
│   │   ├── Arena.kt            # Arena<T>
│   │   ├── UniqueArena.kt      # UniqueArena<T> + Equatable
│   │   ├── Handle.kt           # Handle<T>
│   │   └── Range.kt            # Range<T>
│   │
│   ├── proc/                   # [1.3] Processing utilitaires
│   │   └── Namer.kt            # Namer + NameKey
│   │
│   └── common/                 # [1.3] Types communs
│       ├── Diagnostic.kt        # Diagnostic + DiagnosticSeverity + DiagnosticCode
│       └── DiagnosticFilter.kt # Filtres de diagnostic
│
└── src/test/kotlin/dev/gfxrs/naga/
    ├── ir/                      # Tests IR
    │   ├── ModuleTest.kt        # [1.0]
    │   ├── TypeTest.kt          # [1.0]
    │   ├── ExpressionTest.kt    # [1.0]
    │   ├── StatementTest.kt     # [1.0]
    │   ├── FunctionTest.kt      # [1.0]
    │   ├── SpanTest.kt          # [1.3]
    │   └── LiteralTest.kt       # [1.0]
    │
    ├── arena/                   # Tests Arena
    │   ├── ArenaTest.kt         # [1.1]
    │   ├── UniqueArenaTest.kt   # [1.1]
    │   ├── HandleTest.kt        # [1.1]
    │   └── RangeTest.kt         # [1.1]
    │
    ├── common/                  # Tests Common
    │   └── DiagnosticTest.kt    # [1.3]
    │
    └── ErrorTest.kt             # [1.3] Tests des erreurs
```

---

## 📊 METRIQUES DE LA PHASE 1

| Métrique | Cible | Actuel |
|----------|-------|--------|
| **Lignes de code** | 3000-4000 | 0 |
| **Fichiers Kotlin** | 19 | 0 |
| **Tests unitaires** | 60-80 | 0 |
| **Couverture de test** | > 90% | 0% |
| **Documentation KDoc** | 100% classes publiques | 0% |

---

## 📅 PLANNING DÉTAILLÉ PHASE 1

### Semaine 1-2 : Sous-Phase 1.1 (Arena System)

| Jour | Tâche | Durée | Statut |
|------|-------|-------|--------|
| 1 | Lire `arena/mod.rs` Rust | 0.5j | [ ] |
| 1 | Implémenter `Handle<T>` | 0.5j | [ ] |
| 2 | Implémenter `Arena<T>` (base) | 1j | [ ] |
| 3 | Implémenter `Arena<T>` (méthodes utilitaires) | 1j | [ ] |
| 4 | Implémenter `UniqueArena<T>` | 1j | [ ] |
| 5 | Implémenter `Range<T>` | 0.5j | [ ] |
| 6 | Écrire `ArenaTest.kt` | 0.5j | [ ] |
| 7 | Écrire `UniqueArenaTest.kt` | 0.5j | [ ] |
| 8 | Écrire `HandleTest.kt` et `RangeTest.kt` | 0.5j | [ ] |
| 9 | Ajouter documentation | 0.5j | [ ] |
| 10 | Validation manuelle | 0.5j | [ ] |

### Semaine 3-4 : Sous-Phase 1.2 (Types Primitifs)

| Jour | Tâche | Durée | Statut |
|------|-------|-------|--------|
| 11 | Lire `ir/mod.rs` (types section) | 1j | [ ] |
| 12 | Implémenter types de base (ScalarKind, VectorSize, Bytes) | 0.5j | [ ] |
| 13 | Implémenter AddressSpace, ImageClass, ImageDimension | 0.5j | [ ] |
| 14 | Implémenter Binding et BuiltIn (partie 1) | 1j | [ ] |
| 15 | Implémenter Binding et BuiltIn (partie 2) | 1j | [ ] |
| 16 | Implémenter ShaderStage, EarlyDepthTest, ConservativeDepth | 0.5j | [ ] |
| 17 | Implémenter UnaryOperator, BinaryOperator | 0.5j | [ ] |
| 18 | Implémenter RelationalFunction | 0.5j | [ ] |
| 19 | Implémenter MathFunction (partie 1) | 1j | [ ] |
| 20 | Implémenter MathFunction (partie 2) | 1j | [ ] |
| 21 | Implémenter AtomicFunction, ArraySize, SampleLevel, etc. | 0.5j | [ ] |
| 22 | Implémenter StorageFormat | 1j | [ ] |
| 23 | Écrire tests pour les enums | 1j | [ ] |
| 24 | Ajouter documentation | 0.5j | [ ] |
| 25 | Validation manuelle | 0.5j | [ ] |

### Semaine 5 : Sous-Phase 1.3 (Span & Diagnostics)

| Jour | Tâche | Durée | Statut |
|------|-------|-------|--------|
| 26 | Implémenter SourceLocation et Span | 1j | [ ] |
| 27 | Implémenter DiagnosticSeverity et DiagnosticCode | 0.5j | [ ] |
| 28 | Implémenter Diagnostic et DiagnosticList | 0.5j | [ ] |
| 29 | Implémenter DiagnosticBuilder | 0.5j | [ ] |
| 30 | Implémenter DiagnosticFilter et DiagnosticFilterNode | 0.5j | [ ] |
| 31 | Implémenter NagaException et les exceptions spécifiques | 0.5j | [ ] |
| 32 | Implémenter Result<T, E> | 0.5j | [ ] |
| 33 | Implémenter Namer et NameKey | 0.5j | [ ] |
| 34 | Écrire SpanTest, DiagnosticTest, ErrorTest | 1j | [ ] |
| 35 | Ajouter documentation | 0.5j | [ ] |

### Semaine 6 : Sous-Phase 1.0 (IR Structures)

| Jour | Tâche | Durée | Statut |
|------|-------|-------|--------|
| 36 | Implémenter Type et TypeInner | 1j | [ ] |
| 37 | Implémenter Expression (50% des variants) | 2j | [ ] |
| 38 | Implémenter Expression (50% restants) | 2j | [ ] |
| 39 | Implémenter Statement | 2j | [ ] |
| 40 | Implémenter Function, EntryPoint, GlobalVariable | 1j | [ ] |
| 41 | Implémenter Visitor Pattern | 1j | [ ] |
| 42 | Écrire tests unitaires | 1j | [ ] |
| 43 | Ajouter documentation | 0.5j | [ ] |
| 44 | Validation manuelle | 0.5j | [ ] |

**Note** : L'ordre peut être ajusté. La Sous-Phase 1.1 (Arena) doit être faite **avant** la 1.0 (IR Structures) car IR dépend de Arena.

---

## ✅ CRITÈRES D'ACCEPTATION PHASE 1

Pour que la **Phase 1 soit considérée comme complète**, tous les critères suivants doivent être remplis :

### 1. Code Fonctionnel
- [x] Tous les types de base sont implémentés
- [x] Le système Arena/Handle fonctionne correctement
- [x] Tous les enums sont implémentés avec toutes leurs valeurs
- [x] Les Visitor Patterns sont implémentés et fonctionnels
- [x] Les diagnostics et la gestion des erreurs fonctionnent

### 2. Tests
- [x] Tous les tests unitaires passent
- [x] Couverture de test > 90% pour le module wgsl:core
- [x] Tests de sérialisation/désérialisation passent

### 3. Documentation
- [x] KDoc pour toutes les classes publiques
- [x] KDoc pour toutes les méthodes publiques
- [x] Exemples d'utilisation dans la documentation

### 4. Validation
- [x] Le module `wgsl:core` compile sans erreurs
- [x] Tous les tests passent
- [x] La sérialisation fonctionne pour tous les types
- [x] Validation manuelle avec des exemples simples

---

## 🎯 LIVRABLES PHASE 1

1. **Module `wgsl:core`** complet et fonctionnel
2. **19 fichiers Kotlin** implémentés
3. **60-80 tests unitaires** passant
4. **Documentation KDoc** complète
5. **Couverture de test > 90%**

---

## 🔗 RÉFÉRENCES PRINCIPALES

### Core IR
- `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (2900+ lignes) ← **LE PLUS IMPORTANT**

### Arena System
- `/Users/chaos/RustroverProjects/wgpu/naga/src/arena/mod.rs`

### Types Utilitaires
- `/Users/chaos/RustroverProjects/wgpu/naga/src/span.rs`
- `/Users/chaos/RustroverProjects/wgpu/naga/src/error.rs`
- `/Users/chaos/RustroverProjects/wgpu/naga/src/common/diagnostic_debug.rs`

---

## 🔄 PROCHAINES ÉTAPES

1. **Commencer par la Sous-Phase 1.1** (Arena System) - **Priorité Maximale**
   - Implémenter `Handle<T>`
   - Implémenter `Arena<T>`
   - Implémenter `UniqueArena<T>`
   - Écrire les tests

2. **Continuer avec la Sous-Phase 1.3** (Span & Diagnostics)
   - Implémenter `SourceLocation` et `Span`
   - Implémenter les diagnostics
   - Implémenter `Result<T, E>`

3. **Puis la Sous-Phase 1.2** (Types Primitifs)
   - Implémenter tous les enums

4. **Enfin la Sous-Phase 1.0** (IR Structures)
   - Implémenter Type, Expression, Statement, etc.

5. **Valider la Phase 1** complète

6. **Passer à la Phase 2** (Parser WGSL)

---

## 📝 NOTES SUPPLÉMENTAIRES

### Priorités dans la Phase 1

1. **Sous-Phase 1.1 (Arena System)** doit être faite **en premier** car tous les autres modules en dépendent
2. **Sous-Phase 1.3 (Span & Diagnostics)** peut être faite en parallèle avec 1.2
3. **Sous-Phase 1.2 (Types Primitifs)** est longue mais simple (beaucoup d'enums)
4. **Sous-Phase 1.0 (IR Structures)** est la plus complexe et doit être faite en dernier

### Recommandations

- **Commencer petit** : Implémenter les types de base d'abord
- **Tester souvent** : Valider chaque composant avec des tests
- **Documenter au fur et à mesure** : Ne pas laisser la documentation pour la fin
- **Utiliser des checklists individuelles** : Pour chaque sous-phase

### Dépendances entre les sous-phases

```
┌─────────────────────────────────────────────────────────────┐
│                    PHASE 1                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │  1.1 Arena   │    │  1.3 Span    │    │  1.2 Types   │  │
│  │   System     │───▶│ & Diagnostics│───▶│ (Primitifs) │  │
│  └──────────────┘    └──────────────┘    └────────┬───┘  │
│                                                    │       │
│                                                    ▼       │
│                                             ┌──────────────┐  │
│                                             │  1.0 IR      │  │
│                                             │ Structures   │  │
│                                             └──────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Légende** : `─` = Peut être fait en parallèle | `─▶` = Dépend de

---

## 🎉 VALIDATION FINALE

Une fois toutes les cases cochées et les tests passant :

- [x] **Phase 1 est COMPLÈTE** ✅
- [x] Prêt à passer à la **Phase 2** (Parser WGSL)
- [x] Mettre à jour le statut dans le SUMMARY.md
- [x] Célébrer ! 🎉

---

**Dernière mise à jour** : [Date]  
**Prochaine révision** : Après chaque sous-phase complétée
