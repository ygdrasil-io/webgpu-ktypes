package io.ygdrasil.wgsl.back

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.ygdrasil.wgsl.valid.Capabilities
import io.ygdrasil.wgsl.valid.ShaderStages
import io.ygdrasil.wgsl.valid.ValidationFlags

class BackendOptionsTest : FunSpec({

    test("MslOptions defaults") {
        val options = MslOptions()
        options.indent shouldBe "    "
        options.newline shouldBe "\n"
        options.version shouldBe "2.3"
        options.languageName shouldBe "MSL"
        options.fileExtension shouldBe ".metal"
    }

    test("HlslOptions defaults") {
        val options = HlslOptions()
        options.indent shouldBe "    "
        options.version shouldBe "6.0"
        options.languageName shouldBe "HLSL"
    }

    test("GlslOptions defaults") {
        val options = GlslOptions()
        options.indent shouldBe "    "
        options.version shouldBe "450"
        options.profile shouldBe GlslProfile.CORE
    }

    test("WgslOptions defaults") {
        val options = WgslOptions()
        options.indent shouldBe "    "
        options.languageName shouldBe "WGSL"
        options.capabilities.float64.shouldBeTrue() // WGSL options default to all caps in my impl
    }
})
