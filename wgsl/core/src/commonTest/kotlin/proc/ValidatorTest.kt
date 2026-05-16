package io.ygdrasil.wgsl.proc

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.ygdrasil.wgsl.ir.*

class ValidatorTest : FunSpec({

    test("Validate empty module") {
        val module = Module()
        val validator = Validator()
        val errors = validator.validate(module)
        errors.shouldBeEmpty()
    }

    test("Validate module with valid type") {
        val module = Module()
        module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val validator = Validator()
        val errors = validator.validate(module)
        errors.shouldBeEmpty()
    }

    test("Validate module with invalid vector") {
        val module = Module()
        // Vector of vector (invalid)
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val vec2 = module.types.append(Type(TypeInner.Vector(VectorSize.Bi, f32)))
        module.types.append(Type(TypeInner.Vector(VectorSize.Bi, vec2)))
        
        val validator = Validator()
        val errors = validator.validate(module)
        errors.shouldNotBeEmpty()
    }
})
