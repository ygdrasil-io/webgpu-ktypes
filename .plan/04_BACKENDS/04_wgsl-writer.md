# 🔄 Phase 4.4 : WGSL Writer

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-wgsl`  
**Phase** : 4 - Backends  
**Sous-Phase** : 4.4 - Backend WGSL (Output)  
**Durée** : 2-3 semaines  
**Priorité** : ⭐⭐⭐⭐ (Important - Round-trip validation)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/`
> **Spec** : [WGSL Specification](https://gpuweb.github.io/gpuweb/wgsl/)

---

## 📋 OBJECTIFS

Implémenter le backend **WGSL** qui transforme l'IR Naga en code source WGSL. Ce backend est essentiel pour :
- La **validation round-trip** (WGSL → IR → WGSL)
- Le débogage et l'inspection de l'IR
- L'export vers d'autres outils WebGPU

**Livrable** : Module `naga-wgsl` capable de générer du code WGSL valide et idiomatique.

---

## 🎯 CONCEPTS CLÉS

### 1. WGSL en bref
- **Langage** : WebGPU Shading Language
- **Spécification** : [gpuweb.github.io/gpuweb/wgsl/](https://gpuweb.github.io/gpuweb/wgsl/)
- **Objectif** : Langage unique pour WebGPU, conçu pour être lisible par les humains et les machines
- **Design** : Inspiré de Rust, HLSL, GLSL

### 2. Round-Trip Validation

Le backend WGSL permet de valider que la transformation WGSL → IR → WGSL produit le même code (ou équivalent sémantiquement).

**Processus:**
```
WGSL Source
    ↓
Parser WGSL → IR
    ↓
Validator
    ↓
WGSL Writer → WGSL Output
    ↓
Comparaison avec le source original
```

### 3. Style de sortie

Le code WGSL généré doit être :
- **Valide** : Respecter la grammaire WGSL
- **Lisible** : Bien formaté avec indentation cohérente
- **Idiomatique** : Utiliser les conventions WGSL (ex: `snake_case` pour les variables)
- **Déterministe** : Le même IR produit toujours le même code

### 4. Options de formatage

| Option | Description | Valeur par défaut |
|--------|-------------|------------------|
| `prettyPrint` | Activer le pretty printing | true |
| `indent` | Indentation | 4 espaces |
| `debug` | Inclure des commentaires de débogage | false |
| `version` | Version WGSL (non utilisé pour l'instant) | null |

### 5. Gestion des noms

- Utiliser le `Namer` pour générer des noms uniques
- Respecter les noms originaux si présents
- Générer des noms `snake_case` pour les entités anonymes
- Éviter les mots-clés WGSL

### 6. Dépendances entre déclarations

WGSL requiert que les déclarations soient dans un ordre spécifique :
1. `enable` directives
2. `requires` directives
3. Types (`type` alias)
4. Structs
5. Constantes
6. Variables globales
7. Fonctions
8. Entry points

Le writer doit trier les déclarations dans le bon ordre.

---

## 📦 STRUCTURE DES FICHIERS

```
naga-wgsl/
├── build.gradle.kts
└── src/main/kotlin/dev/gfxrs/naga/back/wgsl/
    ├── WgslOptions.kt     # Options WGSL
    ├── WgslWriter.kt      # Writer principal
    ├── WgslModule.kt      # API publique
    ├── Formatter.kt       # Formatage du code
    └── Sorter.kt          # Tri des déclarations
```

---

## 🎯 IMPLÉMENTATION

### 1. WgslOptions.kt

```kotlin
data class WgslOptions(
    override val validationFlags: ValidationFlags = ValidationFlags.ALL,
    override val capabilities: Capabilities = Capabilities.ALL, // WGSL supporte tout
    override val shaderStages: ShaderStages = ShaderStages.ALL,
    override val indent: String = "    ",
    override val newline: String = "\n",
    override val version: String? = null,  // WGSL n'a pas de version pour l'instant
    override val languageName: String = "WGSL",
    override val fileExtension: String = ".wgsl",
    
    // Options spécifiques WGSL
    val prettyPrint: Boolean = true,
    val debug: Boolean = false,
    val lineLength: Int = 100,
    val spaceBeforeColon: Boolean = true,
    val spaceAfterComma: Boolean = true
) : BackendOptions()
```

### 2. Sorter.kt (Tri des déclarations)

WGSL a des règles d'ordre spécifiques. Le sorter garantit que les déclarations sont dans le bon ordre.

```kotlin
class DeclarationSorter {
    
    sealed class Declaration {
        data class Enable(val name: String) : Declaration()
        data class Requires(val name: String) : Declaration()
        data class TypeAlias(val name: String, val type: Handle<Type>) : Declaration()
        data class Struct(val name: String, val handle: Handle<Type>) : Declaration()
        data class Constant(val name: String, val handle: Handle<Constant>) : Declaration()
        data class GlobalVariable(val name: String, val handle: Handle<GlobalVariable>) : Declaration()
        data class Function(val name: String, val handle: Handle<Function>) : Declaration()
        data class EntryPoint(val index: Int, val handle: EntryPoint) : Declaration()
    }
    
    private val declarations: MutableList<Declaration> = mutableListOf()
    
    fun add(declaration: Declaration) {
        declarations.add(declaration)
    }
    
    fun sort(): List<Declaration> {
        // WGSL ordre:
        // 1. enable
        // 2. requires
        // 3. type (type aliases)
        // 4. struct
        // 5. const
        // 6. var (global variables)
        // 7. fn (functions, entry points)
        
        val order = listOf(
            Declaration::class.sealedSubclasses.find { it.simpleName == "Enable" }!!,
            Declaration::class.sealedSubclasses.find { it.simpleName == "Requires" }!!,
            Declaration::class.sealedSubclasses.find { it.simpleName == "TypeAlias" }!!,
            Declaration::class.sealedSubclasses.find { it.simpleName == "Struct" }!!,
            Declaration::class.sealedSubclasses.find { it.simpleName == "Constant" }!!,
            Declaration::class.sealedSubclasses.find { it.simpleName == "GlobalVariable" }!!,
            Declaration::class.sealedSubclasses.find { it.simpleName == "Function" }!!,
            Declaration::class.sealedSubclasses.find { it.simpleName == "EntryPoint" }!!
        )
        
        return declarations.sortedBy { decl ->
            order.indexOf(decl::class)
        }
    }
}
```

### 3. Formatter.kt

Gère le formatage du code WGSL (indentation, espaces, sauts de ligne).

```kotlin
class WgslFormatter(
    val options: WgslOptions
) {
    
    fun formatType(type: Handle<Type>): String {
        // Formater un type avec les bonnes options
    }
    
    fun formatExpression(expr: Handle<Expression>): String {
        // Formater une expression
    }
    
    fun formatStatement(stmt: Handle<Statement>, indentLevel: Int): String {
        // Formater un statement avec indentation
    }
    
    fun formatFunction(func: Function, name: String, indentLevel: Int): String {
        // Formater une fonction complète
    }
    
    fun addIndentation(code: String, level: Int): String {
        return code.split(options.newline).joinToString(options.newline) { line ->
            "${options.indent.repeat(level)}$line"
        }
    }
    
    fun wrapLine(code: String, maxLength: Int): String {
        // Envelopper les lignes trop longues
    }
}
```

### 4. WgslWriter.kt

Hérite de `WriterBase<WgslOptions>`.

**Header:**
```wgsl
// WGSL Shader
// Generated by WebGPU-KTypes
// From: <original file if debug>

// enable/disable directives
// requires directives
```

**Types:**
```wgsl
// Type aliases
type MyType = struct { ... };

// Ou utiliser les noms originaux
```

**Expressions:** Générer du code WGSL idiomatique.

**Statements:** Générer du code WGSL idiomatique.

**Fonctions:**
```wgsl
fn myFunction(arg1: i32, arg2: f32) -> f32 {
    return arg2 * float(arg1);
}
```

**Entry Points:**
```wgsl
@vertex
fn vertexMain(
    @location(0) position: vec4<f32>
) -> @builtin(position) vec4<f32> {
    return position;
}
```

**Particularités WGSL:**
- Utiliser `@` pour les attributs
- Utiliser `->` pour le type de retour
- Utiliser `:` pour les annotations de type
- Utiliser `let` pour les variables locales
- Utiliser `const` pour les constantes
- Utiliser `var` pour les variables mutables

### 5. WgslModule.kt

```kotlin
object WgslModule {
    fun writeString(module: Module, options: WgslOptions = WgslOptions()): String
    
    /**
     * Valide le round-trip: parse → write → parse
     */
    fun validateRoundTrip(wgslSource: String, options: WgslOptions = WgslOptions()): Result<Module, List<Error>> {
        // 1. Parser le source
        val module1 = WgslParser.parse(wgslSource)
        
        // 2. Générer le code
        val generated = writeString(module1, options)
        
        // 3. Re-parser le code généré
        val module2 = WgslParser.parse(generated)
        
        // 4. Comparer les modules (sémantiquement)
        // Note: Une comparaison exacte est difficile à cause des noms générés
        // On compare le comportement sémantique
        
        return Result.success(module2)
    }
    
    /**
     * Compare deux modules pour l'égalité sémantique
     */
    fun areEquivalent(module1: Module, module2: Module): Boolean {
        // Comparaison sémantique (pas juste structurale)
        // Vérifier:
        // - Même nombre de types, fonctions, etc.
        // - Types équivalents
        // - Fonctions équivalentes
        // - Entry points équivalents
        return true // Implémentation à faire
    }
}
```

---

## ✅ CHECKLIST

### Configuration
- [ ] WgslOptions.kt

### Utilitaires
- [ ] Sorter.kt (tri des déclarations)
- [ ] Formatter.kt (formatage du code)

### Writer
- [ ] WgslWriter.kt (base)
- [ ] writeHeader()
- [ ] writeEnableDirectives()
- [ ] writeRequiresDirectives()
- [ ] writeTypeAlias()
- [ ] getTypeName() pour tous les types WGSL
- [ ] writeStructType()
- [ ] writeConstant()
- [ ] writeGlobalVariable()
- [ ] writeFunction()
- [ ] writeFunctionSignature()
- [ ] writeEntryPoint()
- [ ] Toutes les writeXxx() pour expressions
- [ ] Toutes les writeXxx() pour statements

### Formattage
- [ ] Indentation cohérente
- [ ] Espaces avant/après les opérateurs
- [ ] Sauts de ligne appropriés
- [ ] Alignement des déclarations
- [ ] Enveloppement des lignes longues

### Round-Trip Validation
- [ ] validateRoundTrip()
- [ ] areEquivalent() pour la comparaison sémantique
- [ ] Tests de round-trip

### Tests
- [ ] Module vide
- [ ] Types (tous)
- [ ] Constantes
- [ ] Variables globales
- [ ] Fonctions
- [ ] Entry points
- [ ] Expressions (toutes)
- [ ] Statements (tous)
- [ ] Round-trip (parse → write → parse)

### Intégration
- [ ] build.gradle.kts
- [ ] Enregistrer dans BackendRegistry
- [ ] Documentation

---

## 📖 RÉFÉRENCES

1. **WGSL Specification**: [gpuweb.github.io/gpuweb/wgsl/](https://gpuweb.github.io/gpuweb/wgsl/)
2. **Rust WGSL Backend**: `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/`
3. **Rust Writer**: `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/writer.rs` (~800 lignes)
4. **WGSL Grammar**: [WGSL Grammar](https://gpuweb.github.io/gpuweb/wgsl/#appendix-grammar)

---

## 🎯 PLANNING

| Tâche | Durée | Dépendances |
|-------|-------|-------------|
| WgslOptions.kt | 2h | BackendOptions |
| Sorter.kt | 4-6h | Aucune |
| Formatter.kt | 6-8h | WgslOptions |
| WgslWriter base + header | 8-12h | WriterBase |
| WgslWriter types | 8-12h | WgslWriter base |
| WgslWriter expressions | 16-20h | WgslWriter base |
| WgslWriter statements | 16-20h | WgslWriter base |
| WgslWriter functions + entry points | 8-12h | WgslWriter base |
| Round-trip validation | 8-12h | WgslWriter + Parser |
| WgslModule + tests | 8-12h | Tout |
| build.gradle.kts | 2h | Aucune |
| **Total** | **84-116h (2-3 semaines)** |

---

## 📝 NOTES

1. **Round-Trip Importance** : Le backend WGSL est essentiel pour valider que l'IR est correctement généré et que la transformation est réversible.

2. **Formatage** : Le code généré doit être lisible et bien formaté. C'est important pour le débogage.

3. **Déterminisme** : Le même IR doit toujours produire le même code WGSL. C'est important pour les tests.

4. **Comparaison sémantique** : Comparer deux modules WGSL pour l'équivalence sémantique est complexe. On peut commencer par une comparaison structurale simple.

5. **Validation** : Le code WGSL généré doit être valide selon le parser WGSL.

6. **Performance** : Le backend WGSL est utilisé pour le débogage et les tests, pas pour la production. La performance est moins critique que pour les autres backends.

7. **Debug Mode** : En mode debug, on peut inclure des commentaires dans le code généré pour aider au débogage.
