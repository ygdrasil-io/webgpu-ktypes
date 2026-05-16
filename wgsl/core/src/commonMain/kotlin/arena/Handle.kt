package io.ygdrasil.wgsl.arena

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

/**
 * Handle is a type-safe wrapper around an Int (index in an Arena).
 *
 * Uses @JvmInline to avoid object allocation at runtime.
 * This means that Handle<T> is stored as a simple Int, but with type safety at compile-time.
 *
 * @param T The type of the element referenced in the Arena
 * @property index The index in the Arena (0-based)
 */
@Serializable(HandleSerializer::class)
@JvmInline
value class Handle<T>(val index: Int) {

    /**
     * Invalid handle, used as default or sentinel value.
     */
    companion object {
        /** Invalid handle (index = -1) */
        val INVALID: Handle<Nothing> = Handle(-1)

        /**
         * Creates a Handle from an index.
         * Used internally by Arena.
         */
        internal fun <T> fromIndex(index: Int): Handle<T> = Handle(index)

        internal fun <U> create(index: Int): Handle<U> = Handle(index)
    }

    /**
     * Checks if this Handle is valid (index >= 0).
     */
    fun isValid(): Boolean = index >= 0

    /**
     * Checks if this Handle is invalid.
     */
    fun isInvalid(): Boolean = !isValid()

    /**
     * Compares two Handles ignoring their generic type.
     * Useful for checking if two Handles point to the same element.
     */
    inline fun <reified U> equalsIgnoreType(other: Handle<U>): Boolean {
        return this.index == other.index
    }

    /**
     * Converts this Handle to Handle<Any> for generic comparison.
     */
    fun toUntyped(): Handle<Any> = Handle(index)

    override fun toString(): String = "Handle(${index})"
}

/**
 * Custom serializer for Handle<T>.
 * Handles are serialized as simple integers.
 */
object HandleSerializer : KSerializer<Handle<*>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Handle", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Handle<*>) {
        encoder.encodeInt(value.index)
    }

    override fun deserialize(decoder: Decoder): Handle<*> {
        return Handle.create<Any>(decoder.decodeInt())
    }
}

/**
 * Extension to create an invalid Handle<Nothing>.
 */
fun invalidHandle(): Handle<Nothing> = Handle.INVALID

/**
 * Extension to check if a Handle is valid or null.
 */
fun <T> Handle<T>?.isValidOrNull(): Boolean = this?.isValid() ?: false
