package io.ygdrasil.wgsl.valid

import kotlinx.serialization.Serializable

/**
 * Validation flags for the shader module.
 */
@Serializable
data class ValidationFlags(
    val expressions: Boolean = true,
    val blocks: Boolean = true,
    val controlFlowUniformity: Boolean = true,
    val structLayouts: Boolean = true,
    val constants: Boolean = true,
    val bindings: Boolean = true
) {
    companion object {
        val ALL = ValidationFlags()
        val NONE = ValidationFlags(false, false, false, false, false, false)
    }
}

/**
 * Supported capabilities for the backend.
 */
@Serializable
data class Capabilities(
    val float64: Boolean = false,
    val shaderInt64: Boolean = false,
    val rayQuery: Boolean = false
) {
    companion object {
        val ALL = Capabilities(true, true, true)
        val NONE = Capabilities()
    }
}

/**
 * Supported shader stages.
 */
@Serializable
data class ShaderStages(
    val vertex: Boolean = true,
    val fragment: Boolean = true,
    val compute: Boolean = true
) {
    companion object {
        val ALL = ShaderStages()
        val NONE = ShaderStages(false, false, false)
    }
}

/**
 * Information about a validated module.
 */
class ModuleInfo {
    companion object {
        fun empty() = ModuleInfo()
    }
}
