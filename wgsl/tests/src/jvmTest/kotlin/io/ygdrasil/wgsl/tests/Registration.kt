package io.ygdrasil.wgsl.tests

import io.ygdrasil.wgsl.msl.registerMslBackend
import io.ygdrasil.wgsl.hlsl.registerHlslBackend
import io.ygdrasil.wgsl.glsl.registerGlslBackend
import io.ygdrasil.wgsl.back.wgsl.registerWgslBackend

fun registerAllBackends() {
    registerMslBackend()
    registerHlslBackend()
    registerGlslBackend()
    registerWgslBackend()
}
