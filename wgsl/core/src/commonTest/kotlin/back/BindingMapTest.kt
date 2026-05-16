package io.ygdrasil.wgsl.back

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.ygdrasil.wgsl.ir.Binding

class BindingMapTest : FunSpec({

    test("BindingMap insert and get") {
        val map = BindingMap()
        val binding = Binding(group = 0, index = 1)
        val target = BindingMap.BindTarget(buffer = 2)

        map.contains(binding).shouldBeFalse()
        map[binding].shouldBeNull()

        map.insert(binding, target)

        map.contains(binding).shouldBeTrue()
        map[binding] shouldBe target
        map[binding]?.buffer shouldBe 2
    }

    test("BindingMap clear") {
        val map = BindingMap()
        val binding = Binding(group = 0, index = 1)
        val target = BindingMap.BindTarget(buffer = 2)

        map.insert(binding, target)
        map.contains(binding).shouldBeTrue()

        map.clear()
        map.contains(binding).shouldBeFalse()
        map[binding].shouldBeNull()
    }

    test("BindTarget default values") {
        val target = BindingMap.BindTarget()
        target.buffer.shouldBeNull()
        target.texture.shouldBeNull()
        target.sampler.shouldBeNull()
        target.mutable.shouldBeFalse()
    }

    test("BindTarget custom values") {
        val target = BindingMap.BindTarget(buffer = 1, texture = 2, sampler = 3, mutable = true)
        target.buffer shouldBe 1
        target.texture shouldBe 2
        target.sampler shouldBe 3
        target.mutable.shouldBeTrue()
    }
})
