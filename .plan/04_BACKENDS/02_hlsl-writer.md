# 🎮 Phase 4.2 : HLSL Writer

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-hlsl`  
**Phase** : 4 - Backends  
**Sous-Phase** : 4.2 - Backend HLSL  
**Durée** : 3-4 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Backend DirectX)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/`
> **Spec** : [HLSL Documentation](https://docs.microsoft.com/en-us/windows/win32/direct3dhlsl/dx-graphics-hlsl)

---

## 📋 OBJECTIFS

Implémenter le backend **High-Level Shading Language** (HLSL) pour les APIs DirectX (D3D11, D3D12).

**Livrable** : Module `naga-hlsl` capable de générer du code HLSL valide.

---

## 🎯 CONCEPTS CLÉS

### 1. HLSL en bref
- **Langage** : High-Level Shading Language (Microsoft)
- **APIs** : Direct3D 9-12, Xbox
- **Shader Models** : 2.0, 3.0, 4.0, 4.1, 5.0, 5.1, 6.0-6.6
- **Version par défaut** : Shader Model 6.0

### 2. Différences clés HLSL/WGSL

| Feature | WGSL | HLSL |
|---------|------|------|
| Binding model | Hierarchique | Register-based (tN, sN, uN, bN) |
| Matrices | Column-major | Row-major par défaut (configurable) |
| Layout | Standard WGSL | Doit matcher le layout WGSL dans les buffers |
| Samplers | Objets séparés | Doivent être dans un heap, accès via index buffer |
| External textures | Natif | Lowered vers 3 Texture2D + cbuffer |
| Arrays | `array<T, N>` | `T[N]` ou `T N` |

### 3. Row-Major vs Column-Major

WGSL spécifie que les matrices dans les buffers uniform sont stockées en **column-major**. HLSL utilise **row-major** par défaut. Pour résoudre ce problème :

1. Déclarer les matrices dans les buffers avec `row_major`
2. Transposer les dimensions (ex: WGSL `mat3x4<f32>` → HLSL `row_major float3x4`)
3. Les opérations matrix-vector fonctionnent correctement car HLSL transpose automatiquement

### 4. Matrices 2-rows

HLSL aligne toutes les rows d'une matrice sur 16 octets, tandis que WGSL aligne uniquement selon le type vectoriel. Pour les matrices `matKx2` :

**Solution** : Stocker chaque colonne comme un `float2` séparé et fournir des fonctions helpers pour assembler/désassembler.

```hlsl
// WGSL: mat3x2<f32>
struct Mat3x2 {
    float2 m_0; float2 m_1; float2 m_2;
};

// Helper functions
float3x2 GetMat3x2(Mat3x2 m) {
    return float3x2(m.m_0, m.m_1, m.m_2);
}

void SetMat3x2(out Mat3x2 m, float3x2 v) {
    m.m_0 = v[0]; m.m_1 = v[1]; m.m_2 = v[2];
}
```

### 5. Sampler Handling

HLSL utilise des **sampler heaps**. Chaque shader a accès à un sampler heap contenant tous les samplers. On utilise un **sampler index buffer** pour accéder au bon sampler.

```hlsl
// Dans les arguments de la fonction
SamplerState samplers[];  // Le heap
uint samplerIndexBuffer[]; // Le buffer d'index

// Pour accéder à un sampler
SamplerState sam = samplers[samplerIndexBuffer[index]];
```

### 6. External Textures

Similaire à MSL : lowered vers 3 `Texture2D<float4>` + 1 `cbuffer` de paramètres.

Les fonctions helpers sont générées pour `textureDimensions()`, `textureLoad()`, `textureSampleBaseClampToEdge()`.

### 7. Buffer Access

WGSL supporte les `storage` buffers avec accès arbitrary. HLSL utilise :
- `ByteAddressBuffer` pour les buffers bytes
- `StructuredBuffer` pour les buffers structurés
- `RWStructuredBuffer` pour les buffers read-write

---

## 📦 STRUCTURE DES FICHIERS

```
naga-hlsl/
├── build.gradle.kts
└── src/main/kotlin/dev/gfxrs/naga/back/hlsl/
    ├── HlslOptions.kt     # Options HLSL
    ├── BindTarget.kt      # Cibles de binding
    ├── Keywords.kt        # Mots-clés HLSL
    ├── ShaderModel.kt     # Shader models
    ├── HlslWriter.kt      # Writer principal
    ├── HlslModule.kt      # API publique
    └── writer/
        ├── Helpers.kt       # Fonctions helpers
        ├── StorageBuffer.kt # Gestion des buffers
        └── ExternalTexture.kt
```

---

## 🎯 IMPLÉMENTATION

### 1. HlslOptions.kt

```kotlin
data class HlslOptions(
    override val validationFlags: ValidationFlags = ValidationFlags.ALL,
    override val capabilities: Capabilities = HlslCapabilities.DEFAULT,
    override val shaderStages: ShaderStages = ShaderStages.ALL,
    override val indent: String = "    ",
    override val newline: String = "\n",
    override val version: String = "6.0",
    override val languageName: String = "HLSL",
    override val fileExtension: String = ".hlsl",
    
    // Options spécifiques HLSL
    val shaderModel: ShaderModel = ShaderModel.SM6_0,
    val matrixLayout: MatrixLayout = MatrixLayout.ROW_MAJOR,
    val externalTextureBindingMap: Map<ResourceBinding, Byte> = emptyMap(),
    val useSamplerHeap: Boolean = true,
    val samplerHeapSize: Int = 16
) : BackendOptions()

enum class ShaderModel {
    SM2_0, SM3_0, SM4_0, SM4_1, SM5_0, SM5_1, SM6_0, SM6_1, SM6_2, SM6_3, SM6_4, SM6_5, SM6_6
}

enum class MatrixLayout {
    ROW_MAJOR, COLUMN_MAJOR
}
```

### 2. BindTarget.kt

```kotlin
data class BindTarget(
    val register: String?,    // Ex: "t0", "s0", "u0", "b0"
    val space: Int?,         // Ex: "space1"
    val matrixLayout: MatrixLayout? = null,
    val packed: Boolean = false
)

typealias BindingMap = Map<ResourceBinding, BindTarget>
```

### 3. HlslWriter.kt

Hérite de `WriterBase<HlslOptions>`.

**Header:**
```hlsl
// HLSL Shader
// Generated by WebGPU-KTypes
// Shader Model: ${options.shaderModel}

#pragma pack_matrix(row_major)  // Si ROW_MAJOR
```

**Types:**
- Structs → `struct Name { ... };` avec `[[vk::location(N)]]` si nécessaire
- Vecteurs → `float2`, `float3`, `float4`, `int2`, etc.
- Matrices → `float2x2`, `float3x3`, `float4x4` avec `row_major` ou `column_major`
- Images → `Texture2D<float4>`, `Texture3D<float4>`, etc.
- Samplers → `SamplerState`

**Expressions:** Similaires à MSL mais avec la syntaxe HLSL.

**Statements:** Similaires à MSL.

**Entry Points:**
- Utiliser `[[vk::shader_stage(...)]]` ou les semantics DirectX
- Gérer les inputs/outputs avec les semantics appropriés

**Matrices 2-rows:**
- Détecter les `matKx2` dans les structs
- Générer des membres `float2` séparés
- Générer les fonctions helpers `GetMatKx2` et `SetMatKx2`

**Sampler Handling:**
- Créer un tableau `SamplerState samplers[]`
- Créer un buffer `uint samplerIndices[]`
- Pour chaque sampler, utiliser `samplers[samplerIndices[index]]`

### 4. HlslModule.kt

```kotlin
object HlslModule {
    fun writeString(module: Module, options: HlslOptions = HlslOptions()): String
    fun tryWriteString(module: Module, options: HlslOptions): Result<String, List<ValidationError>>
}
```

---

## ✅ CHECKLIST

### Configuration
- [ ] HlslOptions.kt
- [ ] BindTarget.kt
- [ ] Keywords.kt
- [ ] ShaderModel.kt

### Writer
- [ ] HlslWriter.kt (base)
- [ ] writeHeader() avec pragma pack_matrix
- [ ] getTypeName() pour tous les types HLSL
- [ ] writeStructType() avec row_major
- [ ] writeMatrix2xNHelpers()
- [ ] writeSamplerHeap()
- [ ] Toutes les writeXxx() pour expressions
- [ ] Toutes les writeXxx() pour statements
- [ ] writeEntryPoint()

### Types supportés
- [ ] Scalar (bool, int, uint, float, double, half)
- [ ] Vector (float2-4, int2-4, uint2-4, half2-4)
- [ ] Matrix (2x2, 3x3, 4x4, avec row_major/column_major)
- [ ] Matrix 2-rows (via struct personnalisée)
- [ ] Array (T[N] et ByteAddressBuffer)
- [ ] Struct
- [ ] Image (Texture2D, Texture3D, TextureCube, etc.)
- [ ] Sampler (SamplerState)
- [ ] Storage buffers (StructuredBuffer, RWStructuredBuffer)

### Fonctions built-in HLSL
- [ ] Math: toutes les fonctions standard
- [ ] Texture: Sample, Load, GetDimensions, etc.
- [ ] Derivatives: ddxy, ddy, fwidth
- [ ] Atomic: InterlockedAdd, InterlockedMin, etc.

### Tests
- [ ] Module vide
- [ ] Types (tous)
- [ ] Constantes
- [ ] Variables globales
- [ ] Fonctions
- [ ] Entry points
- [ ] Matrices 2-rows
- [ ] Sampler heap
- [ ] External textures

### Intégration
- [ ] build.gradle.kts
- [ ] Enregistrer dans BackendRegistry
- [ ] Documentation

---

## 📖 RÉFÉRENCES

1. **HLSL Documentation**: [Microsoft HLSL](https://docs.microsoft.com/en-us/windows/win32/direct3dhlsl/dx-graphics-hlsl)
2. **Rust HLSL Backend**: `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/`
3. **Rust Writer**: `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/writer.rs` (~2000 lignes)

---

## 🎯 PLANNING

| Tâche | Durée | Dépendances |
|-------|-------|-------------|
| HlslOptions + BindTarget + ShaderModel | 4-6h | BackendOptions |
| HlslWriter base | 8-12h | WriterBase |
| HlslWriter types (row_major, 2-row matrices) | 8-12h | HlslWriter base |
| HlslWriter expressions | 20-24h | HlslWriter base |
| HlslWriter statements | 20-24h | HlslWriter base |
| HlslWriter entry points + sampler heap | 8-12h | HlslWriter base |
| Matrix 2-row helpers | 4-6h | HlslWriter base |
| External textures | 6-8h | HlslWriter base |
| HlslModule + tests | 8-12h | Tout |
| build.gradle.kts | 2h | Aucune |
| **Total** | **88-124h (2.5-3.5 semaines)** |

---

## 📝 NOTES

1. **Shader Models** : SM 6.0 est le modèle le plus récent et le plus complet. Supporter SM 5.0+ pour la compatibilité.

2. **Matrix Layout** : Le layout row-major/column-major est crucial pour l'interopérabilité avec WGSL.

3. **2-row matrices** : C'est une complexité spécifique à HLSL. Les fonctions helpers doivent être générées correctement.

4. **Sampler heap** : Nécessaire pour supporter les binding arrays de samplers.

5. **Storage buffers** : HLSL a des types spécifiques pour les buffers storage.

6. **Validation** : Valider que le module utilise uniquement des fonctionnalités supportées par le Shader Model cible.
