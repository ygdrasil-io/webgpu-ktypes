package io.ygdrasil.wgsl.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.ygdrasil.wgsl.ast.*
import io.ygdrasil.wgsl.ir.Span

class TypeIndexTest : FunSpec({
    
    context("Built-in types") {
        test("scalar types are known") {
            val index = TypeIndex()
            
            index.isBuiltinScalarType("bool") shouldBeTrue
            index.isBuiltinScalarType("i8") shouldBeTrue
            index.isBuiltinScalarType("u8") shouldBeTrue
            index.isBuiltinScalarType("i16") shouldBeTrue
            index.isBuiltinScalarType("u16") shouldBeTrue
            index.isBuiltinScalarType("i32") shouldBeTrue
            index.isBuiltinScalarType("u32") shouldBeTrue
            index.isBuiltinScalarType("i64") shouldBeTrue
            index.isBuiltinScalarType("u64") shouldBeTrue
            index.isBuiltinScalarType("f16") shouldBeTrue
            index.isBuiltinScalarType("f32") shouldBeTrue
            index.isBuiltinScalarType("f64") shouldBeTrue
            
            index.isBuiltinScalarType("unknown") shouldBeFalse
        }
        
        test("get builtin scalar type") {
            val index = TypeIndex()
            
            val boolType = index.getBuiltinScalarType("bool")
            boolType shouldNotBeNull
            boolType?.kind shouldBe ScalarKind.BOOL
            
            val i32Type = index.getBuiltinScalarType("i32")
            i32Type shouldNotBeNull
            i32Type?.kind shouldBe ScalarKind.I32
        }
        
        test("get builtin scalar kind") {
            val index = TypeIndex()
            
            index.getBuiltinScalarKind("f32") shouldBe ScalarKind.F32
            index.getBuiltinScalarKind("u16") shouldBe ScalarKind.U16
            index.getBuiltinScalarKind("unknown") shouldBeNull
        }
        
        test("vector type detection") {
            val index = TypeIndex()
            
            index.isBuiltinVectorType("vec2") shouldBeTrue
            index.isBuiltinVectorType("vec3") shouldBeTrue
            index.isBuiltinVectorType("vec4") shouldBeTrue
            index.isBuiltinVectorType("vec5") shouldBeFalse
            index.isBuiltinVectorType("vector2") shouldBeFalse
        }
        
        test("parse vector type") {
            val index = TypeIndex()
            
            val result = index.parseBuiltinVectorType("vec2<f32>")
            result shouldNotBeNull
            result?.first shouldBe 2
            result?.second shouldBe "f32"
            
            val result3 = index.parseBuiltinVectorType("vec3<i32>")
            result3 shouldNotBeNull
            result3?.first shouldBe 3
            result3?.second shouldBe "i32"
        }
        
        test("matrix type detection") {
            val index = TypeIndex()
            
            index.isBuiltinMatrixType("mat2x2") shouldBeTrue
            index.isBuiltinMatrixType("mat3x4") shouldBeTrue
            index.isBuiltinMatrixType("mat4x4") shouldBeTrue
            index.isBuiltinMatrixType("matrix2x2") shouldBeFalse
        }
        
        test("parse matrix type") {
            val index = TypeIndex()
            
            val result = index.parseBuiltinMatrixType("mat2x3<f32>")
            result shouldNotBeNull
            result?.first shouldBe 2  // columns
            result?.second shouldBe 3  // rows
            result?.third shouldBe "f32"
        }
    }
    
    context("Indexing declarations") {
        test("index empty translation unit") {
            val index = TypeIndex()
            val unit = TranslationUnit(emptyList(), Span.UNDEFINED)
            index.index(unit)
            
            index.getAllStructs() shouldHaveSize 0
            index.getAllFunctions() shouldHaveSize 0
            index.getAllGlobalVariables() shouldHaveSize 0
            index.getAllGlobalConstants() shouldHaveSize 0
            index.getAllTypeAliases() shouldHaveSize 0
        }
        
        test("index struct declaration") {
            val index = TypeIndex()
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            val unit = TranslationUnit(listOf(struct), Span.UNDEFINED)
            index.index(unit)
            
            index.findStruct("MyStruct") shouldNotBeNull
            index.findStruct("Unknown") shouldBeNull
            index.getAllStructs() shouldHaveSize 1
        }
        
        test("index function declaration") {
            val index = TypeIndex()
            val func = FunctionDecl(emptyList(), "myFunc", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(func), Span.UNDEFINED)
            index.index(unit)
            
            index.findFunction("myFunc") shouldNotBeNull
            index.findFunction("Unknown") shouldBeNull
            index.getAllFunctions() shouldHaveSize 1
        }
        
        test("index variable declaration - let") {
            val index = TypeIndex()
            val varDecl = VariableDecl(VariableDeclKind.LET, emptyList(), "myVar", null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(varDecl), Span.UNDEFINED)
            index.index(unit)
            
            index.findGlobalVariable("myVar") shouldNotBeNull
            index.findGlobalVariable("Unknown") shouldBeNull
            index.getAllGlobalVariables() shouldHaveSize 1
        }
        
        test("index variable declaration - const") {
            val index = TypeIndex()
            val varDecl = VariableDecl(VariableDeclKind.CONST, emptyList(), "MY_CONST", null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(varDecl), Span.UNDEFINED)
            index.index(unit)
            
            index.findGlobalConstant("MY_CONST") shouldNotBeNull
            index.findGlobalConstant("Unknown") shouldBeNull
            index.getAllGlobalConstants() shouldHaveSize 1
        }
        
        test("index type alias declaration") {
            val index = TypeIndex()
            val scalarType = ScalarType(ScalarKind.I32, Span.UNDEFINED)
            val typeAlias = TypeAliasDecl(emptyList(), "MyInt", emptyList(), scalarType, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(typeAlias), Span.UNDEFINED)
            index.index(unit)
            
            index.findTypeAlias("MyInt") shouldNotBeNull
            index.findTypeAlias("Unknown") shouldBeNull
            index.getAllTypeAliases() shouldHaveSize 1
        }
        
        test("index multiple declarations") {
            val index = TypeIndex()
            val struct = StructDecl(emptyList(), "Struct1", emptyList(), emptyList(), Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "func1", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val varDecl = VariableDecl(VariableDeclKind.LET, emptyList(), "var1", null, null, Span.UNDEFINED)
            val constDecl = VariableDecl(VariableDeclKind.CONST, emptyList(), "CONST1", null, null, Span.UNDEFINED)
            val typeAlias = TypeAliasDecl(emptyList(), "Type1", emptyList(), ScalarType(ScalarKind.I32, Span.UNDEFINED), Span.UNDEFINED)
            
            val unit = TranslationUnit(listOf(struct, func, varDecl, constDecl, typeAlias), Span.UNDEFINED)
            index.index(unit)
            
            index.getAllStructs() shouldHaveSize 1
            index.getAllFunctions() shouldHaveSize 1
            index.getAllGlobalVariables() shouldHaveSize 1
            index.getAllGlobalConstants() shouldHaveSize 1
            index.getAllTypeAliases() shouldHaveSize 1
        }
    }
    
    context("Lookup methods") {
        test("isKnownType for built-in types") {
            val index = TypeIndex()
            
            index.isKnownType("i32") shouldBeTrue
            index.isKnownType("vec3<f32>") shouldBeFalse  // Not parsed yet
            index.isKnownType("Unknown") shouldBeFalse
        }
        
        test("isKnownType for user-declared types") {
            val index = TypeIndex()
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            val typeAlias = TypeAliasDecl(emptyList(), "MyType", emptyList(), ScalarType(ScalarKind.I32, Span.UNDEFINED), Span.UNDEFINED)
            val unit = TranslationUnit(listOf(struct, typeAlias), Span.UNDEFINED)
            index.index(unit)
            
            index.isKnownType("MyStruct") shouldBeTrue
            index.isKnownType("MyType") shouldBeTrue
            index.isKnownType("Unknown") shouldBeFalse
        }
        
        test("isKnownValue for built-in values") {
            val index = TypeIndex()
            
            index.isBuiltinValue("true") shouldBeTrue
            index.isBuiltinValue("false") shouldBeTrue
            index.isBuiltinValue("True") shouldBeFalse
        }
        
        test("isKnownValue for user-declared values") {
            val index = TypeIndex()
            val func = FunctionDecl(emptyList(), "myFunc", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val varDecl = VariableDecl(VariableDeclKind.LET, emptyList(), "myVar", null, null, Span.UNDEFINED)
            val constDecl = VariableDecl(VariableDeclKind.CONST, emptyList(), "MY_CONST", null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(func, varDecl, constDecl), Span.UNDEFINED)
            index.index(unit)
            
            index.isKnownValue("myFunc") shouldBeTrue
            index.isKnownValue("myVar") shouldBeTrue
            index.isKnownValue("MY_CONST") shouldBeTrue
            index.isKnownValue("Unknown") shouldBeFalse
        }
        
        test("findDeclaration returns any declaration") {
            val index = TypeIndex()
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "myFunc", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(struct, func), Span.UNDEFINED)
            index.index(unit)
            
            index.findDeclaration("MyStruct") shouldNotBeNull
            index.findDeclaration("myFunc") shouldNotBeNull
            index.findDeclaration("Unknown") shouldBeNull
        }
        
        test("getAllDeclaredNames") {
            val index = TypeIndex()
            val struct = StructDecl(emptyList(), "Struct1", emptyList(), emptyList(), Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "func1", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val varDecl = VariableDecl(VariableDeclKind.LET, emptyList(), "var1", null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(struct, func, varDecl), Span.UNDEFINED)
            index.index(unit)
            
            val names = index.getAllDeclaredNames()
            names shouldHaveSize 3
            names shouldContain "Struct1"
            names shouldContain "func1"
            names shouldContain "var1"
        }
        
        test("isDeclared") {
            val index = TypeIndex()
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            val unit = TranslationUnit(listOf(struct), Span.UNDEFINED)
            index.index(unit)
            
            index.isDeclared("MyStruct") shouldBeTrue
            index.isDeclared("Unknown") shouldBeFalse
        }
    }
    
    context("Reset functionality") {
        test("reset clears user-declared types") {
            val index = TypeIndex()
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            val unit = TranslationUnit(listOf(struct), Span.UNDEFINED)
            
            index.index(unit)
            index.getAllStructs() shouldHaveSize 1
            
            index.reset()
            index.getAllStructs() shouldHaveSize 0
            
            // Built-in types should still be available
            index.isBuiltinScalarType("i32") shouldBeTrue
        }
    }
})
