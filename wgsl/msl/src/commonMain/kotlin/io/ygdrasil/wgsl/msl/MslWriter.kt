package io.ygdrasil.wgsl.msl

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.back.MslOptions
import io.ygdrasil.wgsl.back.WriterBase
import io.ygdrasil.wgsl.back.BackendWriter
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.ir.Function
import io.ygdrasil.wgsl.proc.Layouter
import io.ygdrasil.wgsl.proc.Namer
import io.ygdrasil.wgsl.valid.ModuleInfo

class MslWriter(
    output: StringBuilder,
    module: Module,
    moduleInfo: ModuleInfo,
    options: MslOptions,
    namer: Namer,
    layouter: Layouter
) : WriterBase<MslOptions>(output, module, moduleInfo, options, namer, layouter), BackendWriter<MslOptions> {

    override fun write(module: Module, moduleInfo: ModuleInfo): String {
        return write()
    }

    override fun withOptions(options: MslOptions): BackendWriter<MslOptions> {
        return MslWriter(StringBuilder(), module, moduleInfo, options, namer, layouter)
    }

    override fun canHandle(module: Module, moduleInfo: ModuleInfo): Boolean = true

    override fun writeHeader() {
        writeLine("#include <metal_stdlib>")
        writeLine("using namespace metal;")
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
            // MSL attributes for params would go here
        }
        write(")")
    }

    override fun writeEntryPoint(ep: EntryPoint, index: Int) {
        val stageAttr = when (ep.stage) {
            ShaderStage.Vertex -> "[[vertex]]"
            ShaderStage.Fragment -> "[[fragment]]"
            ShaderStage.Compute -> "[[kernel]]"
        }
        writeLine()
        writeLine("$stageAttr")
        val func = module.functions[ep.function]
        writeFunctionSignature(func, ep.name)
        writeLine(" {")
        indent {
            writeBlock(func.body)
        }
        writeLine("}")
    }

    override fun getScalarTypeName(scalar: TypeInner.Scalar): String {
        return when (scalar.kind) {
            ScalarKind.Bool -> "bool"
            ScalarKind.Sint -> if (scalar.width == 4) "int" else "char" // simplification
            ScalarKind.Uint -> if (scalar.width == 4) "uint" else "uchar"
            ScalarKind.F32 -> "float"
            ScalarKind.F16 -> "half"
            ScalarKind.F64 -> "double"
            else -> "/* unknown scalar */ void"
        }
    }

    override fun getTypeName(handle: Handle<Type>): String {
        val type = module.types[handle]
        return when (val inner = type.inner) {
            is TypeInner.Scalar -> getScalarTypeName(inner)
            is TypeInner.Vector -> {
                val scalarName = getScalarTypeName(module.types[inner.scalar].inner as TypeInner.Scalar)
                "$scalarName${inner.size.ordinal + 2}"
            }
            is TypeInner.Matrix -> {
                val scalarName = getScalarTypeName(module.types[inner.scalar].inner as TypeInner.Scalar)
                "$scalarName${inner.columns.ordinal + 2}x${inner.rows.ordinal + 2}"
            }
            is TypeInner.Struct -> "Struct_${handle.index}"
            is TypeInner.Pointer -> {
                val baseName = getTypeName(inner.base)
                val spaceName = when (inner.addressSpace) {
                    AddressSpace.Uniform -> "constant"
                    AddressSpace.Storage -> "device"
                    else -> "thread"
                }
                "$spaceName $baseName*"
            }
            else -> "/* unknown type */ void"
        }
    }
}
