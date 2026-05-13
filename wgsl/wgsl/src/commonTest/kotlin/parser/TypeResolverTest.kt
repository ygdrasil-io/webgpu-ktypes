package io.ygdrasil.wgsl.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.ygdrasil.wgsl.ast.*
import io.ygdrasil.wgsl.ir.Span

class TypeResolverTest : FunSpec({
    
    context("Resolution result") {
        test("success result") {
            val unit = TranslationUnit(emptyList(), Span.UNDEFINED)
            val result = TypeResolver.ResolutionResult.success(unit)
            result.isSuccess shouldBeTrue
            result.unresolvedReferences shouldHaveSize 0
        }
        
        test("failure result") {
            val error = TypeResolver.UnresolvedReferenceError(
                name = "test",
                kind = TypeResolver.UnresolvedReferenceError.ReferenceKind.TYPE,
                span = Span.UNDEFINED,
                message = "Test error"
            )
            val result = TypeResolver.ResolutionResult.failure(listOf(error))
            result.isSuccess shouldBeFalse
            result.unresolvedReferences shouldHaveSize 1
        }
    }
    
    context("Type resolution") {
        test("resolve scalar type") {
            val resolver = TypeResolver()
            val scalarType = ScalarType(ScalarKind.I32, Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(scalarType, errors)
            resolved shouldBe scalarType
            errors shouldHaveSize 0
        }
        
        test("resolve named type to builtin scalar") {
            val resolver = TypeResolver()
            val namedType = NamedType("i32", Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(namedType, errors)
            resolved shouldNotBeNull
            resolved shouldBe instanceOf<ScalarType>()
            (resolved as? ScalarType)?.kind shouldBe ScalarKind.I32
            errors shouldHaveSize 0
        }
        
        test("resolve named type to builtin vector") {
            val resolver = TypeResolver()
            val namedType = NamedType("vec3<f32>", Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(namedType, errors)
            resolved shouldNotBeNull
            resolved shouldBe instanceOf<VectorType>()
            (resolved as? VectorType)?.size shouldBe 3
            errors shouldHaveSize 0
        }
        
        test("resolve named type to user-defined type alias") {
            val resolver = TypeResolver()
            
            // Create a type alias
            val scalarType = ScalarType(ScalarKind.I32, Span.UNDEFINED)
            val typeAlias = TypeAliasDecl(emptyList(), "MyInt", emptyList(), scalarType, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(typeAlias), Span.UNDEFINED)
            
            // Index it
            resolver.typeIndex.index(unit)
            
            // Try to resolve a reference to it
            val namedType = NamedType("MyInt", Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(namedType, errors)
            resolved shouldNotBeNull
            errors shouldHaveSize 0
        }
        
        test("resolve named type to user-defined struct") {
            val resolver = TypeResolver()
            
            // Create a struct
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            val unit = TranslationUnit(listOf(struct), Span.UNDEFINED)
            
            // Index it
            resolver.typeIndex.index(unit)
            
            // Try to resolve a reference to it
            val structType = StructType("MyStruct", Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(structType, errors)
            resolved shouldNotBeNull
            errors shouldHaveSize 0
        }
        
        test("unknown type returns error") {
            val resolver = TypeResolver()
            val namedType = NamedType("UnknownType", Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(namedType, errors)
            // Should return the original type
            resolved shouldBe namedType
            errors shouldHaveSize 1
            errors[0].name shouldBe "UnknownType"
            errors[0].kind shouldBe TypeResolver.UnresolvedReferenceError.ReferenceKind.TYPE
        }
        
        test("resolve nested vector type") {
            val resolver = TypeResolver()
            val innerNamed = NamedType("f32", Span.UNDEFINED)
            val vectorType = VectorType(3, innerNamed, Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(vectorType, errors)
            resolved shouldNotBeNull
            resolved shouldBe instanceOf<VectorType>()
            (resolved as? VectorType)?.size shouldBe 3
            errors shouldHaveSize 0
        }
        
        test("resolve array type with length expression") {
            val resolver = TypeResolver()
            val length = IntLiteral(10, null, Span.UNDEFINED)
            val elementType = NamedType("i32", Span.UNDEFINED)
            val arrayType = ArrayType(elementType, length, null, Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(arrayType, errors)
            resolved shouldNotBeNull
            resolved shouldBe instanceOf<ArrayType>()
            errors shouldHaveSize 0
        }
        
        test("resolve pointer type") {
            val resolver = TypeResolver()
            val elementType = NamedType("i32", Span.UNDEFINED)
            val pointerType = PointerType(StorageClass.FUNCTION, elementType, Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveTypeDecl(pointerType, errors)
            resolved shouldNotBeNull
            resolved shouldBe instanceOf<PointerType>()
            errors shouldHaveSize 0
        }
    }
    
    context("Expression resolution") {
        test("resolve builtin value - true") {
            val resolver = TypeResolver()
            val ident = IdentExpr("true", Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveExpression(ident, errors)
            resolved shouldNotBeNull
            resolved shouldBe instanceOf<BoolLiteral>()
            (resolved as? BoolLiteral)?.value shouldBe true
            errors shouldHaveSize 0
        }
        
        test("resolve builtin value - false") {
            val resolver = TypeResolver()
            val ident = IdentExpr("false", Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveExpression(ident, errors)
            resolved shouldNotBeNull
            resolved shouldBe instanceOf<BoolLiteral>()
            (resolved as? BoolLiteral)?.value shouldBe false
            errors shouldHaveSize 0
        }
        
        test("resolve call expression") {
            val resolver = TypeResolver()
            
            // Create a function
            val func = FunctionDecl(emptyList(), "myFunc", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(func), Span.UNDEFINED)
            resolver.typeIndex.index(unit)
            
            val funcExpr = IdentExpr("myFunc", Span.UNDEFINED)
            val arg = IntLiteral(42, null, Span.UNDEFINED)
            val call = CallExpr(funcExpr, listOf(arg), null, Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveExpression(call, errors)
            resolved shouldNotBeNull
            errors shouldHaveSize 0
        }
        
        test("unknown identifier returns error") {
            val resolver = TypeResolver()
            val ident = IdentExpr("unknownVar", Span.UNDEFINED)
            val errors = mutableListOf<TypeResolver.UnresolvedReferenceError>()
            
            val resolved = resolver.resolveExpression(ident, errors)
            resolved shouldBe ident
            errors shouldHaveSize 1
            errors[0].name shouldBe "unknownVar"
            errors[0].kind shouldBe TypeResolver.UnresolvedReferenceError.ReferenceKind.VALUE
        }
    }
    
    context("Full resolution") {
        test("resolve empty unit") {
            val resolver = TypeResolver()
            val unit = TranslationUnit(emptyList(), Span.UNDEFINED)
            
            val result = resolver.resolve(unit)
            result.isSuccess shouldBeTrue
            result.resolvedUnit.declarations shouldHaveSize 0
        }
        
        test("resolve function with builtin types") {
            val resolver = TypeResolver()
            val returnType = ScalarType(ScalarKind.I32, Span.UNDEFINED)
            val paramType = ScalarType(ScalarKind.I32, Span.UNDEFINED)
            val param = Param(emptyList(), "x", paramType, null, Span.UNDEFINED)
            val body = BlockStatement(emptyList(), Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "square", emptyList(), listOf(param), returnType, body, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(func), Span.UNDEFINED)
            
            val result = resolver.resolve(unit)
            result.isSuccess shouldBeTrue
            result.resolvedUnit.declarations shouldHaveSize 1
        }
        
        test("resolve struct with fields") {
            val resolver = TypeResolver()
            val fieldType1 = ScalarType(ScalarKind.F32, Span.UNDEFINED)
            val fieldType2 = ScalarType(ScalarKind.F32, Span.UNDEFINED)
            val member1 = StructMember(emptyList(), "x", fieldType1, null, Span.UNDEFINED)
            val member2 = StructMember(emptyList(), "y", fieldType2, null, Span.UNDEFINED)
            val struct = StructDecl(emptyList(), "Point", emptyList(), listOf(member1, member2), Span.UNDEFINED)
            val unit = TranslationUnit(listOf(struct), Span.UNDEFINED)
            
            val result = resolver.resolve(unit)
            result.isSuccess shouldBeTrue
            result.resolvedUnit.declarations shouldHaveSize 1
        }
        
        test("resolve with forward reference") {
            val resolver = TypeResolver()
            
            // Create a function that uses a type declared later
            val structType = NamedType("MyStruct", Span.UNDEFINED)
            val param = Param(emptyList(), "s", structType, null, Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "useStruct", emptyList(), listOf(param), null, null, Span.UNDEFINED)
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            
            // Note: This is a forward reference
            val unit = TranslationUnit(listOf(func, struct), Span.UNDEFINED)
            
            val result = resolver.resolve(unit)
            // Should succeed after reordering
            result.isSuccess shouldBeTrue
            result.resolvedUnit.declarations shouldHaveSize 2
        }
        
        test("resolve with unknown type reference") {
            val resolver = TypeResolver()
            
            val unknownType = NamedType("UnknownType", Span.UNDEFINED)
            val param = Param(emptyList(), "x", unknownType, null, Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "useUnknown", emptyList(), listOf(param), null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(func), Span.UNDEFINED)
            
            val result = resolver.resolve(unit)
            result.isSuccess shouldBeFalse
            result.unresolvedReferences shouldHaveSize 1
            result.unresolvedReferences[0].name shouldBe "UnknownType"
        }
    }
    
    context("Validation") {
        test("validate successful resolution") {
            val resolver = TypeResolver()
            val unit = TranslationUnit(emptyList(), Span.UNDEFINED)
            
            val errors = resolver.validateResolution(unit)
            errors shouldHaveSize 0
        }
        
        test("validate catches unresolved type") {
            val resolver = TypeResolver()
            
            // Create an unresolved type
            val unknownType = StructType("Unknown", Span.UNDEFINED)
            val param = Param(emptyList(), "x", unknownType, null, Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "test", emptyList(), listOf(param), null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(func), Span.UNDEFINED)
            
            // Don't index anything, so Unknown won't be found
            val errors = resolver.validateResolution(unit)
            errors shouldHaveSize 1
            errors[0].name shouldBe "Unknown"
        }
    }
})
