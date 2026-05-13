package io.ygdrasil.wgsl.parser

import io.ygdrasil.wgsl.ast.*

/**
 * Index of all types and values declared in a WGSL module.
 * 
 * This class provides a centralized registry for looking up declarations by name,
 * including built-in WGSL types (scalar, vector, matrix types).
 */
class TypeIndex {
    
    // ========== Built-in WGSL Types ==========
    
    private val builtinScalarTypes: Map<String, ScalarType> = mapOf(
        "bool" to ScalarType(ScalarKind.BOOL, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "i8" to ScalarType(ScalarKind.I8, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "u8" to ScalarType(ScalarKind.U8, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "i16" to ScalarType(ScalarKind.I16, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "u16" to ScalarType(ScalarKind.U16, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "i32" to ScalarType(ScalarKind.I32, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "u32" to ScalarType(ScalarKind.U32, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "i64" to ScalarType(ScalarKind.I64, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "u64" to ScalarType(ScalarKind.U64, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "f16" to ScalarType(ScalarKind.F16, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "f32" to ScalarType(ScalarKind.F32, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
        "f64" to ScalarType(ScalarKind.F64, io.ygdrasil.wgsl.ir.Span.UNDEFINED),
    )
    
    // ========== User-Declared Types ==========
    
    private val structs: MutableMap<String, StructDecl> = mutableMapOf()
    private val typeAliases: MutableMap<String, TypeAliasDecl> = mutableMapOf()
    private val functions: MutableMap<String, FunctionDecl> = mutableMapOf()
    private val globalVariables: MutableMap<String, VariableDecl> = mutableMapOf()
    private val globalConstants: MutableMap<String, VariableDecl> = mutableMapOf()
    
    // ========== Indexing ==========
    
    /**
     * Index all declarations in a translation unit.
     * 
     * @param unit The translation unit to index
     */
    fun index(unit: TranslationUnit) {
        reset()
        for (declaration in unit.declarations) {
            indexDeclaration(declaration)
        }
    }
    
    /**
     * Index a single declaration.
     */
    private fun indexDeclaration(declaration: GlobalDecl) {
        when (declaration) {
            is StructDecl -> structs[declaration.name] = declaration
            is TypeAliasDecl -> typeAliases[declaration.name] = declaration
            is FunctionDecl -> functions[declaration.name] = declaration
            is VariableDecl -> {
                when (declaration.kind) {
                    VariableDeclKind.CONST -> globalConstants[declaration.name] = declaration
                    VariableDeclKind.LET, VariableDeclKind.VAR -> 
                        globalVariables[declaration.name] = declaration
                }
            }
            is OverrideDecl -> {
                // Index the function inside the override
                functions[declaration.function.name] = declaration.function
            }
            else -> {}
        }
    }
    
    /**
     * Reset all indexes (clear user-declared types, keep built-ins).
     */
    fun reset() {
        structs.clear()
        typeAliases.clear()
        functions.clear()
        globalVariables.clear()
        globalConstants.clear()
    }
    
    // ========== Lookup Methods ==========
    
    /**
     * Check if a type name is known (builtin or user-declared).
     */
    fun isKnownType(name: String): Boolean {
        return builtinScalarTypes.containsKey(name) ||
               structs.containsKey(name) ||
               typeAliases.containsKey(name)
    }
    
    /**
     * Check if a value name is known (function, variable, constant).
     */
    fun isKnownValue(name: String): Boolean {
        return functions.containsKey(name) ||
               globalVariables.containsKey(name) ||
               globalConstants.containsKey(name) ||
               isBuiltinValue(name)
    }
    
    /**
     * Check if a name is a builtin value (true, false, etc.).
     */
    fun isBuiltinValue(name: String): Boolean {
        return name == "true" || name == "false"
    }
    
    /**
     * Find a struct declaration by name.
     */
    fun findStruct(name: String): StructDecl? = structs[name]
    
    /**
     * Find a type alias declaration by name.
     */
    fun findTypeAlias(name: String): TypeAliasDecl? = typeAliases[name]
    
    /**
     * Find a function declaration by name.
     */
    fun findFunction(name: String): FunctionDecl? = functions[name]
    
    /**
     * Find a global variable by name.
     */
    fun findGlobalVariable(name: String): VariableDecl? = globalVariables[name]
    
    /**
     * Find a global constant by name.
     */
    fun findGlobalConstant(name: String): VariableDecl? = globalConstants[name]
    
    /**
     * Find any declaration by name (type or value).
     */
    fun findDeclaration(name: String): GlobalDecl? {
        return structs[name] ?: typeAliases[name] ?: functions[name] ?:
               globalVariables[name] ?: globalConstants[name]
    }
    
    /**
     * Get all struct declarations.
     */
    fun getAllStructs(): Collection<StructDecl> = structs.values
    
    /**
     * Get all type alias declarations.
     */
    fun getAllTypeAliases(): Collection<TypeAliasDecl> = typeAliases.values
    
    /**
     * Get all function declarations.
     */
    fun getAllFunctions(): Collection<FunctionDecl> = functions.values
    
    /**
     * Get all global variable declarations.
     */
    fun getAllGlobalVariables(): Collection<VariableDecl> = globalVariables.values
    
    /**
     * Get all global constant declarations.
     */
    fun getAllGlobalConstants(): Collection<VariableDecl> = globalConstants.values
    
    // ========== Type Resolution Helpers ==========
    
    /**
     * Get the ScalarType for a builtin scalar type name.
     */
    fun getBuiltinScalarType(name: String): ScalarType? = builtinScalarTypes[name]
    
    /**
     * Get the ScalarKind for a builtin scalar type name.
     */
    fun getBuiltinScalarKind(name: String): ScalarKind? = builtinScalarTypes[name]?.kind
    
    /**
     * Check if a name is a builtin scalar type.
     */
    fun isBuiltinScalarType(name: String): Boolean = builtinScalarTypes.containsKey(name)
    
    /**
     * Check if a name is a builtin vector type (vec2, vec3, vec4).
     */
    fun isBuiltinVectorType(name: String): Boolean {
        return name.startsWith("vec") && (name.endsWith("2") || name.endsWith("3") || name.endsWith("4"))
    }
    
    /**
     * Check if a name is a builtin matrix type (matCxR).
     */
    fun isBuiltinMatrixType(name: String): Boolean {
        return name.startsWith("mat")
    }
    
    /**
     * Parse a builtin vector type name (e.g., "vec2<f32>") into its components.
     * Returns (size, elementTypeName) or null if not a vector type.
     */
    fun parseBuiltinVectorType(name: String): Pair<Int, String>? {
        val vectorRegex = Regex("vec(\\d+)<(.+)>")
        val match = vectorRegex.matchEntire(name)
        if (match != null) {
            val size = match.groupValues[1].toIntOrNull()
            val elementType = match.groupValues[2]
            if (size != null && size in 2..4) {
                return Pair(size, elementType)
            }
        }
        return null
    }
    
    /**
     * Parse a builtin matrix type name (e.g., "mat2x3<f32>") into its components.
     * Returns (columns, rows, elementTypeName) or null if not a matrix type.
     */
    fun parseBuiltinMatrixType(name: String): Triple<Int, Int, String>? {
        val matrixRegex = Regex("mat(\\d+)x(\\d+)<(.+)>")
        val match = matrixRegex.matchEntire(name)
        if (match != null) {
            val cols = match.groupValues[1].toIntOrNull()
            val rows = match.groupValues[2].toIntOrNull()
            val elementType = match.groupValues[3]
            if (cols != null && rows != null) {
                return Triple(cols, rows, elementType)
            }
        }
        return null
    }
    
    /**
     * Get all declared names (types and values).
     */
    fun getAllDeclaredNames(): Set<String> {
        val names = mutableSetOf<String>()
        names.addAll(structs.keys)
        names.addAll(typeAliases.keys)
        names.addAll(functions.keys)
        names.addAll(globalVariables.keys)
        names.addAll(globalConstants.keys)
        return names
    }
    
    /**
     * Check if a name is already declared.
     */
    fun isDeclared(name: String): Boolean = getAllDeclaredNames().contains(name)
}
