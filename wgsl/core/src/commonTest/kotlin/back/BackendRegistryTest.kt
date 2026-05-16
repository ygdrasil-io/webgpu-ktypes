package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.valid.ModuleInfo
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BackendRegistryTest {

    @Test
    fun testListBackendNames() {
        val registry = BackendRegistry()
        registry.register("msl", object : BackendRegistry.BackendFactory {
            override fun create(): BackendWriter<*> = object : BackendWriter<MslOptions> {
                override fun write(module: Module, moduleInfo: ModuleInfo): String = ""
                override fun withOptions(options: MslOptions): BackendWriter<MslOptions> = this
                override fun canHandle(module: Module, moduleInfo: ModuleInfo): Boolean = true
            }
            override fun createWithOptions(options: BackendOptions): BackendWriter<*> = create()
        })
        
        val names = registry.listBackendNames()
        assertTrue(names.contains("msl"))
    }

    @Test
    fun testGetBackend() {
        val registry = BackendRegistry()
        registry.register("msl", object : BackendRegistry.BackendFactory {
            override fun create(): BackendWriter<*> = object : BackendWriter<MslOptions> {
                override fun write(module: Module, moduleInfo: ModuleInfo): String = ""
                override fun withOptions(options: MslOptions): BackendWriter<MslOptions> = this
                override fun canHandle(module: Module, moduleInfo: ModuleInfo): Boolean = true
            }
            override fun createWithOptions(options: BackendOptions): BackendWriter<*> = create()
        })
        
        val mslBackend = registry.get("msl")
        assertNotNull(mslBackend)
    }
}
