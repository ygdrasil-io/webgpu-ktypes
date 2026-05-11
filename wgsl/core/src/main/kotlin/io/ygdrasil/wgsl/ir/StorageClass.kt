package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * The storage class of a variable.
 */
@Serializable
enum class StorageClass {
    /**
     * Function-local storage.
     */
    Function,
    
    /**
     * Private storage (only accessible within the shader).
     */
    Private,
    
    /**
     * Workgroup storage (shared within a workgroup).
     */
    Workgroup,
    
    /**
     * Uniform storage (read-only, constant across all invocations).
     */
    Uniform,
    
    /**
     * Storage storage (mutable storage buffers).
     */
    Storage,
    
    /**
     * Push-constant storage.
     */
    PushConstant,
}

/**
 * Returns true if this storage class allows mutable access.
 */
val StorageClass.isMutable: Boolean
    get() = when (this) {
        StorageClass.Function, StorageClass.Private, StorageClass.Workgroup, StorageClass.Storage -> true
        else -> false
    }
