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
        return write()
    }

    override fun withOptions(options: GlslOptions): BackendWriter<GlslOptions> {
        return GlslWriter(StringBuilder(), module, moduleInfo, options, namer, layouter)
    }

    override fun canHandle(module: Module, moduleInfo: ModuleInfo): Boolean = true

    override fun writeHeader() {
        writeLine("#version ${options.version}")
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
                    writeLine("layout(location = ${attr.location}) in $typeName $name;")
                }
                else -> {}
            }
        }

        writeLine("void main() {")
        indent {
            // Built-in mapping needs to be handled inside the body if they are used
            val func = module.functions[ep.function]
            writeBlock(func.body)
        }
        writeLine("}")
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
            else -> "void"
        }
    }

    override fun getBuiltinFunctionName(function: BuiltinFunction): String = when (function) {
        BuiltinFunction.Ln -> "log"
        else -> super.getBuiltinFunctionName(function)
    }
}
