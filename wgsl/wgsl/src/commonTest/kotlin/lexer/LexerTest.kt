package io.ygdrasil.wgsl.lexer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ygdrasil.wgsl.ir.Span

class LexerTest : FunSpec({
    context("Basic tokenization") {
        test("empty source") {
            val tokens = tokenize("")
            tokens shouldHaveSize 0
        }

        test("empty source returns EOF") {
            val lexer = Lexer("")
            val token = lexer.next()
            token shouldNotBe null
            token?.isEof shouldBe true
        }

        test("whitespace only") {
            val tokens = tokenizeSignificant("   \t\n\r  ")
            tokens shouldHaveSize 0
        }

        test("single identifier") {
            val tokens = tokenizeSignificant("foo")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.IDENTIFIER
            tokens[0].literal shouldBe "foo"
        }

        test("identifier with underscore") {
            val tokens = tokenizeSignificant("foo_bar")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.IDENTIFIER
            tokens[0].literal shouldBe "foo_bar"
        }

        test("identifier with numbers") {
            val tokens = tokenizeSignificant("foo123")
            tokens shouldHaveSize 1
            tokens[0].kind shouldBe TokenKind.IDENTIFIER
            tokens[0].literal shouldBe "foo123"
        }
    }

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

    context("Complex expressions") {
        test("simple function call") {
            val source = "foo(42)"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 4
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.IDENTIFIER, TokenKind.LEFT_PAREN,
                TokenKind.INT_LITERAL, TokenKind.RIGHT_PAREN
            )
        }

        test("function declaration") {
            val source = "fn main() {}"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 6
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.FN, TokenKind.IDENTIFIER, TokenKind.LEFT_PAREN,
                TokenKind.RIGHT_PAREN, TokenKind.LEFT_BRACE, TokenKind.RIGHT_BRACE
            )
        }

        test("variable declaration") {
            val source = "let x: i32 = 42;"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 7
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.LET, TokenKind.IDENTIFIER, TokenKind.COLON,
                TokenKind.I32, TokenKind.ASSIGN, TokenKind.INT_LITERAL,
                TokenKind.SEMICOLON
            )
        }

        test("struct declaration") {
            val source = "struct Foo { x: i32 }"
            val tokens = tokenizeSignificant(source)
            tokens shouldHaveSize 7
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.STRUCT, TokenKind.IDENTIFIER, TokenKind.LEFT_BRACE,
                TokenKind.IDENTIFIER, TokenKind.COLON, TokenKind.I32,
                TokenKind.RIGHT_BRACE
            )
        }

        test("if statement") {
            val source = "if (x > 0) { return; }"
            val tokens = tokenizeSignificant(source)
            tokens.map { it.kind } shouldContainExactly listOf(
                TokenKind.IF, TokenKind.LEFT_PAREN, TokenKind.IDENTIFIER,
                TokenKind.GT, TokenKind.INT_LITERAL, TokenKind.RIGHT_PAREN,
                TokenKind.LEFT_BRACE, TokenKind.RETURN, TokenKind.SEMICOLON,
                TokenKind.RIGHT_BRACE
            )
        }
    }

    context("Span information") {
        test("token spans are correct") {
            val tokens = tokenize("foo bar")
            // tokenize() skips whitespace, so we get: foo, bar, EOF
            // But EOF is not included, so we get: foo, bar
            tokens shouldHaveSize 2
            
            tokens[0].kind shouldBe TokenKind.IDENTIFIER
            tokens[0].literal shouldBe "foo"
            tokens[0].span.start shouldBe 0u
            tokens[0].span.end shouldBe 3u
            
            tokens[1].kind shouldBe TokenKind.IDENTIFIER
            tokens[1].literal shouldBe "bar"
            tokens[1].span.start shouldBe 4u
            tokens[1].span.end shouldBe 7u
        }
        
        // TODO: Enable when whitespace tokens are produced
        // test("whitespace span is correct") {
        //     val lexer = Lexer("foo bar")
        //     val foo = lexer.next()!!
        //     val space = lexer.next()!!
        //     val bar = lexer.next()!!
        //     
        //     foo.kind shouldBe TokenKind.IDENTIFIER
        //     foo.span.start shouldBe 0u
        //     foo.span.end shouldBe 3u
        //     
        //     space.kind shouldBe TokenKind.WHITESPACE
        //     space.span.start shouldBe 3u
        //     space.span.end shouldBe 4u
        //     
        //     bar.kind shouldBe TokenKind.IDENTIFIER
        //     bar.span.start shouldBe 4u
        //     bar.span.end shouldBe 7u
        // }
    }

    context("TokenStream interface") {
        test("peek does not consume") {
            val lexer = Lexer("foo bar")
            val firstPeek = lexer.peek()
            val secondPeek = lexer.peek()
            
            firstPeek shouldNotBe null
            secondPeek shouldNotBe null
            firstPeek shouldBe secondPeek
        }

        test("next consumes") {
            val lexer = Lexer("foo bar")
            val first = lexer.next()
            val second = lexer.next()
            
            first shouldNotBe null
            second shouldNotBe null
            first shouldNotBe second
        }

        test("peek then next") {
            val lexer = Lexer("foo bar")
            val peeked = lexer.peek()
            val next = lexer.next()
            
            peeked shouldBe next
        }
    }
})
