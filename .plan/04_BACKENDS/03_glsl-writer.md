# 🖼️ Phase 4.3 : GLSL Writer

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:glsl`  
**Phase** : 4 - Backends  
**Sous-Phase** : 4.3 - Backend GLSL  
**Durée** : 2-3 semaines  
**Priorité** : ⭐⭐⭐⭐ (Important - Backend OpenGL/Vulkan)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/`
> **Spec** : [GLSL Specification](https://www.khronos.org/registry/OpenGL/index_gl.php)

---

## 📋 OBJECTIFS

Implémenter le backend **OpenGL Shading Language** (GLSL) pour OpenGL, OpenGL ES, et Vulkan (via SPIR-V).

**Livrable** : Module `wgsl:glsl` capable de générer du code GLSL valide.

---

## 🎯 CONCEPTS CLÉS

### 1. GLSL en bref
- **Langage** : OpenGL Shading Language
- **Versions Core** : 1.10, 1.20, 1.30, 1.40, 1.50, 3.30, 4.00-4.60
- **Versions ES** : 1.00, 3.00, 3.10, 3.20
- **Versions par défaut** : Core 4.50, ES 3.10

### 2. Différences clés GLSL/WGSL

| Feature | WGSL | GLSL |
|---------|------|------|
| Version directive | Non | Oui (`#version 450 core`) |
| Entry points | `fn main()` | `void main()` |
| Types | `texture_2d<f32>` | `sampler2D` (combined) ou `texture2D` + `sampler` (separate) |
| Matrices | Column-major | Column-major par défaut |
| Vecteurs | `vec2<T>` | `vec2` (type implicite) |
| Arrays | `array<T, N>` | `T[N]` ou `T N` |
| Binding | `@group(@binding)` | `layout(binding = N, set = N)` |
| Location | `@location(N)` | `layout(location = N)` |

### 3. Versions supportées

**Core:**
- 140, 150, 330, 400, 410, 420, 430, 440, 450, 460

**ES:**
- 300, 310, 320

### 4. Combined vs Separate Samplers

WGSL sépare les textures et les samplers. GLSL supporte les deux modèles :

**Combined (ancien style, plus simple):**
```glsl
uniform sampler2D myTexture;  // Texture + Sampler combinés
```

**Separate (moderne, plus flexible):**
```glsl
uniform texture2D myTexture;
uniform sampler mySampler;
// Utilisation: texture(sampler2D(myTexture, mySampler), coord)
```

**Choix** : Utiliser le modèle **separate** par défaut pour correspondre à WGSL.

### 5. Layout Qualifiers

WGSL:
```wgsl
@group(0) @binding(0) @location(0)
var position: vec4<f32>;
```

GLSL:
```glsl
layout(location = 0) in vec4 position;
```

Pour les uniform buffers:
```glsl
layout(set = 0, binding = 0) uniform MyBuffer {
    vec4 data;
};
```

### 6. Entry Points

WGSL permet des arguments dans les entry points. GLSL a des règles strictes :
- Les inputs doivent être déclarés avec `in`
- Les outputs doivent être déclarés avec `out`
- Pas de retour de valeur (sauf pour compute shaders avec `layout(local_size_x = N)`)

**Vertex Shader:**
```glsl
#version 450 core
layout(location = 0) in vec4 position;

void main() {
    gl_Position = position;
}
```

**Fragment Shader:**
```glsl
#version 450 core
layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(1.0);
}
```

**Compute Shader:**
```glsl
#version 450 core
layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;

void main() {
    // ...
}
```

### 7. Features et Extensions

Certaines fonctionnalités nécessitent des **extensions** :
- `GL_ARB_shader_storage_buffer_object` pour les SSBOs
- `GL_EXT_shader_explicit_arithmetic_types` pour les types explicites
- `GL_NV_ray_tracing` pour le ray tracing

**Approche** : Ajouter automatiquement les extensions nécessaires en fonction des capacités utilisées.

### 8. Precision Qualifiers

GLSL ES nécessite des **precision qualifiers** :
- `highp`, `mediump`, `lowp`

**Approche** : Utiliser `highp` par défaut pour ES, option pour spécifier.

---

## 📦 STRUCTURE DES FICHIERS

```
wgsl:glsl/
├── build.gradle.kts
└── src/main/kotlin/dev/gfxrs/naga/back/glsl/
    ├── GlslOptions.kt     # Options GLSL
    ├── BindTarget.kt      # Cibles de binding
    ├── Keywords.kt        # Mots-clés GLSL
    ├── GlslVersion.kt     # Versions supportées
    ├── FeatureManager.kt  # Gestion des features/extensions
    ├── GlslWriter.kt      # Writer principal
    ├── GlslModule.kt      # API publique
    └── writer/
        ├── Helpers.kt       # Fonctions helpers
        └── SeparateSamplers.kt  # Gestion separate samplers
```

---

## 🎯 IMPLÉMENTATION

### 1. GlslOptions.kt

```kotlin
data class GlslOptions(
    override val validationFlags: ValidationFlags = ValidationFlags.ALL,
    override val capabilities: Capabilities = GlslCapabilities.DEFAULT,
    override val shaderStages: ShaderStages = ShaderStages.ALL,
    override val indent: String = "    ",
    override val newline: String = "\n",
    override val version: String = "450",
    override val languageName: String = "GLSL",
    override val fileExtension: String = ".glsl",
    
    // Options spécifiques GLSL
    val profile: GlslProfile = GlslProfile.CORE,
    val es: Boolean = false,
    val precision: GlslPrecision = GlslPrecision.HIGH,
    val useSeparateSamplers: Boolean = true,
    val bindingMap: Map<ResourceBinding, Byte> = emptyMap()
) : BackendOptions()

enum class GlslProfile {
    CORE, ES, COMPATIBILITY
}

enum class GlslPrecision {
    HIGH, MEDIUM, LOW
}
```

### 2. GlslVersion.kt

```kotlin
object GlslVersions {
    val SUPPORTED_CORE: List<Int> = listOf(140, 150, 330, 400, 410, 420, 430, 440, 450, 460)
    val SUPPORTED_ES: List<Int> = listOf(300, 310, 320)
    
    fun isCoreSupported(version: Int): Boolean = version in SUPPORTED_CORE
    fun isEsSupported(version: Int): Boolean = version in SUPPORTED_ES
    
    fun getVersionString(version: Int, profile: GlslProfile): String {
        return when (profile) {
            GlslProfile.CORE -> "$version core"
            GlslProfile.ES -> "$version es"
            GlslProfile.COMPATIBILITY -> "$version compatibility"
        }
    }
}
```

### 3. FeatureManager.kt

Gère les features et extensions GLSL automatiquement.

```kotlin
class FeatureManager {
    private val requiredExtensions = mutableSetOf<String>()
    private val requiredVersions = mutableSetOf<Int>()
    
    fun requireExtension(extension: String) {
        requiredExtensions.add(extension)
    }
    
    fun requireVersion(version: Int) {
        requiredVersions.add(version)
    }
    
    fun generatePreamble(version: Int, profile: GlslProfile): String {
        val lines = mutableListOf<String>()
        
        // Version directive
        lines.add("#version ${GlslVersions.getVersionString(version, profile)}")
        
        // Extensions
        for (ext in requiredExtensions.sorted()) {
            lines.add("#extension $ext : require")
        }
        
        // Precision pour ES
        if (profile == GlslProfile.ES) {
            lines.add("precision highp float;")
            lines.add("precision highp int;")
            lines.add("precision highp uint;")
        }
        
        return lines.joinToString("\n") + "\n\n"
    }
}
```

### 4. GlslWriter.kt

Hérite de `WriterBase<GlslOptions>`.

**Header:**
```glsl
#version 450 core
#extension GL_ARB_shader_storage_buffer_object : require

precision highp float;
```

**Types:**
- Structs → `struct Name { ... };`
- Vecteurs → `vec2`, `vec3`, `vec4`, `ivec2`, `uvec2`, etc.
- Matrices → `mat2`, `mat3`, `mat4`, `mat2x3`, `mat3x2`, etc.
- Images → `texture2D`, `texture3D`, `textureCube`, `sampler2D`, `isampler2D`, `usampler2D`
- Storage buffers → `buffer`, `textureBuffer`, `imageBuffer`

**Expressions:** Similaires aux autres backends avec la syntaxe GLSL.

**Statements:** Similaires aux autres backends.

**Entry Points:**
- Vertex: `void main()` avec `gl_Position`
- Fragment: `void main()` avec output variables
- Compute: `void main()` avec `gl_WorkGroupSize`

**Separate Samplers:**
- Texture seule: `texture2D` ou `texture2DMS`
- Sampler seule: `sampler`
- Sample: `texture(sampler2D(texture, sampler), coord)`

### 5. GlslModule.kt

```kotlin
object GlslModule {
    fun writeString(module: Module, options: GlslOptions = GlslOptions()): String
    fun tryWriteString(module: Module, options: GlslOptions): Result<String, List<ValidationError>>
}
```

---

## ✅ CHECKLIST

### Configuration
- [ ] GlslOptions.kt
- [ ] BindTarget.kt
- [ ] Keywords.kt
- [ ] GlslVersion.kt
- [ ] FeatureManager.kt

### Writer
- [ ] GlslWriter.kt (base)
- [ ] writeHeader() avec version et extensions
- [ ] writePreamble() avec precision qualifiers
- [ ] getTypeName() pour tous les types GLSL
- [ ] writeStructType()
- [ ] writeTextureType() (separate ou combined)
- [ ] writeSamplerType()
- [ ] Toutes les writeXxx() pour expressions
- [ ] Toutes les writeXxx() pour statements
- [ ] writeEntryPoint() pour chaque stage
- [ ] writeComputeShader() avec local_size

### Types supportés
- [ ] Scalar (bool, int, uint, float, double)
- [ ] Vector (vec2-4, ivec2-4, uvec2-4, bvec2-4, dvec2-4)
- [ ] Matrix (mat2-4, mat2x2-4x4)
- [ ] Array (T[N])
- [ ] Struct
- [ ] Image (texture1D-3D, textureCube, texture2DArray, etc.)
- [ ] Sampler (sampler1D-3D, samplerCube, etc.)
- [ ] Storage buffers (buffer, textureBuffer, imageBuffer)
- [ ] Atomic counters

### Fonctions built-in GLSL
- [ ] Math: toutes les fonctions standard
- [ ] Texture: texture, textureProj, texelFetch, textureSize, etc.
- [ ] Derivatives: dFdx, dFdy, fwidth
- [ ] Atomic: atomicAdd, atomicMin, atomicMax, etc.
- [ ] Geometry: gl_Position, gl_VertexID, etc.
- [ ] Fragment: gl_FragCoord, gl_FragColor, etc.

### Features/Extensions
- [ ] Détection automatique des extensions nécessaires
- [ ] Version directive
- [ ] Precision qualifiers pour ES

### Tests
- [ ] Module vide
- [ ] Types (tous)
- [ ] Constantes
- [ ] Variables globales
- [ ] Fonctions
- [ ] Entry points (vertex, fragment, compute)
- [ ] Bindings
- [ ] Separate samplers
- [ ] Extensions automatiques

### Intégration
- [ ] build.gradle.kts
- [ ] Enregistrer dans BackendRegistry
- [ ] Documentation

---

## 📖 RÉFÉRENCES

1. **GLSL 4.60 Spec**: [Khronos GLSL](https://www.khronos.org/registry/OpenGL/specs/gl/GLSLangSpec.4.60.pdf)
2. **GLSL ES 3.20 Spec**: [Khronos GLSL ES](https://www.khronos.org/registry/OpenGL/specs/es/GLSL_ES_Specification_3.20.pdf)
3. **Rust GLSL Backend**: `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/`
4. **Rust Writer**: `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/writer.rs` (~2500 lignes)

---

## 🎯 PLANNING

| Tâche | Durée | Dépendances |
|-------|-------|-------------|
| GlslOptions + GlslVersion + FeatureManager | 6-8h | BackendOptions |
| GlslWriter base + header | 8-12h | WriterBase |
| GlslWriter types | 8-12h | GlslWriter base |
| GlslWriter expressions | 20-24h | GlslWriter base |
| GlslWriter statements | 16-20h | GlslWriter base |
| GlslWriter entry points (3 stages) | 8-12h | GlslWriter base |
| Separate samplers | 4-6h | GlslWriter base |
| Features/extensions auto | 4-6h | GlslWriter base |
| GlslModule + tests | 8-12h | Tout |
| build.gradle.kts | 2h | Aucune |
| **Total** | **84-116h (2-3 semaines)** |

---

## 📝 NOTES

1. **Versions** : Supporter GLSL 4.50 core et 3.10 ES par défaut.

2. **Separate Samplers** : C'est le modèle le plus proche de WGSL. Le modèle combined peut être ajouté comme option.

3. **Extensions** : Détecter automatiquement les extensions nécessaires en fonction des capacités utilisées.

4. **Precision** : Utiliser `highp` par défaut pour ES. Permettre de le configurer.

5. **Validation** : Valider que le module utilise uniquement des fonctionnalités supportées par la version GLSL cible.

6. **Compatibilité** : GLSL a beaucoup de variations entre les versions. Bien tester avec différentes versions.
