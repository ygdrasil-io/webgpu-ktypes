package io.ygdrasil.wgsl.parser

import io.ygdrasil.wgsl.ast.ArrayType
import io.ygdrasil.wgsl.ast.Attribute
import io.ygdrasil.wgsl.ast.BinaryExpr
import io.ygdrasil.wgsl.ast.BinaryOperator
import io.ygdrasil.wgsl.ast.BlockStatement
import io.ygdrasil.wgsl.ast.BoolLiteral
import io.ygdrasil.wgsl.ast.BreakStatement
import io.ygdrasil.wgsl.ast.BuiltinValue
import io.ygdrasil.wgsl.ast.Case
import io.ygdrasil.wgsl.ast.ConstAssertDecl
import io.ygdrasil.wgsl.ast.ContinueStatement
import io.ygdrasil.wgsl.ast.DefaultCase
import io.ygdrasil.wgsl.ast.DiscardStatement
import io.ygdrasil.wgsl.ast.EntryPointAttribute
import io.ygdrasil.wgsl.ast.Expression
import io.ygdrasil.wgsl.ast.ExpressionStatement
import io.ygdrasil.wgsl.ast.FloatLiteral
import io.ygdrasil.wgsl.ast.ForStatement
import io.ygdrasil.wgsl.ast.FragmentInput
import io.ygdrasil.wgsl.ast.FunctionDecl
import io.ygdrasil.wgsl.ast.GlobalDecl
import io.ygdrasil.wgsl.ast.IdentExpr
import io.ygdrasil.wgsl.ast.IfStatement
import io.ygdrasil.wgsl.ast.IndexExpr
import io.ygdrasil.wgsl.ast.IntLiteral
import io.ygdrasil.wgsl.ast.LoopStatement
import io.ygdrasil.wgsl.ast.MatrixType
import io.ygdrasil.wgsl.ast.MemberAccessExpr
import io.ygdrasil.wgsl.ast.NamedType
import io.ygdrasil.wgsl.ast.OverrideDecl
import io.ygdrasil.wgsl.ast.Param
import io.ygdrasil.wgsl.ast.ReturnStatement
import io.ygdrasil.wgsl.ast.ScalarKind
import io.ygdrasil.wgsl.ast.ScalarType
import io.ygdrasil.wgsl.ast.Statement
import io.ygdrasil.wgsl.ast.StringLiteral
import io.ygdrasil.wgsl.ast.StructDecl
import io.ygdrasil.wgsl.ast.StructMember
import io.ygdrasil.wgsl.ast.SwitchBody
import io.ygdrasil.wgsl.ast.SwitchCase
import io.ygdrasil.wgsl.ast.SwitchStatement
import io.ygdrasil.wgsl.ast.TemplateParam
import io.ygdrasil.wgsl.ast.TernaryExpr
import io.ygdrasil.wgsl.ast.TranslationUnit
import io.ygdrasil.wgsl.ast.TypeAliasDecl
import io.ygdrasil.wgsl.ast.TypeDecl
import io.ygdrasil.wgsl.ast.UnaryExpr
import io.ygdrasil.wgsl.ast.UnaryOperator
import io.ygdrasil.wgsl.ast.VariableDecl
import io.ygdrasil.wgsl.ast.VariableDeclKind
import io.ygdrasil.wgsl.ast.VariableDeclStatement
import io.ygdrasil.wgsl.ast.VectorType
import io.ygdrasil.wgsl.ast.VertexOutput
import io.ygdrasil.wgsl.ast.WhileStatement
import io.ygdrasil.wgsl.ir.Span
import io.ygdrasil.wgsl.lexer.Lexer
import io.ygdrasil.wgsl.lexer.Token
import io.ygdrasil.wgsl.lexer.TokenKind

/**
 * Parser for the WGSL shader language.
 * 
 * The parser converts a stream of tokens from the lexer into an Abstract Syntax Tree (AST).
 * It uses recursive descent parsing with operator precedence for expressions.
 * 
 * The parser maintains the current token and provides methods for parsing
 * different language constructs (expressions, statements, declarations, etc.).
 */
class Parser(
    /** The lexer providing tokens. */
    private val lexer: Lexer,
) {
    /** The current token. */
    private var currentToken: Token = lexer.next() ?: Token.eof(Span.UNDEFINED)
    
    /** The previous token (for error recovery). */
    private var previousToken: Token? = null
    
    /** true if we've seen an error during parsing. */
    private var hasError: Boolean = false
    
    /** The list of errors encountered during parsing. */
    private val errors: MutableList<ParseError> = mutableListOf()

    /** Returns true if we've reached the end of the token stream. */
    private fun isAtEnd(): Boolean = currentToken.isEof

    /** Returns the kind of the current token. */
    private fun currentKind(): TokenKind = currentToken.kind

    /**
     * Advances to the next token.
     */
    private fun advance(): Token {
        previousToken = currentToken
        currentToken = lexer.next() ?: Token.eof(Span.UNDEFINED)
        return previousToken!!
    }

    /**
     * Returns the current token without consuming it.
     */
    private fun peek(): Token = currentToken

    /**
     * Consumes the current token if it matches the expected kind.
     * 
     * @param kind The expected token kind
     * @return true if the token matched and was consumed
     */
    private fun expect(kind: TokenKind): Boolean {
        if (currentKind() == kind) {
            advance()
            return true
        }
        return false
    }

    /**
     * Consumes the current token if it matches any of the expected kinds.
     * 
     * @param kinds The expected token kinds
     * @return true if the token matched and was consumed
     */
    private fun expectAny(vararg kinds: TokenKind): Boolean {
        if (kinds.contains(currentKind())) {
            advance()
            return true
        }
        return false
    }

    /**
     * Reports a parse error at the current position.
     */
    private fun error(message: String) {
        val span = currentToken.span
        errors.add(ParseError(message, span))
        hasError = true
    }

    /**
     * Reports a parse error at the previous token position.
     */
    private fun errorAtPrevious(message: String) {
        val span = previousToken?.span ?: currentToken.span
        errors.add(ParseError(message, span))
        hasError = true
    }

    /**
     * Expects the current token to be of the given kind, and reports an error if not.
     */
    private fun expectOrError(kind: TokenKind, message: String): Boolean {
        if (expect(kind)) {
            return true
        }
        error("Expected ${kind}, found ${currentKind()}")
        return false
    }

    /**
     * Parses the entire source and returns the translation unit.
     */
    fun parse(): TranslationUnit {
        val declarations = mutableListOf<GlobalDecl>()
        
        while (!isAtEnd()) {
            declarations.add(parseTopLevelDecl())
        }
        
        val start = declarations.firstOrNull()?.span?.start ?: 0u
        val end = declarations.lastOrNull()?.span?.end ?: 0u
        val span = Span(start, end)
        
        return TranslationUnit(declarations, span)
    }

    /**
     * Parses a top-level declaration.
     */
    private fun parseTopLevelDecl(): GlobalDecl {
        return when (currentKind()) {
            TokenKind.FN -> parseFunctionDecl()
            TokenKind.STRUCT -> parseStructDecl()
            TokenKind.LET, TokenKind.CONST, TokenKind.VAR -> parseVariableDecl()
            TokenKind.TYPE -> parseTypeAliasDecl()
            TokenKind.OVERRIDE -> parseOverrideDecl()
            TokenKind.CONST_ASSERT -> parseConstAssertDecl()
            TokenKind.AT -> {
                advance() // consume @
                parseAttributeOrGroup()
            }
            else -> {
                // Try to recover
                error("Unexpected token ${currentKind()} at top level")
                advance()
                // Return a dummy declaration
                parseTopLevelDecl()
            }
        }
    }

    /**
     * Parses a function declaration.
     */
    private fun parseFunctionDecl(): FunctionDecl {
        val start = currentToken.span
        
        // Consume 'fn'
        expectOrError(TokenKind.FN, "Expected 'fn'")
        
        // Parse attributes (if any)
        val attributes = mutableListOf<Attribute>()
        while (currentKind() == TokenKind.AT) {
            attributes.add(parseAttribute())
        }
        
        // Parse name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected function name")
            ""
        }
        
        // Parse template parameters (if any)
        val templateParams = mutableListOf<TemplateParam>()
        if (currentKind() == TokenKind.LEFT_ANGLE) {
            templateParams.addAll(parseTemplateParamList())
        }
        
        // Parse parameters
        expectOrError(TokenKind.LEFT_PAREN, "Expected '('")
        val parameters = parseParameterList()
        expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
        
        // Parse return type (if any)
        val returnType = if (currentKind() == TokenKind.ARROW) {
            advance() // consume ->
            parseTypeDecl()
        } else {
            null
        }
        
        // Parse body (if any)
        val body = if (currentKind() == TokenKind.LEFT_BRACE) {
            parseBlockStatement()
        } else {
            null
        }
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return FunctionDecl(
            attributes = attributes,
            name = name,
            templateParams = templateParams,
            parameters = parameters,
            returnType = returnType,
            body = body,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses a struct declaration.
     */
    private fun parseStructDecl(): StructDecl {
        val start = currentToken.span
        
        // Consume 'struct'
        expectOrError(TokenKind.STRUCT, "Expected 'struct'")
        
        // Parse name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected struct name")
            ""
        }
        
        // Parse template parameters (if any)
        val templateParams = mutableListOf<TemplateParam>()
        if (currentKind() == TokenKind.LEFT_ANGLE) {
            templateParams.addAll(parseTemplateParamList())
        }
        
        // Parse members
        expectOrError(TokenKind.LEFT_BRACE, "Expected '{'")
        val members = mutableListOf<StructMember>()
        while (currentKind() != TokenKind.RIGHT_BRACE && !isAtEnd()) {
            members.add(parseStructMember())
        }
        expectOrError(TokenKind.RIGHT_BRACE, "Expected '}'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return StructDecl(
            attributes = emptyList(), // TODO: parse attributes
            name = name,
            templateParams = templateParams,
            members = members,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses a struct member.
     */
    private fun parseStructMember(): StructMember {
        val start = currentToken.span
        
        // Parse attributes (if any)
        val attributes = mutableListOf<Attribute>()
        while (currentKind() == TokenKind.AT) {
            attributes.add(parseAttribute())
        }
        
        // Parse name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected member name")
            ""
        }
        
        // Parse type
        expectOrError(TokenKind.COLON, "Expected ':'")
        val type = parseTypeDecl()
        
        // Parse default value (if any)
        val defaultValue = if (currentKind() == TokenKind.ASSIGN) {
            advance()
            parseExpression()
        } else {
            null
        }
        
        expectOrError(TokenKind.SEMICOLON, "Expected ';'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return StructMember(
            attributes = attributes,
            name = name,
            type = type,
            defaultValue = defaultValue,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses a variable declaration (global or local).
     */
    private fun parseVariableDecl(): VariableDecl {
        val start = currentToken.span
        
        // Parse kind (let, const, var)
        val kind = when (currentKind()) {
            TokenKind.LET -> VariableDeclKind.LET
            TokenKind.CONST -> VariableDeclKind.CONST
            TokenKind.VAR -> VariableDeclKind.VAR
            else -> {
                error("Expected 'let', 'const', or 'var'")
                VariableDeclKind.LET
            }
        }
        advance()
        
        // Parse name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected variable name")
            ""
        }
        
        // Parse type annotation (if any)
        val type = if (currentKind() == TokenKind.COLON) {
            advance()
            parseTypeDecl()
        } else {
            null
        }
        
        // Parse initializer (required for const)
        val initializer = if (currentKind() == TokenKind.ASSIGN) {
            advance()
            parseExpression()
        } else {
            null
        }
        
        // For const, initializer is required
        if (kind == VariableDeclKind.CONST && initializer == null) {
            error("Const declarations must have an initializer")
        }
        
        expectOrError(TokenKind.SEMICOLON, "Expected ';'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return VariableDecl(
            kind = kind,
            attributes = emptyList(), // TODO: parse attributes
            name = name,
            type = type,
            initializer = initializer,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses a type alias declaration.
     */
    private fun parseTypeAliasDecl(): TypeAliasDecl {
        val start = currentToken.span
        
        // Consume 'type'
        expectOrError(TokenKind.TYPE, "Expected 'type'")
        
        // Parse name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected type alias name")
            ""
        }
        
        // Parse template parameters (if any)
        val templateParams = mutableListOf<TemplateParam>()
        if (currentKind() == TokenKind.LEFT_ANGLE) {
            templateParams.addAll(parseTemplateParamList())
        }
        
        // Parse '=' and type
        expectOrError(TokenKind.ASSIGN, "Expected '='")
        val type = parseTypeDecl()
        
        expectOrError(TokenKind.SEMICOLON, "Expected ';'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return TypeAliasDecl(
            attributes = emptyList(), // TODO: parse attributes
            name = name,
            templateParams = templateParams,
            type = type,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses an override declaration.
     */
    private fun parseOverrideDecl(): OverrideDecl {
        val start = currentToken.span
        
        // Consume 'override'
        expectOrError(TokenKind.OVERRIDE, "Expected 'override'")
        
        // Parse entry point attribute
        val entryPoint = when (currentKind()) {
            TokenKind.COMPUTE -> {
                advance()
                EntryPointAttribute.Compute
            }
            TokenKind.FRAGMENT -> {
                advance()
                val inputs = mutableListOf<FragmentInput>()
                if (currentKind() == TokenKind.LEFT_PAREN) {
                    advance()
                    while (currentKind() != TokenKind.RIGHT_PAREN && !isAtEnd()) {
                        inputs.add(parseFragmentInput())
                        if (currentKind() == TokenKind.COMMA) {
                            advance()
                        }
                    }
                    expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
                }
                EntryPointAttribute.Fragment(inputs)
            }
            TokenKind.VERTEX -> {
                advance()
                val outputs = mutableListOf<VertexOutput>()
                if (currentKind() == TokenKind.LEFT_PAREN) {
                    advance()
                    while (currentKind() != TokenKind.RIGHT_PAREN && !isAtEnd()) {
                        outputs.add(parseVertexOutput())
                        if (currentKind() == TokenKind.COMMA) {
                            advance()
                        }
                    }
                    expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
                }
                EntryPointAttribute.Vertex(outputs)
            }
            else -> {
                error("Expected entry point attribute (compute, fragment, vertex)")
                EntryPointAttribute.Compute
            }
        }
        
        // Parse function
        val function = parseFunctionDecl()
        
        val end = function.span.end
        return OverrideDecl(
            attributes = emptyList(), // TODO: parse attributes
            entryPoint = entryPoint,
            function = function,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses a const assert declaration.
     */
    private fun parseConstAssertDecl(): ConstAssertDecl {
        val start = currentToken.span
        
        // Consume 'const_assert'
        expectOrError(TokenKind.CONST_ASSERT, "Expected 'const_assert'")
        expectOrError(TokenKind.LEFT_PAREN, "Expected '('")
        
        val expression = parseExpression()
        
        expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
        expectOrError(TokenKind.SEMICOLON, "Expected ';'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return ConstAssertDecl(
            expression = expression,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses an attribute.
     */
    private fun parseAttribute(): Attribute {
        val start = currentToken.span
        
        // Consume '@'
        expectOrError(TokenKind.AT, "Expected '@'")
        
        // Parse attribute name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected attribute name")
            ""
        }
        
        // Parse arguments (if any)
        val args = mutableListOf<Expression>()
        if (currentKind() == TokenKind.LEFT_PAREN) {
            advance()
            while (currentKind() != TokenKind.RIGHT_PAREN && !isAtEnd()) {
                args.add(parseExpression())
                if (currentKind() == TokenKind.COMMA) {
                    advance()
                }
            }
            expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
        }
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return Attribute(
            name = name,
            args = args,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses an attribute or a group of attributes.
     */
    private fun parseAttributeOrGroup(): GlobalDecl {
        // This is a placeholder - attributes at top level can apply to the next declaration
        // For now, we'll just parse the attribute and return a dummy declaration
        val attribute = parseAttribute()
        // TODO: associate attribute with the next declaration
        return parseTopLevelDecl()
    }

    /**
     * Parses a list of template parameters.
     */
    private fun parseTemplateParamList(): List<TemplateParam> {
        val params = mutableListOf<TemplateParam>()
        
        expectOrError(TokenKind.LEFT_ANGLE, "Expected '<'")
        
        while (currentKind() != TokenKind.RIGHT_ANGLE && !isAtEnd()) {
            params.add(parseTemplateParam())
            if (currentKind() == TokenKind.COMMA) {
                advance()
            }
        }
        
        expectOrError(TokenKind.RIGHT_ANGLE, "Expected '>'")
        
        return params
    }

    /**
     * Parses a template parameter.
     */
    private fun parseTemplateParam(): TemplateParam {
        val start = currentToken.span
        
        // Parse name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected template parameter name")
            ""
        }
        
        // Parse constraint (if any)
        val constraint = if (currentKind() == TokenKind.COLON) {
            advance()
            parseTypeDecl()
        } else {
            null
        }
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return TemplateParam(
            name = name,
            constraint = constraint,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses a parameter list.
     */
    private fun parseParameterList(): List<Param> {
        val params = mutableListOf<Param>()
        
        while (currentKind() != TokenKind.RIGHT_PAREN && !isAtEnd()) {
            params.add(parseParameter())
            if (currentKind() == TokenKind.COMMA) {
                advance()
            }
        }
        
        return params
    }

    /**
     * Parses a parameter.
     */
    private fun parseParameter(): Param {
        val start = currentToken.span
        
        // Parse attributes (if any)
        val attributes = mutableListOf<Attribute>()
        while (currentKind() == TokenKind.AT) {
            attributes.add(parseAttribute())
        }
        
        // Parse name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected parameter name")
            ""
        }
        
        // Parse type
        expectOrError(TokenKind.COLON, "Expected ':'")
        val type = parseTypeDecl()
        
        // Parse default value (if any)
        val defaultValue = if (currentKind() == TokenKind.ASSIGN) {
            advance()
            parseExpression()
        } else {
            null
        }
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return Param(
            attributes = attributes,
            name = name,
            type = type,
            defaultValue = defaultValue,
            span = Span(start.start, end)
        )
    }

    /**
     * Parses a type declaration.
     */
    private fun parseTypeDecl(): TypeDecl {
        val start = currentToken.span
        
        when (currentKind()) {
            TokenKind.BOOL -> {
                advance()
                return ScalarType(ScalarKind.BOOL, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.I8 -> {
                advance()
                return ScalarType(ScalarKind.I8, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.U8 -> {
                advance()
                return ScalarType(ScalarKind.U8, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.I16 -> {
                advance()
                return ScalarType(ScalarKind.I16, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.U16 -> {
                advance()
                return ScalarType(ScalarKind.U16, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.I32 -> {
                advance()
                return ScalarType(ScalarKind.I32, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.U32 -> {
                advance()
                return ScalarType(ScalarKind.U32, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.I64 -> {
                advance()
                return ScalarType(ScalarKind.I64, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.U64 -> {
                advance()
                return ScalarType(ScalarKind.U64, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.F16 -> {
                advance()
                return ScalarType(ScalarKind.F16, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.F32 -> {
                advance()
                return ScalarType(ScalarKind.F32, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.F64 -> {
                advance()
                return ScalarType(ScalarKind.F64, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.VEC -> {
                advance()
                return parseVectorType(start)
            }
            TokenKind.MAT -> {
                advance()
                return parseMatrixType(start)
            }
            TokenKind.ARRAY -> {
                advance()
                return parseArrayType(start)
            }
            TokenKind.IDENTIFIER -> {
                val nameToken = advance()
                return NamedType(nameToken.literal ?: "", nameToken.span)
            }
            else -> {
                error("Expected a type")
                return NamedType("", start)
            }
        }
    }

    /**
     * Parses a vector type like `vec2<f32>`.
     */
    private fun parseVectorType(start: Span): VectorType {
        val size = if (currentKind() == TokenKind.INT_LITERAL) {
            val sizeToken = advance()
            sizeToken.literal?.toIntOrNull() ?: 2
        } else {
            error("Expected vector size")
            2
        }
        
        expectOrError(TokenKind.LEFT_ANGLE, "Expected '<'")
        val elementType = parseTypeDecl()
        expectOrError(TokenKind.RIGHT_ANGLE, "Expected '>'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return VectorType(size, elementType, Span(start.start, end))
    }

    /**
     * Parses a matrix type like `mat2x3<f32>`.
     */
    private fun parseMatrixType(start: Span): MatrixType {
        val columns = if (currentKind() == TokenKind.INT_LITERAL) {
            val colToken = advance()
            colToken.literal?.toIntOrNull() ?: 2
        } else {
            error("Expected matrix column count")
            2
        }
        
        expectOrError(TokenKind.IDENTIFIER, "Expected 'x'")
        
        val rows = if (currentKind() == TokenKind.INT_LITERAL) {
            val rowToken = advance()
            rowToken.literal?.toIntOrNull() ?: 2
        } else {
            error("Expected matrix row count")
            2
        }
        
        expectOrError(TokenKind.LEFT_ANGLE, "Expected '<'")
        val elementType = parseTypeDecl()
        expectOrError(TokenKind.RIGHT_ANGLE, "Expected '>'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return MatrixType(columns, rows, elementType, Span(start.start, end))
    }

    /**
     * Parses an array type like `array<i32, 4>`.
     */
    private fun parseArrayType(start: Span): ArrayType {
        expectOrError(TokenKind.LEFT_ANGLE, "Expected '<'")
        
        val elementType = parseTypeDecl()
        
        // Parse length (if present)
        var length: Expression? = null
        var stride: Int? = null
        
        if (currentKind() == TokenKind.COMMA) {
            advance()
            length = parseExpression()
            
            if (currentKind() == TokenKind.COMMA) {
                advance()
                if (currentKind() == TokenKind.INT_LITERAL) {
                    val strideToken = advance()
                    stride = strideToken.literal?.toIntOrNull()
                }
            }
        }
        
        expectOrError(TokenKind.RIGHT_ANGLE, "Expected '>'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return ArrayType(elementType, length, stride, Span(start.start, end))
    }

    /**
     * Parses a fragment input.
     */
    private fun parseFragmentInput(): FragmentInput {
        var location: Int? = null
        var builtin: BuiltinValue? = null
        
        if (currentKind() == TokenKind.AT) {
            advance()
            if (currentKind() == TokenKind.LOCATION) {
                advance()
                if (currentKind() == TokenKind.LEFT_PAREN) {
                    advance()
                    if (currentKind() == TokenKind.INT_LITERAL) {
                        val locToken = advance()
                        location = locToken.literal?.toIntOrNull()
                    }
                    expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
                }
            } else if (currentKind() == TokenKind.BUILTIN) {
                advance()
                if (currentKind() == TokenKind.LEFT_PAREN) {
                    advance()
                    // Parse builtin value
                    builtin = parseBuiltinValue()
                    expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
                }
            }
        }
        
        expectOrError(TokenKind.COLON, "Expected ':'")
        val type = parseTypeDecl()
        
        return FragmentInput(location, builtin, type)
    }

    /**
     * Parses a vertex output.
     */
    private fun parseVertexOutput(): VertexOutput {
        var location: Int? = null
        var builtin: BuiltinValue? = null
        
        if (currentKind() == TokenKind.AT) {
            advance()
            if (currentKind() == TokenKind.LOCATION) {
                advance()
                if (currentKind() == TokenKind.LEFT_PAREN) {
                    advance()
                    if (currentKind() == TokenKind.INT_LITERAL) {
                        val locToken = advance()
                        location = locToken.literal?.toIntOrNull()
                    }
                    expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
                }
            } else if (currentKind() == TokenKind.BUILTIN) {
                advance()
                if (currentKind() == TokenKind.LEFT_PAREN) {
                    advance()
                    // Parse builtin value
                    builtin = parseBuiltinValue()
                    expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
                }
            }
        }
        
        expectOrError(TokenKind.COLON, "Expected ':'")
        val type = parseTypeDecl()
        
        return VertexOutput(location, builtin, type)
    }

    /**
     * Parses a builtin value.
     */
    private fun parseBuiltinValue(): BuiltinValue {
        when (currentKind()) {
            TokenKind.POSITION -> { advance(); return BuiltinValue.POSITION }
            TokenKind.VERTEX_INDEX -> { advance(); return BuiltinValue.VERTEX_INDEX }
            TokenKind.INSTANCE_INDEX -> { advance(); return BuiltinValue.INSTANCE_INDEX }
            TokenKind.FRONT_FACING -> { advance(); return BuiltinValue.FRONT_FACING }
            TokenKind.PRIMITIVE_INDEX -> { advance(); return BuiltinValue.PRIMITIVE_INDEX }
            TokenKind.SAMPLE_INDEX -> { advance(); return BuiltinValue.SAMPLE_INDEX }
            TokenKind.SAMPLE_MASK -> { advance(); return BuiltinValue.SAMPLE_MASK }
            TokenKind.VIEWPORT_INDEX -> { advance(); return BuiltinValue.VIEWPORT_INDEX }
            TokenKind.POINTSIZE -> { advance(); return BuiltinValue.POINTSIZE }
            TokenKind.CLIP_DISTANCES -> { advance(); return BuiltinValue.CLIP_DISTANCES }
            TokenKind.CULL_DISTANCES -> { advance(); return BuiltinValue.CULL_DISTANCES }
            TokenKind.DEVICE_INDEX -> { advance(); return BuiltinValue.DEVICE_INDEX }
            TokenKind.VIEW_INDEX -> { advance(); return BuiltinValue.VIEW_INDEX }
            TokenKind.WORKGROUP_ID -> { advance(); return BuiltinValue.WORKGROUP_ID }
            TokenKind.NUM_WORKGROUPS -> { advance(); return BuiltinValue.NUM_WORKGROUPS }
            TokenKind.GLOBAL_INVOCATION_ID -> { advance(); return BuiltinValue.GLOBAL_INVOCATION_ID }
            TokenKind.LOCAL_INVOCATION_ID -> { advance(); return BuiltinValue.LOCAL_INVOCATION_ID }
            TokenKind.LOCAL_INVOCATION_INDEX -> { advance(); return BuiltinValue.LOCAL_INVOCATION_INDEX }
            else -> {
                error("Expected builtin value")
                return BuiltinValue.POSITION
            }
        }
    }

    // ========================================================================
    // Expression parsing
    // ========================================================================

    /**
     * Parses an expression with the lowest precedence.
     */
    private fun parseExpression(): Expression {
        return parseTernaryExpression()
    }

    /**
     * Parses a ternary (conditional) expression.
     */
    private fun parseTernaryExpression(): Expression {
        val left = parseLogicalOrExpression()
        
        if (currentKind() == TokenKind.QUESTION) {
            advance()
            val trueExpr = parseExpression()
            expectOrError(TokenKind.COLON, "Expected ':' after ternary true expression")
            val falseExpr = parseExpression()
            
            val start = left.span.start
            val end = previousToken?.span?.end ?: currentToken.span.end
            return TernaryExpr(left, trueExpr, falseExpr, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a logical OR expression.
     */
    private fun parseLogicalOrExpression(): Expression {
        var left = parseLogicalAndExpression()
        
        while (currentKind() == TokenKind.OR) {
            advance()
            val right = parseLogicalAndExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, BinaryOperator.LOGICAL_OR, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a logical AND expression.
     */
    private fun parseLogicalAndExpression(): Expression {
        var left = parseBitwiseOrExpression()
        
        while (currentKind() == TokenKind.AND) {
            advance()
            val right = parseBitwiseOrExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, BinaryOperator.LOGICAL_AND, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a bitwise OR expression.
     */
    private fun parseBitwiseOrExpression(): Expression {
        var left = parseBitwiseXorExpression()
        
        while (currentKind() == TokenKind.PIPE) {
            advance()
            val right = parseBitwiseXorExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, BinaryOperator.BITWISE_OR, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a bitwise XOR expression.
     */
    private fun parseBitwiseXorExpression(): Expression {
        var left = parseBitwiseAndExpression()
        
        while (currentKind() == TokenKind.CARET) {
            advance()
            val right = parseBitwiseAndExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, BinaryOperator.BITWISE_XOR, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a bitwise AND expression.
     */
    private fun parseBitwiseAndExpression(): Expression {
        var left = parseEqualityExpression()
        
        while (currentKind() == TokenKind.AMPERSAND) {
            advance()
            val right = parseEqualityExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, BinaryOperator.BITWISE_AND, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses an equality expression.
     */
    private fun parseEqualityExpression(): Expression {
        var left = parseRelationalExpression()
        
        while (currentKind() == TokenKind.EQ || currentKind() == TokenKind.NEQ) {
            val op = if (currentKind() == TokenKind.EQ) BinaryOperator.EQ else BinaryOperator.NEQ
            advance()
            val right = parseRelationalExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, op, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a relational expression.
     */
    private fun parseRelationalExpression(): Expression {
        var left = parseShiftExpression()
        
        while (currentKind() == TokenKind.LT || currentKind() == TokenKind.LTE ||
               currentKind() == TokenKind.GT || currentKind() == TokenKind.GTE) {
            val op = when (currentKind()) {
                TokenKind.LT -> BinaryOperator.LT
                TokenKind.LTE -> BinaryOperator.LTE
                TokenKind.GT -> BinaryOperator.GT
                TokenKind.GTE -> BinaryOperator.GTE
                else -> BinaryOperator.LT
            }
            advance()
            val right = parseShiftExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, op, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a shift expression.
     */
    private fun parseShiftExpression(): Expression {
        var left = parseAdditiveExpression()
        
        while (currentKind() == TokenKind.LEFT_SHIFT || currentKind() == TokenKind.RIGHT_SHIFT) {
            val op = if (currentKind() == TokenKind.LEFT_SHIFT) BinaryOperator.LEFT_SHIFT else BinaryOperator.RIGHT_SHIFT
            advance()
            val right = parseAdditiveExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, op, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses an additive expression.
     */
    private fun parseAdditiveExpression(): Expression {
        var left = parseMultiplicativeExpression()
        
        while (currentKind() == TokenKind.PLUS || currentKind() == TokenKind.MINUS) {
            val op = if (currentKind() == TokenKind.PLUS) BinaryOperator.ADD else BinaryOperator.SUBTRACT
            advance()
            val right = parseMultiplicativeExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, op, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a multiplicative expression.
     */
    private fun parseMultiplicativeExpression(): Expression {
        var left = parseUnaryExpression()
        
        while (currentKind() == TokenKind.STAR || currentKind() == TokenKind.SLASH || currentKind() == TokenKind.PERCENT) {
            val op = when (currentKind()) {
                TokenKind.STAR -> BinaryOperator.MULTIPLY
                TokenKind.SLASH -> BinaryOperator.DIVIDE
                TokenKind.PERCENT -> BinaryOperator.MODULO
                else -> BinaryOperator.MULTIPLY
            }
            advance()
            val right = parseUnaryExpression()
            
            val start = left.span.start
            val end = right.span.end
            left = BinaryExpr(left, op, right, Span(start, end))
        }
        
        return left
    }

    /**
     * Parses a unary expression.
     */
    private fun parseUnaryExpression(): Expression {
        when (currentKind()) {
            TokenKind.MINUS -> {
                advance()
                val operand = parseUnaryExpression()
                return UnaryExpr(UnaryOperator.MINUS, operand, operand.span)
            }
            TokenKind.PLUS -> {
                advance()
                val operand = parseUnaryExpression()
                return UnaryExpr(UnaryOperator.PLUS, operand, operand.span)
            }
            TokenKind.NOT -> {
                advance()
                val operand = parseUnaryExpression()
                return UnaryExpr(UnaryOperator.NOT, operand, operand.span)
            }
            TokenKind.TILDE -> {
                advance()
                val operand = parseUnaryExpression()
                return UnaryExpr(UnaryOperator.BITWISE_NOT, operand, operand.span)
            }
            else -> return parsePostfixExpression()
        }
    }

    /**
     * Parses a postfix expression.
     */
    private fun parsePostfixExpression(): Expression {
        var left = parsePrimaryExpression()
        
        while (true) {
            when (currentKind()) {
                TokenKind.DOT -> {
                    advance()
                    if (currentKind() == TokenKind.IDENTIFIER) {
                        val memberToken = advance()
                        val member = memberToken.literal ?: ""
                        val start = left.span.start
                        val end = memberToken.span.end
                        left = MemberAccessExpr(left, member, Span(start, end))
                    } else {
                        error("Expected member name after '.'")
                        break
                    }
                }
                TokenKind.LEFT_BRACKET -> {
                    advance()
                    val index = parseExpression()
                    expectOrError(TokenKind.RIGHT_BRACKET, "Expected ']'")
                    val start = left.span.start
                    val end = previousToken?.span?.end ?: currentToken.span.end
                    left = IndexExpr(left, index, Span(start, end))
                }
                TokenKind.INCREMENT -> {
                    advance()
                    // Postfix increment
                    val start = left.span.start
                    val end = currentToken.span.end
                    // For now, return the left expression
                    // TODO: proper increment expression
                    left = left
                }
                TokenKind.DECREMENT -> {
                    advance()
                    // Postfix decrement
                    val start = left.span.start
                    val end = currentToken.span.end
                    // For now, return the left expression
                    // TODO: proper decrement expression
                    left = left
                }
                else -> break
            }
        }
        
        return left
    }

    /**
     * Parses a primary expression.
     */
    private fun parsePrimaryExpression(): Expression {
        val start = currentToken.span
        
        return when (currentKind()) {
            TokenKind.INT_LITERAL -> {
                val token = advance()
                IntLiteral(token.literal?.toLongOrNull() ?: 0, null, token.span)
            }
            TokenKind.UINT_LITERAL -> {
                val token = advance()
                IntLiteral(token.literal?.toLongOrNull() ?: 0, "u", token.span)
            }
            TokenKind.FLOAT_LITERAL -> {
                val token = advance()
                FloatLiteral(token.literal?.toDoubleOrNull() ?: 0.0, null, token.span)
            }
            TokenKind.TRUE -> {
                advance()
                BoolLiteral(true, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.FALSE -> {
                advance()
                BoolLiteral(false, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.STRING_LITERAL -> {
                val token = advance()
                // Remove quotes from literal
                val value = token.literal?.removeSurrounding("\"") ?: ""
                StringLiteral(value, token.span)
            }
            TokenKind.IDENTIFIER -> {
                val token = advance()
                IdentExpr(token.literal ?: "", token.span)
            }
            TokenKind.LEFT_PAREN -> {
                advance()
                val expr = parseExpression()
                expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
                val end = previousToken?.span?.end ?: currentToken.span.end
                expr // Return the expression inside parentheses
            }
            else -> {
                error("Expected a primary expression")
                IdentExpr("", start)
            }
        }
    }

    // ========================================================================
    // Statement parsing
    // ========================================================================

    /**
     * Parses a block statement.
     */
    private fun parseBlockStatement(): BlockStatement {
        val start = currentToken.span
        
        expectOrError(TokenKind.LEFT_BRACE, "Expected '{'")
        
        val statements = mutableListOf<Statement>()
        while (currentKind() != TokenKind.RIGHT_BRACE && !isAtEnd()) {
            statements.add(parseStatement())
        }
        
        expectOrError(TokenKind.RIGHT_BRACE, "Expected '}'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return BlockStatement(statements, Span(start.start, end))
    }

    /**
     * Parses a statement.
     */
    private fun parseStatement(): Statement {
        val start = currentToken.span
        
        return when (currentKind()) {
            TokenKind.LEFT_BRACE -> parseBlockStatement()
            TokenKind.IF -> parseIfStatement()
            TokenKind.SWITCH -> parseSwitchStatement()
            TokenKind.LOOP -> parseLoopStatement()
            TokenKind.WHILE -> parseWhileStatement()
            TokenKind.FOR -> parseForStatement()
            TokenKind.BREAK -> {
                advance()
                expectOrError(TokenKind.SEMICOLON, "Expected ';'")
                BreakStatement(Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.CONTINUE -> {
                advance()
                expectOrError(TokenKind.SEMICOLON, "Expected ';'")
                ContinueStatement(Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.RETURN -> {
                advance()
                val value = if (currentKind() != TokenKind.SEMICOLON) {
                    parseExpression()
                } else {
                    null
                }
                expectOrError(TokenKind.SEMICOLON, "Expected ';'")
                ReturnStatement(value, Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.DISCARD -> {
                advance()
                expectOrError(TokenKind.SEMICOLON, "Expected ';'")
                DiscardStatement(Span(start.start, previousToken?.span?.end ?: start.end))
            }
            TokenKind.LET, TokenKind.CONST, TokenKind.VAR -> parseVariableDeclStatement()
            else -> {
                // Expression statement
                val expr = parseExpression()
                expectOrError(TokenKind.SEMICOLON, "Expected ';'")
                ExpressionStatement(expr, Span(start.start, previousToken?.span?.end ?: start.end))
            }
        }
    }

    /**
     * Parses an if statement.
     */
    private fun parseIfStatement(): IfStatement {
        val start = currentToken.span
        
        expectOrError(TokenKind.IF, "Expected 'if'")
        expectOrError(TokenKind.LEFT_PAREN, "Expected '('")
        val condition = parseExpression()
        expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
        
        val thenBranch = parseStatement()
        
        val elseBranch = if (currentKind() == TokenKind.ELSE) {
            advance()
            parseStatement()
        } else {
            null
        }
        
        val end = elseBranch?.span?.end ?: thenBranch.span.end
        return IfStatement(condition, thenBranch, elseBranch, Span(start.start, end))
    }

    /**
     * Parses a switch statement.
     */
    private fun parseSwitchStatement(): SwitchStatement {
        val start = currentToken.span
        
        expectOrError(TokenKind.SWITCH, "Expected 'switch'")
        expectOrError(TokenKind.LEFT_PAREN, "Expected '('")
        val expression = parseExpression()
        expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
        
        val body = parseSwitchBody()
        
        val end = body.span.end
        return SwitchStatement(expression, body, Span(start.start, end))
    }

    /**
     * Parses a switch body.
     */
    private fun parseSwitchBody(): SwitchBody {
        val start = currentToken.span
        
        expectOrError(TokenKind.LEFT_BRACE, "Expected '{'")
        
        val cases = mutableListOf<SwitchCase>()
        while (currentKind() != TokenKind.RIGHT_BRACE && !isAtEnd()) {
            cases.add(parseSwitchCase())
        }
        
        expectOrError(TokenKind.RIGHT_BRACE, "Expected '}'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return SwitchBody(cases, Span(start.start, end))
    }

    /**
     * Parses a switch case.
     */
    private fun parseSwitchCase(): SwitchCase {
        if (currentKind() == TokenKind.DEFAULT) {
            advance()
            expectOrError(TokenKind.COLON, "Expected ':'")
            val body = parseBlockStatement()
            return DefaultCase(body, body.span)
        } else {
            val value = parseExpression()
            expectOrError(TokenKind.COLON, "Expected ':'")
            val body = parseBlockStatement()
            return Case(value, body, body.span)
        }
    }

    /**
     * Parses a loop statement.
     */
    private fun parseLoopStatement(): LoopStatement {
        val start = currentToken.span
        
        expectOrError(TokenKind.LOOP, "Expected 'loop'")
        
        val body = parseBlockStatement()
        
        val continuing = if (currentKind() == TokenKind.CONTINUING) {
            advance()
            parseBlockStatement()
        } else {
            null
        }
        
        val end = continuing?.span?.end ?: body.span.end
        return LoopStatement(body, continuing, Span(start.start, end))
    }

    /**
     * Parses a while statement.
     */
    private fun parseWhileStatement(): WhileStatement {
        val start = currentToken.span
        
        expectOrError(TokenKind.WHILE, "Expected 'while'")
        expectOrError(TokenKind.LEFT_PAREN, "Expected '('")
        val condition = parseExpression()
        expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
        
        val body = parseBlockStatement()
        
        val continuing = if (currentKind() == TokenKind.CONTINUING) {
            advance()
            parseBlockStatement()
        } else {
            null
        }
        
        val end = continuing?.span?.end ?: body.span.end
        return WhileStatement(condition, body, continuing, Span(start.start, end))
    }

    /**
     * Parses a for statement.
     */
    private fun parseForStatement(): ForStatement {
        val start = currentToken.span
        
        expectOrError(TokenKind.FOR, "Expected 'for'")
        expectOrError(TokenKind.LEFT_PAREN, "Expected '('")
        
        // Parse init
        val init = if (currentKind() != TokenKind.SEMICOLON) {
            val stmt = parseVariableDeclStatement()
            expectOrError(TokenKind.SEMICOLON, "Expected ';'")
            stmt
        } else {
            advance()
            null
        }
        
        // Parse condition
        val condition = if (currentKind() != TokenKind.SEMICOLON) {
            parseExpression()
        } else {
            null
        }
        expectOrError(TokenKind.SEMICOLON, "Expected ';'")
        
        // Parse update
        val update = if (currentKind() != TokenKind.RIGHT_PAREN) {
            parseExpression()
        } else {
            null
        }
        expectOrError(TokenKind.RIGHT_PAREN, "Expected ')'")
        
        val body = parseBlockStatement()
        
        val end = body.span.end
        return ForStatement(init, condition, update, body, Span(start.start, end))
    }

    /**
     * Parses a local variable declaration statement.
     */
    private fun parseVariableDeclStatement(): VariableDeclStatement {
        val start = currentToken.span
        
        // Parse kind (let, const, var)
        val kind = when (currentKind()) {
            TokenKind.LET -> VariableDeclKind.LET
            TokenKind.CONST -> VariableDeclKind.CONST
            TokenKind.VAR -> VariableDeclKind.VAR
            else -> {
                error("Expected 'let', 'const', or 'var'")
                VariableDeclKind.LET
            }
        }
        advance()
        
        // Parse name
        val name = if (currentKind() == TokenKind.IDENTIFIER) {
            val nameToken = advance()
            nameToken.literal ?: ""
        } else {
            error("Expected variable name")
            ""
        }
        
        // Parse type annotation (if any)
        val type = if (currentKind() == TokenKind.COLON) {
            advance()
            parseTypeDecl()
        } else {
            null
        }
        
        // Parse initializer
        val initializer = if (currentKind() == TokenKind.ASSIGN) {
            advance()
            parseExpression()
        } else {
            null
        }
        
        expectOrError(TokenKind.SEMICOLON, "Expected ';'")
        
        val end = previousToken?.span?.end ?: currentToken.span.end
        return VariableDeclStatement(kind, name, type, initializer, Span(start.start, end))
    }
}

/**
 * A parse error.
 */
data class ParseError(
    /** The error message. */
    val message: String,
    /** The span where the error occurred. */
    val span: Span,
)

/**
 * Parses a WGSL source string and returns the AST.
 */
fun parseWgsl(source: String): TranslationUnit {
    val lexer = Lexer(source)
    val parser = Parser(lexer)
    return parser.parse()
}
