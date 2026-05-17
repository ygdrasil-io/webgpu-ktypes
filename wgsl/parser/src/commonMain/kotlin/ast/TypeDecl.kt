package io.ygdrasil.wgsl.ast

import io.ygdrasil.wgsl.ir.Span

/**
 * A type declaration in WGSL.
 * 
 * This represents a type in the WGSL type system, which can be:
 * - Scalar types (bool, i32, u32, f32, etc.)
 * - Vector types (vec2<f32>, vec3<i32>, etc.)
 * - Matrix types (mat2x2<f32>, mat3x4<f32>, etc.)
 * - Array types (array<i32, 4>, array<f32>, etc.)
 * - Struct types
 * - Pointer types
 * - Reference types
 * - Named types (type aliases)
 * - Template types
 */
sealed class TypeDecl {
    /** The span of this type declaration. */
    abstract val span: Span
}

/**
 * A scalar type.
 */
data class ScalarType(
    /** The scalar kind. */
    val kind: ScalarKind,
    override val span: Span,
) : TypeDecl()

/**
 * Scalar kinds in WGSL.
 */
enum class ScalarKind {
    BOOL,
    I8, U8,
    I16, U16,
    I32, U32,
    I64, U64,
    F16, F32, F64,
}

/**
 * A vector type.
 */
data class VectorType(
    /** The size of the vector (2, 3, or 4). */
    val size: Int,
    /** The type of elements in the vector. */
    val elementType: TypeDecl,
    override val span: Span,
) : TypeDecl()

/**
 * A matrix type.
 */
data class MatrixType(
    /** The number of columns. */
    val columns: Int,
    /** The number of rows. */
    val rows: Int,
    /** The type of elements in the matrix. */
    val elementType: TypeDecl,
    override val span: Span,
) : TypeDecl()

/**
 * An array type.
 */
data class ArrayType(
    /** The element type of the array. */
    val elementType: TypeDecl,
    /** The length of the array (null for runtime-sized arrays). */
    val length: Expression?,
    /** The stride (if specified). */
    val stride: Int?,
    override val span: Span,
) : TypeDecl()

/**
 * A struct type reference.
 */
data class StructType(
    /** The name of the struct. */
    val name: String,
    override val span: Span,
) : TypeDecl()

/**
 * A named type (type alias reference).
 */
data class NamedType(
    /** The name of the type. */
    val name: String,
    override val span: Span,
) : TypeDecl()

/**
 * A pointer type.
 */
data class PointerType(
    /** The storage class. */
    val storageClass: StorageClass,
    /** The type being pointed to. */
    val elementType: TypeDecl,
    /** The access mode (optional). */
    val accessMode: String? = null,
    override val span: Span,
) : TypeDecl()

/**
 * Storage classes for pointers.
 */
enum class StorageClass {
    FUNCTION,
    PRIVATE,
    WORKGROUP,
    UNIFORM,
    STORAGE,
    HANDLE,
}

/**
 * A reference type.
 */
data class ReferenceType(
    /** The storage class. */
    val storageClass: StorageClass,
    /** The type being referenced. */
    val elementType: TypeDecl,
    override val span: Span,
) : TypeDecl()

/**
 * A template type.
 */
data class TemplateType(
    /** The name of the template parameter. */
    val name: String,
    /** The template arguments (if any). */
    val args: List<TypeDecl>,
    override val span: Span,
) : TypeDecl()
