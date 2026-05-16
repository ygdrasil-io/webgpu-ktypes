package io.ygdrasil.wgsl.tests

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ygdrasil.wgsl.back.BackendOptions
import io.ygdrasil.wgsl.back.BackendRegistry
import io.ygdrasil.wgsl.parser.Lowerer
import io.ygdrasil.wgsl.parser.TypeResolver
import io.ygdrasil.wgsl.parser.parseWgsl
import io.ygdrasil.wgsl.tests.validator.BackendType
import io.ygdrasil.wgsl.tests.validator.ValidatorFactory
import io.ygdrasil.wgsl.tests.roundtrip.WgslNormalizer
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
                    if (backendName.lowercase() == "wgsl") {
                        val normalizedActual = WgslNormalizer.normalize(output)
                        val normalizedExpected = WgslNormalizer.normalize(expected)
                        normalizedActual shouldBe normalizedExpected
                    } else {
                        output shouldBe expected
                    }
                }

                // 6. Native Validation (if available)
                val type = when (backendName.lowercase()) {
                    "msl" -> BackendType.MSL
                    "glsl" -> BackendType.GLSL
                    "hlsl" -> BackendType.HLSL
                    "spirv" -> BackendType.SPIRV
                    else -> null
                }

                if (type != null && ValidatorFactory.isAvailable(type)) {
                    println("[DEBUG_LOG] Native validation for $backendName...")
                    val stage = module.entryPoints.firstOrNull()?.stage?.let {
                        when (it) {
                            io.ygdrasil.wgsl.ir.ShaderStage.Vertex -> io.ygdrasil.wgsl.tests.validator.ShaderStage.VERTEX
                            io.ygdrasil.wgsl.ir.ShaderStage.Fragment -> io.ygdrasil.wgsl.tests.validator.ShaderStage.FRAGMENT
                            io.ygdrasil.wgsl.ir.ShaderStage.Compute -> io.ygdrasil.wgsl.tests.validator.ShaderStage.COMPUTE
                        }
                    }

                    val validator = ValidatorFactory.getValidator(type)!!
                    val validationResult = validator.validate(output, stage = stage)
                    if (validationResult.isFailure) {
                        println("[DEBUG_LOG] Native validation FAILED for $fileName ($backendName)")
                        println(validationResult.output)
                        throw RuntimeException("Native validation failed for $fileName:\n${validationResult.output}")
                    } else {
                        println("[DEBUG_LOG] Native validation SUCCESS for $fileName ($backendName)")
                    }
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
