package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.valid.ModuleInfo

/**
 * Interface commune pour tous les writers de backend.
 */
interface BackendWriter<T : BackendOptions> {

    /**
     * Génère le code pour un module.
     *
     * @param module Le module IR à transformer
     * @param moduleInfo Informations calculées sur le module
     * @return Le code source généré
     * @throws BackendException Si une erreur survient pendant la génération
     */
    fun write(module: Module, moduleInfo: ModuleInfo): String

    /**
     * Crée un nouveau writer avec les options spécifiées.
     */
    fun withOptions(options: T): BackendWriter<T>

    /**
     * Valide que le module peut être transformé par ce backend.
     *
     * @param module Le module à valider
     * @param moduleInfo Informations calculées
     * @return true si le module est valide pour ce backend
     */
    fun canHandle(module: Module, moduleInfo: ModuleInfo): Boolean
}
