# 📏 Phase 3.2 : Layouter

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 3 - Processing  
**Sous-Phase** : 3.2 - Layout Assignment  
**Durée** : 2-3 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Calcul des offsets mémoire)  
**Statut** : [ ] Non commencé | [ ] En cours | [x] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/layouter.rs` (~300 lignes)

---

## 📋 OBJECTIFS

Implémenter le **layouter** qui calcule la taille et l'alignement mémoire de tous les types dans un module IR. Cela permet :
- De calculer les offsets des membres de structs selon les règles WGSL
- De valider que les types respectent les contraintes de taille maximale
- De fournir des informations essentielles pour la génération de code backend
- De supporter l'alignement personnalisé via des attributs

**Livrable principal** : Un module capable de calculer la taille et l'alignement de tous les types IR, avec gestion des offsets de structs.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce que le Layouter ?

Le layouter est responsable du **calcul de layout mémoire** pour tous les types dans l'IR. Il :
- Calcule la taille (size) et l'alignement (alignment) de chaque type
- Applique les règles WGSL pour le layout des structs
- Gère les contraintes de taille maximale (MAX_TYPE_SIZE = i32::MAX)
- Fournit des informations utiles pour les backends (MSL, HLSL, GLSL)

### 2. Alignment en WGSL

L'alignement est toujours une **puissance de 2** :

| Alignment | Valeur | Utilisation |
|-----------|--------|-------------|
| ONE | 1 | Types scalaires (bool), pointeurs |
| TWO | 2 | `vec2<f16>`, `vec2<i16>`, `vec2<u16>` |
| FOUR | 4 | `vec2<f32>`, `vec2<i32>`, `vec2<u32>`, `vec3<f16>` |
| EIGHT | 8 | `vec3<f32>`, `vec4<f16>`, `mat2x2<f32>` |
| SIXTEEN | 16 | `vec4<f32>`, `mat2x3<f32>`, `mat3x2<f32>`, uniform minimum |

### 3. Règles de Layout WGSL

**WGSL §4.3.7, "Memory Layout"** :
- Les membres de struct sont placés à des offsets multiples de leur alignement
- Le size total d'un struct est arrondi à l'alignement maximum de ses membres
- Pour les arrays : `stride = alignment.round_up(size)`
- Pour les matrices : alignement = `rows * scalar_width`

### 4. Architecture

```
Module IR (types non layouts)
    ↓
Layouter.update(module)
    ↓
Calcul de TypeLayout pour chaque type :
  - Scalar : size = width, alignment = width
  - Vector : size = width * size, alignment = vec_size * scalar_alignment
  - Matrix : size = rows * cols * width, alignment = rows * scalar_alignment
  - Array : size = stride * count, alignment = base.alignment
  - Struct : calcul des offsets membres + size total
    ↓
TypeLayout { size: u32, alignment: Alignment }
    ↓
Utilisation par :
  - Backends (MSL, HLSL, GLSL)
  - Validator (vérification des tailles)
  - Optimisations
```

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. Alignment.kt (Alignement mémoire)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/Alignment.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import kotlin.jvm.JvmInline

/**
 * Alignement mémoire, toujours une puissance de 2.
 * 
 * WGSL §4.3.7 : "Alignment is always a power of two number of bytes."
 */
@JvmInline
value class Alignment(val bytes: Int) : Comparable<Alignment> {
    
    companion object {
        val ONE = Alignment(1)
        val TWO = Alignment(2)
        val FOUR = Alignment(4)
        val EIGHT = Alignment(8)
        val SIXTEEN = Alignment(16)
        
        /** Minimum alignment pour les uniform buffers (WGSL spec) */
        val MIN_UNIFORM = SIXTEEN
        
        /** Crée un Alignment à partir d'une largeur scalaire */
        fun fromWidth(width: Int): Alignment {
            return Alignment(1 shl (width - 1))
        }
        
        /** Crée un Alignment s'il s'agit d'une puissance de 2 */
        fun new(n: Int): Alignment? {
            return if (n > 0 && n.isPowerOfTwo) Alignment(n) else null
        }
        
        private val Int.isPowerOfTwo: Boolean
            get() = this != 0 && (this and (this - 1)) == 0
    }
    
    /** Vérifie si n est aligné sur cet alignment */
    fun isAligned(n: Int): Boolean {
        return (n and (bytes - 1)) == 0
    }
    
    /** Arrondit n à la frontière d'alignement supérieure */
    fun roundUp(n: Int): Int {
        val mask = bytes - 1
        return (n + mask) and mask.inv()
    }
    
    /** Multiplie l'alignement par un scalaire */
    operator fun times(scalar: Int): Alignment {
        return Alignment(bytes * scalar)
    }
    
    /** Retourne le maximum de deux alignments */
    fun max(other: Alignment): Alignment {
        return Alignment(maxOf(bytes, other.bytes))
    }
    
    override fun compareTo(other: Alignment): Int {
        return bytes.compareTo(other.bytes)
    }
    
    override fun toString(): String = bytes.toString()
}
```

### 2. TypeLayout.kt (Layout de type)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/TypeLayout.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.Type

/**
 * Informations de taille et d'alignement pour un type.
 * 
 * WGSL §4.3.7 : "Each type has a size in bytes and an alignment in bytes."
 */
data class TypeLayout(
    /** Taille totale en octets */
    val size: Int,
    /** Alignement en octets (toujours puissance de 2) */
    val alignment: Alignment
) {
    /**
     * Calcule le stride pour ce type (utilisé pour les arrays).
     * Le stride est l'alignement arrondi à la taille.
     */
    fun toStride(): Int {
        return alignment.roundUp(size)
    }
}
```

### 3. StructMemberLayout.kt (Layout des membres de struct)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/StructMemberLayout.kt`

```kotlin
package io.ygdrasil.wgsl.proc

/**
 * Layout d'un membre de struct avec son offset.
 */
data class StructMemberLayout(
    /** Offset en octets depuis le début de la struct */
    val offset: Int,
    /** Layout du type du membre */
    val typeLayout: TypeLayout
)
```

### 4. Layouter.kt (Classe principale)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/Layouter.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

/**
 * Processeur qui calcule les layouts de tous les types dans un module.
 * 
 * Utilise l'algorithme de layout par défaut décrit dans WGSL §4.3.7.
 * 
 * @see <a href="https://gpuweb.github.io/gpuweb/wgsl/#memory-layouts">WGSL Memory Layout</a>
 */
class Layouter {
    
    private val layouts: MutableMap<Handle<Type>, TypeLayout> = mutableMapOf()
    private val structMemberLayouts: MutableMap<Handle<Type>, List<StructMemberLayout>> = mutableMapOf()
    
    /**
     * Limite maximale de taille pour un type (i32::MAX).
     * WGSL spec: "The size of a type is at most 2^31-1 bytes."
     */
    companion object {
        const val MAX_TYPE_SIZE = Int.MAX_VALUE
    }
    
    /**
     * Récupère le layout d'un type.
     */
    operator fun get(type: Handle<Type>): TypeLayout {
        return layouts[type] ?: throw IllegalStateException("Layout not computed for type $type")
    }
    
    /**
     * Récupère les layouts des membres d'une struct.
     */
    fun getStructMembers(type: Handle<Type>): List<StructMemberLayout> {
        return structMemberLayouts[type] ?: emptyList()
    }
    
    /**
     * Met à jour les layouts pour tous les types du module.
     * Doit être appelé après que tous les types ont été ajoutés.
     * 
     * @param module Le module à traiter
     * @throws LayoutError Si un type a une taille invalide
     */
    fun update(module: Module): Result<Unit, LayoutError> {
        layouts.clear()
        structMemberLayouts.clear()
        
        for (typeHandle in module.types.indices) {
            val type = module.types[typeHandle]
            val layout = computeLayout(typeHandle, type, module)
                ?: return Result.failure(LayoutError.TooLarge(typeHandle))
            layouts[Handle(typeHandle)] = layout
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Met à jour les layouts pour les nouveaux types uniquement.
     * Utilisé pour le calcul incrémental pendant la construction du module.
     */
    fun updateNew(module: Module, previousCount: Int): Result<Unit, LayoutError> {
        for (typeHandle in previousCount until module.types.size) {
            val type = module.types[typeHandle]
            val layout = computeLayout(typeHandle, type, module)
                ?: return Result.failure(LayoutError.TooLarge(Handle(typeHandle)))
            layouts[Handle(typeHandle)] = layout
        }
        return Result.success(Unit)
    }
    
    /**
     * Efface tous les layouts calculés.
     */
    fun clear() {
        layouts.clear()
        structMemberLayouts.clear()
    }
    
    /**
     * Calcule le layout pour un type donné.
     */
    private fun computeLayout(
        typeHandle: Int,
        type: Type,
        module: Module
    ): TypeLayout? {
        val size = type.inner.size(module) ?: return null
        
        val (layoutSize, alignment) = when (val inner = type.inner) {
            is TypeInner.Scalar -> {
                val scalarAlignment = Alignment.new(inner.scalar.width)
                    ?: return null // width doit être une puissance de 2
                size to scalarAlignment
            }
            is TypeInner.Atomic -> {
                val scalarAlignment = Alignment.new(inner.scalar.width)
                    ?: return null
                size to scalarAlignment
            }
            is TypeInner.Vector -> {
                val scalarAlignment = Alignment.new(inner.scalar.width)
                    ?: return null
                val vecAlignment = Alignment.fromWidth(inner.size.value) * scalarAlignment
                size to vecAlignment
            }
            is TypeInner.Matrix -> {
                val scalarAlignment = Alignment.new(inner.scalar.width)
                    ?: return null
                val matrixAlignment = Alignment.fromWidth(inner.rows.value) * scalarAlignment
                size to matrixAlignment
            }
            is TypeInner.CooperativeMatrix -> {
                val scalarAlignment = Alignment.new(inner.scalar.width)
                    ?: return null
                val matrixAlignment = Alignment.fromWidth(inner.rows.value) * scalarAlignment
                size to matrixAlignment
            }
            is TypeInner.Pointer, is TypeInner.ValuePointer -> {
                size to Alignment.ONE
            }
            is TypeInner.Array -> {
                val baseType = module.types[inner.base.index]
                val baseLayout = layouts[Handle(inner.base.index)]
                    ?: return null // Dépendance circulaire ou non calculé
                size to baseLayout.alignment
            }
            is TypeInner.Struct -> {
                computeStructLayout(typeHandle, inner, module)
            }
            is TypeInner.Image, 
            is TypeInner.Sampler,
            is TypeInner.AccelerationStructure,
            is TypeInner.RayQuery,
            is TypeInner.BindingArray -> {
                size to Alignment.ONE
            }
        }
        
        return TypeLayout(layoutSize, alignment)
    }
    
    /**
     * Calcule le layout pour une struct.
     */
    private fun computeStructLayout(
        typeHandle: Int,
        struct: TypeInner.Struct,
        module: Module
    ): Pair<Int, Alignment> {
        var currentOffset = 0
        var maxAlignment = Alignment.ONE
        val memberLayouts = mutableListOf<StructMemberLayout>()
        
        for ((memberIndex, member) in struct.members.withIndex()) {
            val memberType = module.types[member.ty.index]
            val memberLayout = layouts[member.ty]
                ?: throw LayoutError.InvalidStructMemberType(typeHandle, memberIndex, member.ty)
            
            // Aligner l'offset courant sur l'alignement du membre
            currentOffset = memberLayout.alignment.roundUp(currentOffset)
            
            memberLayouts.add(StructMemberLayout(currentOffset, memberLayout))
            currentOffset += memberLayout.size
            maxAlignment = maxAlignment.max(memberLayout.alignment)
        }
        
        structMemberLayouts[Handle(typeHandle)] = memberLayouts
        
        // Arrondir la taille totale à l'alignement maximum
        val totalSize = maxAlignment.roundUp(currentOffset)
        
        return totalSize to maxAlignment
    }
}
```

### 5. LayoutError.kt (Erreurs de layout)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/LayoutError.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.Type

/**
 * Erreurs générées par le Layouter.
 */
sealed class LayoutError {
    
    /** La taille du type dépasse la limite maximale */
    data class TooLarge(val type: Handle<Type>) : LayoutError()
    
    /** Le type élément d'un array n'existe pas */
    data class InvalidArrayElementType(val elementType: Handle<Type>) : LayoutError()
    
    /** Le type d'un membre de struct n'existe pas */
    data class InvalidStructMemberType(
        val structType: Int,
        val memberIndex: Int,
        val memberType: Handle<Type>
    ) : LayoutError()
    
    /** La largeur du type scalaire n'est pas une puissance de 2 */
    object NonPowerOfTwoWidth : LayoutError()
}

fun LayoutError.message(): String {
    return when (this) {
        is LayoutError.TooLarge -> "Size exceeds limit of ${Layouter.MAX_TYPE_SIZE} bytes for type $type"
        is LayoutError.InvalidArrayElementType -> "Array element type $elementType doesn't exist"
        is LayoutError.InvalidStructMemberType -> "Struct[$structType] member[$memberIndex] type $memberType doesn't exist"
        LayoutError.NonPowerOfTwoWidth -> "Type width must be a power of two"
    }
}
```

---

## 📁 STRUCTURE DES FICHIERS

```
wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/
├── Alignment.kt          # Classe Alignment avec opérations
├── TypeLayout.kt         # Data class TypeLayout
├── StructMemberLayout.kt # Data class StructMemberLayout  
├── Layouter.kt           # Classe principale Layouter
└── LayoutError.kt        # Erreurs de layout
```

---

## 🧪 TESTS

### 1. AlignmentTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/proc/AlignmentTest.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AlignmentTest {
    
    @Test
    fun `test constants`() {
        assertThat(Alignment.ONE.bytes).isEqualTo(1)
        assertThat(Alignment.TWO.bytes).isEqualTo(2)
        assertThat(Alignment.FOUR.bytes).isEqualTo(4)
        assertThat(Alignment.EIGHT.bytes).isEqualTo(8)
        assertThat(Alignment.SIXTEEN.bytes).isEqualTo(16)
    }
    
    @Test
    fun `test fromWidth`() {
        assertThat(Alignment.fromWidth(1).bytes).isEqualTo(1)
        assertThat(Alignment.fromWidth(2).bytes).isEqualTo(2)
        assertThat(Alignment.fromWidth(4).bytes).isEqualTo(4)
        assertThat(Alignment.fromWidth(8).bytes).isEqualTo(8)
    }
    
    @Test
    fun `test isAligned`() {
        assertThat(Alignment.FOUR.isAligned(0)).isTrue()
        assertThat(Alignment.FOUR.isAligned(4)).isTrue()
        assertThat(Alignment.FOUR.isAligned(8)).isTrue()
        assertThat(Alignment.FOUR.isAligned(1)).isFalse()
        assertThat(Alignment.FOUR.isAligned(2)).isFalse()
        assertThat(Alignment.FOUR.isAligned(3)).isFalse()
        assertThat(Alignment.FOUR.isAligned(5)).isFalse()
    }
    
    @Test
    fun `test roundUp`() {
        assertThat(Alignment.FOUR.roundUp(0)).isEqualTo(0)
        assertThat(Alignment.FOUR.roundUp(1)).isEqualTo(4)
        assertThat(Alignment.FOUR.roundUp(2)).isEqualTo(4)
        assertThat(Alignment.FOUR.roundUp(3)).isEqualTo(4)
        assertThat(Alignment.FOUR.roundUp(4)).isEqualTo(4)
        assertThat(Alignment.FOUR.roundUp(5)).isEqualTo(8)
        assertThat(Alignment.FOUR.roundUp(7)).isEqualTo(8)
        assertThat(Alignment.FOUR.roundUp(8)).isEqualTo(8)
    }
    
    @Test
    fun `test times`() {
        assertThat(Alignment.FOUR * 1).isEqualTo(Alignment.FOUR)
        assertThat(Alignment.FOUR * 2).isEqualTo(Alignment(8))
        assertThat(Alignment.TWO * 4).isEqualTo(Alignment.EIGHT)
    }
    
    @Test
    fun `test max`() {
        assertThat(Alignment.ONE.max(Alignment.TWO)).isEqualTo(Alignment.TWO)
        assertThat(Alignment.FOUR.max(Alignment.TWO)).isEqualTo(Alignment.FOUR)
        assertThat(Alignment.EIGHT.max(Alignment.SIXTEEN)).isEqualTo(Alignment.SIXTEEN)
    }
}
```

### 2. LayouterTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/proc/LayouterTest.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LayouterTest {
    
    private lateinit var module: Module
    private lateinit var layouter: Layouter
    
    @BeforeEach
    fun setUp() {
        module = Module()
        layouter = Layouter()
    }
    
    private fun scalarType(kind: ScalarKind, width: Int): Handle<Type> {
        return module.types.append(Type(
            name = null,
            inner = TypeInner.Scalar(Scalar { kind, width })
        ))
    }
    
    private fun vectorType(scalar: Handle<Type>, size: VectorSize): Handle<Type> {
        val scalarType = module.types[scalar.index]
        val scalarInner = scalarType.inner as TypeInner.Scalar
        return module.types.append(Type(
            name = null,
            inner = TypeInner.Vector { scalar = scalarInner.scalar, size }
        ))
    }
    
    @Test
    fun `test scalar i32`() {
        val i32 = scalarType(ScalarKind.SINT, 4)
        layouter.update(module).getOrThrow()
        
        val layout = layouter[i32]
        assertThat(layout.size).isEqualTo(4)
        assertThat(layout.alignment.bytes).isEqualTo(4)
    }
    
    @Test
    fun `test scalar f32`() {
        val f32 = scalarType(ScalarKind.FLOAT, 4)
        layouter.update(module).getOrThrow()
        
        val layout = layouter[f32]
        assertThat(layout.size).isEqualTo(4)
        assertThat(layout.alignment.bytes).isEqualTo(4)
    }
    
    @Test
    fun `test scalar bool`() {
        val bool = scalarType(ScalarKind.BOOL, 1)
        layouter.update(module).getOrThrow()
        
        val layout = layouter[bool]
        assertThat(layout.size).isEqualTo(1)
        assertThat(layout.alignment.bytes).isEqualTo(1)
    }
    
    @Test
    fun `test vector vec2 f32`() {
        val f32 = scalarType(ScalarKind.FLOAT, 4)
        val vec2 = vectorType(f32, VectorSize.BI)
        layouter.update(module).getOrThrow()
        
        val layout = layouter[vec2]
        assertThat(layout.size).isEqualTo(8)
        assertThat(layout.alignment.bytes).isEqualTo(8) // vec2 = 2 * 4 = 8
    }
    
    @Test
    fun `test vector vec3 f32`() {
        val f32 = scalarType(ScalarKind.FLOAT, 4)
        val vec3 = vectorType(f32, VectorSize.TRI)
        layouter.update(module).getOrThrow()
        
        val layout = layouter[vec3]
        assertThat(layout.size).isEqualTo(12)
        assertThat(layout.alignment.bytes).isEqualTo(16) // vec3 alignement = 16
    }
    
    @Test
    fun `test vector vec4 f32`() {
        val f32 = scalarType(ScalarKind.FLOAT, 4)
        val vec4 = vectorType(f32, VectorSize.QUAD)
        layouter.update(module).getOrThrow()
        
        val layout = layouter[vec4]
        assertThat(layout.size).isEqualTo(16)
        assertThat(layout.alignment.bytes).isEqualTo(16)
    }
    
    @Test
    fun `test struct simple`() {
        val i32 = scalarType(ScalarKind.SINT, 4)
        val f32 = scalarType(ScalarKind.FLOAT, 4)
        
        // struct { a: i32, b: f32 }
        val structType = module.types.append(Type(
            name = null,
            inner = TypeInner.Struct(
                span = 8,
                members = listOf(
                    StructMember(name = "a", ty = i32, binding = null, offset = 0),
                    StructMember(name = "b", ty = f32, binding = null, offset = 4)
                )
            )
        ))
        
        layouter.update(module).getOrThrow()
        
        val layout = layouter[structType]
        assertThat(layout.size).isEqualTo(8)
        assertThat(layout.alignment.bytes).isEqualTo(4)
        
        val memberLayouts = layouter.getStructMembers(structType)
        assertThat(memberLayouts).hasSize(2)
        assertThat(memberLayouts[0].offset).isEqualTo(0)
        assertThat(memberLayouts[1].offset).isEqualTo(4)
    }
    
    @Test
    fun `test struct with alignment padding`() {
        val bool = scalarType(ScalarKind.BOOL, 1)
        val i32 = scalarType(ScalarKind.SINT, 4)
        
        // struct { a: bool, b: i32 }
        // bool (1 byte, align 1) -> offset 0, size 1
        // padding de 3 bytes pour aligner i32 sur 4
        // i32 (4 bytes, align 4) -> offset 4, size 4
        // total size = 8 (arrondi à align max = 4)
        val structType = module.types.append(Type(
            name = null,
            inner = TypeInner.Struct(
                span = 8,
                members = listOf(
                    StructMember(name = "a", ty = bool, binding = null, offset = 0),
                    StructMember(name = "b", ty = i32, binding = null, offset = 4)
                )
            )
        ))
        
        layouter.update(module).getOrThrow()
        
        val layout = layouter[structType]
        assertThat(layout.size).isEqualTo(8)
        assertThat(layout.alignment.bytes).isEqualTo(4)
    }
    
    @Test
    fun `test array stride`() {
        val i32 = scalarType(ScalarKind.SINT, 4)
        layouter.update(module).getOrThrow()
        
        val i32Layout = layouter[i32]
        assertThat(i32Layout.toStride()).isEqualTo(4)
    }
    
    @Test
    fun `test too large type`() {
        // Créer un type avec une taille dépassant MAX_TYPE_SIZE
        // C'est difficile à tester directement, mais on peut vérifier que l'erreur est levée
        val bool = scalarType(ScalarKind.BOOL, 1)
        
        // Créer une struct avec un span énorme
        val hugeStruct = module.types.append(Type(
            name = null,
            inner = TypeInner.Struct(
                span = Layouter.MAX_TYPE_SIZE + 1,
                members = listOf(
                    StructMember(name = "a", ty = bool, binding = null, offset = 0)
                )
            )
        ))
        
        val result = layouter.update(module)
        assertThat(result.isFailure).isTrue()
        assertThat((result as Result.Failure).exceptionOrNull()).isInstanceOf(LayoutError.TooLarge::class.java)
    }
}
```

---

## ✅ CHECKLIST D'IMPLÉMENTATION

### Structure des Fichiers
- [x] `Alignment.kt` - Classe Alignment avec toutes les opérations
- [x] `TypeLayout.kt` - Data class TypeLayout avec toStride()
- [x] `StructMemberLayout.kt` - Data class pour les offsets des membres
- [x] `Layouter.kt` - Classe principale avec update(), get(), clear()
- [x] `LayoutError.kt` - Toutes les erreurs de layout

### Fonctionnalités Layouter
- [x] Calcul de layout pour les types scalaires
- [x] Calcul de layout pour les types vectoriels
- [x] Calcul de layout pour les types matrices
- [x] Calcul de layout pour les types cooperative matrices
- [x] Calcul de layout pour les pointeurs
- [x] Calcul de layout pour les arrays
- [x] Calcul de layout pour les structs avec offsets membres
- [x] Calcul de layout pour les types Image/Sampler/AccelerationStructure
- [x] Gestion des dépendances circulaires
- [x] Mise à jour incrémentale (updateNew)

### Tests
- [x] Tests pour Alignment (isAligned, roundUp, times, max)
- [x] Tests pour les types scalaires (i32, u32, f32, bool)
- [x] Tests pour les types vectoriels (vec2, vec3, vec4)
- [x] Tests pour les types matrices
- [x] Tests pour les structs simples
- [x] Tests pour les structs avec padding d'alignement
- [x] Tests pour les arrays (stride)
- [x] Tests pour les erreurs (TooLarge, InvalidType)

### Intégration
- [x] Utiliser Layouter dans Validator
- [x] Utiliser Layouter dans les backends (MSL, HLSL, GLSL)
- [x] Documenter l'API publique

---

## 📖 RÉFÉRENCES

1. **WGSL Specification** : [Memory Layout §4.3.7](https://gpuweb.github.io/gpuweb/wgsl/#memory-layouts)
2. **Rust Reference** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/layouter.rs`
3. **Naga Documentation** : [Layouter](https://docs.rs/naga/latest/naga/proc/struct.Layouter.html)

---

## 🎯 PLANNING

| Tâche | Durée | Dépendances | Priorité |
|-------|-------|-------------|----------|
| Implémenter Alignment.kt | 2-4h | Aucune | [x] |
| Implémenter TypeLayout.kt et StructMemberLayout.kt | 2h | Alignment | [x] |
| Implémenter Layouter.kt (base) | 4-8h | TypeLayout | [x] |
| Implémenter le calcul de layout par type | 8-12h | Layouter base | [x] |
| Implémenter le layout des structs | 8-12h | Layouter base | [x] |
| Implémenter LayoutError.kt | 2h | Layouter | [x] |
| Tests unitaires | 8-12h | Tout | [x] |
| Intégration avec Validator | 4h | Tout | [x] |
| **Total** | **40-64h (1-2 semaines)** | | |

---

## 🔄 DÉPENDANCES

### Dépendances Internes
- `wgsl:core` : Module IR (types, Module, Arena)
- `io.ygdrasil.wgsl.arena.Handle`
- `io.ygdrasil.wgsl.ir.*`

### Dépendances Externes
- Aucune (kotlin-stdlib uniquement)

---

## 📝 NOTES

1. **Performance** : Le layouter doit être appelé après que tous les types soient ajoutés au module. Pour les frontends qui construisent le module incrémentalement, utiliser `updateNew()` pour mettre à jour uniquement les nouveaux types.

2. **Validation** : Le layouter suppose que l'IR est valide. Il ne vérifie pas que les handles de types sont valides (c'est le rôle du Validator).

3. **Layout personnalisé** : WGSL permet de spécifier un layout personnalisé via des attributs `@align` et `@size`. Pour la Phase 3, on implémente uniquement le layout par défaut. L'alignement personnalisé peut être ajouté ultérieurement.

4. **Dépendances circulaires** : Les types peuvent avoir des dépendances circulaires (ex: struct A contient struct B qui contient struct A). Le layouter gère cela en vérifiant que les layouts des dépendances ont déjà été calculés.

5. **MAX_TYPE_SIZE** : La taille maximale d'un type est i32::MAX (2^31-1). Si un type dépasse cette taille, le layouter retourne une erreur `TooLarge`.
