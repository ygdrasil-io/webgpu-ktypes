package io.ygdrasil.wgsl.lexer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LexerKeywordTest : FunSpec({
    context("Keywords") {
        test("control flow keywords") {
            val source = "if else switch case default loop while for break continue return discard"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 12
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.IF, TokenKind.ELSE, TokenKind.SWITCH, TokenKind.CASE,
                TokenKind.DEFAULT, TokenKind.LOOP, TokenKind.WHILE, TokenKind.FOR,
                TokenKind.BREAK, TokenKind.CONTINUE, TokenKind.RETURN,
                TokenKind.DISCARD
            )
        }

        test("declaration keywords") {
            val source = "fn let const var type struct"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 6
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.FN, TokenKind.LET, TokenKind.CONST, TokenKind.VAR,
                TokenKind.TYPE, TokenKind.STRUCT
            )
        }

        test("type keywords") {
            val source = "bool i8 u8 i16 u16 i32 u32 f16 f32"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 9
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.BOOL, TokenKind.I8, TokenKind.U8, TokenKind.I16, TokenKind.U16,
                TokenKind.I32, TokenKind.U32, TokenKind.F16, TokenKind.F32
            )
        }

        test("storage class keywords") {
            val source = "uniform storage workgroup private function"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 5
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.UNIFORM, TokenKind.STORAGE, TokenKind.WORKGROUP,
                TokenKind.PRIVATE, TokenKind.FUNCTION
            )
        }

        test("attribute keywords") {
            val source = "@enable @requires @interpolate @invariant @must_use @override @compute @fragment @vertex"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 18 // 9 @ symbols + 9 keywords
            tokens.filter { it.kind == TokenKind.AT } shouldHaveSize 9
            tokens.filter { it.kind != TokenKind.AT }.map { it.kind } shouldContainExactly listOf(
                TokenKind.ENABLE, TokenKind.REQUIRES, TokenKind.INTERPOLATE,
                TokenKind.INVARIANT, TokenKind.MUST_USE, TokenKind.OVERRIDE,
                TokenKind.COMPUTE, TokenKind.FRAGMENT, TokenKind.VERTEX
            )
        }
    }
})
