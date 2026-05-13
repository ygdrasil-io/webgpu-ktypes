package io.ygdrasil.wgsl.ir

import io.ygdrasil.wgsl.arena.Handle
import kotlinx.serialization.Serializable

/**
 * An expression in the Naga IR.
 * 
 * Expressions represent computations that produce values.
 */
@Serializable
data class Expression(
    /**
     * The kind of expression.
     */
    val kind: ExpressionKind,
) {
    override fun toString(): String = kind.toString()
}

/**
 * The kind of an expression.
 */
@Serializable
sealed class ExpressionKind {
    
    // Literal values
    data class Literal(val value: LiteralValue) : ExpressionKind()
    
    // Variable access
    data class Variable(val variable: Handle<GlobalVariable>) : ExpressionKind()
    data class LocalVariable(val variable: Handle<LocalVariable>) : ExpressionKind()
    data class FunctionArgument(val index: Int) : ExpressionKind()
    
    // Type constructors
    data class TypeConstructor(val type: Handle<Type>, val arguments: List<Handle<Expression>>) : ExpressionKind()
    
    // Unary operations
    data class Unary(val operator: UnaryOperator, val expr: Handle<Expression>) : ExpressionKind()
    
    // Binary operations
    data class Binary(val operator: BinaryOperator, val left: Handle<Expression>, val right: Handle<Expression>) : ExpressionKind()
    
    // Ternary operation
    data class Select(
        val condition: Handle<Expression>,
        val accept: Handle<Expression>,
        val reject: Handle<Expression>
    ) : ExpressionKind()
    
    // Function calls
    data class Call(
        val function: Handle<Function>,
        val arguments: List<Handle<Expression>>
    ) : ExpressionKind()
    
    // Built-in function calls
    data class BuiltinCall(
        val function: BuiltinFunction,
        val arguments: List<Handle<Expression>>
    ) : ExpressionKind()
    
    // Constant access
    data class Constant(val constant: Handle<Constant>) : ExpressionKind()
    
    // Load from pointer
    data class Load(val pointer: Handle<Expression>) : ExpressionKind()
    
    // Store to pointer
    data class Store(val pointer: Handle<Expression>, val value: Handle<Expression>) : ExpressionKind()
    
    // Access index
    data class AccessIndex(
        val expr: Handle<Expression>,
        val index: Int
    ) : ExpressionKind()
    
    // Access member
    data class Access(
        val expr: Handle<Expression>,
        val index: Int
    ) : ExpressionKind()
    
    // Texture sampling
    data class Sample(
        val texture: Handle<Expression>,
        val sampler: Handle<Expression>?,
        val coordinate: Handle<Expression>,
        val level: SampleLevel? = null,
        val depthRef: Handle<Expression>? = null
    ) : ExpressionKind()
    
    // Texture query
    data class TextureQuery(val texture: Handle<Expression>, val query: TextureQueryKind) : ExpressionKind()
    
    // Array length
    data class ArrayLength(val expr: Handle<Expression>) : ExpressionKind()
    
    // Cast operations
    data class As(val expr: Handle<Expression>, val target: Handle<Type>) : ExpressionKind()
    data class Bitcast(val expr: Handle<Expression>) : ExpressionKind()
    
    // Relational operations
    data class Relational(val fun_: RelationalFunction, val arguments: List<Handle<Expression>>) : ExpressionKind()
    
    // Atomic operations
    data class Atomic(
        val pointer: Handle<Expression>,
        val fun_: AtomicFunction,
        val arguments: List<Handle<Expression>>
    ) : ExpressionKind()
    
    // Value pointer
    data class ValuePointer(val base: Handle<Expression>) : ExpressionKind()
    
    // Ray query
    data class RayQuery(val query: RayQueryKind, val arguments: List<Handle<Expression>>) : ExpressionKind()
}

/**
 * Sample level for texture sampling.
 */
@Serializable
sealed class SampleLevel : io.ygdrasil.wgsl.arena.Equatable {
    class Zero : SampleLevel()
    data class MIPMAP(val level: Handle<Expression>) : SampleLevel()
    data class AUTOMATIC(val level: Handle<Expression>) : SampleLevel()
    
    override fun isEquivalentTo(other: Any): Boolean {
        if (this === other) return true
        if (other !is SampleLevel) return false
        return when (this) {
            is Zero -> other is Zero
            is MIPMAP -> other is MIPMAP && level == other.level
            is AUTOMATIC -> other is AUTOMATIC && level == other.level
        }
    }
}

/**
 * Texture query kinds.
 */
@Serializable
enum class TextureQueryKind {
    NumSamples,
    NumLevels,
    NumLayers,
    Size,
    SizeLevel,
}

/**
 * Relational functions.
 */
@Serializable
enum class RelationalFunction {
    All,
    Any,
    IsNan,
    IsInf,
    IsFinite,
    IsNormal,
    SignBit,
}

/**
 * Atomic functions.
 */
@Serializable
enum class AtomicFunction {
    Add,
    Subtract,
    And,
    Or,
    Xor,
    Min,
    Max,
    Exchange,
    CompSwap,
}

/**
 * Ray query kinds.
 */
@Serializable
enum class RayQueryKind {
    // ... ray query kinds
}

/**
 * Literal value for expressions.
 */
@Serializable
sealed class LiteralValue {
    data class Scalar(val value: ScalarValue) : LiteralValue()
    data class Vector(val components: List<ScalarValue>) : LiteralValue()
    data class Matrix(val columns: List<List<ScalarValue>>) : LiteralValue()
    data class Bool(val value: Boolean) : LiteralValue()
}

/**
 * Scalar value for literals.
 */
@Serializable
sealed class ScalarValue {
    data class Sint(val value: Int) : ScalarValue()
    data class Uint(val value: Int) : ScalarValue()
    data class S64(val value: Long) : ScalarValue()
    data class U64(val value: Long) : ScalarValue()
    data class F32(val value: Float) : ScalarValue()
    data class F64(val value: Double) : ScalarValue()
    data class Bool(val value: Boolean) : ScalarValue()
}
