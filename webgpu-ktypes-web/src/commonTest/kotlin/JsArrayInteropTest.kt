@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.ygdrasil.webgpu

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.JsArray

class JsArrayInteropTest : FunSpec({

    test("jsArray vararg creation and indexing") {
        val arr: JsArray<JsNumber> = jsArray(1.asJsNumber(), 2.asJsNumber(), 3.asJsNumber())
        arr.length shouldBe 3
        arr.bridge_get(0).toLong() shouldBe 1L
        arr.bridge_get(1).toLong() shouldBe 2L
        arr.bridge_get(2).toLong() shouldBe 3L
    }

    test("jsArray extension map to kotlin list") {
        val arr = jsArray(10.asJsNumber(), 20.asJsNumber(), 30.asJsNumber())
        val mapped: List<Long> = arr.map { it.toLong() }
        mapped shouldBe listOf(10L, 20L, 30L)
    }

    test("collection mapJsArray to jsarray") {
        val src = listOf(7, 8, 9, Int.MAX_VALUE, Int.MIN_VALUE)
        val jsArr = src.mapJsArray { it.asJsNumber() }
        jsArr.length shouldBe src.size
        jsArr.bridge_get(0).toLong() shouldBe 7L
        jsArr.bridge_get(0).toInt() shouldBe 7
        jsArr.bridge_get(1).toLong() shouldBe 8L
        jsArr.bridge_get(1).toInt() shouldBe 8
        jsArr.bridge_get(2).toLong() shouldBe 9L
        jsArr.bridge_get(2).toInt() shouldBe 9
        jsArr.bridge_get(3).toLong() shouldBe Int.MAX_VALUE.toLong()
        jsArr.bridge_get(3).toInt() shouldBe Int.MAX_VALUE
        jsArr.bridge_get(4).toLong() shouldBe Int.MIN_VALUE.toLong()
        jsArr.bridge_get(4).toInt() shouldBe Int.MIN_VALUE
    }

    test("createJsObject is mutable and holds properties") {
        val o = createJsObject<TestJs>()
        o shouldNotBe null
        // assign a property via JS and read it back
        o.x = 42.toJsNumber()
        o.x.toLong() shouldBe 42L
    }
})

// js and wasmjs footprint mismatch
expect fun<A: JsAny> JsArray<A>.bridge_get(index: Int): A

external interface TestJs : JsAny {
    var x: JsNumber
}