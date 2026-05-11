# 🔤 Phase 2.0 : Lexer WGSL

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-wgsl`  
**Phase** : 2 - Parsing  
**Sous-Phase** : 2.0 - Lexer WGSL  
**Durée** : 2-3 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Premier pas du parsing)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/lexer.rs` (~800 lignes)

---

## 📋 OBJECTIFS

Implémenter un **lexer (tokenizer)** pour le langage **WGSL** (WebGPU Shading Language).  
Le lexer convertit une chaîne de caractères en une séquence de **tokens** qui seront ensuite parsés.

**Livrable principal** : Un lexer fonctionnel qui peut tokenizer tout code WGSL valide.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce qu'un Lexer ?

Un **lexer** (ou tokenizer) est la première étape du parsing. Il :
- Lit le code source caractère par caractère
- Identifie les **tokens** (mots-clés, identifiants, littéraux, opérateurs, etc.)
- Ignore les espaces et commentaires
- Gère les erreurs de syntaxe basiques

### 2. Tokens WGSL

Les tokens WGSL incluent :
- **Mots-clés** : `fn`, `let`, `if`, `else`, `for`, `while`, `return`, etc.
- **Identifiants** : `main`, `position`, `color`, `my_function`, etc.
- **Littéraux** : `42`, `3.14`, `true`, `false`, `"hello"`
- **Opérateurs** : `+`, `-`, `*`, `/`, `=`, `==`, `!=`, `<`, `>`, etc.
- **Séparateurs** : `(`, `)`, `{`, `}`, `[`, `]`, `,`, `;`, `:`, `.`, `@`
- **Commentaires** : `// commentaire`, `/* commentaire */`

### 3. Différence entre Lexer et Parser

| Lexer | Parser |
|-------|--------|
| Travaille au niveau **caractère** | Travaille au niveau **token** |
| Produit une **liste de tokens** | Produit un **AST (arbre de syntaxe)** |
| Gère la **syntaxe lexicale** | Gère la **grammaire** |
| Exemple : `1 + 2` → `[Token.Number(1), Token.Plus, Token.Number(2)]` | Exemple : `[1, +, 2]` → `BinaryExpression(left=1, op=+, right=2)` |

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. TokenKind.kt

**Fichier** : `naga-wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/TokenKind.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import kotlinx.serialization.Serializable

/**
 * Type de token WGSL.
 * Chaque token a un type (TokenKind) et éventuellement une valeur.
 */
@Serializable
sealed class TokenKind {
    // ===== Fin de fichier =====
    @Serializable
    object EOF : TokenKind()
    
    // ===== Mots-clés WGSL =====
    
    // Déclaration
    @Serializable
    object LET : TokenKind()
    @Serializable
    object CONST : TokenKind()
    @Serializable
    object VAR : TokenKind()
    @Serializable
    object FN : TokenKind()
    @Serializable
    object STRUCT : TokenKind()
    @Serializable
    object ALIAS : TokenKind()
    @Serializable
    object TYPE : TokenKind()
    @Serializable
    object OVERRIDE : TokenKind()
    
    // Contrôle de flux
    @Serializable
    object IF : TokenKind()
    @Serializable
    object ELSE : TokenKind()
    @Serializable
    object SWITCH : TokenKind()
    @Serializable
    object CASE : TokenKind()
    @Serializable
    object DEFAULT : TokenKind()
    @Serializable
    object LOOP : TokenKind()
    @Serializable
    object WHILE : TokenKind()
    @Serializable
    object FOR : TokenKind()
    @Serializable
    object BREAK : TokenKind()
    @Serializable
    object CONTINUE : TokenKind()
    @Serializable
    object RETURN : TokenKind()
    @Serializable
    object DISCARD : TokenKind() // Équivalent à Kill
    
    // Attributs
    @Serializable
    object AT : TokenKind() // @
    @Serializable
    object STAGE : TokenKind()
    @Serializable
    object WORKGROUP_SIZE : TokenKind()
    @Serializable
    object EARLY_DEPTH_TEST : TokenKind()
    @Serializable
    object BINDING : TokenKind()
    @Serializable
    object GROUP : TokenKind()
    @Serializable
    object LOCATION : TokenKind()
    @Serializable
    object BUILTIN : TokenKind()
    @Serializable
    object INTERPOLATE : TokenKind()
    @Serializable
    object INVARIANT : TokenKind()
    @Serializable
    object COMPUTE : TokenKind()
    @Serializable
    object VERTEX : TokenKind()
    @Serializable
    object FRAGMENT : TokenKind()
    @Serializable
    object MESH : TokenKind()
    @Serializable
    object DIAGNOSTIC : TokenKind()
    
    // Types primitifs
    @Serializable
    object BOOL : TokenKind()
    @Serializable
    object I8 : TokenKind()
    @Serializable
    object U8 : TokenKind()
    @Serializable
    object I16 : TokenKind()
    @Serializable
    object U16 : TokenKind()
    @Serializable
    object I32 : TokenKind()
    @Serializable
    object U32 : TokenKind()
    @Serializable
    object I64 : TokenKind()
    @Serializable
    object U64 : TokenKind()
    @Serializable
    object F16 : TokenKind()
    @Serializable
    object F32 : TokenKind()
    @Serializable
    object F64 : TokenKind()
    
    // Types composés
    @Serializable
    object VEC2 : TokenKind()
    @Serializable
    object VEC3 : TokenKind()
    @Serializable
    object VEC4 : TokenKind()
    @Serializable
    object MAT2X2 : TokenKind()
    @Serializable
    object MAT2X3 : TokenKind()
    @Serializable
    object MAT2X4 : TokenKind()
    @Serializable
    object MAT3X2 : TokenKind()
    @Serializable
    object MAT3X3 : TokenKind()
    @Serializable
    object MAT3X4 : TokenKind()
    @Serializable
    object MAT4X2 : TokenKind()
    @Serializable
    object MAT4X3 : TokenKind()
    @Serializable
    object MAT4X4 : TokenKind()
    
    // Classes de types
    @Serializable
    object POINTER : TokenKind()
    @Serializable
    object ARRAY : TokenKind()
    @Serializable
    object SAMPLER : TokenKind()
    @Serializable
    object TEXTURE : TokenKind()
    @Serializable
    object ATOMIC : TokenKind()
    
    // Modificateurs
    @Serializable
    object PRIVATE : TokenKind()
    @Serializable
    object WORKGROUP : TokenKind()
    @Serializable
    object UNIFORM : TokenKind()
    @Serializable
    object STORAGE : TokenKind()
    @Serializable
    object FUNCTION : TokenKind()
    
    // Opérateurs
    @Serializable
    object PLUS : TokenKind() // +
    @Serializable
    object MINUS : TokenKind() // -
    @Serializable
    object STAR : TokenKind() // *
    @Serializable
    object SLASH : TokenKind() // /
    @Serializable
    object PERCENT : TokenKind() // %
    
    @Serializable
    object EQUAL_EQUAL : TokenKind() // ==
    @Serializable
    object NOT_EQUAL : TokenKind() // !=
    @Serializable
    object LESS : TokenKind() // <
    @Serializable
    object LESS_EQUAL : TokenKind() // <=
    @Serializable
    object GREATER : TokenKind() // >
    @Serializable
    object GREATER_EQUAL : TokenKind() // >=
    
    @Serializable
    object AND : TokenKind() // &&
    @Serializable
    object OR : TokenKind() // ||
    @Serializable
    object NOT : TokenKind() // !
    
    @Serializable
    object EQUAL : TokenKind() // =
    @Serializable
    object PLUS_EQUAL : TokenKind() // +=*
    @Serializable
    object MINUS_EQUAL : TokenKind() // -=*
    @Serializable
    object STAR_EQUAL : TokenKind() // *=
    @Serializable
    object SLASH_EQUAL : TokenKind() // /=
    @Serializable
    object PERCENT_EQUAL : TokenKind() // %=
    
    @Serializable
    object AND_EQUAL : TokenKind() // &=
    @Serializable
    object OR_EQUAL : TokenKind() // |=
    @Serializable
    object XOR_EQUAL : TokenKind() // ^=
    @Serializable
    object SHL_EQUAL : TokenKind() // <<=
    @Serializable
    object SHR_EQUAL : TokenKind() // >>=
    
    // Séparateurs
    @Serializable
    object LEFT_PAREN : TokenKind() // (
    @Serializable
    object RIGHT_PAREN : TokenKind() // )
    @Serializable
    object LEFT_BRACE : TokenKind() // {
    @Serializable
    object RIGHT_BRACE : TokenKind() // }
    @Serializable
    object LEFT_BRACKET : TokenKind() // [
    @Serializable
    object RIGHT_BRACKET : TokenKind() // ]
    
    @Serializable
    object COMMA : TokenKind() // ,
    @Serializable
    object DOT : TokenKind() // .
    @Serializable
    object COLON : TokenKind() // :
    @Serializable
    object SEMICOLON : TokenKind() // ;
    @Serializable
    object ARROW : TokenKind() // ->
    @Serializable
    object DOUBLE_ARROW : TokenKind() // =>
    
    // Littéraux
    @Serializable
    object IDENTIFIER : TokenKind() // main, my_var, etc.
    @Serializable
    object INT_LITERAL : TokenKind() // 42, 0x2A, etc.
    @Serializable
    object FLOAT_LITERAL : TokenKind() // 3.14, 1.0, etc.
    @Serializable
    object BOOL_LITERAL : TokenKind() // true, false
    
    // Autres
    @Serializable
    object UNDERSCORE : TokenKind() // _
    
    // Pour l'erreur
    @Serializable
    object ERROR : TokenKind()
}

/**
 * Vérifie si un TokenKind est un mot-clé.
 */
fun TokenKind.isKeyword(): Boolean = when (this) {
    TokenKind.EOF -> false
    TokenKind.IDENTIFIER -> false
    TokenKind.INT_LITERAL -> false
    TokenKind.FLOAT_LITERAL -> false
    TokenKind.BOOL_LITERAL -> false
    TokenKind.ERROR -> false
    TokenKind.UNDERSCORE -> false
    else -> true
}

/**
 * Vérifie si un TokenKind est un type de base.
 */
fun TokenKind.isScalarType(): Boolean = when (this) {
    TokenKind.BOOL, TokenKind.I8, TokenKind.U8,
    TokenKind.I16, TokenKind.U16, TokenKind.I32, TokenKind.U32,
    TokenKind.I64, TokenKind.U64, TokenKind.F16, TokenKind.F32, TokenKind.F64 -> true
    else -> false
}

/**
 * Vérifie si un TokenKind est un type vecteur.
 */
fun TokenKind.isVectorType(): Boolean = when (this) {
    TokenKind.VEC2, TokenKind.VEC3, TokenKind.VEC4 -> true
    else -> false
}

/**
 * Vérifie si un TokenKind est un type matrice.
 */
fun TokenKind.isMatrixType(): Boolean = when (this) {
    TokenKind.MAT2X2, TokenKind.MAT2X3, TokenKind.MAT2X4,
    TokenKind.MAT3X2, TokenKind.MAT3X3, TokenKind.MAT3X4,
    TokenKind.MAT4X2, TokenKind.MAT4X3, TokenKind.MAT4X4 -> true
    else -> false
}

/**
 * Vérifie si un TokenKind est un stage de shader.
 */
fun TokenKind.isShaderStage(): Boolean = when (this) {
    TokenKind.COMPUTE, TokenKind.VERTEX, TokenKind.FRAGMENT, TokenKind.MESH -> true
    else -> false
}
```

---

### 2. Token.kt

**Fichier** : `naga-wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/Token.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import dev.gfxrs.naga.ir.Span
import kotlinx.serialization.Serializable

/**
 * Token WGSL.
 * Représente une unité lexicale du code source.
 * 
 * @property kind Le type du token
 * @property text Le texte original du token (pour debugging)
 * @property span La position dans le code source
 * @property value La valeur du token (pour les littéraux)
 */
@Serializable
data class Token(
    val kind: TokenKind,
    val text: String,
    val span: Span,
    val value: Any? = null
) {
    /**
     * Vérifie si ce token est un EOF.
     */
    fun isEof(): Boolean = kind == TokenKind.EOF
    
    /**
     * Vérifie si ce token est une erreur.
     */
    fun isError(): Boolean = kind == TokenKind.ERROR
    
    /**
     * Vérifie si ce token est un identifiant.
     */
    fun isIdentifier(): Boolean = kind == TokenKind.IDENTIFIER
    
    /**
     * Vérifie si ce token est un littéral.
     */
    fun isLiteral(): Boolean = when (kind) {
        TokenKind.INT_LITERAL, TokenKind.FLOAT_LITERAL, TokenKind.BOOL_LITERAL -> true
        else -> false
    }
    
    /**
     * Récupère la valeur comme String.
     */
    fun stringValue(): String? = value as? String
    
    /**
     * Récupère la valeur comme Int.
     */
    fun intValue(): Int? = value as? Int
    
    /**
     * Récupère la valeur comme Long.
     */
    fun longValue(): Long? = value as? Long
    
    /**
     * Récupère la valeur comme Float.
     */
    fun floatValue(): Float? = value as? Float
    
    /**
     * Récupère la valeur comme Double.
     */
    fun doubleValue(): Double? = value as? Double
    
    /**
     * Récupère la valeur comme Boolean.
     */
    fun boolValue(): Boolean? = value as? Boolean
    
    override fun toString(): String = "Token(${kind}, '${text}', ${span})"
}
```

---

### 3. Lexer.kt (Cœur du Lexer)

**Fichier** : `naga-wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/Lexer.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import dev.gfxrs.naga.ir.SourceLocation
import dev.gfxrs.naga.ir.Span
import dev.gfxrs.naga.ir.spanOf

/**
 * Lexer pour le langage WGSL.
 * Convertit une chaîne de caractères en une séquence de tokens.
 */
class Lexer(private val input: String) : Iterator<Token> {
    
    // Position actuelle dans l'input
    private var position: Int = 0
    private var line: Int = 1
    private var column: Int = 1
    
    // Token actuel (pour peek)
    private var currentToken: Token? = null
    
    // Liste de tous les tokens (pour debugging)
    private val tokens: MutableList<Token> = mutableListOf()
    
    /**
     * Position actuelle.
     */
    val currentPosition: Int get() = position
    
    /**
     * Position actuelle sous forme de SourceLocation.
     */
    val currentLocation: SourceLocation get() = SourceLocation(line, column)
    
    /**
     * Vérifie si la fin de l'input a été atteinte.
     */
    fun isEof(): Boolean = position >= input.length
    
    /**
     * Retourne le prochain caractère sans le consommer.
     */
    private fun peekChar(): Char? = input.getOrNull(position)
    
    /**
     * Retourne le prochain caractère et avance la position.
     */
    private fun nextChar(): Char? {
        val char = input.getOrNull(position)
        if (char != null) {
            position++
            if (char == '\n') {
                line++
                column = 1
            } else {
                column++
            }
        }
        return char
    }
    
    /**
     * Retourne le prochain caractère et avance, ou null si EOF.
     * Lance une exception si le caractère attendu ne correspond pas.
     */
    private fun expectChar(expected: Char): Char {
        val char = nextChar()
        if (char != expected) {
            throw unexpectedCharError(expected, char)
        }
        return char
    }
    
    /**
     * Consomme un caractère si c'est le caractère attendu.
     * Retourne true si le caractère a été consommé.
     */
    private fun consumeChar(expected: Char): Boolean {
        if (peekChar() == expected) {
            nextChar()
            return true
        }
        return false
    }
    
    /**
     * Consomme tous les caractères tant que le prédicat est vrai.
     */
    private inline fun consumeWhile(predicate: (Char) -> Boolean): String {
        val startPosition = position
        val startLine = line
        val startColumn = column
        
        while (peekChar()?.let(predicate) == true) {
            nextChar()
        }
        
        return input.substring(startPosition, position)
    }
    
    /**
     * Consomme tous les espaces (espaces, tabs, nouvelles lignes).
     */
    private fun skipWhitespace() {
        consumeWhile { it.isWhitespace() }
    }
    
    /**
     * Consomme un commentaire de ligne (// ...).
     */
    private fun skipLineComment() {
        if (peekChar() == '/' && input.getOrNull(position + 1) == '/') {
            consumeChar('/')
            consumeChar('/')
            consumeWhile { it != '\n' }
        }
    }
    
    /**
     * Consomme un commentaire de bloc (/* ... */).
     */
    private fun skipBlockComment(): Boolean {
        if (peekChar() == '/' && input.getOrNull(position + 1) == '*') {
            consumeChar('/')
            consumeChar('*')
            
            while (true) {
                if (peekChar() == '*' && input.getOrNull(position + 1) == '/') {
                    consumeChar('*')
                    consumeChar('/')
                    return true
                }
                if (peekChar() == null) {
                    throw lexerError("Unterminated block comment")
                }
                nextChar()
            }
        }
        return false
    }
    
    /**
     * Consomme le prochain token.
     */
    fun nextToken(): Token {
        skipWhitespace()
        
        if (skipBlockComment()) {
            return nextToken()
        }
        
        skipLineComment()
        skipWhitespace()
        
        if (isEof()) {
            return createToken(TokenKind.EOF, "", spanOf(line, column, line, column))
        }
        
        val startPosition = position
        val startLine = line
        val startColumn = column
        
        val char = peekChar()!!
        
        // ===== Identifiants et Mots-clés =====
        if (char.isLetter() || char == '_') {
            val text = consumeWhile { it.isLetterOrDigit() || it == '_' }
            val kind = keywordMap[text] ?: TokenKind.IDENTIFIER
            return createToken(kind, text, spanOf(startLine, startColumn, line, column))
        }
        
        // ===== Littéraux Numériques =====
        if (char.isDigit()) {
            return lexNumber(startLine, startColumn)
        }
        
        // ===== Littéraux Booléens =====
        if (peekChar() == 't' && input.startsWith("true", position)) {
            nextChar() // t
            nextChar() // r
            nextChar() // u
            nextChar() // e
            return createToken(TokenKind.BOOL_LITERAL, "true", spanOf(startLine, startColumn, line, column), true)
        }
        if (peekChar() == 'f' && input.startsWith("false", position)) {
            nextChar() // f
            nextChar() // a
            nextChar() // l
            nextChar() // s
            nextChar() // e
            return createToken(TokenKind.BOOL_LITERAL, "false", spanOf(startLine, startColumn, line, column), false)
        }
        
        // ===== Opérateurs et Séparateurs =====
        return when (char) {
            // Opérateurs à 2 caractères
            '=' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.EQUAL_EQUAL, "==", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.EQUAL, "=", spanOf(startLine, startColumn, line, column))
                }
            }
            '!' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.NOT_EQUAL, "!=", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.NOT, "!", spanOf(startLine, startColumn, line, column))
                }
            }
            '<' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.LESS_EQUAL, "<=", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.LESS, "<", spanOf(startLine, startColumn, line, column))
                }
            }
            '>' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.GREATER_EQUAL, ">=", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.GREATER, ">", spanOf(startLine, startColumn, line, column))
                }
            }
            '&' -> {
                nextChar()
                if (consumeChar('&')) {
                    createToken(TokenKind.AND, "&&", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.AMPERSAND, "&", spanOf(startLine, startColumn, line, column))
                }
            }
            '|' -> {
                nextChar()
                if (consumeChar('|')) {
                    createToken(TokenKind.OR, "||", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.PIPE, "|", spanOf(startLine, startColumn, line, column))
                }
            }
            '+' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.PLUS_EQUAL, "+=", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.PLUS, "+", spanOf(startLine, startColumn, line, column))
                }
            }
            '-' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.MINUS_EQUAL, "-=", spanOf(startLine, startColumn, line, column))
                } else if (consumeChar('>')) {
                    createToken(TokenKind.ARROW, "->", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.MINUS, "-", spanOf(startLine, startColumn, line, column))
                }
            }
            '*' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.STAR_EQUAL, "*=", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.STAR, "*", spanOf(startLine, startColumn, line, column))
                }
            }
            '/' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.SLASH_EQUAL, "/=", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.SLASH, "/", spanOf(startLine, startColumn, line, column))
                }
            }
            '%' -> {
                nextChar()
                if (consumeChar('=')) {
                    createToken(TokenKind.PERCENT_EQUAL, "%=", spanOf(startLine, startColumn, line, column))
                } else {
                    createToken(TokenKind.PERCENT, "%", spanOf(startLine, startColumn, line, column))
                }
            }
            
            // Séparateurs simples
            '(' -> {
                nextChar()
                createToken(TokenKind.LEFT_PAREN, "(", spanOf(startLine, startColumn, line, column))
            }
            ')' -> {
                nextChar()
                createToken(TokenKind.RIGHT_PAREN, ")", spanOf(startLine, startColumn, line, column))
            }
            '{' -> {
                nextChar()
                createToken(TokenKind.LEFT_BRACE, "{", spanOf(startLine, startColumn, line, column))
            }
            '}' -> {
                nextChar()
                createToken(TokenKind.RIGHT_BRACE, "}", spanOf(startLine, startColumn, line, column))
            }
            '[' -> {
                nextChar()
                createToken(TokenKind.LEFT_BRACKET, "[", spanOf(startLine, startColumn, line, column))
            }
            ']' -> {
                nextChar()
                createToken(TokenKind.RIGHT_BRACKET, "]", spanOf(startLine, startColumn, line, column))
            }
            ',' -> {
                nextChar()
                createToken(TokenKind.COMMA, ",", spanOf(startLine, startColumn, line, column))
            }
            '.' -> {
                nextChar()
                createToken(TokenKind.DOT, ".", spanOf(startLine, startColumn, line, column))
            }
            ':' -> {
                nextChar()
                createToken(TokenKind.COLON, ":", spanOf(startLine, startColumn, line, column))
            }
            ';' -> {
                nextChar()
                createToken(TokenKind.SEMICOLON, ";", spanOf(startLine, startColumn, line, column))
            }
            '@' -> {
                nextChar()
                createToken(TokenKind.AT, "@", spanOf(startLine, startColumn, line, column))
            }
            '_' -> {
                nextChar()
                createToken(TokenKind.UNDERSCORE, "_", spanOf(startLine, startColumn, line, column))
            }
            
            // Caractère inattendu
            else -> {
                nextChar()
                createToken(TokenKind.ERROR, char.toString(), spanOf(startLine, startColumn, line, column))
            }
        }
    }
    
    /**
     * Lexe un littéral numérique (entier ou flottant).
     */
    private fun lexNumber(startLine: Int, startColumn: Int): Token {
        val startPosition = position
        var isFloat = false
        var isHex = false
        var isNegative = false
        
        // Gérer le signe négatif (pour les flottants)
        if (peekChar() == '-') {
            nextChar()
            isNegative = true
        }
        
        // Gérer le préfixe hexadécimal
        if (peekChar() == '0' && input.getOrNull(position + 1)?.lowercaseChar() == 'x') {
            nextChar() // 0
            nextChar() // x
            isHex = true
            consumeWhile { it.isHexDigit() }
        } else {
            // Littéral décimal
            consumeWhile { it.isDigit() }
            
            // Partie décimale
            if (peekChar() == '.') {
                isFloat = true
                nextChar() // .
                consumeWhile { it.isDigit() }
            }
            
            // Exposant
            if (peekChar()?.lowercaseChar() in setOf('e', 'p')) {
                isFloat = true
                nextChar() // e ou p
                if (peekChar() in setOf('+', '-')) {
                    nextChar()
                }
                consumeWhile { it.isDigit() }
            }
        }
        
        val text = input.substring(startPosition, position)
        val fullText = if (isNegative) "-" + text else text
        
        // Calculer la valeur
        val value = try {
            if (isHex) {
                Long.parseLong(fullText.substring(2), 16)
            } else if (isFloat) {
                fullText.toDouble()
            } else {
                fullText.toLong()
            }
        } catch (e: NumberFormatException) {
            throw lexerError("Invalid number literal: ${fullText}")
        }
        
        val kind = if (isFloat) TokenKind.FLOAT_LITERAL else TokenKind.INT_LITERAL
        return createToken(kind, fullText, spanOf(startLine, startColumn, line, column), value)
    }
    
    /**
     * Crée un token avec les informations données.
     */
    private fun createToken(
        kind: TokenKind,
        text: String,
        span: Span,
        value: Any? = null
    ): Token {
        val token = Token(kind, text, span, value)
        tokens.add(token)
        return token
    }
    
    /**
     * Lance une erreur de lexer.
     */
    private fun lexerError(message: String): Nothing {
        throw LexerException(message, SourceLocation(line, column))
    }
    
    /**
     * Lance une erreur de caractère inattendu.
     */
    private fun unexpectedCharError(expected: Char, actual: Char?): Nothing {
        val actualText = actual?.toString() ?: "EOF"
        throw LexerException("Expected '$expected' but found '$actualText'", SourceLocation(line, column))
    }
    
    // ===== Iterator Implementation =====
    
    override fun hasNext(): Boolean = !isEof() || currentToken == null
    
    override fun next(): Token {
        if (currentToken != null) {
            val token = currentToken!!
            currentToken = null
            return token
        }
        return nextToken()
    }
    
    /**
     * Retourne le prochain token sans le consommer.
     */
    fun peek(): Token {
        if (currentToken == null) {
            currentToken = nextToken()
        }
        return currentToken!!
    }
    
    /**
     * Consomme le prochain token si c'est du type attendu.
     * Retourne true si le token a été consommé.
     */
    fun consume(expected: TokenKind): Boolean {
        if (peek().kind == expected) {
            next()
            return true
        }
        return false
    }
    
    /**
     * Consomme le prochain token et retourne true si c'est du type attendu.
     * Lance une exception sinon.
     */
    fun expect(expected: TokenKind): Token {
        val token = next()
        if (token.kind != expected) {
            throw LexerException(
                "Expected ${expected} but found ${token.kind}",
                token.span.start
            )
        }
        return token
    }
    
    /**
     * Retourne tous les tokens restants.
     */
    fun allTokens(): List<Token> {
        val result = mutableListOf<Token>()
        while (hasNext()) {
            result.add(next())
        }
        return result
    }
    
    /**
     * Réinitialise le lexer.
     */
    fun reset() {
        position = 0
        line = 1
        column = 1
        currentToken = null
        tokens.clear()
    }
    
    companion object {
        /**
         * Map des mots-clés WGSL vers leurs TokenKind.
         */
        private val keywordMap: Map<String, TokenKind> = mapOf(
            // Déclaration
            "let" to TokenKind.LET,
            "const" to TokenKind.CONST,
            "var" to TokenKind.VAR,
            "fn" to TokenKind.FN,
            "struct" to TokenKind.STRUCT,
            "alias" to TokenKind.ALIAS,
            "type" to TokenKind.TYPE,
            "override" to TokenKind.OVERRIDE,
            
            // Contrôle de flux
            "if" to TokenKind.IF,
            "else" to TokenKind.ELSE,
            "switch" to TokenKind.SWITCH,
            "case" to TokenKind.CASE,
            "default" to TokenKind.DEFAULT,
            "loop" to TokenKind.LOOP,
            "while" to TokenKind.WHILE,
            "for" to TokenKind.FOR,
            "break" to TokenKind.BREAK,
            "continue" to TokenKind.CONTINUE,
            "return" to TokenKind.RETURN,
            "discard" to TokenKind.DISCARD,
            
            // Attributs
            "stage" to TokenKind.STAGE,
            "workgroup_size" to TokenKind.WORKGROUP_SIZE,
            "early_depth_test" to TokenKind.EARLY_DEPTH_TEST,
            "binding" to TokenKind.BINDING,
            "group" to TokenKind.GROUP,
            "location" to TokenKind.LOCATION,
            "builtin" to TokenKind.BUILTIN,
            "interpolate" to TokenKind.INTERPOLATE,
            "invariant" to TokenKind.INVARIANT,
            "diagnostic" to TokenKind.DIAGNOSTIC,
            
            // Stages
            "compute" to TokenKind.COMPUTE,
            "vertex" to TokenKind.VERTEX,
            "fragment" to TokenKind.FRAGMENT,
            "mesh" to TokenKind.MESH,
            
            // Types primitifs
            "bool" to TokenKind.BOOL,
            "i8" to TokenKind.I8,
            "u8" to TokenKind.U8,
            "i16" to TokenKind.I16,
            "u16" to TokenKind.U16,
            "i32" to TokenKind.I32,
            "u32" to TokenKind.U32,
            "i64" to TokenKind.I64,
            "u64" to TokenKind.U64,
            "f16" to TokenKind.F16,
            "f32" to TokenKind.F32,
            "f64" to TokenKind.F64,
            
            // Types vecteurs
            "vec2" to TokenKind.VEC2,
            "vec3" to TokenKind.VEC3,
            "vec4" to TokenKind.VEC4,
            
            // Types matrices
            "mat2x2" to TokenKind.MAT2X2,
            "mat2x3" to TokenKind.MAT2X3,
            "mat2x4" to TokenKind.MAT2X4,
            "mat3x2" to TokenKind.MAT3X2,
            "mat3x3" to TokenKind.MAT3X3,
            "mat3x4" to TokenKind.MAT3X4,
            "mat4x2" to TokenKind.MAT4X2,
            "mat4x3" to TokenKind.MAT4X3,
            "mat4x4" to TokenKind.MAT4X4,
            
            // Classes de types
            "pointer" to TokenKind.POINTER,
            "array" to TokenKind.ARRAY,
            "sampler" to TokenKind.SAMPLER,
            "texture" to TokenKind.TEXTURE,
            "atomic" to TokenKind.ATOMIC,
            
            // Modificateurs
            "private" to TokenKind.PRIVATE,
            "workgroup" to TokenKind.WORKGROUP,
            "uniform" to TokenKind.UNIFORM,
            "storage" to TokenKind.STORAGE,
            "function" to TokenKind.FUNCTION
        )
        
        /**
         * Crée un lexer à partir d'une chaîne.
         */
        fun fromString(input: String): Lexer = Lexer(input)
        
        /**
         * Tokenize une chaîne complète.
         */
        fun tokenize(input: String): List<Token> {
            return Lexer(input).allTokens()
        }
    }
}

/**
 * Exception levée par le lexer.
 */
class LexerException(
    message: String,
    val location: SourceLocation
) : RuntimeException("$message at ${location}")
```

---

## 🧪 TESTS UNITAIRES

### LexerTest.kt

**Fichier** : `naga-wgsl/src/test/kotlin/dev/gfxrs/naga/front/wgsl/LexerTest.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import dev.gfxrs.naga.ir.Span
import dev.gfxrs.naga.ir.spanOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LexerTest {
    
    @Test
    fun `test empty input`() {
        val lexer = Lexer.fromString("")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(1)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.EOF)
    }
    
    @Test
    fun `test whitespace`() {
        val lexer = Lexer.fromString("   \t\n  ")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(1)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.EOF)
    }
    
    @Test
    fun `test line comment`() {
        val lexer = Lexer.fromString("// This is a comment\n42")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2) // INT_LITERAL et EOF
        assertThat(tokens[0].kind).isEqualTo(TokenKind.INT_LITERAL)
        assertThat(tokens[0].intValue()).isEqualTo(42)
    }
    
    @Test
    fun `test block comment`() {
        val lexer = Lexer.fromString("/* This is a\n   multiline comment */42")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2) // INT_LITERAL et EOF
        assertThat(tokens[0].kind).isEqualTo(TokenKind.INT_LITERAL)
        assertThat(tokens[0].intValue()).isEqualTo(42)
    }
    
    @Test
    fun `test integer literal`() {
        val lexer = Lexer.fromString("42")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.INT_LITERAL)
        assertThat(tokens[0].longValue()).isEqualTo(42)
        assertThat(tokens[0].text).isEqualTo("42")
    }
    
    @Test
    fun `test negative integer literal`() {
        val lexer = Lexer.fromString("-42")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.INT_LITERAL)
        assertThat(tokens[0].longValue()).isEqualTo(-42)
        assertThat(tokens[0].text).isEqualTo("-42")
    }
    
    @Test
    fun `test hex integer literal`() {
        val lexer = Lexer.fromString("0x2A")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.INT_LITERAL)
        assertThat(tokens[0].longValue()).isEqualTo(0x2A)
        assertThat(tokens[0].text).isEqualTo("0x2A")
    }
    
    @Test
    fun `test float literal`() {
        val lexer = Lexer.fromString("3.14")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.FLOAT_LITERAL)
        assertThat(tokens[0].doubleValue()).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001))
        assertThat(tokens[0].text).isEqualTo("3.14")
    }
    
    @Test
    fun `test float literal with exponent`() {
        val lexer = Lexer.fromString("1.5e10")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.FLOAT_LITERAL)
        assertThat(tokens[0].doubleValue()).isCloseTo(1.5e10, org.assertj.core.data.Offset.offset(0.1e10))
    }
    
    @Test
    fun `test float literal with p suffix`() {
        val lexer = Lexer.fromString("1.5p10")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.FLOAT_LITERAL)
    }
    
    @Test
    fun `test bool literal true`() {
        val lexer = Lexer.fromString("true")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.BOOL_LITERAL)
        assertThat(tokens[0].boolValue()).isTrue()
    }
    
    @Test
    fun `test bool literal false`() {
        val lexer = Lexer.fromString("false")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.BOOL_LITERAL)
        assertThat(tokens[0].boolValue()).isFalse()
    }
    
    @Test
    fun `test identifier`() {
        val lexer = Lexer.fromString("my_variable123")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.IDENTIFIER)
        assertThat(tokens[0].text).isEqualTo("my_variable123")
    }
    
    @Test
    fun `test identifier starting with underscore`() {
        val lexer = Lexer.fromString("_private")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.IDENTIFIER)
        assertThat(tokens[0].text).isEqualTo("_private")
    }
    
    @Test
    fun `test keyword fn`() {
        val lexer = Lexer.fromString("fn")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.FN)
    }
    
    @Test
    fun `test keyword let`() {
        val lexer = Lexer.fromString("let")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.LET)
    }
    
    @Test
    fun `test keyword if`() {
        val lexer = Lexer.fromString("if")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.IF)
    }
    
    @Test
    fun `test keyword vertex`() {
        val lexer = Lexer.fromString("vertex")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.VERTEX)
    }
    
    @Test
    fun `test keyword vec4`() {
        val lexer = Lexer.fromString("vec4")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.VEC4)
    }
    
    @Test
    fun `test operators`() {
        val lexer = Lexer.fromString("+ - * / %")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(6) // 5 opérateurs + EOF
        assertThat(tokens[0].kind).isEqualTo(TokenKind.PLUS)
        assertThat(tokens[1].kind).isEqualTo(TokenKind.MINUS)
        assertThat(tokens[2].kind).isEqualTo(TokenKind.STAR)
        assertThat(tokens[3].kind).isEqualTo(TokenKind.SLASH)
        assertThat(tokens[4].kind).isEqualTo(TokenKind.PERCENT)
    }
    
    @Test
    fun `test comparison operators`() {
        val lexer = Lexer.fromString("== != < <= > >=")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(7) // 6 opérateurs + EOF
        assertThat(tokens[0].kind).isEqualTo(TokenKind.EQUAL_EQUAL)
        assertThat(tokens[1].kind).isEqualTo(TokenKind.NOT_EQUAL)
        assertThat(tokens[2].kind).isEqualTo(TokenKind.LESS)
        assertThat(tokens[3].kind).isEqualTo(TokenKind.LESS_EQUAL)
        assertThat(tokens[4].kind).isEqualTo(TokenKind.GREATER)
        assertThat(tokens[5].kind).isEqualTo(TokenKind.GREATER_EQUAL)
    }
    
    @Test
    fun `test assignment operators`() {
        val lexer = Lexer.fromString("= += -= *= /=")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(6) // 5 opérateurs + EOF
        assertThat(tokens[0].kind).isEqualTo(TokenKind.EQUAL)
        assertThat(tokens[1].kind).isEqualTo(TokenKind.PLUS_EQUAL)
        assertThat(tokens[2].kind).isEqualTo(TokenKind.MINUS_EQUAL)
        assertThat(tokens[3].kind).isEqualTo(TokenKind.STAR_EQUAL)
        assertThat(tokens[4].kind).isEqualTo(TokenKind.SLASH_EQUAL)
    }
    
    @Test
    fun `test arrow operator`() {
        val lexer = Lexer.fromString("->")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.ARROW)
    }
    
    @Test
    fun `test separators`() {
        val lexer = Lexer.fromString("(){}[]:;.,")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(11) // 10 séparateurs + EOF
        assertThat(tokens[0].kind).isEqualTo(TokenKind.LEFT_PAREN)
        assertThat(tokens[1].kind).isEqualTo(TokenKind.RIGHT_PAREN)
        assertThat(tokens[2].kind).isEqualTo(TokenKind.LEFT_BRACE)
        assertThat(tokens[3].kind).isEqualTo(TokenKind.RIGHT_BRACE)
        assertThat(tokens[4].kind).isEqualTo(TokenKind.LEFT_BRACKET)
        assertThat(tokens[5].kind).isEqualTo(TokenKind.RIGHT_BRACKET)
        assertThat(tokens[6].kind).isEqualTo(TokenKind.COLON)
        assertThat(tokens[7].kind).isEqualTo(TokenKind.SEMICOLON)
        assertThat(tokens[8].kind).isEqualTo(TokenKind.DOT)
        assertThat(tokens[9].kind).isEqualTo(TokenKind.COMMA)
    }
    
    @Test
    fun `test at sign`() {
        val lexer = Lexer.fromString("@")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].kind).isEqualTo(TokenKind.AT)
    }
    
    @Test
    fun `test complex expression`() {
        val input = "let x: vec4<f32> = vec4<f32>(1.0, 2.0, 3.0, 4.0);"
        val lexer = Lexer.fromString(input)
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(15) // 14 tokens + EOF
        assertThat(tokens[0].kind).isEqualTo(TokenKind.LET)
        assertThat(tokens[1].kind).isEqualTo(TokenKind.IDENTIFIER)
        assertThat(tokens[2].kind).isEqualTo(TokenKind.COLON)
        assertThat(tokens[3].kind).isEqualTo(TokenKind.VEC4)
        assertThat(tokens[4].kind).isEqualTo(TokenKind.LESS)
        assertThat(tokens[5].kind).isEqualTo(TokenKind.F32)
        assertThat(tokens[6].kind).isEqualTo(TokenKind.EQUAL)
        assertThat(tokens[7].kind).isEqualTo(TokenKind.VEC4)
        // ... etc.
    }
    
    @Test
    fun `test peek`() {
        val lexer = Lexer.fromString("42 + 100")
        
        val firstPeek = lexer.peek()
        assertThat(firstPeek.kind).isEqualTo(TokenKind.INT_LITERAL)
        assertThat(firstPeek.intValue()).isEqualTo(42)
        
        val first = lexer.next()
        assertThat(first.kind).isEqualTo(TokenKind.INT_LITERAL)
        
        val secondPeek = lexer.peek()
        assertThat(secondPeek.kind).isEqualTo(TokenKind.PLUS)
    }
    
    @Test
    fun `test consume`() {
        val lexer = Lexer.fromString("+ 42")
        
        assertThat(lexer.consume(TokenKind.PLUS)).isTrue()
        assertThat(lexer.peek().kind).isEqualTo(TokenKind.INT_LITERAL)
        
        assertThat(lexer.consume(TokenKind.MINUS)).isFalse()
        assertThat(lexer.peek().kind).isEqualTo(TokenKind.INT_LITERAL)
    }
    
    @Test
    fun `test expect`() {
        val lexer = Lexer.fromString("+ 42")
        
        val token = lexer.expect(TokenKind.PLUS)
        assertThat(token.kind).isEqualTo(TokenKind.PLUS)
    }
    
    @Test
    fun `test expect wrong token`() {
        val lexer = Lexer.fromString("+ 42")
        
        assertThatThrownBy { lexer.expect(TokenKind.MINUS) }
            .isInstanceOf(LexerException::class)
    }
    
    @Test
    fun `test span tracking`() {
        val lexer = Lexer.fromString("let x = 42;")
        val tokens = lexer.allTokens()
        
        // Vérifier que les spans sont corrects
        assertThat(tokens[0].span.startLine).isEqualTo(1)
        assertThat(tokens[0].span.startColumn).isEqualTo(1)
        assertThat(tokens[0].span.endLine).isEqualTo(1)
        assertThat(tokens[0].span.endColumn).isEqualTo(3)
        
        assertThat(tokens[1].span.startColumn).isEqualTo(5)
        assertThat(tokens[1].span.endColumn).isEqualTo(5)
    }
    
    @Test
    fun `test multiline input`() {
        val lexer = Lexer.fromString("let x = 1;\nlet y = 2;")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(9) // let, x, =, 1, ;, let, y, =, 2, ;, EOF
        
        // Vérifier les lignes
        assertThat(tokens[0].span.startLine).isEqualTo(1)
        assertThat(tokens[5].span.startLine).isEqualTo(2)
    }
    
    @Test
    fun `test unterminated block comment`() {
        val lexer = Lexer.fromString("/* unterminated comment")
        
        assertThatThrownBy { lexer.allTokens() }
            .isInstanceOf(LexerException::class)
            .hasMessageContaining("Unterminated block comment")
    }
    
    @Test
    fun `test invalid character`() {
        val lexer = Lexer.fromString("x @# y")
        val tokens = lexer.allTokens()
        
        assertThat(tokens).hasSize(4) // IDENTIFIER, ERROR, IDENTIFIER, EOF
        assertThat(tokens[1].kind).isEqualTo(TokenKind.ERROR)
    }
}
```

---

## ✅ CHECKLIST PHASE 2.0

### TokenKind.kt
- [ ] Définir tous les TokenKind (50+ variants)
- [ ] Implémenter `isKeyword()`
- [ ] Implémenter `isScalarType()`
- [ ] Implémenter `isVectorType()`
- [ ] Implémenter `isMatrixType()`
- [ ] Implémenter `isShaderStage()`

### Token.kt
- [ ] Définir la data class Token
- [ ] Implémenter `isEof()`, `isError()`, `isIdentifier()`, `isLiteral()`
- [ ] Implémenter les fonctions de conversion (intValue, floatValue, etc.)

### Lexer.kt
- [ ] Implémenter la classe Lexer (Iterator<Token>)
- [ ] Implémenter `peekChar()`, `nextChar()`, `expectChar()`, `consumeChar()`
- [ ] Implémenter `skipWhitespace()`
- [ ] Implémenter `skipLineComment()` et `skipBlockComment()`
- [ ] Implémenter `consumeWhile()`
- [ ] Implémenter `lexNumber()` (entiers, flottants, hexadécimaux)
- [ ] Implémenter la détection des mots-clés
- [ ] Implémenter la détection des identifiants
- [ ] Implémenter la détection de tous les opérateurs
- [ ] Implémenter la détection de tous les séparateurs
- [ ] Implémenter `peek()`, `consume()`, `expect()`
- [ ] Implémenter `allTokens()` et `reset()`
- [ ] Implémenter le suivi des positions (line, column)

### keywordMap
- [ ] Ajouter tous les mots-clés WGSL (50+)
- [ ] Vérifier que tous les mots-clés sont couverts

### Tests
- [ ] `LexerTest` avec 25+ tests
- [ ] Tests pour tous les types de tokens
- [ ] Tests pour les commentaires
- [ ] Tests pour les littéraux (entiers, flottants, booléens)
- [ ] Tests pour les opérateurs (simples et composés)
- [ ] Tests pour les séparateurs
- [ ] Tests pour le suivi des positions (Span)
- [ ] Tests pour les erreurs (commentaire non terminé, caractère invalide)

### Documentation
- [ ] KDoc pour toutes les classes et méthodes
- [ ] Documentation des algorithmes de lexing

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Lire lexer.rs Rust | 1-2 jours | Aucune | [ ] |
| Implémenter TokenKind.kt | 1 jour | Aucune | [ ] |
| Implémenter Token.kt | 0.5 jour | TokenKind | [ ] |
| Implémenter keywordMap | 1 jour | TokenKind | [ ] |
| Implémenter Lexer (base) | 2 jours | Token, TokenKind | [ ] |
| Implémenter lexNumber() | 1 jour | Lexer base | [ ] |
| Implémenter la détection des mots-clés | 0.5 jour | keywordMap | [ ] |
| Implémenter la détection des opérateurs | 1 jour | Lexer base | [ ] |
| Implémenter peek/consume/expect | 0.5 jour | Lexer | [ ] |
| Écrire LexerTest (15 tests) | 1 jour | Lexer | [ ] |
| Écrire LexerTest (10 tests supplémentaires) | 1 jour | Lexer | [ ] |
| Ajouter documentation | 0.5 jour | Tout | [ ] |
| Validation manuelle | 1 jour | Tout | [ ] |

**Total estimé** : **2-3 semaines** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichiers Kotlin** :
   - `TokenKind.kt`
   - `Token.kt`
   - `Lexer.kt`

2. **Tests unitaires** :
   - `LexerTest.kt` (25+ tests)

3. **Couverture de test** : > 95%

4. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Lexer Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/lexer.rs`
- **WGSL Spec** : https://gpuweb.github.io/gpuweb/wgsl/#lexical-structure

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Lire attentivement `lexer.rs` Rust
2. [ ] Implémenter `TokenKind.kt` avec tous les tokens
3. [ ] Implémenter `Token.kt`
4. [ ] Implémenter `Lexer.kt` (version de base)
5. [ ] Implémenter `lexNumber()` et les autres méthodes de lexing
6. [ ] Écrire les tests unitaires
7. [ ] Valider avec des exemples WGSL
8. [ ] Passer à `01_wgsl-parser.md` (Parser)

**Fichier suivant** : `01_wgsl-parser.md`
