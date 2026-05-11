package io.ygdrasil.wgsl.ir

import io.ygdrasil.wgsl.arena.Handle
import kotlinx.serialization.Serializable

/**
 * A constant value in the module.
 */
@Serializable
data class Constant(
    /**
     * The type of the constant.
     */
    val type: Handle<Type>,
    
    /**
     * The specialization constants used (if any).
     */
    val specialization: Map<Int, ScalarValue> = emptyMap(),
    
    /**
     * The inner value of the constant.
     */
    val inner: ConstantInner,
) {
    override fun toString(): String = inner.toString()
}

/**
 * The inner value of a constant.
 */
@Serializable
sealed class ConstantInner {
    /**
     * A scalar constant value.
     */
    data class Scalar(val value: ScalarValue) : ConstantInner()
    
    /**
     * A vector constant value.
     */
    data class Vector(val components: List<ScalarValue>) : ConstantInner()
    
    /**
     * A matrix constant value.
     */
    data class Matrix(val columns: List<List<ScalarValue>>) : ConstantInner()
    
    /**
     * A zero value of the given type.
     */
    data class Zero(val type: Handle<Type>) : ConstantInner()
    
    /**
     * A composite of other constants.
     */
    data class Composite(val type: Handle<Type>, val components: List<Handle<Constant>>) : ConstantInner()
    
    /**
     * A constant expression that needs to be evaluated.
     */
    data class Expression(val expr: Handle<Expression>) : ConstantInner()
    
    override fun toString(): String = when (this) {
        is Scalar -> value.toString()
        is Vector -> "vec(${components.joinToString()})"
        is Matrix -> "mat(${columns.size}x${columns.firstOrNull()?.size})"
        is Zero -> "zero(${type.index})"
        is Composite -> "composite(${components.size})"
        is Expression -> "expr(${expr.index})"
    }
}
