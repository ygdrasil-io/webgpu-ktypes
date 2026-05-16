# 🏷️ Phase 3.3 : Namer

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 3 - Processing  
**Sous-Phase** : 3.3 - Name Resolution & Mangling  
**Durée** : 2-3 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Génération de noms uniques)  
**Statut** : [ ] Non commencé | [ ] En cours | [x] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/namer.rs` (~450 lignes)

---

## 📋 OBJECTIFS

Implémenter le **namer** qui assigne des noms uniques à toutes les entités d'un module IR qui nécessitent des identifiants dans les backends textuels. Cela permet :
- De générer des noms valides pour les langages cibles (MSL, HLSL, GLSL)
- De garantir l'unicité des noms dans le scope approprié
- De gérer les noms par défaut pour les entités anonymes
- De sanitizer les noms pour qu'ils soient valides dans tous les backends
- De supporter le name mangling pour éviter les conflits

**Livrable principal** : Un module capable d'assigner des noms uniques et valides à toutes les entités IR pour la génération de code.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce que le Namer ?

Le namer est responsable de **l'assignation de noms** à toutes les entités d'un module IR qui auront une représentation dans le code généré. Il :
- Assigne des noms uniques aux types, fonctions, variables, etc.
- Gère les noms par défaut pour les entités anonymes
- Sanitize les noms pour qu'ils soient valides dans tous les backends
- Gère les namespaces (scope global vs scope local)
- Fournit des noms cohérents pour les tests et le débogage

### 2. NameKey : Identification des entités

Chaque entité à nommer est identifiée par une `NameKey` qui contient :

| NameKey | Description | Scope |
|---------|-------------|-------|
| `Type(handle)` | Type (struct, alias) | Global |
| `StructMember(type, index)` | Membre d'une struct | Local à la struct |
| `Function(handle)` | Fonction | Global |
| `FunctionArgument(func, index)` | Argument de fonction | Local à la fonction |
| `FunctionLocal(func, handle)` | Variable locale | Local à la fonction |
| `GlobalVariable(handle)` | Variable globale | Global |
| `Constant(handle)` | Constante | Global |
| `Override(handle)` | Override | Global |
| `EntryPoint(index)` | Entry point | Global |
| `EntryPointArgument(ep, index)` | Argument d'entry point | Local à l'entry point |
| `EntryPointLocal(ep, handle)` | Variable locale d'entry point | Local à l'entry point |

### 3. Sanitization des noms

Les noms doivent être valides dans tous les langages cibles (MSL, HLSL, GLSL, WGSL). Le sanitizer :
- Supprime les caractères non alphanumériques (sauf `_`)
- Remplace les caractères spéciaux (`:`, `<`, `>`, `,`) par `_`
- Convertit les séquences de `_` multiples en un seul `_`
- Ajoute un préfixe si le nom commence par un chiffre
- Vérifie que le nom n'est pas vide (utilise "unnamed" dans ce cas)
- Évite les conflits avec les mots-clés des langages cibles

### 4. Unicité des noms

Pour garantir l'unicité :
- Un compteur est maintenu pour chaque nom de base
- Si un nom est déjà utilisé, on ajoute un suffixe `_N`
- Si le nom de base se termine déjà par des chiffres, on sépare avec `_`
- Ex: `x`, `x_1`, `x_2`, `x1_`, `x1_1`, etc.

### 5. Architecture

```
Module IR (avec noms optionnels)
    ↓
Namer.reset(module, keywords)
    ↓
Pour chaque entité à nommer :
  1. Récupérer le nom source (ou utiliser un fallback)
  2. Sanitizer le nom
  3. Vérifier si le nom est unique
  4. Ajouter un suffixe si nécessaire
  5. Stocker dans la map NameKey -> String
    ↓
Map<NameKey, String> (noms finaux)
    ↓
Utilisation par :
  - Backends (MSL, HLSL, GLSL, WGSL)
  - Debug/Tests
```

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. NameKey.kt (Clé d'identification)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/NameKey.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

/**
 * Clé pour identifier une entité à nommer.
 * Chaque variant représente un type d'entité différent.
 */
sealed class NameKey {
    
    // Types
    data class Type(val handle: Handle<Type>) : NameKey()
    data class StructMember(val typeHandle: Handle<Type>, val memberIndex: Int) : NameKey()
    
    // Fonctions
    data class Function(val handle: Handle<Function>) : NameKey()
    data class FunctionArgument(val function: Handle<Function>, val argumentIndex: Int) : NameKey()
    data class FunctionLocal(val function: Handle<Function>, val variable: Handle<LocalVariable>) : NameKey()
    
    // Variables globales
    data class GlobalVariable(val handle: Handle<GlobalVariable>) : NameKey()
    
    // Constantes et Overrides
    data class Constant(val handle: Handle<Constant>) : NameKey()
    data class Override(val handle: Handle<Override>) : NameKey()
    
    // Entry Points
    data class EntryPoint(val index: Int) : NameKey()
    data class EntryPointArgument(val entryPointIndex: Int, val argumentIndex: Int) : NameKey()
    data class EntryPointLocal(val entryPointIndex: Int, val variable: Handle<LocalVariable>) : NameKey()
    
    // Pour les backends spécifiques (ex: external textures en MSL)
    data class ExternalTextureGlobalVariable(
        val globalVariable: Handle<GlobalVariable>,
        val key: ExternalTextureNameKey
    ) : NameKey()
    
    data class ExternalTextureFunctionArgument(
        val function: Handle<Function>,
        val argumentIndex: Int,
        val key: ExternalTextureNameKey
    ) : NameKey()
    
    data class FunctionOobLocal(val function: Handle<Function>, val type: Handle<Type>) : NameKey()
    data class EntryPointOobLocal(val entryPointIndex: Int, val type: Handle<Type>) : NameKey()
}

/**
 * Clé pour identifier les composantes d'une texture externe (pour MSL backend).
 */
sealed class ExternalTextureNameKey {
    data class Plane(val index: Int) : ExternalTextureNameKey()
    object Params : ExternalTextureNameKey()
    
    companion object {
        val ALL: List<Pair<String, ExternalTextureNameKey>> = listOf(
            "_plane0" to Plane(0),
            "_plane1" to Plane(1),
            "_plane2" to Plane(2),
            "_params" to Params
        )
    }
}
```

### 2. KeywordSet.kt (Gestion des mots-clés)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/KeywordSet.kt`

```kotlin
package io.ygdrasil.wgsl.proc

/**
 * Ensemble de mots-clés pour un langage cible.
 * Utilisé pour éviter les conflits de noms.
 */
class KeywordSet(private val keywords: Set<String>) {
    
    fun contains(word: String): Boolean {
        return keywords.contains(word)
    }
    
    companion object {
        // Mots-clés WGSL
        val WGSL_KEYWORDS = setOf(
            "align", "binding", "builtin", "compute", "const", "const_assert",
            "continue", "discard", "else", "enable", "fn", "for", "group",
            "if", "interpolate", "invariant", "layout", "let", "location",
            "override", "requires", "return", "set", "struct", "switch",
            "template", "then", "type", "var", "while", "workgroup_size",
            "_"
        )
        
        // Mots-clés MSL (Metal Shading Language)
        val MSL_KEYWORDS = setOf(
            "array", "asm", "bool", "break", "buffer", "case", "cast",
            "char", "class", "coherent", "const", "constexpr", "continue",
            "default", "device", "do", "double", "else", "enum", "export",
            "extern", "false", "float", "for", "half", "if", "inline",
            "int", "int2", "int3", "int4", "long", "namespace", "operator",
            "packed", "private", "protocol", "register", "return", "sampler",
            "short", "signed", "sizeof", "static", "struct", "switch",
            "template", "texture", "thread", "threadgroup", "true", "typedef",
            "typeof", "uint", "uint2", "uint3", "uint4", "ulong", "union",
            "unsigned", "using", "virtual", "void", "volatile", "while"
        )
        
        // Mots-clés HLSL
        val HLSL_KEYWORDS = setOf(
            "append", "asm", "BlendState", "bool", "break", "Buffer",
            "byteaddress", "case", "cbuffer", "centroid", "class",
            "clip", "column_major", "compile", "const", "continue",
            "ComputeShader", "ConsumeStructuredBuffer", "default", "DepthStencilState",
            "DepthStencilView", "discard", "do", "double", "else", "export",
            "extern", "false", "float", "for", "GeometryShader", "groupshared",
            "half", "Hullshader", "if", "in", "inline", "inout", "InputPatch",
            "int", "int2", "int3", "int4", "interface", "line", "lineadj",
            "linear", "LineStream", "matrix", "min16float", "min10float",
            "min16int", "min12int", "min16uint", "namespace", "nointerpolation",
            "noperspective", "out", "OutputPatch", "packoffset", "pass",
            "pixel", "PointStream", "precise", "Predicate", "RasterizerState",
            "RasterizerOrderedView", "RawBuffer", "RenderTargetView",
            "return", "register", "row_major", "RWBuffer", "RWByteAddressBuffer",
            "RWStructuredBuffer", "RWTexture1D", "RWTexture2D", "RWTexture3D",
            "sample", "SamplerState", "SamplerComparisonState", "shared",
            "snorm", "stateblock", "StateBlockState", "static", "string",
            "struct", "structured", "switch", "tbuffer", "Technique",
            "template", "Texture1D", "Texture2D", "Texture2DMS", "Texture3D",
            "TextureCube", "true", "typedef", "triangle", "triangleadj",
            "uniform", "unordered", "unsigned", "unroll", "vector", "vertex",
            "void", "volatile", "while"
        )
        
        // Mots-clés GLSL
        val GLSL_KEYWORDS = setOf(
            "active", "and", "any", "as", "asm", "atomic_uint", "bool",
            "break", "buffer", "case", "cast", "centroid", "class",
            "coherent", "const", "continue", "default", "discard", "do",
            "double", "else", "enum", "equal", "explicit", "export",
            "extern", "external", "false", "float", "for", "greater",
            "highp", "hvec2", "hvec3", "hvec4", "if", "iimage1D",
            "iimage1DArray", "iimage2D", "iimage2DArray", "iimage2DMS",
            "iimage2DRect", "iimage3D", "iimageBuffer", "iimageCube",
            "iimageCubeArray", "in", "inline", "inout", "input", "int",
            "interface", "invariant", "isampler1D", "isampler1DArray",
            "isampler2D", "isampler2DArray", "isampler2DMS", "isampler2DRect",
            "isampler3D", "isamplerBuffer", "isamplerCube", "isamplerCubeArray",
            "layout", "less", "line", "lowp", "mat2", "mat2x2", "mat2x3",
            "mat2x4", "mat3", "mat3x2", "mat3x3", "mat3x4", "mat4",
            "mat4x2", "mat4x3", "mat4x4", "mediump", "memory", "mix", "not",
            "notEqual", "out", "output", "packed", "partition", "patch",
            "precise", "precision", "pragma", " readonly", "resource",
            "restrict", "return", "sample", "sampler1D", "sampler1DArray",
            "sampler1DArrayShadow", "sampler1DShadow", "sampler2D",
            "sampler2DArray", "sampler2DArrayShadow", "sampler2DMS",
            "sampler2DMSArray", "sampler2DRect", "sampler2DRectShadow",
            "sampler2DShadow", "sampler3D", "sampler3DRect", "samplerBuffer",
            "samplerCube", "samplerCubeArray", "samplerCubeArrayShadow",
            "samplerCubeShadow", "shared", "smooth", "struct", "subroutine",
            "superp", "switch", "template", "this", "true", "typedef",
            "typeid", "typeof", "uimage1D", "uimage1DArray", "uimage2D",
            "uimage2DArray", "uimage2DMS", "uimage2DRect", "uimage3D",
            "uimageBuffer", "uimageCube", "uimageCubeArray", "uint",
            "uniform", "unordered", "usampler1D", "usampler1DArray",
            "usampler2D", "usampler2DArray", "usampler2DMS", "usampler2DRect",
            "usampler3D", "usamplerBuffer", "usamplerCube", "usamplerCubeArray",
            "using", "vec2", "vec3", "vec4", "void", "volatile", "while",
            "writeonly"
        )
        
        // Identifiants built-in WGSL
        val WGSL_BUILTIN_IDENTIFIERS = setOf(
            "AbstractInt", "AbstractIntVector", "array", "atomic", "bool",
            "f16", "f32", "f64", "i8", "i16", "i32", "i64", "mat2x2",
            "mat2x3", "mat2x4", "mat3x2", "mat3x3", "mat3x4", "mat4x2",
            "mat4x3", "mat4x4", "ptr", "sampler", "sampler_comparison",
            "texture_1d", "texture_2d", "texture_2d_array", "texture_3d",
            "texture_cube", "texture_cube_array", "texture_depth_2d",
            "texture_depth_2d_array", "texture_depth_cube", "texture_depth_cube_array",
            "texture_depth_multisampled_2d", "texture_external", "texture_multisampled_2d",
            "texture_multisampled_2d_array", "texture_storage_1d", "texture_storage_2d",
            "texture_storage_2d_array", "texture_storage_3d", "u8", "u16", "u32", "u64",
            "u32vector", "uvec2", "uvec3", "uvec4", "vec2", "vec3", "vec4"
        )
    }
}

/**
 * Ensemble de mots-clés insensible à la casse.
 */
class CaseInsensitiveKeywordSet(private val keywords: Set<String>) {
    fun contains(word: String): Boolean {
        return keywords.any { it.equals(word, ignoreCase = true) }
    }
}
```

### 3. Namer.kt (Classe principale)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/Namer.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

/**
 * Processeur qui assigne des noms à toutes les entités d'un module.
 * 
 * Ce namer garantit que :
 * - Chaque nom est unique dans son scope
 * - Chaque nom est valide dans tous les langages cibles
 * - Chaque nom évite les conflits avec les mots-clés
 * - Les entités anonymes reçoivent un nom par défaut
 */
class Namer {
    
    private val unique: MutableMap<String, Int> = mutableMapOf()
    private var keywords: KeywordSet = KeywordSet(emptySet())
    private var builtinIdentifiers: KeywordSet = KeywordSet(emptySet())
    private var keywordsCaseInsensitive: CaseInsensitiveKeywordSet = CaseInsensitiveKeywordSet(emptySet())
    private val reservedPrefixes: MutableList<String> = mutableListOf()
    
    private val SEPARATOR = '_'
    
    /**
     * Réinitialise le namer pour un nouveau module.
     * 
     * @param module Le module à traiter
     * @param reservedKeywords Mots-clés réservés pour le langage cible
     * @param builtinIdentifiers Identifiants built-in à éviter
     * @param reservedKeywordsCaseInsensitive Mots-clés insensibles à la casse
     * @param reservedPrefixes Préfixes réservés
     * @param output Map à remplir avec les noms générés
     */
    fun reset(
        module: Module,
        reservedKeywords: KeywordSet,
        builtinIdentifiers: KeywordSet,
        reservedKeywordsCaseInsensitive: CaseInsensitiveKeywordSet,
        reservedPrefixes: List<String>,
        output: MutableMap<NameKey, String>
    ) {
        this.unique.clear()
        this.keywords = reservedKeywords
        this.builtinIdentifiers = builtinIdentifiers
        this.keywordsCaseInsensitive = reservedKeywordsCaseInsensitive
        this.reservedPrefixes.clear()
        this.reservedPrefixes.addAll(reservedPrefixes)
        
        output.clear()
        
        // Nommer les types
        nameTypes(module, output)
        
        // Nommer les entry points
        nameEntryPoints(module, output)
        
        // Nommer les fonctions
        nameFunctions(module, output)
        
        // Nommer les variables globales
        nameGlobalVariables(module, output)
        
        // Nommer les constantes
        nameConstants(module, output)
        
        // Nommer les overrides
        nameOverrides(module, output)
    }
    
    /**
     * Génère un nom unique pour une entité.
     * 
     * @param labelRaw Le nom source (peut être null)
     * @param fallback Nom par défaut si labelRaw est null ou vide
     * @return Un nom unique et valide
     */
    fun call(labelRaw: String?, fallback: String): String {
        return call(labelRaw ?: fallback)
    }
    
    /**
     * Génère un nom unique pour une entité.
     * 
     * @param labelRaw Le nom source
     * @return Un nom unique et valide
     */
    fun call(labelRaw: String): String {
        val base = sanitize(labelRaw)
        
        return when (val count = unique[base]) {
            null -> {
                unique[base] = 0
                ensureUniqueness(base)
            }
            else -> {
                unique[base] = count + 1
                ensureUniqueness("$base${SEPARATOR}${count + 1}")
            }
        }
    }
    
    /**
     * Nettoie un nom pour qu'il soit valide dans tous les langages cibles.
     * 
     * @param string Le nom à nettoyer
     * @return Un nom valide
     */
    private fun sanitize(string: String): String {
        var result = string
            .trimStart { it.isDigit() }
            .trimEnd { it == SEPARATOR }
        
        // Si le résultat est vide, utiliser "unnamed"
        if (result.isEmpty()) {
            return "unnamed"
        }
        
        // Vérifier si le résultat contient uniquement des caractères alphanumériques et _
        // et ne contient pas de __
        if (result.none { !it.isLetterOrDigit() && it != SEPARATOR } && "__" !in result) {
            return result
        }
        
        // Sinon, filtrer et remplacer les caractères spéciaux
        val mutResult = StringBuilder()
        var hadUnderscoreAtEnd = false
        
        for (c in result) {
            val replacedChar = when (c) {
                ':', '<', '>', ',' -> SEPARATOR
                else -> c
            }
            
            if (hadUnderscoreAtEnd && replacedChar == SEPARATOR) {
                continue
            }
            
            if (replacedChar.isLetterOrDigit() || replacedChar == SEPARATOR) {
                mutResult.append(replacedChar)
            } else {
                if (!hadUnderscoreAtEnd && mutResult.isNotEmpty()) {
                    mutResult.append(SEPARATOR)
                }
                mutResult.append("u${c.code.toString(16).padStart(4, '0')}_")
            }
            
            hadUnderscoreAtEnd = replacedChar == SEPARATOR
        }
        
        // Supprimer les _ à la fin
        result = mutResult.toString().trimEnd { it == SEPARATOR }
        
        if (result.isEmpty()) {
            result = "unnamed"
        } else if (result.startsWith { it.isDigit() }) {
            // Ne devrait pas arriver après le trimStart initial
            result = "gen_$result"
        }
        
        // Vérifier les préfixes réservés
        for (prefix in reservedPrefixes) {
            if (result.startsWith(prefix)) {
                return "gen_$result"
            }
        }
        
        return result
    }
    
    /**
     * Garantit que le nom est unique et n'est pas un mot-clé.
     */
    private fun ensureUniqueness(mutName: String): String {
        var name = mutName
        
        // Vérifier si le nom est un mot-clé
        if (keywords.contains(name) || builtinIdentifiers.contains(name) ||
            keywordsCaseInsensitive.contains(name)) {
            name += SEPARATOR
        }
        
        return name
    }
    
    /**
     * Nomme tous les types du module.
     */
    private fun nameTypes(module: Module, output: MutableMap<NameKey, String>) {
        val entryPointTypeFallbacks = mutableMapOf<Handle<Type>, String>()
        
        // Trouver les noms par défaut pour les types de retour d'entry points
        for ((epIndex, ep) in module.entryPoints.withIndex()) {
            val result = ep.function.result ?: continue
            val resultType = module.types[result.ty.index]
            
            if (resultType.name == null && resultType.inner is TypeInner.Struct) {
                val label = when (ep.stage) {
                    ShaderStage.VERTEX -> "VertexOutput"
                    ShaderStage.FRAGMENT -> "FragmentOutput"
                    ShaderStage.COMPUTE -> "ComputeOutput"
                    ShaderStage.TASK,
                    ShaderStage.MESH,
                    ShaderStage.RAY_GENERATION,
                    ShaderStage.CLOSEST_HIT,
                    ShaderStage.ANY_HIT,
                    ShaderStage.MISS -> continue
                }
                entryPointTypeFallbacks[result.ty] = label
            }
        }
        
        // Nommer les types
        for ((typeHandle, type) in module.types.withIndex()) {
            val handle = Handle<Type>(typeHandle)
            
            // Nom du type
            val rawLabel = type.name ?: entryPointTypeFallbacks[handle] ?: "type"
            val typeName = call(rawLabel)
            output[NameKey.Type(handle)] = typeName
            
            // Nommer les membres si c'est une struct
            if (type.inner is TypeInner.Struct) {
                val structInner = type.inner as TypeInner.Struct
                namespace(structInner.members.size) { namer ->
                    for ((memberIndex, member) in structInner.members.withIndex()) {
                        val memberName = namer.callOr(member.name, "member")
                        output[NameKey.StructMember(handle, memberIndex)] = memberName
                    }
                }
            }
        }
    }
    
    /**
     * Nomme tous les entry points du module.
     */
    private fun nameEntryPoints(module: Module, output: MutableMap<NameKey, String>) {
        for ((epIndex, ep) in module.entryPoints.withIndex()) {
            val epName = call(ep.name)
            output[NameKey.EntryPoint(epIndex)] = epName
            
            // Nommer les arguments
            for ((argIndex, arg) in ep.function.arguments.withIndex()) {
                val argName = callOr(arg.name, "param")
                output[NameKey.EntryPointArgument(epIndex, argIndex)] = argName
            }
            
            // Nommer les variables locales
            for ((varHandle, var) in ep.function.localVariables.withIndex()) {
                val varName = callOr(var.name, "local")
                output[NameKey.EntryPointLocal(epIndex, Handle(varHandle))] = varName
            }
        }
    }
    
    /**
     * Nomme toutes les fonctions du module.
     */
    private fun nameFunctions(module: Module, output: MutableMap<NameKey, String>) {
        for ((funcHandle, func) in module.functions.withIndex()) {
            val handle = Handle<Function>(funcHandle)
            val funcName = callOr(func.name, "function")
            output[NameKey.Function(handle)] = funcName
            
            // Nommer les arguments
            for ((argIndex, arg) in func.arguments.withIndex()) {
                val argName = callOr(arg.name, "param")
                output[NameKey.FunctionArgument(handle, argIndex)] = argName
            }
            
            // Nommer les variables locales
            for ((varHandle, var) in func.localVariables.withIndex()) {
                val varName = callOr(var.name, "local")
                output[NameKey.FunctionLocal(handle, Handle(varHandle))] = varName
            }
        }
    }
    
    /**
     * Nomme toutes les variables globales du module.
     */
    private fun nameGlobalVariables(module: Module, output: MutableMap<NameKey, String>) {
        for ((handle, var) in module.globalVariables.withIndex()) {
            val varName = callOr(var.name, "global")
            output[NameKey.GlobalVariable(Handle(handle))] = varName
        }
    }
    
    /**
     * Nomme toutes les constantes du module.
     */
    private fun nameConstants(module: Module, output: MutableMap<NameKey, String>) {
        var temp = ""
        
        for ((handle, constant) in module.constants.withIndex()) {
            val label = constant.name ?: run {
                temp = "const_${output[NameKey.Type(constant.ty)]}"
                temp
            }
            val constName = call(label)
            output[NameKey.Constant(Handle(handle))] = constName
        }
    }
    
    /**
     * Nomme tous les overrides du module.
     */
    private fun nameOverrides(module: Module, output: MutableMap<NameKey, String>) {
        var temp = ""
        
        for ((handle, override) in module.overrides.withIndex()) {
            val label = override.name ?: run {
                temp = "override_${output[NameKey.Type(override.ty)]}"
                temp
            }
            val overrideName = call(label)
            output[NameKey.Override(Handle(handle))] = overrideName
        }
    }
    
    /**
     * Appelle call() avec un fallback si label est null.
     */
    private fun callOr(label: String?, fallback: String): String {
        return call(label ?: fallback)
    }
    
    /**
     * Crée un nouveau namespace local pour éviter les conflits.
     * Utilisé pour les membres de structs, arguments de fonctions, etc.
     */
    private fun namespace(capacity: Int, body: (Namer) -> Unit) {
        val savedUnique = unique.toMutableMap()
        val savedKeywords = keywords
        val savedBuiltinIdentifiers = builtinIdentifiers
        val savedKeywordsCaseInsensitive = keywordsCaseInsensitive
        val savedReservedPrefixes = reservedPrefixes.toMutableList()
        
        unique.clear()
        reservedPrefixes.clear()
        
        body(this)
        
        unique.clear()
        unique.putAll(savedUnique)
        keywords = savedKeywords
        builtinIdentifiers = savedBuiltinIdentifiers
        keywordsCaseInsensitive = savedKeywordsCaseInsensitive
        reservedPrefixes.clear()
        reservedPrefixes.addAll(savedReservedPrefixes)
    }
}
```

---

## 📁 STRUCTURE DES FICHIERS

```
wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/
├── NameKey.kt              # Clé d'identification des entités
├── KeywordSet.kt           # Gestion des mots-clés par langage
├── Namer.kt                # Classe principale Namer
└── namer/
    └── CaseInsensitiveKeywordSet.kt  # (Optionnel: dans un sous-package)
```

---

## 🧪 TESTS

### 1. NamerTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/proc/NamerTest.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NamerTest {
    
    private lateinit var module: Module
    private lateinit var namer: Namer
    private lateinit var output: MutableMap<NameKey, String>
    
    @BeforeEach
    fun setUp() {
        module = Module()
        namer = Namer()
        output = mutableMapOf()
        
        val keywords = KeywordSet(setOf("function", "param", "local", "global", "type"))
        val builtins = KeywordSet(emptySet())
        val caseInsensitive = CaseInsensitiveKeywordSet(emptySet())
        
        namer.reset(module, keywords, builtins, caseInsensitive, emptyList(), output)
    }
    
    @Test
    fun `test simple name`() {
        assertThat(namer.call("x")).isEqualTo("x")
    }
    
    @Test
    fun `test duplicate names`() {
        assertThat(namer.call("x")).isEqualTo("x")
        assertThat(namer.call("x")).isEqualTo("x_1")
        assertThat(namer.call("x")).isEqualTo("x_2")
    }
    
    @Test
    fun `test name with digits suffix`() {
        assertThat(namer.call("x1")).isEqualTo("x1_1")
    }
    
    @Test
    fun `test double underscore`() {
        assertThat(namer.call("__x")).isEqualTo("_x")
    }
    
    @Test
    fun `test leading digits`() {
        assertThat(namer.call("123x")).isEqualTo("x")
        assertThat(namer.call("1___x")).isEqualTo("_x_1")
    }
    
    @Test
    fun `test special characters`() {
        assertThat(namer.call("x:y")).isEqualTo("x_y")
        assertThat(namer.call("x<y>")).isEqualTo("x_y_")
        assertThat(namer.call("x,y")).isEqualTo("x_y")
    }
    
    @Test
    fun `test non ascii characters`() {
        val result = namer.call("xéy")
        assertThat(result).doesNotContain("é")
        assertThat(result).startsWith("x")
    }
    
    @Test
    fun `test empty name`() {
        assertThat(namer.call("")).isEqualTo("unnamed")
    }
    
    @Test
    fun `test whitespace name`() {
        assertThat(namer.call("   ")).isEqualTo("unnamed")
    }
    
    @Test
    fun `test keyword name`() {
        val keywords = KeywordSet(setOf("function"))
        val builtins = KeywordSet(emptySet())
        val caseInsensitive = CaseInsensitiveKeywordSet(emptySet())
        namer.reset(module, keywords, builtins, caseInsensitive, emptyList(), output)
        
        assertThat(namer.call("function")).isEqualTo("function_")
    }
    
    @Test
    fun `test builtin identifier`() {
        val keywords = KeywordSet(emptySet())
        val builtins = KeywordSet(setOf("vec3"))
        val caseInsensitive = CaseInsensitiveKeywordSet(emptySet())
        namer.reset(module, keywords, builtins, caseInsensitive, emptyList(), output)
        
        assertThat(namer.call("vec3")).isEqualTo("vec3_")
    }
    
    @Test
    fun `test case insensitive keyword`() {
        val keywords = KeywordSet(emptySet())
        val builtins = KeywordSet(emptySet())
        val caseInsensitive = CaseInsensitiveKeywordSet(setOf("FUNCTION"))
        namer.reset(module, keywords, builtins, caseInsensitive, emptyList(), output)
        
        assertThat(namer.call("function")).isEqualTo("function_")
    }
    
    @Test
    fun `test reserved prefix`() {
        val keywords = KeywordSet(emptySet())
        val builtins = KeywordSet(emptySet())
        val caseInsensitive = CaseInsensitiveKeywordSet(emptySet())
        namer.reset(module, keywords, builtins, caseInsensitive, listOf("gl_"), output)
        
        assertThat(namer.call("gl_position")).isEqualTo("gen_gl_position")
    }
    
    @Test
    fun `test null label with fallback`() {
        assertThat(namer.callOr(null, "fallback")).isEqualTo("fallback")
    }
    
    @Test
    fun `test callOr with non-null label`() {
        assertThat(namer.callOr("x", "fallback")).isEqualTo("x")
    }
    
    // Tests d'intégration avec Module
    
    @Test
    fun `test name types`() {
        module.types.append(Type(name = "MyType", inner = TypeInner.Scalar(Scalar(ScalarKind.SINT, 4))))
        module.types.append(Type(name = null, inner = TypeInner.Scalar(Scalar(ScalarKind.FLOAT, 4))))
        
        val keywords = KeywordSet(emptySet())
        val builtins = KeywordSet(emptySet())
        val caseInsensitive = CaseInsensitiveKeywordSet(emptySet())
        namer.reset(module, keywords, builtins, caseInsensitive, emptyList(), output)
        
        assertThat(output[NameKey.Type(Handle(0))]).isEqualTo("MyType")
        assertThat(output[NameKey.Type(Handle(1))]).isEqualTo("type")
    }
    
    @Test
    fun `test name struct members`() {
        val structType = module.types.append(Type(
            name = "MyStruct",
            inner = TypeInner.Struct(
                span = 8,
                members = listOf(
                    StructMember(name = "a", ty = Handle(0), binding = null, offset = 0),
                    StructMember(name = null, ty = Handle(0), binding = null, offset = 4)
                )
            )
        ))
        
        module.types.append(Type(name = null, inner = TypeInner.Scalar(Scalar(ScalarKind.SINT, 4))))
        
        val keywords = KeywordSet(emptySet())
        val builtins = KeywordSet(emptySet())
        val caseInsensitive = CaseInsensitiveKeywordSet(emptySet())
        namer.reset(module, keywords, builtins, caseInsensitive, emptyList(), output)
        
        assertThat(output[NameKey.Type(structType)]).isEqualTo("MyStruct")
        assertThat(output[NameKey.StructMember(structType, 0)]).isEqualTo("a")
        assertThat(output[NameKey.StructMember(structType, 1)]).isEqualTo("member")
    }
    
    @Test
    fun `test name functions`() {
        val func = Function(
            name = "myFunction",
            arguments = listOf(
                FunctionArgument(name = "x", ty = Handle(0), binding = null)
            ),
            result = null,
            localVariables = emptyList(),
            expressions = emptyList(),
            body = Handle(0),
            namedExpressions = emptyList(),
            termination = FunctionTermination.RETURN
        )
        module.functions.append(func)
        module.types.append(Type(name = null, inner = TypeInner.Scalar(Scalar(ScalarKind.SINT, 4))))
        
        val keywords = KeywordSet(emptySet())
        val builtins = KeywordSet(emptySet())
        val caseInsensitive = CaseInsensitiveKeywordSet(emptySet())
        namer.reset(module, keywords, builtins, caseInsensitive, emptyList(), output)
        
        assertThat(output[NameKey.Function(Handle(0))]).isEqualTo("myFunction")
        assertThat(output[NameKey.FunctionArgument(Handle(0), 0)]).isEqualTo("x")
    }
}
```

---

## ✅ CHECKLIST D'IMPLÉMENTATION

### Structure des Fichiers
- [x] `NameKey.kt` - Toutes les clés d'identification
- [x] `KeywordSet.kt` - Gestion des mots-clés par langage
- [x] `CaseInsensitiveKeywordSet.kt` - Mots-clés insensibles à la casse
- [x] `Namer.kt` - Classe principale avec call(), reset(), sanitize()

### Fonctionnalités Namer
- [x] Sanitization des noms (caractères spéciaux, espaces, etc.)
- [x] Gestion de l'unicité des noms (suffixes)
- [x] Éviter les conflits avec les mots-clés
- [x] Nommer les types (y compris structs et leurs membres)
- [x] Nommer les entry points et leurs arguments/variables
- [x] Nommer les fonctions et leurs arguments/variables
- [x] Nommer les variables globales
- [x] Nommer les constantes
- [x] Nommer les overrides
- [x] Gestion des namespaces locaux (struct, fonction, entry point)
- [x] Noms par défaut pour les entités anonymes

### Mots-clés par Langage
- [x] Mots-clés WGSL
- [x] Mots-clés MSL
- [x] Mots-clés HLSL
- [x] Mots-clés GLSL
- [x] Identifiants built-in WGSL

### Tests
- [x] Tests pour sanitize() (tous les cas spéciaux)
- [x] Tests pour call() (unicité, mots-clés)
- [x] Tests pour callOr()
- [x] Tests d'intégration avec Module
- [x] Tests pour chaque type d'entité (Type, Function, etc.)

### Intégration
- [x] Utiliser Namer dans les backends (MSL, HLSL, GLSL, WGSL)
- [x] Passer les bons mots-clés selon le backend
- [x] Documenter l'API publique

---

## 📖 RÉFÉRENCES

1. **WGSL Specification** : [Lexicon §2.1](https://gpuweb.github.io/gpuweb/wgsl/#lexicon)
2. **Rust Reference** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/namer.rs`
3. **Naga Documentation** : [Namer](https://docs.rs/naga/latest/naga/proc/struct.Namer.html)

---

## 🎯 PLANNING

| Tâche | Durée | Dépendances | Priorité |
|-------|-------|-------------|----------|
| Implémenter NameKey.kt | 4-6h | Aucune | [x] |
| Implémenter KeywordSet.kt | 2-4h | Aucune | [x] |
| Implémenter Namer.kt (base) | 4-8h | NameKey, KeywordSet | [x] |
| Implémenter sanitize() | 4-6h | Namer base | [x] |
| Implémenter call() et unicité | 4h | Namer base | [x] |
| Implémenter le naming des types | 4-6h | Namer base | [x] |
| Implémenter le naming des fonctions | 4h | Namer base | [x] |
| Implémenter le naming des variables | 4h | Namer base | [x] |
| Implémenter le naming des constantes | 2h | Namer base | [x] |
| Implémenter les namespaces | 4h | Namer base | [x] |
| Tests unitaires | 8-12h | Tout | [x] |
| Intégration avec les backends | 4h | Tout | [x] |
| **Total** | **52-80h (1.5-2.5 semaines)** | | |

---

## 🔄 DÉPENDANCES

### Dépendances Internes
- `wgsl:core` : Module IR (Module, Type, Function, etc.)
- `io.ygdrasil.wgsl.arena.Handle`
- `io.ygdrasil.wgsl.ir.*`

### Dépendances Externes
- Aucune (kotlin-stdlib uniquement)

---

## 📝 NOTES

1. **Performance** : Le namer est appelé une fois par module. La complexité est O(n) où n est le nombre total d'entités à nommer. Pour un shader typique, cela représente quelques centaines d'entités, donc la performance n'est pas un problème.

2. **Stabilité** : Les noms générés doivent être stables (déterministes) pour le même module. Cela permet de comparer les sorties entre différentes exécutions.

3. **Compatibilité** : Les noms générés doivent être valides dans tous les langages cibles. Le sanitizer doit être suffisamment strict pour éviter les caractères invalides.

4. **Mots-clés par backend** : Chaque backend a ses propres mots-clés. Le Namer doit accepter un ensemble de mots-clés en paramètre pour éviter les conflits.

5. **Noms par défaut** : Pour les entités anonymes, le namer utilise des noms par défaut comme "type", "function", "param", "local", "global", "member", "const", "override". Ces noms sont choisis pour être descriptifs tout en restant courts.

6. **External Texture Support** : Certains backends (comme MSL) ont besoin de nommer des entités supplémentaires pour les textures externes. Ces entités n'existent pas dans l'IR mais sont créées pendant la génération de code. Les NameKey correspondantes sont fournies pour supporter ce cas d'usage.
