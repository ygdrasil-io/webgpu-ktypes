package io.ygdrasil.wgsl.parser

import io.ygdrasil.wgsl.ast.*
import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.arena.Arena
import io.ygdrasil.wgsl.ir.ScalarKind as IrScalarKind
import io.ygdrasil.wgsl.ir.Block as IrBlock
import io.ygdrasil.wgsl.ir.Expression as IrExpression
import io.ygdrasil.wgsl.ir.Statement as IrStatement
import io.ygdrasil.wgsl.ir.Function as IrFunction
import io.ygdrasil.wgsl.ir.Type as IrType
import io.ygdrasil.wgsl.ir.GlobalVariable as IrGlobalVariable
import io.ygdrasil.wgsl.ir.LocalVariable as IrLocalVariable
import io.ygdrasil.wgsl.ir.Module as IrModule
import io.ygdrasil.wgsl.ir.TypeInner as IrTypeInner
import io.ygdrasil.wgsl.ir.ArraySize as IrArraySize
import io.ygdrasil.wgsl.ir.VectorSize as IrVectorSize
import io.ygdrasil.wgsl.ir.AddressSpace as IrAddressSpace
import io.ygdrasil.wgsl.ir.StorageClass as IrStorageClass
import io.ygdrasil.wgsl.ir.AccessMode as IrAccessMode
import io.ygdrasil.wgsl.ir.EntryPoint as IrEntryPoint
import io.ygdrasil.wgsl.ir.ShaderStage as IrShaderStage
import io.ygdrasil.wgsl.ir.FunctionParameter as IrFunctionParameter
import io.ygdrasil.wgsl.ir.StructMember as IrStructMember
import io.ygdrasil.wgsl.ir.ScalarValue as IrScalarValue
import io.ygdrasil.wgsl.ir.ExpressionKind as IrExpressionKind
import io.ygdrasil.wgsl.ir.LiteralValue as IrLiteralValue
import io.ygdrasil.wgsl.ir.BinaryOperator as IrBinaryOperator
import io.ygdrasil.wgsl.ir.UnaryOperator as IrUnaryOperator

/**
 * Lowers a resolved WGSL AST to the IR (Module).
 */
class Lowerer {

    private val module = IrModule()
    private val typeMap = mutableMapOf<TypeDecl, Handle<IrType>>()
    private val globalVarMap = mutableMapOf<String, Handle<IrGlobalVariable>>()
    private val functionMap = mutableMapOf<String, Handle<IrFunction>>()

    private var currentExpressions: Arena<IrExpression>? = null
    private var currentBlocks: Arena<IrBlock>? = null
    private var currentLocalVars: Arena<IrLocalVariable>? = null
    private val localVariablesMap = mutableMapOf<String, Handle<IrLocalVariable>>()
    private val functionParamsMap = mutableMapOf<String, Int>()

    fun lower(unit: TranslationUnit): IrModule {
        // 1. Lower structs and types
        for (decl in unit.declarations) {
            when (decl) {
                is StructDecl -> lowerStruct(decl)
                is TypeAliasDecl -> lowerTypeAlias(decl)
                else -> {}
            }
        }

        // 2. Lower global variables
        for (decl in unit.declarations) {
            if (decl is VariableDecl) {
                lowerGlobalVariable(decl)
            }
        }

        // 3. Lower functions and identify entry points
        for (decl in unit.declarations) {
            if (decl is FunctionDecl) {
                val functionHandle = lowerFunction(decl)
                
                // Identify entry points
                for (attr in decl.attributes) {
                    val stage = when (attr.name) {
                        "vertex" -> IrShaderStage.Vertex
                        "fragment" -> IrShaderStage.Fragment
                        "compute" -> IrShaderStage.Compute
                        else -> null
                    }
                    
                    if (stage != null) {
                        module.entryPoints.add(IrEntryPoint(
                            name = decl.name,
                            function = functionHandle,
                            stage = stage,
                        ))
                    }
                }
            }
        }

        return module
    }

    private fun lowerType(typeDecl: TypeDecl): Handle<IrType> {
        return typeMap.getOrPut(typeDecl) {
            val inner = when (typeDecl) {
                is ScalarType -> IrTypeInner.Scalar(lowerScalarKind(typeDecl.kind), 4)
                is VectorType -> IrTypeInner.Vector(lowerVectorSize(typeDecl.size), lowerType(typeDecl.elementType))
                is MatrixType -> IrTypeInner.Matrix(lowerVectorSize(typeDecl.columns), lowerVectorSize(typeDecl.rows), lowerType(typeDecl.elementType))
                is ArrayType -> IrTypeInner.Array(lowerType(typeDecl.elementType), IrArraySize.Dynamic(Handle<IrExpression>(0)))
                is StructType -> IrTypeInner.Struct(emptyList()) 
                is PointerType -> IrTypeInner.Pointer(
                    lowerType(typeDecl.elementType),
                    lowerAddressSpace(typeDecl.storageClass),
                    lowerAccessModeText(typeDecl.accessMode)
                )
                else -> IrTypeInner.Scalar(IrScalarKind.F32, 4)
            }
            module.types.append(IrType(inner))
        }
    }

    private fun lowerScalarKind(kind: ScalarKind): IrScalarKind = when (kind) {
        ScalarKind.BOOL -> IrScalarKind.Bool
        ScalarKind.U32 -> IrScalarKind.Uint
        ScalarKind.I32 -> IrScalarKind.Sint
        ScalarKind.F32 -> IrScalarKind.F32
        else -> IrScalarKind.F32
    }

    private fun lowerVectorSize(size: Int): IrVectorSize = when (size) {
        2 -> IrVectorSize.Bi
        3 -> IrVectorSize.Tri
        4 -> IrVectorSize.Quad
        else -> IrVectorSize.Quad
    }

    private fun lowerAddressSpace(storageClass: io.ygdrasil.wgsl.ast.StorageClass): IrAddressSpace = when (storageClass) {
        io.ygdrasil.wgsl.ast.StorageClass.FUNCTION -> IrAddressSpace.Function
        io.ygdrasil.wgsl.ast.StorageClass.PRIVATE -> IrAddressSpace.Private
        io.ygdrasil.wgsl.ast.StorageClass.WORKGROUP -> IrAddressSpace.Workgroup
        io.ygdrasil.wgsl.ast.StorageClass.UNIFORM -> IrAddressSpace.Uniform
        io.ygdrasil.wgsl.ast.StorageClass.STORAGE -> IrAddressSpace.Storage
        io.ygdrasil.wgsl.ast.StorageClass.HANDLE -> IrAddressSpace.Private // Fallback
    }

    private fun lowerStorageClass(storageClass: io.ygdrasil.wgsl.ast.StorageClass): IrStorageClass = when (storageClass) {
        io.ygdrasil.wgsl.ast.StorageClass.FUNCTION -> IrStorageClass.Function
        io.ygdrasil.wgsl.ast.StorageClass.PRIVATE -> IrStorageClass.Private
        io.ygdrasil.wgsl.ast.StorageClass.WORKGROUP -> IrStorageClass.Workgroup
        io.ygdrasil.wgsl.ast.StorageClass.UNIFORM -> IrStorageClass.Uniform
        io.ygdrasil.wgsl.ast.StorageClass.STORAGE -> IrStorageClass.Storage
        io.ygdrasil.wgsl.ast.StorageClass.HANDLE -> IrStorageClass.Handle
    }

    private fun lowerStruct(decl: StructDecl) {
        val members = decl.members.map { member ->
            IrStructMember(
                name = member.name,
                type = lowerType(member.type),
                binding = null,
                offset = 0
            )
        }
        val type = IrType(IrTypeInner.Struct(members))
        val handle = module.types.append(type)
        typeMap[StructType(decl.name, decl.span)] = handle
    }

    private fun lowerTypeAlias(decl: TypeAliasDecl) {
        val handle = lowerType(decl.type)
        typeMap[NamedType(decl.name, decl.span)] = handle
    }

    private fun lowerGlobalVariable(decl: VariableDecl) {
        val type = decl.type?.let { lowerType(it) } ?: return
        
        val storageClass = if (decl.storageClass != null) {
            lowerStorageClassText(decl.storageClass)
        } else {
            lowerStorageClass(decl.kind.toStorageClass())
        }
        
        val accessMode = lowerAccessModeText(decl.accessMode)

        val variable = IrGlobalVariable(
            name = decl.name,
            storageClass = storageClass,
            accessMode = accessMode,
            binding = null,
            type = type,
            init = null
        )
        globalVarMap[decl.name] = module.globalVariables.append(variable)
    }

    private fun lowerStorageClassText(text: String?): IrStorageClass = when (text) {
        "storage" -> IrStorageClass.Storage
        "uniform" -> IrStorageClass.Uniform
        "workgroup" -> IrStorageClass.Workgroup
        "private" -> IrStorageClass.Private
        "function" -> IrStorageClass.Function
        "push_constant" -> IrStorageClass.PushConstant
        else -> IrStorageClass.Private
    }

    private fun lowerAccessModeText(text: String?): IrAccessMode? = when (text) {
        "read" -> IrAccessMode.Read
        "write" -> IrAccessMode.Write
        "read_write" -> IrAccessMode.ReadWrite
        else -> null
    }

    private fun VariableDeclKind.toStorageClass(): io.ygdrasil.wgsl.ast.StorageClass = when (this) {
        VariableDeclKind.VAR -> io.ygdrasil.wgsl.ast.StorageClass.PRIVATE
        VariableDeclKind.LET -> io.ygdrasil.wgsl.ast.StorageClass.FUNCTION
        VariableDeclKind.CONST -> io.ygdrasil.wgsl.ast.StorageClass.PRIVATE
    }

    private fun lowerFunction(decl: FunctionDecl): Handle<IrFunction> {
        val expressions = Arena<IrExpression>()
        val blocks = Arena<IrBlock>()
        val localVars = Arena<IrLocalVariable>()
        
        currentExpressions = expressions
        currentBlocks = blocks
        currentLocalVars = localVars
        localVariablesMap.clear()
        functionParamsMap.clear()

        val parameters = decl.parameters.mapIndexed { index, param ->
            functionParamsMap[param.name] = index
            IrFunctionParameter(
                name = param.name,
                type = lowerType(param.type),
                binding = null
            )
        }

        val bodyHandle = if (decl.body != null) {
            lowerBlock(decl.body)
        } else {
            blocks.append(IrBlock(emptyList()))
        }

        val func = IrFunction(
            name = decl.name,
            parameters = parameters,
            returnType = decl.returnType?.let { lowerType(it) },
            expressions = expressions,
            localVariables = localVars,
            blocks = blocks,
            body = bodyHandle
        )
        
        val handle = module.functions.append(func)
        functionMap[decl.name] = handle
        
        currentExpressions = null
        currentBlocks = null
        currentLocalVars = null
        
        return handle
    }

    private fun lowerBlock(astBlock: BlockStatement): Handle<IrBlock> {
        val statements = astBlock.statements.map { lowerStatement(it) }
        return currentBlocks!!.append(IrBlock(statements))
    }

    private fun lowerStatement(astStmt: Statement): IrStatement {
        return when (astStmt) {
            is ReturnStatement -> IrStatement.Return(astStmt.value?.let { lowerExpression(it) })
            is AssignmentStatement -> IrStatement.Assign(lowerExpression(astStmt.lhs), lowerExpression(astStmt.rhs))
            is IfStatement -> {
                val cond = lowerExpression(astStmt.condition)
                val accept = lowerBlock(astStmt.thenBranch as? BlockStatement ?: BlockStatement(listOf(astStmt.thenBranch), astStmt.thenBranch.span))
                val reject = astStmt.elseBranch?.let { 
                    lowerBlock(it as? BlockStatement ?: BlockStatement(listOf(it), it.span))
                }
                IrStatement.If(cond, accept, reject)
            }
            is BlockStatement -> IrStatement.Block(lowerBlock(astStmt))
            is VariableDeclStatement -> {
                val type = astStmt.type?.let { lowerType(it) } ?: lowerInferredType(astStmt.initializer)
                val localVar = IrLocalVariable(
                    name = astStmt.name,
                    type = type,
                    init = astStmt.initializer?.let { lowerExpression(it) }
                )
                val handle = currentLocalVars!!.append(localVar)
                localVariablesMap[astStmt.name] = handle
                if (astStmt.initializer != null) {
                    IrStatement.Init(handle)
                } else {
                    IrStatement.Declare(handle)
                }
            }
            else -> IrStatement.Nop
        }
    }

    private fun lowerExpression(astExpr: Expression): Handle<IrExpression> {
        val kind = when (astExpr) {
            is IntLiteral -> {
                val scalar = if (astExpr.suffix == "u") IrScalarValue.U32(astExpr.value) else IrScalarValue.I32(astExpr.value.toInt())
                IrExpressionKind.Literal(IrLiteralValue.Scalar(scalar))
            }
            is FloatLiteral -> IrExpressionKind.Literal(IrLiteralValue.Scalar(IrScalarValue.F32(astExpr.value.toFloat())))
            is BoolLiteral -> IrExpressionKind.Literal(IrLiteralValue.Scalar(IrScalarValue.Bool(astExpr.value)))
            is IdentExpr -> {
                val name = astExpr.name
                if (localVariablesMap.containsKey(name)) {
                    IrExpressionKind.LocalVar(localVariablesMap[name]!!)
                } else if (functionParamsMap.containsKey(name)) {
                    IrExpressionKind.FunctionArgument(functionParamsMap[name]!!)
                } else if (globalVarMap.containsKey(name)) {
                    IrExpressionKind.GlobalVar(globalVarMap[name]!!)
                } else {
                    // Fallback to a placeholder if not found (should be resolved by TypeResolver)
                    IrExpressionKind.Literal(IrLiteralValue.Scalar(IrScalarValue.I32(0)))
                }
            }
            is BinaryExpr -> IrExpressionKind.Binary(
                lowerBinaryOperator(astExpr.op),
                lowerExpression(astExpr.left),
                lowerExpression(astExpr.right)
            )
            is UnaryExpr -> IrExpressionKind.Unary(
                lowerUnaryOperator(astExpr.op),
                lowerExpression(astExpr.operand)
            )
            is CallExpr -> {
                // Heuristic: if callee is IdentExpr and matches a function name, it's a call
                // If it matches a type name, it's a TypeConstructor
                val calleeName = (astExpr.callee as? IdentExpr)?.name
                if (calleeName != null && functionMap.containsKey(calleeName)) {
                    IrExpressionKind.Call(functionMap[calleeName]!!, astExpr.args.map { lowerExpression(it) })
                } else {
                    // Assume type constructor for now
                    val type = lowerType(NamedType(calleeName ?: "unknown", astExpr.span))
                    IrExpressionKind.TypeConstructor(type, astExpr.args.map { lowerExpression(it) })
                }
            }
            is MemberAccessExpr -> {
                // Simplified: assume index 0 for now as we don't have full type info here
                IrExpressionKind.Access(lowerExpression(astExpr.objectExpr), 0)
            }
            else -> IrExpressionKind.Literal(IrLiteralValue.Scalar(IrScalarValue.I32(0)))
        }
        return currentExpressions!!.append(IrExpression(kind))
    }

    private fun lowerBinaryOperator(op: BinaryOperator): IrBinaryOperator = when (op) {
        BinaryOperator.ADD -> IrBinaryOperator.Add
        BinaryOperator.SUBTRACT -> IrBinaryOperator.Subtract
        BinaryOperator.MULTIPLY -> IrBinaryOperator.Multiply
        BinaryOperator.DIVIDE -> IrBinaryOperator.Divide
        BinaryOperator.MODULO -> IrBinaryOperator.Modulo
        BinaryOperator.EQ -> IrBinaryOperator.Equal
        BinaryOperator.NEQ -> IrBinaryOperator.NotEqual
        BinaryOperator.LT -> IrBinaryOperator.Less
        BinaryOperator.LTE -> IrBinaryOperator.LessOrEqual
        BinaryOperator.GT -> IrBinaryOperator.Greater
        BinaryOperator.GTE -> IrBinaryOperator.GreaterOrEqual
        BinaryOperator.LOGICAL_AND -> IrBinaryOperator.LogicalAnd
        BinaryOperator.LOGICAL_OR -> IrBinaryOperator.LogicalOr
        BinaryOperator.BITWISE_AND -> IrBinaryOperator.BitAnd
        BinaryOperator.BITWISE_OR -> IrBinaryOperator.BitOr
        BinaryOperator.BITWISE_XOR -> IrBinaryOperator.BitXor
        BinaryOperator.LEFT_SHIFT -> IrBinaryOperator.ShiftLeft
        BinaryOperator.RIGHT_SHIFT -> IrBinaryOperator.ShiftRight
        else -> IrBinaryOperator.Add
    }

    private fun lowerUnaryOperator(op: UnaryOperator): IrUnaryOperator = when (op) {
        UnaryOperator.MINUS -> IrUnaryOperator.Negate
        UnaryOperator.NOT -> IrUnaryOperator.Not
        UnaryOperator.BITWISE_NOT -> IrUnaryOperator.BitNot
        UnaryOperator.PLUS -> IrUnaryOperator.BitNot // Plus is often a no-op, use BitNot as placeholder if needed but usually it should be handled
    }

    private fun lowerInferredType(initializer: Expression?): Handle<IrType> {
        // Very simplified inference
        return module.types.append(IrType(IrTypeInner.Scalar(IrScalarKind.F32, 4)))
    }
}
