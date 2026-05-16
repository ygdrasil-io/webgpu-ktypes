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
    protected var currentFunction: Function? = null

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
        currentFunction = func
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
        currentFunction = null
    }

    protected abstract fun writeFunctionSignature(func: Function, name: String)

    protected open fun writeEntryPoints() {
        module.entryPoints.forEachIndexed { index, ep ->
            writeEntryPoint(ep, index)
        }
    }

    protected abstract fun writeEntryPoint(ep: EntryPoint, index: Int)

    protected open fun writeBlock(handle: Handle<io.ygdrasil.wgsl.ir.Block>) {
        val blocks = currentFunction?.blocks ?: return
        val block = blocks[handle]
        block.statements.forEach { writeStatement(it) }
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun writeStatement(stmt: Statement) {
        when (stmt) {
            is Statement.Nop -> {}
            is Statement.Block -> {
                writeLine("{")
                indent { writeBlock(stmt.block as Handle<io.ygdrasil.wgsl.ir.Block>) }
                writeLine("}")
            }
            is Statement.Declare -> {
                val variable = currentFunction!!.localVariables[stmt.variable]
                val name = getLocalVariableName(stmt.variable)
                val typeName = getTypeName(variable.type)
                writeLine("$typeName $name;")
            }
            is Statement.Init -> {
                val variable = currentFunction!!.localVariables[stmt.variable]
                val name = getLocalVariableName(stmt.variable)
                val typeName = getTypeName(variable.type)
                val init = variable.init?.let { writeExpression(it) } ?: "/* error: no init */"
                writeLine("$typeName $name = $init;")
            }
            is Statement.Assign -> {
                val pointer = writeExpression(stmt.pointer)
                val value = writeExpression(stmt.value)
                writeLine("$pointer = $value;")
            }
            is Statement.Emit -> {
                // In some backends, Emit might be a no-op or might trigger something
                // For now, we skip it as it's IR metadata for expression ranges
            }
            is Statement.If -> {
                val cond = writeExpression(stmt.condition)
                writeLine("if ($cond) {")
                indent { writeBlock(stmt.accept as Handle<io.ygdrasil.wgsl.ir.Block>) }
                if (stmt.reject != null) {
                    writeLine("} else {")
                    indent { writeBlock(stmt.reject as Handle<io.ygdrasil.wgsl.ir.Block>) }
                }
                writeLine("}")
            }
            is Statement.Switch -> {
                val selector = writeExpression(stmt.selector)
                writeLine("switch ($selector) {")
                indent {
                    stmt.cases.forEach { case ->
                        case.selector.let { sel ->
                            when (sel) {
                                is CaseSelector.Value -> writeLine("case ${writeScalarValue(sel.value)}:")
                                is CaseSelector.Default -> writeLine("default:")
                            }
                        }
                        indent {
                            writeBlock(case.body)
                            writeLine("break;")
                        }
                    }
                }
                writeLine("}")
            }
            is Statement.Loop -> {
                writeLine("while (true) {")
                indent {
                    writeBlock(stmt.body as Handle<io.ygdrasil.wgsl.ir.Block>)
                    if (stmt.continuing != null) {
                        // Continuing block is tricky in C-like languages
                        // For now we just write it at the end
                        writeBlock(stmt.continuing as Handle<io.ygdrasil.wgsl.ir.Block>)
                    }
                }
                writeLine("}")
            }
            is Statement.Return -> {
                val value = stmt.value?.let { " ${writeExpression(it)}" } ?: ""
                writeLine("return$value;")
            }
            is Statement.Break -> writeLine("break;")
            is Statement.Continue -> writeLine("continue;")
            is Statement.Kill -> writeLine("kill;")
            is Statement.Discard -> writeLine("discard;")
        }
    }

    // Expressions
    protected open fun writeExpression(handle: Handle<Expression>): String {
        val expr = if (currentFunction != null) {
            currentFunction!!.expressions[handle]
        } else {
            module.globalExpressions[handle]
        }

        return when (val kind = expr.kind) {
            is ExpressionKind.Literal -> writeLiteralValue(kind.value)
            is ExpressionKind.GlobalVar -> getGlobalVariableName(kind.handle)
            is ExpressionKind.LocalVar -> getLocalVariableName(kind.handle)
            is ExpressionKind.FunctionArgument -> getFunctionArgumentName(kind.index)
            is ExpressionKind.ConstantExpr -> getConstantName(kind.handle)
            is ExpressionKind.Binary -> {
                val left = writeExpression(kind.left)
                val right = writeExpression(kind.right)
                "($left ${getBinaryOperator(kind.operator)} $right)"
            }
            is ExpressionKind.Unary -> {
                val e = writeExpression(kind.expr)
                "${getUnaryOperator(kind.operator)}($e)"
            }
            is ExpressionKind.Call -> {
                val name = getFunctionName(kind.function)
                val args = kind.arguments.joinToString { writeExpression(it) }
                "$name($args)"
            }
            is ExpressionKind.BuiltinCall -> {
                val args = kind.arguments.joinToString { writeExpression(it) }
                "${getBuiltinFunctionName(kind.function)}($args)"
            }
            is ExpressionKind.AccessIndex -> {
                val e = writeExpression(kind.expr)
                "$e[${kind.index}]"
            }
            is ExpressionKind.Access -> {
                val e = writeExpression(kind.expr)
                val type = getExpressionType(kind.expr)
                if (type.inner is TypeInner.Struct) {
                    val member = type.inner.members[kind.index]
                    "$e.${member.name}"
                } else {
                    "$e[${kind.index}]"
                }
            }
            is ExpressionKind.Swizzle -> {
                val e = writeExpression(kind.vector)
                val components = kind.pattern.joinToString("") { "xyzw"[it].toString() }
                "$e.$components"
            }
            is ExpressionKind.Splat -> {
                val e = writeExpression(kind.value)
                "/* splat */ $e"
            }
            is ExpressionKind.Load -> writeExpression(kind.pointer)
            is ExpressionKind.Store -> {
                val p = writeExpression(kind.pointer)
                val v = writeExpression(kind.value)
                "($p = $v)"
            }
            else -> "/* unsupported expression: ${kind::class.simpleName} */"
        }
    }

    protected open fun getExpressionType(handle: Handle<Expression>): Type {
        // This is a simplification, we should use Typifier
        return Type(TypeInner.Error)
    }

    protected open fun getBuiltinFunctionName(function: BuiltinFunction): String = function.name.lowercase()

    protected open fun getBinaryOperator(op: BinaryOperator): String = when (op) {
        BinaryOperator.Add -> "+"
        BinaryOperator.Subtract -> "-"
        BinaryOperator.Multiply -> "*"
        BinaryOperator.Divide -> "/"
        BinaryOperator.Modulo -> "%"
        BinaryOperator.Equal -> "=="
        BinaryOperator.NotEqual -> "!="
        BinaryOperator.Less -> "<"
        BinaryOperator.LessOrEqual -> "<="
        BinaryOperator.Greater -> ">"
        BinaryOperator.GreaterOrEqual -> ">="
        BinaryOperator.BitAnd -> "&"
        BinaryOperator.BitOr -> "|"
        BinaryOperator.BitXor -> "^"
        BinaryOperator.LogicalAnd -> "&&"
        BinaryOperator.LogicalOr -> "||"
        BinaryOperator.ShiftLeft -> "<<"
        BinaryOperator.ShiftRight -> ">>"
    }

    protected open fun getUnaryOperator(op: UnaryOperator): String = when (op) {
        UnaryOperator.Negate -> "-"
        UnaryOperator.Not -> "!"
        UnaryOperator.BitNot -> "~"
    }

    protected open fun writeLiteralValue(value: LiteralValue): String {
        return when (value) {
            is LiteralValue.Scalar -> writeScalarValue(value.value)
            is LiteralValue.Vector -> "vec(${value.components.joinToString { writeScalarValue(it) }})"
            is LiteralValue.Matrix -> "mat(...)"
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

    protected open fun getFunctionArgumentName(index: Int): String {
        return currentFunction?.parameters?.getOrNull(index)?.name ?: "arg_$index"
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

    protected fun write(text: String) {
        output.append(text)
    }
}
