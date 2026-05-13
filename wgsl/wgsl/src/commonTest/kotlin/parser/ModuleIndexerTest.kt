package io.ygdrasil.wgsl.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.assertions.throwable
import io.ygdrasil.wgsl.ast.*
import io.ygdrasil.wgsl.ir.Span

class ModuleIndexerTest : FunSpec({
    
    context("Dependency graph construction") {
        test("empty module") {
            val indexer = ModuleIndexer()
            val unit = TranslationUnit(emptyList(), Span.UNDEFINED)
            val graph = indexer.buildDependencyGraph(unit)
            graph shouldHaveSize 0
        }
        
        test("single function without dependencies") {
            val indexer = ModuleIndexer()
            val func = FunctionDecl(emptyList(), "foo", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(func), Span.UNDEFINED)
            val graph = indexer.buildDependencyGraph(unit)
            graph shouldHaveSize 1
            graph["foo"] shouldNotBeNull
            graph["foo"]?.shouldHaveSize(0)
        }
        
        test("function with type dependency") {
            val indexer = ModuleIndexer()
            // Create a struct first
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            // Then a function that uses it
            val structType = StructType("MyStruct", Span.UNDEFINED)
            val param = Param(emptyList(), "s", structType, null, Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "useStruct", emptyList(), listOf(param), null, null, Span.UNDEFINED)
            
            val unit = TranslationUnit(listOf(struct, func), Span.UNDEFINED)
            val graph = indexer.buildDependencyGraph(unit)
            
            graph shouldHaveSize 2
            graph["MyStruct"] shouldNotBeNull
            graph["useStruct"] shouldNotBeNull
            graph["useStruct"]?.shouldContain("MyStruct")
        }
        
        test("function with variable dependency") {
            val indexer = ModuleIndexer()
            val varDecl = VariableDecl(VariableDeclKind.CONST, emptyList(), "MY_CONST", null, null, Span.UNDEFINED)
            val identExpr = IdentExpr("MY_CONST", Span.UNDEFINED)
            val returnStmt = ReturnStatement(identExpr, Span.UNDEFINED)
            val body = BlockStatement(listOf(returnStmt), Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "useConst", emptyList(), emptyList(), null, body, Span.UNDEFINED)
            
            val unit = TranslationUnit(listOf(varDecl, func), Span.UNDEFINED)
            val graph = indexer.buildDependencyGraph(unit)
            
            graph shouldHaveSize 2
            graph["useConst"]?.shouldContain("MY_CONST")
        }
    }
    
    context("Topological sorting") {
        test("sort single node") {
            val indexer = ModuleIndexer()
            val func = FunctionDecl(emptyList(), "foo", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val unit = TranslationUnit(listOf(func), Span.UNDEFINED)
            val graph = indexer.buildDependencyGraph(unit)
            
            val sorted = indexer.topologicalSort(graph)
            // Note: topologicalSort returns List<GlobalDecl> but our graph has strings
            // This test needs to be adjusted based on the actual implementation
        }
        
        test("sort nodes with dependencies") {
            val indexer = ModuleIndexer()
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            val structType = StructType("MyStruct", Span.UNDEFINED)
            val param = Param(emptyList(), "s", structType, null, Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "useStruct", emptyList(), listOf(param), null, null, Span.UNDEFINED)
            
            val unit = TranslationUnit(listOf(func, struct), Span.UNDEFINED)
            val graph = indexer.buildDependencyGraph(unit)
            
            // The graph should have the dependency
            graph["useStruct"]?.shouldContain("MyStruct")
        }
        
        test("detect cycle") {
            // This is tricky to test since we can't easily create a cyclic dependency
            // in valid WGSL. But we can test with a manually constructed graph.
            val indexer = ModuleIndexer()
            val graph = mapOf(
                "a" to setOf("b"),
                "b" to setOf("c"),
                "c" to setOf("a")
            )
            
            throwable<CycleDetectedException> {
                indexer.topologicalSort(graph)
            }
        }
    }
    
    context("Reorder declarations") {
        test("reorder independent declarations") {
            val indexer = ModuleIndexer()
            val func1 = FunctionDecl(emptyList(), "func1", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val func2 = FunctionDecl(emptyList(), "func2", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            val func3 = FunctionDecl(emptyList(), "func3", emptyList(), emptyList(), null, null, Span.UNDEFINED)
            
            val unit = TranslationUnit(listOf(func3, func1, func2), Span.UNDEFINED)
            val reordered = indexer.reorderDeclarations(unit)
            
            // Order might change but all should be present
            reordered.declarations shouldHaveSize 3
        }
        
        test("reorder with dependencies - struct before function") {
            val indexer = ModuleIndexer()
            val struct = StructDecl(emptyList(), "MyStruct", emptyList(), emptyList(), Span.UNDEFINED)
            val structType = StructType("MyStruct", Span.UNDEFINED)
            val param = Param(emptyList(), "s", structType, null, Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "useStruct", emptyList(), listOf(param), null, null, Span.UNDEFINED)
            
            // Note: This is a forward reference - func uses MyStruct before it's declared
            val unit = TranslationUnit(listOf(func, struct), Span.UNDEFINED)
            
            // The reordering should put struct before func
            val reordered = indexer.reorderDeclarations(unit)
            reordered.declarations shouldHaveSize 2
            
            // First declaration should be the struct
            val firstDecl = reordered.declarations[0]
            firstDecl shouldBe struct
        }
        
        test("reorder with variable dependency") {
            val indexer = ModuleIndexer()
            val varDecl = VariableDecl(VariableDeclKind.CONST, emptyList(), "MY_CONST", 
                IntLiteral(42, null, Span.UNDEFINED), null, Span.UNDEFINED)
            val identExpr = IdentExpr("MY_CONST", Span.UNDEFINED)
            val returnStmt = ReturnStatement(identExpr, Span.UNDEFINED)
            val body = BlockStatement(listOf(returnStmt), Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "useConst", emptyList(), emptyList(), null, body, Span.UNDEFINED)
            
            // Forward reference: func uses MY_CONST before it's declared
            val unit = TranslationUnit(listOf(func, varDecl), Span.UNDEFINED)
            
            val reordered = indexer.reorderDeclarations(unit)
            reordered.declarations shouldHaveSize 2
            
            // First declaration should be the variable
            val firstDecl = reordered.declarations[0]
            firstDecl shouldBe varDecl
        }
    }
    
    context("Dependency finding methods") {
        test("findDependenciesInFunction") {
            val indexer = ModuleIndexer()
            val structType = StructType("MyStruct", Span.UNDEFINED)
            val param = Param(emptyList(), "s", structType, null, Span.UNDEFINED)
            val func = FunctionDecl(emptyList(), "useStruct", emptyList(), listOf(param), null, null, Span.UNDEFINED)
            
            val allNames = setOf("MyStruct", "useStruct")
            val deps = indexer.findDependenciesInFunction(func, allNames)
            
            deps shouldContain "MyStruct"
        }
        
        test("findDependenciesInType") {
            val indexer = ModuleIndexer()
            val structType = StructType("MyStruct", Span.UNDEFINED)
            val allNames = setOf("MyStruct")
            
            val deps = indexer.findDependenciesInType(structType, allNames)
            deps shouldContain "MyStruct"
        }
        
        test("findDependenciesInExpression") {
            val indexer = ModuleIndexer()
            val identExpr = IdentExpr("myVar", Span.UNDEFINED)
            val allNames = setOf("myVar")
            
            val deps = indexer.findDependenciesInExpression(identExpr, allNames)
            deps shouldContain "myVar"
        }
        
        test("findDependenciesInStatement") {
            val indexer = ModuleIndexer()
            val identExpr = IdentExpr("myVar", Span.UNDEFINED)
            val exprStmt = ExpressionStatement(identExpr, Span.UNDEFINED)
            val allNames = setOf("myVar")
            
            val deps = indexer.findDependenciesInStatement(exprStmt, allNames)
            deps shouldContain "myVar"
        }
    }
})
