# 🔍 Phase 2.3 : Résolution de Types

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:wgsl`  
**Phase** : 2 - Parsing  
**Sous-Phase** : 2.3 - Type Resolution  
**Durée** : 1-2 semaines  
**Priorité** : ⭐⭐⭐⭐ (Important - Après le parsing)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/index.rs`

---

## 📋 OBJECTIFS

Implémenter la **résolution des références de types** dans l'AST. Après le parsing, beaucoup de types sont représentés comme des noms non résolus (ex: `vec3<f32>`). Cette phase résout ces références en les connectant aux déclarations de types correspondantes.

**Livrable principal** : Un AST avec toutes les références de types résolues.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce que la résolution de types ?

Pendant le parsing, quand on voit un type comme `vec3<f32>` ou `MyStruct`, on crée un `TypeDeclaration.Named` avec juste le nom. La résolution de types :
- **Indexe** toutes les déclarations de types dans le module
- **Résout** les références aux types par leur nom
- **Vérifie** que tous les types référencés existent
- **Construit** les types concrets (ex: `vec3<f32>` → `Vector(TRI, f32)`)

### 2. Types de références à résoudre

| Type de référence | Exemple | Résolution |
|------------------|---------|------------|
| Type scalaire | `i32`, `f32` | → `Scalar(I32)`, `Scalar(F32)` |
| Type vecteur | `vec3<f32>` | → `Vector(TRI, Scalar(F32))` |
| Type matrice | `mat4x4<f32>` | → `Matrix(QUAD, QUAD, Scalar(F32))` |
| Type struct | `MyStruct` | → Référence à la `StructDecl` |
| Type alias | `MyVec3` | → Déreferencé à sa définition |
| Type template | `array<i32, 10>` | → `Array(Scalar(I32), Literal(10))` |

### 3. Ordonnancement (Indexing)

Avant de résoudre les références, il faut **ordonnancer** les déclarations pour que chaque déclaration apparaisse avant ses utilisations. Exemple :

```wgsl
fn useIt() {
    let x: MyType = MyType();  // Utilisation avant déclaration
}

struct MyType { ... }  // Déclaration après
```

WGSL permet les **forward references**, donc on doit réordonner le module.

### 4. Étapes de la résolution

```
1. Indexer toutes les déclarations
   ├── Créer une map : nom → Handle<GlobalDecl>
   ├── Inclure les types prédéclarés (i32, vec2, etc.)
   
2. Ordonnancer les déclarations (indexing)
   ├── Garantir que chaque déclaration apparaît avant ses utilisations
   ├── Utiliser l'algorithme de topological sort
   
3. Résoudre les types dans l'AST
   ├── Parcourir toutes les expressions
   ├── Remplacer les TypeDeclaration.Named par les types concrets
   ├── Remplacer les IdentExpr.Unresolved par IdentExpr.Local ou référence
   
4. Valider les références
   ├── Vérifier que tous les types référencés existent
   ├── Vérifier que tous les identifiants référencés existent
   ├── Générer des erreurs pour les références non résolues
```

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. TypeIndex.kt (Index des types)

**Fichier** : `wgsl:wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/TypeIndex.kt`

```kotlin
package io.ygdrasil.wgsl.front.wgsl

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.front.wgsl.ast.*

/**
 * Index de tous les types déclarés dans un module.
 * Utilisé pour résoudre les références de types par nom.
 */
class TypeIndex(private val translationUnit: TranslationUnit) {
    
    /** Map des noms de struct vers leurs déclarations */
    private val structMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()
    
    /** Map des noms de type alias vers leurs déclarations */
    private val typeAliasMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()
    
    /** Map des noms de variables globales vers leurs déclarations */
    private val variableMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()
    
    /** Map des noms de constantes globales vers leurs déclarations */
    private val constMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()
    
    /** Map des noms de fonctions vers leurs déclarations */
    private val functionMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()
    
    /** Types prédéclarés WGSL */
    private val builtinTypes: Map<String, Handle<TypeDeclaration>>
    
    init {
        // Initialiser les types prédéclarés
        builtinTypes = createBuiltinTypes()
    }
    
    /**
     * Crée les types prédéclarés WGSL.
     */
    private fun createBuiltinTypes(): Map<String, Handle<TypeDeclaration>> {
        val map = mutableMapOf<String, Handle<TypeDeclaration>>()
        
        // Types scalaires
        map["bool"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.BOOL, Span.INVALID)
        )
        map["i8"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.I8, Span.INVALID)
        )
        map["u8"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.U8, Span.INVALID)
        )
        map["i16"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.I16, Span.INVALID)
        )
        map["u16"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.U16, Span.INVALID)
        )
        map["i32"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.I32, Span.INVALID)
        )
        map["u32"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.U32, Span.INVALID)
        )
        map["i64"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.I64, Span.INVALID)
        )
        map["u64"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.U64, Span.INVALID)
        )
        map["f16"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.F16, Span.INVALID)
        )
        map["f32"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.F32, Span.INVALID)
        )
        map["f64"] = translationUnit.types.append(
            TypeDeclaration.Scalar(ScalarKind.F64, Span.INVALID)
        )
        
        // Types vecteurs (seront résolus avec leurs template parameters)
        map["vec2"] = translationUnit.types.append(
            TypeDeclaration.Named("vec2", emptyList(), Span.INVALID)
        )
        map["vec3"] = translationUnit.types.append(
            TypeDeclaration.Named("vec3", emptyList(), Span.INVALID)
        )
        map["vec4"] = translationUnit.types.append(
            TypeDeclaration.Named("vec4", emptyList(), Span.INVALID)
        )
        
        // Types matrices
        map["mat2x2"] = translationUnit.types.append(
            TypeDeclaration.Named("mat2x2", emptyList(), Span.INVALID)
        )
        map["mat2x3"] = translationUnit.types.append(
            TypeDeclaration.Named("mat2x3", emptyList(), Span.INVALID)
        )
        map["mat2x4"] = translationUnit.types.append(
            TypeDeclaration.Named("mat2x4", emptyList(), Span.INVALID)
        )
        map["mat3x2"] = translationUnit.types.append(
            TypeDeclaration.Named("mat3x2", emptyList(), Span.INVALID)
        )
        map["mat3x3"] = translationUnit.types.append(
            TypeDeclaration.Named("mat3x3", emptyList(), Span.INVALID)
        )
        map["mat3x4"] = translationUnit.types.append(
            TypeDeclaration.Named("mat3x4", emptyList(), Span.INVALID)
        )
        map["mat4x2"] = translationUnit.types.append(
            TypeDeclaration.Named("mat4x2", emptyList(), Span.INVALID)
        )
        map["mat4x3"] = translationUnit.types.append(
            TypeDeclaration.Named("mat4x3", emptyList(), Span.INVALID)
        )
        map["mat4x4"] = translationUnit.types.append(
            TypeDeclaration.Named("mat4x4", emptyList(), Span.INVALID)
        )
        
        // Autres types prédéclarés
        map["sampler"] = translationUnit.types.append(
            TypeDeclaration.Named("sampler", emptyList(), Span.INVALID)
        )
        map["texture_2d"] = translationUnit.types.append(
            TypeDeclaration.Named("texture_2d", emptyList(), Span.INVALID)
        )
        // ... etc.
        
        return map
    }
    
    /**
     * Indexe toutes les déclarations du module.
     */
    fun index() {
        for (declHandle in translationUnit.declarations) {
            val decl = translationUnit.declarations[declHandle]
            when (val kind = decl.kind) {
                is GlobalDeclKind.Struct -> {
                    structMap[kind.struct.name!!] = declHandle
                }
                is GlobalDeclKind.TypeAlias -> {
                    typeAliasMap[kind.typeAlias.name] = declHandle
                }
                is GlobalDeclKind.Var -> {
                    variableMap[kind.var.name] = declHandle
                }
                is GlobalDeclKind.Const -> {
                    constMap[kind.const.name] = declHandle
                }
                is GlobalDeclKind.Function -> {
                    functionMap[kind.function.name] = declHandle
                }
                else -> {}
            }
        }
    }
    
    /**
     * Trouve une déclaration par nom.
     * Cherche dans l'ordre : variable, const, function, struct, type alias, builtin.
     */
    fun findDeclaration(name: String): Handle<GlobalDecl>? {
        return variableMap[name]
            ?: constMap[name]
            ?: functionMap[name]
            ?: structMap[name]
            ?: typeAliasMap[name]
    }
    
    /**
     * Trouve un type par nom.
     * Cherche dans l'ordre : type alias, struct, builtin.
     */
    fun findType(name: String): Handle<TypeDeclaration>? {
        // D'abord vérifier les type aliases
        val aliasDecl = typeAliasMap[name]
        if (aliasDecl != null) {
            val alias = (translationUnit.declarations[aliasDecl].kind as GlobalDeclKind.TypeAlias).typeAlias
            return alias.type
        }
        
        // Puis les structs
        val structDecl = structMap[name]
        if (structDecl != null) {
            val struct = (translationUnit.declarations[structDecl].kind as GlobalDeclKind.Struct).struct
            // Créer une référence au type struct
            // Note: Il faudrait retourner un TypeDeclaration.Struct
            // Mais pour l'instant, on retourne une référence
        }
        
        // Enfin les types builtin
        return builtinTypes[name]
    }
    
    /**
     * Vérifie si un nom est un type connu.
     */
    fun isKnownType(name: String): Boolean {
        return typeAliasMap.containsKey(name)
            || structMap.containsKey(name)
            || builtinTypes.containsKey(name)
    }
    
    /**
     * Vérifie si un nom est une variable/constante/fonction connue.
     */
    fun isKnownValue(name: String): Boolean {
        return variableMap.containsKey(name)
            || constMap.containsKey(name)
            || functionMap.containsKey(name)
    }
}
```

### 2. ModuleIndexer.kt (Ordonnancement du module)

**Fichier** : `wgsl:wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/ModuleIndexer.kt`

```kotlin
package io.ygdrasil.wgsl.front.wgsl

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.front.wgsl.ast.*

/**
 * Ordonnance les déclarations d'un module pour garantir que
 * chaque déclaration apparaît avant ses utilisations.
 * 
 * Implémente l'algorithme de "indexing" de Naga Rust.
 */
class ModuleIndexer(private val translationUnit: TranslationUnit) {
    
    /**
     * Réordonnance les déclarations du module.
     * Retourne une nouvelle liste de handles de déclarations dans le bon ordre.
     */
    fun reorderDeclarations(): List<Handle<GlobalDecl>> {
        // 1. Construire le graphe de dépendances
        val graph = buildDependencyGraph()
        
        // 2. Effectuer un topological sort
        val sorted = topologicalSort(graph)
        
        // 3. Retourner les déclarations dans l'ordre
        return sorted
    }
    
    /**
     * Construit le graphe de dépendances entre les déclarations.
     * Si declA utilise declB, alors declB doit venir avant declA.
     */
    private fun buildDependencyGraph(): Map<Handle<GlobalDecl>, Set<Handle<GlobalDecl>>> {
        val graph: MutableMap<Handle<GlobalDecl>, MutableSet<Handle<GlobalDecl>>> = mutableMapOf()
        
        // Initialiser tous les nodes
        for (declHandle in translationUnit.declarations) {
            graph[declHandle] = mutableSetOf()
        }
        
        // Pour chaque déclaration, trouver ses dépendances
        for (declHandle in translationUnit.declarations) {
            val decl = translationUnit.declarations[declHandle]
            val dependencies = findDependencies(decl)
            
            for (dep in dependencies) {
                if (dep != declHandle) {
                    // decl dépend de dep, donc dep doit venir avant decl
                    graph[declHandle]?.add(dep)
                }
            }
        }
        
        return graph
    }
    
    /**
     * Trouve toutes les dépendances d'une déclaration.
     */
    private fun findDependencies(decl: GlobalDecl): Set<Handle<GlobalDecl>> {
        val dependencies = mutableSetOf<Handle<GlobalDecl>>()
        
        when (val kind = decl.kind) {
            is GlobalDeclKind.Function -> {
                findDependenciesInFunction(kind.function, dependencies)
            }
            is GlobalDeclKind.Struct -> {
                findDependenciesInStruct(kind.struct, dependencies)
            }
            is GlobalDeclKind.TypeAlias -> {
                findDependenciesInType(kind.typeAlias.type, dependencies)
            }
            is GlobalDeclKind.Var -> {
                findDependenciesInVariable(kind.var, dependencies)
            }
            is GlobalDeclKind.Const -> {
                findDependenciesInConstant(kind.const, dependencies)
            }
            else -> {}
        }
        
        return dependencies
    }
    
    private fun findDependenciesInFunction(func: Function, dependencies: MutableSet<Handle<GlobalDecl>>) {
        // Les types des paramètres
        for (param in func.parameters) {
            findDependenciesInType(param.type, dependencies)
        }
        
        // Le type de retour
        if (func.returnType != null) {
            findDependenciesInType(func.returnType!!, dependencies)
        }
        
        // Les dépendances des dépendances (meta)
        for (dep in func.dependencies) {
            // dep est une Dependency, il faut trouver la déclaration correspondante
        }
        
        // Les expressions dans le body
        findDependenciesInStatements(func.body.statements, dependencies)
    }
    
    private fun findDependenciesInStatements(
        statements: List<Handle<Statement>>,
        dependencies: MutableSet<Handle<GlobalDecl>>
    ) {
        for (stmtHandle in statements) {
            val stmt = translationUnit.statements[stmtHandle]
            findDependenciesInStatement(stmt, dependencies)
        }
    }
    
    private fun findDependenciesInStatement(
        stmt: Statement,
        dependencies: MutableSet<Handle<GlobalDecl>>
    ) {
        when (stmt) {
            is Statement.Block -> {
                findDependenciesInStatements(stmt.statements, dependencies)
            }
            is Statement.If -> {
                findDependenciesInExpression(stmt.condition, dependencies)
                findDependenciesInStatement(stmt.accept, dependencies)
                if (stmt.reject != null) {
                    findDependenciesInStatement(stmt.reject!!, dependencies)
                }
            }
            is Statement.LetDecl -> {
                if (stmt.type != null) {
                    findDependenciesInType(stmt.type!!, dependencies)
                }
                findDependenciesInExpression(stmt.init, dependencies)
            }
            is Statement.Assignment -> {
                findDependenciesInExpression(stmt.lhs, dependencies)
                findDependenciesInExpression(stmt.rhs, dependencies)
            }
            is Statement.Return -> {
                if (stmt.expr != null) {
                    findDependenciesInExpression(stmt.expr!!, dependencies)
                }
            }
            // ... autres types de statements
            else -> {}
        }
    }
    
    private fun findDependenciesInExpression(
        exprHandle: Handle<Expression>,
        dependencies: MutableSet<Handle<GlobalDecl>>
    ) {
        val expr = translationUnit.expressions[exprHandle]
        
        when (expr) {
            is Expression.Ident -> {
                if (expr.ident is IdentExpr.Unresolved) {
                    val name = (expr.ident as IdentExpr.Unresolved).name
                    // Trouver la déclaration correspondante
                    val decl = findDeclarationByName(name)
                    if (decl != null) {
                        dependencies.add(decl)
                    }
                }
            }
            is Expression.Call -> {
                // Résoudre la fonction appelée
                if (expr.call.function.ident is IdentExpr.Unresolved) {
                    val name = (expr.call.function.ident as IdentExpr.Unresolved).name
                    val decl = findDeclarationByName(name)
                    if (decl != null) {
                        dependencies.add(decl)
                    }
                }
                // Résoudre les arguments
                for (arg in expr.call.arguments) {
                    findDependenciesInExpression(arg, dependencies)
                }
            }
            is Expression.Binary -> {
                findDependenciesInExpression(expr.left, dependencies)
                findDependenciesInExpression(expr.right, dependencies)
            }
            is Expression.Unary -> {
                findDependenciesInExpression(expr.expr, dependencies)
            }
            is Expression.Ternary -> {
                findDependenciesInExpression(expr.condition, dependencies)
                findDependenciesInExpression(expr.accept, dependencies)
                findDependenciesInExpression(expr.reject, dependencies)
            }
            is Expression.MemberAccess -> {
                findDependenciesInExpression(expr.expr, dependencies)
            }
            is Expression.Access -> {
                findDependenciesInExpression(expr.expr, dependencies)
                findDependenciesInExpression(expr.index, dependencies)
            }
            is Expression.As -> {
                findDependenciesInExpression(expr.expr, dependencies)
                findDependenciesInType(expr.type, dependencies)
            }
            is Expression.Compose -> {
                findDependenciesInType(expr.type, dependencies)
                for (arg in expr.arguments) {
                    findDependenciesInExpression(arg, dependencies)
                }
            }
            // ... autres types d'expressions
            else -> {}
        }
    }
    
    private fun findDependenciesInType(
        typeHandle: Handle<TypeDeclaration>,
        dependencies: MutableSet<Handle<GlobalDecl>>
    ) {
        val type = translationUnit.types[typeHandle]
        
        when (type) {
            is TypeDeclaration.Named -> {
                // Type nommé : struct, type alias, ou builtin
                if (type.name !in TypeIndex(translationUnit).builtinTypes) {
                    val decl = findDeclarationByName(type.name)
                    if (decl != null) {
                        dependencies.add(decl)
                    }
                }
                // Résoudre les template parameters
                for (param in type.templateList) {
                    findDependenciesInExpression(param, dependencies)
                }
            }
            is TypeDeclaration.Vector -> {
                findDependenciesInType(type.scalar, dependencies)
            }
            is TypeDeclaration.Matrix -> {
                findDependenciesInType(type.scalar, dependencies)
            }
            is TypeDeclaration.Array -> {
                findDependenciesInType(type.element, dependencies)
                if (type.length != null) {
                    findDependenciesInExpression(type.length!!, dependencies)
                }
            }
            is TypeDeclaration.Struct -> {
                if (type.name != null) {
                    val decl = findDeclarationByName(type.name!!)
                    if (decl != null) {
                        dependencies.add(decl)
                    }
                }
                for (member in type.members) {
                    findDependenciesInType(member.type, dependencies)
                }
            }
            is TypeDeclaration.Pointer -> {
                findDependenciesInType(type.base, dependencies)
            }
            // ... autres types
            else -> {}
        }
    }
    
    private fun findDeclarationByName(name: String): Handle<GlobalDecl>? {
        val index = TypeIndex(translationUnit)
        return index.findDeclaration(name)
    }
    
    /**
     * Effectue un topological sort sur un graphe de dépendances.
     * Utilise l'algorithme de Kahn.
     */
    private fun topologicalSort(
        graph: Map<Handle<GlobalDecl>, Set<Handle<GlobalDecl>>>
    ): List<Handle<GlobalDecl>> {
        val inDegree: MutableMap<Handle<GlobalDecl>, Int> = mutableMapOf()
        val result = mutableListOf<Handle<GlobalDecl>>()
        val queue = ArrayDeque<Handle<GlobalDecl>>()
        
        // Calculer le in-degree pour chaque node
        for (node in graph.keys) {
            inDegree[node] = 0
        }
        
        for (node in graph.keys) {
            for (dep in graph[node] ?: emptySet()) {
                inDegree[dep] = inDegree.getOrDefault(dep, 0) + 1
            }
        }
        
        // Ajouter tous les nodes avec in-degree 0 à la queue
        for (node in inDegree.keys) {
            if (inDegree[node] == 0) {
                queue.add(node)
            }
        }
        
        // Traiter la queue
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)
            
            for (neighbor in graph[node] ?: emptySet()) {
                inDegree[neighbor] = inDegree[neighbor]!! - 1
                if (inDegree[neighbor] == 0) {
                    queue.add(neighbor)
                }
            }
        }
        
        // Vérifier s'il y a des cycles (ne devrait pas arriver en WGSL valide)
        if (result.size != graph.size) {
            throw IllegalStateException("Cyclic dependency detected in module")
        }
        
        return result
    }
}
```

### 3. TypeResolver.kt (Résolution finale des types)

**Fichier** : `wgsl:wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/TypeResolver.kt`

```kotlin
package io.ygdrasil.wgsl.front.wgsl

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.front.wgsl.ast.*

/**
 * Résout toutes les références de types dans un module.
 * Remplace les TypeDeclaration.Named par les types concrets.
 */
class TypeResolver(private val translationUnit: TranslationUnit) {
    
    private val typeIndex: TypeIndex = TypeIndex(translationUnit)
    private val errors: MutableList<TypeError> = mutableListOf()
    
    /**
     * Résout toutes les références de types dans le module.
     * Retourne true si tout a été résolu sans erreur.
     */
    fun resolve(): Boolean {
        // 1. Indexer les déclarations
        typeIndex.index()
        
        // 2. Ordonnancer les déclarations
        val indexer = ModuleIndexer(translationUnit)
        val orderedDeclarations = indexer.reorderDeclarations()
        
        // 3. Résoudre les types dans chaque déclaration (dans l'ordre)
        for (declHandle in orderedDeclarations) {
            val decl = translationUnit.declarations[declHandle]
            resolveInDecl(declHandle, decl)
        }
        
        // 4. Valider qu'il n'y a pas de références non résolues
        return validateResolution()
    }
    
    /**
     * Résout les types dans une déclaration.
     */
    private fun resolveInDecl(
        declHandle: Handle<GlobalDecl>,
        decl: GlobalDecl
    ) {
        when (val kind = decl.kind) {
            is GlobalDeclKind.Function -> {
                resolveInFunction(declHandle, kind.function)
            }
            is GlobalDeclKind.Struct -> {
                resolveInStruct(kind.struct)
            }
            is GlobalDeclKind.TypeAlias -> {
                resolveInTypeAlias(kind.typeAlias)
            }
            is GlobalDeclKind.Var -> {
                resolveInGlobalVariable(kind.var)
            }
            is GlobalDeclKind.Const -> {
                resolveInGlobalConstant(kind.const)
            }
            else -> {}
        }
    }
    
    private fun resolveInFunction(
        declHandle: Handle<GlobalDecl>,
        func: Function
    ) {
        // Résoudre les types des paramètres
        for (param in func.parameters) {
            param.type = resolveType(param.type)
        }
        
        // Résoudre le type de retour
        if (func.returnType != null) {
            func.returnType = resolveType(func.returnType!!)
        }
        
        // Résoudre les types dans le body
        resolveInStatements(func.body.statements)
    }
    
    private fun resolveInStruct(struct: Struct) {
        // Les structs sont déjà résolues (ce sont des déclarations)
        // Mais on doit résoudre les types de leurs membres
        for (member in struct.members) {
            member.type = resolveType(member.type)
        }
    }
    
    private fun resolveInTypeAlias(alias: TypeAlias) {
        alias.type = resolveType(alias.type)
    }
    
    private fun resolveInGlobalVariable(var: GlobalVariable) {
        var.type = resolveType(var.type)
        if (var.init != null) {
            resolveInExpression(var.init!!)
        }
    }
    
    private fun resolveInGlobalConstant(const: Const) {
        const.type = resolveType(const.type)
        resolveInExpression(const.init)
    }
    
    private fun resolveInStatements(statements: List<Handle<Statement>>) {
        for (stmtHandle in statements) {
            val stmt = translationUnit.statements[stmtHandle]
            resolveInStatement(stmt)
        }
    }
    
    private fun resolveInStatement(stmt: Statement) {
        when (stmt) {
            is Statement.Block -> {
                resolveInStatements(stmt.statements)
            }
            is Statement.If -> {
                resolveInExpression(stmt.condition)
                resolveInStatement(stmt.accept)
                if (stmt.reject != null) {
                    resolveInStatement(stmt.reject!!)
                }
            }
            is Statement.Switch -> {
                resolveInExpression(stmt.selector)
                for (case in stmt.body.cases) {
                    for (selector in case.selectors) {
                        resolveInExpression(selector)
                    }
                    resolveInStatement(case.body)
                }
            }
            is Statement.Loop -> {
                resolveInStatement(stmt.body)
                if (stmt.continuing != null) {
                    resolveInStatement(stmt.continuing.body)
                }
            }
            is Statement.While -> {
                resolveInExpression(stmt.condition)
                resolveInStatement(stmt.body)
                if (stmt.continuing != null) {
                    resolveInStatement(stmt.continuing.body)
                }
            }
            is Statement.For -> {
                if (stmt.init != null) {
                    resolveInStatement(stmt.init!!)
                }
                if (stmt.condition != null) {
                    resolveInExpression(stmt.condition!!)
                }
                if (stmt.update != null) {
                    resolveInStatement(stmt.update!!)
                }
                resolveInStatement(stmt.body)
                if (stmt.continuing != null) {
                    resolveInStatement(stmt.continuing.body)
                }
            }
            is Statement.LetDecl -> {
                if (stmt.type != null) {
                    stmt.type = resolveType(stmt.type!!)
                }
                resolveInExpression(stmt.init)
            }
            is Statement.ConstDecl -> {
                stmt.type = resolveType(stmt.type)
                resolveInExpression(stmt.init)
            }
            is Statement.VariableDecl -> {
                if (stmt.type != null) {
                    stmt.type = resolveType(stmt.type!!)
                }
                if (stmt.init != null) {
                    resolveInExpression(stmt.init!!)
                }
            }
            is Statement.Assignment -> {
                resolveInExpression(stmt.lhs)
                resolveInExpression(stmt.rhs)
            }
            is Statement.IncrementDecrement -> {
                resolveInExpression(stmt.expr)
            }
            is Statement.Return -> {
                if (stmt.expr != null) {
                    resolveInExpression(stmt.expr!!)
                }
            }
            is Statement.Emit -> {
                // Les ranges d'expressions
            }
            is Statement.Break, is Statement.Continue, is Statement.Discard -> {
                // Pas de types à résoudre
            }
        }
    }
    
    private fun resolveInExpression(exprHandle: Handle<Expression>) {
        val expr = translationUnit.expressions[exprHandle]
        
        when (expr) {
            is Expression.Literal -> {
                // Les littéraux ont des types implicites, pas besoin de résolution
                // Mais on peut déduire leur type
                expr.resolvedType = inferLiteralType(expr.value)
            }
            is Expression.Ident -> {
                resolveIdent(expr)
            }
            is Expression.Call -> {
                resolveCall(expr.call)
                for (arg in expr.call.arguments) {
                    resolveInExpression(arg)
                }
            }
            is Expression.Binary -> {
                resolveInExpression(expr.left)
                resolveInExpression(expr.right)
                // Le type du résultat dépend des types des opérandes
            }
            is Expression.Unary -> {
                resolveInExpression(expr.expr)
            }
            is Expression.Ternary -> {
                resolveInExpression(expr.condition)
                resolveInExpression(expr.accept)
                resolveInExpression(expr.reject)
            }
            is Expression.MemberAccess -> {
                resolveInExpression(expr.expr)
                // Le type du membre dépend du type de l'expression parente
            }
            is Expression.Access -> {
                resolveInExpression(expr.expr)
                resolveInExpression(expr.index)
            }
            is Expression.AccessIndex -> {
                resolveInExpression(expr.expr)
            }
            is Expression.Splat -> {
                resolveInExpression(expr.expr)
            }
            is Expression.As -> {
                resolveInExpression(expr.expr)
                expr.type = resolveType(expr.type)
            }
            is Expression.Compose -> {
                expr.type = resolveType(expr.type)
                for (arg in expr.arguments) {
                    resolveInExpression(arg)
                }
            }
            is Expression.Select -> {
                resolveInExpression(expr.condition)
                resolveInExpression(expr.accept)
                resolveInExpression(expr.reject)
            }
            is Expression.ArrayLength -> {
                resolveInExpression(expr.expr)
            }
        }
    }
    
    /**
     * Résout une référence de type.
     * Remplace les TypeDeclaration.Named par les types concrets.
     */
    private fun resolveType(typeHandle: Handle<TypeDeclaration>): Handle<TypeDeclaration> {
        val type = translationUnit.types[typeHandle]
        
        return when (type) {
            is TypeDeclaration.Named -> {
                resolveNamedType(type)
            }
            is TypeDeclaration.Scalar -> {
                typeHandle // Déjà résolu
            }
            is TypeDeclaration.Vector -> {
                // Résoudre le type scalaire
                val resolvedScalar = resolveType(type.scalar)
                if (resolvedScalar != type.scalar) {
                    // Créer une nouvelle TypeDeclaration.Vector avec le scalar résolu
                    val resolved = TypeDeclaration.Vector(
                        type.size,
                        resolvedScalar,
                        type.span
                    )
                    translationUnit.types.append(resolved)
                } else {
                    typeHandle
                }
            }
            is TypeDeclaration.Matrix -> {
                val resolvedScalar = resolveType(type.scalar)
                if (resolvedScalar != type.scalar) {
                    val resolved = TypeDeclaration.Matrix(
                        type.rows,
                        type.cols,
                        resolvedScalar,
                        type.span
                    )
                    translationUnit.types.append(resolved)
                } else {
                    typeHandle
                }
            }
            is TypeDeclaration.Array -> {
                val resolvedElement = resolveType(type.element)
                var resolvedLength = type.length
                if (resolvedLength != null) {
                    resolveInExpression(resolvedLength!!)
                }
                if (resolvedElement != type.element || resolvedLength != type.length) {
                    val resolved = TypeDeclaration.Array(
                        resolvedElement,
                        resolvedLength,
                        type.span
                    )
                    translationUnit.types.append(resolved)
                } else {
                    typeHandle
                }
            }
            is TypeDeclaration.Struct -> {
                // Les structs sont déjà indexées, pas besoin de résolution supplémentaire
                typeHandle
            }
            is TypeDeclaration.Pointer -> {
                val resolvedBase = resolveType(type.base)
                if (resolvedBase != type.base) {
                    val resolved = TypeDeclaration.Pointer(
                        resolvedBase,
                        type.access,
                        type.span
                    )
                    translationUnit.types.append(resolved)
                } else {
                    typeHandle
                }
            }
            else -> {
                typeHandle
            }
        }
    }
    
    /**
     * Résout un type nommé (struct, type alias, ou builtin).
     */
    private fun resolveNamedType(type: TypeDeclaration.Named): Handle<TypeDeclaration> {
        // Vérifier si c'est un type builtin spécial (vec2, vec3, mat4x4, etc.)
        when (type.name) {
            "vec2" -> {
                require(type.templateList.size == 1) { "vec2 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Vector(VectorSize.BI, scalar, type.span)
                )
            }
            "vec3" -> {
                require(type.templateList.size == 1) { "vec3 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Vector(VectorSize.TRI, scalar, type.span)
                )
            }
            "vec4" -> {
                require(type.templateList.size == 1) { "vec4 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Vector(VectorSize.QUAD, scalar, type.span)
                )
            }
            "mat2x2" -> {
                require(type.templateList.size == 1) { "mat2x2 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.BI, VectorSize.BI, scalar, type.span)
                )
            }
            "mat2x3" -> {
                require(type.templateList.size == 1) { "mat2x3 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.BI, VectorSize.TRI, scalar, type.span)
                )
            }
            "mat2x4" -> {
                require(type.templateList.size == 1) { "mat2x4 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.BI, VectorSize.QUAD, scalar, type.span)
                )
            }
            "mat3x2" -> {
                require(type.templateList.size == 1) { "mat3x2 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.TRI, VectorSize.BI, scalar, type.span)
                )
            }
            "mat3x3" -> {
                require(type.templateList.size == 1) { "mat3x3 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.TRI, VectorSize.TRI, scalar, type.span)
                )
            }
            "mat3x4" -> {
                require(type.templateList.size == 1) { "mat3x4 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.TRI, VectorSize.QUAD, scalar, type.span)
                )
            }
            "mat4x2" -> {
                require(type.templateList.size == 1) { "mat4x2 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.QUAD, VectorSize.BI, scalar, type.span)
                )
            }
            "mat4x3" -> {
                require(type.templateList.size == 1) { "mat4x3 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.QUAD, VectorSize.TRI, scalar, type.span)
                )
            }
            "mat4x4" -> {
                require(type.templateList.size == 1) { "mat4x4 requires exactly 1 template parameter" }
                val scalar = resolveExpressionToType(type.templateList[0])
                return translationUnit.types.append(
                    TypeDeclaration.Matrix(VectorSize.QUAD, VectorSize.QUAD, scalar, type.span)
                )
            }
            "array" -> {
                require(type.templateList.size == 2) { "array requires exactly 2 template parameters" }
                val element = resolveExpressionToType(type.templateList[0])
                val length = type.templateList[1]
                return translationUnit.types.append(
                    TypeDeclaration.Array(element, Some(length), type.span)
                )
            }
            else -> {
                // Chercher dans les déclarations
                val decl = typeIndex.findDeclaration(type.name)
                if (decl != null) {
                    when (val kind = translationUnit.declarations[decl].kind) {
                        is GlobalDeclKind.Struct -> {
                            // Retourner une référence au type struct
                            // Note: Il faudrait créer un TypeDeclaration.Struct
                            // qui référence la déclaration
                            return typeHandle // Pour l'instant, on retourne le named type
                        }
                        is GlobalDeclKind.TypeAlias -> {
                            // Déreferencer l'alias
                            return kind.typeAlias.type
                        }
                        else -> {
                            error("'$type.name' is not a type")
                            return typeHandle
                        }
                    }
                } else {
                    error("Unknown type: '${type.name}'")
                    return typeHandle
                }
            }
        }
    }
    
    /**
     * Convertit une expression en type.
     * Utilisé pour les template parameters qui sont des types.
     */
    private fun resolveExpressionToType(exprHandle: Handle<Expression>): Handle<TypeDeclaration> {
        val expr = translationUnit.expressions[exprHandle]
        
        return when (expr) {
            is Expression.Ident -> {
                if (expr.ident is IdentExpr.Unresolved) {
                    val name = (expr.ident as IdentExpr.Unresolved).name
                    // C'est une référence à un type par nom
                    val typeDecl = typeIndex.findType(name)
                    if (typeDecl != null) {
                        typeDecl
                    } else {
                        error("Unknown type: '$name'")
                        // Retourner un type par défaut
                        translationUnit.types.append(
                            TypeDeclaration.Scalar(ScalarKind.I32, expr.span)
                        )
                    }
                } else {
                    // C'est une référence à une variable, pas un type
                    error("Expected type, got value")
                    translationUnit.types.append(
                        TypeDeclaration.Scalar(ScalarKind.I32, expr.span)
                    )
                }
            }
            else -> {
                error("Expected type expression, got ${expr::class.simpleName}")
                translationUnit.types.append(
                    TypeDeclaration.Scalar(ScalarKind.I32, expr.span)
                )
            }
        }
    }
    
    /**
     * Résout un identifiant.
     */
    private fun resolveIdent(expr: Expression.Ident) {
        if (expr.ident is IdentExpr.Unresolved) {
            val name = (expr.ident as IdentExpr.Unresolved).name
            
            // Vérifier si c'est une valeur builtin
            if (isBuiltinValue(name)) {
                // Marquer comme builtin
                expr.ident = IdentExpr.BuiltIn(name)
                return
            }
            
            // Chercher dans les déclarations
            val decl = typeIndex.findDeclaration(name)
            if (decl != null) {
                when (val kind = translationUnit.declarations[decl].kind) {
                    is GlobalDeclKind.Function -> {
                        // C'est une fonction
                        expr.ident = IdentExpr.Function(decl)
                    }
                    is GlobalDeclKind.Var -> {
                        // C'est une variable globale
                        expr.ident = IdentExpr.Global(decl)
                    }
                    is GlobalDeclKind.Const -> {
                        // C'est une constante globale
                        expr.ident = IdentExpr.Global(decl)
                    }
                    is GlobalDeclKind.LetDecl -> {
                        // C'est une variable let globale
                        expr.ident = IdentExpr.Global(decl)
                    }
                    else -> {
                        error("'$name' is not a value")
                    }
                }
            } else {
                error("Unknown identifier: '$name'")
            }
        }
    }
    
    /**
     * Résout un appel de fonction.
     */
    private fun resolveCall(call: CallPhrase) {
        if (call.function.ident is IdentExpr.Unresolved) {
            val name = (call.function.ident as IdentExpr.Unresolved).name
            
            // Vérifier si c'est un constructeur builtin
            if (isBuiltinConstructor(name)) {
                // Marquer comme constructeur builtin
                call.function.ident = IdentExpr.BuiltInConstructor(name)
                return
            }
            
            // Chercher la fonction
            val decl = typeIndex.findDeclaration(name)
            if (decl != null) {
                when (val kind = translationUnit.declarations[decl].kind) {
                    is GlobalDeclKind.Function -> {
                        call.function.ident = IdentExpr.Function(decl)
                    }
                    else -> {
                        error("'${name}' is not a function")
                    }
                }
            } else {
                error("Unknown function: '$name'")
            }
        }
        
        // Résoudre les template parameters
        for (i in call.function.templateList.indices) {
            call.function.templateList[i] = resolveExpression(call.function.templateList[i])
        }
    }
    
    private fun resolveExpression(exprHandle: Handle<Expression>): Handle<Expression> {
        resolveInExpression(exprHandle)
        return exprHandle
    }
    
    /**
     * Infère le type d'un littéral.
     */
    private fun inferLiteralType(value: LiteralValue): Handle<TypeDeclaration> {
        return when (value) {
            is LiteralValue.Bool -> translationUnit.types.append(
                TypeDeclaration.Scalar(ScalarKind.BOOL, Span.INVALID)
            )
            is LiteralValue.I32 -> translationUnit.types.append(
                TypeDeclaration.Scalar(ScalarKind.I32, Span.INVALID)
            )
            is LiteralValue.U32 -> translationUnit.types.append(
                TypeDeclaration.Scalar(ScalarKind.U32, Span.INVALID)
            )
            is LiteralValue.F32 -> translationUnit.types.append(
                TypeDeclaration.Scalar(ScalarKind.F32, Span.INVALID)
            )
            is LiteralValue.F16 -> translationUnit.types.append(
                TypeDeclaration.Scalar(ScalarKind.F16, Span.INVALID)
            )
        }
    }
    
    /**
     * Vérifie si un nom est une valeur builtin.
     */
    private fun isBuiltinValue(name: String): Boolean {
        return when (name) {
            "true", "false" -> true
            else -> false
        }
    }
    
    /**
     * Vérifie si un nom est un constructeur builtin.
     */
    private fun isBuiltinConstructor(name: String): Boolean {
        return when (name) {
            "vec2", "vec3", "vec4",
            "mat2x2", "mat2x3", "mat2x4",
            "mat3x2", "mat3x3", "mat3x4",
            "mat4x2", "mat4x3", "mat4x4",
            "array", "bool", "i32", "u32", "f32", "f16",
            "true", "false" -> true
            else -> false
        }
    }
    
    /**
     * Valide que toutes les références ont été résolues.
     */
    private fun validateResolution(): Boolean {
        var hasErrors = false
        
        // Vérifier les types non résolus
        for (typeHandle in translationUnit.types) {
            val type = translationUnit.types[typeHandle]
            if (type is TypeDeclaration.Named) {
                error("Unresolved type: '${type.name}'")
                hasErrors = true
            }
        }
        
        // Vérifier les identifiants non résolus
        for (exprHandle in translationUnit.expressions) {
            val expr = translationUnit.expressions[exprHandle]
            if (expr is Expression.Ident && expr.ident is IdentExpr.Unresolved) {
                val name = (expr.ident as IdentExpr.Unresolved).name
                error("Unresolved identifier: '$name'")
                hasErrors = true
            }
        }
        
        return !hasErrors
    }
    
    private fun error(message: String) {
        errors.add(TypeError(message, Span.INVALID))
    }
}

/**
 * Erreur de résolution de type.
 */
data class TypeError(
    val message: String,
    val span: Span
)
```

---

## 📝 EXTENSIONS DE L'AST POUR LA RÉSOLUTION

### IdentExpr étendu

```kotlin
sealed class IdentExpr {
    /** Identifiant non résolu (nom seulement) */
    data class Unresolved(val name: String) : IdentExpr()
    
    /** Identifiant local (variable ou paramètre de fonction) */
    data class Local(val handle: Handle<Local>) : IdentExpr()
    
    /** Identifiant global (variable, constante, fonction) */
    data class Global(val handle: Handle<GlobalDecl>) : IdentExpr()
    
    /** Fonction */
    data class Function(val handle: Handle<GlobalDecl>) : IdentExpr()
    
    /** Valeur builtin (true, false) */
    data class BuiltIn(val name: String) : IdentExpr()
    
    /** Constructeur builtin (vec2, vec3, etc.) */
    data class BuiltInConstructor(val name: String) : IdentExpr()
}
```

---

## ✅ CHECKLIST PHASE 2.3

### Index et Résolution
- [ ] Implémenter `TypeIndex` avec toutes les maps
- [ ] Implémenter les types prédéclarés WGSL
- [ ] Implémenter `ModuleIndexer` avec topological sort
- [ ] Implémenter `TypeResolver` avec résolution complète
- [ ] Étendre `IdentExpr` pour supporter les références résolues
- [ ] Implémenter la détection des cycles

### Résolution des types spécifiques
- [ ] Types scalaires
- [ ] Types vecteurs (vec2, vec3, vec4)
- [ ] Types matrices (mat2x2, mat3x3, mat4x4, etc.)
- [ ] Types tableaux (array<T, N>)
- [ ] Types struct
- [ ] Types pointeurs
- [ ] Types templates
- [ ] Type aliases

### Validation
- [ ] Détecter les types non résolus
- [ ] Détecter les identifiants non résolus
- [ ] Détecter les fonctions non résolues
- [ ] Générer des erreurs avec spans correctes

### Tests
- [ ] TypeIndexTest
- [ ] ModuleIndexerTest
- [ ] TypeResolverTest
- [ ] Tests d'intégration

### Documentation
- [ ] KDoc complet
- [ ] Exemples de résolution
- [ ] Documentation des algorithmes

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Implémenter TypeIndex | 2 jours | AST complet | [ ] |
| Implémenter ModuleIndexer | 3 jours | TypeIndex | [ ] |
| Implémenter TypeResolver | 3 jours | ModuleIndexer | [ ] |
| Étendre IdentExpr | 1 jour | AST | [ ] |
| Implémenter les types prédéclarés | 2 jours | TypeIndex | [ ] |
| Implémenter la résolution des types builtin | 2 jours | TypeResolver | [ ] |
| Écrire les tests | 2 jours | Tout | [ ] |
| Documentation | 1 jour | Tout | [ ] |
| Validation manuelle | 1 jour | Tout | [ ] |

**Total estimé** : **1-2 semaines** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichiers Kotlin** :
   - `TypeIndex.kt`
   - `ModuleIndexer.kt`
   - `TypeResolver.kt`

2. **Fichiers modifiés** :
   - `IdentExpr.kt` (extensions)

3. **Tests unitaires** :
   - `TypeIndexTest.kt`
   - `ModuleIndexerTest.kt`
   - `TypeResolverTest.kt`

4. **Couverture de test** : > 95%

5. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Fichier Rust principal** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/index.rs`
- **AST Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/ast.rs`
- **Fichier précédent** : `02_ast-building.md`
- **Fichier suivant** : `04_error-handling.md`

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Finaliser la conception de TypeIndex
2. [ ] Implémenter TypeIndex avec tous les types prédéclarés
3. [ ] Implémenter ModuleIndexer avec topological sort
4. [ ] Implémenter TypeResolver
5. [ ] Étendre IdentExpr pour les références résolues
6. [ ] Écrire tous les tests
7. [ ] Valider avec des tests manuels
8. [ ] Passer à `04_error-handling.md`
