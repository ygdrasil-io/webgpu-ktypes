package io.ygdrasil.wgsl.back

/**
 * Spécifie les valeurs des constantes de pipeline dans le module shader.
 */
typealias PipelineConstants = Map<String, Double>

enum class BackendType {
    MSL, HLSL, GLSL, WGSL
}
