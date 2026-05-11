# 📦 Phase 1.1 : Système Arena/Handle

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 1 - Fondations  
**Sous-Phase** : 1.1 - Arena System  
**Durée** : 1-2 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Fondamentale pour tout le reste)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/arena/mod.rs`

---

## 📋 OBJECTIFS

Implémenter le **système de gestion mémoire Arena/Handle** qui est au cœur de Naga.  
Ce système permet une **gestion efficace de la mémoire** sans GC overhead (en Rust) et offre une **alternative type-safe** aux pointeurs bruts.

---

## 🎯 CONCEPTS CLÉS

### Pourquoi Arena/Handle ?

1. **Performance** : Éviter les allocations individuelles (les objets sont stockés dans des tableaux contigus)
2. **Type Safety** : `Handle<T>` est typé, donc le compilateur peut détecter les erreurs de type
3. **Stabilité** : Les Handles sont des indices, donc ils restent valides même si l'Arena grandit
4. **Simplicité** : Pas besoin de GC, pas de pointeurs, pas de problèmes de lifetime

### Comparaison Rust vs Kotlin

| Concept | Rust | Kotlin | Notes |
|---------|------|--------|-------|
| **Handle** | `pub struct Handle<T>(u32)` | `@JvmInline value class Handle<T>(val index: Int)` | Kotlin utilise value class pour éviter allocation |
| **Arena** | `pub struct Arena<T> { data: Vec<T> }` | `class Arena<T> { private val data: MutableList<T> }` | MutableList au lieu de Vec |
| **UniqueArena** | `pub struct UniqueArena<T> { data: Vec<T>, map: HashMap<T, u32> }` | `class UniqueArena<T> { private val data: MutableList<T>, indexMap: MutableMap<T, Int> }` | Dédoublonnage automatique |
| **Memory** | Stack allocation possible | Toujours heap (mais optimisé avec value class) | JVM limite les optimisations |

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. Handle.kt

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/arena/Handle.kt`

```kotlin
package io.ygdrasil.wgsl.arena

import kotlin.jvm.JvmInline
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Handle est un wrapper type-safe autour d'un Int (index dans une Arena).
 * 
 * Utilise @JvmInline pour éviter l'allocation d'objet au runtime.
 * Cela signifie que Handle<T> est stocké comme un Int simple, mais avec type safety au compile-time.
 * 
 * @param T Le type de l'élément référencé dans l'Arena
 * @property index L'index dans l'Arena (0-based)
 */
@Serializable(HandleSerializer::class)
@JvmInline
value class Handle<T>(val index: Int) {
    
    /**
     * Handle invalide, utilisé comme valeur par défaut ou sentinelle.
     */
    companion object {
        /** Handle invalide (index = -1) */
        val INVALID: Handle<Nothing> = Handle(-1)
        
        /**
         * Crée un Handle à partir d'un index.
         * Utilisé en interne par Arena.
         */
        internal fun <T> fromIndex(index: Int): Handle<T> = Handle(index)
    }
    
    /**
     * Vérifie si ce Handle est valide (index >= 0).
     */
    fun isValid(): Boolean = index >= 0
    
    /**
     * Vérifie si ce Handle est invalide.
     */
    fun isInvalid(): Boolean = !isValid()
    
    /**
     * Compare deux Handles en ignorant leur type générique.
     * Utile pour vérifier si deux Handles pointent vers le même élément.
     */
    inline fun <reified U> equalsIgnoreType(other: Handle<U>): Boolean {
        return this.index == other.index
    }
    
    /**
     * Convertit ce Handle en Handle<Any> pour comparaison générique.
     */
    fun toUntyped(): Handle<Any> = Handle(index)
    
    override fun toString(): String = "Handle(${index})"
}

/**
 * Sérialiseur personnalisé pour Handle<T>.
 * Les Handles sont sérialisés comme des entiers simples.
 */
object HandleSerializer : KSerializer<Handle<*>> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("Handle", PrimitiveKind.INT)
    
    override fun serialize(encoder: Encoder, value: Handle<*>) {
        encoder.encodeInt(value.index)
    }
    
    override fun deserialize(decoder: Decoder): Handle<*> {
        return Handle(decoder.decodeInt())
    }
}

/**
 * Extension pour créer un Handle<Nothing> invalide.
 */
fun invalidHandle(): Handle<Nothing> = Handle.INVALID

/**
 * Extension pour vérifier si un Handle est valide.
 */
fun <T> Handle<T>?.isValidOrNull(): Boolean = this?.isValid() ?: false
```

---

### 2. Arena.kt

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/arena/Arena.kt`

```kotlin
package io.ygdrasil.wgsl.arena

import kotlinx.serialization.Serializable

/**
 * Arena pour stocker des éléments de manière efficace.
 * 
 * Les Arenas sont utilisées pour stocker des collections d'éléments qui :
 * - Sont accédés par index (Handle) plutôt que par référence
 * - Ont une durée de vie limitée au scope de leur Arena
 * - Doivent être stockés de manière contiguë pour la performance
 * 
 * @param T Le type des éléments stockés dans l'Arena
 */
@Serializable
class Arena<T> : Iterable<T>, Collection<T>, MutableCollection<T> {
    
    private val data: MutableList<T> = mutableListOf()
    
    /**
     * Nombre d'éléments dans l'Arena.
     */
    override val size: Int get() = data.size
    
    /**
     * Ajoute un élément à l'Arena et retourne son Handle.
     * 
     * @param value L'élément à ajouter
     * @return Handle<T> pointant vers l'élément ajouté
     */
    fun append(value: T): Handle<T> {
        val index = data.size
        data.add(value)
        return Handle.fromIndex(index)
    }
    
    /**
     * Ajoute plusieurs éléments à l'Arena.
     * 
     * @param values Les éléments à ajouter
     * @return Liste de Handles pointant vers les éléments ajoutés
     */
    fun appendAll(values: Iterable<T>): List<Handle<T>> {
        val startIndex = data.size
        data.addAll(values)
        return (startIndex until data.size).map { Handle.fromIndex<Any>(it) as Handle<T> }
    }
    
    /**
     * Récupère un élément par son Handle.
     * 
     * @param handle Le Handle de l'élément
     * @return L'élément correspondant
     * @throws IndexOutOfBoundsException si le Handle est invalide
     */
    operator fun get(handle: Handle<T>): T {
        require(handle.isValid()) { "Invalid handle: ${handle.index}" }
        return data[handle.index]
    }
    
    /**
     * Récupère un élément par son Handle, ou null si invalide.
     */
    fun getOrNull(handle: Handle<T>): T? {
        return if (handle.isValid() && handle.index < data.size) {
            data[handle.index]
        } else {
            null
        }
    }
    
    /**
     * Récupère un élément par index.
     */
    operator fun get(index: Int): T = data[index]
    
    /**
     * Vérifie si l'Arena est vide.
     */
    override fun isEmpty(): Boolean = data.isEmpty()
    
    /**
     * Vérifie si l'Arena contient un élément.
     */
    override fun contains(element: T): Boolean = data.contains(element)
    
    /**
     * Vérifie si l'Arena contient un élément correspondant au prédicat.
     */
    override fun containsAll(elements: Collection<T>): Boolean = data.containsAll(elements)
    
    /**
     * Retourne un itérateur sur tous les éléments.
     */
    override fun iterator(): Iterator<T> = data.iterator()
    
    /**
     * Applique une action à chaque élément avec son Handle.
     * 
     * @param action Action à appliquer (Handle<T>, T) -> Unit
     */
    inline fun forEachWithHandle(action: (Handle<T>, T) -> Unit) {
        data.forEachIndexed { index, value ->
            action(Handle.fromIndex(index), value)
        }
    }
    
    /**
     * Applique une action à chaque élément avec son index.
     */
    inline fun forEachIndexed(action: (Int, T) -> Unit) {
        data.forEachIndexed(action)
    }
    
    /**
     * Transforme chaque élément en un autre type.
     */
    inline fun <R> map(transform: (T) -> R): List<R> = data.map(transform)
    
    /**
     * Transforme chaque élément avec son Handle.
     */
    inline fun <R> mapWithHandle(transform: (Handle<T>, T) -> R): List<R> {
        return data.mapIndexed { index, value ->
            transform(Handle.fromIndex(index), value)
        }
    }
    
    /**
     * Filtre les éléments.
     */
    fun filter(predicate: (T) -> Boolean): List<T> = data.filter(predicate)
    
    /**
     * Filtre les éléments avec leur Handle.
     */
    fun filterWithHandle(predicate: (Handle<T>, T) -> Boolean): List<T> {
        return data.filterIndexed { index, value ->
            predicate(Handle.fromIndex(index), value)
        }
    }
    
    /**
     * Trouve le premier élément correspondant au prédicat.
     */
    fun find(predicate: (T) -> Boolean): T? = data.find(predicate)
    
    /**
     * Trouve le Handle du premier élément correspondant au prédicat.
     */
    fun findHandle(predicate: (T) -> Boolean): Handle<T>? {
        data.forEachIndexed { index, value ->
            if (predicate(value)) return Handle.fromIndex(index)
        }
        return null
    }
    
    /**
     * Trouve le Handle et l'élément correspondant au prédicat.
     */
    fun findEntry(predicate: (T) -> Boolean): Pair<Handle<T>, T>? {
        data.forEachIndexed { index, value ->
            if (predicate(value)) return Handle.fromIndex(index) to value
        }
        return null
    }
    
    /**
     * Vérifie si un prédicat est vrai pour tous les éléments.
     */
    fun all(predicate: (T) -> Boolean): Boolean = data.all(predicate)
    
    /**
     * Vérifie si un prédicat est vrai pour au moins un élément.
     */
    fun any(predicate: (T) -> Boolean): Boolean = data.any(predicate)
    
    /**
     * Compte le nombre d'éléments correspondant au prédicat.
     */
    fun count(predicate: (T) -> Boolean): Int = data.count(predicate)
    
    /**
     * Trie les éléments.
     */
    fun sortedWith(comparator: Comparator<in T>): List<T> = data.sortedWith(comparator)
    
    /**
     * Retourne une sous-liste.
     */
    fun slice(indices: IntRange): List<T> = data.slice(indices)
    
    /**
     * Retourne le dernier élément.
     */
    fun last(): T = data.last()
    
    /**
     * Retourne le dernier élément ou null si vide.
     */
    fun lastOrNull(): T? = data.lastOrNull()
    
    /**
     * Retourne le premier élément.
     */
    fun first(): T = data.first()
    
    /**
     * Retourne le premier élément ou null si vide.
     */
    fun firstOrNull(): T? = data.firstOrNull()
    
    /**
     * Retourne l'élément à l'index donné.
     */
    fun elementAt(index: Int): T = data.elementAt(index)
    
    /**
     * Retourne l'élément à l'index donné ou null.
     */
    fun elementAtOrNull(index: Int): T? = data.elementAtOrNull(index)
    
    /**
     * Retourne l'index d'un élément.
     */
    fun indexOf(element: T): Int = data.indexOf(element)
    
    /**
     * Retourne le Handle d'un élément.
     */
    fun handleOf(element: T): Handle<T>? {
        val index = data.indexOf(element)
        return if (index >= 0) Handle.fromIndex(index) else null
    }
    
    /**
     * Vide l'Arena.
     */
    fun clear() {
        data.clear()
    }
    
    /**
     * Crée une copie de l'Arena.
     */
    fun copy(): Arena<T> {
        val copy = Arena<T>()
        copy.data.addAll(data)
        return copy
    }
    
    /**
     * Convertit en liste.
     */
    fun toList(): List<T> = data.toList()
    
    /**
     * Convertit en tableau.
     */
    fun toArray(): Array<Any> = data.toArray()
    
    // ===== Implémentation de Collection =====
    
    override fun add(element: T): Boolean {
        data.add(element)
        return true
    }
    
    override fun addAll(elements: Collection<T>): Boolean {
        return data.addAll(elements)
    }
    
    override fun remove(element: T): Boolean {
        return data.remove(element)
    }
    
    override fun removeAll(elements: Collection<T>): Boolean {
        return data.removeAll(elements)
    }
    
    override fun retainAll(elements: Collection<T>): Boolean {
        return data.retainAll(elements)
    }
    
    // ===== Opérateurs =====
    
    operator fun set(handle: Handle<T>, value: T) {
        require(handle.isValid()) { "Invalid handle: ${handle.index}" }
        data[handle.index] = value
    }
    
    operator fun plusAssign(value: T) {
        append(value)
    }
    
    operator fun plusAssign(values: Iterable<T>) {
        appendAll(values)
    }
}

/**
 * Crée une Arena vide.
 */
fun <T> arenaOf(): Arena<T> = Arena()

/**
 * Crée une Arena avec des éléments initiaux.
 */
fun <T> arenaOf(vararg elements: T): Arena<T> {
    val arena = Arena<T>()
    elements.forEach { arena.append(it) }
    return arena
}

/**
 * Crée une Arena à partir d'une collection.
 */
fun <T> arenaOf(collection: Iterable<T>): Arena<T> {
    val arena = Arena<T>()
    collection.forEach { arena.append(it) }
    return arena
}
```

---

### 3. UniqueArena.kt

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/arena/UniqueArena.kt`

```kotlin
package io.ygdrasil.wgsl.arena

import kotlinx.serialization.Serializable

/**
 * Arena qui garantit l'unicité des éléments.
 * 
 * Contrairement à Arena, UniqueArena vérifie si un élément existe déjà avant de l'ajouter.
 * Si l'élément existe, le Handle existant est retourné.
 * 
 * Utilisé principalement pour les types (Type), où plusieurs définitions identiques
 * doivent partager le même Handle.
 * 
 * @param T Le type des éléments stockés (doit implémenter Equatable pour comparaison)
 */
@Serializable
class UniqueArena<T> where T : Equatable {
    
    private val data: MutableList<T> = mutableListOf()
    private val indexMap: MutableMap<T, Int> = mutableMapOf()
    
    /**
     * Nombre d'éléments uniques dans l'Arena.
     */
    val size: Int get() = data.size
    
    /**
     * Ajoute un élément à l'Arena. Si l'élément existe déjà, retourne le Handle existant.
     * 
     * @param value L'élément à ajouter
     * @return Handle<T> pointant vers l'élément (existant ou nouveau)
     */
    fun append(value: T): Handle<T> {
        return indexMap.getOrPut(value) {
            val index = data.size
            data.add(value)
            index
        }.let { Handle.fromIndex(it) }
    }
    
    /**
     * Ajoute plusieurs éléments à l'Arena.
     */
    fun appendAll(values: Iterable<T>): List<Handle<T>> {
        return values.map { append(it) }
    }
    
    /**
     * Récupère un élément par son Handle.
     */
    operator fun get(handle: Handle<T>): T {
        require(handle.isValid()) { "Invalid handle: ${handle.index}" }
        return data[handle.index]
    }
    
    /**
     * Récupère un élément par son Handle, ou null si invalide.
     */
    fun getOrNull(handle: Handle<T>): T? {
        return if (handle.isValid() && handle.index < data.size) {
            data[handle.index]
        } else {
            null
        }
    }
    
    /**
     * Vérifie si l'Arena contient un élément spécifique.
     */
    fun contains(value: T): Boolean = value in indexMap
    
    /**
     * Trouve le Handle d'un élément existant.
     */
    fun findHandle(value: T): Handle<T>? {
        return indexMap[value]?.let { Handle.fromIndex(it) }
    }
    
    /**
     * Vérifie si l'Arena est vide.
     */
    fun isEmpty(): Boolean = data.isEmpty()
    
    /**
     * Applique une action à chaque élément avec son Handle.
     */
    inline fun forEachWithHandle(action: (Handle<T>, T) -> Unit) {
        data.forEachIndexed { index, value ->
            action(Handle.fromIndex(index), value)
        }
    }
    
    /**
     * Retourne une liste de tous les éléments.
     */
    fun toList(): List<T> = data.toList()
    
    /**
     * Vide l'Arena.
     */
    fun clear() {
        data.clear()
        indexMap.clear()
    }
    
    /**
     * Crée une copie de l'Arena.
     */
    fun copy(): UniqueArena<T> {
        val copy = UniqueArena<T>()
        copy.data.addAll(data)
        copy.indexMap.putAll(indexMap)
        return copy
    }
}

/**
 * Interface pour les types qui peuvent être comparés pour équivalence.
 * Utilisé par UniqueArena pour détecter les doublons.
 */
interface Equatable {
    /**
     * Vérifie si cet objet est équivalent à un autre.
     * 
     * Contrairement à equals(), isEquivalentTo() est utilisé pour comparer
     * des objets qui sont sémantiquement équivalents mais pas nécessairement
     * la même instance (ex: deux Type avec la même structure).
     */
    fun isEquivalentTo(other: Any): Boolean
}

/**
 * Implémentation de Equatable pour Type.
 * Deux types sont équivalents si leur TypeInner est égal.
 */
fun Type.isEquivalentTo(other: Any): Boolean {
    if (other !is Type) return false
    return this.inner == other.inner
}

/**
 * Crée une UniqueArena vide.
 */
fun <T> uniqueArenaOf(): UniqueArena<T> where T : Equatable = UniqueArena()

/**
 * Crée une UniqueArena avec des éléments initiaux.
 */
fun <T> uniqueArenaOf(vararg elements: T): UniqueArena<T> where T : Equatable {
    val arena = UniqueArena<T>()
    elements.forEach { arena.append(it) }
    return arena
}

/**
 * Crée une UniqueArena à partir d'une collection.
 */
fun <T> uniqueArenaOf(collection: Iterable<T>): UniqueArena<T> where T : Equatable {
    val arena = UniqueArena<T>()
    collection.forEach { arena.append(it) }
    return arena
}
```

---

### 4. Range.kt (Pour Emit)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/arena/Range.kt`

```kotlin
package io.ygdrasil.wgsl.arena

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Range représente une plage d'expressions dans une Arena<Expression>.
 * Utilisé par Statement.Emit pour émettre plusieurs expressions à la fois.
 */
@Serializable(RangeSerializer::class)
@JvmInline
value class Range<T>(val indices: IntRange) {
    
    /**
     * Début de la plage (inclus).
     */
    val start: Int get() = indices.start
    
    /**
     * Fin de la plage (inclus).
     */
    val endInclusive: Int get() = indices.endInclusive
    
    /**
     * Nombre d'éléments dans la plage.
     */
    val count: Int get() = indices.count()
    
    /**
     * Vérifie si la plage est vide.
     */
    fun isEmpty(): Boolean = indices.isEmpty()
    
    /**
     * Vérifie si la plage contient un index.
     */
    fun contains(index: Int): Boolean = index in indices
    
    /**
     * Retourne la plage comme une liste d'indices.
     */
    fun toList(): List<Int> = indices.toList()
    
    companion object {
        /**
         * Crée une Range à partir d'un début et une fin.
         */
        fun from(start: Int, end: Int): Range<T> = Range(start..end)
        
        /**
         * Range vide.
         */
        val EMPTY: Range<T> = Range(0..-1)
    }
}

/**
 * Sérialiseur pour Range.
 */
object RangeSerializer : KSerializer<Range<*>> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("Range", PrimitiveKind.INT)
    
    override fun serialize(encoder: Encoder, value: Range<*>) {
        encoder.encodeInt(value.start)
        encoder.encodeInt(value.endInclusive)
    }
    
    override fun deserialize(decoder: Decoder): Range<*> {
        val start = decoder.decodeInt()
        val end = decoder.decodeInt()
        return Range(start..end)
    }
}

/**
 * Crée une Range à partir d'un seul élément.
 */
fun <T> rangeOf(element: Handle<T>): Range<T> = Range(element.index..element.index)

/**
 * Crée une Range à partir de plusieurs Handles.
 */
fun <T> rangeOf(elements: List<Handle<T>>): Range<T> {
    if (elements.isEmpty()) return Range.EMPTY
    val start = elements.first().index
    val end = elements.last().index
    return Range(start..end)
}
```

---

## 🧪 TESTS UNITAIRES

### ArenaTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/arena/ArenaTest.kt`

```kotlin
package io.ygdrasil.wgsl.arena

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ArenaTest {
    
    @Test
    fun `test empty arena`() {
        val arena = Arena<String>()
        
        assertThat(arena.size).isEqualTo(0)
        assertThat(arena.isEmpty()).isTrue()
    }
    
    @Test
    fun `test append single element`() {
        val arena = Arena<String>()
        val handle = arena.append("test")
        
        assertThat(handle.index).isEqualTo(0)
        assertThat(handle.isValid()).isTrue()
        assertThat(arena.size).isEqualTo(1)
        assertThat(arena[handle]).isEqualTo("test")
    }
    
    @Test
    fun `test append multiple elements`() {
        val arena = Arena<String>()
        val handle1 = arena.append("first")
        val handle2 = arena.append("second")
        val handle3 = arena.append("third")
        
        assertThat(handle1.index).isEqualTo(0)
        assertThat(handle2.index).isEqualTo(1)
        assertThat(handle3.index).isEqualTo(2)
        
        assertThat(arena[handle1]).isEqualTo("first")
        assertThat(arena[handle2]).isEqualTo("second")
        assertThat(arena[handle3]).isEqualTo("third")
    }
    
    @Test
    fun `test appendAll`() {
        val arena = Arena<String>()
        val handles = arena.appendAll(listOf("a", "b", "c"))
        
        assertThat(handles.size).isEqualTo(3)
        assertThat(handles[0].index).isEqualTo(0)
        assertThat(handles[1].index).isEqualTo(1)
        assertThat(handles[2].index).isEqualTo(2)
        
        assertThat(arena[handles[0]]).isEqualTo("a")
        assertThat(arena[handles[1]]).isEqualTo("b")
        assertThat(arena[handles[2]]).isEqualTo("c")
    }
    
    @Test
    fun `test getOrNull with invalid handle`() {
        val arena = Arena<String>()
        arena.append("test")
        
        val invalidHandle = Handle<String>(100)
        assertThat(arena.getOrNull(invalidHandle)).isNull()
    }
    
    @Test
    fun `test get with invalid handle throws`() {
        val arena = Arena<String>()
        arena.append("test")
        
        val invalidHandle = Handle<String>(100)
        assertThatThrownBy { arena[invalidHandle] }
            .isInstanceOf(IndexOutOfBoundsException::class)
    }
    
    @Test
    fun `test forEachWithHandle`() {
        val arena = Arena<String>()
        val h1 = arena.append("a")
        val h2 = arena.append("b")
        
        val result = mutableListOf<Pair<Int, String>>()
        arena.forEachWithHandle { handle, value ->
            result.add(handle.index to value)
        }
        
        assertThat(result).containsExactly(
            0 to "a",
            1 to "b"
        )
    }
    
    @Test
    fun `test findHandle`() {
        val arena = Arena<String>()
        val h1 = arena.append("a")
        val h2 = arena.append("b")
        
        val foundHandle = arena.findHandle { it == "b" }
        assertThat(foundHandle?.index).isEqualTo(1)
        assertThat(foundHandle).isEqualTo(h2)
    }
    
    @Test
    fun `test findEntry`() {
        val arena = Arena<String>()
        val h1 = arena.append("a")
        val h2 = arena.append("b")
        
        val entry = arena.findEntry { it == "b" }
        assertThat(entry?.first?.index).isEqualTo(1)
        assertThat(entry?.second).isEqualTo("b")
    }
    
    @Test
    fun `test clear`() {
        val arena = Arena<String>()
        arena.append("a")
        arena.append("b")
        
        arena.clear()
        
        assertThat(arena.size).isEqualTo(0)
        assertThat(arena.isEmpty()).isTrue()
    }
    
    @Test
    fun `test plusAssign operator`() {
        val arena = Arena<String>()
        
        arena += "test"
        
        assertThat(arena.size).isEqualTo(1)
        assertThat(arena[Handle(0)]).isEqualTo("test")
    }
    
    @Test
    fun `test set operator`() {
        val arena = Arena<String>()
        val handle = arena.append("original")
        
        arena[handle] = "modified"
        
        assertThat(arena[handle]).isEqualTo("modified")
    }
}
```

---

### UniqueArenaTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/arena/UniqueArenaTest.kt`

```kotlin
package io.ygdrasil.wgsl.arena

import io.ygdrasil.wgsl.ir.Type
import io.ygdrasil.wgsl.ir.TypeInner
import io.ygdrasil.wgsl.ir.ScalarKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// Implémentation de Equatable pour Type (pour les tests)
fun Type.isEquivalentTo(other: Any): Boolean {
    if (other !is Type) return false
    return this.inner == other.inner
}

class UniqueArenaTest {
    
    @Test
    fun `test empty unique arena`() {
        val arena = UniqueArena<Type>()
        
        assertThat(arena.size).isEqualTo(0)
        assertThat(arena.isEmpty()).isTrue()
    }
    
    @Test
    fun `test append unique elements`() {
        val arena = UniqueArena<Type>()
        
        val type1 = Type(TypeInner.Scalar(ScalarKind.F32, 4u))
        val type2 = Type(TypeInner.Scalar(ScalarKind.I32, 4u))
        
        val h1 = arena.append(type1)
        val h2 = arena.append(type2)
        
        assertThat(h1.index).isEqualTo(0)
        assertThat(h2.index).isEqualTo(1)
        assertThat(arena.size).isEqualTo(2)
    }
    
    @Test
    fun `test append duplicate element returns same handle`() {
        val arena = UniqueArena<Type>()
        
        val type1 = Type(TypeInner.Scalar(ScalarKind.F32, 4u))
        val type2 = Type(TypeInner.Scalar(ScalarKind.F32, 4u))  // Même type
        
        val h1 = arena.append(type1)
        val h2 = arena.append(type2)
        
        // Les deux Handles doivent pointer vers le même élément
        assertThat(h1.index).isEqualTo(h2.index)
        assertThat(arena.size).isEqualTo(1)  // Un seul élément dans l'arena
    }
    
    @Test
    fun `test contains`() {
        val arena = UniqueArena<Type>()
        val type = Type(TypeInner.Scalar(ScalarKind.F32, 4u))
        
        val handle = arena.append(type)
        
        assertThat(arena.contains(type)).isTrue()
        assertThat(arena.contains(Type(TypeInner.Scalar(ScalarKind.I32, 4u)))).isFalse()
    }
    
    @Test
    fun `test findHandle`() {
        val arena = UniqueArena<Type>()
        val type = Type(TypeInner.Scalar(ScalarKind.F32, 4u))
        
        val handle = arena.append(type)
        val foundHandle = arena.findHandle(type)
        
        assertThat(foundHandle).isEqualTo(handle)
    }
    
    @Test
    fun `test appendAll`() {
        val arena = UniqueArena<Type>()
        
        val types = listOf(
            Type(TypeInner.Scalar(ScalarKind.F32, 4u)),
            Type(TypeInner.Scalar(ScalarKind.F32, 4u)),  // Duplicate
            Type(TypeInner.Scalar(ScalarKind.I32, 4u))
        )
        
        val handles = arena.appendAll(types)
        
        // 3 Handles retournés, mais seulement 2 éléments uniques dans l'arena
        assertThat(handles.size).isEqualTo(3)
        assertThat(arena.size).isEqualTo(2)
        
        // Les deux premiers Handles pointent vers le même élément
        assertThat(handles[0].index).isEqualTo(handles[1].index)
    }
    
    @Test
    fun `test clear`() {
        val arena = UniqueArena<Type>()
        arena.append(Type(TypeInner.Scalar(ScalarKind.F32, 4u)))
        
        arena.clear()
        
        assertThat(arena.size).isEqualTo(0)
        assertThat(arena.isEmpty()).isTrue()
    }
}
```

---

### HandleTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/arena/HandleTest.kt`

```kotlin
package io.ygdrasil.wgsl.arena

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HandleTest {
    
    @Test
    fun `test valid handle`() {
        val handle = Handle<String>(0)
        
        assertThat(handle.isValid()).isTrue()
        assertThat(handle.isInvalid()).isFalse()
    }
    
    @Test
    fun `test invalid handle`() {
        val handle = Handle<String>(-1)
        
        assertThat(handle.isValid()).isFalse()
        assertThat(handle.isInvalid()).isTrue()
    }
    
    @Test
    fun `test Handle.INVALID`() {
        assertThat(Handle.INVALID.isValid()).isFalse()
        assertThat(Handle.INVALID.index).isEqualTo(-1)
    }
    
    @Test
    fun `test equalsIgnoreType`() {
        val handle1 = Handle<String>(0)
        val handle2 = Handle<Int>(0)
        
        assertThat(handle1.equalsIgnoreType<Any>(handle2)).isTrue()
    }
    
    @Test
    fun `test toUntyped`() {
        val handle = Handle<String>(0)
        val untyped: Handle<Any> = handle.toUntyped()
        
        assertThat(untyped.index).isEqualTo(0)
    }
    
    @Test
    fun `test toString`() {
        val handle = Handle<String>(42)
        
        assertThat(handle.toString()).isEqualTo("Handle(42)")
    }
}
```

---

### RangeTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/arena/RangeTest.kt`

```kotlin
package io.ygdrasil.wgsl.arena

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RangeTest {
    
    @Test
    fun `test range from start and end`() {
        val range = Range.from<String>(0, 5)
        
        assertThat(range.start).isEqualTo(0)
        assertThat(range.endInclusive).isEqualTo(5)
        assertThat(range.count).isEqualTo(6)
    }
    
    @Test
    fun `test empty range`() {
        val range = Range.EMPTY
        
        assertThat(range.isEmpty()).isTrue()
        assertThat(range.count).isEqualTo(0)
    }
    
    @Test
    fun `test range contains`() {
        val range = Range.from<String>(0, 5)
        
        assertThat(range.contains(0)).isTrue()
        assertThat(range.contains(3)).isTrue()
        assertThat(range.contains(5)).isTrue()
        assertThat(range.contains(6)).isFalse()
        assertThat(range.contains(-1)).isFalse()
    }
    
    @Test
    fun `test range toList`() {
        val range = Range.from<String>(2, 4)
        
        assertThat(range.toList()).containsExactly(2, 3, 4)
    }
    
    @Test
    fun `test rangeOf single element`() {
        val handle = Handle<String>(5)
        val range = rangeOf(handle)
        
        assertThat(range.start).isEqualTo(5)
        assertThat(range.endInclusive).isEqualTo(5)
        assertThat(range.count).isEqualTo(1)
    }
    
    @Test
    fun `test rangeOf multiple elements`() {
        val handles = listOf(
            Handle<String>(0),
            Handle<String>(1),
            Handle<String>(2),
            Handle<String>(3)
        )
        val range = rangeOf(handles)
        
        assertThat(range.start).isEqualTo(0)
        assertThat(range.endInclusive).isEqualTo(3)
        assertThat(range.count).isEqualTo(4)
    }
    
    @Test
    fun `test rangeOf empty list`() {
        val range = rangeOf<String>(emptyList())
        
        assertThat(range.isEmpty()).isTrue()
    }
}
```

---

## ✅ CHECKLIST PHASE 1.1

### Système Arena/Handle
- [ ] Implémenter `Handle<T>` (value class inline, @Serializable)
- [ ] Implémenter `Arena<T>` (MutableList wrapper, toutes les méthodes utilitaires)
- [ ] Implémenter `UniqueArena<T>` (avec déduplication, interface Equatable)
- [ ] Implémenter `Range<T>` (pour Emit)
- [ ] Implémenter les fonctions helpers (`arenaOf`, `uniqueArenaOf`, `rangeOf`, etc.)

### Tests
- [ ] ArenaTest (`test empty arena`, `test append single element`, etc.)
- [ ] UniqueArenaTest (`test empty unique arena`, `test append duplicate element`, etc.)
- [ ] HandleTest (`test valid handle`, `test equalsIgnoreType`, etc.)
- [ ] RangeTest (`test range from start and end`, `test empty range`, etc.)

### Sérialisation
- [ ] Vérifier que Handle<T> est sérialisable
- [ ] Vérifier que Range<T> est sérialisable
- [ ] Tester la sérialisation/désérialisation

### Documentation
- [ ] KDoc pour toutes les classes publiques
- [ ] KDoc pour toutes les méthodes publiques
- [ ] Exemples d'utilisation dans la documentation

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Implémenter Handle<T> | 1 jour | Aucune | [ ] |
| Implémenter Arena<T> | 2 jours | Handle<T> | [ ] |
| Implémenter UniqueArena<T> | 2 jours | Arena<T> | [ ] |
| Implémenter Range<T> | 1 jour | Handle<T> | [ ] |
| Écrire tests ArenaTest | 1 jour | Arena<T> | [ ] |
| Écrire tests UniqueArenaTest | 1 jour | UniqueArena<T> | [ ] |
| Écrire tests HandleTest | 0.5 jour | Handle<T> | [ ] |
| Écrire tests RangeTest | 0.5 jour | Range<T> | [ ] |
| Ajouter documentation | 1 jour | Tout | [ ] |
| Validation manuelle | 1 jour | Tout | [ ] |

**Total estimé** : **1-2 semaines** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichiers Kotlin** :
   - `Handle.kt`
   - `Arena.kt`
   - `UniqueArena.kt`
   - `Range.kt`

2. **Tests unitaires** :
   - `ArenaTest.kt`
   - `UniqueArenaTest.kt`
   - `HandleTest.kt`
   - `RangeTest.kt`

3. **Couverture de test** : > 95%

4. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Fichier Rust principal** : `/Users/chaos/RustroverProjects/wgpu/naga/src/arena/mod.rs`
- **Utilisation dans IR** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (rechercher `Arena<` et `Handle<`)

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Implémenter `Handle<T>`
2. [ ] Implémenter `Arena<T>`
3. [ ] Implémenter `UniqueArena<T>`
4. [ ] Implémenter `Range<T>`
5. [ ] Écrire tous les tests unitaires
6. [ ] Valider avec des tests manuels
7. [ ] Passer à `02_primitive-types.md` (types de base)

**Fichier suivant** : `02_primitive-types.md`
