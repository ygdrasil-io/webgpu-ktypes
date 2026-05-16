package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.valid.Capabilities
import io.ygdrasil.wgsl.valid.ShaderStages
import io.ygdrasil.wgsl.valid.ValidationFlags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackendOptionsTest {

    @Test
    fun testMslOptionsDefaults() {
        val options = MslOptions()
        assertEquals("    ", options.indent)
        assertEquals("\n", options.newline)
        assertEquals("2.3", options.version)
        assertEquals("MSL", options.languageName)
        assertEquals(".metal", options.fileExtension)
    }

    @Test
    fun testHlslOptionsDefaults() {
        val options = HlslOptions()
        assertEquals("    ", options.indent)
        assertEquals("6.0", options.version)
        assertEquals("HLSL", options.languageName)
    }

    @Test
    fun testGlslOptionsDefaults() {
        val options = GlslOptions()
        assertEquals("    ", options.indent)
        assertEquals("450", options.version)
        assertEquals(GlslProfile.CORE, options.profile)
    }

    @Test
    fun testWgslOptionsDefaults() {
        val options = WgslOptions()
        assertEquals("    ", options.indent)
        assertEquals("WGSL", options.languageName)
        assertTrue(options.capabilities.float64) // WGSL options default to all caps in my impl
    }
}
