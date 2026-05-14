package io.ygdrasil.wgsl.parser

import io.ygdrasil.wgsl.ast.*

/**
 * Performs topological sorting of WGSL module declarations to handle forward references.
 * 
 * This class builds a dependency graph and reorders declarations so that
 * all dependencies come before the declarations that use them.
 */
class ModuleIndexer {

    /**
     * Reorders declarations in a translation unit to ensure all dependencies
     * are declared before they are used.
     * 
     * @param unit The translation unit to reorder
     * @return A new translation unit with reordered declarations
     */
    fun reorderDeclarations(unit: TranslationUnit): TranslationUnit {
        val dependencyGraph = buildDependencyGraph(unit)
        val sortedNames = topologicalSort(dependencyGraph)

        // Map names back to declarations
        val nameToDecl = mutableMapOf<String, GlobalDecl>()
        for (decl in unit.declarations) {
            val name = getDeclarationName(decl)
            if (name != null) {
                nameToDecl[name] = decl
            }
        }

        val sortedDeclarations = sortedNames.mapNotNull { nameToDecl[it] }
        return TranslationUnit(sortedDeclarations, unit.span)
    }

    /**
     * Builds a dependency graph from a translation unit.
     * 
     * @param unit The translation unit
     * @return Map of declaration name to set of names it depends on
     */
    fun buildDependencyGraph(unit: TranslationUnit): Map<String, Set<String>> {
        val graph = mutableMapOf<String, MutableSet<String>>()
        val typeIndex = TypeIndex()

        // First pass: collect all declaration names
        val allNames = mutableSetOf<String>()
        for (decl in unit.declarations) {
            val name = getDeclarationName(decl)
            if (name != null) {
                allNames.add(name)
                graph[name] = mutableSetOf()
            }
        }

        // Second pass: find dependencies for each declaration
        for (decl in unit.declarations) {
            val name = getDeclarationName(decl)
            if (name == null) continue

            val dependencies = findDependenciesInDeclaration(decl, allNames)
            graph[name]?.addAll(dependencies)
        }

        return graph
    }

    /**
     * Find all dependencies in a declaration.
     */
    private fun findDependenciesInDeclaration(
        decl: GlobalDecl,
        allNames: Set<String>
    ): Set<String> {
        val dependencies = mutableSetOf<String>()

        when (decl) {
            is FunctionDecl -> {
                // Dependencies in parameters
                for (param in decl.parameters) {
                    dependencies.addAll(findDependenciesInType(param.type, allNames))
                    if (param.defaultValue != null) {
                        dependencies.addAll(findDependenciesInExpression(param.defaultValue, allNames))
                    }
                }

                // Dependencies in return type
                if (decl.returnType != null) {
                    dependencies.addAll(findDependenciesInType(decl.returnType, allNames))
                }

                // Dependencies in body
                if (decl.body != null) {
                    dependencies.addAll(findDependenciesInBlock(decl.body, allNames))
                }
            }

            is StructDecl -> {
                for (member in decl.members) {
                    dependencies.addAll(findDependenciesInType(member.type, allNames))
                    if (member.defaultValue != null) {
                        dependencies.addAll(findDependenciesInExpression(member.defaultValue, allNames))
                    }
                }
            }

            is VariableDecl -> {
                if (decl.type != null) {
                    dependencies.addAll(findDependenciesInType(decl.type, allNames))
                }
                if (decl.initializer != null) {
                    dependencies.addAll(findDependenciesInExpression(decl.initializer, allNames))
                }
            }

            is TypeAliasDecl -> {
                dependencies.addAll(findDependenciesInType(decl.type, allNames))
            }

            is OverrideDecl -> {
                // The function itself might have dependencies
                dependencies.addAll(findDependenciesInDeclaration(decl.function, allNames))
            }

            else -> {}
        }

        return dependencies
    }

    /**
     * Find dependencies in a block statement.
     */
    private fun findDependenciesInBlock(
        block: BlockStatement,
        allNames: Set<String>
    ): Set<String> {
        val dependencies = mutableSetOf<String>()
        for (stmt in block.statements) {
            dependencies.addAll(findDependenciesInStatement(stmt, allNames))
        }
        return dependencies
    }

    /**
     * Find dependencies in a statement.
     */
    private fun findDependenciesInStatement(
        stmt: Statement,
        allNames: Set<String>
    ): Set<String> {
        val dependencies = mutableSetOf<String>()

        when (stmt) {
            is BlockStatement -> {
                dependencies.addAll(findDependenciesInBlock(stmt, allNames))
            }

            is IfStatement -> {
                dependencies.addAll(findDependenciesInExpression(stmt.condition, allNames))
                dependencies.addAll(findDependenciesInStatement(stmt.thenBranch, allNames))
                if (stmt.elseBranch != null) {
                    dependencies.addAll(findDependenciesInStatement(stmt.elseBranch, allNames))
                }
            }

            is SwitchStatement -> {
                dependencies.addAll(findDependenciesInExpression(stmt.expression, allNames))
                dependencies.addAll(findDependenciesInSwitchBody(stmt.body, allNames))
            }

            is LoopStatement -> {
                dependencies.addAll(findDependenciesInBlock(stmt.body, allNames))
                if (stmt.continuing != null) {
                    dependencies.addAll(findDependenciesInBlock(stmt.continuing, allNames))
                }
            }

            is WhileStatement -> {
                dependencies.addAll(findDependenciesInExpression(stmt.condition, allNames))
                dependencies.addAll(findDependenciesInBlock(stmt.body, allNames))
                if (stmt.continuing != null) {
                    dependencies.addAll(findDependenciesInBlock(stmt.continuing, allNames))
                }
            }

            is ForStatement -> {
                if (stmt.init != null) {
                    dependencies.addAll(findDependenciesInStatement(stmt.init, allNames))
                }
                if (stmt.condition != null) {
                    dependencies.addAll(findDependenciesInExpression(stmt.condition, allNames))
                }
                if (stmt.update != null) {
                    dependencies.addAll(findDependenciesInExpression(stmt.update, allNames))
                }
                dependencies.addAll(findDependenciesInBlock(stmt.body, allNames))
            }

            is VariableDeclStatement -> {
                if (stmt.type != null) {
                    dependencies.addAll(findDependenciesInType(stmt.type, allNames))
                }
                if (stmt.initializer != null) {
                    dependencies.addAll(findDependenciesInExpression(stmt.initializer, allNames))
                }
            }

            is AssignmentStatement -> {
                dependencies.addAll(findDependenciesInExpression(stmt.lhs, allNames))
                dependencies.addAll(findDependenciesInExpression(stmt.rhs, allNames))
            }

            is IncDecStatement -> {
                dependencies.addAll(findDependenciesInExpression(stmt.expr, allNames))
            }

            is ReturnStatement -> {
                if (stmt.value != null) {
                    dependencies.addAll(findDependenciesInExpression(stmt.value, allNames))
                }
            }

            is ExpressionStatement -> {
                dependencies.addAll(findDependenciesInExpression(stmt.expr, allNames))
            }

            else -> {}
        }

        return dependencies
    }

    /**
     * Find dependencies in a switch body.
     */
    private fun findDependenciesInSwitchBody(
        body: SwitchBody,
        allNames: Set<String>
    ): Set<String> {
        val dependencies = mutableSetOf<String>()
        for (case in body.cases) {
            when (case) {
                is Case -> {
                    dependencies.addAll(findDependenciesInExpression(case.value, allNames))
                    dependencies.addAll(findDependenciesInBlock(case.body, allNames))
                }

                is DefaultCase -> {
                    dependencies.addAll(findDependenciesInBlock(case.body, allNames))
                }
            }
        }
        return dependencies
    }

    /**
     * Find dependencies in an expression.
     */
    private fun findDependenciesInExpression(
        expr: Expression,
        allNames: Set<String>
    ): Set<String> {
        val dependencies = mutableSetOf<String>()

        when (expr) {
            is IdentExpr -> {
                if (allNames.contains(expr.name)) {
                    dependencies.add(expr.name)
                }
            }

            is CallExpr -> {
                dependencies.addAll(findDependenciesInExpression(expr.callee, allNames))
                for (arg in expr.args) {
                    dependencies.addAll(findDependenciesInExpression(arg, allNames))
                }
                if (expr.templateArgs != null) {
                    for (typeArg in expr.templateArgs) {
                        dependencies.addAll(findDependenciesInType(typeArg, allNames))
                    }
                }
            }

            is MemberAccessExpr -> {
                dependencies.addAll(findDependenciesInExpression(expr.objectExpr, allNames))
            }

            is IndexExpr -> {
                dependencies.addAll(findDependenciesInExpression(expr.objectExpr, allNames))
                dependencies.addAll(findDependenciesInExpression(expr.index, allNames))
            }

            is UnaryExpr -> {
                dependencies.addAll(findDependenciesInExpression(expr.operand, allNames))
            }

            is BinaryExpr -> {
                dependencies.addAll(findDependenciesInExpression(expr.left, allNames))
                dependencies.addAll(findDependenciesInExpression(expr.right, allNames))
            }

            is TernaryExpr -> {
                dependencies.addAll(findDependenciesInExpression(expr.condition, allNames))
                dependencies.addAll(findDependenciesInExpression(expr.trueExpr, allNames))
                dependencies.addAll(findDependenciesInExpression(expr.falseExpr, allNames))
            }

            is TypeCastExpr -> {
                dependencies.addAll(findDependenciesInExpression(expr.expr, allNames))
                dependencies.addAll(findDependenciesInType(expr.type, allNames))
            }

            is SwizzleExpr -> {
                dependencies.addAll(findDependenciesInExpression(expr.objectExpr, allNames))
            }

            else -> {}
        }

        return dependencies
    }

    /**
     * Find dependencies in a type declaration.
     */
    private fun findDependenciesInType(
        type: TypeDecl,
        allNames: Set<String>
    ): Set<String> {
        val dependencies = mutableSetOf<String>()

        when (type) {
            is ScalarType -> {
                // No dependencies for scalar types
            }

            is VectorType -> {
                dependencies.addAll(findDependenciesInType(type.elementType, allNames))
            }

            is MatrixType -> {
                dependencies.addAll(findDependenciesInType(type.elementType, allNames))
            }

            is ArrayType -> {
                dependencies.addAll(findDependenciesInType(type.elementType, allNames))
                if (type.length != null) {
                    dependencies.addAll(findDependenciesInExpression(type.length, allNames))
                }
            }

            is StructType -> {
                if (allNames.contains(type.name)) {
                    dependencies.add(type.name)
                }
            }

            is NamedType -> {
                if (allNames.contains(type.name)) {
                    dependencies.add(type.name)
                }
            }

            is PointerType -> {
                dependencies.addAll(findDependenciesInType(type.elementType, allNames))
            }

            is ReferenceType -> {
                dependencies.addAll(findDependenciesInType(type.elementType, allNames))
            }

            is TemplateType -> {
                if (allNames.contains(type.name)) {
                    dependencies.add(type.name)
                }
                for (arg in type.args) {
                    dependencies.addAll(findDependenciesInType(arg, allNames))
                }
            }
        }

        return dependencies
    }

    /**
     * Get the name of a declaration.
     */
    private fun getDeclarationName(decl: GlobalDecl): String? {
        return when (decl) {
            is FunctionDecl -> decl.name
            is StructDecl -> decl.name
            is VariableDecl -> decl.name
            is TypeAliasDecl -> decl.name
            is OverrideDecl -> decl.function.name
            else -> null
        }
    }

    /**
     * Perform topological sort using Kahn's algorithm.
     * 
     * @param graph Dependency graph (node -> set of dependencies)
     * @return List of node names in topological order
     * @throws CycleDetectedException if a cycle is detected
     */
    fun topologicalSort(graph: Map<String, Set<String>>): List<String> {
        // Calculate in-degrees
        val inDegree = mutableMapOf<String, Int>()
        val allNodes = graph.keys.toSet()

        for (node in allNodes) {
            inDegree[node] = 0
        }

        for (node in allNodes) {
            for (dep in graph[node] ?: emptySet()) {
                inDegree[dep] = inDegree.getOrElse(dep, { 0 }) + 1
            }
        }

        // Find all nodes with in-degree 0
        val queue = mutableListOf<String>()
        for (node in allNodes) {
            if (inDegree[node] == 0) {
                queue.add(node)
            }
        }

        // Process nodes
        val sortedNames = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            sortedNames.add(node)

            // Reduce in-degree of dependents
            for (dependent in allNodes) {
                if (graph[dependent]?.contains(node) == true) {
                    inDegree[dependent] = inDegree[dependent]!! - 1
                    if (inDegree[dependent] == 0) {
                        queue.add(dependent)
                    }
                }
            }
        }

        // Check for cycles
        if (sortedNames.size != allNodes.size) {
            val missingNodes = allNodes - sortedNames.toSet()
            throw CycleDetectedException("Cycle detected involving nodes: ${missingNodes.joinToString(", ")}")
        }

        return sortedNames
    }

    /**
     * Find dependencies in a function declaration.
     */
    fun findDependenciesInFunction(func: FunctionDecl, allNames: Set<String>): Set<String> {
        val dependencies = mutableSetOf<String>()

        for (param in func.parameters) {
            dependencies.addAll(findDependenciesInType(param.type, allNames))
            if (param.defaultValue != null) {
                dependencies.addAll(findDependenciesInExpression(param.defaultValue, allNames))
            }
        }

        if (func.returnType != null) {
            dependencies.addAll(findDependenciesInType(func.returnType, allNames))
        }

        if (func.body != null) {
            dependencies.addAll(findDependenciesInBlock(func.body, allNames))
        }

        return dependencies
    }


}

/**
 * Exception thrown when a cycle is detected in the dependency graph.
 */
class CycleDetectedException(message: String) : Exception(message)
