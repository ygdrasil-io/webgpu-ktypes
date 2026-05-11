package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * The scalar types that can be used in WGSL and the Naga IR.
 */
@Serializable
enum class ScalarKind {
    /**
     * Boolean type (true/false).
     */
    Bool,
    
    /**
     * Signed 8-bit integer.
     */
    Sint,
    
    /**
     * Unsigned 8-bit integer.
     */
    Uint,
    
    /**
     * Signed 16-bit integer.
     */
    S16,
    
    /**
     * Unsigned 16-bit integer.
     */
    U16,
    
    /**
     * Signed 32-bit integer.
     */
    S32,
    
    /**
     * Unsigned 32-bit integer.
     */
    U32,
    
    /**
     * Signed 64-bit integer.
     */
    S64,
    
    /**
     * Unsigned 64-bit integer.
     */
    U64,
    
    /**
     * 16-bit floating point.
     */
    F16,
    
    /**
     * 32-bit floating point.
     */
    F32,
    
    /**
     * 64-bit floating point.
     */
    F64,
    
    /**
     * Abstract integer type (no fixed bit width).
     */
    AbstractInt,
    
    /**
     * Abstract float type (no fixed bit width).
     */
    AbstractFloat,
}

/**
 * Returns the width in bytes of this scalar kind.
 * Returns null for abstract types.
 */
val ScalarKind.width: Int?
    get() = when (this) {
        ScalarKind.Bool -> 1
        ScalarKind.Sint -> 1
        ScalarKind.Uint -> 1
        ScalarKind.S16 -> 2
        ScalarKind.U16 -> 2
        ScalarKind.S32 -> 4
        ScalarKind.U32 -> 4
        ScalarKind.S64 -> 8
        ScalarKind.U64 -> 8
        ScalarKind.F16 -> 2
        ScalarKind.F32 -> 4
        ScalarKind.F64 -> 8
        ScalarKind.AbstractInt -> null
        ScalarKind.AbstractFloat -> null
    }

/**
 * Returns true if this scalar kind represents an integer type.
 */
val ScalarKind.isInteger: Boolean
    get() = when (this) {
        ScalarKind.Sint, ScalarKind.Uint, ScalarKind.S16, ScalarKind.U16,
        ScalarKind.S32, ScalarKind.U32, ScalarKind.S64, ScalarKind.U64,
        ScalarKind.AbstractInt -> true
        else -> false
    }

/**
 * Returns true if this scalar kind represents a signed integer type.
 */
val ScalarKind.isSigned: Boolean
    get() = when (this) {
        ScalarKind.Sint, ScalarKind.S16, ScalarKind.S32, ScalarKind.S64,
        ScalarKind.AbstractInt -> true
        else -> false
    }

/**
 * Returns true if this scalar kind represents a floating point type.
 */
val ScalarKind.isFloat: Boolean
    get() = when (this) {
        ScalarKind.F16, ScalarKind.F32, ScalarKind.F64, ScalarKind.AbstractFloat -> true
        else -> false
    }

/**
 * Returns true if this scalar kind represents a numeric type (integer or float).
 */
val ScalarKind.isNumeric: Boolean
    get() = isInteger || isFloat
