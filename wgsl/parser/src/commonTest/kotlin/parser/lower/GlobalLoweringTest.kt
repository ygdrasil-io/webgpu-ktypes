package io.ygdrasil.wgsl.parser.lower

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ygdrasil.wgsl.ir.StorageClass
import io.ygdrasil.wgsl.parser.TestUtils.lowerWgsl

class GlobalLoweringTest : FunSpec({
    test("T021: should lower private global variable") {
        val module = lowerWgsl("var<private> global: i32 = 0;")
        
        module.globalVariables.toList() shouldHaveSize 1
        val globalVar = module.globalVariables.toList()[0]
        
        globalVar.name shouldBe "global"
        globalVar.storageClass shouldBe StorageClass.Private
    }

    test("T022: should lower uniform global variable") {
        val module = lowerWgsl("""
            @group(0) @binding(0)
            var<uniform> uniforms: i32;
        """)
        
        module.globalVariables.toList() shouldHaveSize 1
        val globalVar = module.globalVariables.toList()[0]
        
        globalVar.name shouldBe "uniforms"
        globalVar.storageClass shouldBe StorageClass.Uniform
    }
})
