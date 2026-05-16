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
        
        val moduleWithEp = module.copy(entryPoints = mutableListOf(ep))
        
        val code = MslModule.writeString(moduleWithEp)
        
        assertTrue(code.contains("[[vertex]]"))
        assertTrue(code.contains("vs_main_Output vs_main("))
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

    @Test
    fun testTexturesAndSamplers() {
        val module = Module()
        val textureType = module.types.append(Type(TypeInner.Opaque("texture2d")))
        val samplerType = module.types.append(Type(TypeInner.Opaque("sampler")))
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val vec2f = module.types.append(Type(TypeInner.Vector(VectorSize.Bi, f32)))
        
        val texHandle = module.globalVariables.append(GlobalVariable(
            name = "myTexture",
            storageClass = StorageClass.Handle,
            type = textureType,
            binding = Binding(0, 0)
        ))
        
        val sampHandle = module.globalVariables.append(GlobalVariable(
            name = "mySampler",
            storageClass = StorageClass.Handle,
            type = samplerType,
            binding = Binding(0, 1)
        ))
        
        val expressions = Arena<Expression>()
        val texExpr = expressions.append(Expression(ExpressionKind.GlobalVar(texHandle)))
        val sampExpr = expressions.append(Expression(ExpressionKind.GlobalVar(sampHandle)))
        val coordExpr = expressions.append(Expression(ExpressionKind.Literal(LiteralValue.Vector(listOf(ScalarValue.F32(0.5f), ScalarValue.F32(0.5f))))))
        
        val sampleExpr = expressions.append(Expression(ExpressionKind.Sample(
            texture = texExpr,
            sampler = sampExpr,
            coordinate = coordExpr
        )))
        
        val queryExpr = expressions.append(Expression(ExpressionKind.TextureQuery(
            texture = texExpr,
            query = TextureQueryKind.Size
        )))
        
        val blocks = Arena<io.ygdrasil.wgsl.ir.Block>()
        val body = blocks.append(io.ygdrasil.wgsl.ir.Block(listOf(
            Statement.Return(sampleExpr),
            Statement.Return(queryExpr)
        )))
        
        val funcHandle = module.functions.append(
            Function(
                name = "tex_test",
                parameters = emptyList(),
                returnType = f32,
                localVariables = Arena(),
                expressions = expressions,
                blocks = blocks,
                body = body
            )
        )
        
        module.entryPoints.add(EntryPoint(
            name = "fs_main",
            stage = ShaderStage.Fragment,
            function = funcHandle,
            bindings = emptyList()
        ))
        
        val code = MslModule.writeString(module)
        assertTrue(code.contains("texture2d<float> global_0 [[texture(0)]]"))
        assertTrue(code.contains("sampler global_1 [[sampler(1)]]"))
        assertTrue(code.contains("global_0.sample(global_1, float2(0.5f, 0.5f))"))
        assertTrue(code.contains("global_0.get_width(), global_0.get_height()"))
    }

    @Test
    fun testAtomicsAndRelational() {
        val module = Module()
        val u32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.Uint, 4)))
        val ptrU32 = module.types.append(Type(TypeInner.Pointer(u32, AddressSpace.Storage)))
        
        val expressions = Arena<Expression>()
        val ptrExpr = expressions.append(Expression(ExpressionKind.FunctionArgument(0)))
        val valExpr = expressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.U32(10)))))
        
        val atomicExpr = expressions.append(Expression(ExpressionKind.Atomic(
            pointer = ptrExpr,
            fun_ = AtomicFunction.Add,
            arguments = listOf(valExpr)
        )))
        
        val boolExpr = expressions.append(Expression(ExpressionKind.Literal(LiteralValue.Scalar(ScalarValue.Bool(true)))))
        val relationalExpr = expressions.append(Expression(ExpressionKind.Relational(
            fun_ = RelationalFunction.Any,
            arguments = listOf(boolExpr)
        )))
        
        val blocks = Arena<io.ygdrasil.wgsl.ir.Block>()
        val body = blocks.append(io.ygdrasil.wgsl.ir.Block(listOf(
            Statement.Return(atomicExpr),
            Statement.Return(relationalExpr)
        )))
        
        module.functions.append(
            Function(
                name = "atomic_test",
                parameters = listOf(FunctionParameter("p", ptrU32)),
                returnType = u32,
                localVariables = Arena(),
                expressions = expressions,
                blocks = blocks,
                body = body
            )
        )
        
        val code = MslModule.writeString(module)
        assertTrue(code.contains("atomic_fetch_add_explicit(p, 10u, memory_order_relaxed)"))
        assertTrue(code.contains("any(true)"))
    }

    @Test
    fun testEntryPointWithStructs() {
        val module = Module()
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val vec4f = module.types.append(Type(TypeInner.Vector(VectorSize.Quad, f32)))
        
        val expressions = Arena<Expression>()
        val posExpr = expressions.append(Expression(ExpressionKind.Literal(LiteralValue.Vector(listOf(
            ScalarValue.F32(0.0f), ScalarValue.F32(0.0f), ScalarValue.F32(0.0f), ScalarValue.F32(1.0f)
        )))))
        
        val blocks = Arena<io.ygdrasil.wgsl.ir.Block>()
        val body = blocks.append(io.ygdrasil.wgsl.ir.Block(listOf(
            Statement.Return(posExpr)
        )))
        
        val funcHandle = module.functions.append(
            Function(
                name = "vs_main_impl",
                parameters = emptyList(),
                returnType = vec4f,
                localVariables = Arena(),
                expressions = expressions,
                blocks = blocks,
                body = body
            )
        )
        
        module.entryPoints.add(EntryPoint(
            name = "vs_main",
            stage = ShaderStage.Vertex,
            function = funcHandle,
            bindings = listOf(
                BindingAttribute.Location(0),
                BindingAttribute.Builtin(BuiltinValue.VertexIndex)
            )
        ))
        
        val code = MslModule.writeString(module)
        assertTrue(code.contains("struct vs_main_Input {"))
        assertTrue(code.contains("float4 loc_0 [[attribute(0)]];"))
        assertTrue(code.contains("struct vs_main_Output {"))
        assertTrue(code.contains("float4 position [[position]];"))
        assertTrue(code.contains("vs_main_Output vs_main("))
        assertTrue(code.contains("vs_main_Input in [[stage_in]]"))
        assertTrue(code.contains("uint vertex_index [[vertex_id]]"))
    }

    @Test
    fun testStructPadding() {
        val module = Module()
        val f32 = module.types.append(Type(TypeInner.Scalar(ScalarKind.F32, 4)))
        val vec3f = module.types.append(Type(TypeInner.Vector(VectorSize.Tri, f32)))
        
        // Struct with float and then float3 (float3 has alignment of 16)
        module.types.append(
            Type(TypeInner.Struct(listOf(
                StructMember("a", f32, null, 0),
                StructMember("b", vec3f, null, 16)
            )))
        )
        
        val code = MslModule.writeString(module)
        assertTrue(code.contains("float a;"))
        assertTrue(code.contains("char _pad4[12];")) // 4 to 16
        assertTrue(code.contains("float3 b;"))
    }
}
