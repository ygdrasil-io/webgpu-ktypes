package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.Expression
import io.ygdrasil.wgsl.ir.Statement
import io.ygdrasil.wgsl.ir.Type

/**
 * Erreurs générées par les backends.
 */
sealed class BackendError {
    abstract val message: String

    data class UnsupportedFeature(val feature: String) : BackendError() {
        override val message: String get() = "Feature not supported: $feature"
    }

    data class UnsupportedType(val type: Handle<Type>) : BackendError() {
        override val message: String get() = "Type not supported: $type"
    }

    data class UnsupportedExpression(val expr: Handle<Expression>) : BackendError() {
        override val message: String get() = "Expression not supported: $expr"
    }

    data class UnsupportedStatement(val stmt: Handle<Statement>) : BackendError() {
        override val message: String get() = "Statement not supported: $stmt"
    }

    data class InternalError(val cause: Throwable) : BackendError() {
        override val message: String get() = "Internal error: ${cause.message}"
    }

    data class GenericError(override val message: String) : BackendError()
}

class BackendException(val error: BackendError) : Exception(error.message)
