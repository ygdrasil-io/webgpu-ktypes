package io.ygdrasil.wgsl.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ygdrasil.wgsl.ast.*
import io.ygdrasil.wgsl.ir.Span
import io.ygdrasil.wgsl.lexer.Lexer

class ParserTest : FunSpec({
    
    // Helper function to parse WGSL source
    private fun parseSource(source: String): TranslationUnit {
        val lexer = Lexer(source)
        val parser = Parser(lexer)
        return parser.parseWgsl(source)
    }
    
    context("Empty and simple modules") {
        test("empty module") {
            val source = ""
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 0
        }
        
        test("module with only whitespace") {
            val source = "   \n\n  \t  "
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 0
        }
    }
    
    context("Function declarations") {
        test("simple function without parameters") {
            val source = "fn main() {}"
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 1
            
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
            func.name shouldBe "main"
            func.parameters shouldHaveSize 0
            func.returnType shouldBeNull
            func.body shouldNotBeNull
            func.body?.statements shouldHaveSize 0
        }
        
        test("function with parameters") {
            val source = "fn add(a: i32, b: i32) -> i32 { return a + b; }"
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 1
            
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
            func.name shouldBe "add"
            func.parameters shouldHaveSize 2
            func.parameters[0].name shouldBe "a"
            func.parameters[1].name shouldBe "b"
        }
        
        test("function with return type") {
            val source = "fn square(x: f32) -> f32 { return x * x; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
            func.returnType shouldNotBeNull
        }
        
        test("function with attributes") {
            val source = "@compute @workgroup_size(1, 1, 1) fn main() {}"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
            func.attributes shouldHaveSize 2
        }
    }
    
    context("Struct declarations") {
        test("simple struct") {
            val source = "struct Vertex { position: vec3<f32>, color: vec3<f32> }"
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 1
            
            val struct = unit.declarations[0] as? StructDecl
            struct shouldNotBeNull
            struct.name shouldBe "Vertex"
            struct.members shouldHaveSize 2
        }
        
        test("struct with attributes") {
            val source = "@align(16) struct Data { value: i32 }"
            val unit = parseSource(source)
            val struct = unit.declarations[0] as? StructDecl
            struct shouldNotBeNull
            struct.attributes shouldHaveSize 1
        }
    }
    
    context("Variable declarations") {
        test("global let") {
            val source = "let x: i32 = 42;"
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 1
            
            val varDecl = unit.declarations[0] as? VariableDecl
            varDecl shouldNotBeNull
            varDecl.kind shouldBe VariableDeclKind.LET
            varDecl.name shouldBe "x"
        }
        
        test("global const") {
            val source = "const PI: f32 = 3.14159;"
            val unit = parseSource(source)
            val varDecl = unit.declarations[0] as? VariableDecl
            varDecl shouldNotBeNull
            varDecl.kind shouldBe VariableDeclKind.CONST
        }
        
        test("global var") {
            val source = "var counter: i32 = 0;"
            val unit = parseSource(source)
            val varDecl = unit.declarations[0] as? VariableDecl
            varDecl shouldNotBeNull
            varDecl.kind shouldBe VariableDeclKind.VAR
        }
    }
    
    context("Type alias declarations") {
        test("simple type alias") {
            val source = "type MyVec = vec3<f32>;"
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 1
            
            val typeAlias = unit.declarations[0] as? TypeAliasDecl
            typeAlias shouldNotBeNull
            typeAlias.name shouldBe "MyVec"
        }
    }
    
    context("Override declarations") {
        test("compute override") {
            val source = "@compute @workgroup_size(1) fn main() {}"
            val unit = parseSource(source)
            // Note: This might be parsed as a function with attributes
            // A true override would be: @override(fn) but that's not valid
            // In Naga, override is used differently
        }
    }
    
    context("Expressions") {
        test("integer literal") {
            val source = "fn test() { let x = 42; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
            func.body shouldNotBeNull
            
            val varDecl = func.body?.statements?.get(0) as? VariableDeclStatement
            varDecl shouldNotBeNull
            
            val literal = varDecl.initializer as? IntLiteral
            literal shouldNotBeNull
            literal.value shouldBe 42
        }
        
        test("float literal") {
            val source = "fn test() { let x = 3.14; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            val varDecl = func.body?.statements?.get(0) as? VariableDeclStatement
            val literal = varDecl?.initializer as? FloatLiteral
            literal shouldNotBeNull
            literal.value shouldBe 3.14
        }
        
        test("boolean literals") {
            val source = "fn test() { let a = true; let b = false; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("binary expression") {
            val source = "fn test() { let x = a + b; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("call expression") {
            val source = "fn test() { let x = foo(a, b); }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("member access") {
            val source = "fn test() { let x = obj.member; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("index expression") {
            val source = "fn test() { let x = arr[0]; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("unary expression") {
            val source = "fn test() { let x = -y; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("ternary expression") {
            val source = "fn test() { let x = a ? b : c; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("type cast expression") {
            val source = "fn test() { let x = f32(y); }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
    }
    
    context("Statements") {
        test("block statement") {
            val source = "fn test() { { let x = 1; let y = 2; } }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("if statement") {
            val source = "fn test() { if (x > 0) { return 1; } }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("if-else statement") {
            val source = "fn test() { if (x > 0) { return 1; } else { return -1; } }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("switch statement") {
            val source = """
                fn test() {
                    switch (x) {
                        case 1: { return 1; }
                        case 2: { return 2; }
                        default: { return 0; }
                    }
                }
            """.trimIndent()
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("loop statement") {
            val source = "fn test() { loop { if (done) { break; } } }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("while statement") {
            val source = "fn test() { while (x < 10) { x = x + 1; } }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("for statement") {
            val source = "fn test() { for (var i: i32 = 0; i < 10; i = i + 1) { } }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("break statement") {
            val source = "fn test() { loop { break; } }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("continue statement") {
            val source = "fn test() { loop { continue; } }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("return statement") {
            val source = "fn test() -> i32 { return 42; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("return statement with expression") {
            val source = "fn test() -> i32 { return a + b; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("discard statement") {
            val source = "@fragment fn main() { discard; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("assignment statement") {
            val source = "fn test() { x = 42; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("compound assignment") {
            val source = "fn test() { x += 1; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("increment statement") {
            val source = "fn test() { x++; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("decrement statement") {
            val source = "fn test() { x--; }"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
    }
    
    context("Types") {
        test("scalar types") {
            val source = "fn test(a: i32, b: u32, c: f32, d: bool) {}"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
            func.parameters shouldHaveSize 4
        }
        
        test("vector types") {
            val source = "fn test(a: vec2<f32>, b: vec3<i32>, c: vec4<u8>) {}"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("matrix types") {
            val source = "fn test(a: mat2x2<f32>, b: mat3x4<f32>, c: mat4x4<f32>) {}"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("array types") {
            val source = "fn test(a: array<i32, 10>, b: array<f32>) {}"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("pointer types") {
            val source = "fn test(a: ptr<function, i32>, b: ptr<uniform, vec3<f32>>) {}"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
        
        test("reference types") {
            val source = "fn test(a: ref<i32>, b: ref<vec3<f32>>) {}"
            val unit = parseSource(source)
            val func = unit.declarations[0] as? FunctionDecl
            func shouldNotBeNull
        }
    }
    
    context("Complex examples") {
        test("full shader with struct and function") {
            val source = """
                struct VertexInput {
                    @location(0) position: vec3<f32>,
                    @location(1) color: vec3<f32>,
                };
                
                @vertex
                fn main(in: VertexInput) -> @builtin(position) vec4<f32> {
                    return vec4<f32>(in.position, 1.0);
                }
            """.trimIndent()
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 2
        }
        
        test("shader with compute function") {
            val source = """
                @compute @workgroup_size(8, 8, 1)
                fn main(@builtin(global_invocation_id) id: vec3<u32>) {
                    // do something
                }
            """.trimIndent()
            val unit = parseSource(source)
            unit.declarations shouldHaveSize 1
        }
    }
    
    context("Error handling") {
        test("parser handles incomplete source") {
            val source = "fn test() {"
            val unit = parseSource(source)
            // Should still produce a partial AST
            unit.declarations shouldHaveSize 1
        }
        
        test("parser handles unexpected tokens") {
            val source = "fn test() { let x = ; }"
            val unit = parseSource(source)
            // Should still produce a partial AST
            unit.declarations shouldHaveSize 1
        }
    }
})
