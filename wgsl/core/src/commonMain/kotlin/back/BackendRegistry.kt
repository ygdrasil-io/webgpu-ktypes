package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.valid.ModuleInfo

/**
 * Registre de tous les backends disponibles.
 */
class BackendRegistry {

    private val backends: MutableMap<String, BackendFactory> = mutableMapOf()

    interface BackendFactory {
        fun create(): BackendWriter<*>
        fun createWithOptions(options: BackendOptions): BackendWriter<*>
    }

    /**
     * Enregistre un nouveau backend.
     */
    fun register(name: String, factory: BackendFactory) {
        backends[name.lowercase()] = factory
    }

    /**
     * Obtient un backend par nom.
     */
    fun get(name: String): BackendWriter<*>? {
        return backends[name.lowercase()]?.create()
    }

    /**
     * Liste tous les noms de backends disponibles.
     */
    fun listBackendNames(): List<String> {
        return backends.keys.toList()
    }

    companion object {
        val DEFAULT = BackendRegistry()
    }
}
