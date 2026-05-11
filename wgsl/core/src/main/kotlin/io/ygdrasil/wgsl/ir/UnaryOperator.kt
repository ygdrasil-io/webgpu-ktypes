package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * Unary operators for expressions.
 */
@Serializable
enum class UnaryOperator {
    Negate,
    Not,
    BitNot,
}
