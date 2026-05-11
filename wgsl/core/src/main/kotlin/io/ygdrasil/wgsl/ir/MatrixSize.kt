package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * The dimensions of a matrix type.
 * The first component is the number of columns, the second is the number of rows.
 */
@Serializable
enum class MatrixSize {
    /**
     * 2x2 matrix.
     */
    Mat2x2,
    
    /**
     * 2x3 matrix.
     */
    Mat2x3,
    
    /**
     * 2x4 matrix.
     */
    Mat2x4,
    
    /**
     * 3x2 matrix.
     */
    Mat3x2,
    
    /**
     * 3x3 matrix.
     */
    Mat3x3,
    
    /**
     * 3x4 matrix.
     */
    Mat3x4,
    
    /**
     * 4x2 matrix.
     */
    Mat4x2,
    
    /**
     * 4x3 matrix.
     */
    Mat4x3,
    
    /**
     * 4x4 matrix.
     */
    Mat4x4,
}

/**
 * Returns the number of columns in this matrix.
 */
val MatrixSize.columns: Int
    get() = when (this) {
        MatrixSize.Mat2x2, MatrixSize.Mat2x3, MatrixSize.Mat2x4 -> 2
        MatrixSize.Mat3x2, MatrixSize.Mat3x3, MatrixSize.Mat3x4 -> 3
        MatrixSize.Mat4x2, MatrixSize.Mat4x3, MatrixSize.Mat4x4 -> 4
    }

/**
 * Returns the number of rows in this matrix.
 */
val MatrixSize.rows: Int
    get() = when (this) {
        MatrixSize.Mat2x2, MatrixSize.Mat3x2, MatrixSize.Mat4x2 -> 2
        MatrixSize.Mat2x3, MatrixSize.Mat3x3, MatrixSize.Mat4x3 -> 3
        MatrixSize.Mat2x4, MatrixSize.Mat3x4, MatrixSize.Mat4x4 -> 4
    }
