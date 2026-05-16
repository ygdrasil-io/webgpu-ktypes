package io.ygdrasil.wgsl.tests

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ygdrasil.wgsl.back.BackendOptions
import io.ygdrasil.wgsl.back.BackendRegistry
import io.ygdrasil.wgsl.parser.Lowerer
import io.ygdrasil.wgsl.parser.TypeResolver
import io.ygdrasil.wgsl.parser.parseWgsl
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

abstract class GoldenTestBase(val backendName: String) : FunSpec({

    registerAllBackends()
    val goldenUpdate = System.getenv("GOLDEN_UPDATE")?.toBoolean() ?: false
    val rootDir = findProjectRoot()
    val inputDir = rootDir.resolve("tests/golden/inputs")
    val outputBaseDir = rootDir.resolve("tests/golden/outputs")

    context("$backendName Golden Tests") {
        val inputFiles = Files.list(inputDir)
            .filter { it.toString().endsWith(".wgsl") }
            .toList()

        inputFiles.forEach { inputFile ->
            val fileName = inputFile.fileName.toString()
            test("Golden test: $fileName") {
                println("[DEBUG_LOG] Testing $fileName")
                val source = Files.readString(inputFile)
                
                // 1. Parse
                println("[DEBUG_LOG] Parsing...")
                val unit = parseWgsl(source)
                
                // 2. Resolve types
                println("[DEBUG_LOG] Resolving types...")
                val resolver = TypeResolver()
                val resolutionResult = resolver.resolve(unit)
                if (!resolutionResult.isSuccess) {
                    throw RuntimeException("Type resolution failed: ${resolutionResult.unresolvedReferences}")
                }
                
                // 3. Lower to IR
                println("[DEBUG_LOG] Lowering to IR...")
                val lowerer = Lowerer()
                val module = lowerer.lower(resolutionResult.resolvedUnit)
                
                // 4. Generate backend code
                println("[DEBUG_LOG] Generating backend code for $backendName...")
                val writer = BackendRegistry.DEFAULT.get(backendName) ?: throw RuntimeException("Backend $backendName not found")
                val output = writer.write(module, io.ygdrasil.wgsl.valid.ModuleInfo())
                
                // 5. Compare or Update
                val outputFile = outputBaseDir.resolve(backendName).resolve(fileName.replace(".wgsl", getExtension(backendName)))
                Files.createDirectories(outputFile.parent)
                
                if (goldenUpdate || !Files.exists(outputFile)) {
                    Files.writeString(outputFile, output)
                } else {
                    val expected = Files.readString(outputFile)
                    // TODO: normalize output before comparison?
                    output shouldBe expected
                }
            }
        }
    }
})

private fun findProjectRoot(): java.nio.file.Path {
    var current = Paths.get(".").toAbsolutePath()
    while (current != null) {
        if (Files.exists(current.resolve("settings.gradle.kts"))) {
            return current
        }
        current = current.parent
    }
    return Paths.get(".")
}

private fun getExtension(backend: String): String = when (backend.lowercase()) {
    "msl" -> ".metal"
    "hlsl" -> ".hlsl"
    "glsl" -> ".glsl"
    "wgsl" -> ".wgsl"
    else -> ".txt"
}
