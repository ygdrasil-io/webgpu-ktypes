package io.ygdrasil.wgsl.ir

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DiagnosticTest : FunSpec({
    test("DiagnosticSeverity values") {
        DiagnosticSeverity.Error.toString() shouldBe "Error"
        DiagnosticSeverity.Warning.toString() shouldBe "Warning"
        DiagnosticSeverity.Info.toString() shouldBe "Info"
    }
    
    test("Diagnostic.error creates error diagnostic") {
        val span = Span(0u, 10u)
        val diagnostic = Diagnostic.error("Test error", span to "here")
        
        diagnostic.message shouldBe "Test error"
        diagnostic.severity shouldBe DiagnosticSeverity.Error
        diagnostic.spans.size shouldBe 1
    }
    
    test("Diagnostic.warning creates warning diagnostic") {
        val diagnostic = Diagnostic.warning("Test warning")
        diagnostic.message shouldBe "Test warning"
        diagnostic.severity shouldBe DiagnosticSeverity.Warning
    }
    
    test("Diagnostic.info creates info diagnostic") {
        val diagnostic = Diagnostic.info("Test info")
        diagnostic.message shouldBe "Test info"
        diagnostic.severity shouldBe DiagnosticSeverity.Info
    }
    
    test("Diagnostic.withSpan adds span with context") {
        val span = Span(5u, 15u)
        val diagnostic = Diagnostic.error("Error").withSpan(span, "problem here")
        
        diagnostic.spans.size shouldBe 1
        diagnostic.spans[0].first shouldBe span
        diagnostic.spans[0].second shouldBe "problem here"
    }
    
    test("Diagnostic.withSpans adds multiple spans") {
        val span1 = Span(0u, 5u)
        val span2 = Span(10u, 15u)
        val diagnostic = Diagnostic.error("Error")
            .withSpan(span1, "first")
            .withSpan(span2, "second")
        
        diagnostic.spans.size shouldBe 2
    }
    
    test("ShaderError wraps diagnostic") {
        val span = Span(0u, 10u)
        val error = ShaderError("Test error", span, "context")
        
        error.message shouldBe "Test error"
        error.diagnostic.firstSpan() shouldBe span
    }
    
    test("DiagnosticBuilder creates diagnostic") {
        val diagnostic = diagnostic {
            message("Builder error")
            severity(DiagnosticSeverity.Warning)
            span(Span(0u, 5u), "first")
            span(Span(10u, 15u), "second")
        }
        
        diagnostic.message shouldBe "Builder error"
        diagnostic.severity shouldBe DiagnosticSeverity.Warning
        diagnostic.spans.size shouldBe 2
    }
    
    test("Diagnostic.firstSpan returns first span") {
        val span = Span(0u, 10u)
        val diagnostic = Diagnostic.error("Error", span to "test")
        diagnostic.firstSpan() shouldBe span
    }
    
    test("Diagnostic with empty spans") {
        val diagnostic = Diagnostic.error("Error")
        diagnostic.spans.isEmpty() shouldBe true
        diagnostic.firstSpan() shouldBe null
    }
})
