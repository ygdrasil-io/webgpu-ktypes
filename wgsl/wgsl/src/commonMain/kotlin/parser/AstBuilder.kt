package io.ygdrasil.wgsl.parser

import io.ygdrasil.wgsl.ast.*
import io.ygdrasil.wgsl.ir.Span

/**
 * Centralized builder for creating AST nodes.
 * 
 * This class provides helper methods for creating all types of AST nodes,
 * managing arenas, and tracking statistics.
 */
class AstBuilder {
    
    /** Statistics tracking */
    private var _declarationCount: Int = 0
    private var _expressionCount: Int = 0
    private var _typeCount: Int = 0
    private var _statementCount: Int = 0
    
    /** Reset all statistics */
    fun reset() {
        _declarationCount = 0
        _expressionCount = 0
        _typeCount = 0
        _statementCount = 0
    }
    
    // ========== Statistic Getters ==========
    
    val declarationCount: Int get() = _declarationCount
    val expressionCount: Int get() = _expressionCount
    val typeCount: Int get() = _typeCount
    val statementCount: Int get() = _statementCount
    
    val totalCount: Int get() = _declarationCount + _expressionCount + _typeCount + _statementCount
    
    // ========== TranslationUnit ==========
    
    fun translationUnit(
        declarations: List<GlobalDecl>,
        span: Span = Span.UNDEFINED
    ): TranslationUnit {
        return TranslationUnit(declarations, span)
    }
    
    // ========== Global Declarations ==========
    
    fun functionDecl(
        attributes: List<Attribute> = emptyList(),
        name: String,
        templateParams: List<TemplateParam> = emptyList(),
        parameters: List<Param> = emptyList(),
        returnType: TypeDecl? = null,
        body: BlockStatement? = null,
        span: Span = Span.UNDEFINED
    ): FunctionDecl {
        _declarationCount++
        return FunctionDecl(attributes, name, templateParams, parameters, returnType, body, span)
    }
    
    fun structDecl(
        attributes: List<Attribute> = emptyList(),
        name: String,
        templateParams: List<TemplateParam> = emptyList(),
        members: List<StructMember> = emptyList(),
        span: Span = Span.UNDEFINED
    ): StructDecl {
        _declarationCount++
        return StructDecl(attributes, name, templateParams, members, span)
    }
    
    fun variableDecl(
        kind: VariableDeclKind,
        attributes: List<Attribute> = emptyList(),
        name: String,
        type: TypeDecl? = null,
        initializer: Expression? = null,
        span: Span = Span.UNDEFINED
    ): VariableDecl {
        _declarationCount++
        return VariableDecl(kind, attributes, name, type, initializer, span)
    }
    
    fun typeAliasDecl(
        attributes: List<Attribute> = emptyList(),
        name: String,
        templateParams: List<TemplateParam> = emptyList(),
        type: TypeDecl,
        span: Span = Span.UNDEFINED
    ): TypeAliasDecl {
        _declarationCount++
        return TypeAliasDecl(attributes, name, templateParams, type, span)
    }
    
    fun overrideDecl(
        attributes: List<Attribute> = emptyList(),
        entryPoint: EntryPointAttribute,
        function: FunctionDecl,
        span: Span = Span.UNDEFINED
    ): OverrideDecl {
        _declarationCount++
        return OverrideDecl(attributes, entryPoint, function, span)
    }
    
    fun constAssertDecl(
        expression: Expression,
        span: Span = Span.UNDEFINED
    ): ConstAssertDecl {
        _declarationCount++
        return ConstAssertDecl(expression, span)
    }
    
    // ========== Types ==========
    
    fun scalarType(
        kind: ScalarKind,
        span: Span = Span.UNDEFINED
    ): ScalarType {
        _typeCount++
        return ScalarType(kind, span)
    }
    
    fun vectorType(
        size: Int,
        elementType: TypeDecl,
        span: Span = Span.UNDEFINED
    ): VectorType {
        _typeCount++
        return VectorType(size, elementType, span)
    }
    
    fun matrixType(
        columns: Int,
        rows: Int,
        elementType: TypeDecl,
        span: Span = Span.UNDEFINED
    ): MatrixType {
        _typeCount++
        return MatrixType(columns, rows, elementType, span)
    }
    
    fun arrayType(
        elementType: TypeDecl,
        length: Expression? = null,
        stride: Int? = null,
        span: Span = Span.UNDEFINED
    ): ArrayType {
        _typeCount++
        return ArrayType(elementType, length, stride, span)
    }
    
    fun structType(
        name: String,
        span: Span = Span.UNDEFINED
    ): StructType {
        _typeCount++
        return StructType(name, span)
    }
    
    fun namedType(
        name: String,
        span: Span = Span.UNDEFINED
    ): NamedType {
        _typeCount++
        return NamedType(name, span)
    }
    
    fun pointerType(
        storageClass: StorageClass,
        elementType: TypeDecl,
        span: Span = Span.UNDEFINED
    ): PointerType {
        _typeCount++
        return PointerType(storageClass, elementType, span)
    }
    
    fun referenceType(
        storageClass: StorageClass,
        elementType: TypeDecl,
        span: Span = Span.UNDEFINED
    ): ReferenceType {
        _typeCount++
        return ReferenceType(storageClass, elementType, span)
    }
    
    fun templateType(
        name: String,
        args: List<TypeDecl> = emptyList(),
        span: Span = Span.UNDEFINED
    ): TemplateType {
        _typeCount++
        return TemplateType(name, args, span)
    }
    
    // ========== Expressions ==========
    
    fun intLiteral(
        value: Long,
        suffix: String? = null,
        span: Span = Span.UNDEFINED
    ): IntLiteral {
        _expressionCount++
        return IntLiteral(value, suffix, span)
    }
    
    fun floatLiteral(
        value: Double,
        suffix: String? = null,
        span: Span = Span.UNDEFINED
    ): FloatLiteral {
        _expressionCount++
        return FloatLiteral(value, suffix, span)
    }
    
    fun boolLiteral(
        value: Boolean,
        span: Span = Span.UNDEFINED
    ): BoolLiteral {
        _expressionCount++
        return BoolLiteral(value, span)
    }
    
    fun stringLiteral(
        value: String,
        span: Span = Span.UNDEFINED
    ): StringLiteral {
        _expressionCount++
        return StringLiteral(value, span)
    }
    
    fun identExpr(
        name: String,
        span: Span = Span.UNDEFINED
    ): IdentExpr {
        _expressionCount++
        return IdentExpr(name, span)
    }
    
    fun callExpr(
        callee: Expression,
        args: List<Expression> = emptyList(),
        templateArgs: List<TypeDecl>? = null,
        span: Span = Span.UNDEFINED
    ): CallExpr {
        _expressionCount++
        return CallExpr(callee, args, templateArgs, span)
    }
    
    fun memberAccessExpr(
        objectExpr: Expression,
        member: String,
        span: Span = Span.UNDEFINED
    ): MemberAccessExpr {
        _expressionCount++
        return MemberAccessExpr(objectExpr, member, span)
    }
    
    fun indexExpr(
        objectExpr: Expression,
        index: Expression,
        span: Span = Span.UNDEFINED
    ): IndexExpr {
        _expressionCount++
        return IndexExpr(objectExpr, index, span)
    }
    
    fun unaryExpr(
        op: UnaryOperator,
        operand: Expression,
        span: Span = Span.UNDEFINED
    ): UnaryExpr {
        _expressionCount++
        return UnaryExpr(op, operand, span)
    }
    
    fun binaryExpr(
        left: Expression,
        op: BinaryOperator,
        right: Expression,
        span: Span = Span.UNDEFINED
    ): BinaryExpr {
        _expressionCount++
        return BinaryExpr(left, op, right, span)
    }
    
    fun ternaryExpr(
        condition: Expression,
        trueExpr: Expression,
        falseExpr: Expression,
        span: Span = Span.UNDEFINED
    ): TernaryExpr {
        _expressionCount++
        return TernaryExpr(condition, trueExpr, falseExpr, span)
    }
    
    fun typeCastExpr(
        expr: Expression,
        type: TypeDecl,
        span: Span = Span.UNDEFINED
    ): TypeCastExpr {
        _expressionCount++
        return TypeCastExpr(expr, type, span)
    }
    
    fun swizzleExpr(
        objectExpr: Expression,
        components: List<String>,
        span: Span = Span.UNDEFINED
    ): SwizzleExpr {
        _expressionCount++
        return SwizzleExpr(objectExpr, components, span)
    }
    
    // ========== Statements ==========
    
    fun blockStatement(
        statements: List<Statement> = emptyList(),
        span: Span = Span.UNDEFINED
    ): BlockStatement {
        _statementCount++
        return BlockStatement(statements, span)
    }
    
    fun ifStatement(
        condition: Expression,
        thenBranch: Statement,
        elseBranch: Statement? = null,
        span: Span = Span.UNDEFINED
    ): IfStatement {
        _statementCount++
        return IfStatement(condition, thenBranch, elseBranch, span)
    }
    
    fun switchStatement(
        expression: Expression,
        body: SwitchBody,
        span: Span = Span.UNDEFINED
    ): SwitchStatement {
        _statementCount++
        return SwitchStatement(expression, body, span)
    }
    
    fun switchBody(
        cases: List<SwitchCase> = emptyList(),
        span: Span = Span.UNDEFINED
    ): SwitchBody {
        return SwitchBody(cases, span)
    }
    
    fun case(
        value: Expression,
        body: BlockStatement,
        span: Span = Span.UNDEFINED
    ): Case {
        return Case(value, body, span)
    }
    
    fun defaultCase(
        body: BlockStatement,
        span: Span = Span.UNDEFINED
    ): DefaultCase {
        return DefaultCase(body, span)
    }
    
    fun loopStatement(
        body: BlockStatement,
        continuing: BlockStatement? = null,
        span: Span = Span.UNDEFINED
    ): LoopStatement {
        _statementCount++
        return LoopStatement(body, continuing, span)
    }
    
    fun whileStatement(
        condition: Expression,
        body: BlockStatement,
        continuing: BlockStatement? = null,
        span: Span = Span.UNDEFINED
    ): WhileStatement {
        _statementCount++
        return WhileStatement(condition, body, continuing, span)
    }
    
    fun forStatement(
        init: Statement? = null,
        condition: Expression? = null,
        update: Expression? = null,
        body: BlockStatement,
        span: Span = Span.UNDEFINED
    ): ForStatement {
        _statementCount++
        return ForStatement(init, condition, update, body, span)
    }
    
    fun breakStatement(
        span: Span = Span.UNDEFINED
    ): BreakStatement {
        _statementCount++
        return BreakStatement(span)
    }
    
    fun continueStatement(
        span: Span = Span.UNDEFINED
    ): ContinueStatement {
        _statementCount++
        return ContinueStatement(span)
    }
    
    fun returnStatement(
        value: Expression? = null,
        span: Span = Span.UNDEFINED
    ): ReturnStatement {
        _statementCount++
        return ReturnStatement(value, span)
    }
    
    fun discardStatement(
        span: Span = Span.UNDEFINED
    ): DiscardStatement {
        _statementCount++
        return DiscardStatement(span)
    }
    
    fun variableDeclStatement(
        kind: VariableDeclKind,
        name: String,
        type: TypeDecl? = null,
        initializer: Expression? = null,
        span: Span = Span.UNDEFINED
    ): VariableDeclStatement {
        _statementCount++
        return VariableDeclStatement(kind, name, type, initializer, span)
    }
    
    fun assignmentStatement(
        lhs: Expression,
        rhs: Expression,
        op: BinaryOperator? = null,
        span: Span = Span.UNDEFINED
    ): AssignmentStatement {
        _statementCount++
        return AssignmentStatement(lhs, rhs, op, span)
    }
    
    fun incDecStatement(
        expr: Expression,
        isIncrement: Boolean,
        span: Span = Span.UNDEFINED
    ): IncDecStatement {
        _statementCount++
        return IncDecStatement(expr, isIncrement, span)
    }
    
    fun expressionStatement(
        expr: Expression,
        span: Span = Span.UNDEFINED
    ): ExpressionStatement {
        _statementCount++
        return ExpressionStatement(expr, span)
    }
    
    // ========== Miscellaneous ==========
    
    fun attribute(
        name: String,
        args: List<Expression> = emptyList(),
        span: Span = Span.UNDEFINED
    ): Attribute {
        return Attribute(name, args, span)
    }
    
    fun param(
        attributes: List<Attribute> = emptyList(),
        name: String,
        type: TypeDecl,
        defaultValue: Expression? = null,
        span: Span = Span.UNDEFINED
    ): Param {
        return Param(attributes, name, type, defaultValue, span)
    }
    
    fun templateParam(
        name: String,
        constraint: TypeDecl? = null,
        span: Span = Span.UNDEFINED
    ): TemplateParam {
        return TemplateParam(name, constraint, span)
    }
    
    fun structMember(
        attributes: List<Attribute> = emptyList(),
        name: String,
        type: TypeDecl,
        defaultValue: Expression? = null,
        span: Span = Span.UNDEFINED
    ): StructMember {
        return StructMember(attributes, name, type, defaultValue, span)
    }
    
    fun entryPointCompute(): EntryPointAttribute.Compute {
        return EntryPointAttribute.Compute
    }
    
    fun entryPointFragment(
        inputs: List<FragmentInput> = emptyList()
    ): EntryPointAttribute.Fragment {
        return EntryPointAttribute.Fragment(inputs)
    }
    
    fun entryPointVertex(
        outputs: List<VertexOutput> = emptyList()
    ): EntryPointAttribute.Vertex {
        return EntryPointAttribute.Vertex(outputs)
    }
    
    fun fragmentInput(
        location: Int? = null,
        builtin: BuiltinValue? = null,
        type: TypeDecl
    ): FragmentInput {
        return FragmentInput(location, builtin, type)
    }
    
    fun vertexOutput(
        location: Int? = null,
        builtin: BuiltinValue? = null,
        type: TypeDecl
    ): VertexOutput {
        return VertexOutput(location, builtin, type)
    }
}
