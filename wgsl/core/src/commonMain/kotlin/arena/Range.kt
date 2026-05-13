package io.ygdrasil.wgsl.arena

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Range represents a range of expressions in an Arena<Expression>.
 * Used by Statement.Emit to emit multiple expressions at once.
 */
@Serializable(RangeSerializer::class)
@JvmInline
value class Range<T>(val indices: IntRange) {
    
    /**
     * Start of the range (inclusive).
     */
    val start: Int get() = indices.start
    
    /**
     * End of the range (inclusive).
     */
    val endInclusive: Int get() = indices.endInclusive
    
    /**
     * Number of elements in the range.
     */
    val count: Int get() = indices.count()
    
    /**
     * Checks if the range is empty.
     */
    fun isEmpty(): Boolean = indices.isEmpty()
    
    /**
     * Checks if the range contains an index.
     */
    fun contains(index: Int): Boolean = index in indices
    
    /**
     * Returns the range as a list of indices.
     */
    fun toList(): List<Int> = indices.toList()
    
    companion object {
        /**
         * Creates a Range from a start and end.
         */
        fun <U> from(start: Int, end: Int): Range<U> = Range(start..end)
        
        /**
         * Empty range.
         */
        fun <U> empty(): Range<U> = Range(0..-1)
    }
}

/**
 * Serializer for Range.
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
        return Range.from<Any>(start, end)
    }
}

/**
 * Creates a Range from a single element.
 */
fun <T> rangeOf(element: Handle<T>): Range<T> = Range(element.index..element.index)

/**
 * Creates a Range from multiple Handles.
 */
fun <T> rangeOf(elements: List<Handle<T>>): Range<T> {
    if (elements.isEmpty()) return Range.from<T>(0, -1)
    val start = elements.first().index
    val end = elements.last().index
    return Range.from<T>(start, end)
}
