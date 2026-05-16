package io.ygdrasil.wgsl.lexer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LexerOperatorTest : FunSpec({
    context("Operators") {
        test("single arithmetic operators") {
            val tokens = tokenizeSignificant("+")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.PLUS
        }

        test("single minus operator") {
            val tokens = tokenizeSignificant("-")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.MINUS
        }

        test("star operator") {
            val tokens = tokenizeSignificant("*")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.STAR
        }

        test("slash operator") {
            val tokens = tokenizeSignificant("/")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.SLASH
        }

        test("percent operator") {
            val tokens = tokenizeSignificant("%")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.PERCENT
        }

        test("assignment operator") {
            val tokens = tokenizeSignificant("=")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.ASSIGN
        }

        test("equality operator") {
            val tokens = tokenizeSignificant("==")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.EQ
        }

        test("not equal operator") {
            val tokens = tokenizeSignificant("!=")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.NEQ
        }

        test("less than operator") {
            val tokens = tokenizeSignificant("<")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.LT
        }

        test("greater than operator") {
            val tokens = tokenizeSignificant(">")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.GT
        }

        test("logical and operator") {
            val tokens = tokenizeSignificant("&&")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.AND
        }

        test("logical or operator") {
            val tokens = tokenizeSignificant("||")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.OR
        }

        test("increment and decrement") {
            val source = "++ --"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 2
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.INCREMENT, TokenKind.DECREMENT
            )
        }

        test("arrow operator") {
            val tokens = tokenizeSignificant("->")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.ARROW
        }

        test("fat arrow") {
            val tokens = tokenizeSignificant("=>")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.FAT_ARROW
        }

        test("dot star") {
            val tokens = tokenizeSignificant(".*")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.DOT_STAR
        }

        test("double colon") {
            val tokens = tokenizeSignificant("::")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.COLON_COLON
        }
    }

    context("Punctuation") {
        test("parentheses") {
            val tokens = tokenizeSignificant("()")
            tokens shouldHaveSize 2
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.LEFT_PAREN, TokenKind.RIGHT_PAREN
            )
        }

        test("braces") {
            val tokens = tokenizeSignificant("{}")
            tokens shouldHaveSize 2
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.LEFT_BRACE, TokenKind.RIGHT_BRACE
            )
        }

        test("brackets") {
            val tokens = tokenizeSignificant("[]")
            tokens shouldHaveSize 2
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.LEFT_BRACKET, TokenKind.RIGHT_BRACKET
            )
        }

        test("comma and semicolon") {
            val tokens = tokenizeSignificant(",;")
            tokens shouldHaveSize 2
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.COMMA, TokenKind.SEMICOLON
            )
        }

        test("dot and colon") {
            val tokens = tokenizeSignificant(":.")
            tokens shouldHaveSize 2
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.COLON, TokenKind.DOT
            )
        }
    }
})
