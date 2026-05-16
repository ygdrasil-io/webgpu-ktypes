package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * Binary operators for expressions.
 */
@Serializable
enum class BinaryOperator {
    // Arithmetic
    Add,
    Subtract,
    Multiply,
    Divide,
    Modulo,

    // Bitwise
    BitAnd,
    BitOr,
    BitXor,
    ShiftLeft,
    ShiftRight,

    // Comparison
    Equal,
    NotEqual,
    Less,
    LessOrEqual,
    Greater,
    GreaterOrEqual,

    // Logical
    LogicalAnd,
    LogicalOr,
}

/**
 * Returns true if this is an arithmetic operator.
 */
val BinaryOperator.isArithmetic: Boolean
    get() = when (this) {
        BinaryOperator.Add, BinaryOperator.Subtract, BinaryOperator.Multiply,
        BinaryOperator.Divide, BinaryOperator.Modulo -> true

        else -> false
    }

/**
 * Returns true if this is a bitwise operator.
 */
val BinaryOperator.isBitwise: Boolean
    get() = when (this) {
        BinaryOperator.BitAnd, BinaryOperator.BitOr, BinaryOperator.BitXor,
        BinaryOperator.ShiftLeft, BinaryOperator.ShiftRight -> true

        else -> false
    }

/**
 * Returns true if this is a comparison operator.
 */
val BinaryOperator.isComparison: Boolean
    get() = when (this) {
        BinaryOperator.Equal, BinaryOperator.NotEqual,
        BinaryOperator.Less, BinaryOperator.LessOrEqual,
        BinaryOperator.Greater, BinaryOperator.GreaterOrEqual -> true

        else -> false
    }

/**
 * Returns true if this is a logical operator.
 */
val BinaryOperator.isLogical: Boolean
    get() = when (this) {
        BinaryOperator.LogicalAnd, BinaryOperator.LogicalOr -> true
        else -> false
    }
