package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * The size of a vector type.
 */
@Serializable
enum class VectorSize {
    /**
     * 2-element vector.
     */
    Bi,

    /**
     * 3-element vector.
     */
    Tri,

    /**
     * 4-element vector.
     */
    Quad;

    companion object {
        fun fromInt(value: Int): VectorSize = when (value) {
            2 -> Bi
            3 -> Tri
            4 -> Quad
            else -> throw IllegalArgumentException("Invalid vector size: $value")
        }
    }
}

/**
 * Returns the number of components in this vector size.
 */
val VectorSize.value: Int
    get() = when (this) {
        VectorSize.Bi -> 2
        VectorSize.Tri -> 3
        VectorSize.Quad -> 4
    }
