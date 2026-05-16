package io.ygdrasil.wgsl.arena

import kotlinx.serialization.Serializable

/**
 * Arena that guarantees uniqueness of elements.
 *
 * Unlike Arena, UniqueArena checks if an element already exists before adding it.
 * If the element exists, the existing Handle is returned.
 *
 * Used primarily for types (Type), where multiple identical definitions
 * must share the same Handle.
 *
 * @param T The type of elements stored (must implement Equatable for comparison)
 */
@Serializable
class UniqueArena<T> where T : Equatable {

    internal val data: MutableList<T> = mutableListOf()
    internal val indexMap: MutableMap<T, Int> = mutableMapOf()

    /**
     * Number of unique elements in the Arena.
     */
    val size: Int get() = data.size

    /**
     * Adds an element to the Arena. If the element already exists, returns the existing Handle.
     *
     * @param value The element to add
     * @return Handle<T> pointing to the element (existing or new)
     */
    fun append(value: T): Handle<T> {
        return indexMap.getOrPut(value) {
            val index = data.size
            data.add(value)
            index
        }.let { Handle.fromIndex<T>(it) }
    }

    /**
     * Adds multiple elements to the Arena.
     */
    fun appendAll(values: Iterable<T>): List<Handle<T>> {
        return values.map { append(it) }
    }

    /**
     * Retrieves an element by its Handle.
     */
    operator fun get(handle: Handle<T>): T {
        require(handle.isValid()) { "Invalid handle: ${handle.index}" }
        return data[handle.index]
    }

    /**
     * Retrieves an element by its Handle, or null if invalid.
     */
    fun getOrNull(handle: Handle<T>): T? {
        return if (handle.isValid() && handle.index < data.size) {
            data[handle.index]
        } else {
            null
        }
    }

    /**
     * Checks if the Arena contains a specific element.
     */
    fun contains(value: T): Boolean = value in indexMap

    /**
     * Finds the Handle of an existing element.
     */
    fun findHandle(value: T): Handle<T>? {
        return indexMap[value]?.let { Handle.fromIndex<T>(it) }
    }

    /**
     * Checks if the Arena is empty.
     */
    fun isEmpty(): Boolean = data.isEmpty()

    /**
     * Applies an action to each element with its Handle.
     */
    fun forEachWithHandle(action: (Handle<T>, T) -> Unit) {
        data.forEachIndexed { index, value ->
            action(Handle.fromIndex<T>(index), value)
        }
    }

    /**
     * Returns a list of all elements.
     */
    fun toList(): List<T> = data.toList()

    /**
     * Clears the Arena.
     */
    fun clear() {
        data.clear()
        indexMap.clear()
    }

    /**
     * Creates a copy of the Arena.
     */
    fun copy(): UniqueArena<T> {
        val copy = UniqueArena<T>()
        copy.data.addAll(data)
        copy.indexMap.putAll(indexMap)
        return copy
    }
}

/**
 * Interface for types that can be compared for equivalence.
 * Used by UniqueArena to detect duplicates.
 */
interface Equatable {
    /**
     * Checks if this object is equivalent to another.
     *
     * Unlike equals(), isEquivalentTo() is used to compare
     * objects that are semantically equivalent but not necessarily
     * the same instance (ex: two Type with the same structure).
     */
    fun isEquivalentTo(other: Any): Boolean
}

/**
 * Creates an empty UniqueArena.
 */
fun <T> uniqueArenaOf(): UniqueArena<T> where T : Equatable = UniqueArena()

/**
 * Creates a UniqueArena with initial elements.
 */
fun <T> uniqueArenaOf(vararg elements: T): UniqueArena<T> where T : Equatable {
    val arena = UniqueArena<T>()
    elements.forEach { arena.append(it) }
    return arena
}

/**
 * Creates a UniqueArena from a collection.
 */
fun <T> uniqueArenaOf(collection: Iterable<T>): UniqueArena<T> where T : Equatable {
    val arena = UniqueArena<T>()
    collection.forEach { arena.append(it) }
    return arena
}
