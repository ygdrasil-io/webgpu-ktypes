package io.ygdrasil.wgsl.glsl

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.back.GlslOptions
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
        write(")")
    }

    override fun writeEntryPoint(ep: EntryPoint, index: Int) {
        writeLine("// Entry point: ${ep.name}")
    }
}
