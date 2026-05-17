package io.ygdrasil.wgsl.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ygdrasil.wgsl.ast.*
import io.ygdrasil.wgsl.lexer.Lexer

class GlobalDeclParserTest : FunSpec({
    test("parse struct with optional semicolon") {
        val source = """
            struct S {
                a: i32,
            }
            
            struct T {
                b: f32,
            };
        """.trimIndent()
        val lexer = Lexer(source)
        val parser = Parser(lexer)
        val unit = parser.parse()
        
        unit.declarations shouldHaveSize 2
        unit.declarations[0].shouldBeInstanceOf<StructDecl>()
        unit.declarations[1].shouldBeInstanceOf<StructDecl>()
    }

    test("parse override declaration") {
        val source = "override depth: f32 = 1.0;"
        val lexer = Lexer(source)
        val parser = Parser(lexer)
        val unit = parser.parse()
        
        unit.declarations shouldHaveSize 1
        val override = unit.declarations[0] as OverrideDecl
        override.name shouldBe "depth"
        override.type.shouldBeInstanceOf<ScalarType>()
        override.initializer.shouldBeInstanceOf<FloatLiteral>()
    }

    test("parse override without type") {
        val source = "override width = 100;"
        val lexer = Lexer(source)
        val parser = Parser(lexer)
        val unit = parser.parse()
        
        unit.declarations shouldHaveSize 1
        val override = unit.declarations[0] as OverrideDecl
        override.name shouldBe "width"
        override.type shouldBe null
        override.initializer.shouldBeInstanceOf<IntLiteral>()
    }
})
