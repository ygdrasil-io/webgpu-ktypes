# Comparaison Rust ↔ Kotlin pour WebGPU-KTypes

## Sommaire

1. [Overview](#overview)
2. [Paradigmes de Programmation](#paradigmes-de-programmation)
3. [Comparaison des Concepts](#comparaison-des-concepts)
4. [Gestion de la Mémoire](#gestion-de-la-mémoire)
5. [Gestion des Erreurs](#gestion-des-erreurs)
6. [Système de Types](#système-de-types)
7. [Macros et Métaprogrammation](#macros-et-métaprogrammation)
8. [Pattern Matching](#pattern-matching)
9. [Concurrences](#concurrences)
10. [Écosystème et Outils](#écosystème-et-outils)
11. [Exemples de Portage](#exemples-de-portage)

---

## Overview

### Objectif

Ce document compare les concepts clés entre **Rust** (langage original de Naga) et **Kotlin** (langage cible pour WebGPU-KTypes), afin de faciliter le portage et de garantir une implémentation fidèle.

### Résumé des Différences Majeures

| Aspect | Rust | Kotlin | Impact sur le Portage |
|--------|------|--------|---------------------|
| **Paradigme** | Multi-paradigme (procédural, orienté objet, générique) | Multi-paradigme (OOP, fonctionnel) | Adaptation nécessaire des patterns |
| **Gestion mémoire** | Ownership, Borrow Checker, lifetimes | Garbage Collection (JVM) | Simplification mais moins de contrôle |
| **Sécurité mémoire** | Compile-time (100% safe) | Runtime (NullPointerException possible) | Ajouter des null checks |
| **Gestion des erreurs** | `Result<T, E>`, `Option<T>` | `Result<T>` (personnalisé), nullables | Adapter les patterns d'erreur |
| **Système de types** | Fort, statique, inférence | Fort, statique, inférence | Très similaire |
| **Generics** | Puissants, avec traits | Puissants, avec interfaces | Adaptation des traits → interfaces |
| **Macros** | Macros procédurales | Pas de macros | Réimplémenter sans macros |
| **Pattern Matching** | `match` expression | `when` expression | Très similaire |
| **Concurrency** | Threads, async/await | Coroutines, Flow | Utiliser les coroutines |
| **Interop** | FFI, unsafe | JNI, JVM | Différent mais possible |
| **Performance** | Natif, zero-cost | JVM, overhead | 3-10x plus lent acceptable |

---

## Paradigmes de Programmation

### Rust

```rust
// Paradigme procédural
fn add(a: i32, b: i32) -> i32 {
    a + b
}

// Paradigme orienté objet (avec traits)
trait Shape {
    fn area(&self) -> f64;
}

struct Circle {
    radius: f64,
}

impl Shape for Circle {
    fn area(&self) -> f64 {
        std::f64::consts::PI * self.radius * self.radius
    }
}

// Paradigme générique
fn max<T: PartialOrd>(a: T, b: T) -> T {
    if a > b { a } else { b }
}

// Paradigme fonctionnel
fn factorial(n: u32) -> u32 {
    match n {
        0 => 1,
        _ => n * factorial(n - 1),
    }
}

// Macros
macro_rules! vec {
    ($($x:expr),*) => {
        <[_]>::into_vec(Box::new([$($x),*]))
    };
}
```

### Kotlin

```kotlin
// Paradigme procédural (dans un object ou package)
fun add(a: Int, b: Int): Int = a + b

// Paradigme orienté objet (classique)
interface Shape {
    fun area(): Double
}

class Circle(val radius: Double) : Shape {
    override fun area(): Double = Math.PI * radius * radius
}

// Paradigme générique
fun <T : Comparable<T>> max(a: T, b: T): T = if (a > b) a else b

// Paradigme fonctionnel
fun factorial(n: Int): Int = when (n) {
    0 -> 1
    else -> n * factorial(n - 1)
}

// Pas de macros, mais :
// - Inline functions
// - Reified generics
// - Extension functions
inline fun <reified T> isA(value: Any): Boolean = value is T
```

### Tableau Comparatif

| Concept | Rust | Kotlin | Équivalence |
|---------|------|--------|-------------|
| Fonction | `fn` | `fun` | 1:1 |
| Classe | `struct`, `enum` | `class`, `data class` | `struct` → `data class` |
| Interface | `trait` | `interface` | `trait` → `interface` |
| Implémentation | `impl` | `:`, `override` | `impl Trait for Type` → `class Type : Trait` |
| Héritage | Pas d'héritage de struct | `open class`, `:` | Limité en Rust |
| Polymorphisme | Traits, enums | Interfaces, sealed classes | `enum` → `sealed class` |

---

## Comparaison des Concepts

### 1. Système de Modules

#### Rust
```rust
// Déclaration du module
mod frontends {
    pub mod wgsl {
        pub mod parser;
        pub mod lexer;
    }
}

// Utilisation
use frontends::wgsl::parser::parse;
use frontends::wgsl::lexer::Lexer;
```

#### Kotlin
```kotlin
// Structure des packages
package io.ygdrasil.wgsl.frontends.wgsl

// Fichiers séparés pour parser et lexer
// parser/Parser.kt
// lexer/Lexer.kt

// Utilisation
import io.ygdrasil.wgsl.frontends.wgsl.parser.parse
import io.ygdrasil.wgsl.frontends.wgsl.lexer.Lexer
```

| Aspect | Rust | Kotlin |
|--------|------|--------|
| Organisation | Modules, fichiers | Packages, fichiers |
| Visibilité | `pub` explicite | `public` par défaut, `private`, `internal` |
| Chemins | `crate::`, `self::`, `super::` | Packages hiérarchiques |

### 2. Structures de Données

#### Rust - Structs
```rust
// Struct simple
#[derive(Debug, Clone, PartialEq)]
pub struct Handle<T> {
    pub index: u32,
}

// Tuple struct
pub struct Range(pub u32, pub u32);

// Struct avec lifetime
pub struct Arena<'a, T> {
    items: Vec<T>,
    _marker: PhantomData<&'a T>,
}
```

#### Kotlin - Data Classes
```kotlin
// Data class (équivalent d'un struct avec Derive)
@JvmInline
value class Handle<T>(val index: Int)

// Data class classique
data class Range(val start: Int, val end: Int)

// Classe générique
class Arena<T>(
    private val items: MutableList<T> = mutableListOf()
)
```

| Aspect | Rust | Kotlin | Notes |
|--------|------|--------|-------|
| Struct simple | `struct` | `data class` | `data class` génère `toString()`, `equals()`, `hashCode()`, `copy()` |
| Value class | Pas équivalent | `@JvmInline value class` | Optimisation mémoire |
| Génériques | `<T>` | `<T>` | Très similaire |
| Lifetimes | `'a` | Non applicable | GC gère cela |

### 3. Énumérations

#### Rust - Enums
```rust
// Enum simple
#[derive(Debug, Clone, PartialEq)]
pub enum ScalarKind {
    Bool,
    Sint,
    Uint,
    Float,
}

// Enum avec données
pub enum Expression {
    Literal(Literal),
    Variable(Handle<Variable>),
    Binary { op: BinaryOperator, left: Handle<Expression>, right: Handle<Expression> },
    Unary { op: UnaryOperator, expr: Handle<Expression> },
    // ...
}

// Enum comme pattern matching
match expr {
    Expression::Literal(lit) => { /* ... */ }
    Expression::Binary { op, left, right } => { /* ... */ }
    _ => { /* ... */ }
}
```

#### Kotlin - Sealed Classes
```kotlin
// Sealed class (équivalent d'un enum avec données)
sealed class ScalarKind {
    object Bool : ScalarKind()
    object Sint : ScalarKind()
    object Uint : ScalarKind()
    object Float : ScalarKind()
}

// Sealed class avec données
sealed class Expression {
    data class Literal(val value: LiteralValue) : Expression()
    data class Variable(val handle: Handle<Variable>) : Expression()
    data class Binary(
        val op: BinaryOperator,
        val left: Handle<Expression>,
        val right: Handle<Expression>
    ) : Expression()
    data class Unary(
        val op: UnaryOperator,
        val expr: Handle<Expression>
    ) : Expression()
    // ...
}

// Pattern matching avec when
when (expr) {
    is Expression.Literal -> { /* ... */ }
    is Expression.Binary -> { /* expr.op, expr.left, expr.right */ }
    else -> { /* ... */ }
}
```

| Aspect | Rust | Kotlin | Notes |
|--------|------|--------|-------|
| Enum simple | `enum` | `enum class` | Pour les valeurs simples |
| Enum avec données | `enum` | `sealed class` | `sealed class` permet des données complexes |
| Pattern matching | `match` | `when` | `when` est plus flexible |
| Exhaustivité | Vérifiée par le compilateur | Vérifiée si `sealed` | `sealed` garantit l'exhaustivité |

### 4. Options et Résultats

#### Rust - Option et Result
```rust
// Option
pub enum Option<T> {
    Some(T),
    None,
}

fn find_handle(index: u32) -> Option<Handle> {
    if index < arena.len() {
        Some(Handle { index })
    } else {
        None
    }
}

// Result
pub enum Result<T, E> {
    Ok(T),
    Err(E),
}

fn parse_wgsl(source: &str) -> Result<Module, ParseError> {
    // ...
    Ok(module)
    // ou
    Err(ParseError::new("Invalid syntax"))
}

// Utilisation
match parse_wgsl(source) {
    Ok(module) => { /* ... */ }
    Err(error) => { /* ... */ }
}

// Méthodes utiles
let value = option.unwrap();           // Panic si None
let value = option.unwrap_or(default);  // Défaut si None
let value = option.expect("message");   // Panic avec message
let result = result.unwrap();          // Panic si Err
let result = result.expect("message"); // Panic avec message
```

#### Kotlin - Nullables et Result
```kotlin
// Nullable (équivalent Option)
fun findHandle(index: Int): Handle? {
    return if (index < arena.size) Handle(index) else null
}

// Result (personnalisé ou depuis Kotlin 1.6)
fun parseWgsl(source: String): Result<Module> {
    return try {
        Result.success(module)
    } catch (e: ParseException) {
        Result.failure(e)
    }
}

// Utilisation
val module = parseWgsl(source).getOrThrow()
val module = parseWgsl(source).getOrNull()
val module = parseWgsl(source).fold(
    onSuccess = { it },
    onFailure = { throw it }
)

// Méthodes utiles
val value = nullable ?: default          // opérateur Elvis
val value = nullable!!                   // throw NPE si null (éviter!)
val value = nullable.requireNotNull()    // throw IllegalStateException si null
val result = result.getOrThrow()         // throw si failure
val result = result.getOrNull()          // null si failure
```

| Aspect | Rust | Kotlin | Notes |
|--------|------|--------|-------|
| Option | `Option<T>` | `T?` (nullable) | Préferer `T?` pour la simplicité |
| Result | `Result<T, E>` | `Result<T>` | Kotlin a `Result<T>` depuis 1.6 |
| Pattern matching | `match` sur Option/Result | `when` + checks | Moins élégant en Kotlin |
| Unwrap | `.unwrap()`, `.expect()` | `!!`, `requireNotNull()` | Éviter `!!` (non-safe) |
| Propagation | `?` operator | Pas d'équivalent direct | Utiliser `getOrThrow()` ou `fold()` |

---

## Gestion de la Mémoire

### Rust - Ownership System

```rust
// Ownership : chaque valeur a un propriétaire unique
let mut v = vec![1, 2, 3];  // v est le propriétaire
let v2 = v;                // v n'est plus valide, v2 est le nouveau propriétaire
// let v3 = v;            // ERREUR: v a été déplacé (moved)

// Borrowing : emprunter une référence
let v = vec![1, 2, 3];
let v_ref = &v;          // référence immuable
let v_mut_ref = &mut v; // référence mutable (exclusive)

// Lifetimes : garantie que les références sont valides
struct DanglingReference<'a> {
    reference: &'a i32,
}

fn longest<'a>(x: &'a str, y: &'a str) -> &'a str {
    if x.len() > y.len() { x } else { y }
}

// Rc et Arc : comptage de références
use std::rc::Rc;
let rc = Rc::new(vec![1, 2, 3]);
let rc_clone = Rc::clone(&rc);  // Comptage de références

use std::sync::Arc;
let arc = Arc::new(vec![1, 2, 3]);  // Thread-safe

// RefCell : mutabilité intérieure
use std::cell::RefCell;
let cell = RefCell::new(vec![1, 2, 3]);
cell.borrow_mut().push(4);  // Mutabilité à l'exécution
```

### Kotlin - Garbage Collection

```kotlin
// Pas d'ownership explicite, le GC gère tout
val v = mutableListOf(1, 2, 3)  // v est une référence
val v2 = v                     // v et v2 pointent vers la même liste
v.add(4)                      // Modifie la même liste via v et v2

// Pas de borrowing explicite, mais :
// - Les val sont immuables (références)
// - Les var sont mutables (références)

// Pas de lifetimes explicites, le GC gère cela
class DanglingReference(val reference: Int)

// Pas besoin de Rc/Arc, le GC gère le comptage

// Cellules de mutabilité
val mutableList = mutableListOf(1, 2, 3)
mutableList.add(4)  // Modification possible

val immutableList = listOf(1, 2, 3)
// immutableList.add(4)  // ERREUR: immutable

// Pour l'émulation de RefCell :
class RefCell<T>(private var value: T) {
    fun borrow(): T = value
    fun borrowMut(): MutableRef<T> = MutableRef(value)
    
    inner class MutableRef(private var refValue: T) {
        fun set(newValue: T) {
            value = newValue
        }
    }
}
```

| Aspect | Rust | Kotlin | Impact |
|--------|------|--------|--------|
| Ownership | Explicite (compile-time) | Implicite (GC) | Moins de contrôle, plus simple |
| Borrowing | Explicite (`&`, `&mut`) | Implicite | Moins de sécurité à l'exécution |
| Lifetimes | Explicites (`'a`) | Implicites (GC) | Pas de lifetime annotations |
| Rc/Arc | Comptage de références | GC | Le GC est plus simple mais moins prévisible |
| RefCell | Mutabilité intérieure | Pas d'équivalent | Utiliser des classes wrapper |

### Stratégies de Portage

#### 1. Arena System

**Rust (original)** :
```rust
// Dans Naga, les handles référencent des indices dans une Arena
pub struct Arena<T> {
    items: Vec<T>,
}

impl<T> Arena<T> {
    pub fn append(&mut self, item: T) -> Handle<T> {
        let index = self.items.len();
        self.items.push(item);
        Handle { index }
    }
    
    pub fn get(&self, handle: Handle<T>) -> Option<&T> {
        self.items.get(handle.index)
    }
}

#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
pub struct Handle<T> {
    pub index: u32,
}
```

**Kotlin (porté)** :
```kotlin
// Utiliser value classes pour les handles (optimisation mémoire)
@JvmInline
value class Handle<T>(val index: Int)

// Arena simple
class Arena<T> {
    private val items: MutableList<T> = mutableListOf()
    
    fun append(item: T): Handle<T> {
        val index = items.size
        items.add(item)
        return Handle(index)
    }
    
    fun get(handle: Handle<T>): T? {
        return items.getOrNull(handle.index)
    }
    
    fun getOrThrow(handle: Handle<T>): T {
        return items[handle.index]
    }
    
    operator fun get(handle: Handle<T>): T = items[handle.index]
}

// Arena unique (pour éviter les doublons)
class UniqueArena<T> {
    private val items: MutableList<T> = mutableListOf()
    private val indexMap: MutableMap<T, Int> = mutableMapOf()
    
    fun intern(item: T): Handle<T> {
        return indexMap.getOrPut(item) {
            val index = items.size
            items.add(item)
            index
        }.let { Handle(it) }
    }
}
```

#### 2. Éviter les Références Cycliques

**Rust** : Utilise `Rc<RefCell<T>>` ou `Arc<Mutex<T>>` pour les références cycliques.

**Kotlin** : Le GC gère les références cycliques, mais attention aux fuites mémoire.

```kotlin
// En Kotlin, pas besoin de RefCell, mais :
class Node {
    var parent: Node? = null
    val children: MutableList<Node> = mutableListOf()
}

// Le GC gère les références cycliques
val root = Node()
val child = Node()
root.children.add(child)
child.parent = root  // Référence cyclique, gérée par le GC
```

#### 3. Émuler les Lifetimes

**Rust** :
```rust
struct Context<'a> {
    arena: &'a Arena<Expression>,
}
```

**Kotlin** : Pas besoin, mais on peut utiliser des classes pour encapsuler :
```kotlin
class Context(private val arena: Arena<Expression>) {
    // Les méthodes peuvent utiliser arena
    fun createExpression(): Handle<Expression> {
        return arena.append(/* ... */)
    }
}
```

---

## Gestion des Erreurs

### Rust - Result et Option

```rust
// Result avec erreurs typées
#[derive(Debug, thiserror::Error)]
pub enum ParseError {
    #[error("Unexpected token {0:?} at {line}:{column}")]
    UnexpectedToken(Token, line: usize, column: usize),
    
    #[error("Expected {expected:?}, found {found:?}")]
    ExpectedToken { expected: TokenKind, found: TokenKind, line: usize, column: usize },
    
    #[error("End of file")]
    EndOfFile,
}

// Propagation avec ?
fn parse_expression(parser: &mut Parser) -> Result<Handle<Expression>, ParseError> {
    let token = parser.next_token()?;
    // ...
    Ok(expression_handle)
}

// Gestion avec match
match parse_expression(parser) {
    Ok(expr) => { /* ... */ }
    Err(ParseError::UnexpectedToken(token, line, col)) => { /* ... */ }
    Err(_) => { /* ... */ }
}

// Conversion entre Result et Option
fn to_option<T, E>(result: Result<T, E>) -> Option<T> {
    result.ok()
}

fn to_result<T>(option: Option<T>, error: E) -> Result<T, E> {
    option.ok_or(error)
}
```

### Kotlin - Result et Nullables

```kotlin
// Result avec erreurs typées
sealed class ParseError(message: String) : Exception(message) {
    data class UnexpectedToken(
        val token: Token,
        val line: Int,
        val column: Int
    ) : ParseError("Unexpected token ${token.kind} at $line:$column")
    
    data class ExpectedToken(
        val expected: TokenKind,
        val found: TokenKind,
        val line: Int,
        val column: Int
    ) : ParseError("Expected $expected, found $found at $line:$column")
    
    object EndOfFile : ParseError("End of file")
}

// Propagation avec getOrThrow
fun parseExpression(parser: Parser): Handle<Expression> {
    val token = parser.nextToken() ?: throw ParseError.EndOfFile
    // ...
    return expressionHandle
}

// Gestion avec try-catch
try {
    val expr = parseExpression(parser)
    // ...
} catch (e: ParseError) {
    when (e) {
        is ParseError.UnexpectedToken -> { /* ... */ }
        is ParseError.ExpectedToken -> { /* ... */ }
        ParseError.EndOfFile -> { /* ... */ }
    }
}

// Utilisation de Result<T>
fun parseExpressionSafe(parser: Parser): Result<Handle<Expression>> {
    return try {
        Result.success(parseExpression(parser))
    } catch (e: ParseError) {
        Result.failure(e)
    }
}

// Conversion entre Result et nullable
fun <T> Result<T>.toNullable(): T? = fold(onSuccess = { it }, onFailure = { null })
fun <T> T?.toResult(error: Throwable): Result<T> = this?.let { Result.success(it) } ?: Result.failure(error)
```

| Pattern | Rust | Kotlin | Notes |
|---------|------|--------|-------|
| Propagation | `?` operator | `getOrThrow()` ou `throw` | Moins élégant en Kotlin |
| Pattern matching | `match` sur Result | `try-catch` + `when` | `try-catch` est plus lourd |
| Erreurs typées | `enum` avec `thiserror` | `sealed class` | Très similaire |
| Conversion | `.ok()`, `.err()` | `.toNullable()`, `.toResult()` | Méthodes d'extension utiles |

### Bonnes Pratiques Kotlin

1. **Utiliser Result pour les erreurs récupérables** :
```kotlin
fun parseWgsl(source: String): Result<Module> = try {
    Result.success(parseWgslInternal(source))
} catch (e: ParseException) {
    Result.failure(e)
}
```

2. **Utiliser les exceptions pour les erreurs non récupérables** :
```kotlin
fun parseWgslOrThrow(source: String): Module = parseWgsl(source).getOrThrow()
```

3. **Éviter `!!`** : Préferer `requireNotNull()`, `checkNotNull()`, ou `getOrThrow()`

4. **Utiliser sealed classes pour les erreurs** : Permet le pattern matching exhaustif

---

## Système de Types

### Types Primitive

| Type | Rust | Kotlin | Notes |
|------|------|--------|-------|
| Booléen | `bool` | `Boolean` | `true`, `false` |
| Entier signé 8 bits | `i8` | `Byte` | -128 à 127 |
| Entier signé 16 bits | `i16` | `Short` | -32768 à 32767 |
| Entier signé 32 bits | `i32` | `Int` | -2^31 à 2^31-1 |
| Entier signé 64 bits | `i64` | `Long` | -2^63 à 2^63-1 |
| Entier non signé 8 bits | `u8` | `UByte` | 0 à 255 |
| Entier non signé 16 bits | `u16` | `UShort` | 0 à 65535 |
| Entier non signé 32 bits | `u32` | `UInt` | 0 à 2^32-1 |
| Entier non signé 64 bits | `u64` | `ULong` | 0 à 2^64-1 |
| Float 32 bits | `f32` | `Float` | IEEE 754 |
| Float 64 bits | `f64` | `Double` | IEEE 754 |
| String | `&str`, `String` | `String` | `&str` = vue, `String` = propriété |
| Char | `char` | `Char` | 4 octets Unicode |

### Types Composés

| Type | Rust | Kotlin | Notes |
|------|------|--------|-------|
| Tableau | `[T; N]` | `Array<T>` | Taille fixe |
| Vecteur | `Vec<T>` | `MutableList<T>`, `List<T>` | `Vec` → `MutableList` |
| HashMap | `HashMap<K, V>` | `MutableMap<K, V>`, `Map<K, V>` | `HashMap` → `MutableMap` |
| HashSet | `HashSet<T>` | `MutableSet<T>`, `Set<T>` | `HashSet` → `MutableSet` |
| Option | `Option<T>` | `T?` | `None` → `null` |
| Result | `Result<T, E>` | `Result<T>` | `Err` → `Failure` |

### Types Naga Spécifiques

| Type Naga | Rust | Kotlin | Notes |
|-----------|------|--------|-------|
| ScalarKind | `enum ScalarKind { Bool, Sint, Uint, Float }` | `sealed class ScalarKind { object Bool; object Sint; object Uint; object Float }` | ou `enum class` |
| VectorSize | `enum VectorSize { Bi, Tri, Quad }` | `enum class VectorSize { BI, TRI, QUAD }` | |
| MatrixSize | `enum MatrixSize { X2, X3, X4 }` | `enum class MatrixSize { X2, X3, X4 }` | |
| BinaryOperator | `enum BinaryOperator { Add, Subtract, ... }` | `enum class BinaryOperator { ADD, SUBTRACT, ... }` | |
| UnaryOperator | `enum UnaryOperator { Negate, Not, ... }` | `enum class UnaryOperator { NEGATE, NOT, ... }` | |

---

## Macros et Métaprogrammation

### Rust - Macros Procédurales

Naga utilise des macros procédurales pour :
1. **Génération de code répétitif**
2. **Implémentation de traits sur des types**
3. **Définition de structures complexes**

```rust
// Macro pour générer des implémentations de Display
macro_rules! gen_component_wise_extractor {
    ($name:ident, $t:ident) => {
        #[allow(dead_code)]
        pub fn $name(&self, e: Expression) -> Vec<$t> {
            // ...
        }
    };
}

// Macro pour générer des implémentations de traits
macro_rules! impl_display_for_expression {
    ($($type:ident),*) => {
        $(impl std::fmt::Display for $type {
            fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
                write!(f, "{:?}", self)
            }
        })*
    };
}

// Macro pour générer des tests
#[cfg(test)]
macro_rules! test_expr {
    ($name:ident, $code:expr, $expected:expr) => {
        #[test]
        fn $name() {
            let result = $code;
            assert_eq!(result, $expected);
        }
    };
}
```

### Kotlin - Alternatives aux Macros

Kotlin n'a pas de macros procédurales, mais offre plusieurs alternatives :

#### 1. Inline Functions

```kotlin
// Fonction inline pour éviter l'allocation
inline fun <T> lock(lock: Lock, body: () -> T): T {
    lock.lock()
    try {
        return body()
    } finally {
        lock.unlock()
    }
}
```

#### 2. Extension Functions

```kotlin
// Extension function pour ajouter des méthodes
fun String.toTokenKind(): TokenKind? {
    return when (this) {
        "fn" -> TokenKind.Fn
        "let" -> TokenKind.Let
        // ...
        else -> null
    }
}
```

#### 3. Reified Generics

```kotlin
// Génériques réifiés pour accéder au type à l'exécution
inline fun <reified T> isA(value: Any): Boolean = value is T
```

#### 4. Génération de Code (Annotation Processing)

Pour générer du code à la compilation, utiliser KSP (Kotlin Symbol Processing) :

```kotlin
// Annotation
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateDisplay

// Processor KSP (à implémenter séparément)
```

#### 5. Génération de Code à l'Exécution

```kotlin
// Utiliser la réflexion pour générer dynamiquement
fun generateDisplayImplementation(cls: Class<*>) {
    // Générer du code dynamiquement
}
```

### Exemple de Portage d'une Macro

**Rust (original)** :
```rust
macro_rules! gen_component_wise_extractor {
    ($name:ident, $t:ident) => {
        pub fn $name(&self, e: Expression) -> Vec<$t> {
            match e {
                Expression::Splat { .. } => vec![],
                Expression::Literal(lit) => self.$name(lit),
                Expression::Binary { op, .. } => vec![self.$name(op)],
                // ...
            }
        }
    };
}
```

**Kotlin (porté)** :
```kotlin
// Méthode d'extension ou méthode de classe
fun Proc.extractComponents(expr: Expression): List<Any> {
    return when (expr) {
        is Expression.Splat -> emptyList()
        is Expression.Literal -> extractComponents(expr.value)
        is Expression.Binary -> listOf(extractComponents(expr.op))
        // ...
    }.flatten()
}

// Ou avec une interface
interface ComponentExtractor {
    fun extract(expr: Expression): List<Any>
}

// Implémentation pour chaque type
```

---

## Pattern Matching

### Rust - match Expression

```rust
match expression {
    Expression::Literal(lit) => {
        // Traiter le littéral
    }
    Expression::Variable(var) => {
        // Traiter la variable
    }
    Expression::Binary { op, left, right } => {
        // Traiter l'opération binaire
        match op {
            BinaryOperator::Add => { /* ... */ }
            BinaryOperator::Subtract => { /* ... */ }
            // ...
        }
    }
    Expression::Unary { op, expr } => {
        // Traiter l'opération unaire
    }
    Expression::Call { function, arguments } => {
        // Traiter l'appel de fonction
    }
    _ => {
        // Cas par défaut (si l'enum n'est pas exhaustif)
    }
}
```

### Kotlin - when Expression

```kotlin
when (expr) {
    is Expression.Literal -> {
        // Traiter le littéral
    }
    is Expression.Variable -> {
        // Traiter la variable
    }
    is Expression.Binary -> {
        // Traiter l'opération binaire
        when (expr.op) {
            BinaryOperator.ADD -> { /* ... */ }
            BinaryOperator.SUBTRACT -> { /* ... */ }
            // ...
        }
    }
    is Expression.Unary -> {
        // Traiter l'opération unaire
    }
    is Expression.Call -> {
        // Traiter l'appel de fonction
    }
    // Pas de else nécessaire si sealed class
}
```

### Comparaison

| Aspect | Rust | Kotlin | Notes |
|--------|------|--------|-------|
| Syntaxe | `match` | `when` | `when` est plus flexible |
| Exhaustivité | Vérifiée par le compilateur | Vérifiée si `sealed class` | Les deux garantissent l'exhaustivité |
| Binding | `Expression::Binary { op, left, right }` | `is Expression.Binary` puis accéder aux propriétés | Moins direct en Kotlin |
| Expression | Oui | Oui | Les deux sont des expressions (retournent une valeur) |
| Default | `_` | `else` | Les deux supportent un cas par défaut |

### Exemple Avancé : Visitor Pattern

**Rust (original)** :
```rust
pub trait Visit {
    fn visit_expression(&mut self, e: &Expression) -> Result<(), VisitError>;
    // ...
}

// Implémentation générée par macro
impl Visit for MyVisitor {
    fn visit_expression(&mut self, e: &Expression) -> Result<(), VisitError> {
        match e {
            Expression::Literal(lit) => self.visit_literal(lit),
            Expression::Variable(var) => self.visit_variable(var),
            // ...
        }
    }
}
```

**Kotlin (porté)** :
```kotlin
// Interface Visitor
interface ExpressionVisitor {
    fun visitLiteral(lit: LiteralValue)
    fun visitVariable(var: Handle<Variable>)
    fun visitBinary(expr: Expression.Binary)
    fun visitUnary(expr: Expression.Unary)
    // ...
    
    // Méthode principale avec dispatch
    fun visit(expr: Expression) {
        when (expr) {
            is Expression.Literal -> visitLiteral(expr.value)
            is Expression.Variable -> visitVariable(expr.handle)
            is Expression.Binary -> visitBinary(expr)
            is Expression.Unary -> visitUnary(expr)
            // ...
        }
    }
}

// Implémentation concrète
class MyVisitor : ExpressionVisitor {
    override fun visitLiteral(lit: LiteralValue) { /* ... */ }
    override fun visitVariable(var: Handle<Variable>) { /* ... */ }
    override fun visitBinary(expr: Expression.Binary) { /* ... */ }
    override fun visitUnary(expr: Expression.Unary) { /* ... */ }
}
```

---

## Concurrences

### Rust - Threads et Async

```rust
// Threads
use std::thread;

let handle = thread::spawn(move || {
    // Code exécuté dans un thread
    expensive_computation()
});

let result = handle.join().unwrap();

// Async/Await
use tokio::task;

async fn async_function() -> Result<(), ()> {
    let result1 = task::spawn(async { computation1() }).await?;
    let result2 = task::spawn(async { computation2() }).await?;
    Ok(())
}

// Channels
use std::sync::mpsc;

let (tx, rx) = mpsc::channel();

thread::spawn(move || {
    tx.send(result).unwrap();
});

let received = rx.recv().unwrap();
```

### Kotlin - Coroutines et Flow

```kotlin
// Coroutines
import kotlinx.coroutines.*

val scope = CoroutineScope(Dispatchers.Default)

val deferred = scope.async {
    // Code exécuté dans une coroutine
    expensiveComputation()
}

val result = deferred.await()

// Async/Await
suspend fun asyncFunction() {
    val result1 = coroutineScope { async { computation1() } }.await()
    val result2 = coroutineScope { async { computation2() } }.await()
}

// Channels
import kotlinx.coroutines.channels.*

val channel = Channel<Result>()

scope.launch {
    channel.send(result)
}

val received = channel.receive()
```

### Tableau Comparatif

| Concept | Rust | Kotlin | Notes |
|---------|------|--------|-------|
| Threads | `std::thread` | `kotlinx.coroutines` | Kotlin utilise des coroutines (lightweight) |
| Async/Await | `async`/`await` | `suspend` + `async`/`await` | Très similaire |
| Channels | `std::sync::mpsc` | `kotlinx.coroutines.channels` | API similaire |
| Mutex | `Mutex`, `RwLock` | `Mutex` | Kotlin a `Mutex` dans `kotlinx.coroutines.sync` |
| Parallelism | Rayon, crossbeam | `Dispatchers.Default`, `parallelStream` | Utiliser `Dispatchers.Default` pour le parallélisme |

### Exemple : Traitement Parallèle des Fichiers

**Rust** :
```rust
use rayon::prelude::*;

let files: Vec<PathBuf> = get_wgsl_files();
let results: Vec<Result<Module, ParseError>> = files
    .par_iter()
    .map(|file| parse_wgsl_file(file))
    .collect();
```

**Kotlin** :
```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

suspend fun processFilesParallel(files: List<Path>): List<Result<Module>> {
    return coroutineScope {
        files.map { file ->
            async(Dispatchers.IO) {
                parseWgslFile(file)
            }
        }.awaitAll()
    }
}
```

---

## Écosystème et Outils

### Gestion des Dépendances

| Aspect | Rust | Kotlin | Notes |
|--------|------|--------|-------|
| Gestionnaire | Cargo | Gradle | Cargo est intégré, Gradle est plus flexible |
| Fichier de config | Cargo.toml | build.gradle.kts | Formats différents |
| Résolution | Dépendances par crate | Dépendances par module | Kotlin permet les modules multi-plateformes |
| Lock file | Cargo.lock | gradle.lockfile | Les deux verrouillent les versions |
| Workspaces | Workspace | Multi-project build | Concepts similaires |

### Exemple Cargo.toml
```toml
[package]
name = "naga"
version = "0.15.0"
edition = "2021"

[dependencies]
bitflags = "2.3"
bytemuck = { version = "1.0", features = ["derive"] }
num-traits = "0.2"
spirv = { version = "0.2", optional = true }

[features]
d default = []
spirv = ["dep:spirv"]
```

### Exemple build.gradle.kts
```kotlin
plugins {
    kotlin("jvm") version "1.9.0"
    `maven-publish`
}

group = "dev.gfxrs"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

kotlin {
    jvmToolchain(17)
}
```

### Testing

| Aspect | Rust | Kotlin | Notes |
|--------|------|--------|-------|
| Framework | Built-in | JUnit 5 | JUnit est plus mature |
| Assertions | Built-in | AssertJ | AssertJ est plus puissant |
| Tests async | `tokio::test` | `runBlockingTest` | Kotlin a des utilitaires pour les tests de coroutines |
| Benchmarks | Criterion | JMH | JMH est standard pour le JVM |
| Coverage | tarpaulin | JaCoCo | JaCoCo est standard pour le JVM |

### Build et Distribution

| Aspect | Rust | Kotlin | Notes |
|--------|------|--------|-------|
| Build | `cargo build` | `./gradlew build` | Gradle est plus lent mais plus flexible |
| Test | `cargo test` | `./gradlew test` | Les deux exécutent les tests |
| Run | `cargo run` | `./gradlew run` | Les deux exécutent le code |
| Documentation | `cargo doc` | Dokka | Dokka génère une documentation similaire |
| Publication | `cargo publish` | `./gradlew publish` | Publication sur crates.io vs Maven |
| Binaire | `cargo build --release` | `./gradlew shadowJar` | Shadow plugin pour le JAR fat |
| Binaire natif | Natif | GraalVM | GraalVM pour les binaires natifs |

---

## Exemples de Portage

### 1. Module Naga

**Rust** :
```rust
#[derive(Debug, Clone)]
pub struct Module {
    pub types: Vec<Type>,
    pub functions: Vec<Function>,
    pub global_variables: Vec<GlobalVariable>,
    pub entry_points: Vec<EntryPoint>,
    pub name_hint: Option<String>,
    pub capabilities: Capabilities,
}
```

**Kotlin** :
```kotlin
data class Module(
    val types: List<Type> = emptyList(),
    val functions: List<Function> = emptyList(),
    val globalVariables: List<GlobalVariable> = emptyList(),
    val entryPoints: List<EntryPoint> = emptyList(),
    val nameHint: String? = null,
    val capabilities: Capabilities = Capabilities.NONE
)
```

### 2. Type Naga

**Rust** :
```rust
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum Type {
    Bool,
    I8,
    I16,
    I32,
    U8,
    U16,
    U32,
    F16,
    F32,
    Vector { size: VectorSize, width: ScalarWidth },
    Matrix { columns: MatrixSize, rows: MatrixSize, width: ScalarWidth },
    // ...
}
```

**Kotlin** :
```kotlin
sealed class Type {
    object Bool : Type()
    object I8 : Type()
    object I16 : Type()
    object I32 : Type()
    object U8 : Type()
    object U16 : Type()
    object U32 : Type()
    object F16 : Type()
    object F32 : Type()
    
    data class Vector(
        val size: VectorSize,
        val width: ScalarWidth
    ) : Type()
    
    data class Matrix(
        val columns: MatrixSize,
        val rows: MatrixSize,
        val width: ScalarWidth
    ) : Type()
    
    // ...
}
```

### 3. Expression Naga

**Rust** :
```rust
#[derive(Debug, Clone)]
pub enum Expression {
    Literal(Literal),
    Variable(Handle<Variable>),
    Binary {
        op: BinaryOperator,
        left: Handle<Expression>,
        right: Handle<Expression>,
    },
    Unary {
        op: UnaryOperator,
        expr: Handle<Expression>,
    },
    Call {
        function: Handle<Function>,
        arguments: Vec<Handle<Expression>>,
        result: Option<Handle<Value>>,
    },
    // ...
}
```

**Kotlin** :
```kotlin
sealed class Expression {
    data class Literal(val value: LiteralValue) : Expression()
    data class Variable(val handle: Handle<Variable>) : Expression()
    
    data class Binary(
        val op: BinaryOperator,
        val left: Handle<Expression>,
        val right: Handle<Expression>
    ) : Expression()
    
    data class Unary(
        val op: UnaryOperator,
        val expr: Handle<Expression>
    ) : Expression()
    
    data class Call(
        val function: Handle<Function>,
        val arguments: List<Handle<Expression>>,
        val result: Handle<Value>? = null
    ) : Expression()
    
    // ...
}
```

### 4. Parser WGSL

**Rust** :
```rust
pub fn parse(source: &str) -> Result<Module, ParseError> {
    let mut parser = Parser::new(source);
    parser.parse_module()
}

impl Parser {
    pub fn new(source: &str) -> Self {
        Parser {
            lexer: Lexer::new(source),
            current_token: Token::eof(),
            // ...
        }
    }
    
    pub fn parse_module(&mut self) -> Result<Module, ParseError> {
        // ...
    }
}
```

**Kotlin** :
```kotlin
fun parse(source: String): Result<Module> {
    return try {
        val parser = Parser(source)
        Result.success(parser.parseModule())
    } catch (e: ParseException) {
        Result.failure(e)
    }
}

class Parser(private val source: String) {
    private val lexer: Lexer = Lexer(source)
    private var currentToken: Token = Token.EOF
    
    fun parseModule(): Module {
        // ...
    }
}
```

### 5. Backend Writer (MSL)

**Rust** :
```rust
pub struct MslWriter {
    // ...
}

impl Backend for MslWriter {
    fn write(&mut self, module: &Module) -> String {
        let mut output = String::new();
        // ...
        output
    }
}
```

**Kotlin** :
```kotlin
class MslWriter {
    // ...
}

interface BackendWriter {
    fun write(module: Module): String
}

class MslWriterImpl : BackendWriter {
    override fun write(module: Module): String {
        val output = StringBuilder()
        // ...
        return output.toString()
    }
}
```

---

## Résumé des Correspondances

### Tableau de Correspondance Rapid

| Rust | Kotlin | Notes |
|------|--------|-------|
| `crate` | Module Gradle | |
| `mod` | `package` | |
| `pub` | `public` (défaut) | Kotlin: `public`, `private`, `internal` |
| `struct` | `data class` | |
| `enum` | `enum class` ou `sealed class` | `sealed class` pour les données |
| `trait` | `interface` | |
| `impl` | `:`, `override` | |
| `fn` | `fun` | |
| `let` | `val` | |
| `let mut` | `var` | |
| `const` | `val` (dans un companion object) | |
| `Option<T>` | `T?` | |
| `Result<T, E>` | `Result<T>` | Kotlin 1.6+ |
| `Vec<T>` | `MutableList<T>` | |
| `HashMap<K, V>` | `MutableMap<K, V>` | |
| `&T` | `T` (référence immuable) | Pas de borrowing explicite |
| `&mut T` | `var` (référence mutable) | Pas de borrowing explicite |
| `Box<T>` | `T` (objet sur le heap) | Tout est sur le heap en Kotlin |
| `Rc<T>` | `T` (GC) | Le GC gère cela |
| `Arc<T>` | `T` (GC thread-safe) | Le GC gère cela |
| `RefCell<T>` | Classe wrapper | |
| `match` | `when` | |
| `?` operator | `getOrThrow()` | |
| `unsafe` | Pas d'équivalent | Kotlin est safe par défaut |
| `macro_rules!` | Inline functions, extensions | |
| `#[derive(...)]` | `data class`, plugins | `data class` génère beaucoup automatiquement |

---

## Bonnes Pratiques pour le Portage

### 1. Structurer le Code

- **Rust** : Utilise des modules (`mod`) et des crates
- **Kotlin** : Utiliser des packages et des modules Gradle
- **Conseil** : Suivre la même hiérarchie que le projet Rust

### 2. Gérer la Mémoire

- **Rust** : Ownership explicite avec lifetimes
- **Kotlin** : Garbage Collection
- **Conseil** : Utiliser des `value class` pour les handles et des `Arena` pour le stockage

### 3. Gérer les Erreurs

- **Rust** : `Result<T, E>` et `Option<T>` partout
- **Kotlin** : `Result<T>` pour les erreurs récupérables, exceptions pour les erreurs fatales
- **Conseil** : Créer des `sealed class` pour les erreurs typées

### 4. Utiliser les Génériques

- **Rust** : Generics puissants avec traits bounds
- **Kotlin** : Generics puissants avec interfaces
- **Conseil** : Adapter les traits Rust → interfaces Kotlin

### 5. Pattern Matching

- **Rust** : `match` expression
- **Kotlin** : `when` expression
- **Conseil** : Utiliser `sealed class` pour garantir l'exhaustivité

### 6. Concurrence

- **Rust** : Threads et async/await
- **Kotlin** : Coroutines et Flow
- **Conseil** : Utiliser `kotlinx.coroutines` pour la concurrence

### 7. Testing

- **Rust** : Tests intégrés avec `#[test]`
- **Kotlin** : JUnit 5 avec AssertJ
- **Conseil** : Utiliser JUnit 5 pour les tests

---

## Outils de Portage

### Outils Utiles

| Outil | Description | Lien |
|-------|-------------|------|
| IntelliJ IDEA | IDE avec support Rust et Kotlin | [jetbrains.com/idea](https://www.jetbrains.com/idea/) |
| Rust Analyzer | LSP pour Rust | [rust-analyzer.github.io](https://rust-analyzer.github.io/) |
| Kotlin Plugin | Plugin Kotlin pour IntelliJ | Inclus avec IntelliJ |
| jb-rust | Plugin Rust pour IntelliJ | [plugins.jetbrains.com/plugin/8182-rust](https://plugins.jetbrains.com/plugin/8182-rust) |

### Plugins Gradle

| Plugin | Description | Lien |
|--------|-------------|------|
| Kotlin JVM | Support Kotlin pour Gradle | [kotlinlang.org/docs/gradle.html](https://kotlinlang.org/docs/gradle.html) |
| KSP | Kotlin Symbol Processing | [kotlinlang.org/docs/ksp-overview.html](https://kotlinlang.org/docs/ksp-overview.html) |
| Shadow | Création de JAR fat | [imperceptiblethoughts.com/shadow/](https://imperceptiblethoughts.com/shadow/) |
| JaCoCo | Coverage de code | [www.eclemma.org/jacoco/](https://www.eclemma.org/jacoco/) |
| JMH | Benchmarks | [openjdk.java.net/projects/code-tools/jmh/](https://openjdk.java.net/projects/code-tools/jmh/) |

---

## Références

### Documentation Rust
- [The Rust Programming Language](https://doc.rust-lang.org/book/)
- [Rust by Example](https://doc.rust-lang.org/rust-by-example/)
- [Rust Standard Library](https://doc.rust-lang.org/std/)
- [Naga Source Code](https://github.com/gfx-rs/naga)

### Documentation Kotlin
- [Kotlin Language Documentation](https://kotlinlang.org/docs/home.html)
- [Kotlin for Rust Developers](https://kotlinlang.org/docs/rust-devs.html)
- [Kotlin Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### Outils de Comparaison
- [Rust vs Kotlin - Stack Overflow](https://stackoverflow.com/questions/tagged/rust+kotlin)
- [Language Comparison - Wikipedia](https://en.wikipedia.org/wiki/Comparison_of_programming_languages)

---

**Dernière mise à jour** : 2024-XX-XX
