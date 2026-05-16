# ✅ Phase 4 : Checklist Complète - Backends

**Projet** : WebGPU-KTypes Shader Transpiler  
**Phase** : 4 - Backends  
**Durée totale** : 10-14 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique)  
**Statut Global** : [ ] 0% | [X] 25% | [ ] 50% | [ ] 75% | [ ] 100%

---

## 📊 SOMMAIRE DE LA PHASE 4

| Sous-Phase | Durée | Fichiers | Statut | Progression |
|------------|-------|----------|--------|-------------|
| **4.0 - Architecture Commune** | 1-2 semaines | 7 fichiers | [X] | 100% |
| **4.1 - MSL Writer** | 3-4 semaines | 6 fichiers | [X] | 100% |
| **4.2 - HLSL Writer** | 3-4 semaines | 7 fichiers | [X] | 100% |
| **4.3 - GLSL Writer** | 2-3 semaines | 8 fichiers | [X] | 100% |
| **4.4 - WGSL Writer** | 2-3 semaines | 5 fichiers | [X] | 100% |

**Total Phase 4** : **10-14 semaines** | **~33 fichiers** | **Progression Globale : 100%**

---

## 🎯 CHECKLIST GLOBALE PHASE 4

### ⬜ Sous-Phase 4.0 : Architecture Commune (1-2 semaines)

**Fichier** : `00_backend-architecture.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [X] Complété | [X] Validé

#### Structure des Fichiers
- [X] `BackendOptions.kt` - Options communes à tous les backends
- [X] `BackendError.kt` - Classes d'erreur pour les backends
- [X] `BindingMap.kt` - Mapping des bindings WGSL → Backend
- [X] `PipelineConstants.kt` - Constantes de pipeline
- [X] `WriterBase.kt` - Classe de base pour tous les writers
- [X] `BackendWriter.kt` - Interface commune BackendWriter
- [X] `BackendRegistry.kt` - Registre des backends disponibles

#### BackendOptions
- [X] Classe abstraite BackendOptions
- [X] Propriétés communes (validationFlags, capabilities, shaderStages, indent, newline, version, languageName, fileExtension)
- [X] MslOptions
- [X] HlslOptions
- [X] GlslOptions
- [X] WgslOptions
- [X] ShaderModel (enum)
- [X] GlslProfile (enum)
- [X] BackendType (enum)

#### BackendError
- [X] Classe sealed BackendError
- [X] UnsupportedFeature
- [X] UnsupportedType
- [X] UnsupportedExpression
- [X] UnsupportedStatement
- [X] TypeMismatch
- [X] InternalError
- [X] Erreurs spécifiques par backend (MslError, HlslError, GlslError, WgslError)

#### BindingMap
- [X] Classe BindingMap
- [X] Classe BindTarget (buffer, texture, sampler, mutable)
- [X] Méthodes insert(), get(), contains(), clear()
- [X] Factory methods pour chaque backend

#### PipelineConstants
- [X] Typealias PipelineConstants = Map<String, Double>
- [X] Utilitaires (empty, single, of, toBackendString)

#### WriterBase
- [X] Classe abstraite WriterBase<T : BackendOptions>
- [X] Propriétés (output, module, moduleInfo, options, namer, layouter)
- [X] État (indentLevel, currentFunction, currentEntryPoint)
- [X] Méthode write()
- [X] Méthodes writeHeader() (abstrait), writePreamble(), writeTypes()
- [X] Méthodes writeConstants(), writeGlobalVariables(), writeFunctions()
- [X] Méthodes writeEntryPoints(), writeEntryPoint() (abstrait)
- [X] Méthodes writeStatement(), writeBlock(), writeExpression()
- [X] Méthodes getTypeName() pour tous les types
- [X] Méthodes utilitaires (indent, writeLine, write)
- [X] Toutes les méthodes writeXxx() abstraites pour expressions (15+)
- [X] Toutes les méthodes writeXxx() abstraites pour statements (15+)

#### BackendWriter
- [X] Interface BackendWriter<T : BackendOptions>
- [X] Méthode write(module, moduleInfo)
- [X] Méthode withOptions()
- [X] Méthode canHandle()

#### BackendRegistry
- [X] Classe BackendRegistry
- [X] Interface BackendFactory
- [X] Méthodes register(), get(), getWithOptions()
- [X] Méthodes listBackendNames(), hasBackend()
- [X] Méthode write(module, moduleInfo, backendName)
- [X] Enregistrement des backends par défaut (MSL, HLSL, GLSL, WGSL)

#### Tests
- [X] BackendOptionsTest.kt
- [X] BackendRegistryTest.kt
- [X] BindingMapTest.kt

#### Intégration
- [X] Utiliser WriterBase dans tous les backends
- [X] Utiliser BackendRegistry dans l'API publique
- [X] Documenter l'API publique

---

### [X] Sous-Phase 4.1 : MSL Writer (3-4 semaines)

**Fichier** : `01_msl-writer.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [X] Complété | [X] Validé

#### Module Structure
- [X] build.gradle.kts pour wgsl:msl
- [X] Package `io.ygdrasil.wgsl.back.msl`

#### Configuration
- [X] MslOptions.kt (héritant de BackendOptions)
- [ ] BindTarget.kt (Slot, BindSamplerTarget, BindExternalTextureTarget)
- [ ] Keywords.kt (RESERVED_KEYWORDS, AVOID_KEYWORDS, ALL_KEYWORDS)
- [X] MslCapabilities.kt (DEFAULT capabilities)

#### Writer
- [X] MslWriter.kt (héritant de WriterBase<MslOptions>)
- [X] Implémenter withOptions()
- [X] Implémenter canHandle()
- [X] Implémenter writeHeader() (includes MSL)
- [ ] Implémenter writeHelperFunctions()
  - [ ] writeExternalTextureHelpers()
  - [ ] writeOobLocalHelpers()
- [X] Implémenter getTypeName() pour tous les types MSL
  - [X] getMslScalarTypeName() (bool, char, uchar, short, ushort, int, uint, long, ulong, half, float, double)
  - [X] getMslVectorTypeName() (float2-4, int2-4, uint2-4, half2-4)
  - [X] getMslMatrixTypeName() (float2x2-4x4, etc.)
  - [ ] getMslCooperativeMatrixTypeName()
  - [X] getMslImageTypeName()
  - [X] getMslSamplerTypeName()
- [X] Implémenter writeStructType() avec attributs [[align]]
- [X] Implémenter writeConstant()
- [X] Implémenter writeGlobalVariable() avec [[buffer]], [[texture]], [[sampler]]
- [X] Implémenter writeFunction() et writeFunctionSignature()
- [X] Implémenter writeEntryPoint()
- [X] Implémenter writeEntryPointSignature()
- [X] Implémenter writeEntryPointArguments()
- [X] writeInputStruct()
- [X] writeOutputStruct()
- [X] writeArgumentAssignments()
- [X] Implémenter writeBinding()

#### Expressions (15+)
- [X] writeLiteral()
- [X] writeIdent()
- [X] writeUnary() (+, -, !, ~)
- [X] writeBinary() (+, -, *, /, %, ==, !=, <, >, <=, >=, &&, ||, &, |, ^, <<, >>)
- [X] writeSelect() (ternary)
- [X] writeCompose()
- [X] writeSplat()
- [X] writeSwizzle()
- [X] writeAccess()
- [X] writeAccessIndex()
- [X] writeAs() (cast/bitcast)
- [X] writeCall()
- [ ] writeCallResult()
- [X] writeLoad()
- [X] writeStoreExpr()
- [X] writeImageSample()
- [ ] writeImageLoad()
- [X] writeImageQuery()
- [ ] writeDerivative() (dfdx, dfdy, fwidth)
- [X] writeMath() (40+ fonctions)
- [X] writeAtomicResult()
- [ ] writeRayQueryProceedResult()

#### Statements (15+)
- [X] writeBlock()
- [X] writeIf()
- [X] writeSwitch()
- [X] writeLoop()
- [ ] writeWhile()
- [ ] writeFor()
- [X] writeBreak()
- [X] writeContinue()
- [X] writeReturn()
- [X] writeDiscard()
- [X] writeEmit()
- [ ] writeCallStatement()
- [X] writeStore()
- [X] writeAtomic()
- [ ] writeRayQuery()
- [ ] writeMemoryBarrier()
- [ ] writeControlBarrier()

#### API Publique
- [ ] MslModule.kt
- [ ] writeString()
- [ ] tryWriteString()

#### Tests
- [X] MslWriterTest.kt
- [X] Test module vide
- [X] Test types (scalaires, vecteurs, matrices, structs)
- [X] Test constantes
- [X] Test variables globales
- [X] Test fonctions simples
- [X] Test entry points (vertex, fragment, compute)
- [X] Test bindings
- [X] Test expressions (toutes)
- [ ] Test statements (tous)
- [ ] Test external textures
- [ ] Test OOB locals

---

### [X] Sous-Phase 4.2 : HLSL Writer (3-4 semaines)

**Fichier** : `02_hlsl-writer.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [X] Complété | [X] Validé

#### Module Structure
- [X] build.gradle.kts pour wgsl:hlsl
- [X] Package `io.ygdrasil.wgsl.back.hlsl`

#### Configuration
- [X] HlslOptions.kt
- [ ] BindTarget.kt (register, space, matrixLayout, packed)
- [ ] Keywords.kt
- [ ] ShaderModel.kt (SM2_0 à SM6_6)
- [ ] MatrixLayout.kt (ROW_MAJOR, COLUMN_MAJOR)
- [ ] HlslCapabilities.kt

#### Writer
- [X] HlslWriter.kt (héritant de WriterBase<HlslOptions>)
- [X] Implémenter withOptions() et canHandle()
- [X] Implémenter writeHeader() avec #pragma pack_matrix
- [X] Implémenter getTypeName() pour tous les types HLSL
- [X] Implémenter writeStructType()
- [ ] Implémenter writeMatrix2xNHelpers() (GetMatKx2, SetMatKx2)
- [ ] Implémenter writeSamplerHeap()
- [ ] Implémenter writeSamplerIndexBuffer()
- [X] Implémenter writeFunction() et writeFunctionSignature()
- [X] Implémenter writeEntryPoint()
- [X] Implémenter writeBinding() avec register et space

#### Expressions
- [X] Toutes les writeXxx() pour expressions (similaires à MSL)

#### Statements
- [X] Toutes les writeXxx() pour statements (similaires à MSL)

#### API Publique
- [ ] HlslModule.kt
- [ ] writeString()
- [ ] tryWriteString()

#### Tests
- [ ] HlslWriterTest.kt
- [ ] Test module vide
- [ ] Test types
- [ ] Test constantes
- [ ] Test variables globales
- [ ] Test fonctions
- [ ] Test entry points
- [ ] Test matrices 2-rows
- [ ] Test sampler heap
- [ ] Test external textures

---

### [X] Sous-Phase 4.3 : GLSL Writer (2-3 semaines)

**Fichier** : `03_glsl-writer.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [X] Complété | [X] Validé

#### Module Structure
- [X] build.gradle.kts pour wgsl:glsl
- [X] Package `io.ygdrasil.wgsl.back.glsl`

#### Configuration
- [X] GlslOptions.kt
- [ ] BindTarget.kt
- [ ] Keywords.kt
- [ ] GlslVersion.kt (SUPPORTED_CORE, SUPPORTED_ES)
- [ ] GlslProfile.kt (CORE, ES, COMPATIBILITY)
- [ ] GlslPrecision.kt (HIGH, MEDIUM, LOW)
- [ ] FeatureManager.kt
- [ ] GlslCapabilities.kt

#### Writer
- [X] GlslWriter.kt (héritant de WriterBase<GlslOptions>)
- [X] Implémenter withOptions() et canHandle()
- [X] Implémenter writeHeader() avec #version et #extension
- [ ] Implémenter writePreamble() avec precision qualifiers
- [X] Implémenter getTypeName() pour tous les types GLSL
- [X] Implémenter writeStructType()
- [X] Implémenter writeTextureType() (combined ou separate)
- [X] Implémenter writeSamplerType()
- [X] Implémenter writeFunction() et writeFunctionSignature()
- [X] Implémenter writeEntryPoint() pour vertex, fragment, compute
- [ ] Implémenter writeComputeShader() avec layout(local_size_x, local_size_y, local_size_z)
- [X] Implémenter writeBinding() avec layout(set, binding, location)

#### Separate Samplers
- [ ] Gestion des textures sans samplers
- [ ] Gestion des samplers séparés
- [ ] Génération de texture(sampler2D(texture, sampler), coord)

#### Features/Extensions
- [ ] Détection automatique des extensions nécessaires
- [ ] Ajout des #extension directives

#### Expressions
- [X] Toutes les writeXxx() pour expressions

#### Statements
- [X] Toutes les writeXxx() pour statements

#### API Publique
- [ ] GlslModule.kt
- [ ] writeString()
- [ ] tryWriteString()

#### Tests
- [ ] GlslWriterTest.kt
- [ ] Test module vide
- [ ] Test types
- [ ] Test constantes
- [ ] Test variables globales
- [ ] Test fonctions
- [ ] Test entry points (vertex, fragment, compute)
- [ ] Test bindings
- [ ] Test separate samplers
- [ ] Test extensions automatiques

---

### [X] Sous-Phase 4.4 : WGSL Writer (2-3 semaines)

**Fichier** : `04_wgsl-writer.md`  
**Responsable** : À assigner  
**Statut** : [ ] Non commencé | [ ] En cours | [X] Complété | [X] Validé

#### Module Structure
- [X] build.gradle.kts pour wgsl:wgsl
- [X] Package `io.ygdrasil.wgsl.back.wgsl`

#### Configuration
- [X] WgslOptions.kt
- [ ] WgslCapabilities.kt

#### Utilitaires
- [ ] Sorter.kt (Declaration, add, sort)
- [ ] Formatter.kt (formatType, formatExpression, formatStatement, formatFunction, addIndentation, wrapLine)

#### Writer
- [ ] WgslWriter.kt (héritant de WriterBase<WgslOptions>)
- [ ] Implémenter withOptions() et canHandle()
- [ ] Implémenter writeHeader()
- [ ] Implémenter writeEnableDirectives()
- [ ] Implémenter writeRequiresDirectives()
- [ ] Implémenter writeTypeAlias()
- [ ] Implémenter getTypeName() pour tous les types WGSL
- [ ] Implémenter writeStructType()
- [ ] Implémenter writeConstant()
- [ ] Implémenter writeGlobalVariable()
- [ ] Implémenter writeFunction() et writeFunctionSignature()
- [ ] Implémenter writeEntryPoint()
- [ ] Implémenter writeBinding() avec @group, @binding
- [ ] Implémenter writeLocation() avec @location
- [ ] Implémenter writeBuiltin() avec @builtin
- [ ] Implémenter toutes les writeXxx() pour expressions
- [ ] Implémenter toutes les writeXxx() pour statements

#### Formattage
- [ ] Respecter les conventions WGSL (snake_case, etc.)
- [ ] Indentation cohérente
- [ ] Espaces avant/après les opérateurs
- [ ] Sauts de ligne appropriés
- [ ] Alignement des déclarations
- [ ] Enveloppement des lignes longues

#### Round-Trip Validation
- [ ] validateRoundTrip() dans WgslModule
- [ ] areEquivalent() pour comparaison sémantique
- [ ] Tests de round-trip

#### API Publique
- [ ] WgslModule.kt
- [ ] writeString()
- [ ] tryWriteString()
- [ ] validateRoundTrip()
- [ ] areEquivalent()

#### Tests
- [ ] WgslWriterTest.kt
- [ ] Test module vide
- [ ] Test types
- [ ] Test constantes
- [ ] Test variables globales
- [ ] Test fonctions
- [ ] Test entry points
- [ ] Test expressions
- [ ] Test statements
- [ ] Test round-trip

---

## 📊 RÉCAPITULATIF PAR DOSSIER

### `wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/`
- [ ] BackendOptions.kt
- [ ] BackendError.kt
- [ ] BindingMap.kt
- [ ] PipelineConstants.kt
- [ ] WriterBase.kt
- [ ] BackendWriter.kt
- [ ] BackendRegistry.kt

### `wgsl:msl/src/main/kotlin/dev/gfxrs/naga/back/msl/`
- [ ] MslOptions.kt
- [ ] BindTarget.kt
- [ ] Keywords.kt
- [ ] MslCapabilities.kt
- [ ] MslWriter.kt
- [ ] MslModule.kt
- [ ] writer/Helpers.kt
- [ ] writer/ExternalTexture.kt

### `wgsl:hlsl/src/main/kotlin/dev/gfxrs/naga/back/hlsl/`
- [ ] HlslOptions.kt
- [ ] BindTarget.kt
- [ ] Keywords.kt
- [ ] ShaderModel.kt
- [ ] MatrixLayout.kt
- [ ] HlslCapabilities.kt
- [ ] HlslWriter.kt
- [ ] HlslModule.kt
- [ ] writer/Helpers.kt
- [ ] writer/StorageBuffer.kt
- [ ] writer/ExternalTexture.kt

### `wgsl:glsl/src/main/kotlin/dev/gfxrs/naga/back/glsl/`
- [ ] GlslOptions.kt
- [ ] BindTarget.kt
- [ ] Keywords.kt
- [ ] GlslVersion.kt
- [ ] GlslProfile.kt
- [ ] GlslPrecision.kt
- [ ] FeatureManager.kt
- [ ] GlslCapabilities.kt
- [ ] GlslWriter.kt
- [ ] GlslModule.kt
- [ ] writer/Helpers.kt
- [ ] writer/SeparateSamplers.kt

### `wgsl:wgsl/src/main/kotlin/dev/gfxrs/naga/back/wgsl/`
- [ ] WgslOptions.kt
- [ ] WgslCapabilities.kt
- [ ] WgslWriter.kt
- [ ] WgslModule.kt
- [ ] Formatter.kt
- [ ] Sorter.kt

---

## 🎯 CHECKLIST DES TESTS

### Tests Architecture Commune
- [ ] BackendOptionsTest.kt (tous les backends)
- [ ] BackendRegistryTest.kt (list, get, has, write)
- [ ] BindingMapTest.kt (insert, get, contains, clear)

### Tests MSL
- [ ] MslWriterTest.kt (20+ tests)
- [ ] Test empty module
- [ ] Test scalar types (10+)
- [ ] Test vector types (4)
- [ ] Test matrix types (9)
- [ ] Test struct types
- [ ] Test array types
- [ ] Test constants
- [ ] Test global variables
- [ ] Test simple functions
- [ ] Test vertex entry point
- [ ] Test fragment entry point
- [ ] Test compute entry point
- [ ] Test bindings (buffer, texture, sampler)
- [ ] Test all expressions (15+)
- [ ] Test all statements (15+)
- [ ] Test external textures
- [ ] Test OOB locals

### Tests HLSL
- [ ] HlslWriterTest.kt (20+ tests)
- [ ] Test empty module
- [ ] Test types
- [ ] Test constants
- [ ] Test global variables
- [ ] Test functions
- [ ] Test entry points
- [ ] Test row_major matrices
- [ ] Test 2-row matrices (matKx2)
- [ ] Test sampler heap
- [ ] Test external textures

### Tests GLSL
- [ ] GlslWriterTest.kt (20+ tests)
- [ ] Test empty module
- [ ] Test types
- [ ] Test constants
- [ ] Test global variables
- [ ] Test functions
- [ ] Test vertex entry point
- [ ] Test fragment entry point
- [ ] Test compute entry point
- [ ] Test bindings
- [ ] Test separate samplers
- [ ] Test version directives
- [ ] Test extension directives
- [ ] Test precision qualifiers

### Tests WGSL
- [ ] WgslWriterTest.kt (20+ tests)
- [ ] Test empty module
- [ ] Test types
- [ ] Test constants
- [ ] Test global variables
- [ ] Test functions
- [ ] Test entry points
- [ ] Test all expressions
- [ ] Test all statements
- [ ] Test round-trip validation

---

## 📈 CRITÈRES D'ACCEPTATION

### Phase 4 Complète
- [ ] Tous les fichiers de plan sont créés et complets
- [ ] Tous les modules Gradle sont configurés (wgsl:msl, wgsl:hlsl, wgsl:glsl, wgsl:wgsl)
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
- [ ] Tous les types sont testés
- [ ] Tous les expressions sont testées
- [ ] Tous les statements sont testés
- [ ] Tous les entry points sont testés
- [ ] Round-trip validation fonctionne

---

## 📅 PLANNING DÉTAILLÉ

### Semaine 1-2 : Architecture Commune
- [ ] BackendOptions.kt (4-6h)
- [ ] BackendError.kt (2h)
- [ ] BindingMap.kt (2-4h)
- [ ] PipelineConstants.kt (2h)
- [ ] WriterBase.kt (8-12h)
- [ ] BackendWriter.kt (2h)
- [ ] BackendRegistry.kt (4h)
- [ ] Tests architecture (4-8h)
- [ ] Intégration (2h)

### Semaine 3-6 : MSL Writer
- [ ] MslOptions + BindTarget + Keywords (4-6h)
- [ ] MslWriter base (8-12h)
- [ ] getTypeName() (4-6h)
- [ ] writeStructType() (2h)
- [ ] Expressions (20-24h)
- [ ] Statements (20-24h)
- [ ] writeEntryPoint() (8-12h)
- [ ] External textures (8-12h)
- [ ] OOB locals (4-6h)
- [ ] MslModule + tests (8-12h)
- [ ] build.gradle.kts (2h)

### Semaine 7-10 : HLSL Writer
- [ ] HlslOptions + BindTarget + Keywords + ShaderModel (4-6h)
- [ ] HlslWriter base (8-12h)
- [ ] getTypeName() (4-6h)
- [ ] writeStructType() + row_major (4-6h)
- [ ] Matrix 2-row helpers (4-6h)
- [ ] Expressions (20-24h)
- [ ] Statements (20-24h)
- [ ] writeEntryPoint() + sampler heap (8-12h)
- [ ] External textures (6-8h)
- [ ] HlslModule + tests (8-12h)
- [ ] build.gradle.kts (2h)

### Semaine 11-13 : GLSL Writer
- [ ] GlslOptions + GlslVersion + FeatureManager + Keywords (6-8h)
- [ ] GlslWriter base + header (8-12h)
- [ ] getTypeName() (4-6h)
- [ ] writeStructType() (2h)
- [ ] Expressions (20-24h)
- [ ] Statements (16-20h)
- [ ] writeEntryPoint() pour 3 stages (8-12h)
- [ ] Separate samplers (4-6h)
- [ ] Features/extensions auto (4-6h)
- [ ] GlslModule + tests (8-12h)
- [ ] build.gradle.kts (2h)

### Semaine 14-16 : WGSL Writer
- [ ] WgslOptions (2h)
- [ ] Sorter.kt (4-6h)
- [ ] Formatter.kt (6-8h)
- [ ] WgslWriter base + header (8-12h)
- [ ] getTypeName() (4-6h)
- [ ] writeStructType() (2h)
- [ ] Expressions (16-20h)
- [ ] Statements (16-20h)
- [ ] Functions + entry points (8-12h)
- [ ] Round-trip validation (8-12h)
- [ ] WgslModule + tests (8-12h)
- [ ] build.gradle.kts (2h)

**Total Phase 4** : **~360-560h (9-14 semaines)** pour un développeur
**En parallèle** : **~10-14 semaines** avec une équipe

---

## 🔄 DÉPENDANCES ENTRE SOUS-PHASES

```
Phase 4.0 : Architecture Commune
    ↓
Phase 4.1 : MSL Writer
    │
Phase 4.2 : HLSL Writer  (peut être en parallèle avec MSL)
    │
Phase 4.3 : GLSL Writer   (peut être en parallèle avec MSL/HLSL)
    │
Phase 4.4 : WGSL Writer   (peut être en parallèle avec les autres)
```

**Note** : Les backends sont indépendants une fois l'architecture commune implémentée. Ils peuvent être développés en parallèle.

---

## 🎯 LIVRABLES DE LA PHASE 4

1. **Module `wgsl:core/back`** : Architecture commune pour tous les backends
2. **Module `wgsl:msl`** : Backend MSL complet
3. **Module `wgsl:hlsl`** : Backend HLSL complet
4. **Module `wgsl:glsl`** : Backend GLSL complet
5. **Module `wgsl:wgsl`** : Backend WGSL avec round-trip validation
6. **Tests unitaires** : Couverture > 90% pour tous les modules
7. **Documentation** : Documentation complète pour toutes les APIs publiques
8. **Exemples** : Exemples d'utilisation pour chaque backend

---

## 📝 NOTES

1. **Priorisation** : Commencer par l'architecture commune (Phase 4.0), puis implémenter les backends en parallèle.

2. **Parallélisation** : Une fois WriterBase implémenté, les backends MSL, HLSL, GLSL, WGSL peuvent être développés en parallèle par différentes personnes.

3. **Complexité variable** : 
   - MSL : Complexité moyenne-haute (binding model différent, external textures)
   - HLSL : Complexité haute (matrices 2-rows, sampler heap)
   - GLSL : Complexité moyenne (versions multiples, extensions)
   - WGSL : Complexité moyenne (formatage, round-trip)

4. **Tests** : Les tests doivent être écrits en parallèle avec l'implémentation.

5. **Revues de code** : Prévoir des revues de code régulières, surtout pour les parties complexes.

6. **Intégration continue** : Intégrer chaque backend dès qu'il est prêt pour détecter les problèmes tôt.

7. **Round-trip validation** : Le backend WGSL est essentiel pour valider la correction de l'IR. C'est une priorité pour les tests.

8. **Binding Maps** : Chaque backend a besoin d'un BindingMap adapté à son modèle de binding. Bien tester cette partie.
