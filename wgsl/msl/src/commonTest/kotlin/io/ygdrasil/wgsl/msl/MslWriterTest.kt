package io.ygdrasil.wgsl.msl

import io.ygdrasil.wgsl.arena.Arena
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.ir.Function
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

    @Test
    fun testModuleWithStructAndFunction() {
        val module = Module()
        
        // Add a scalar type
        val f32Handle = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        
        // Add a struct
        module.types.append(
            Type(TypeInner.Struct(listOf(
                StructMember("a", f32Handle, null, 0)
            )))
        )
        
        // Add a function
        val expressions = Arena<Expression>()
        val blocks = Arena<io.ygdrasil.wgsl.ir.Block>()
        val body = blocks.append(io.ygdrasil.wgsl.ir.Block(emptyList()))
        
        module.functions.append(
            Function(
                name = "my_func",
                parameters = emptyList(),
                returnType = null,
                localVariables = Arena(),
                expressions = expressions,
                blocks = blocks,
                body = body
            )
        )
        
        val code = MslModule.writeString(module)
        assertTrue(code.contains("struct Struct_1")) // Struct is index 1 because f32 is index 0
        assertTrue(code.contains("float a;"))
        assertTrue(code.contains("void my_func()"))
    }
}
