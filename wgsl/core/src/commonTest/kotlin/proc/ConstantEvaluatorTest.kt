package io.ygdrasil.wgsl.proc

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ygdrasil.wgsl.ir.*

class ConstantEvaluatorTest : FunSpec({

    test("Simple arithmetic evaluation") {
        val module = Module()
        val evaluator = ConstantEvaluator(module)

        // 1 + 2
        val left = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.I32(1)))))
        val right = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.I32(2)))))
        val expr = module.globalExpressions.append(Expression(ExpressionKind.Binary(BinaryOperator.Add, left, right)))

        val result = evaluator.evaluate(expr)
        val constValue = result.value
        constValue.shouldBeInstanceOf<ConstValue.Scalar>()
        val scalar = constValue.value
        scalar.shouldBeInstanceOf<ScalarValue.I32>()
        scalar.value shouldBe 3
    }

    test("Logical AND evaluation") {
        val module = Module()
        val evaluator = ConstantEvaluator(module)

        // true && false
        val left = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.Bool(true)))))
        val right = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.Bool(false)))))
        val expr = module.globalExpressions.append(Expression(ExpressionKind.Binary(BinaryOperator.LogicalAnd, left, right)))

        val result = evaluator.evaluate(expr)
        val constValue = result.value
        constValue.shouldBeInstanceOf<ConstValue.Scalar>()
        val scalar = constValue.value
        scalar.shouldBeInstanceOf<ScalarValue.Bool>()
        scalar.value shouldBe false
    }

    test("Complex arithmetic expression") {
        val module = Module()
        val evaluator = ConstantEvaluator(module)

        // (10 - 2) * 5
        val e10 = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.I32(10)))))
        val e2 = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.I32(2)))))
        val e5 = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.I32(5)))))
        
        val sub = module.globalExpressions.append(Expression(ExpressionKind.Binary(BinaryOperator.Subtract, e10, e2)))
        val mul = module.globalExpressions.append(Expression(ExpressionKind.Binary(BinaryOperator.Multiply, sub, e5)))

        val result = evaluator.evaluate(mul)
        val constValue = result.value
        constValue.shouldBeInstanceOf<ConstValue.Scalar>()
        val scalar = constValue.value
        scalar.shouldBeInstanceOf<ScalarValue.I32>()
        scalar.value shouldBe 40
    }

    test("Splat evaluation") {
        val module = Module()
        val evaluator = ConstantEvaluator(module)

        val value = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.F32(1.0f)))))
        val splat = module.globalExpressions.append(Expression(ExpressionKind.Splat(VectorSize.Tri, value)))

        val result = evaluator.evaluate(splat)
        val constValue = result.value
        constValue.shouldBeInstanceOf<ConstValue.Vector>()
        constValue.components.size shouldBe 3
        constValue.components.forEach {
            it.shouldBeInstanceOf<ScalarValue.F32>()
            it.value shouldBe 1.0f
        }
    }

    test("Swizzle evaluation") {
        val module = Module()
        val evaluator = ConstantEvaluator(module)

        val vec = module.globalExpressions.append(Expression(ExpressionKind.Literal(LiteralValue.Vector(listOf(
            ScalarValue.F32(1.0f),
            ScalarValue.F32(2.0f),
            ScalarValue.F32(3.0f),
            ScalarValue.F32(4.0f)
        )))))
        // Swizzle .zyx (indices 2, 1, 0)
        val swizzle = module.globalExpressions.append(Expression(ExpressionKind.Swizzle(VectorSize.Tri, vec, listOf(2, 1, 0))))

        val result = evaluator.evaluate(swizzle)
        val constValue = result.value
        constValue.shouldBeInstanceOf<ConstValue.Vector>()
        constValue.components.size shouldBe 3
        (constValue.components[0] as ScalarValue.F32).value shouldBe 3.0f
        (constValue.components[1] as ScalarValue.F32).value shouldBe 2.0f
        (constValue.components[2] as ScalarValue.F32).value shouldBe 1.0f
    }
})
