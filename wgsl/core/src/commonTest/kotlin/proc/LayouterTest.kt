package io.ygdrasil.wgsl.proc

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

class LayouterTest : FunSpec({

    test("Scalar layout") {
        val module = Module()
        val layouter = Layouter()
        
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        layouter.update(module)
        
        val layout = layouter[f32]
        layout.size shouldBe 4
        layout.alignment.value shouldBe 4
    }

    test("Vector layout") {
        val module = Module()
        val layouter = Layouter()
        
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val vec3 = module.types.append(Type(TypeInner.Vector(VectorSize.Tri, f32)))
        val vec4 = module.types.append(Type(TypeInner.Vector(VectorSize.Quad, f32)))
        
        layouter.update(module)
        
        val layout3 = layouter[vec3]
        layout3.size shouldBe 12
        layout3.alignment.value shouldBe 16 // vec3 alignment is 16
        
        val layout4 = layouter[vec4]
        layout4.size shouldBe 16
        layout4.alignment.value shouldBe 16
    }

    test("Struct layout") {
        val module = Module()
        val layouter = Layouter()
        
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val vec3 = module.types.append(Type(TypeInner.Vector(VectorSize.Tri, f32)))
        
        // struct S { a: f32, b: vec3<f32> }
        val struct = module.types.append(Type(TypeInner.Struct(listOf(
            StructMember("a", f32, offset = 0),
            StructMember("b", vec3, offset = 16)
        ))))
        
        layouter.update(module)
        
        val layout = layouter[struct]
        // alignment should be max(4, 16) = 16
        layout.alignment.value shouldBe 16
        // size: a(4) -> alignment(16) -> b(12) = 28 -> roundUp(16) = 32
        layout.size shouldBe 32
    }
})
