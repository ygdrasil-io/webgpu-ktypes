package io.ygdrasil.wgsl.parser

import io.ygdrasil.wgsl.ast.TranslationUnit
import io.ygdrasil.wgsl.ast.StructDecl
import io.ygdrasil.wgsl.ast.TypeAliasDecl
import io.ygdrasil.wgsl.ast.VariableDecl
import io.ygdrasil.wgsl.ast.FunctionDecl
import io.ygdrasil.wgsl.ast.EntryPointAttribute
import io.ygdrasil.wgsl.ast.TypeDecl
import io.ygdrasil.wgsl.ast.ScalarType
import io.ygdrasil.wgsl.ast.VectorType
import io.ygdrasil.wgsl.ast.MatrixType
import io.ygdrasil.wgsl.ast.ArrayType
import io.ygdrasil.wgsl.ast.StructType
import io.ygdrasil.wgsl.ast.PointerType
import io.ygdrasil.wgsl.ast.VariableDeclKind
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.ir.Function
import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.ir.Type
import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.arena.Arena
import io.ygdrasil.wgsl.ir.ScalarKind as IrScalarKind
import io.ygdrasil.wgsl.ast.ScalarKind as AstScalarKind

/**
 * Lowers a resolved WGSL AST to the IR (Module).
 */
class Lowerer {

    private val module = Module()
    private val typeMap = mutableMapOf<TypeDecl, Handle<Type>>()
    private val globalVarMap = mutableMapOf<String, Handle<GlobalVariable>>()
    private val functionMap = mutableMapOf<String, Handle<Function>>()

    fun lower(unit: TranslationUnit): Module {
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
                        "vertex" -> ShaderStage.Vertex
                        "fragment" -> ShaderStage.Fragment
                        "compute" -> ShaderStage.Compute
                        else -> null
                    }
                    
                    if (stage != null) {
                        module.entryPoints.add(EntryPoint(
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

    private fun lowerType(typeDecl: TypeDecl): Handle<Type> {
        return typeMap.getOrPut(typeDecl) {
            val inner = when (typeDecl) {
                is ScalarType -> TypeInner.Scalar(lowerScalarKind(typeDecl.kind), 4)
                is VectorType -> TypeInner.Vector(lowerVectorSize(typeDecl.size), lowerType(typeDecl.elementType))
                is MatrixType -> TypeInner.Matrix(lowerVectorSize(typeDecl.columns), lowerVectorSize(typeDecl.rows), lowerType(typeDecl.elementType))
                is ArrayType -> TypeInner.Array(lowerType(typeDecl.elementType), ArraySize.Dynamic(Handle<Expression>(0)))
                is StructType -> TypeInner.Struct(emptyList()) 
                is PointerType -> TypeInner.Pointer(lowerType(typeDecl.elementType), lowerAddressSpace(typeDecl.storageClass))
                else -> TypeInner.Scalar(IrScalarKind.F32, 4)
            }
            module.types.append(Type(inner))
        }
    }

    private fun lowerScalarKind(kind: AstScalarKind): IrScalarKind = when (kind) {
        AstScalarKind.BOOL -> IrScalarKind.Bool
        AstScalarKind.U32 -> IrScalarKind.Uint
        AstScalarKind.I32 -> IrScalarKind.Sint
        AstScalarKind.F32 -> IrScalarKind.F32
        else -> IrScalarKind.F32
    }

    private fun lowerVectorSize(size: Int): VectorSize = when (size) {
        2 -> VectorSize.Bi
        3 -> VectorSize.Tri
        4 -> VectorSize.Quad
        else -> VectorSize.Quad
    }

    private fun lowerAddressSpace(storageClass: io.ygdrasil.wgsl.ast.StorageClass): AddressSpace = when (storageClass) {
        io.ygdrasil.wgsl.ast.StorageClass.FUNCTION -> AddressSpace.Function
        io.ygdrasil.wgsl.ast.StorageClass.PRIVATE -> AddressSpace.Private
        io.ygdrasil.wgsl.ast.StorageClass.WORKGROUP -> AddressSpace.Workgroup
        io.ygdrasil.wgsl.ast.StorageClass.UNIFORM -> AddressSpace.Uniform
        io.ygdrasil.wgsl.ast.StorageClass.STORAGE -> AddressSpace.Storage
        else -> AddressSpace.Private
    }

    private fun lowerStorageClass(storageClass: io.ygdrasil.wgsl.ast.StorageClass): StorageClass = when (storageClass) {
        io.ygdrasil.wgsl.ast.StorageClass.FUNCTION -> StorageClass.Function
        io.ygdrasil.wgsl.ast.StorageClass.PRIVATE -> StorageClass.Private
        io.ygdrasil.wgsl.ast.StorageClass.WORKGROUP -> StorageClass.Workgroup
        io.ygdrasil.wgsl.ast.StorageClass.UNIFORM -> StorageClass.Uniform
        io.ygdrasil.wgsl.ast.StorageClass.STORAGE -> StorageClass.Storage
        io.ygdrasil.wgsl.ast.StorageClass.HANDLE -> StorageClass.Handle
    }

    private fun lowerStruct(decl: StructDecl) {
        // TODO: implement struct lowering
    }

    private fun lowerTypeAlias(decl: TypeAliasDecl) {
        lowerType(decl.type)
    }

    private fun lowerGlobalVariable(decl: VariableDecl) {
        val type = decl.type?.let { lowerType(it) } ?: return
        val variable = GlobalVariable(
            name = decl.name,
            storageClass = lowerStorageClass(decl.kind.toStorageClass()),
            binding = null,
            type = type,
            init = null
        )
        globalVarMap[decl.name] = module.globalVariables.append(variable)
    }

    private fun VariableDeclKind.toStorageClass(): io.ygdrasil.wgsl.ast.StorageClass = when (this) {
        VariableDeclKind.VAR -> io.ygdrasil.wgsl.ast.StorageClass.PRIVATE
        VariableDeclKind.LET -> io.ygdrasil.wgsl.ast.StorageClass.FUNCTION
        VariableDeclKind.CONST -> io.ygdrasil.wgsl.ast.StorageClass.PRIVATE
    }

    private fun lowerFunction(decl: FunctionDecl): Handle<Function> {
        val expressions = Arena<Expression>()
        val blocks = Arena<Block>()
        
        // Simple body with a nop for now
        val bodyHandle = blocks.append(Block(emptyList()))

        val func = Function(
            name = decl.name,
            parameters = decl.parameters.map { param ->
                FunctionParameter(
                    name = param.name,
                    type = lowerType(param.type),
                    binding = null
                )
            },
            returnType = decl.returnType?.let { lowerType(it) },
            expressions = expressions,
            localVariables = Arena(),
            blocks = blocks,
            body = bodyHandle
        )
        
        val handle = module.functions.append(func)
        functionMap[decl.name] = handle
        return handle
    }
}
