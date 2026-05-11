# 📍 Phase 1.3 : Span, Diagnostics et Types Utilitaires

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 1 - Fondations  
**Sous-Phase** : 1.3 - Span et Diagnostics  
**Durée** : 1 semaine  
**Priorité** : ⭐⭐⭐⭐ (Importante pour le debugging et la validation)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : 
> - `/Users/chaos/RustroverProjects/wgpu/naga/src/span.rs`
> - `/Users/chaos/RustroverProjects/wgpu/naga/src/common/diagnostic_debug.rs`
> - `/Users/chaos/RustroverProjects/wgpu/naga/src/error.rs`

---

## 📋 OBJECTIFS

Implémenter les **types utilitaires** pour la gestion des positions source, des diagnostics et des erreurs.  
Ces types sont essentiels pour :
- **Debugging** : Savoir où une erreur s'est produite dans le code source
- **Validation** : Reporter des erreurs avec des informations de contexte
- **Optimisation** : Ignorer des parties du code en développement

**Livrable principal** : Module utilitaire complet avec Span, Diagnostic, et gestion des erreurs.

---

## 🎯 CONCEPTS CLÉS

### 1. Span
Un **Span** représente une **plage de caractères** dans le code source.  
Il est utilisé pour :
- Localiser les erreurs dans le code source
- Optimiser le parsing (ignorer certaines parties)
- Générer des messages d'erreur plus précis

### 2. Diagnostic
Un **Diagnostic** est un message (erreur, warning, info) associé à une position dans le code source.  
Il est utilisé pour :
- Reporter les erreurs de parsing
- Reporter les erreurs de validation
- Avertir de pratiques douteuses

### 3. Diagnostic Filter
Un **Diagnostic Filter** permet de contrôler quels diagnostics sont affichés.  
Il est utilisé pour :
- Désactiver certains warnings
- Contrôler le niveau de verbosité
- Filtrer les diagnostics par type

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. Span.kt

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/ir/Span.kt`

```kotlin
package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * Position dans un fichier source (ligne et colonne).
 * 
 * @property line Le numéro de ligne (1-based)
 * @property column Le numéro de colonne (0-based)
 */
@Serializable
data class SourceLocation(
    val line: Int,
    val column: Int
) {
    companion object {
        /**
         * Position par défaut (utilisée quand la position est inconnue).
         */
        val DEFAULT = SourceLocation(0, 0)
    }
    
    /**
     * Vérifie si cette position est valide (line > 0).
     */
    fun isValid(): Boolean = line > 0
    
    /**
     * Compare deux positions.
     * Retourne -1 si this < other, 0 si this == other, 1 si this > other.
     */
    fun compareTo(other: SourceLocation): Int {
        val lineCompare = this.line.compareTo(other.line)
        return if (lineCompare != 0) lineCompare else this.column.compareTo(other.column)
    }
    
    override fun toString(): String = "${line}:${column}"
}

/**
 * Plage de caractères dans un fichier source.
 * Représentée par une position de début et une position de fin.
 * 
 * @property start Position de début (inclus)
 * @property end Position de fin (inclus)
 */
@Serializable
data class Span(
    val start: SourceLocation = SourceLocation.DEFAULT,
    val end: SourceLocation = SourceLocation.DEFAULT
) {
    companion object {
        /**
         * Span par défaut (position inconnue).
         */
        val DEFAULT = Span(SourceLocation.DEFAULT, SourceLocation.DEFAULT)
    }
    
    /**
     * Vérifie si ce Span est valide (start et end sont valides).
     */
    fun isValid(): Boolean = start.isValid() && end.isValid()
    
    /**
     * Vérifie si ce Span est sur une seule ligne.
     */
    fun isSingleLine(): Boolean = start.line == end.line
    
    /**
     * Retourne le numéro de ligne de début.
     */
    val startLine: Int get() = start.line
    
    /**
     * Retourne le numéro de colonne de début.
     */
    val startColumn: Int get() = start.column
    
    /**
     * Retourne le numéro de ligne de fin.
     */
    val endLine: Int get() = end.line
    
    /**
     * Retourne le numéro de colonne de fin.
     */
    val endColumn: Int get() = end.column
    
    /**
     * Fusionne ce Span avec un autre.
     * Retourne un Span qui couvre les deux.
     */
    fun merge(other: Span): Span {
        val newStart = if (start.compareTo(other.start) <= 0) start else other.start
        val newEnd = if (end.compareTo(other.end) >= 0) end else other.end
        return Span(newStart, newEnd)
    }
    
    /**
     * Vérifie si ce Span contient une position.
     */
    fun contains(location: SourceLocation): Boolean {
        if (location.line < start.line || location.line > end.line) return false
        if (location.line == start.line && location.column < start.column) return false
        if (location.line == end.line && location.column > end.column) return false
        return true
    }
    
    /**
     * Vérifie si ce Span contient un autre Span.
     */
    fun contains(span: Span): Boolean {
        return contains(span.start) && contains(span.end)
    }
    
    /**
     * Vérifie si ce Span chevauche un autre Span.
     */
    fun overlaps(other: Span): Boolean {
        return this.start <= other.end && other.start <= this.end
    }
    
    /**
     * Compare deux spans par leur position de début.
     */
    fun compareByStart(other: Span): Int = this.start.compareTo(other.start)
    
    override fun toString(): String = "${start}-${end}"
}

/**
 * Opérateur pour comparer deux SourceLocation.
 */
operator fun SourceLocation.compareTo(other: SourceLocation): Int = this.compareTo(other)

/**
 * Opérateur pour vérifier si une SourceLocation est avant une autre.
 */
operator fun SourceLocation.rangeTo(other: SourceLocation): Span = Span(this, other)

/**
 * Crée un Span à partir d'une seule position.
 */
fun SourceLocation.toSpan(): Span = Span(this, this)

/**
 * Interface pour les éléments qui ont une position source.
 */
interface WithSpan {
    val span: Span
}

/**
 * Extension pour créer un Span à partir de deux positions.
 */
fun spanOf(start: SourceLocation, end: SourceLocation): Span = Span(start, end)

/**
 * Extension pour créer un Span à partir d'une ligne et colonne de début et fin.
 */
fun spanOf(
    startLine: Int,
    startColumn: Int,
    endLine: Int,
    endColumn: Int
): Span = Span(
    SourceLocation(startLine, startColumn),
    SourceLocation(endLine, endColumn)
)
```

---

### 2. Diagnostic.kt

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/common/Diagnostic.kt`

```kotlin
package io.ygdrasil.wgsl.common

import io.ygdrasil.wgsl.ir.Span
import kotlinx.serialization.Serializable

/**
 * Niveau de sévérité d'un diagnostic.
 */
@Serializable
enum class DiagnosticSeverity {
    /** Erreur (le shader ne peut pas être compilé) */
    ERROR,
    
    /** Warning (le shader peut être compilé mais il y a un problème) */
    WARNING,
    
    /** Information (message informatif) */
    INFO,
    
    /** Debug (message de debug) */
    DEBUG
}

/**
 * Code d'erreur pour les diagnostics.
 * Permet de catégoriser et filtrer les diagnostics.
 */
@Serializable
sealed class DiagnosticCode {
    /**
     * Erreurs de parsing.
     */
    @Serializable
    sealed class Parse : DiagnosticCode() {
        @Serializable
        object UNEXPECTED_TOKEN : Parse()
        @Serializable
        object UNEXPECTED_EOF : Parse()
        @Serializable
        object INVALID_NUMBER : Parse()
        @Serializable
        object INVALID_IDENTIFIER : Parse()
        @Serializable
        object EXPECTED : Parse()
        @Serializable
        data class CUSTOM(val code: String) : Parse()
    }
    
    /**
     * Erreurs de validation sémantique.
     */
    @Serializable
    sealed class Validation : DiagnosticCode() {
        @Serializable
        object TYPE_MISMATCH : Validation()
        @Serializable
        object INVALID_EXPRESSION : Validation()
        @Serializable
        object INVALID_STATEMENT : Validation()
        @Serializable
        object INVALID_FUNCTION : Validation()
        @Serializable
        object INVALID_ENTRY_POINT : Validation()
        @Serializable
        object INVALID_BINDING : Validation()
        @Serializable
        object MISSING_RETURN : Validation()
        @Serializable
        data class CUSTOM(val code: String) : Validation()
    }
    
    /**
     * Warnings.
     */
    @Serializable
    sealed class Warning : DiagnosticCode() {
        @Serializable
        object UNUSED_VARIABLE : Warning()
        @Serializable
        object UNUSED_FUNCTION : Warning()
        @Serializable
        object DEPRECATED_FEATURE : Warning()
        @Serializable
        object UNREACHABLE_CODE : Warning()
        @Serializable
        data class CUSTOM(val code: String) : Warning()
    }
    
    /**
     * Code personnalisé.
     */
    @Serializable
    data class Custom(val code: String) : DiagnosticCode()
}

/**
 * Diagnostic message.
 * Représente un message (erreur, warning, info) avec une position dans le code source.
 */
@Serializable
data class Diagnostic(
    /** Niveau de sévérité */
    val severity: DiagnosticSeverity,
    
    /** Code du diagnostic */
    val code: DiagnosticCode,
    
    /** Message du diagnostic */
    val message: String,
    
    /** Position dans le code source */
    val span: Span = Span.DEFAULT,
    
    /** Contexte supplémentaire (optionnel) */
    val context: Map<String, String> = emptyMap()
) {
    /**
     * Vérifie si ce diagnostic est une erreur.
     */
    fun isError(): Boolean = severity == DiagnosticSeverity.ERROR
    
    /**
     * Vérifie si ce diagnostic est un warning.
     */
    fun isWarning(): Boolean = severity == DiagnosticSeverity.WARNING
    
    /**
     * Vérifie si ce diagnostic est une info.
     */
    fun isInfo(): Boolean = severity == DiagnosticSeverity.INFO
    
    /**
     * Vérifie si ce diagnostic est un debug.
     */
    fun isDebug(): Boolean = severity == DiagnosticSeverity.DEBUG
    
    /**
     * Formate le diagnostic pour affichage.
     */
    fun format(): String {
        val location = if (span.isValid()) " at ${span.start}" else ""
        return "[${severity.name}]${location}: ${code}: ${message}"
    }
    
    override fun toString(): String = format()
}

/**
 * Collection de diagnostics.
 */
@Serializable
data class DiagnosticList(
    val diagnostics: List<Diagnostic> = emptyList()
) {
    /**
     * Ajoute un diagnostic à la liste.
     */
    fun add(diagnostic: Diagnostic): DiagnosticList {
        return DiagnosticList(diagnostics + diagnostic)
    }
    
    /**
     * Vérifie s'il y a des erreurs.
     */
    fun hasErrors(): Boolean = diagnostics.any { it.isError() }
    
    /**
     * Vérifie s'il y a des warnings.
     */
    fun hasWarnings(): Boolean = diagnostics.any { it.isWarning() }
    
    /**
     * Filtre les erreurs.
     */
    fun errors(): List<Diagnostic> = diagnostics.filter { it.isError() }
    
    /**
     * Filtre les warnings.
     */
    fun warnings(): List<Diagnostic> = diagnostics.filter { it.isWarning() }
    
    /**
     * Filtre par sévérité.
     */
    fun filterBySeverity(severity: DiagnosticSeverity): List<Diagnostic> = 
        diagnostics.filter { it.severity == severity }
    
    /**
     * Formate tous les diagnostics pour affichage.
     */
    fun format(): String = diagnostics.joinToString("\n") { it.format() }
    
    companion object {
        /**
         * Liste vide.
         */
        val EMPTY = DiagnosticList(emptyList())
    }
}

/**
 * Builder pour créer des diagnostics facilement.
 */
class DiagnosticBuilder {
    private val diagnostics: MutableList<Diagnostic> = mutableListOf()
    
    /**
     * Ajoute une erreur.
     */
    fun error(
        message: String,
        code: DiagnosticCode = DiagnosticCode.Validation.CUSTOM("UNKNOWN"),
        span: Span = Span.DEFAULT,
        context: Map<String, String> = emptyMap()
    ) {
        diagnostics.add(Diagnostic(
            DiagnosticSeverity.ERROR,
            code,
            message,
            span,
            context
        ))
    }
    
    /**
     * Ajoute un warning.
     */
    fun warning(
        message: String,
        code: DiagnosticCode = DiagnosticCode.Warning.CUSTOM("UNKNOWN"),
        span: Span = Span.DEFAULT,
        context: Map<String, String> = emptyMap()
    ) {
        diagnostics.add(Diagnostic(
            DiagnosticSeverity.WARNING,
            code,
            message,
            span,
            context
        ))
    }
    
    /**
     * Ajoute un info.
     */
    fun info(
        message: String,
        code: DiagnosticCode = DiagnosticCode.Custom("INFO"),
        span: Span = Span.DEFAULT,
        context: Map<String, String> = emptyMap()
    ) {
        diagnostics.add(Diagnostic(
            DiagnosticSeverity.INFO,
            code,
            message,
            span,
            context
        ))
    }
    
    /**
     * Construit la liste de diagnostics.
     */
    fun build(): DiagnosticList = DiagnosticList(diagnostics)
    
    /**
     * Vérifie s'il y a des erreurs.
     */
    fun hasErrors(): Boolean = diagnostics.any { it.isError() }
    
    companion object {
        fun create(): DiagnosticBuilder = DiagnosticBuilder()
    }
}

/**
 * Crée un diagnostic d'erreur rapidement.
 */
fun errorDiagnostic(
    message: String,
    code: DiagnosticCode = DiagnosticCode.Validation.CUSTOM("UNKNOWN"),
    span: Span = Span.DEFAULT
): Diagnostic = Diagnostic(
    DiagnosticSeverity.ERROR,
    code,
    message,
    span
)

/**
 * Crée un diagnostic de warning rapidement.
 */
fun warningDiagnostic(
    message: String,
    code: DiagnosticCode = DiagnosticCode.Warning.CUSTOM("UNKNOWN"),
    span: Span = Span.DEFAULT
): Diagnostic = Diagnostic(
    DiagnosticSeverity.WARNING,
    code,
    message,
    span
)
```

---

### 3. DiagnosticFilter.kt

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/common/DiagnosticFilter.kt`

```kotlin
package io.ygdrasil.wgsl.common

import io.ygdrasil.wgsl.ir.Span
import kotlinx.serialization.Serializable

/**
 * Action à prendre pour un diagnostic.
 */
@Serializable
enum class DiagnosticAction {
    /** Afficher le diagnostic */
    SHOW,
    
    /** Masquer le diagnostic */
    HIDE,
    
    /** Afficher comme erreur (même si c'est un warning) */
    ERROR
}

/**
 * Filtre pour un diagnostic spécifique.
 */
@Serializable
sealed class DiagnosticFilter {
    /**
     * Filtre par code de diagnostic.
     */
    @Serializable
    data class ByCode(
        val code: DiagnosticCode,
        val action: DiagnosticAction
    ) : DiagnosticFilter()
    
    /**
     * Filtre par sévérité.
     */
    @Serializable
    data class BySeverity(
        val severity: DiagnosticSeverity,
        val action: DiagnosticAction
    ) : DiagnosticFilter()
    
    /**
     * Filtre par plage de positions.
     */
    @Serializable
    data class BySpan(
        val span: Span,
        val action: DiagnosticAction
    ) : DiagnosticFilter()
    
    /**
     * Filtre personnalisé (avec prédicat).
     */
    @Serializable
    data class Custom(
        val predicate: (Diagnostic) -> Boolean,
        val action: DiagnosticAction
    ) : DiagnosticFilter()
}

/**
 * Noeud dans l'arbre des filtres de diagnostic.
 * Permet une configuration hiérarchique des filtres.
 */
@Serializable
data class DiagnosticFilterNode(
    val filter: DiagnosticFilter,
    val children: List<DiagnosticFilterNode> = emptyList(),
    val span: Span = Span.DEFAULT
)

/**
 * Applique un filtre à un diagnostic.
 */
fun DiagnosticFilter.apply(diagnostic: Diagnostic): Diagnostic? {
    return when (this) {
        is DiagnosticFilter.ByCode -> {
            if (this.code == diagnostic.code) {
                when (action) {
                    DiagnosticAction.SHOW -> diagnostic
                    DiagnosticAction.HIDE -> null
                    DiagnosticAction.ERROR -> diagnostic.copy(
                        severity = DiagnosticSeverity.ERROR
                    )
                }
            } else {
                diagnostic
            }
        }
        is DiagnosticFilter.BySeverity -> {
            if (this.severity == diagnostic.severity) {
                when (action) {
                    DiagnosticAction.SHOW -> diagnostic
                    DiagnosticAction.HIDE -> null
                    DiagnosticAction.ERROR -> diagnostic.copy(
                        severity = DiagnosticSeverity.ERROR
                    )
                }
            } else {
                diagnostic
            }
        }
        is DiagnosticFilter.BySpan -> {
            if (this.span.contains(diagnostic.span)) {
                when (action) {
                    DiagnosticAction.SHOW -> diagnostic
                    DiagnosticAction.HIDE -> null
                    DiagnosticAction.ERROR -> diagnostic.copy(
                        severity = DiagnosticSeverity.ERROR
                    )
                }
            } else {
                diagnostic
            }
        }
        is DiagnosticFilter.Custom -> {
            if (this.predicate(diagnostic)) {
                when (action) {
                    DiagnosticAction.SHOW -> diagnostic
                    DiagnosticAction.HIDE -> null
                    DiagnosticAction.ERROR -> diagnostic.copy(
                        severity = DiagnosticSeverity.ERROR
                    )
                }
            } else {
                diagnostic
            }
        }
    }
}

/**
 * Applique un noeud de filtre et ses enfants à un diagnostic.
 */
fun DiagnosticFilterNode.apply(diagnostic: Diagnostic): Diagnostic? {
    var result: Diagnostic? = this.filter.apply(diagnostic)
    
    for (child in children) {
        result = result?.let { child.apply(it) }
    }
    
    return result
}

/**
 * Applique une liste de filtres à une liste de diagnostics.
 */
fun List<DiagnosticFilterNode>.apply(diagnostics: List<Diagnostic>): List<Diagnostic> {
    return diagnostics.mapNotNull { diagnostic ->
        var result: Diagnostic? = diagnostic
        for (filter in this) {
            result = result?.let { filter.apply(it) }
        }
        result
    }
}
```

---

### 4. Error.kt (Gestion des Erreurs)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/Error.kt`

```kotlin
package io.ygdrasil.wgsl

import io.ygdrasil.wgsl.common.Diagnostic
import io.ygdrasil.wgsl.common.DiagnosticCode
import io.ygdrasil.wgsl.common.DiagnosticSeverity
import io.ygdrasil.wgsl.ir.Span

/**
 * Exception de base pour les erreurs Naga.
 */
open class NagaException(
    message: String,
    val diagnostics: List<Diagnostic> = emptyList()
) : RuntimeException(message) {
    
    constructor(
        message: String,
        span: Span,
        code: DiagnosticCode = DiagnosticCode.Validation.CUSTOM("UNKNOWN")
    ) : this(message, listOf(Diagnostic(
        DiagnosticSeverity.ERROR,
        code,
        message,
        span
    )))
    
    constructor(
        diagnostic: Diagnostic
    ) : this(diagnostic.message, listOf(diagnostic))
    
    constructor(
        diagnostics: List<Diagnostic>
    ) : this(
        diagnostics.joinToString("; ") { it.message },
        diagnostics
    )
    
    /**
     * Retourne le premier diagnostic qui est une erreur.
     */
    fun firstError(): Diagnostic? = diagnostics.firstOrNull { it.isError() }
    
    /**
     * Formate tous les diagnostics pour affichage.
     */
    fun formatDiagnostics(): String = diagnostics.joinToString("\n") { it.format() }
}

/**
 * Exception pour les erreurs de parsing.
 */
class ParseError(
    message: String,
    span: Span = Span.DEFAULT,
    code: DiagnosticCode = DiagnosticCode.Parse.UNEXPECTED_TOKEN
) : NagaException(message, span, code)

/**
 * Exception pour les erreurs de validation.
 */
class ValidationError(
    message: String,
    span: Span = Span.DEFAULT,
    code: DiagnosticCode = DiagnosticCode.Validation.TYPE_MISMATCH
) : NagaException(message, span, code)

/**
 * Exception pour les erreurs de type.
 */
class TypeError(
    message: String,
    span: Span = Span.DEFAULT
) : NagaException(message, span, DiagnosticCode.Validation.TYPE_MISMATCH)

/**
 * Résultat d'une opération qui peut échouer.
 */
sealed class Result<out T, out E> {
    /**
     * Succès avec une valeur.
     */
    data class Ok<T>(val value: T) : Result<T, Nothing>()
    
    /**
     * Échec avec une erreur.
     */
    data class Err<E>(val error: E) : Result<Nothing, E>()
    
    /**
     * Vérifie si c'est un succès.
     */
    fun isOk(): Boolean = this is Ok<T>
    
    /**
     * Vérifie si c'est un échec.
     */
    fun isErr(): Boolean = this is Err<E>
    
    /**
     * Récupère la valeur (lance une exception si échec).
     */
    fun unwrap(): T where T : Any, E : Throwable {
        return when (this) {
            is Ok -> value
            is Err -> throw error
        }
    }
    
    /**
     * Récupère la valeur ou une valeur par défaut.
     */
    fun unwrapOr(default: T): T {
        return when (this) {
            is Ok -> value
            is Err -> default
        }
    }
    
    /**
     * Récupère la valeur ou null.
     */
    fun getOrNull(): T? {
        return when (this) {
            is Ok -> value
            is Err -> null
        }
    }
    
    /**
     * Récupère l'erreur (lance une exception si succès).
     */
    fun unwrapErr(): E where T : Any, E : Throwable {
        return when (this) {
            is Ok -> throw IllegalStateException("Not an error")
            is Err -> error
        }
    }
    
    /**
     * Applique une fonction à la valeur si succès.
     */
    inline fun <R> map(transform: (T) -> R): Result<R, E> {
        return when (this) {
            is Ok -> Ok(transform(value))
            is Err -> Err(error)
        }
    }
    
    /**
     * Applique une fonction à l'erreur si échec.
     */
    inline fun <F> mapErr(transform: (E) -> F): Result<T, F> {
        return when (this) {
            is Ok -> Ok(value)
            is Err -> Err(transform(error))
        }
    }
    
    /**
     * Chaîne deux opérations.
     */
    inline fun <U> flatMap(f: (T) -> Result<U, E>): Result<U, E> {
        return when (this) {
            is Ok -> f(value)
            is Err -> Err(error)
        }
    }
    
    companion object {
        /**
         * Crée un Result.Ok.
         */
        fun <T> ok(value: T): Result<T, Nothing> = Ok(value)
        
        /**
         * Crée un Result.Err.
         */
        fun <E> err(error: E): Result<Nothing, E> = Err(error)
    }
}

/**
 * Alias pour Result avec NagaException.
 */
typealias NagaResult<T> = Result<T, NagaException>

/**
 * Fonction helper pour créer un Result.Ok.
 */
fun <T> ok(value: T): Result<T, Nothing> = Result.ok(value)

/**
 * Fonction helper pour créer un Result.Err.
 */
fun <E> err(error: E): Result<Nothing, E> = Result.err(error)

/**
 * Fonction helper pour créer un Result.Err avec une NagaException.
 */
fun nagaErr(message: String, span: Span = Span.DEFAULT): NagaResult<Nothing> {
    return Result.err(NagaException(message, span))
}
```

---

### 5. Namer.kt (Génération de Noms)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/Namer.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Arena
import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

/**
 * Génère des noms uniques pour les variables temporaires.
 * Utilisé pour nommer les variables générées automatiquement.
 */
class Namer {
    private var expressionCounter: Int = 0
    private var variableCounter: Int = 0
    private var functionCounter: Int = 0
    
    /**
     * Map des expressions avec des noms.
     */
    val namedExpressions: MutableMap<Handle<Expression>, String> = mutableMapOf()
    
    /**
     * Génère un nom pour une expression temporaire.
     */
    fun nameExpression(expression: Handle<Expression>): String {
        return namedExpressions.getOrPut(expression) {
            "_e${expressionCounter++}"
        }
    }
    
    /**
     * Génère un nom pour une variable locale.
     */
    fun nameLocalVariable(): String {
        return "_v${variableCounter++}"
    }
    
    /**
     * Génère un nom pour une fonction temporaire.
     */
    fun nameFunction(): String {
        return "_f${functionCounter++}"
    }
    
    /**
     * Génère un nom pour un paramètre.
     */
    fun nameParameter(index: Int): String {
        return "_p${index}"
    }
    
    /**
     * Réinitialise le compteur de noms.
     */
    fun reset() {
        expressionCounter = 0
        variableCounter = 0
        functionCounter = 0
        namedExpressions.clear()
    }
    
    companion object {
        fun create(): Namer = Namer()
    }
}

/**
 * Clé pour identifier un élément à nommer.
 */
@Serializable
sealed class NameKey {
    @Serializable
    data class ExpressionKey(val handle: Handle<Expression>) : NameKey()
    
    @Serializable
    data class LocalVariableKey(val handle: Handle<LocalVariable>) : NameKey()
    
    @Serializable
    data class FunctionKey(val handle: Handle<Function>) : NameKey()
    
    @Serializable
    data class EntryPointKey(val index: Int) : NameKey()
    
    @Serializable
    data class ExternalTextureKey(val handle: Handle<GlobalVariable>) : NameKey()
}
```

---

## 🧪 TESTS UNITAIRES

### SpanTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/ir/SpanTest.kt`

```kotlin
package io.ygdrasil.wgsl.ir

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpanTest {
    
    @Test
    fun `test default source location`() {
        val location = SourceLocation.DEFAULT
        
        assertThat(location.line).isEqualTo(0)
        assertThat(location.column).isEqualTo(0)
        assertThat(location.isValid()).isFalse()
    }
    
    @Test
    fun `test valid source location`() {
        val location = SourceLocation(1, 5)
        
        assertThat(location.isValid()).isTrue()
    }
    
    @Test
    fun `test source location compareTo`() {
        val loc1 = SourceLocation(1, 5)
        val loc2 = SourceLocation(1, 10)
        val loc3 = SourceLocation(2, 0)
        
        assertThat(loc1.compareTo(loc2)).isLessThan(0)
        assertThat(loc2.compareTo(loc1)).isGreaterThan(0)
        assertThat(loc1.compareTo(loc1)).isEqualTo(0)
        assertThat(loc2.compareTo(loc3)).isLessThan(0)
    }
    
    @Test
    fun `test default span`() {
        val span = Span.DEFAULT
        
        assertThat(span.isValid()).isFalse()
    }
    
    @Test
    fun `test valid span`() {
        val span = spanOf(1, 0, 1, 10)
        
        assertThat(span.isValid()).isTrue()
        assertThat(span.isSingleLine()).isTrue()
    }
    
    @Test
    fun `test multi line span`() {
        val span = spanOf(1, 0, 3, 5)
        
        assertThat(span.isValid()).isTrue()
        assertThat(span.isSingleLine()).isFalse()
    }
    
    @Test
    fun `test span merge`() {
        val span1 = spanOf(1, 0, 1, 10)
        val span2 = spanOf(2, 0, 2, 10)
        
        val merged = span1.merge(span2)
        
        assertThat(merged.startLine).isEqualTo(1)
        assertThat(merged.startColumn).isEqualTo(0)
        assertThat(merged.endLine).isEqualTo(2)
        assertThat(merged.endColumn).isEqualTo(10)
    }
    
    @Test
    fun `test span contains location`() {
        val span = spanOf(1, 0, 1, 10)
        
        assertThat(span.contains(SourceLocation(1, 0))).isTrue()
        assertThat(span.contains(SourceLocation(1, 5))).isTrue()
        assertThat(span.contains(SourceLocation(1, 10))).isTrue()
        assertThat(span.contains(SourceLocation(1, 11))).isFalse()
        assertThat(span.contains(SourceLocation(2, 0))).isFalse()
    }
    
    @Test
    fun `test span contains span`() {
        val span1 = spanOf(1, 0, 2, 10)
        val span2 = spanOf(1, 5, 1, 8)
        
        assertThat(span1.contains(span2)).isTrue()
    }
    
    @Test
    fun `test span overlaps`() {
        val span1 = spanOf(1, 0, 1, 10)
        val span2 = spanOf(1, 5, 1, 15)
        val span3 = spanOf(2, 0, 2, 10)
        val span4 = spanOf(1, 11, 1, 20)
        
        assertThat(span1.overlaps(span2)).isTrue()
        assertThat(span1.overlaps(span3)).isFalse()
        assertThat(span1.overlaps(span4)).isFalse()
    }
    
    @Test
    fun `test span toString`() {
        val span = spanOf(1, 5, 2, 10)
        
        assertThat(span.toString()).isEqualTo("1:5-2:10")
    }
    
    @Test
    fun `test source location toString`() {
        val location = SourceLocation(42, 13)
        
        assertThat(location.toString()).isEqualTo("42:13")
    }
    
    @Test
    fun `test rangeTo operator`() {
        val start = SourceLocation(1, 0)
        val end = SourceLocation(1, 10)
        val span = start..end
        
        assertThat(span.start).isEqualTo(start)
        assertThat(span.end).isEqualTo(end)
    }
    
    @Test
    fun `test source location toSpan`() {
        val location = SourceLocation(5, 3)
        val span = location.toSpan()
        
        assertThat(span.start).isEqualTo(location)
        assertThat(span.end).isEqualTo(location)
    }
}
```

---

### DiagnosticTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/common/DiagnosticTest.kt`

```kotlin
package io.ygdrasil.wgsl.common

import io.ygdrasil.wgsl.ir.SourceLocation
import io.ygdrasil.wgsl.ir.spanOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DiagnosticTest {
    
    @Test
    fun `test diagnostic severity`() {
        val error = Diagnostic(
            DiagnosticSeverity.ERROR,
            DiagnosticCode.Validation.TYPE_MISMATCH,
            "Type mismatch",
            spanOf(1, 0, 1, 10)
        )
        
        assertThat(error.isError()).isTrue()
        assertThat(error.isWarning()).isFalse()
        assertThat(error.isInfo()).isFalse()
        assertThat(error.isDebug()).isFalse()
    }
    
    @Test
    fun `test diagnostic warning`() {
        val warning = Diagnostic(
            DiagnosticSeverity.WARNING,
            DiagnosticCode.Warning.UNUSED_VARIABLE,
            "Unused variable",
            spanOf(2, 5, 2, 15)
        )
        
        assertThat(warning.isError()).isFalse()
        assertThat(warning.isWarning()).isTrue()
    }
    
    @Test
    fun `test diagnostic format`() {
        val diagnostic = Diagnostic(
            DiagnosticSeverity.ERROR,
            DiagnosticCode.Parse.UNEXPECTED_TOKEN,
            "Unexpected token '}'",
            spanOf(5, 10, 5, 11)
        )
        
        val formatted = diagnostic.format()
        assertThat(formatted).contains("[ERROR]")
        assertThat(formatted).contains("5:10")
        assertThat(formatted).contains("UNEXPECTED_TOKEN")
        assertThat(formatted).contains("Unexpected token '}'")
    }
    
    @Test
    fun `test diagnostic list`() {
        val list = DiagnosticList(listOf(
            Diagnostic(DiagnosticSeverity.ERROR, DiagnosticCode.Validation.TYPE_MISMATCH, "Error 1"),
            Diagnostic(DiagnosticSeverity.WARNING, DiagnosticCode.Warning.UNUSED_VARIABLE, "Warning 1"),
            Diagnostic(DiagnosticSeverity.ERROR, DiagnosticCode.Validation.INVALID_EXPRESSION, "Error 2")
        ))
        
        assertThat(list.hasErrors()).isTrue()
        assertThat(list.hasWarnings()).isTrue()
        assertThat(list.errors()).hasSize(2)
        assertThat(list.warnings()).hasSize(1)
    }
    
    @Test
    fun `test diagnostic builder`() {
        val builder = DiagnosticBuilder.create()
        builder.error("Error 1", span = spanOf(1, 0, 1, 5))
        builder.warning("Warning 1", span = spanOf(2, 0, 2, 5))
        builder.info("Info 1")
        
        val list = builder.build()
        
        assertThat(list.diagnostics).hasSize(3)
        assertThat(list.hasErrors()).isTrue()
    }
}
```

---

### ErrorTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/ErrorTest.kt`

```kotlin
package io.ygdrasil.wgsl

import io.ygdrasil.wgsl.common.Diagnostic
import io.ygdrasil.wgsl.common.DiagnosticCode
import io.ygdrasil.wgsl.common.DiagnosticSeverity
import io.ygdrasil.wgsl.ir.Span
import io.ygdrasil.wgsl.ir.spanOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ErrorTest {
    
    @Test
    fun `test naga exception with message`() {
        val exception = NagaException("Test error")
        
        assertThat(exception.message).isEqualTo("Test error")
        assertThat(exception.diagnostics).isEmpty()
    }
    
    @Test
    fun `test naga exception with diagnostic`() {
        val diagnostic = Diagnostic(
            DiagnosticSeverity.ERROR,
            DiagnosticCode.Validation.TYPE_MISMATCH,
            "Type mismatch",
            spanOf(1, 0, 1, 10)
        )
        val exception = NagaException(diagnostic)
        
        assertThat(exception.message).isEqualTo("Type mismatch")
        assertThat(exception.diagnostics).hasSize(1)
        assertThat(exception.firstError()).isEqualTo(diagnostic)
    }
    
    @Test
    fun `test naga exception with diagnostics`() {
        val diagnostics = listOf(
            Diagnostic(DiagnosticSeverity.ERROR, DiagnosticCode.Parse.UNEXPECTED_TOKEN, "Error 1"),
            Diagnostic(DiagnosticSeverity.WARNING, DiagnosticCode.Warning.UNUSED_VARIABLE, "Warning 1")
        )
        val exception = NagaException(diagnostics)
        
        assertThat(exception.message).isEqualTo("Error 1; Warning 1")
        assertThat(exception.diagnostics).hasSize(2)
    }
    
    @Test
    fun `test parse error`() {
        val error = ParseError("Unexpected token", spanOf(5, 10, 5, 11))
        
        assertThat(error.message).isEqualTo("Unexpected token")
        assertThat(error.diagnostics).hasSize(1)
        assertThat(error.diagnostics[0].code).isInstanceOf(DiagnosticCode.Parse::class.java)
    }
    
    @Test
    fun `test validation error`() {
        val error = ValidationError("Invalid expression", spanOf(3, 5, 3, 15))
        
        assertThat(error.message).isEqualTo("Invalid expression")
        assertThat(error.diagnostics).hasSize(1)
        assertThat(error.diagnostics[0].code).isInstanceOf(DiagnosticCode.Validation::class.java)
    }
    
    @Test
    fun `test result ok`() {
        val result: Result<Int, String> = Result.ok(42)
        
        assertThat(result.isOk()).isTrue()
        assertThat(result.isErr()).isFalse()
        assertThat(result.unwrap()).isEqualTo(42)
        assertThat(result.unwrapOr(0)).isEqualTo(42)
        assertThat(result.getOrNull()).isEqualTo(42)
    }
    
    @Test
    fun `test result err`() {
        val result: Result<Int, String> = Result.err("Error")
        
        assertThat(result.isOk()).isFalse()
        assertThat(result.isErr()).isTrue()
        assertThat(result.unwrapOr(0)).isEqualTo(0)
        assertThat(result.getOrNull()).isNull()
        assertThat(result.unwrapErr()).isEqualTo("Error")
    }
    
    @Test
    fun `test result map`() {
        val result: Result<Int, String> = Result.ok(42)
        val mapped = result.map { it * 2 }
        
        assertThat(mapped.unwrap()).isEqualTo(84)
    }
    
    @Test
    fun `test result flatMap`() {
        val result: Result<Int, String> = Result.ok(42)
        val flatMapped = result.flatMap { 
            if (it > 0) Result.ok(it * 2) else Result.err("Negative")
        }
        
        assertThat(flatMapped.unwrap()).isEqualTo(84)
    }
    
    @Test
    fun `test naga result`() {
        val result: NagaResult<Int> = ok(42)
        
        assertThat(result.isOk()).isTrue()
        assertThat(result.unwrap()).isEqualTo(42)
    }
    
    @Test
    fun `test naga err`() {
        val result: NagaResult<Int> = nagaErr("Test error")
        
        assertThat(result.isErr()).isTrue()
    }
}
```

---

## ✅ CHECKLIST PHASE 1.3

### Types de Base
- [ ] `SourceLocation` (ligne, colonne, isValid, compareTo)
- [ ] `Span` (start, end, isValid, merge, contains, overlaps)
- [ ] `WithSpan` (interface)

### Diagnostics
- [ ] `DiagnosticSeverity` (ERROR, WARNING, INFO, DEBUG)
- [ ] `DiagnosticCode` (Parse, Validation, Warning, Custom)
- [ ] `Diagnostic` (severity, code, message, span, context)
- [ ] `DiagnosticList` (diagnostics, hasErrors, hasWarnings, filter)
- [ ] `DiagnosticBuilder` (error, warning, info, build)

### Filtres de Diagnostic
- [ ] `DiagnosticAction` (SHOW, HIDE, ERROR)
- [ ] `DiagnosticFilter` (ByCode, BySeverity, BySpan, Custom)
- [ ] `DiagnosticFilterNode` (filter, children, span)
- [ ] Fonctions d'application des filtres

### Gestion des Erreurs
- [ ] `NagaException` (message, diagnostics)
- [ ] `ParseError` (message, span, code)
- [ ] `ValidationError` (message, span, code)
- [ ] `TypeError` (message, span)
- [ ] `Result<T, E>` (Ok, Err, unwrap, map, flatMap)
- [ ] `NagaResult<T>` (typealias)
- [ ] Fonctions helpers (ok, err, nagaErr)

### Namer
- [ ] `Namer` (nameExpression, nameLocalVariable, nameFunction, nameParameter)
- [ ] `NameKey` (ExpressionKey, LocalVariableKey, FunctionKey, EntryPointKey, ExternalTextureKey)

### Tests
- [ ] `SpanTest` (tous les tests de Span et SourceLocation)
- [ ] `DiagnosticTest` (tests de Diagnostic et DiagnosticList)
- [ ] `ErrorTest` (tests de Result, NagaException, etc.)

### Documentation
- [ ] KDoc pour toutes les classes publiques
- [ ] KDoc pour toutes les méthodes publiques
- [ ] Exemples d'utilisation

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Implémenter SourceLocation | 0.5 jour | Aucune | [ ] |
| Implémenter Span | 1 jour | SourceLocation | [ ] |
| Implémenter DiagnosticSeverity, DiagnosticCode | 1 jour | Aucune | [ ] |
| Implémenter Diagnostic, DiagnosticList | 1 jour | DiagnosticSeverity, DiagnosticCode, Span | [ ] |
| Implémenter DiagnosticBuilder | 0.5 jour | Diagnostic | [ ] |
| Implémenter DiagnosticFilter | 1 jour | Diagnostic | [ ] |
| Implémenter DiagnosticFilterNode | 0.5 jour | DiagnosticFilter | [ ] |
| Implémenter NagaException, ParseError, ValidationError | 1 jour | Diagnostic | [ ] |
| Implémenter Result<T, E> | 1 jour | Aucune | [ ] |
| Implémenter Namer, NameKey | 0.5 jour | Handle, Expression, Function | [ ] |
| Écrire tests SpanTest | 0.5 jour | Span | [ ] |
| Écrire tests DiagnosticTest | 0.5 jour | Diagnostic | [ ] |
| Écrire tests ErrorTest | 0.5 jour | Result, NagaException | [ ] |
| Ajouter documentation | 1 jour | Tout | [ ] |
| Validation manuelle | 0.5 jour | Tout | [ ] |

**Total estimé** : **1 semaine** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichiers Kotlin** :
   - `Span.kt`
   - `Diagnostic.kt`
   - `DiagnosticFilter.kt`
   - `Error.kt`
   - `Namer.kt`

2. **Tests unitaires** :
   - `SpanTest.kt`
   - `DiagnosticTest.kt`
   - `ErrorTest.kt`

3. **Couverture de test** : > 95%

4. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Span** : `/Users/chaos/RustroverProjects/wgpu/naga/src/span.rs`
- **Diagnostic Debug** : `/Users/chaos/RustroverProjects/wgpu/naga/src/common/diagnostic_debug.rs`
- **Error** : `/Users/chaos/RustroverProjects/wgpu/naga/src/error.rs`
- **Namer** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/namer.rs`

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Implémenter `SourceLocation` et `Span`
2. [ ] Implémenter `DiagnosticCode` et `DiagnosticSeverity`
3. [ ] Implémenter `Diagnostic` et `DiagnosticList`
4. [ ] Implémenter `DiagnosticFilter` et `DiagnosticFilterNode`
5. [ ] Implémenter `Result<T, E>` et les exceptions
6. [ ] Implémenter `Namer` et `NameKey`
7. [ ] Écrire tous les tests unitaires
8. [ ] Valider avec des tests manuels
9. [ ] Passer à la **Phase 2** (Parser WGSL) ou compléter la checklist Phase 1

**Fichier suivant** : `99_checklist.md` ou `02_PARSING/00_wgsl-lexer.md`
