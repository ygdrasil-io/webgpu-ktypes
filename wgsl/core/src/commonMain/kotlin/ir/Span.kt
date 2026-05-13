package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * A source code span, used for error reporting.
 *
 * Represents a range of byte indices in source code (start is inclusive, end is exclusive).
 */
@Serializable
data class Span(
    /** Start position in bytes (inclusive) */
    val start: UInt,
    /** End position in bytes (exclusive) */
    val end: UInt,
) {
    companion object {
        /** Undefined/unknown span */
        val UNDEFINED: Span = Span(0u, 0u)
        
        /** Creates a new Span from a range of byte indices */
        fun new(start: UInt, end: UInt): Span = Span(start, end)
    }
    
    /** Returns a new Span starting at this and ending at other */
    fun until(other: Span): Span = Span(start, other.end)
    
    /** Check whether this span was defined or is a default/unknown span */
    fun isDefined(): Boolean = this != UNDEFINED
    
    /** Returns the span as a Kotlin IntRange if defined */
    fun toRange(): IntRange? = if (isDefined()) {
        start.toInt()..end.toInt()
    } else {
        null
    }
    
    /** Subsumes another span, returning the smallest span that contains both */
    fun subsume(other: Span): Span = if (!isDefined()) {
        other
    } else if (!other.isDefined()) {
        this
    } else {
        Span(
            start = start.coerceAtMost(other.start),
            end = end.coerceAtLeast(other.end)
        )
    }
    
    /** Returns a SourceLocation for this span in the provided source */
    fun location(source: String): SourceLocation {
        val startInt = start.toInt()
        val endInt = end.toInt()
        val prefix = source.substring(0, startInt)
        val lineNumber = prefix.count { it == '\n' }.toUInt() + 1u
        val lineStart = prefix.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
        val linePosition = (startInt - lineStart + 1).toUInt()
        val length = (endInt - startInt).toUInt()
        
        return SourceLocation(
            lineNumber = lineNumber,
            linePosition = linePosition,
            offset = start,
            length = length
        )
    }
}

/**
 * A human-readable representation for a span, tailored for text source.
 *
 * Corresponds roughly to GPUCompilationMessage from the WebGPU specification,
 * but with offsets and lengths in bytes (UTF-8 code units) instead of UTF-16.
 */
@Serializable
data class SourceLocation(
    /** 1-based line number */
    val lineNumber: UInt,
    /** 1-based column position in code units (bytes) of the start of the span */
    val linePosition: UInt,
    /** 0-based offset in code units (bytes) of the start of the span */
    val offset: UInt,
    /** Length in code units (bytes) of the span */
    val length: UInt,
)

/**
 * A span together with a context description for error reporting.
 */
typealias SpanContext = Pair<Span, String>
