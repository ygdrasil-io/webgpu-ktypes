package io.ygdrasil.wgsl.proc

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

class TypifierTest : FunSpec({

    test("Resolve literal types") {
        val module = Module()
        val typifier = Typifier()
        
        val exprI32 = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.I32(1)))))
        val exprF32 = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.F32(1.0f)))))
        
        typifier.fill(module, null, module.globalExpressions)
        
        val resI32 = typifier[exprI32]
        val innerI32 = resI32.getInner(module)
        innerI32.shouldBeInstanceOf<TypeInner.Scalar>()
        innerI32.kind shouldBe ScalarKind.S32
        
        val resF32 = typifier[exprF32]
        val innerF32 = resF32.getInner(module)
        innerF32.shouldBeInstanceOf<TypeInner.Scalar>()
        innerF32.kind shouldBe ScalarKind.F32
    }

    test("Resolve binary operation type") {
        val module = Module()
        val typifier = Typifier()
        
        val left = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.I32(1)))))
        val right = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.I32(2)))))
        val add = module.globalExpressions.append(Expression(ExpressionKind.Binary(BinaryOperator.Add, left, right)))
        
        typifier.fill(module, null, module.globalExpressions)
        
        val resAdd = typifier[add]
        val innerAdd = resAdd.getInner(module)
        innerAdd.shouldBeInstanceOf<TypeInner.Scalar>()
        innerAdd.kind shouldBe ScalarKind.S32
    }

    test("Resolve splat type") {
        val module = Module()
        val typifier = Typifier()
        
        val scalar = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.F32(1.0f)))))
        val splat = module.globalExpressions.append(Expression(ExpressionKind.Splat(VectorSize.Tri, scalar)))
        
        typifier.fill(module, null, module.globalExpressions)
        
        val resSplat = typifier[splat]
        val innerSplat = resSplat.getInner(module)
        innerSplat.shouldBeInstanceOf<TypeInner.Vector>()
        innerSplat.size shouldBe VectorSize.Tri
        
        val scalarType = module.types[innerSplat.scalar]
        (scalarType.inner as TypeInner.Scalar).kind shouldBe ScalarKind.F32
    }
})
