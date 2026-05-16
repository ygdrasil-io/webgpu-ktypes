package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.ir.Function
import io.ygdrasil.wgsl.proc.Layouter
import io.ygdrasil.wgsl.proc.Namer
import io.ygdrasil.wgsl.valid.ModuleInfo

/**
 * Classe de base pour tous les writers de backend.
 */
abstract class WriterBase<T : BackendOptions>(
    protected val output: StringBuilder,
    protected val module: Module,
    protected val moduleInfo: ModuleInfo,
    protected val options: T,
    protected val namer: Namer,
    protected val layouter: Layouter
) {

    protected var indentLevel: Int = 0

    /**
     * Génère le code complet pour le module.
     */
    open fun write(): String {
        output.clear()
        indentLevel = 0

        writeHeader()
        writePreamble()
        writeTypes()
        writeConstants()
        writeGlobalVariables()
        writeFunctions()
        writeEntryPoints()

        return output.toString()
    }

    protected abstract fun writeHeader()

    protected open fun writePreamble() {}

    protected open fun writeTypes() {
        module.types.forEachWithHandle { handle, type ->
            if (type.inner is TypeInner.Struct) {
                writeStructType(handle, type.inner, getTypeName(handle))
            }
        }
    }

    protected abstract fun writeStructType(
        handle: Handle<Type>,
        structInner: TypeInner.Struct,
        name: String
    )

    protected open fun writeConstants() {
        module.constants.forEachWithHandle { handle, constant ->
            val name = getConstantName(handle)
            val typeName = getTypeName(constant.type)
            val init = writeConstantInner(constant.inner)
            writeLine("const $typeName $name = $init;")
        }
    }

    protected open fun writeGlobalVariables() {
        module.globalVariables.forEachWithHandle { handle, variable ->
            val name = getGlobalVariableName(handle)
            val typeName = getTypeName(variable.type)
            val init = variable.init?.let { " = ${writeExpression(it)}" } ?: ""
            writeLine("$typeName $name$init;")
        }
    }

    protected open fun writeFunctions() {
        module.functions.forEachWithHandle { handle, func ->
            writeFunction(func, handle)
        }
    }

    protected open fun writeFunction(func: Function, handle: Handle<Function>) {
        val name = getFunctionName(handle)
        writeLine()
        writeFunctionSignature(func, name)
        writeLine(" {")
        indent {
            func.localVariables.forEachWithHandle { vHandle, variable ->
                val varName = getLocalVariableName(vHandle)
                val typeName = getTypeName(variable.type)
                val init = variable.init?.let { " = ${writeExpression(it)}" } ?: ""
                writeLine("$typeName $varName$init;")
            }
            writeLine()
            writeBlock(func.body)
        }
        writeLine("}")
    }

    protected abstract fun writeFunctionSignature(func: Function, name: String)

    protected open fun writeEntryPoints() {
        module.entryPoints.forEachIndexed { index, ep ->
            writeEntryPoint(ep, index)
        }
    }

    protected abstract fun writeEntryPoint(ep: EntryPoint, index: Int)

    protected open fun writeBlock(handle: Handle<Block>) {
        val block = module.functions.firstOrNull { it.body == handle }?.body // This is hacky, need better block access
        // Actually Block is accessible via module.functions or ep.function
        // But the IR stores Block in Handle<Block> which is not in an Arena in Module.
        // Wait, where are Blocks stored?
        // In Function.kt: val body: Handle<Block>
        // But there's no Arena<Block> in Module.
        // Let's check Module.kt again.
    }

    // Expressions
    protected open fun writeExpression(handle: Handle<Expression>): String {
        // We need to know which Arena to use (global or function local)
        // This suggests WriterBase needs more context about current function
        return "/* expression ${handle.index} */"
    }

    protected open fun writeConstantInner(inner: ConstantInner): String {
        return when (inner) {
            is ConstantInner.Scalar -> writeScalarValue(inner.value)
            is ConstantInner.Vector -> "vec(${inner.components.joinToString { writeScalarValue(it) }})"
            is ConstantInner.Matrix -> "mat(...)"
            is ConstantInner.Zero -> "/* zero */"
            is ConstantInner.Composite -> "/* composite */"
            is ConstantInner.Expression -> writeExpression(inner.expr)
        }
    }

    protected open fun writeScalarValue(value: ScalarValue): String {
        return when (value) {
            is ScalarValue.Bool -> value.value.toString()
            is ScalarValue.I8 -> value.value.toString()
            is ScalarValue.U8 -> "${value.value}u"
            is ScalarValue.I16 -> value.value.toString()
            is ScalarValue.U16 -> "${value.value}u"
            is ScalarValue.I32 -> value.value.toString()
            is ScalarValue.U32 -> "${value.value}u"
            is ScalarValue.I64 -> value.value.toString()
            is ScalarValue.U64 -> "${value.value}u"
            is ScalarValue.F16 -> "${value.value}h"
            is ScalarValue.F32 -> {
                val s = value.value.toString()
                if ("." in s) "${s}f" else "${s}.0f"
            }
            is ScalarValue.F64 -> {
                val s = value.value.toString()
                if ("." in s) s else "$s.0"
            }
            is ScalarValue.AbstractInt -> value.value.toString()
            is ScalarValue.AbstractFloat -> value.value.toString()
        }
    }

    // Utility methods for naming (to be refined)
    protected open fun getTypeName(handle: Handle<Type>): String {
        val type = module.types[handle]
        return when (val inner = type.inner) {
            is TypeInner.Scalar -> getScalarTypeName(inner)
            is TypeInner.Vector -> "vec${inner.size.ordinal + 2}<${getScalarTypeName(module.types[inner.scalar].inner as TypeInner.Scalar)}>"
            is TypeInner.Struct -> "Struct_${handle.index}"
            else -> "Type_${handle.index}"
        }
    }

    protected open fun getScalarTypeName(scalar: TypeInner.Scalar): String {
        return when (scalar.kind) {
            ScalarKind.Bool -> "bool"
            ScalarKind.Sint -> "i${scalar.width * 8}"
            ScalarKind.Uint -> "u${scalar.width * 8}"
            ScalarKind.F32 -> "f32"
            ScalarKind.F16 -> "f16"
            ScalarKind.F64 -> "f64"
            else -> "unknown"
        }
    }

    protected open fun getConstantName(handle: Handle<Constant>): String = "CONST_${handle.index}"
    protected open fun getGlobalVariableName(handle: Handle<GlobalVariable>): String = "global_${handle.index}"
    protected open fun getFunctionName(handle: Handle<Function>): String = module.functions[handle].name
    protected open fun getLocalVariableName(handle: Handle<LocalVariable>): String = "local_${handle.index}"

    protected fun indent(block: () -> Unit) {
        indentLevel++
        block()
        indentLevel--
    }

    protected fun writeLine(line: String = "") {
        repeat(indentLevel) { output.append(options.indent) }
        output.append(line).append(options.newline)
    }

    protected fun write(text: String) {
        output.append(text)
    }
}
