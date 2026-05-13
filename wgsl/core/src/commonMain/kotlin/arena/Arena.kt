package io.ygdrasil.wgsl.arena

import kotlinx.serialization.Serializable

/**
 * Arena for storing elements efficiently.
 *
 * Arenas are used to store collections of elements that:
 * - Are accessed by index (Handle) rather than by reference
 * - Have a lifetime limited to the scope of their Arena
 * - Should be stored contiguously for performance
 *
 * @param T The type of elements stored in the Arena
 */
@Serializable
class Arena<T> : Iterable<T>, Collection<T>, MutableCollection<T> {
    
    internal val data: MutableList<T> = mutableListOf()
    
    /**
     * Number of elements in the Arena.
     */
    override val size: Int get() = data.size
    
    /**
     * Adds an element to the Arena and returns its Handle.
     *
     * @param value The element to add
     * @return Handle<T> pointing to the added element
     */
    fun append(value: T): Handle<T> {
        val index = data.size
        data.add(value)
        return Handle.fromIndex<T>(index)
    }
    
    /**
     * Adds multiple elements to the Arena.
     *
     * @param values The elements to add
     * @return List of Handles pointing to the added elements
     */
    fun appendAll(values: Iterable<T>): List<Handle<T>> {
        val startIndex = data.size
        data.addAll(values)
        return (startIndex until data.size).map { Handle.fromIndex<T>(it) }
    }
    
    /**
     * Retrieves an element by its Handle.
     *
     * @param handle The Handle of the element
     * @return The corresponding element
     * @throws IndexOutOfBoundsException if the Handle is invalid
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
     * Retrieves an element by index.
     */
    operator fun get(index: Int): T = data[index]
    
    /**
     * Checks if the Arena is empty.
     */
    override fun isEmpty(): Boolean = data.isEmpty()
    
    /**
     * Checks if the Arena contains an element.
     */
    override fun contains(element: T): Boolean = data.contains(element)
    
    /**
     * Checks if the Arena contains all elements.
     */
    override fun containsAll(elements: Collection<T>): Boolean = data.containsAll(elements)
    
    /**
     * Returns an iterator over all elements.
     */
    override fun iterator(): MutableIterator<T> = data.iterator()
    
    /**
     * Applies an action to each element with its Handle.
     *
     * @param action Action to apply (Handle<T>, T) -> Unit
     */
    fun forEachWithHandle(action: (Handle<T>, T) -> Unit) {
        data.forEachIndexed { index, value ->
            action(Handle.fromIndex<T>(index), value)
        }
    }
    
    /**
     * Applies an action to each element with its index.
     */
    fun forEachIndexed(action: (Int, T) -> Unit) {
        data.forEachIndexed(action)
    }
    
    /**
     * Transforms each element.
     */
    fun <R> map(transform: (T) -> R): List<R> = data.map(transform)
    
    /**
     * Transforms each element with its Handle.
     */
    fun <R> mapWithHandle(transform: (Handle<T>, T) -> R): List<R> {
        return data.mapIndexed { index, value ->
            transform(Handle.fromIndex<T>(index), value)
        }
    }
    
    /**
     * Filters the elements.
     */
    fun filter(predicate: (T) -> Boolean): List<T> = data.filter(predicate)
    
    /**
     * Filters the elements with their Handle.
     */
    fun filterWithHandle(predicate: (Handle<T>, T) -> Boolean): List<T> {
        return data.filterIndexed { index, value ->
            predicate(Handle.fromIndex<T>(index), value)
        }
    }
    
    /**
     * Finds the first element matching the predicate.
     */
    fun find(predicate: (T) -> Boolean): T? = data.find(predicate)
    
    /**
     * Finds the Handle of the first element matching the predicate.
     */
    fun findHandle(predicate: (T) -> Boolean): Handle<T>? {
        data.forEachIndexed { index, value ->
            if (predicate(value)) return Handle.fromIndex<T>(index)
        }
        return null
    }
    
    /**
     * Finds the Handle and element matching the predicate.
     */
    fun findEntry(predicate: (T) -> Boolean): Pair<Handle<T>, T>? {
        data.forEachIndexed { index, value ->
            if (predicate(value)) return Handle.fromIndex<T>(index) to value
        }
        return null
    }
    
    /**
     * Checks if a predicate is true for all elements.
     */
    fun all(predicate: (T) -> Boolean): Boolean = data.all(predicate)
    
    /**
     * Checks if a predicate is true for at least one element.
     */
    fun any(predicate: (T) -> Boolean): Boolean = data.any(predicate)
    
    /**
     * Counts the number of elements matching the predicate.
     */
    fun count(predicate: (T) -> Boolean): Int = data.count(predicate)
    
    /**
     * Sorts the elements.
     */
    fun sortedWith(comparator: Comparator<in T>): List<T> = data.sortedWith(comparator)
    
    /**
     * Returns a sublist.
     */
    fun slice(indices: IntRange): List<T> = data.slice(indices)
    
    /**
     * Returns the last element.
     */
    fun last(): T = data.last()
    
    /**
     * Returns the last element or null if empty.
     */
    fun lastOrNull(): T? = data.lastOrNull()
    
    /**
     * Returns the first element.
     */
    fun first(): T = data.first()
    
    /**
     * Returns the first element or null if empty.
     */
    fun firstOrNull(): T? = data.firstOrNull()
    
    /**
     * Returns the element at the given index.
     */
    fun elementAt(index: Int): T = data.elementAt(index)
    
    /**
     * Returns the element at the given index or null.
     */
    fun elementAtOrNull(index: Int): T? = data.elementAtOrNull(index)
    
    /**
     * Returns the index of an element.
     */
    fun indexOf(element: T): Int = data.indexOf(element)
    
    /**
     * Returns the Handle of an element.
     */
    fun handleOf(element: T): Handle<T>? {
        val index = data.indexOf(element)
        return if (index >= 0) Handle.fromIndex<T>(index) else null
    }
    
    /**
     * Clears the Arena.
     */
    override fun clear() {
        data.clear()
    }
    
    /**
     * Creates a copy of the Arena.
     */
    fun copy(): Arena<T> {
        val copy = Arena<T>()
        copy.data.addAll(data)
        return copy
    }
    
    /**
     * Converts to list.
     */
    fun toList(): List<T> = data.toList()
    
    // ===== Collection Implementation =====
    
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
    
    // ===== Operators =====
    
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
 * Creates an empty Arena.
 */
fun <T> arenaOf(): Arena<T> = Arena()

/**
 * Creates an Arena with initial elements.
 */
fun <T> arenaOf(vararg elements: T): Arena<T> {
    val arena = Arena<T>()
    elements.forEach { arena.append(it) }
    return arena
}

/**
 * Creates an Arena from a collection.
 */
fun <T> arenaOf(collection: Iterable<T>): Arena<T> {
    val arena = Arena<T>()
    collection.forEach { arena.append(it) }
    return arena
}
