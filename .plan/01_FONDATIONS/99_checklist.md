# ✅ Phase 1 : Checklist Complète - Fondations IR

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-core`  
**Phase** : 1 - Fondations  
**Durée** : 4-6 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique)  
**Statut Global** : [ ] 0% | [ ] 25% | [ ] 50% | [ ] 75% | [ ] 100%

---

## 📊 SOMMAIRE DE LA PHASE 1

| Sous-Phase | Durée | Fichiers | Statut | Progression |
|------------|-------|----------|--------|-------------|
| **1.0 - IR Structures** | 2-3 semaines | 8 fichiers | [ ] | 0% |
| **1.1 - Arena System** | 1-2 semaines | 4 fichiers | [ ] | 0% |
| **1.2 - Primitive Types** | 1-2 semaines | 2 fichiers | [ ] | 0% |
| **1.3 - Span & Diagnostics** | 1 semaine | 5 fichiers | [ ] | 0% |

**Total Phase 1** : **4-6 semaines** | **~19 fichiers** | **Progression Globale : 0%**

---

## 🎯 CHECKLIST GLOBALE PHASE 1

### ⬜ Sous-Phase 1.0 : Structures IR (2-3 semaines)

**Fichier** : `00_ir-structures.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Structures de Base
- [ ] `Module` (data class + Builder pattern)
- [ ] `Type` et `TypeInner` (sealed class avec tous les variants)
- [ ] `ScalarKind` (BOOL, SINT, UINT, FLOAT, ABSTRACT_INT)
- [ ] `VectorSize` (BI, TRI, QUAD)
- [ ] `AddressSpace` (10 valeurs)
- [ ] `ImageClass` (DEPTH, COLOR, STORAGE)
- [ ] `ImageDimension` (7 valeurs)
- [ ] `ImageAccess` (LOAD, STORE, LOAD_STORE)
- [ ] `Binding` (BuiltIn, Location, Resource)
- [ ] `BuiltIn` (50+ valeurs)

#### Expressions (60+ variants)
- [ ] `Expression` (sealed class)
- [ ] `Literal` (Bool, Sint, Uint, Float, AbstractInt, Vector)
- [ ] `Constant` et `Override`
- [ ] `ZeroValue`
- [ ] `Compose`
- [ ] `Access` et `AccessIndex`
- [ ] `Splat` et `Swizzle`
- [ ] `FunctionArgument`, `GlobalVariable`, `LocalVariable`
- [ ] `Load`
- [ ] `ImageSample` et `ImageLoad`
- [ ] `Unary` et `Binary`
- [ ] `Select` et `Relational`
- [ ] `Math`
- [ ] `As` (cast/bitcast)
- [ ] `Derivative`
- [ ] `CallResult` et `AtomicResult`
- [ ] `RayQueryProceedResult` (optionnel pour Phase 1)

#### Statements (25+ variants)
- [ ] `Statement` (sealed class)
- [ ] `Emit` (avec Range)
- [ ] `Block`
- [ ] `If` (condition, accept, reject)
- [ ] `Switch` (selector, cases)
- [ ] `Loop` (body, continuing, break_if)
- [ ] `Break` et `Continue`
- [ ] `Return` (avec valeur optionnelle)
- [ ] `Kill`
- [ ] `Store`
- [ ] `Call` (function, arguments, result)
- [ ] `Atomic`
- [ ] `RayQuery` (optionnel pour Phase 1)
- [ ] `ControlBarrier` et `MemoryBarrier`

#### Fonctions et Entry Points
- [ ] `Function` (name, arguments, result, local_variables, expressions, body)
- [ ] `FunctionArgument` (name, ty, binding)
- [ ] `FunctionResult` (ty, binding)
- [ ] `LocalVariable` (name, ty, init)
- [ ] `EntryPoint` (name, stage, early_depth_test, workgroup_size, function)
- [ ] `ShaderStage` (10 valeurs)
- [ ] `EarlyDepthTest` (FORCE, ALLOW)
- [ ] `ConservativeDepth` (3 valeurs)
- [ ] `MeshStageInfo` et `MeshOutputTopology`

#### Variables Globales
- [ ] `GlobalVariable` (name, class, binding, ty, init)
- [ ] `VariableClass` (UNIFORM, STORAGE, WORKGROUP, PRIVATE)
- [ ] `ResourceBinding` (Buffer, Texture, Sampler)
- [ ] `StorageAccess` (LOAD, STORE, LOAD_STORE)

#### Types Utilitaires
- [ ] `SpecialTypes` (tous les types prédéfinis)
- [ ] `DocComments` (arbre des commentaires)

#### Visitor Pattern
- [ ] `ExpressionVisitor<T>` (60+ méthodes)
- [ ] `StatementVisitor<T>` (25+ méthodes)
- [ ] Implémentation de `accept()` pour Expression et Statement

#### Tests
- [ ] Tests unitaires pour Module.Builder
- [ ] Tests unitaires pour tous les types
- [ ] Tests de sérialisation/désérialisation

#### Documentation
- [ ] KDoc pour toutes les classes
- [ ] KDoc pour toutes les propriétés
- [ ] Exemples d'utilisation

---

### ⬜ Sous-Phase 1.1 : Système Arena/Handle (1-2 semaines)

**Fichier** : `01_arena-system.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Arena System
- [ ] `Handle<T>` (@JvmInline value class, sérialisation)
- [ ] `Arena<T>` (MutableList wrapper, toutes les méthodes utilitaires)
  - [ ] `append(value)`
  - [ ] `appendAll(values)`
  - [ ] `get(handle)` et `getOrNull(handle)`
  - [ ] `forEachWithHandle`
  - [ ] `findHandle` et `findEntry`
  - [ ] `filter`, `filterWithHandle`
  - [ ] Opérateurs (`set`, `plusAssign`)
  - [ ] Implémentation de `Collection<T>` et `MutableCollection<T>`
- [ ] `UniqueArena<T>` (avec déduplication)
  - [ ] `append(value)` (retourne Handle existant si duplicate)
  - [ ] `contains(value)`
  - [ ] `findHandle(value)`
  - [ ] Interface `Equatable`
  - [ ] Implémentation pour `Type`
- [ ] `Range<T>` (pour Emit)
  - [ ] `start`, `endInclusive`, `count`
  - [ ] `isEmpty()`, `contains(index)`
  - [ ] Sérialisation

#### Fonctions Helpers
- [ ] `arenaOf()`, `arenaOf(vararg)`, `arenaOf(collection)`
- [ ] `uniqueArenaOf()`, `uniqueArenaOf(vararg)`, `uniqueArenaOf(collection)`
- [ ] `rangeOf(handle)`, `rangeOf(handles)`
- [ ] `invalidHandle()`

#### Tests
- [ ] `ArenaTest` (15+ tests)
- [ ] `UniqueArenaTest` (10+ tests)
- [ ] `HandleTest` (8+ tests)
- [ ] `RangeTest` (8+ tests)

#### Documentation
- [ ] KDoc complet pour toutes les classes et méthodes

---

### ⬜ Sous-Phase 1.2 : Types Primitifs (1-2 semaines)

**Fichier** : `02_primitive-types.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Types de Base
- [ ] `Bytes` (typealias UInt)
- [ ] `ScalarKind` (5 valeurs)
- [ ] `VectorSize` (3 valeurs)

#### Espaces et Classes
- [ ] `AddressSpace` (10 valeurs)
- [ ] `ImageClass` (3 valeurs)
- [ ] `ImageDimension` (7 valeurs)
- [ ] `ImageAccess` (3 valeurs)
- [ ] `VariableClass` (4 valeurs)

#### Binding et I/O
- [ ] `Binding` (BuiltIn, Location, Resource)
- [ ] `BuiltIn` (50+ valeurs - **Très important**)

#### Stages et Tests
- [ ] `ShaderStage` (10 valeurs)
- [ ] `EarlyDepthTest` (FORCE, ALLOW)
- [ ] `ConservativeDepth` (3 valeurs)
- [ ] `WorkgroupSize` (typealias List<UInt>)
- [ ] `MeshOutputTopology` (3 valeurs)

#### Opérateurs et Fonctions
- [ ] `UnaryOperator` (2 valeurs)
- [ ] `BinaryOperator` (15 valeurs)
- [ ] `RelationalFunction` (6 valeurs)
- [ ] `MathFunction` (50+ valeurs - **Très long**)
- [ ] `AtomicFunction` (11 valeurs)

#### Autres Types
- [ ] `ArraySize` (Constant, Dynamic, UN_SIZED)
- [ ] `StorageAccess` (3 valeurs)
- [ ] `SwizzleComponent` (8 valeurs)
- [ ] `DerivativeAxis` (2 valeurs)
- [ ] `SampleLevel` (4 variants)

#### Storage Format
- [ ] `StorageFormat` (50+ valeurs - **Très long**)

#### Tests
- [ ] Tests de sérialisation pour tous les enums
- [ ] Tests de comparaison

#### Documentation
- [ ] KDoc complet avec descriptions pour chaque valeur

---

### ⬜ Sous-Phase 1.3 : Span & Diagnostics (1 semaine)

**Fichier** : `03_span-diagnostics.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété | [ ] Validé

#### Span et SourceLocation
- [ ] `SourceLocation` (line, column, isValid, compareTo)
- [ ] `Span` (start, end, isValid, isSingleLine, merge, contains, overlaps)
- [ ] Opérateurs (`rangeTo`, `compareTo`)
- [ ] Fonctions helpers (`spanOf`, `toSpan`)
- [ ] `WithSpan` (interface)

#### Diagnostics
- [ ] `DiagnosticSeverity` (4 valeurs)
- [ ] `DiagnosticCode` (Parse, Validation, Warning, Custom)
- [ ] `Diagnostic` (severity, code, message, span, context)
- [ ] `DiagnosticList` (diagnostics, hasErrors, hasWarnings, filter, format)
- [ ] `DiagnosticBuilder` (error, warning, info, build)

#### Filtres de Diagnostic
- [ ] `DiagnosticAction` (3 valeurs)
- [ ] `DiagnosticFilter` (ByCode, BySeverity, BySpan, Custom)
- [ ] `DiagnosticFilterNode` (filter, children, span)
- [ ] Fonctions `apply()` pour les filtres

#### Gestion des Erreurs
- [ ] `NagaException` (message, diagnostics)
- [ ] `ParseError`, `ValidationError`, `TypeError`
- [ ] `Result<T, E>` (Ok, Err, isOk, isErr, unwrap, unwrapOr, getOrNull, map, flatMap)
- [ ] `NagaResult<T>` (typealias)
- [ ] Fonctions helpers (`ok`, `err`, `nagaErr`)

#### Namer
- [ ] `Namer` (nameExpression, nameLocalVariable, nameFunction, nameParameter)
- [ ] `NameKey` (5 variants)

#### Tests
- [ ] `SpanTest` (12+ tests)
- [ ] `DiagnosticTest` (5+ tests)
- [ ] `ErrorTest` (10+ tests)

#### Documentation
- [ ] KDoc complet

---

## 📁 STRUCTURE DES FICHIERS PHASE 1

```
naga-core/
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
- [ ] Tous les types de base sont implémentés
- [ ] Le système Arena/Handle fonctionne correctement
- [ ] Tous les enums sont implémentés avec toutes leurs valeurs
- [ ] Les Visitor Patterns sont implémentés et fonctionnels
- [ ] Les diagnostics et la gestion des erreurs fonctionnent

### 2. Tests
- [ ] Tous les tests unitaires passent
- [ ] Couverture de test > 90% pour le module naga-core
- [ ] Tests de sérialisation/désérialisation passent

### 3. Documentation
- [ ] KDoc pour toutes les classes publiques
- [ ] KDoc pour toutes les méthodes publiques
- [ ] Exemples d'utilisation dans la documentation

### 4. Validation
- [ ] Le module `naga-core` compile sans erreurs
- [ ] Tous les tests passent
- [ ] La sérialisation fonctionne pour tous les types
- [ ] Validation manuelle avec des exemples simples

---

## 🎯 LIVRABLES PHASE 1

1. **Module `naga-core`** complet et fonctionnel
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

- [ ] **Phase 1 est COMPLÈTE** ✅
- [ ] Prêt à passer à la **Phase 2** (Parser WGSL)
- [ ] Mettre à jour le statut dans le SUMMARY.md
- [ ] Célébrer ! 🎉

---

**Dernière mise à jour** : [Date]  
**Prochaine révision** : Après chaque sous-phase complétée
