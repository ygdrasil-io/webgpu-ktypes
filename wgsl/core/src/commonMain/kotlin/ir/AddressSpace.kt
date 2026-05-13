package io.ygdrasil.wgsl.ir

import kotlinx.serialization.Serializable

/**
 * Address spaces for pointer types.
 */
@Serializable
enum class AddressSpace {
    /**
     * Function address space.
     */
    Function,
    
    /**
     * Private address space.
     */
    Private,
    
    /**
     * Workgroup address space.
     */
    Workgroup,
    
    /**
     * Uniform address space.
     */
    Uniform,
    
    /**
     * Storage address space.
     */
    Storage,
}
