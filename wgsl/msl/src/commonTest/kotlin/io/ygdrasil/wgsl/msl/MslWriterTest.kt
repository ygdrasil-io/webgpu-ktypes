package io.ygdrasil.wgsl.msl

import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.valid.ModuleInfo
import kotlin.test.Test
import kotlin.test.assertTrue

class MslWriterTest {

    @Test
    fun testEmptyModule() {
        val module = Module()
        val code = MslModule.writeString(module)
        assertTrue(code.contains("#include <metal_stdlib>"))
        assertTrue(code.contains("using namespace metal;"))
    }
}
