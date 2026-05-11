# ⚠️ Phase 2.4 : Gestion des Erreurs

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-wgsl`  
**Phase** : 2 - Parsing  
**Sous-Phase** : 2.4 - Error Handling  
**Durée** : 1-2 semaines  
**Priorité** : ⭐⭐⭐⭐ (Important - Robustesse du parser)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/error.rs`

---

## 📋 OBJECTIFS

Implémenter un système de **gestion d'erreurs robuste** pour le lexer et le parser WGSL. Ce système doit :
- Capturer toutes les erreurs de syntaxe lexicale et grammaticale
- Fournir des messages d'erreur clairs et précis
- Permettre la **récupération d'erreurs** (error recovery) pour continuer le parsing après une erreur
- Gérer les erreurs multiples dans un seul fichier
- Fournir des informations de position (span) pour chaque erreur

**Livrable principal** : Un système de gestion d'erreurs complet qui permet de parser des fichiers avec des erreurs tout en rapportant tous les problèmes.

---

## 🎯 CONCEPTS CLÉS

### 1. Philosophie de la gestion d'erreurs

**Ne pas échouer sur la première erreur** : Le parser doit continuer à parser même après avoir rencontré une erreur, pour rapports tous les problèmes dans un fichier.

**Récupération d'erreur** : Après une erreur, le parser doit se "récupérer" et continuer à parser à partir d'un point connu (ex: la prochaine instruction, la prochaine déclaration).

**Messages clairs** : Chaque erreur doit avoir :
- Un message descriptif
- Une position précise (span)
- Un code d'erreur optionnel pour categorisation

### 2. Types d'erreurs

| Type d'erreur | Source | Exemple |
|--------------|--------|---------|
| **Lexical Error** | Lexer | Caractère inattendu, nombre mal formé |
| **Syntax Error** | Parser | Token inattendu, structure invalide |
| **Semantic Error** | Type Resolver | Type inconnu, référence non résolue |
| **Validation Error** | Validator | Type incompatible, usage incorrect |

### 3. Niveaux de sévérité

```kotlin
enum class Severity {
    ERROR,      // Problème bloquant, le code ne peut pas être compilé
    WARNING,    // Problème potentiel, le code peut être compilé
    INFO,       // Information, pas un problème
    DEBUG      // Debug, uniquement pour le développement
}
```

### 4. Stratégies de récupération

1. **Skip to next statement** : Après une erreur dans une expression, ignorer jusqu'au point-virgule
2. **Skip to next declaration** : Après une erreur dans une déclaration, ignorer jusqu'au prochain token top-level
3. **Insert token** : Si un token est manquant mais attendu, l'insérer virtuellement
4. **Replace token** : Si un token est incorrect mais proche d'un token valide, le remplacer

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. Diagnostic.kt (Structure de base des diagnostics)

**Fichier** : `naga-wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/Diagnostic.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import dev.gfxrs.naga.span.Span

/**
 * Niveau de sévérité d'un diagnostic.
 */
enum class Severity {
    ERROR,
    WARNING,
    INFO,
    DEBUG
}

/**
 * Code d'erreur pour categorisation.
 * Permet de filtrer ou de traiter différemment certains types d'erreurs.
 */
enum class ErrorCode {
    // Erreurs lexicales
    UNEXPECTED_CHARACTER,
    INVALID_NUMBER,
    UNTERMINATED_STRING,
    UNTERMINATED_COMMENT,
    INVALID_IDENTIFIER,
    
    // Erreurs syntaxiques
    UNEXPECTED_TOKEN,
    EXPECTED_TOKEN,
    EXPECTED_IDENTIFIER,
    EXPECTED_TYPE,
    EXPECTED_EXPRESSION,
    EXPECTED_STATEMENT,
    EXPECTED_DECLARATION,
    MISSING_SEMICOLON,
    MISSING_PAREN,
    MISSING_BRACE,
    MISSING_BRACKET,
    EXTRA_TOKEN,
    
    // Erreurs sémantiques
    UNKNOWN_TYPE,
    UNKNOWN_IDENTIFIER,
    UNKNOWN_FUNCTION,
    DUPLICATE_DECLARATION,
    TYPE_MISMATCH,
    INVALID_TYPE_PARAMETER,
    
    // Erreurs de validation
    INVALID_STATEMENT,
    INCOMPATIBLE_TYPES,
    MISSING_RETURN,
    INVALID_OPERAND,
    INVALID_OPERATOR,
    
    // Autres
    INTERNAL_ERROR,
    NOT_IMPLEMENTED,
    UNRECOGNIZED_FEATURE
}

/**
 * Un diagnostic représente un message (erreur, warning, info) à un endroit précis dans le code.
 */
data class Diagnostic(
    /** Message descriptif */
    val message: String,
    
    /** Span de l'erreur dans le code source */
    val span: Span,
    
    /** Niveau de sévérité */
    val severity: Severity = Severity.ERROR,
    
    /** Code d'erreur pour categorisation */
    val code: ErrorCode? = null,
    
    /** Contexte additionnel (ex: nom du type attendu) */
    val context: Map<String, String> = emptyMap(),
    
    /** Erreurs connexes (pour les erreurs cascades) */
    val related: List<Diagnostic> = emptyList()
) {
    /**
     * Formate le diagnostic pour affichage.
     */
    fun format(source: String): String {
        val location = span.formatLocation(source)
        val severityStr = when (severity) {
            Severity.ERROR -> "error"
            Severity.WARNING -> "warning"
            Severity.INFO -> "info"
            Severity.DEBUG -> "debug"
        }
        val codeStr = code?.name?.lowercase() ?: ""
        
        return buildString {
            appendLine("$severityStr${if (codeStr.isNotEmpty()) "[$codeStr]" else ""}: $message")
            appendLine("  --> $location")
            appendLine(span.formatContext(source))
        }
    }
}

/**
 * Collection de diagnostics avec des méthodes utilitaires.
 */
class DiagnosticCollection {
    private val diagnostics: MutableList<Diagnostic> = mutableListOf()
    
    /** Nombre total de diagnostics */
    val count: Int get() = diagnostics.size
    
    /** Nombre d'erreurs */
    val errorCount: Int get() = diagnostics.count { it.severity == Severity.ERROR }
    
    /** Nombre de warnings */
    val warningCount: Int get() = diagnostics.count { it.severity == Severity.WARNING }
    
    /** Vérifie s'il y a des erreurs */
    fun hasErrors(): Boolean = errorCount > 0
    
    /** Vérifie s'il y a des diagnostics */
    fun hasDiagnostics(): Boolean = count > 0
    
    /** Ajoute un diagnostic */
    fun add(diagnostic: Diagnostic) {
        diagnostics.add(diagnostic)
    }
    
    /** Ajoute une erreur */
    fun error(message: String, span: Span, code: ErrorCode? = null): Diagnostic {
        val diag = Diagnostic(message, span, Severity.ERROR, code)
        diagnostics.add(diag)
        return diag
    }
    
    /** Ajoute un warning */
    fun warning(message: String, span: Span, code: ErrorCode? = null): Diagnostic {
        val diag = Diagnostic(message, span, Severity.WARNING, code)
        diagnostics.add(diag)
        return diag
    }
    
    /** Ajoute une info */
    fun info(message: String, span: Span, code: ErrorCode? = null): Diagnostic {
        val diag = Diagnostic(message, span, Severity.INFO, code)
        diagnostics.add(diag)
        return diag
    }
    
    /** Retourne tous les diagnostics */
    fun getAll(): List<Diagnostic> = diagnostics.toList()
    
    /** Retourne uniquement les erreurs */
    fun getErrors(): List<Diagnostic> = diagnostics.filter { it.severity == Severity.ERROR }
    
    /** Retourne uniquement les warnings */
    fun getWarnings(): List<Diagnostic> = diagnostics.filter { it.severity == Severity.WARNING }
    
    /** Retourne les diagnostics triés par position */
    fun getSorted(): List<Diagnostic> = diagnostics.sortedBy { it.span.start }
    
    /** Fusionne avec une autre collection */
    fun merge(other: DiagnosticCollection): DiagnosticCollection {
        val merged = DiagnosticCollection()
        merged.diagnostics.addAll(this.diagnostics)
        merged.diagnostics.addAll(other.diagnostics)
        return merged
    }
    
    /** Formate tous les diagnostics */
    fun format(source: String): String {
        return diagnostics.joinToString("\n\n") { it.format(source) }
    }
    
    /** Vérifie si un diagnostic existe à une position donnée */
    fun hasAt(span: Span): Boolean = diagnostics.any { it.span.overlaps(span) }
    
    /** Trouve les diagnostics à une position donnée */
    fun findAt(span: Span): List<Diagnostic> = diagnostics.filter { it.span.overlaps(span) }
    
    /** Vide la collection */
    fun clear() {
        diagnostics.clear()
    }
}
```

### 2. ParseError.kt (Erreurs spécifiques au parsing)

**Fichier** : `naga-wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/ParseError.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import dev.gfxrs.naga.span.Span
import dev.gfxrs.naga.front.wgsl.lexer.TokenKind

/**
 * Erreur de parsing avec contexte additionnel.
 */
data class ParseError(
    /** Message d'erreur */
    override val message: String,
    
    /** Position de l'erreur */
    override val span: Span,
    
    /** Token attendu (optionnel) */
    val expected: List<TokenKind> = emptyList(),
    
    /** Token trouvé (optionnel) */
    val found: TokenKind? = null,
    
    /** Suggestions pour corriger l'erreur */
    val suggestions: List<String> = emptyList()
) : Diagnostic(message, span, Severity.ERROR, ErrorCode.UNEXPECTED_TOKEN) {
    
    companion object {
        /**
         * Crée une erreur "token inattendu".
         */
        fun unexpectedToken(
            found: TokenKind,
            span: Span,
            expected: List<TokenKind> = emptyList()
        ): ParseError {
            val expectedStr = if (expected.isNotEmpty()) {
                "expected ${expected.joinToString(" or ") { "'${it}'" }}"
            } else {
                "unexpected token"
            }
            
            val message = "Unexpected token '${found}': $expectedStr"
            val suggestions = generateSuggestions(found, expected)
            
            return ParseError(message, span, expected, found, suggestions)
        }
        
        /**
         * Crée une erreur "token attendu".
         */
        fun expectedToken(
            expected: TokenKind,
            span: Span
        ): ParseError {
            return ParseError(
                "Expected '${expected}'",
                span,
                listOf(expected),
                null,
                listOf("Add '${expected}'")
            )
        }
        
        /**
         * Crée une erreur "token attendu" avec plusieurs options.
         */
        fun expectedOneOf(
            expected: List<TokenKind>,
            found: TokenKind,
            span: Span
        ): ParseError {
            return ParseError(
                "Expected one of: ${expected.joinToString(", ") { "'${it}'" }}",
                span,
                expected,
                found,
                emptyList()
            )
        }
        
        /**
         * Crée une erreur "fin de fichier inattendue".
         */
        fun unexpectedEof(span: Span, expected: String): ParseError {
            return ParseError(
                "Unexpected end of file, expected $expected",
                span,
                emptyList(),
                null,
                listOf("Add $expected")
            )
        }
        
        /**
         * Crée une erreur "déclaration incomplète".
         */
        fun incompleteDeclaration(
            declarationType: String,
            span: Span
        ): ParseError {
            return ParseError(
                "Incomplete $declarationType declaration",
                span,
                emptyList(),
                null,
                listOf("Complete the $declarationType declaration")
            )
        }
        
        /**
         * Génère des suggestions pour corriger une erreur de token.
         */
        private fun generateSuggestions(
            found: TokenKind,
            expected: List<TokenKind>
        ): List<String> {
            val suggestions = mutableListOf<String>()
            
            // Si c'est un point-virgule manquant
            if (found == TokenKind.BRACE_CLOSE && expected.contains(TokenKind.SEMICOLON)) {
                suggestions.add("Add ';' before '}'")
            }
            
            // Si c'est une virgule manquante
            if (found == TokenKind.WORD && expected.contains(TokenKind.COMMA)) {
                suggestions.add("Add ',' between items")
            }
            
            // Si c'est un deux-points manquant
            if (found == TokenKind.ASSIGN && expected.contains(TokenKind.COLON)) {
                suggestions.add("Use ':' for type annotation, '=' for assignment")
            }
            
            // Suggestions générales
            if (expected.size == 1) {
                suggestions.add("Replace '${found}' with '${expected[0]}'")
            }
            
            return suggestions
        }
    }
}

/**
 * Exception levée quand trop d'erreurs sont accumulées.
 * Utilisé pour éviter un temps de parsing excessif sur du code complètement invalide.
 */
class TooManyErrorsException(
    val maxErrors: Int,
    val diagnostics: DiagnosticCollection
) : RuntimeException(
    "Too many errors (${diagnostics.count}), stopping after $maxErrors"
)
```

### 3. ErrorRecovery.kt (Récupération d'erreurs dans le parser)

**Fichier** : `naga-wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/ErrorRecovery.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import dev.gfxrs.naga.front.wgsl.lexer.Token
import dev.gfxrs.naga.front.wgsl.lexer.TokenKind

/**
 * Stratégies de récupération d'erreurs pour le parser.
 * Fournit des méthodes pour récupérer après une erreur et continuer le parsing.
 */
object ErrorRecovery {
    
    /**
     * Récupère jusqu'au prochain token de synchronisation.
     * Les tokens de synchronisation sont des tokens qui marquent le début
     * d'une nouvelle déclaration ou instruction.
     */
    fun recoverToNextStatement(lexer: Lexer, diagnostics: DiagnosticCollection): Token? {
        val syncTokens = setOf(
            TokenKind.SEMICOLON,
            TokenKind.BRACE_CLOSE,
            TokenKind.ELSE,
            TokenKind.CASE,
            TokenKind.DEFAULT,
            TokenKind.EOF
        )
        
        return recoverTo(lexer, syncTokens, diagnostics)
    }
    
    /**
     * Récupère jusqu'au prochain token de déclaration.
     */
    fun recoverToNextDeclaration(lexer: Lexer, diagnostics: DiagnosticCollection): Token? {
        val syncTokens = setOf(
            TokenKind.FN,
            TokenKind.LET,
            TokenKind.CONST,
            TokenKind.VAR,
            TokenKind.STRUCT,
            TokenKind.TYPE,
            TokenKind.OVERRIDE,
            TokenKind.CONST_ASSERT,
            TokenKind.ENABLE,
            TokenKind.REQUIRES,
            TokenKind.EOF
        )
        
        return recoverTo(lexer, syncTokens, diagnostics)
    }
    
    /**
     * Récupère jusqu'à un des tokens spécifiés.
     * Consomme les tokens jusqu'à trouver un token de synchronisation.
     */
    private fun recoverTo(
        lexer: Lexer,
        syncTokens: Set<TokenKind>,
        diagnostics: DiagnosticCollection
    ): Token? {
        var token: Token? = null
        
        while (true) {
            token = lexer.nextToken()
            
            if (token.kind == TokenKind.EOF) {
                return token
            }
            
            if (syncTokens.contains(token.kind)) {
                // On a trouvé un token de synchronisation
                // Si c'est un token qui ferme une structure, le consommer
                if (token.kind == TokenKind.BRACE_CLOSE || 
                    token.kind == TokenKind.PAREN_CLOSE ||
                    token.kind == TokenKind.BRACKET_CLOSE) {
                    lexer.consume()
                }
                return token
            }
            
            // Consommer ce token et continuer
        }
    }
    
    /**
     * Récupère après une erreur dans une expression.
     * Essaye de trouver un point où l'expression peut continuer.
     */
    fun recoverInExpression(lexer: Lexer, diagnostics: DiagnosticCollection): Token? {
        val syncTokens = setOf(
            TokenKind.SEMICOLON,
            TokenKind.COMMA,
            TokenKind.PAREN_CLOSE,
            TokenKind.BRACE_CLOSE,
            TokenKind.BRACKET_CLOSE,
            TokenKind.EOF
        )
        
        return recoverTo(lexer, syncTokens, diagnostics)
    }
    
    /**
     * Tente de corriger une erreur en insérant un token manquant.
     * Par exemple, si un point-virgule est manquant, l'insérer virtuellement.
     */
    fun tryInsertToken(
        lexer: Lexer,
        expected: TokenKind,
        diagnostics: DiagnosticCollection
    ): Boolean {
        // Vérifier si on peut insérer ce token
        when (expected) {
            TokenKind.SEMICOLON -> {
                // Vérifier si le prochain token est un token qui peut suivre un point-virgule
                val next = lexer.peek()
                if (next.kind == TokenKind.BRACE_CLOSE || 
                    next.kind == TokenKind.EOF ||
                    next.kind == TokenKind.ELSE) {
                    // Insérer virtuellement le point-virgule
                    diagnostics.warning(
                        "Missing ';' (auto-inserted)",
                        lexer.currentSpan()
                    )
                    return true
                }
            }
            TokenKind.COLON -> {
                // Vérifier si le prochain token est un type
                val next = lexer.peek()
                if (next.kind == TokenKind.WORD) {
                    // Peut-être que l'utilisateur a oublié le deux-points
                    diagnostics.warning(
                        "Missing ':' (auto-inserted)",
                        lexer.currentSpan()
                    )
                    return true
                }
            }
            else -> {
                // Pour d'autres tokens, ne pas insérer automatiquement
            }
        }
        
        return false
    }
    
    /**
     * Tente de corriger une erreur en remplaçant un token.
     */
    fun tryReplaceToken(
        lexer: Lexer,
        found: TokenKind,
        expected: TokenKind,
        diagnostics: DiagnosticCollection
    ): Boolean {
        // Certaines replacements sont courantes
        when {
            found == TokenKind.ASSIGN && expected == TokenKind.COLON -> {
                // Confusion entre = et :
                diagnostics.warning(
                    "Using '=' instead of ':' for type annotation",
                    lexer.currentSpan()
                )
                lexer.consume() // Consommer le = et continuer comme si c'était un :
                return true
            }
            found == TokenKind.COLON && expected == TokenKind.ASSIGN -> {
                // Confusion entre : et =
                diagnostics.warning(
                    "Using ':' instead of '=' for assignment",
                    lexer.currentSpan()
                )
                return false // Ne pas remplacer, juste avertir
            }
            else -> {
                // Pas de replacement automatique
            }
        }
        
        return false
    }
    
    /**
     * Vérifie si on est dans un état de récupération.
     * Après une erreur, le parser peut être dans un état incohérent.
     */
    fun isRecovering(lexer: Lexer): Boolean {
        // On est en récupération si on a accumulé des erreurs
        // et on n'a pas encore trouvé un point de synchronisation
        return false // À implémenter
    }
}
```

### 4. Parser avec Error Recovery (Modifications à Parser.kt)

```kotlin
class Parser(private val input: String) {
    private val lexer: Lexer = Lexer(input)
    private val builder: AstBuilder = AstBuilder(input)
    
    /** Collection de diagnostics */
    val diagnostics: DiagnosticCollection = DiagnosticCollection()
    
    /** Nombre maximum d'erreurs avant d'arrêter */
    var maxErrors: Int = 100
    
    /** Indique si on est en mode récupération */
    private var recovering: Boolean = false
    
    /** Compteur d'erreurs */
    private var errorCount: Int = 0
    
    fun parse(): TranslationUnit {
        advance()
        skipTrivia()
        
        try {
            while (!match(TokenKind.EOF)) {
                try {
                    val decl = parseTopLevelDecl()
                    if (decl != null) {
                        builder.declarations.append(decl)
                    }
                } catch (e: ParseException) {
                    // Erreur de parsing
                    handleParseError(e)
                }
                skipTrivia()
            }
            
            return builder.buildTranslationUnit()
            
        } catch (e: TooManyErrorsException) {
            // Trop d'erreurs, retourner ce qu'on a
            return builder.buildTranslationUnit()
        }
    }
    
    private fun handleParseError(e: ParseException) {
        errorCount++
        diagnostics.add(e.diagnostic)
        
        if (errorCount >= maxErrors) {
            throw TooManyErrorsException(maxErrors, diagnostics)
        }
        
        // Entrer en mode récupération
        recovering = true
        
        // Récupérer jusqu'au prochain point de synchronisation
        ErrorRecovery.recoverToNextDeclaration(lexer, diagnostics)
        
        // Sortir du mode récupération
        recovering = false
    }
    
    /**
     * Signale une erreur de parsing.
     */
    private fun error(
        message: String,
        span: Span = currentSpan(),
        code: ErrorCode? = null,
        expected: List<TokenKind> = emptyList(),
        found: TokenKind? = currentToken?.kind
    ): Nothing {
        if (recovering) {
            // Déjà en récupération, ne pas ajouter plus d'erreurs
            // mais continuer à essayer de récupérer
            return throw ParseException(
                Diagnostic("Internal error during recovery", span, Severity.ERROR)
            )
        }
        
        val diagnostic = if (found != null && expected.isNotEmpty()) {
            ParseError.unexpectedToken(found, span, expected)
        } else if (found != null) {
            ParseError.unexpectedToken(found, span)
        } else {
            Diagnostic(message, span, Severity.ERROR, code)
        }
        
        throw ParseException(diagnostic)
    }
    
    /**
     * Signale une erreur et tente de récupérer.
     */
    private fun errorAndRecover(
        message: String,
        span: Span = currentSpan(),
        code: ErrorCode? = null
    ) {
        if (recovering) return
        
        errorCount++
        val diagnostic = Diagnostic(message, span, Severity.ERROR, code)
        diagnostics.add(diagnostic)
        
        if (errorCount >= maxErrors) {
            throw TooManyErrorsException(maxErrors, diagnostics)
        }
        
        recovering = true
        ErrorRecovery.recoverToNextDeclaration(lexer, diagnostics)
        recovering = false
    }
    
    /**
     * Signale un warning.
     */
    private fun warning(
        message: String,
        span: Span = currentSpan(),
        code: ErrorCode? = null
    ) {
        diagnostics.warning(message, span, code)
    }
    
    // Modifications aux méthodes de parsing pour gérer la récupération
    
    private fun parseFunctionDecl(): GlobalDecl? {
        return try {
            expect(TokenKind.FN)
            val startSpan = previousSpan()
            
            val attributes = parseAttributes()
            val name = expectIdentifier()
            
            expect(TokenKind.PAREN_OPEN)
            val parameters = parseFunctionParameters()
            expect(TokenKind.PAREN_CLOSE)
            
            val returnType = if (match(TokenKind.COLON)) {
                parseTypeDecl()
            } else {
                null
            }
            
            val body = parseBlock()
            
            builder.function(attributes, name, parameters, returnType, body, spanFrom(startSpan))
            
        } catch (e: ParseException) {
            // Erreur dans la fonction, récupérer et retourner null
            diagnostics.add(e.diagnostic)
            ErrorRecovery.recoverToNextDeclaration(lexer, diagnostics)
            null
        }
    }
    
    private fun parseExpression(): Handle<Expression> {
        return try {
            parseTernaryExpression()
        } catch (e: ParseException) {
            // Erreur dans l'expression, récupérer
            diagnostics.add(e.diagnostic)
            
            // Essayer de récupérer dans l'expression
            ErrorRecovery.recoverInExpression(lexer, diagnostics)
            
            // Retourner une expression d'erreur
            builder.literal(LiteralValue.I32(0), e.diagnostic.span)
        }
    }
    
    // ... autres méthodes modifiées
}

/**
 * Exception levée lors d'une erreur de parsing.
 */
class ParseException(val diagnostic: Diagnostic) : RuntimeException(diagnostic.message)
```

### 5. PrettyPrintError.kt (Affichage joli des erreurs)

**Fichier** : `naga-wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/PrettyPrintError.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import dev.gfxrs.naga.span.Span

/**
 * Extensions pour un affichage joli des erreurs.
 */

/**
 * Formate un span comme "line:column".
 */
fun Span.formatLocation(source: String): String {
    val lines = source.lines()
    var line = 1
    var column = 1
    var charCount = 0
    
    for (i in 0 until start) {
        if (i < source.length && source[i] == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    
    return "line $line, column $column"
}

/**
 * Formate un span avec son contexte dans le code source.
 */
fun Span.formatContext(source: String, contextLines: Int = 2): String {
    val lines = source.lines()
    val startLine = source.substring(0, start).count { it == '\n' } + 1
    val endLine = source.substring(0, end).count { it == '\n' } + 1
    
    val minLine = maxOf(1, startLine - contextLines)
    val maxLine = minOf(lines.size, endLine + contextLines)
    
    val gutterWidth = maxLine.toString().length + 2
    
    return buildString {
        for (lineNum in minLine..maxLine) {
            val lineText = if (lineNum <= lines.size) lines[lineNum - 1] else ""
            
            // Numéro de ligne
            appendLine("%.${gutterWidth}d | %s".format(lineNum, lineText))
            
            // Indicateurs pour le span
            if (lineNum >= startLine && lineNum <= endLine) {
                val lineStart = if (lineNum == startLine) {
                    source.substring(0, start).count { it == '\n' }
                } else {
                    0
                }
                
                val startCol = if (lineNum == startLine) {
                    start - lines.subList(0, startLine - 1).sumOf { it.length + 1 } + 1
                } else {
                    1
                }
                
                val endCol = if (lineNum == endLine) {
                    end - lines.subList(0, endLine - 1).sumOf { it.length + 1 }
                } else {
                    lineText.length
                }
                
                val spaces = " ".repeat(gutterWidth + 3 + startCol - 1)
                val carets = "^".repeat(maxOf(1, endCol - startCol + 1))
                
                appendLine("$spaces$carets")
            }
        }
    }
}

/**
 * Formateur de diagnostics.
 */
class DiagnosticFormatter {
    
    companion object {
        /**
         * Formate une collection de diagnostics.
         */
        fun format(diagnostics: DiagnosticCollection, source: String): String {
            if (diagnostics.count == 0) {
                return "No diagnostics"
            }
            
            val errors = diagnostics.getErrors()
            val warnings = diagnostics.getWarnings()
            
            return buildString {
                if (errors.isNotEmpty()) {
                    appendLine("error: ${errors.size} error(s)")
                }
                if (warnings.isNotEmpty()) {
                    appendLine("warning: ${warnings.size} warning(s)")
                }
                appendLine()
                
                for (diag in diagnostics.getSorted()) {
                    appendLine(diag.format(source))
                    appendLine()
                }
            }
        }
        
        /**
         * Formate un diagnostic unique.
         */
        fun format(diagnostic: Diagnostic, source: String): String {
            return diagnostic.format(source)
        }
    }
}
```

---

## ✅ CHECKLIST PHASE 2.4

### Structures de base
- [ ] Implémenter `Diagnostic` class
- [ ] Implémenter `DiagnosticCollection` class
- [ ] Implémenter `Severity` enum
- [ ] Implémenter `ErrorCode` enum
- [ ] Implémenter `ParseError` class
- [ ] Implémenter `TooManyErrorsException`

### Récupération d'erreurs
- [ ] Implémenter `ErrorRecovery` object
- [ ] Implémenter `recoverToNextStatement`
- [ ] Implémenter `recoverToNextDeclaration`
- [ ] Implémenter `recoverTo` (générique)
- [ ] Implémenter `tryInsertToken`
- [ ] Implémenter `tryReplaceToken`
- [ ] Intégrer la récupération dans Parser

### Affichage des erreurs
- [ ] Implémenter `formatLocation` pour Span
- [ ] Implémenter `formatContext` pour Span
- [ ] Implémenter `DiagnosticFormatter`
- [ ] Implémenter `format` pour Diagnostic

### Intégration
- [ ] Modifier Parser pour utiliser DiagnosticCollection
- [ ] Ajouter `maxErrors` configuration
- [ ] Ajouter `recovering` state
- [ ] Modifier toutes les méthodes de parsing pour gérer les erreurs
- [ ] Ajouter `ParseException`

### Tests
- [ ] DiagnosticTest
- [ ] ParseErrorTest
- [ ] ErrorRecoveryTest
- [ ] DiagnosticFormatterTest
- [ ] Tests d'intégration avec Parser

### Documentation
- [ ] KDoc complet
- [ ] Exemples d'erreurs formatées
- [ ] Documentation des stratégies de récupération

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Implémenter Diagnostic et DiagnosticCollection | 1 jour | Aucun | [ ] |
| Implémenter ErrorCode et Severity | 0.5 jour | Diagnostic | [ ] |
| Implémenter ParseError | 1 jour | Diagnostic | [ ] |
| Implémenter ErrorRecovery | 2 jours | Diagnostic | [ ] |
| Implémenter PrettyPrintError | 1 jour | Diagnostic | [ ] |
| Intégrer avec Parser | 2 jours | Tout | [ ] |
| Écrire les tests | 2 jours | Intégration | [ ] |
| Documentation | 1 jour | Tout | [ ] |
| Validation manuelle | 1 jour | Tout | [ ] |

**Total estimé** : **1-2 semaines** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichiers Kotlin** :
   - `Diagnostic.kt`
   - `ParseError.kt`
   - `ErrorRecovery.kt`
   - `PrettyPrintError.kt`

2. **Fichiers modifiés** :
   - `Parser.kt` (intégration de la gestion d'erreurs)

3. **Tests unitaires** :
   - `DiagnosticTest.kt`
   - `ParseErrorTest.kt`
   - `ErrorRecoveryTest.kt`
   - `DiagnosticFormatterTest.kt`

4. **Couverture de test** : > 95%

5. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Fichier Rust principal** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/error.rs`
- **Fichier précédent** : `03_type-resolution.md`
- **Fichier suivant** : `99_checklist.md`

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Finaliser la conception de Diagnostic
2. [ ] Implémenter Diagnostic et DiagnosticCollection
3. [ ] Implémenter ErrorCode et Severity
4. [ ] Implémenter ParseError
5. [ ] Implémenter ErrorRecovery
6. [ ] Implémenter PrettyPrintError
7. [ ] Intégrer avec Parser
8. [ ] Écrire tous les tests
9. [ ] Valider avec des tests manuels
10. [ ] Passer à `99_checklist.md` et commencer Phase 3
