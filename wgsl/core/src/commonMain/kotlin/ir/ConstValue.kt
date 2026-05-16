package io.ygdrasil.wgsl.ir

import io.ygdrasil.wgsl.arena.Handle
import kotlinx.serialization.Serializable

/**
 * Evaluated constant value.
 * Represents the result of evaluating a constant expression.
 */
@Serializable
sealed class ConstValue {

    /**
     * Type of the constant value.
     */
    abstract val type: Handle<Type>

    // ===== Scalar values =====

    @Serializable
    data class Scalar(
        val value: ScalarValue,
        override val type: Handle<Type>
    ) : ConstValue()

    @Serializable
    data class Vector(
        val components: List<ScalarValue>,
        override val type: Handle<Type>
    ) : ConstValue()

    @Serializable
    data class Matrix(
        val columns: List<List<ScalarValue>>,
        override val type: Handle<Type>
    ) : ConstValue()

    @Serializable
    data class Array(
        val elements: List<ConstValue>,
        override val type: Handle<Type>
    ) : ConstValue()

    @Serializable
    data class Struct(
        val members: List<ConstValue>,
        override val type: Handle<Type>
    ) : ConstValue()

    // ===== Special value for "not evaluatable" =====

    @Serializable
    data object NotConst : ConstValue() {
        override val type: Handle<Type> get() = Handle.create(-1)
    }
}

/**
 * Helper to create ConstValue.NotConst.
 */
fun notConst(): ConstValue = ConstValue.NotConst

/**
 * Checks if a ConstValue is not NotConst.
 */
fun ConstValue.isConst(): Boolean = this !is ConstValue.NotConst

/**
 * Checks if a ConstValue is not NotConst (nullable version).
 */
fun ConstValue?.isConstOrNull(): Boolean = this?.isConst() ?: false
