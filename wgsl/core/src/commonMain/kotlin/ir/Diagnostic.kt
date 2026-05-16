package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * Severity levels for diagnostics.
 */
@Serializable
enum class DiagnosticSeverity {
    /** No diagnostic, just information */
    Info,

    /** Warning diagnostic */
    Warning,

    /** Error diagnostic */
    Error,
}

/**
 * A diagnostic message with optional spans.
 *
 * Used for error reporting during shader compilation and validation.
 */
@Serializable
data class Diagnostic(
    /** The message content */
    val message: String,
    /** The severity level */
    val severity: DiagnosticSeverity = DiagnosticSeverity.Error,
    /** The spans associated with this diagnostic */
    val spans: List<SpanContext> = emptyList(),
) {
    companion object {
        /** Create an error diagnostic */
        fun error(message: String, vararg spans: SpanContext): Diagnostic =
            Diagnostic(message, DiagnosticSeverity.Error, spans.toList())

        /** Create a warning diagnostic */
        fun warning(message: String, vararg spans: SpanContext): Diagnostic =
            Diagnostic(message, DiagnosticSeverity.Warning, spans.toList())

        /** Create an info diagnostic */
        fun info(message: String, vararg spans: SpanContext): Diagnostic =
            Diagnostic(message, DiagnosticSeverity.Info, spans.toList())
    }

    /** Add a span with context to this diagnostic */
    fun withSpan(span: Span, description: String): Diagnostic =
        copy(spans = spans + (span to description))

    /** Add multiple spans to this diagnostic */
    fun withSpans(vararg contexts: SpanContext): Diagnostic =
        copy(spans = spans + contexts)

    /** Get the first span, if any */
    fun firstSpan(): Span? = spans.firstOrNull()?.first

    /** Get the first source location, if any */
    fun location(source: String): SourceLocation? =
        firstSpan()?.location(source)
}

/**
 * Exception with span information for error reporting.
 */
class ShaderError(
    message: String,
    val diagnostic: Diagnostic,
) : Exception(message) {
    constructor(
        message: String,
        severity: DiagnosticSeverity = DiagnosticSeverity.Error,
        spans: List<SpanContext> = emptyList(),
    ) : this(message, Diagnostic(message, severity, spans))

    constructor(
        message: String,
        span: Span,
        description: String = "",
    ) : this(message, DiagnosticSeverity.Error, listOf(span to description))
}

/**
 * Builder for creating diagnostics with spans.
 */
@DiagnosticBuilderDsl
class DiagnosticBuilder {
    private var message: String = ""
    private var severity: DiagnosticSeverity = DiagnosticSeverity.Error
    private val spans: MutableList<SpanContext> = mutableListOf()

    fun message(text: String) {
        message = text
    }

    fun severity(level: DiagnosticSeverity) {
        severity = level
    }

    fun span(span: Span, description: String = "") {
        spans += span to description
    }

    fun build(): Diagnostic = Diagnostic(message, severity, spans.toList())
}

@DslMarker
annotation class DiagnosticBuilderDsl

/**
 * Create a diagnostic builder for convenient construction.
 */
fun diagnostic(block: DiagnosticBuilder.() -> Unit): Diagnostic {
    val builder = DiagnosticBuilder()
    builder.block()
    return builder.build()
}
