package io.ygdrasil.wgsl.back

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldNotBe
import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.valid.ModuleInfo

class BackendRegistryTest : FunSpec({

    test("list backend names") {
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
        names shouldContain "msl"
    }

    test("get backend") {
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
        mslBackend shouldNotBe null
    }
})
