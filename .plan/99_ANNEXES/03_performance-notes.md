# Notes sur les Optimisations de Performance

## Table des Matières

1. [Contexte et Objectifs](#contexte-et-objectifs)
2. [Analyse des Contraintes](#analyse-des-contraintes)
3. [Stratégies d'Optimisation par Module](#stratégies-doptimisation-par-module)
4. [Comparaison Rust vs Kotlin](#comparaison-rust-vs-kotlin)
5. [Benchmark et Mesures](#benchmark-et-mesures)
6. [Bonnes Pratiques Kotlin](#bonnes-pratiques-kotlin)
7. [Points d'Attention Spécifiques](#points-dattention-spécifiques)
8. [Références](#références)

---

## Contexte et Objectifs

### Contexte
Le projet **WebGPU-KTypes** est un outil de **build-time** (exécuté pendant la compilation). Contrairement aux applications runtime, les contraintes de performance sont différentes :

- **Temps d'exécution acceptable** : 3-10x plus lent que Naga Rust est toléré
- **Mémoire** : Consommation modérée (pas de contraintes strictes)
- **Objectif principal** : **Correction fonctionnelle** avant optimisation

### Objectifs de Performance
| Métrique | Cible | Priorité |
|----------|-------|----------|
| Temps de parsing (1000 lignes WGSL) | < 1 seconde | Moyenne |
| Temps de conversion IR → MSL | < 500ms | Moyenne |
| Mémoire par module | < 50MB | Faible |
| Temps de build complet (100 shaders) | < 10 secondes | Élevée |

### Philosophie
> **"Make it work, make it right, make it fast"**

1. **D'abord** : Implémentation fonctionnelle correcte
2. **Ensuite** : Correction des bugs et validation
3. **Enfin** : Optimisation des performances

---

## Analyse des Contraintes

### Contraintes JVM/Kotlin
| Contrainte | Impact | Solution |
|------------|--------|----------|
| **Garbage Collector** | Latence imprévisible | Minimiser les allocations |
| **Pas de contrôle fin sur la mémoire** | Moins d'optimisations low-level | Utiliser des structures efficaces |
| **Pas d'inline assembly** | Impossible d'optimiser manuellement | Compter sur le JIT |
| **JVM Warmup** | Performance variable au démarrage | Pré-compiler si nécessaire |

### Contraintes Spécifiques au Projet
- **Build-time tool** : Pas besoin de performance runtime
- **Taille des inputs** : Shaders typiquement < 1000 lignes
- **Nombre de shaders** : Compilation par lots (10-100 shaders)
- **Usage** : Une fois par build, pas en boucle serrée

---

## Stratégies d'Optimisation par Module

### 1. Module Core (IR)

#### Arena/Handle System
| Technique | Impact | Implémentation |
|-----------|--------|----------------|
| **@JvmInline value class** | Élimine l'allocation pour Handle<T> | `value class Handle<T>(val index: Int)` |
| **Stockage contigu** | Cache-friendly | `MutableList<T>` au lieu de `HashMap` |
| **Accès par index** | O(1) garanti | `Arena<T>[handle]` |

**Exemple d'optimisation :**
```kotlin
// ❌ Mauvais - allocation d'objet
class Handle<T>(val index: Int)

// ✅ Bon - @JvmInline évite l'allocation
@JvmInline
value class Handle<T>(val index: Int)
```

#### UniqueArena
| Technique | Impact | Implémentation |
|-----------|--------|----------------|
| **HashMap pour déduplication** | O(1) lookup | `MutableMap<T, Int>` |
| **Comparaison par équivalence** | Évite les doublons | `Equatable` interface |
| **Cache des résultats** | Réutilisation | Mémoization si nécessaire |

### 2. Module WGSL (Parser)

#### Lexer
| Technique | Impact | Implémentation |
|-----------|--------|----------------|
| **Table de lookup** | O(1) pour token recognition | `Map<String, TokenKind>` |
| **Buffering** | Réduit les I/O | `String` entier chargé en mémoire |
| **State machine** | Parsing efficace | Switch sur current char |

**Exemple :**
```kotlin
// ❌ Mauvais - concaténation de strings
fun readIdentifier(): String {
    var result = ""
    while (hasNext()) { result += nextChar() }
    return result
}

// ✅ Bon - StringBuilder
fun readIdentifier(): String {
    val sb = StringBuilder()
    while (hasNext()) { sb.append(nextChar()) }
    return sb.toString()
}
```

#### Parser
| Technique | Impact | Implémentation |
|-----------|--------|----------------|
| **Recursive Descent** | Simple et efficace | Fonctions pour chaque règle |
| **Éviter la récursion profonde** | Pas de stack overflow | Iterative pour les listes |
| **Allocation minimale** | Réduit GC pressure | Réutiliser les objets |

### 3. Modules Backend (MSL, HLSL, GLSL)

#### Code Generation
| Technique | Impact | Implémentation |
|-----------|--------|----------------|
| **StringBuilder** | Évite les allocations | `StringBuilder` pour le code |
| **Cache des templates** | Réutilisation | Pré-compiler les patterns |
| **Visiteur pattern** | Évite la duplication | `BackendWriter` interface |

**Exemple :**
```kotlin
// ❌ Mauvais - concaténation
fun generate(): String {
    var code = ""
    for (stmt in statements) {
        code += generateStatement(stmt) + ";\n"
    }
    return code
}

// ✅ Bon - StringBuilder
fun generate(): String {
    val sb = StringBuilder()
    for (stmt in statements) {
        sb.append(generateStatement(stmt)).append(";\n")
    }
    return sb.toString()
}
```

### 4. Module Processing (ConstantEvaluator, Typifier, etc.)

#### ConstantEvaluator
| Technique | Impact | Implémentation |
|-----------|--------|----------------|
| **Memoization** | Évite les calculs redondants | `MutableMap<Expr, Value>` |
| **Évaluation lazy** | Ne calcule que si nécessaire | `lazy { compute() }` |
| **Cache des résultats** | Persistance entre appels | Cache global |

#### Typifier
| Technique | Impact | Implémentation |
|-----------|--------|----------------|
| **Union-Find** | Gestion efficace des types | Pour la déduplication |
| **Type inference** | Réduit les annotations | Algorithm de Hindley-Milner adapté |

---

## Comparaison Rust vs Kotlin

### Points Forts de Rust (vs Kotlin)
| Caractéristique | Rust | Kotlin | Impact |
|-----------------|------|--------|--------|
| **No GC** | ✅ | ❌ | Allocations plus contrôlées |
| **Stack allocation** | ✅ | ❌ | Moins de pressure mémoire |
| **Zero-cost abstractions** | ✅ | ⚠️ Partiel | Inline, generics |
| **Pattern matching** | ✅ `match` | ✅ `when` | Équivalent |
| **Iterators** | ✅ | ✅ | Équivalent |

### Points Forts de Kotlin (vs Rust)
| Caractéristique | Kotlin | Rust | Impact |
|-----------------|--------|------|--------|
| **JIT Optimization** | ✅ HotSpot | ❌ | Performance après warmup |
| **Simpler code** | ✅ | ⚠️ Complexe | Productivité |
| **GC automatic** | ✅ | ❌ | Moins de bugs mémoire |
| **Interop Java** | ✅ | ❌ | Accès aux librairies Java |

### Benchmark Attendu
| Opération | Naga Rust | WebGPU-KTypes | Ratio |
|-----------|-----------|---------------|-------|
| Parsing WGSL (100 lignes) | 5ms | 20-50ms | 4-10x |
| IR → MSL (shader moyen) | 10ms | 30-100ms | 3-10x |
| Validation complète | 15ms | 50-150ms | 3-10x |

**→ Tout est dans les limites acceptables (3-10x plus lent)**

---

## Benchmark et Mesures

### Outils de Benchmark
1. **Kotlin Built-in** : `kotlinx.benchmark` library
2. **JMH** : Java Microbenchmark Harness (plus précis)
3. **Manual** : `System.nanoTime()`

### Métriques à Mesurer
| Métrique | Méthode | Fréquence |
|----------|---------|-----------|
| Temps de parsing | Mesurer sur 100 shaders | Chaque commit |
| Temps de conversion | MSL, HLSL, GLSL séparément | Chaque release |
| Mémoire utilisée | Runtime.getRuntime().totalMemory() | Si problème |
| Taille du JAR | `du -sh build/libs/` | Release |

### Exemple de Benchmark JMH
```kotlin
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
class WgslParserBenchmark {
    private lateinit var wgslSource: String
    
    @Setup
    fun setup() {
        wgslSource = loadTestShader("complex.wgsl") // 1000+ lignes
    }
    
    @Benchmark
    fun parseWgsl() {
        val parser = WgslParser()
        parser.parse(wgslSource)
    }
}
```

### Configuration Gradle pour JMH
```kotlin
// build.gradle.kts
plugins {
    id("org.openjdk.jmh") version "1.36"
}

dependencies {
    implementation("org.openjdk.jmh:jmh-core:1.36")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.36")
}
```

---

## Bonnes Pratiques Kotlin

### 1. Éviter les Allocations Inutiles

#### Collections
```kotlin
// ❌ Mauvais - crée une nouvelle liste
fun process(items: List<T>): List<T> {
    return items.map { transform(it) }
}

// ✅ Bon - utilise la liste existante si mutable
fun process(items: MutableList<T>) {
    val iterator = items.iterator()
    while (iterator.hasNext()) {
        val item = iterator.next()
        iterator.set(transform(item))
    }
}
```

#### Strings
```kotlin
// ❌ Mauvais - crée des strings intermédiaires
fun buildPath(a: String, b: String, c: String): String {
    return "$a/$b/$c"
}

// ✅ Bon - StringBuilder pour les concaténations multiples
fun buildPath(a: String, b: String, c: String): String {
    return StringBuilder().apply {
        append(a).append('/').append(b).append('/').append(c)
    }.toString()
}
```

### 2. Utiliser les Structures de Données Efficaces

#### Array vs List
```kotlin
// ✅ Array - allocation contiguë, accès rapide
val array: Array<Int> = Array(1000) { 0 }

// ⚠️ MutableList - peut être moins efficace
val list: MutableList<Int> = MutableList(1000) { 0 }
```

**→ Utiliser `Array` quand la taille est fixe et connue**

#### Primitive Types
```kotlin
// ✅ IntArray, FloatArray, etc. - pas de boxing
val ints: IntArray = IntArray(1000)

// ⚠️ List<Int> - boxing des Int
val ints: List<Int> = List(1000) { 0 }
```

**→ Utiliser `XxxArray` pour les types primitifs**

### 3. Optimiser les Boucles

```kotlin
// ❌ Mauvais - itération avec opérations coûteuses
for (i in 0 until list.size) {
    val item = list[i]
    if (item.isValid()) {
        process(item)
    }
}

// ✅ Bon - itération directe
for (item in list) {
    if (item.isValid()) {
        process(item)
    }
}

// ✅ Meilleur - inline function
list.forEach { item ->
    if (item.isValid()) process(item)
}
```

### 4. Lazy Initialization

```kotlin
// ❌ Mauvais - initialisation immédiatement
class Parser {
    private val expensiveMap: Map<String, TokenKind> = createTokenMap()
}

// ✅ Bon - lazy initialization
class Parser {
    private val expensiveMap: Map<String, TokenKind> by lazy { createTokenMap() }
}
```

### 5. Inline Functions

```kotlin
// ✅ Pour les fonctions fréquemment appelées
inline fun <T> Arena<T>.forEachWithHandle(action: (Handle<T>, T) -> Unit) {
    data.forEachIndexed { index, value ->
        action(Handle.fromIndex(index), value)
    }
}
```

---

## Points d'Attention Spécifiques

### 1. UniqueArena et Dédoublonnage
**Problème** : Le `HashMap` dans `UniqueArena` peut devenir un goulot d'étranglement.

**Solution** :
- Utiliser un `HashMap` avec une bonne fonction de hash
- Pour les types, utiliser une comparaison structurelle
- Éviter de vérifier l'équivalence pour les grands objets

**Exemple :**
```kotlin
class UniqueArena<T> where T : Equatable {
    private val data: MutableList<T> = mutableListOf()
    // ✅ HashMap pour O(1) lookup
    private val indexMap: MutableMap<T, Int> = mutableMapOf()
    
    fun append(value: T): Handle<T> {
        return indexMap.getOrPut(value) {
            val index = data.size
            data.add(value)
            index
        }.let { Handle.fromIndex(it) }
    }
}
```

### 2. Parsing de Gros Shaders
**Problème** : Les shaders peuvent avoir des milliers de tokens.

**Solution** :
- Utiliser un `CharArray` ou `String` avec accès par index
- Éviter de créer des objets intermédiaires
- Bufferer les tokens dans un tableau pré-alloué

### 3. Génération de Code Backend
**Problème** : La concaténation de strings est coûteuse.

**Solution** :
- Toujours utiliser `StringBuilder`
- Pré-allouer la taille si possible
- Éviter les `+=` sur les strings

### 4. Sérialisation/Desérialisation
**Problème** : Kotlinx.serialization peut être lent.

**Solution** :
- Utiliser des formats binaires si possible
- Éviter la sérialisation pendant le processing
- Cache les résultats sérialisés

---

## Références

### Outils
- [Kotlinx Benchmark](https://github.com/Kotlin/kotlinx-benchmark)
- [JMH - Java Microbenchmark Harness](https://openjdk.org/projects/code-tools/jmh/)
- [VisualVM - Profiling JVM](https://visualvm.github.io/)
- [YourKit Java Profiler](https://www.yourkit.com/)

### Bonnes Pratiques
- [Kotlin Performance Guide](https://kotlinlang.org/docs/performance.html)
- [Effective Kotlin - Item 43: Prefer lazy initialization](https://kotlinlang.org/effective-kotlin/lazy-initialization.html)
- [JVM Performance Tuning](https://www.oracle.com/java/technologies/javase/performance-tuning.html)

### Comparaison avec Rust
- [Rust Performance Patterns](https://nnethercote.github.io/perf-book/)
- [Are we fast yet? - Rust benchmarks](https://arewefasyet.rs/)
