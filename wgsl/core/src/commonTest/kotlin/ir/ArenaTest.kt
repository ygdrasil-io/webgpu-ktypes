package io.ygdrasil.wgsl.ir

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ygdrasil.wgsl.arena.Arena
import io.ygdrasil.wgsl.arena.Handle

class ArenaTest : FunSpec({
    test("Empty arena has size 0") {
        val arena = Arena<String>()
        arena.size shouldBe 0
        arena.isEmpty() shouldBe true
    }

    test("Arena.append adds element and returns handle") {
        val arena = Arena<String>()
        val handle = arena.append("test")

        arena.size shouldBe 1
        arena[handle] shouldBe "test"
    }

    test("Arena.appendAll adds multiple elements") {
        val arena = Arena<String>()
        val handles = arena.appendAll(listOf("a", "b", "c"))

        arena.size shouldBe 3
        handles.size shouldBe 3
        arena[handles[0]] shouldBe "a"
        arena[handles[1]] shouldBe "b"
        arena[handles[2]] shouldBe "c"
    }

    test("Arena get by Handle") {
        val arena = Arena<String>()
        val handle = arena.append("value")
        arena[handle] shouldBe "value"
    }

    test("Arena.getOrNull returns null for invalid handle") {
        val arena = Arena<String>()
        val handle = arena.append("value")

        arena.getOrNull(handle) shouldBe "value"
        arena.getOrNull(Handle.fromIndex<String>(999)) shouldBe null
    }

    test("Arena get by Int index") {
        val arena = Arena<String>()
        arena.append("first")
        arena.append("second")

        arena[0] shouldBe "first"
        arena[1] shouldBe "second"
    }

    test("Arena.contains checks for element") {
        val arena = Arena<String>()
        arena.append("test")

        arena.contains("test") shouldBe true
        arena.contains("missing") shouldBe false
    }

    test("Arena.findHandle finds handle by predicate") {
        val arena = Arena<String>()
        val handle1 = arena.append("find me")
        arena.append("other")

        arena.findHandle { it == "find me" } shouldBe handle1
    }

    test("Arena.handleOf finds handle by element") {
        val arena = Arena<String>()
        val handle = arena.append("value")

        arena.handleOf("value") shouldBe handle
    }

    test("Arena.clear removes all elements") {
        val arena = Arena<String>()
        arena.append("test")
        arena.clear()

        arena.size shouldBe 0
        arena.isEmpty() shouldBe true
    }

    test("Arena.copy creates a copy") {
        val arena1 = Arena<String>()
        arena1.append("original")

        val arena2 = arena1.copy()
        arena2.size shouldBe 1
        arena2[0] shouldBe "original"
    }

    test("Arena.forEachWithHandle iterates with handles") {
        val arena = Arena<String>()
        val handle1 = arena.append("first")
        val handle2 = arena.append("second")

        var count = 0
        arena.forEachWithHandle { handle, value ->
            count++
            when (handle) {
                handle1 -> value shouldBe "first"
                handle2 -> value shouldBe "second"
                else -> error("Unexpected handle")
            }
        }
        count shouldBe 2
    }

    test("Arena.map transforms elements") {
        val arena = Arena<String>()
        arena.append("a")
        arena.append("b")

        arena.map { it.uppercase() } shouldBe listOf("A", "B")
    }

    test("Arena.filter filters elements") {
        val arena = Arena<Int>()
        arena.append(1)
        arena.append(2)
        arena.append(3)

        arena.filter { it % 2 == 0 } shouldBe listOf(2)
    }

    test("Handle.INVALID is not valid") {
        Handle.INVALID.isValid() shouldBe false
        Handle.INVALID.isInvalid() shouldBe true
    }

    test("Handle.fromIndex creates valid handle") {
        val handle = Handle.fromIndex<String>(5)
        handle.isValid() shouldBe true
        handle.isInvalid() shouldBe false
    }

    test("Handle.toUntyped converts type") {
        val handle: Handle<String> = Handle.fromIndex(10)
        val untyped: Handle<Any> = handle.toUntyped()
        untyped.index shouldBe 10
    }
})
