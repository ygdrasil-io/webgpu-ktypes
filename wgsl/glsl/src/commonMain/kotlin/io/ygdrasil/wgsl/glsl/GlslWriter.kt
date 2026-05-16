package io.ygdrasil.wgsl.glsl

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.back.GlslOptions
import io.ygdrasil.wgsl.back.BindingMap
import io.ygdrasil.wgsl.back.WriterBase
import io.ygdrasil.wgsl.back.BackendWriter
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.ir.Function
import io.ygdrasil.wgsl.proc.Layouter
import io.ygdrasil.wgsl.proc.Namer
import io.ygdrasil.wgsl.valid.ModuleInfo

class GlslWriter(
    output: StringBuilder,
    module: Module,
    moduleInfo: ModuleInfo,
    options: GlslOptions,
    namer: Namer,
    layouter: Layouter
) : WriterBase<GlslOptions>(output, module, moduleInfo, options, namer, layouter), BackendWriter<GlslOptions> {

    override fun write(module: Module, moduleInfo: ModuleInfo): String {
        return GlslWriter(StringBuilder(), module, moduleInfo, options, namer, layouter).write()
    }

    override fun withOptions(options: GlslOptions): BackendWriter<GlslOptions> {
        return GlslWriter(StringBuilder(), module, moduleInfo, options, namer, layouter)
    }

    override fun canHandle(module: Module, moduleInfo: ModuleInfo): Boolean = true

    override fun writeHeader() {
        writeLine("#version 450 core")
        writeLine("#extension GL_ARB_separate_shader_objects : enable")
    }

    override fun writePreamble() {
        writeLine("precision highp float;")
        writeLine("precision highp int;")
        writeLine()
    }

    override fun writeStructType(handle: Handle<Type>, structInner: TypeInner.Struct, name: String) {
        writeLine("struct $name {")
        indent {
            for (member in structInner.members) {
                val memberName = member.name
                val typeName = getTypeName(member.type)
                writeLine("$typeName $memberName;")
            }
        }
        writeLine("};")
    }

    override fun writeFunctionSignature(func: Function, name: String) {
        val returnType = func.returnType?.let { getTypeName(it) } ?: "void"
        write("$returnType $name(")
        func.parameters.forEachIndexed { i, param ->
            if (i > 0) write(", ")
            val typeName = getTypeName(param.type)
            write("$typeName ${param.name}")
        }
        write(")")
    }

    override fun writeGlobalVariables() {
        module.globalVariables.forEachWithHandle { handle, variable ->
            val name = getGlobalVariableName(handle)
            val typeName = getTypeName(variable.type)
            val binding = variable.binding
            if (binding != null) {
                val target = options.bindingMap[binding]
                val layout = "layout(set = ${binding.group}, binding = ${target?.buffer ?: binding.index})"
                val storage = when (variable.storageClass) {
                    StorageClass.Uniform -> "uniform"
                    StorageClass.Storage -> "buffer"
                    else -> "uniform"
                }
                writeLine("$layout $storage $typeName $name;")
            } else {
                val storage = when (variable.storageClass) {
                    StorageClass.Private -> ""
                    StorageClass.Workgroup -> "shared"
                    else -> ""
                }
                val init = variable.init?.let { " = ${writeExpression(it)}" } ?: ""
                writeLine("$storage $typeName $name$init;")
            }
        }
    }

    override fun writeEntryPoint(ep: EntryPoint, index: Int) {
        writeLine()
        // GLSL entry point arguments are global variables with in/out qualifiers
        ep.bindings.forEach { attr ->
            when (attr) {
                is BindingAttribute.Location -> {
                    val typeName = "vec4" 
                    val name = "loc_${attr.location}"
                    // Vertex input, Fragment input/output
                    val qualifier = if (ep.stage == ShaderStage.Vertex) "in" else "in" // simplified
                    writeLine("layout(location = ${attr.location}) $qualifier $typeName $name;")
                }
                is BindingAttribute.Builtin -> {
                    // Built-ins like gl_VertexIndex are pre-defined in GLSL, 
                    // but we might need to map them if the IR uses custom names
                }
                else -> {}
            }
        }
        
        // Output for vertex shader (gl_Position)
        if (ep.stage == ShaderStage.Vertex) {
            // gl_Position is builtin
        }

        writeLine("void main() {")
        indent {
            val func = module.functions[ep.function]
            writeBlock(func.body)
        }
        writeLine("}")
    }

    override fun writeLiteralValue(value: LiteralValue): String {
        return when (value) {
            is LiteralValue.Scalar -> writeScalarValue(value.value)
            is LiteralValue.Vector -> {
                val first = value.components.first()
                val prefix = when (first) {
                    is ScalarValue.U32 -> "uvec"
                    is ScalarValue.I32 -> "ivec"
                    is ScalarValue.Bool -> "bvec"
                    is ScalarValue.F64 -> "dvec"
                    else -> "vec"
                }
                "$prefix${value.components.size}(${value.components.joinToString { writeScalarValue(it) }})"
            }
            is LiteralValue.Matrix -> "mat(...)"
        }
    }

    override fun writeSample(
        texture: String,
        sampler: String?,
        coordinate: String,
        level: SampleLevel?,
        depthRef: Handle<Expression>?
    ): String {
        val s = if (sampler != null) "sampler2D($texture, $sampler)" else texture
        return when (level) {
            is SampleLevel.Zero -> "textureLod($s, $coordinate, 0.0)"
            is SampleLevel.MIPMAP -> {
                val l = writeExpression(level.level)
                "textureLod($s, $coordinate, $l)"
            }
            is SampleLevel.AUTOMATIC -> "texture($s, $coordinate)"
            null -> {
                if (depthRef != null) {
                    val d = writeExpression(depthRef)
                    // GLSL texture takes coordinate with depth as last component for shadow samplers
                    "texture($s, vec3($coordinate, $d))"
                } else {
                    "texture($s, $coordinate)"
                }
            }
        }
    }

    override fun writeTextureQuery(texture: String, query: TextureQueryKind): String {
        val method = when (query) {
            TextureQueryKind.Size -> "textureSize($texture, 0)"
            TextureQueryKind.SizeLevel -> "textureSize($texture, level)" // TODO: level
            TextureQueryKind.NumLevels -> "textureQueryLevels($texture)"
            TextureQueryKind.NumLayers -> "textureQueryLevels($texture)" // Simplified
            TextureQueryKind.NumSamples -> "textureSamples($texture)"
        }
        return method
    }

    override fun getScalarTypeName(scalar: TypeInner.Scalar): String {
        return when (scalar.kind) {
            ScalarKind.Bool -> "bool"
            ScalarKind.Sint -> "int"
            ScalarKind.Uint -> "uint"
            ScalarKind.F32 -> "float"
            ScalarKind.F16 -> "float16_t"
            ScalarKind.F64 -> "double"
            else -> "void"
        }
    }

    override fun getTypeName(handle: Handle<Type>): String {
        val type = module.types[handle]
        return when (val inner = type.inner) {
            is TypeInner.Scalar -> getScalarTypeName(inner)
            is TypeInner.Vector -> {
                val scalarType = module.types[inner.scalar]
                val prefix = when ((scalarType.inner as TypeInner.Scalar).kind) {
                    ScalarKind.Uint -> "u"
                    ScalarKind.Sint -> "i"
                    ScalarKind.Bool -> "b"
                    ScalarKind.F64 -> "d"
                    else -> ""
                }
                "${prefix}vec${inner.size.ordinal + 2}"
            }
            is TypeInner.Matrix -> {
                val scalarType = module.types[inner.scalar]
                val prefix = if ((scalarType.inner as TypeInner.Scalar).kind == ScalarKind.F64) "d" else ""
                "${prefix}mat${inner.columns.ordinal + 2}x${inner.rows.ordinal + 2}"
            }
            is TypeInner.Struct -> "Struct_${handle.index}"
            is TypeInner.Pointer -> {
                val baseName = getTypeName(inner.base)
                // GLSL doesn't really have pointers like C, except for some extensions
                // In many cases, it's just the type
                baseName
            }
            is TypeInner.Opaque -> {
                when {
                    inner.name == "sampler" -> "sampler"
                    inner.name == "comparison_sampler" -> "samplerShadow"
                    inner.name == "texture_1d<f32>" -> "texture1D"
                    inner.name == "texture_2d<f32>" -> "texture2D"
                    inner.name == "texture_2d_array<f32>" -> "texture2DArray"
                    inner.name == "texture_3d<f32>" -> "texture3D"
                    inner.name == "texture_cube<f32>" -> "textureCube"
                    inner.name == "texture_cube_array<f32>" -> "textureCubeArray"
                    inner.name == "texture_multisampled_2d<f32>" -> "texture2DMS"
                    inner.name.startsWith("texture_depth") -> "texture2D"
                    else -> inner.name
                }
            }
            else -> "void"
        }
    }

    override fun writeBitcast(expr: String, targetType: Type): String {
        val inner = targetType.inner
        val func = when {
            inner is TypeInner.Scalar && inner.kind == ScalarKind.Uint -> "floatBitsToUint"
            inner is TypeInner.Scalar && inner.kind == ScalarKind.F32 -> "intBitsToFloat" // simplified
            inner is TypeInner.Scalar && inner.kind == ScalarKind.Sint -> "floatBitsToInt"
            else -> "bitfieldExtract" // fallback/error
        }
        return "$func($expr)"
    }

    override fun writeRelational(function: RelationalFunction, arguments: List<String>): String {
        return when (function) {
            RelationalFunction.Any -> "any(${arguments.joinToString()})"
            RelationalFunction.All -> "all(${arguments.joinToString()})"
            RelationalFunction.IsNan -> "isnan(${arguments.joinToString()})"
            RelationalFunction.IsInf -> "isinf(${arguments.joinToString()})"
            RelationalFunction.IsFinite -> "!isinf(${arguments.joinToString()}) && !isnan(${arguments.joinToString()})"
            RelationalFunction.IsNormal -> "/* isnormal unsupported */ true"
            RelationalFunction.SignBit -> "/* signbit unsupported */ false"
        }
    }

    override fun writeAtomic(pointer: String, function: AtomicFunction, arguments: List<String>): String {
        val glslFunc = when (function) {
            AtomicFunction.Add -> "atomicAdd"
            AtomicFunction.Subtract -> "atomicAdd" // use negative
            AtomicFunction.And -> "atomicAnd"
            AtomicFunction.Or -> "atomicOr"
            AtomicFunction.Xor -> "atomicXor"
            AtomicFunction.Min -> "atomicMin"
            AtomicFunction.Max -> "atomicMax"
            AtomicFunction.Exchange -> "atomicExchange"
            AtomicFunction.CompSwap -> "atomicCompSwap"
        }
        return "$glslFunc($pointer, ${arguments.joinToString()})"
    }

    override fun getBuiltinFunctionName(function: BuiltinFunction): String = when (function) {
        BuiltinFunction.Ln -> "log"
        BuiltinFunction.Atan2 -> "atan" // GLSL uses atan(y, x)
        BuiltinFunction.Mix -> "mix"
        BuiltinFunction.Fract -> "fract"
        BuiltinFunction.Reflect -> "reflect"
        BuiltinFunction.Refract -> "refract"
        BuiltinFunction.InverseSqrt -> "inversesqrt"
        BuiltinFunction.Log2 -> "log2"
        BuiltinFunction.Fma -> "fma"
        BuiltinFunction.Determinant -> "determinant"
        // Most others are same as WGSL lowercase
        else -> super.getBuiltinFunctionName(function)
    }
}
