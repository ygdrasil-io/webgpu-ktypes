package io.ygdrasil.wgsl.lexer

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

@Ignored
class LexerCommentTest : FunSpec({
    context("Comments") {
        test("single line comment") {
            val tokens = tokenize("// comment\n42")
            tokens.filter { !it.isWhitespace && !it.isEof } shouldHaveSize 2
            tokens[0].kind shouldBe TokenKind.SINGLE_LINE_COMMENT
            tokens[1].kind shouldBe TokenKind.INT_LITERAL
        }

        test("doc comment") {
            val tokens = tokenize("/// doc comment\n42")
            tokens.filter { !it.isWhitespace && !it.isEof } shouldHaveSize 2
            tokens[0].kind shouldBe TokenKind.DOC_COMMENT
            tokens[1].kind shouldBe TokenKind.INT_LITERAL
        }

        test("multi-line comment") {
            val tokens = tokenize("/* comment */42")
            tokens.filter { !it.isWhitespace && !it.isEof } shouldHaveSize 2
            tokens[0].kind shouldBe TokenKind.MULTI_LINE_COMMENT
            tokens[1].kind shouldBe TokenKind.INT_LITERAL
        }

        test("multi-line doc comment") {
            val tokens = tokenize("/** doc comment */42")
            tokens.filter { !it.isWhitespace && !it.isEof } shouldHaveSize 2
            tokens[0].kind shouldBe TokenKind.DOC_COMMENT
            tokens[1].kind shouldBe TokenKind.INT_LITERAL
        }
    }
})
