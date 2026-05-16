package io.ygdrasil.wgsl.proc

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NamerTest : FunSpec({

    test("Sanitize names") {
        val namer = Namer()
        namer.call("valid_name") shouldBe "valid_name"
        namer.call("invalid-name") shouldBe "invalid_name"
        namer.call("123name") shouldBe "_123name"
        namer.call("") shouldBe "_"
    }

    test("Unique names") {
        val namer = Namer()
        namer.call("test") shouldBe "test"
        namer.call("test") shouldBe "test_1"
        namer.call("test") shouldBe "test_2"
    }

    test("Reserved keywords") {
        val namer = Namer()
        namer.reset(listOf("reserved"))
        namer.call("reserved") shouldBe "reserved_1"
        namer.call("reserved") shouldBe "reserved_2"
    }

    test("Sanitization and collision") {
        val namer = Namer()
        namer.call("a-b") shouldBe "a_b"
        namer.call("a_b") shouldBe "a_b_1"
    }
})
