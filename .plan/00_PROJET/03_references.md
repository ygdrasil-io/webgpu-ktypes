# 📚 Références Rust : Naga Source Code

**Projet** : WebGPU-KTypes Shader Transpiler  
**Code** : `naga-kt`  
**Version** : 1.0  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **⚠️ IMPORTANT** : Tous les chemins sont **absolus** et pointent vers :  
> `/Users/chaos/RustroverProjects/wgpu/naga/`

---

## 🎯 ORGANISATION PAR PRIORITÉ DE LECTURE

### 🔴 **PRIORITÉ MAXIMALE (À lire en premier - Core IR)**

Ces fichiers sont **essentiels** pour comprendre l'architecture et implémenter le core Kotlin.

| # | Fichier | Lignes | Description | Statut Kotlin |
|---|--------|--------|-------------|--------------|
| 1 | `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` | ~2900 | **COEUR ABSOLU** - Toutes les structures IR (Module, Type, Expression, Statement, Function, EntryPoint) | [ ] Non implémenté |
| 2 | `/Users/chaos/RustroverProjects/wgpu/naga/src/arena/mod.rs` | ~500 | Système Arena/Handle - Gestion mémoire optimisée | [ ] Non implémenté |
| 3 | `/Users/chaos/RustroverProjects/wgpu/naga/src/lib.rs` | ~160 | Point d'entrée, exports publics, types utilitaires | [ ] Non implémenté |

**Action requise** : Lire ces 3 fichiers **avant toute implémentation Kotlin**.

---

### 🟡 **PRIORITÉ ÉLEVÉE (À lire avant la phase correspondante)**

#### Frontend WGSL (Phase 2)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 4 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/mod.rs` | ~500 | Point d'entrée du frontend WGSL | [ ] | 2 |
| 5 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/lexer.rs` | ~800 | **Lexer WGSL** - Tokenization | [ ] | 2 |
| 6 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parser.rs` | ~2000 | **Parser WGSL** - Parsing récursif | [ ] | 2 |
| 7 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/mod.rs` | ~200 | Typifier et utilitaires frontends | [ ] | 2 |

#### Backend MSL (Phase 4 - Priorité pour Metal)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 8 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/msl/mod.rs` | ~500 | Point d'entrée MSL | [ ] | 4 |
| 9 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/msl/writer.rs` | ~3500 | **Writer MSL** - Génération de code Metal | [ ] | 4 |
| 10 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/msl/mesh_shader.rs` | ~300 | Support Mesh Shaders pour MSL | [ ] | 4 |
| 11 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/msl/ray.rs` | ~400 | Support Ray Tracing pour MSL | [ ] | 4 |

#### Backend HLSL (Phase 4 - Priorité pour DirectX)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 12 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/mod.rs` | ~800 | Point d'entrée HLSL | [ ] | 4 |
| 13 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/writer.rs` | ~4000 | **Writer HLSL** - Génération de code HLSL | [ ] | 4 |
| 14 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/mesh_shader.rs` | ~200 | Support Mesh Shaders pour HLSL | [ ] | 4 |
| 15 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/ray.rs` | ~500 | Support Ray Tracing pour HLSL | [ ] | 4 |
| 16 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/storage.rs` | ~400 | Gestion du storage pour HLSL | [ ] | 4 |
| 17 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/help.rs` | ~1000 | Fonctions helpers HLSL | [ ] | 4 |

#### Backend GLSL (Phase 4)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 18 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/mod.rs` | ~400 | Point d'entrée GLSL | [ ] | 4 |
| 19 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/writer.rs` | ~3000 | **Writer GLSL** - Génération de code GLSL | [ ] | 4 |
| 20 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/features.rs` | ~300 | Gestion des features GLSL | [ ] | 4 |
| 21 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/keywords.rs` | ~100 | Mots-clés réservés GLSL | [ ] | 4 |

#### Backend WGSL (Phase 4)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 22 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/mod.rs` | ~200 | Point d'entrée WGSL | [ ] | 4 |
| 23 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/writer.rs` | ~2500 | **Writer WGSL** - Génération de code WGSL | [ ] | 4 |
| 24 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/polyfill/mod.rs` | ~500 | Polyfills pour WGSL | [ ] | 4 |

---

### 🟢 **PRIORITÉ MOYENNE (À lire pour les phases avancées)**

#### Processing (Phase 3)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 25 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/mod.rs` | ~400 | Exports publics du module proc | [ ] | 3 |
| 26 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/constant_evaluator.rs` | ~3000 | **Évaluateur d'expressions constantes** | [ ] | 3 |
| 27 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/typifier.rs` | ~1000 | Résolution et inférence de types | [ ] | 3 |
| 28 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/layouter.rs` | ~1500 | Calcul de layout mémoire | [ ] | 3 |
| 29 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/namer.rs` | ~800 | Nommage automatique des variables | [ ] | 3 |
| 30 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/index.rs` | ~500 | Gestion des indexations | [ ] | 3 |

#### Validation (Phase 5)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 31 | `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/mod.rs` | ~800 | Validator principal, ModuleInfo | [ ] | 5 |
| 32 | `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/expression.rs` | ~1500 | Validation des expressions | [ ] | 5 |
| 33 | `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/function.rs` | ~1000 | Validation des fonctions | [ ] | 5 |
| 34 | `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/type.rs` | ~800 | Validation des types | [ ] | 5 |
| 35 | `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/interface.rs` | ~600 | Validation de l'interface (I/O) | [ ] | 5 |
| 36 | `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/analyzer.rs` | ~1000 | Analyse de l'uniformité (control flow) | [ ] | 5 |

---

### ⚪ **PRIORITÉ FAIBLE (Pour référence ou futur)**

#### Overloads System (Phase 3 - Optionnel pour MVP)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 37 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/overloads/mod.rs` | ~300 | Exports publics overloads | [ ] | 3 |
| 38 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/overloads/rule.rs` | ~400 | Règles de surcharge | [ ] | 3 |
| 39 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/overloads/regular.rs` | ~800 | Fonctions régulières | [ ] | 3 |
| 40 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/overloads/scalar_set.rs` | ~500 | Fonctions scalaires | [ ] | 3 |
| 41 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/overloads/constructor_set.rs` | ~300 | Constructeurs | [ ] | 3 |
| 42 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/overloads/mathfunction.rs` | ~600 | Fonctions mathématiques | [ ] | 3 |
| 43 | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/overloads/utils.rs` | ~200 | Utilitaires overloads | [ ] | 3 |

#### Frontend GLSL (Futur)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 44 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/glsl/mod.rs` | ~1500 | Point d'entrée GLSL | [ ] | Futur |
| 45 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/glsl/parser/` | ~5000 | Parseur GLSL (multiple fichiers) | [ ] | Futur |
| 46 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/glsl/ast.rs` | ~1000 | AST GLSL | [ ] | Futur |
| 47 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/glsl/builtins.rs` | ~800 | Fonctions built-in GLSL | [ ] | Futur |

#### Frontend SPIR-V (Futur)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 48 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/spv/mod.rs` | ~800 | Point d'entrée SPIR-V | [ ] | Futur |
| 49 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/spv/function.rs` | ~1500 | Parsing des fonctions SPIR-V | [ ] | Futur |
| 50 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/spv/image.rs` | ~500 | Gestion des images SPIR-V | [ ] | Futur |
| 51 | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/spv/convert.rs` | ~2000 | Conversion SPIR-V → IR | [ ] | Futur |

#### Backend SPIR-V (Futur)

| # | Fichier | Lignes | Description | Statut | Phase |
|---|--------|--------|-------------|--------|-------|
| 52 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/spv/mod.rs` | ~1000 | Point d'entrée SPIR-V | [ ] | Futur |
| 53 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/spv/writer.rs` | ~3000 | Writer SPIR-V binaire | [ ] | Futur |
| 54 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/spv/layout.rs` | ~800 | Layout SPIR-V | [ ] | Futur |
| 55 | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/spv/instructions.rs` | ~2000 | Génération instructions SPIR-V | [ ] | Futur |

---

## 📁 ORGANISATION PAR DOMAINE FONCTIONNEL

### 1. **Types et Structures de Données** (Phase 1)

```
Rust Naga → Kotlin Mapping
├── src/ir/mod.rs          ← TODOS les types IR principaux
│   ├── Module             → data class Module
│   ├── Type               → data class Type (avec TypeInner)
│   ├── TypeInner          → sealed class TypeInner
│   ├── Expression          → sealed class Expression
│   ├── Statement           → sealed class Statement
│   ├── Function            → data class Function
│   ├── EntryPoint          → data class EntryPoint
│   ├── Scalar              → data class Scalar
│   ├── VectorSize          → enum class VectorSize
│   ├── ShaderStage         → enum class ShaderStage
│   ├── AddressSpace        → enum class AddressSpace
│   ├── BuiltIn             → enum class BuiltIn
│   └── ... (50+ autres types)
│
├── src/arena/mod.rs        ← Système de gestion mémoire
│   ├── Arena<T>            → class Arena<T>
│   ├── UniqueArena<T>      → class UniqueArena<T>
│   └── Handle<T>           → value class Handle<T> (inline)
│
└── src/span.rs             ← Positions source
    ├── Span                → data class Span
    ├── SourceLocation      → data class SourceLocation
    └── WithSpan            → interface WithSpan
```

**Fichiers à lire** : #1, #2, #3

---

### 2. **Parsing WGSL** (Phase 2)

```
WGSL Frontend
├── src/front/wgsl/mod.rs         ← Point d'entrée, typifier
├── src/front/wgsl/lexer.rs       ← Lexer (tokenization)
│   ├── Token                  → enum class Token
│   ├── TokenKind              → enum class TokenKind
│   └── Lexer                  → class Lexer
│
├── src/front/wgsl/parser.rs      ← Parser principal
│   ├── Parser                 → class Parser
│   ├── parse_module()         → fun parseModule(): Module
│   ├── parse_function()       → fun parseFunction(): Function
│   ├── parse_expression()     → fun parseExpression(): Expression
│   └── parse_statement()      → fun parseStatement(): Statement
│
└── src/front/type_gen.rs         ← Génération de types partagés
```

**Fichiers à lire** : #4, #5, #6, #7

---

### 3. **Traitement IR** (Phase 3)

```
IR Processing
├── src/proc/constant_evaluator.rs  ← Évaluation des constantes
│   ├── ConstantEvaluator         → class ConstantEvaluator
│   ├── evaluate()                → fun evaluate(expr: Expression): Literal?
│   └── try_eval_and_append_impl() → Complexe - cœur de l'évaluation
│
├── src/proc/typifier.rs            ← Résolution de types
│   ├── Typifier                  → class Typifier
│   └── TypeResolution           → data class TypeResolution
│
├── src/proc/layouter.rs            ← Layout mémoire
│   ├── Layouter                  → class Layouter
│   ├── LayoutError               → sealed class LayoutError
│   └── TypeLayout                → data class TypeLayout
│
└── src/proc/overloads/             ← Système de surcharge
    ├── OverloadSet              → class OverloadSet
    ├── Rule                     → data class Rule
    └── Conclusion                → enum class Conclusion
```

**Fichiers à lire** : #25-#43

---

### 4. **Validation** (Phase 5)

```
Validation
├── src/valid/mod.rs               ← Validator principal
│   ├── Validator                  → class Validator
│   ├── ValidationFlags           → bitflags ValidationFlags
│   ├── Capabilities               → bitflags Capabilities
│   └── ModuleInfo                → data class ModuleInfo
│
├── src/valid/expression.rs        ← Validation expressions
├── src/valid/function.rs          ← Validation fonctions
├── src/valid/type.rs              ← Validation types
├── src/valid/interface.rs         ← Validation I/O
└── src/valid/analyzer.rs          ← Analyse control flow
```

**Fichiers à lire** : #31-#36

---

### 5. **Backends** (Phase 4)

#### Architecture Commune
```
Backends
└── src/back/mod.rs                ← Types/utilitaires partagés
    ├── COMPONENTS                → const val COMPONENTS
    ├── INDENT                   → const val INDENT
    ├── NeedBakeExpressions      → typealias Set<Handle<Expression>>
    └── PipelineConstants        → typealias Map<String, Double>
```

#### MSL Writer
```
MSL Backend
├── src/back/msl/mod.rs            ← Point d'entrée
├── src/back/msl/writer.rs          ← Writer principal
│   ├── Writer                    → class Writer
│   ├── write_module()            → fun writeModule()
│   ├── write_function()          → fun writeFunction()
│   └── write_expression()        → fun writeExpression()
│
├── src/back/msl/keywords.rs        ← Mots-clés MSL
└── src/back/msl/sampler.rs        ← Gestion samplers
```

#### HLSL Writer
```
HLSL Backend
├── src/back/hlsl/mod.rs            ← Point d'entrée
├── src/back/hlsl/writer.rs          ← Writer principal
├── src/back/hlsl/help.rs            ← Fonctions helpers
├── src/back/hlsl/storage.rs         ← Gestion storage
├── src/back/hlsl/mesh_shader.rs     ← Mesh shaders
└── src/back/hlsl/ray.rs             ← Ray tracing
```

#### GLSL Writer
```
GLSL Backend
├── src/back/glsl/mod.rs            ← Point d'entrée
├── src/back/glsl/writer.rs          ← Writer principal
├── src/back/glsl/features.rs        ← Features GLSL
└── src/back/glsl/keywords.rs        ← Mots-clés GLSL
```

**Fichiers à lire** : #8-#24

---

## 🔍 CARTES MENTALES POUR COMPRENDRE L'ARCHITECTURE

### Carte 1 : Flux Principal (WGSL → MSL)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        WGSL Source Code                                │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Phase 2 : PARSING                                  │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐               │
│  │   Lexer     │───▶│   Parser    │───▶│  Type Gen   │               │
│  │ (lexer.rs)  │    │ (parser.rs) │    │ (type_gen.rs)│               │
│  └─────────────┘    └─────────────┘    └─────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        IR Module                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    Module                                      │    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────┐      │    │
│  │  │  types  │  │constants │  │ functions│  │ entry_points│      │    │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────────┘      │    │
│  │  ┌─────────────────────────────────────────────────────────┐│    │
│  │  │              Arena<Expression>                             ││    │
│  │  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌───────────┐   ││    │
│  │  │  │Literal │  │Binary  │  │Load    │  │Call      │   ││    │
│  │  │  └────────┘  └────────┘  └────────┘  └───────────┘   ││    │
│  │  └─────────────────────────────────────────────────────────┘│    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Phase 3 : PROCESSING                               │
│  ┌─────────────────────┐    ┌─────────────────────┐                 │
│  │  ConstantEvaluator   │    │       Typifier        │                 │
│  │ (constant_evaluator)│    │      (typifier)       │                 │
│  └─────────────────────┘    └─────────────────────┘                 │
│  ┌─────────────────────┐    ┌─────────────────────┐                 │
│  │      Layouter        │    │       Validator      │                 │
│  │      (layouter)      │    │      (valid/)        │                 │
│  └─────────────────────┘    └─────────────────────┘                 │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Phase 4 : BACKEND                                  │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    MSL Writer                                  │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │    │
│  │  │  write_     │  │  write_     │  │  write_     │           │    │
│  │  │  module()   │─▶│  function() │─▶│  expression()│           │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘           │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        MSL Source Code                                │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Carte 2 : Structure du Module IR

```
┌──────────────────────────────────────────────────────────────────────┐
│                           Module                                         │
├──────────────────────────────────────────────────────────────────────┤
│  types: UniqueArena<Type>                                                │
│  constants: Arena<Constant>                                             │
│  global_variables: Arena<GlobalVariable>                               │
│  functions: Arena<Function>                                            │
│  entry_points: List<EntryPoint>                                        │
│  global_expressions: Arena<Expression>                                 │
│  special_types: SpecialTypes                                           │
│  capabilities: Capabilities                                            │
└──────────────────────────────────────────────────────────────────────┘
                              │
                              ├─────────────────┬─────────────────┐
                              ▼                 ▼                 ▼
┌─────────────────────────┐ ┌─────────────┐ ┌─────────────────┐
│        Type             │ │  Constant   │ │   Function       │
├─────────────────────────┤ ├─────────────┤ ├─────────────────┤
│  inner: TypeInner       │ │  name       │ │   name           │
│  span: Span             │ │  value      │ │   arguments      │
└─────────────────────────┘ │  span       │ │   result         │
                           └─────────────┘ │   local_vars    │
                                                 │   expressions   │
                                                 │   body          │
                                                 └─────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        TypeInner                                        │
├──────────────────────────────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────┐    │
│  │ Scalar  │ │ Vector  │ │ Matrix  │ │ Array   │ │ Struct      │    │
│  │         │ │         │ │         │ │         │ │             │    │
│  │ kind    │ │ inner   │ │ inner   │ │ inner   │ │ members     │    │
│  │ width   │ │ size    │ │ columns │ │ length  │ │ span        │    │
│  └─────────┘ │         │ │ rows    │ │ stride  │ └─────────────┘    │
│              └─────────┘ └─────────┘ └─────────┘                │
│  ┌─────────┐ ┌─────────────────────┐ ┌─────────────────┐          │
│  │ Pointer │ │ sampler           │ │ Image           │          │
│  │         │ │                     │ │                 │          │
│  │ base    │ │ class: ImageClass │ │ class: ImageClass│          │
│  │ space   │ │ dim: ImageDim     │ │ dim: ImageDim   │          │
│  └─────────┘ └─────────────────────┘ └─────────────────┘          │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Carte 3 : Hiérarchie Expression

```
┌──────────────────────────────────────────────────────────────────────┐
│                      Expression (Sealed Class)                           │
├──────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │
│  │   Literal   │ │  Constant   │ │ ZeroValue   │ │   Compose   │    │
│  │             │ │             │ │             │ │             │    │
│  │ value: T   │ │ handle     │ │ handle     │ │ ty         │    │
│  └─────────────┘ └─────────────┘ └─────────────┘ │ components │    │
│                                                  └─────────────┘    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │
│  │    Access    │ │AccessIndex  │ │    Splat    │ │   Swizzle   │    │
│  │             │ │             │ │             │ │             │    │
│  │ base        │ │ base        │ │ size       │ │ size       │    │
│  │ index       │ │ index: u32  │ │ value      │ │ vector     │    │
│  └─────────────┘ └─────────────┘ └─────────────┘ │ pattern    │    │
│                                                  └─────────────┘    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │
│  │FunctionArg   │ │GlobalVariable│ │ LocalVariable│ │    Load     │    │
│  │             │ │             │ │             │ │             │    │
│  │ index: u32  │ │ handle      │ │ handle      │ │ pointer    │    │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │
│  │   Unary     │ │   Binary    │ │   Select    │ │ Relational  │    │
│  │             │ │             │ │             │ │             │    │
│  │ op          │ │ op          │ │ condition  │ │ op         │    │
│  │ expr        │ │ left        │ │ accept     │ │ left       │    │
│  │             │ │ right       │ │ reject     │ │ right      │    │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────────────┐    │
│  │    Math     │ │  Derivative  │ │               ImageSample            │    │
│  │             │ │             │ │                                     │    │
│  │ func       │ │ axis        │ │ image, sampler, coordinate, level  │    │
│  │ args       │ │ expr        │ │                                     │    │
│  └─────────────┘ └─────────────┘ └─────────────────────────────────┘    │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │                    ... 30+ autres variants                           ││
│  └─────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────┘
```

---

## 📖 ALGORITHMES CLÉS À COMPRENDRE

### 1. **Arena/Handle System** (Fichier #2)

**Concept** : Éviter les allocations individuelles et les pointeurs bruts.

```rust
// Rust
pub struct Arena<T> {
    data: Vec<T>,
}

pub struct Handle<T>(u32);

impl<T> Arena<T> {
    pub fn append(&mut self, value: T) -> Handle<T> {
        let index = self.data.len();
        self.data.push(value);
        Handle(index as u32)
    }
    
    pub fn get(&self, handle: Handle<T>) -> &T {
        &self.data[handle.0 as usize]
    }
}
```

**Kotlin Equivalent :**
```kotlin
// Option 1: Typealias simple (moins safe)
typealias Handle<T> = Int

class Arena<T> {
    private val data = mutableListOf<T>()
    fun append(value: T): Handle<T> = data.size.also { data.add(value) }
    operator fun get(handle: Handle<T>): T = data[handle]
}

// Option 2: Value class (type-safe, inline)
@JvmInline
value class Handle<T>(val index: Int)
```

---

### 2. **Pattern Matching → Visitor Pattern** (Fichier #1)

**Rust** utilise `match` exhaustif :
```rust
match expression {
    Expression::Literal(lit) => { /* ... */ }
    Expression::Binary { op, left, right } => { /* ... */ }
    Expression::Load { pointer } => { /* ... */ }
    // ... 60+ variants
}
```

**Kotlin** utilise le Visitor Pattern :
```kotlin
sealed class Expression {
    // ... variants
    
    fun <T> accept(visitor: ExpressionVisitor<T>): T
}

interface ExpressionVisitor<T> {
    fun visitLiteral(expr: Expression.Literal): T
    fun visitBinary(expr: Expression.Binary): T
    fun visitLoad(expr: Expression.Load): T
    // ... 60+ méthodes
}

// Utilisation
val result = expression.accept(object : ExpressionVisitor<String> {
    override fun visitLiteral(expr: Expression.Literal): String = "Literal"
    override fun visitBinary(expr: Expression.Binary): String = "Binary"
    // ...
})
```

---

### 3. **Type Resolution** (Fichier #27)

**Concept** : Résoudre les types des expressions pendant le parsing ou le traitement.

**Algorithme clé dans Rust :**
- `Typifier::grow()` - Résout les types jusqu'à une expression donnée
- `TypeResolution` - Représentation légère d'un type résolu

**À porter en Kotlin :**
- Créer un `Typifier` qui prend un `Module` et résout les types
- Utiliser un cache pour éviter la réévaluation

---

### 4. **Constant Evaluation** (Fichier #26)

**Concept** : Évaluer les expressions constantes à la "compilation" (quand le shader est parsed).

**Algorithme :**
1. Parcourir toutes les expressions du module
2. Pour chaque expression, essayer de l'évaluer
3. Si réussie, stocker le résultat dans une cache
4. Si une expression dépend d'une non-évaluable, marquer comme non-constante

**Expressions constantes WGSL :**
- Literals
- Constants
- ZeroValue
- Compose (de constantes)
- Access/AccessIndex (sur constantes)
- Splat (de constantes)
- Swizzle (de constantes)
- Unary/Binary (sur constantes)
- Select (avec condition constante)
- Math (sur constantes)
- As (cast/bitcast de constantes)

---

### 5. **MSL Code Generation** (Fichier #9)

**Concepts clés :**
- **Nommage** : Préfixer les variables temporaires avec `_eN`
- **Layout** : Calculer l'offset des membres de struct
- **Types** : Mapper les types WGSL → MSL
- **Functions** : Générer les fonctions avec la bonne signature
- **Entry Points** : Ajouter les attributs Metal spécifiques

**Exemple de mapping :**
```
WGSL: vec4<f32> → MSL: float4
WGSL: mat4x4<f32> → MSL: float4x4
WGSL: array<T, N> → MSL: T[N]
WGSL: @location(0) → MSL: [[location(0)]]
```

---

## 🎯 RÉSUMÉ DES FICHIERS À LIRE PAR PHASE

| Phase | Fichiers Rust à Lire | Temps Estimé | Priorité |
|-------|---------------------|---------------|----------|
| **Phase 0** | #1, #2, #3 | 1-2 jours | ⭐⭐⭐⭐⭐ |
| **Phase 1** | #1, #2, #3 (relecture) | 2-3 jours | ⭐⭐⭐⭐⭐ |
| **Phase 2** | #4, #5, #6, #7 | 3-5 jours | ⭐⭐⭐⭐⭐ |
| **Phase 3** | #25, #26, #27, #28 | 3-4 jours | ⭐⭐⭐⭐ |
| **Phase 4** | #8-#11 (MSL), puis #12-#17 (HLSL) | 5-7 jours | ⭐⭐⭐⭐ |
| **Phase 5** | #31-#36 | 2-3 jours | ⭐⭐⭐ |

---

## 🔗 COMMANDES UTILES POUR EXPLORER LE CODE RUST

```bash
# Depuis /Users/chaos/RustroverProjects/wgpu/naga/

# Lister les fichiers par taille
find src -name "*.rs" -exec wc -l {} \; | sort -nr | head -20

# Chercher une fonction spécifique
grep -rn "fn parse_expression" src/front/wgsl/

# Chercher un type spécifique
grep -rn "pub struct Module" src/

# Chercher des usages de Handle
grep -rn "Handle<" src/ir/mod.rs | head -20

# Voir la structure des enums
grep -n "^pub enum" src/ir/mod.rs

# Voir la structure des structs
grep -n "^pub struct" src/ir/mod.rs

# Compter les variants d'Expression
grep -A1 "^pub enum Expression" src/ir/mod.rs | grep -c "=" || echo "N/A"
```

---

## 📝 NOTES POUR LA LECTURE

1. **Commencer par `ir/mod.rs`** - C'est le cœur du système
2. **Prendre des notes** sur les structures de données et leurs relations
3. ** Dessiner des diagrammes** pour comprendre les hiérarchies
4. **Comparer avec la spec WGSL** : https://gpuweb.github.io/gpuweb/wgsl/
5. **Tester avec des exemples** : Regarder les fichiers dans `tests/in/wgsl/`

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Lire les fichiers **#1, #2, #3** (Core IR) en détail
2. [ ] Créer un **diagramme des classes** pour le Module IR
3. [ ] Commencer à **mapper Rust → Kotlin** pour les structures de base
4. [ ] Passer au fichier `01_FONDATIONS/00_ir-structures.md` pour l'implémentation
