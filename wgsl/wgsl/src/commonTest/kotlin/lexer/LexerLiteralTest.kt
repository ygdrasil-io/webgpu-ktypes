package io.ygdrasil.wgsl.lexer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LexerLiteralTest : FunSpec({
    context("Numeric literals") {
        test("integer literal") {
            val tokens = tokenizeSignificant("42")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.INT_LITERAL
            tokens[0].literal shouldBe "42"
        }

        test("negative integer is parsed as minus + integer") {
            val tokens = tokenizeSignificant("-42")
            tokens shouldHaveSize 2
            tokens[0].kind shouldBe TokenKind.MINUS
            tokens[1].kind shouldBe TokenKind.INT_LITERAL
            tokens[1].literal shouldBe "42"
        }

        test("unsigned integer literal") {
            val tokens = tokenizeSignificant("42u")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.UINT_LITERAL
            tokens[0].literal shouldBe "42u"
        }

        test("unsigned integer literal uppercase") {
            val tokens = tokenizeSignificant("42U")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.UINT_LITERAL
            tokens[0].literal shouldBe "42U"
        }

        test("hexadecimal integer") {
            val tokens = tokenizeSignificant("0x2a")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.INT_LITERAL
            tokens[0].literal shouldBe "0x2a"
        }

        test("hexadecimal unsigned") {
            val tokens = tokenizeSignificant("0x2au")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.UINT_LITERAL
            tokens[0].literal shouldBe "0x2au"
        }

        test("float literal") {
            val tokens = tokenizeSignificant("3.14")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.FLOAT_LITERAL
            tokens[0].literal shouldBe "3.14"
        }

        test("float literal with exponent") {
            val tokens = tokenizeSignificant("1.0e-5")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.FLOAT_LITERAL
            tokens[0].literal shouldBe "1.0e-5"
        }

        test("float literal with f suffix") {
            val tokens = tokenizeSignificant("3.14f")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.FLOAT_LITERAL
            tokens[0].literal shouldBe "3.14f"
        }

        test("float literal starting with dot") {
            val tokens = tokenizeSignificant(".5")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.FLOAT_LITERAL
            tokens[0].literal shouldBe ".5"
        }

        test("hex float literal") {
            val tokens = tokenizeSignificant("0x1.0p1")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.FLOAT_LITERAL
            tokens[0].literal shouldBe "0x1.0p1"
        }

        test("integer with suffixes") {
            val source = "42i 42u 42li 42lu"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 4
            tokens[0].kind shouldBe TokenKind.INT_LITERAL
            tokens[0].literal shouldBe "42i"
            tokens[1].kind shouldBe TokenKind.UINT_LITERAL
            tokens[1].literal shouldBe "42u"
            tokens[2].kind shouldBe TokenKind.INT_LITERAL
            tokens[2].literal shouldBe "42li"
            tokens[3].kind shouldBe TokenKind.UINT_LITERAL
            tokens[3].literal shouldBe "42lu"
        }

        test("float with suffixes") {
            val source = "3.14f 3.14h 3.14lf"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 3
            tokens.forEach { it.kind shouldBe TokenKind.FLOAT_LITERAL }
        }
    }

    context("Boolean literals") {
        test("true literal") {
            val tokens = tokenizeSignificant("true")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.TRUE
        }

        test("false literal") {
            val tokens = tokenizeSignificant("false")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.FALSE
        }
    }

    context("String literals") {
        test("empty string") {
            val tokens = tokenizeSignificant("\"\"")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.STRING_LITERAL
            tokens[0].literal shouldBe "\"\""
        }

        test("simple string") {
            val tokens = tokenizeSignificant("\"hello\"")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.STRING_LITERAL
            tokens[0].literal shouldBe "\"hello\""
        }

        test("string with escaped quote") {
            val tokens = tokenizeSignificant("\"he\\\"llo\"")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.STRING_LITERAL
            tokens[0].literal shouldBe "\"he\\\"llo\""
        }

        test("string with backslash escape") {
            val tokens = tokenizeSignificant("\"he\\\\llo\"")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.STRING_LITERAL
            tokens[0].literal shouldBe "\"he\\\\llo\""
        }
    }
})
