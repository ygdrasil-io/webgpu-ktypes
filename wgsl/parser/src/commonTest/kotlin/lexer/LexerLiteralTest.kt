package io.ygdrasil.wgsl.lexer

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LexerLiteralTest : FunSpec({
    context("WGSL Numeric Literals") {
        test("Decimal and hexadecimal integers") {
            val source = "42 0x2a 42u 0x2au"
            val tokens = tokenizeSignificant(source)
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.INT_LITERAL, TokenKind.INT_LITERAL,
                TokenKind.UINT_LITERAL, TokenKind.UINT_LITERAL
            )
        }

        test("Integers with size suffixes (i/u/li/lu)") {
            val source = "42i 42u 42li 42lu"
            val tokens = tokenizeSignificant(source)
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.INT_LITERAL, TokenKind.UINT_LITERAL,
                TokenKind.INT_LITERAL, TokenKind.UINT_LITERAL
            )
        }

        test("Floating point numbers (Floats)") {
            val source = "3.14 1.0e-5 .5 0x1.0p1 3.14f 3.14h 3.14lf 0x1p+0 0x1.8p+1"
            val tokens = tokenizeSignificant(source)
            tokens.forEach { it.kind shouldBe TokenKind.FLOAT_LITERAL }
        }

        test("Digit separators") {
            val source = "1_234 0x1_234 3.141_592"
            val tokens = tokenizeSignificant(source)
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.INT_LITERAL, TokenKind.INT_LITERAL, TokenKind.FLOAT_LITERAL
            )
            tokens[0].literal shouldBe "1_234"
            tokens[1].literal shouldBe "0x1_234"
            tokens[2].literal shouldBe "3.141_592"
        }

        test("Negative sign treated as separate operator") {
            val tokens = tokenizeSignificant("-42")
            tokens shouldHaveSize 2
            tokens[0].kind shouldBe TokenKind.MINUS
            tokens[1].kind shouldBe TokenKind.INT_LITERAL
        }
    }

    context("WGSL Boolean Literals") {
        test("True and false values") {
            val source = "true false"
            val tokens = tokenizeSignificant(source)
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.TRUE, TokenKind.FALSE
            )
        }
    }

    context("WGSL String Literals") {
        test("Strings with escapes") {
            val source = "\"\" \"hello\" \"he\\\"llo\" \"he\\\\llo\""
            val tokens = tokenizeSignificant(source)
            tokens.forEach { it.kind shouldBe TokenKind.STRING_LITERAL }
        }
    }
})
