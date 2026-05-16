package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.Binding

/**
 * Mapping entre les Binding et les cibles de binding spécifiques au backend.
 */
class BindingMap {

    private val map: MutableMap<Binding, BindTarget> = mutableMapOf()

    /**
     * Cible de binding pour un backend spécifique.
     */
    data class BindTarget(
        val buffer: Int? = null,
        val texture: Int? = null,
        val sampler: Int? = null,
        val mutable: Boolean = false
    )

    /**
     * Ajoute un mapping.
     */
    fun insert(binding: Binding, target: BindTarget) {
        map[binding] = target
    }

    /**
     * Récupère la cible pour un binding.
     */
    operator fun get(binding: Binding): BindTarget? {
        return map[binding]
    }

    /**
     * Vérifie si un binding a un mapping.
     */
    fun contains(binding: Binding): Boolean {
        return map.containsKey(binding)
    }

    /**
     * Efface tous les mappings.
     */
    fun clear() {
        map.clear()
    }

    companion object {
        fun defaultMsl(): BindingMap = BindingMap()
        fun defaultHlsl(): BindingMap = BindingMap()
        fun defaultGlsl(): BindingMap = BindingMap()
    }
}
