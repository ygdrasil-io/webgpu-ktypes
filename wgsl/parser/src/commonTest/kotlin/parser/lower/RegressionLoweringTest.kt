package io.ygdrasil.wgsl.parser.lower

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.parser.TestUtils.lowerWgsl

/**
 * Tests de régression pour les bugs connus du Lowerer.
 * Ces tests DOIVENT échouer avec l'implémentation actuelle.
 */
class RegressionLoweringTest : FunSpec({
    test("P001: vec3<f32> should not duplicate Scalar F32 type") {
        val module = lowerWgsl("fn main() -> vec3<f32> { return vec3(1.0); }")
        
        val scalarF32Count = module.types.toList().count {
            it.inner is TypeInner.Scalar && 
            (it.inner as TypeInner.Scalar).kind == ScalarKind.F32
        }
        
        // Bug: crée un nouveau F32 pour chaque vec3
        scalarF32Count shouldBe 1
    }

    test("P002: StructType should resolve to actual struct, not empty") {
        val module = lowerWgsl("""
            struct Inner { a: i32 }
            struct Outer { inner: Inner }
        """)
        
        // Trouver Outer (celui qui a un membre 'inner')
        val outerType = module.types.toList().find { type ->
            type.inner is TypeInner.Struct && 
            (type.inner as TypeInner.Struct).members.any { it.name == "inner" }
        }
        outerType shouldNotBe null
        
        val innerMember = (outerType!!.inner as TypeInner.Struct).members.find { it.name == "inner" }
        innerMember shouldNotBe null
        
        // Le type du membre inner doit pointer vers Inner (pas un struct vide)
        val innerType = module.types[innerMember!!.type]
        (innerType.inner as? TypeInner.Struct)?.members?.isEmpty() shouldBe false
    }

    test("P003: Global variable initializer should not be null") {
        val module = lowerWgsl("var<private> global: i32 = 42;")
        
        val globalVar = module.globalVariables.toList().first()
        globalVar.init shouldNotBe null
    }

    test("P004: Undefined variable should throw error") {
        val exception = kotlin.runCatching {
            lowerWgsl("fn main() -> i32 { return undefined_var; }")
        }.exceptionOrNull()
        
        exception shouldNotBe null
    }

    test("P005: s.b should use correct member index") {
        val module = lowerWgsl("""
            struct S { a: i32, b: i32 }
            fn main() -> i32 { return S(1, 2).b; }
        """)
        
        val mainFunc = module.functions.toList().first { it.name == "main" }
        val bodyBlock = mainFunc.blocks[mainFunc.body]
        val returnStmt = bodyBlock.statements.first { it is Statement.Return } as Statement.Return
        val accessExpr = returnStmt.value?.let { module.globalExpressions[it] }
        
        accessExpr shouldNotBe null
        
        if (accessExpr!!.kind is ExpressionKind.AccessIndex) {
            val index = (accessExpr.kind as ExpressionKind.AccessIndex).index
            // b est le 2ème membre (index 1), pas 0
            index shouldBe 1u
        }
    }

    test("P008: For loop should not be Nop") {
        val module = lowerWgsl("""
            fn main() {
                for (var i: i32 = 0; i < 10; i = i + 1) {}
            }
        """)
        
        val mainFunc = module.functions.toList().first { it.name == "main" }
        val bodyBlock = mainFunc.blocks[mainFunc.body]
        bodyBlock.statements.any { it is Statement.Loop } shouldBe true
    }

    test("P008: While loop should not be Nop") {
        val module = lowerWgsl("""
            fn main() {
                while (true) {}
            }
        """)
        
        val mainFunc = module.functions.toList().first { it.name == "main" }
        val bodyBlock = mainFunc.blocks[mainFunc.body]
        bodyBlock.statements.any { it is Statement.Loop } shouldBe true
    }
})
