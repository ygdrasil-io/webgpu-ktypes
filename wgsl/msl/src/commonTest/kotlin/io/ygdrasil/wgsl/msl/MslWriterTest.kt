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

    @Test
    fun testIntrinsicFunctions() {
        val module = Module()
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        
        val expressions = Arena<Expression>()
        val arg0 = expressions.append(Expression(ExpressionKind.FunctionArgument(0)))
        val sinCall = expressions.append(Expression(ExpressionKind.BuiltinCall(BuiltinFunction.Sin, listOf(arg0))))
        val logCall = expressions.append(Expression(ExpressionKind.BuiltinCall(BuiltinFunction.Ln, listOf(arg0))))
        
        val blocks = Arena<io.ygdrasil.wgsl.ir.Block>()
        val body = blocks.append(io.ygdrasil.wgsl.ir.Block(listOf(
            Statement.Return(sinCall),
            Statement.Return(logCall)
        )))
        
        module.functions.append(
            Function(
                name = "math_test",
                parameters = listOf(FunctionParameter("x", f32)),
                returnType = f32,
                localVariables = Arena(),
                expressions = expressions,
                blocks = blocks,
                body = body
            )
        )
        
        val code = MslModule.writeString(module)
        assertTrue(code.contains("sin(x)"))
        assertTrue(code.contains("log(x)")) // log because Ln is mapped to log
    }

    @Test
    fun testEntryPointWithBindings() {
        val module = Module()
        
        // Types
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val vec4f = module.types.append(Type(TypeInner.Vector(VectorSize.Quad, f32)))
        val pointer = module.types.append(Type(TypeInner.Pointer(f32, AddressSpace.Uniform)))
        
        // Global variable with binding
        module.globalVariables.append(
            GlobalVariable(
                name = "u_buffer",
                storageClass = StorageClass.Uniform,
                type = pointer,
                binding = Binding(group = 0, index = 1)
            )
        )
        
        // Function for entry point
        val expressions = Arena<Expression>()
        val blocks = Arena<io.ygdrasil.wgsl.ir.Block>()
        val body = blocks.append(io.ygdrasil.wgsl.ir.Block(emptyList()))
        
        val funcHandle = module.functions.append(
            Function(
                name = "vs_main_func",
                parameters = emptyList(),
                returnType = vec4f,
                localVariables = Arena(),
                expressions = expressions,
                blocks = blocks,
                body = body
            )
        )
        
        // Entry point
        val ep = EntryPoint(
            name = "vs_main",
            function = funcHandle,
            stage = ShaderStage.Vertex,
            bindings = listOf(
                BindingAttribute.Builtin(BuiltinValue.VertexIndex),
                BindingAttribute.Location(0)
            )
        )
        
        val moduleWithEp = module.copy(entryPoints = listOf(ep))
        
        val code = MslModule.writeString(moduleWithEp)
        
        assertTrue(code.contains("[[vertex]]"))
        assertTrue(code.contains("float4 vs_main("))
        assertTrue(code.contains("uint vertex_index [[vertex_id]]"))
        assertTrue(code.contains("float4 loc_0 [[attribute(0)]]"))
        assertTrue(code.contains("constant float* global_0 [[buffer(1)]]"))
    }

    @Test
    fun testExpressions() {
        val module = Module()
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val bool = module.types.append(Type(TypeInner.Scalar(ScalarKind.Bool, 1)))
        
        val expressions = Arena<Expression>()
        val f1 = expressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.F32(1.0f)))))
        val f2 = expressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.F32(2.0f)))))
        val b = expressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.Bool(true)))))
        
        val select = expressions.append(Expression(ExpressionKind.Select(b, f1, f2)))
        val splat = expressions.append(Expression(ExpressionKind.Splat(VectorSize.Tri, f1)))
        val cast = expressions.append(Expression(ExpressionKind.As(f1, bool)))
        
        val blocks = Arena<io.ygdrasil.wgsl.ir.Block>()
        val body = blocks.append(io.ygdrasil.wgsl.ir.Block(listOf(
            Statement.Return(select),
            Statement.Return(splat),
            Statement.Return(cast)
        )))
        
        module.functions.append(
            Function(
                name = "expr_test",
                parameters = emptyList(),
                returnType = f32,
                localVariables = Arena(),
                expressions = expressions,
                blocks = blocks,
                body = body
            )
        )
        
        val code = MslModule.writeString(module)
        assertTrue(code.contains("(true ? 1.0f : 2.0f)"))
        assertTrue(code.contains("float3(1.0f)"))
        assertTrue(code.contains("(bool)(1.0f)"))
    }
}
