package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.Binding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BindingMapTest {

    @Test
    fun testBindingMapInsertAndGet() {
        val map = BindingMap()
        val binding = Binding(group = 0, index = 1)
        val target = BindingMap.BindTarget(buffer = 2)

        assertFalse(map.contains(binding))
        assertNull(map[binding])

        map.insert(binding, target)

        assertTrue(map.contains(binding))
        assertEquals(target, map[binding])
        assertEquals(2, map[binding]?.buffer)
    }

    @Test
    fun testBindingMapClear() {
        val map = BindingMap()
        val binding = Binding(group = 0, index = 1)
        val target = BindingMap.BindTarget(buffer = 2)

        map.insert(binding, target)
        assertTrue(map.contains(binding))

        map.clear()
        assertFalse(map.contains(binding))
        assertNull(map[binding])
    }

    @Test
    fun testBindTargetDefaultValues() {
        val target = BindingMap.BindTarget()
        assertNull(target.buffer)
        assertNull(target.texture)
        assertNull(target.sampler)
        assertFalse(target.mutable)
    }

    @Test
    fun testBindTargetCustomValues() {
        val target = BindingMap.BindTarget(buffer = 1, texture = 2, sampler = 3, mutable = true)
        assertEquals(1, target.buffer)
        assertEquals(2, target.texture)
        assertEquals(3, target.sampler)
        assertTrue(target.mutable)
    }
}
