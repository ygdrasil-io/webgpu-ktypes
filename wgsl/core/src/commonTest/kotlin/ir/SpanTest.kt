package io.ygdrasil.wgsl.ir

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SpanTest : FunSpec({
    test("Span.UNDEFINED has zero start and end") {
        Span.UNDEFINED.start shouldBe 0u
        Span.UNDEFINED.end shouldBe 0u
        Span.UNDEFINED.isDefined() shouldBe false
    }
    
    test("Defined span has correct properties") {
        val span = Span(10u, 20u)
        span.isDefined() shouldBe true
        span.start shouldBe 10u
        span.end shouldBe 20u
    }
    
    test("Span.until creates span from start to other's end") {
        val span1 = Span(10u, 20u)
        val span2 = Span(25u, 30u)
        span1.until(span2) shouldBe Span(10u, 30u)
    }
    
    test("Span.subsume creates smallest containing span") {
        val span1 = Span(10u, 20u)
        val span2 = Span(15u, 25u)
        span1.subsume(span2) shouldBe Span(10u, 25u)
    }
    
    test("Span.subsume with UNDEFINED returns defined span") {
        val span1 = Span(10u, 20u)
        span1.subsume(Span.UNDEFINED) shouldBe span1
    }
    
    test("Span.toRange converts to IntRange") {
        val span = Span(10u, 20u)
        span.toRange() shouldBe (10..20)
    }
    
    test("Span.location calculates correct position") {
        val source = "line1\nline2\nline3"
        // line1 = indices 0-4, \n = 5, line2 = indices 6-10, \n = 11, line3 = 12-16
        val span = Span(6u, 11u) // "line2" (exclusive end, so 6-10)
        val location = span.location(source)
        
        location.lineNumber shouldBe 2u
        location.linePosition shouldBe 1u
        location.offset shouldBe 6u
        location.length shouldBe 5u
    }
    
    test("SourceLocation properties") {
        val location = SourceLocation(
            lineNumber = 1u,
            linePosition = 5u,
            offset = 0u,
            length = 10u
        )
        location.lineNumber shouldBe 1u
        location.linePosition shouldBe 5u
        location.offset shouldBe 0u
        location.length shouldBe 10u
    }
    
    test("SpanContext is Pair of Span and String") {
        val span = Span(0u, 10u)
        val context: SpanContext = span to "test context"
        context.first shouldBe span
        context.second shouldBe "test context"
    }
})
