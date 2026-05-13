package io.ygdrasil.wgsl.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ygdrasil.wgsl.ast.*
import io.ygdrasil.wgsl.ir.Span

class BuilderTest : FunSpec({
    
    context("AstBuilder basic functionality") {
        test("create builder") {
            val builder = AstBuilder()
            builder.declarationCount shouldBe 0
            builder.expressionCount shouldBe 0
            builder.typeCount shouldBe 0
            builder.statementCount shouldBe 0
            builder.totalCount shouldBe 0
        }
        
        test("reset clears statistics") {
            val builder = AstBuilder()
            builder.identExpr("x")
            builder.declarationCount shouldBe 0
            builder.expressionCount shouldBe 1
            
            builder.reset()
            builder.expressionCount shouldBe 0
        }
    }
    
    context("Building types") {
        test("scalar type") {
            val builder = AstBuilder()
            val type = builder.scalarType(ScalarKind.I32, Span.UNDEFINED)
            type shouldNotBeNull
            type.kind shouldBe ScalarKind.I32
            builder.typeCount shouldBe 1
        }
        
        test("vector type") {
            val builder = AstBuilder()
            val elementType = builder.scalarType(ScalarKind.F32, Span.UNDEFINED)
            val vecType = builder.vectorType(3, elementType, Span.UNDEFINED)
            vecType shouldNotBeNull
            vecType.size shouldBe 3
            builder.typeCount shouldBe 2 // scalar + vector
        }
        
        test("matrix type") {
            val builder = AstBuilder()
            val elementType = builder.scalarType(ScalarKind.F32, Span.UNDEFINED)
            val matType = builder.matrixType(4, 4, elementType, Span.UNDEFINED)
            matType shouldNotBeNull
            matType.columns shouldBe 4
            matType.rows shouldBe 4
            builder.typeCount shouldBe 2
        }
        
        test("array type") {
            val builder = AstBuilder()
            val elementType = builder.scalarType(ScalarKind.I32, Span.UNDEFINED)
            val length = builder.intLiteral(10, null, Span.UNDEFINED)
            val arrayType = builder.arrayType(elementType, length, null, Span.UNDEFINED)
            arrayType shouldNotBeNull
            arrayType.length shouldNotBeNull
            builder.typeCount shouldBe 1
            builder.expressionCount shouldBe 1
        }
        
        test("named type") {
            val builder = AstBuilder()
            val namedType = builder.namedType("MyType", Span.UNDEFINED)
            namedType shouldNotBeNull
            namedType.name shouldBe "MyType"
            builder.typeCount shouldBe 1
        }
        
        test("pointer type") {
            val builder = AstBuilder()
            val elementType = builder.scalarType(ScalarKind.I32, Span.UNDEFINED)
            val ptrType = builder.pointerType(StorageClass.FUNCTION, elementType, Span.UNDEFINED)
            ptrType shouldNotBeNull
            ptrType.storageClass shouldBe StorageClass.FUNCTION
            builder.typeCount shouldBe 2
        }
        
        test("reference type") {
            val builder = AstBuilder()
            val elementType = builder.scalarType(ScalarKind.F32, Span.UNDEFINED)
            val refType = builder.referenceType(StorageClass.UNIFORM, elementType, Span.UNDEFINED)
            refType shouldNotBeNull
            refType.storageClass shouldBe StorageClass.UNIFORM
            builder.typeCount shouldBe 2
        }
    }
    
    context("Building expressions") {
        test("int literal") {
            val builder = AstBuilder()
            val lit = builder.intLiteral(42, "i", Span.UNDEFINED)
            lit shouldNotBeNull
            lit.value shouldBe 42
            lit.suffix shouldBe "i"
            builder.expressionCount shouldBe 1
        }
        
        test("float literal") {
            val builder = AstBuilder()
            val lit = builder.floatLiteral(3.14, "f", Span.UNDEFINED)
            lit shouldNotBeNull
            lit.value shouldBe 3.14
            builder.expressionCount shouldBe 1
        }
        
        test("bool literal") {
            val builder = AstBuilder()
            val lit = builder.boolLiteral(true, Span.UNDEFINED)
            lit shouldNotBeNull
            lit.value shouldBe true
            builder.expressionCount shouldBe 1
        }
        
        test("string literal") {
            val builder = AstBuilder()
            val lit = builder.stringLiteral("hello", Span.UNDEFINED)
            lit shouldNotBeNull
            lit.value shouldBe "hello"
            builder.expressionCount shouldBe 1
        }
        
        test("ident expression") {
            val builder = AstBuilder()
            val ident = builder.identExpr("x", Span.UNDEFINED)
            ident shouldNotBeNull
            ident.name shouldBe "x"
            builder.expressionCount shouldBe 1
        }
        
        test("call expression") {
            val builder = AstBuilder()
            val func = builder.identExpr("foo", Span.UNDEFINED)
            val arg1 = builder.identExpr("a", Span.UNDEFINED)
            val arg2 = builder.identExpr("b", Span.UNDEFINED)
            val call = builder.callExpr(func, listOf(arg1, arg2), null, Span.UNDEFINED)
            call shouldNotBeNull
            call.args shouldHaveSize 2
            builder.expressionCount shouldBe 4 // func, arg1, arg2, call
        }
        
        test("member access expression") {
            val builder = AstBuilder()
            val obj = builder.identExpr("obj", Span.UNDEFINED)
            val member = builder.memberAccessExpr(obj, "field", Span.UNDEFINED)
            member shouldNotBeNull
            member.member shouldBe "field"
            builder.expressionCount shouldBe 2 // obj, member
        }
        
        test("index expression") {
            val builder = AstBuilder()
            val obj = builder.identExpr("arr", Span.UNDEFINED)
            val index = builder.intLiteral(0, null, Span.UNDEFINED)
            val indexed = builder.indexExpr(obj, index, Span.UNDEFINED)
            indexed shouldNotBeNull
            builder.expressionCount shouldBe 3 // obj, index, indexed
        }
        
        test("unary expression") {
            val builder = AstBuilder()
            val operand = builder.identExpr("x", Span.UNDEFINED)
            val unary = builder.unaryExpr(UnaryOperator.MINUS, operand, Span.UNDEFINED)
            unary shouldNotBeNull
            unary.op shouldBe UnaryOperator.MINUS
            builder.expressionCount shouldBe 2 // operand, unary
        }
        
        test("binary expression") {
            val builder = AstBuilder()
            val left = builder.identExpr("a", Span.UNDEFINED)
            val right = builder.identExpr("b", Span.UNDEFINED)
            val binary = builder.binaryExpr(left, BinaryOperator.ADD, right, Span.UNDEFINED)
            binary shouldNotBeNull
            binary.op shouldBe BinaryOperator.ADD
            builder.expressionCount shouldBe 3 // left, right, binary
        }
        
        test("ternary expression") {
            val builder = AstBuilder()
            val cond = builder.identExpr("cond", Span.UNDEFINED)
            val trueExpr = builder.intLiteral(1, null, Span.UNDEFINED)
            val falseExpr = builder.intLiteral(0, null, Span.UNDEFINED)
            val ternary = builder.ternaryExpr(cond, trueExpr, falseExpr, Span.UNDEFINED)
            ternary shouldNotBeNull
            builder.expressionCount shouldBe 4 // cond, true, false, ternary
        }
        
        test("type cast expression") {
            val builder = AstBuilder()
            val expr = builder.identExpr("x", Span.UNDEFINED)
            val type = builder.scalarType(ScalarKind.F32, Span.UNDEFINED)
            val cast = builder.typeCastExpr(expr, type, Span.UNDEFINED)
            cast shouldNotBeNull
            builder.expressionCount shouldBe 1
            builder.typeCount shouldBe 1
        }
        
        test("swizzle expression") {
            val builder = AstBuilder()
            val obj = builder.identExpr("vec", Span.UNDEFINED)
            val swizzle = builder.swizzleExpr(obj, listOf("x", "y", "z"), Span.UNDEFINED)
            swizzle shouldNotBeNull
            swizzle.components shouldHaveSize 3
            builder.expressionCount shouldBe 2 // obj, swizzle
        }
    }
    
    context("Building statements") {
        test("block statement") {
            val builder = AstBuilder()
            val stmt1 = builder.expressionStatement(builder.intLiteral(1, null, Span.UNDEFINED), Span.UNDEFINED)
            val stmt2 = builder.expressionStatement(builder.intLiteral(2, null, Span.UNDEFINED), Span.UNDEFINED)
            val block = builder.blockStatement(listOf(stmt1, stmt2), Span.UNDEFINED)
            block shouldNotBeNull
            block.statements shouldHaveSize 2
            builder.statementCount shouldBe 3 // stmt1, stmt2, block
        }
        
        test("if statement") {
            val builder = AstBuilder()
            val cond = builder.identExpr("x", Span.UNDEFINED)
            val thenStmt = builder.returnStatement(null, Span.UNDEFINED)
            val ifStmt = builder.ifStatement(cond, thenStmt, null, Span.UNDEFINED)
            ifStmt shouldNotBeNull
            ifStmt.condition shouldNotBeNull
            builder.statementCount shouldBe 2 // then, if
        }
        
        test("if-else statement") {
            val builder = AstBuilder()
            val cond = builder.identExpr("x", Span.UNDEFINED)
            val thenStmt = builder.returnStatement(null, Span.UNDEFINED)
            val elseStmt = builder.returnStatement(null, Span.UNDEFINED)
            val ifStmt = builder.ifStatement(cond, thenStmt, elseStmt, Span.UNDEFINED)
            ifStmt shouldNotBeNull
            ifStmt.elseBranch shouldNotBeNull
            builder.statementCount shouldBe 3 // then, else, if
        }
        
        test("switch statement") {
            val builder = AstBuilder()
            val expr = builder.identExpr("x", Span.UNDEFINED)
            val case1 = builder.case(builder.intLiteral(1, null, Span.UNDEFINED), 
                builder.blockStatement(emptyList(), Span.UNDEFINED), Span.UNDEFINED)
            val defaultCase = builder.defaultCase(
                builder.blockStatement(emptyList(), Span.UNDEFINED), Span.UNDEFINED)
            val switchBody = builder.switchBody(listOf(case1, defaultCase), Span.UNDEFINED)
            val switchStmt = builder.switchStatement(expr, switchBody, Span.UNDEFINED)
            switchStmt shouldNotBeNull
            switchStmt.body.cases shouldHaveSize 2
            builder.statementCount shouldBe 3 // case1, default, switch
        }
        
        test("loop statement") {
            val builder = AstBuilder()
            val body = builder.blockStatement(emptyList(), Span.UNDEFINED)
            val loop = builder.loopStatement(body, null, Span.UNDEFINED)
            loop shouldNotBeNull
            builder.statementCount shouldBe 2 // body, loop
        }
        
        test("while statement") {
            val builder = AstBuilder()
            val cond = builder.identExpr("x", Span.UNDEFINED)
            val body = builder.blockStatement(emptyList(), Span.UNDEFINED)
            val whileStmt = builder.whileStatement(cond, body, null, Span.UNDEFINED)
            whileStmt shouldNotBeNull
            builder.statementCount shouldBe 2 // body, while
        }
        
        test("for statement") {
            val builder = AstBuilder()
            val init = builder.variableDeclStatement(
                VariableDeclKind.LET, "i", builder.scalarType(ScalarKind.I32, Span.UNDEFINED),
                builder.intLiteral(0, null, Span.UNDEFINED), Span.UNDEFINED)
            val cond = builder.binaryExpr(
                builder.identExpr("i", Span.UNDEFINED),
                BinaryOperator.LT,
                builder.intLiteral(10, null, Span.UNDEFINED),
                Span.UNDEFINED)
            val update = builder.assignmentStatement(
                builder.identExpr("i", Span.UNDEFINED),
                builder.binaryExpr(
                    builder.identExpr("i", Span.UNDEFINED),
                    BinaryOperator.ADD,
                    builder.intLiteral(1, null, Span.UNDEFINED),
                    Span.UNDEFINED),
                null, Span.UNDEFINED)
            val body = builder.blockStatement(emptyList(), Span.UNDEFINED)
            val forStmt = builder.forStatement(init, cond, update, body, Span.UNDEFINED)
            forStmt shouldNotBeNull
            builder.statementCount shouldBe 5 // init, cond(expr), update, body, for
        }
        
        test("break statement") {
            val builder = AstBuilder()
            val breakStmt = builder.breakStatement(Span.UNDEFINED)
            breakStmt shouldNotBeNull
            builder.statementCount shouldBe 1
        }
        
        test("continue statement") {
            val builder = AstBuilder()
            val continueStmt = builder.continueStatement(Span.UNDEFINED)
            continueStmt shouldNotBeNull
            builder.statementCount shouldBe 1
        }
        
        test("return statement") {
            val builder = AstBuilder()
            val value = builder.identExpr("x", Span.UNDEFINED)
            val returnStmt = builder.returnStatement(value, Span.UNDEFINED)
            returnStmt shouldNotBeNull
            returnStmt.value shouldNotBeNull
            builder.statementCount shouldBe 1
        }
        
        test("discard statement") {
            val builder = AstBuilder()
            val discardStmt = builder.discardStatement(Span.UNDEFINED)
            discardStmt shouldNotBeNull
            builder.statementCount shouldBe 1
        }
        
        test("variable declaration statement") {
            val builder = AstBuilder()
            val type = builder.scalarType(ScalarKind.I32, Span.UNDEFINED)
            val init = builder.intLiteral(42, null, Span.UNDEFINED)
            val varStmt = builder.variableDeclStatement(
                VariableDeclKind.LET, "x", type, init, Span.UNDEFINED)
            varStmt shouldNotBeNull
            builder.statementCount shouldBe 1
        }
        
        test("assignment statement") {
            val builder = AstBuilder()
            val lhs = builder.identExpr("x", Span.UNDEFINED)
            val rhs = builder.intLiteral(42, null, Span.UNDEFINED)
            val assign = builder.assignmentStatement(lhs, rhs, null, Span.UNDEFINED)
            assign shouldNotBeNull
            builder.statementCount shouldBe 1
        }
        
        test("increment statement") {
            val builder = AstBuilder()
            val expr = builder.identExpr("x", Span.UNDEFINED)
            val inc = builder.incDecStatement(expr, true, Span.UNDEFINED)
            inc shouldNotBeNull
            inc.isIncrement shouldBe true
            builder.statementCount shouldBe 1
        }
        
        test("decrement statement") {
            val builder = AstBuilder()
            val expr = builder.identExpr("x", Span.UNDEFINED)
            val dec = builder.incDecStatement(expr, false, Span.UNDEFINED)
            dec shouldNotBeNull
            dec.isIncrement shouldBe false
            builder.statementCount shouldBe 1
        }
        
        test("expression statement") {
            val builder = AstBuilder()
            val expr = builder.callExpr(
                builder.identExpr("foo", Span.UNDEFINED),
                emptyList(),
                null, Span.UNDEFINED)
            val stmt = builder.expressionStatement(expr, Span.UNDEFINED)
            stmt shouldNotBeNull
            builder.statementCount shouldBe 1
        }
    }
    
    context("Building declarations") {
        test("function declaration") {
            val builder = AstBuilder()
            val param1 = builder.param(emptyList(), "a", builder.scalarType(ScalarKind.I32, Span.UNDEFINED), null, Span.UNDEFINED)
            val param2 = builder.param(emptyList(), "b", builder.scalarType(ScalarKind.I32, Span.UNDEFINED), null, Span.UNDEFINED)
            val returnType = builder.scalarType(ScalarKind.I32, Span.UNDEFINED)
            val body = builder.blockStatement(emptyList(), Span.UNDEFINED)
            val func = builder.functionDecl(emptyList(), "add", emptyList(), listOf(param1, param2), returnType, body, Span.UNDEFINED)
            func shouldNotBeNull
            func.name shouldBe "add"
            func.parameters shouldHaveSize 2
            builder.declarationCount shouldBe 1
        }
        
        test("struct declaration") {
            val builder = AstBuilder()
            val member1 = builder.structMember(emptyList(), "x", builder.scalarType(ScalarKind.F32, Span.UNDEFINED), null, Span.UNDEFINED)
            val member2 = builder.structMember(emptyList(), "y", builder.scalarType(ScalarKind.F32, Span.UNDEFINED), null, Span.UNDEFINED)
            val struct = builder.structDecl(emptyList(), "Point", emptyList(), listOf(member1, member2), Span.UNDEFINED)
            struct shouldNotBeNull
            struct.name shouldBe "Point"
            struct.members shouldHaveSize 2
            builder.declarationCount shouldBe 1
        }
        
        test("variable declaration") {
            val builder = AstBuilder()
            val type = builder.scalarType(ScalarKind.I32, Span.UNDEFINED)
            val init = builder.intLiteral(42, null, Span.UNDEFINED)
            val varDecl = builder.variableDecl(VariableDeclKind.CONST, emptyList(), "PI", type, init, Span.UNDEFINED)
            varDecl shouldNotBeNull
            varDecl.name shouldBe "PI"
            builder.declarationCount shouldBe 1
        }
        
        test("type alias declaration") {
            val builder = AstBuilder()
            val type = builder.vectorType(3, builder.scalarType(ScalarKind.F32, Span.UNDEFINED), Span.UNDEFINED)
            val typeAlias = builder.typeAliasDecl(emptyList(), "Vec3", emptyList(), type, Span.UNDEFINED)
            typeAlias shouldNotBeNull
            typeAlias.name shouldBe "Vec3"
            builder.declarationCount shouldBe 1
        }
        
        test("translation unit") {
            val builder = AstBuilder()
            val func = builder.functionDecl(emptyList(), "main", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val struct = builder.structDecl(emptyList(), "Data", emptyList(), emptyList(), Span.UNDEFINED)
            val unit = builder.translationUnit(listOf(func, struct), Span.UNDEFINED)
            unit shouldNotBeNull
            unit.declarations shouldHaveSize 2
        }
    }
    
    context("Building miscellaneous") {
        test("attribute") {
            val builder = AstBuilder()
            val arg = builder.intLiteral(1, null, Span.UNDEFINED)
            val attr = builder.attribute("align", listOf(arg), Span.UNDEFINED)
            attr shouldNotBeNull
            attr.name shouldBe "align"
            attr.args shouldHaveSize 1
        }
        
        test("param") {
            val builder = AstBuilder()
            val type = builder.scalarType(ScalarKind.I32, Span.UNDEFINED)
            val param = builder.param(emptyList(), "x", type, null, Span.UNDEFINED)
            param shouldNotBeNull
            param.name shouldBe "x"
        }
        
        test("template param") {
            val builder = AstBuilder()
            val constraint = builder.scalarType(ScalarKind.I32, Span.UNDEFINED)
            val templateParam = builder.templateParam("T", constraint, Span.UNDEFINED)
            templateParam shouldNotBeNull
            templateParam.name shouldBe "T"
        }
        
        test("struct member") {
            val builder = AstBuilder()
            val type = builder.scalarType(ScalarKind.F32, Span.UNDEFINED)
            val member = builder.structMember(emptyList(), "value", type, null, Span.UNDEFINED)
            member shouldNotBeNull
            member.name shouldBe "value"
        }
        
        test("entry point attributes") {
            val builder = AstBuilder()
            val compute = builder.entryPointCompute()
            compute shouldBe EntryPointAttribute.Compute
            
            val vertexOutput = builder.vertexOutput(location = 0, builtin = null, type = builder.scalarType(ScalarKind.F32, Span.UNDEFINED))
            val vertex = builder.entryPointVertex(listOf(vertexOutput))
            vertex shouldBe EntryPointAttribute.Vertex(listOf(vertexOutput))
            
            val fragmentInput = builder.fragmentInput(location = 0, builtin = null, type = builder.scalarType(ScalarKind.F32, Span.UNDEFINED))
            val fragment = builder.entryPointFragment(listOf(fragmentInput))
            fragment shouldBe EntryPointAttribute.Fragment(listOf(fragmentInput))
        }
    }
})
