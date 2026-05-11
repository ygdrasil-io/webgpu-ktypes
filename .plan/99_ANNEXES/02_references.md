# References

## Table of Contents

1. [Specifications](#specifications)
2. [WebGPU Ecosystem](#webgpu-ecosystem)
3. [Shader Languages](#shader-languages)
4. [Naga Rust Source](#naga-rust-source)
5. [Kotlin & JVM](#kotlin--jvm)
6. [Build & Testing](#build--testing)
7. [Validation Tools](#validation-tools)
8. [Related Projects](#related-projects)

---

## Specifications

### WebGPU

| Resource | URL | Description |
|----------|-----|-------------|
| WebGPU Specification | https://www.w3.org/TR/webgpu/ | Official W3C WebGPU API specification |
| WebGPU Explainer | https://surma.dev/things/blog/webgpu-explainer | Community explainer document |

### WGSL

| Resource | URL | Description |
|----------|-----|-------------|
| WGSL Specification | https://gpuweb.github.io/gpuweb/wgsl/ | Official WGSL shading language specification |
| WGSL Grammar | https://gpuweb.github.io/gpuweb/wgsl/#grammar | EBNF grammar for WGSL |
| WGSL Built-in Functions | https://gpuweb.github.io/gpuweb/wgsl/#builtin-functions | Complete list of WGSL built-in functions |

### SPIR-V

| Resource | URL | Description |
|----------|-----|-------------|
| SPIR-V Specification | https://www.khronos.org/registry/SPIR-V/specs/unified1/SPIRV.html | Khronos SPIR-V specification |
| SPIR-V Tools | https://github.com/KhronosGroup/SPIRV-Tools | Official SPIR-V tools and validators |

---

## WebGPU Ecosystem

### Implementations

| Project | URL | Description |
|---------|-----|-------------|
| wgpu-native | https://github.com/gfx-rs/wgpu | Rust WebGPU implementation (reference) |
| Dawn | https://dawn.googlesource.com/dawn | Google's WebGPU implementation (C++) |
| WebGPU (Chrome) | https://chromestatus.com/feature/6252254436925440 | Chrome's WebGPU implementation |
| WebGPU (Firefox) | https://bugzilla.mozilla.org/show_bug.cgi?id=1534480 | Firefox's WebGPU implementation |
| WebGPU (Safari) | https://developer.apple.com/documentation/webkit/configure_webgpu | Safari/WebKit WebGPU support |

### Bindings

| Project | URL | Description |
|---------|-----|-------------|
| wgpu (Rust) | https://crates.io/crates/wgpu | Official Rust bindings for wgpu-native |
| wgpu-native | https://crates.io/crates/wgpu-hal | Native backend for wgpu |
| deno_webgpu | https://github.com/gfx-rs/wgpu/tree/trunk/deno_webgpu | Deno JavaScript bindings |

---

## Shader Languages

### Metal Shading Language (MSL)

| Resource | URL | Description |
|----------|-----|-------------|
| MSL Specification | https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf | Apple's MSL specification |
| MSL Reference | https://developer.apple.com/documentation/metal/metal_shading_language | MSL documentation |
| Metal Developer Guide | https://developer.apple.com/documentation/metal | Metal API and shading language guides |

### High-Level Shading Language (HLSL)

| Resource | URL | Description |
|----------|-----|-------------|
| HLSL Documentation | https://docs.microsoft.com/en-us/windows/win32/directx-hlsl/dx-graphics-hlsl | Microsoft HLSL documentation |
| HLSL Reference | https://docs.microsoft.com/en-us/windows/win32/directx-hlsl/hlsl-reference | HLSL reference manual |
| FXC Compiler | https://docs.microsoft.com/en-us/windows/win32/directx-hlsl/fxc | DirectX Effects Compiler |
| DXC Compiler | https://github.com/microsoft/DirectXShaderCompiler | DirectX Shader Compiler (open source) |

### OpenGL Shading Language (GLSL)

| Resource | URL | Description |
|----------|-----|-------------|
| GLSL Specification | https://www.khronos.org/registry/OpenGL/specs/gl/GLSLangSpec.4.60.pdf | Khronos GLSL 4.60 specification |
| GLSL Reference | https://www.khronos.org/opengl/wiki/Core_Language_(GLSL) | GLSL reference pages |
| glslangValidator | https://github.com/KhronosGroup/glslang | Khronos GLSL validator and compiler |

### SPIR-V

| Resource | URL | Description |
|----------|-----|-------------|
| SPIR-V Specification | https://www.khronos.org/registry/SPIR-V/specs/unified1/SPIRV.html | SPIR-V binary IR specification |
| SPIR-V Tools | https://github.com/KhronosGroup/SPIRV-Tools | SPIR-V assembler, disassembler, validator |
| spirv-val | https://github.com/KhronosGroup/SPIRV-Tools/blob/master/README.md#validation | SPIR-V validator tool |

---

## Naga Rust Source

All paths are absolute from the reference implementation at `/Users/chaos/RustroverProjects/wgpu/naga/`

### Core Module

| File | Path | Description |
|------|------|-------------|
| Module | `/Users/chaos/RustroverProjects/wgpu/naga/src/module.rs` | Core IR Module structure and definitions |
| Arena | `/Users/chaos/RustroverProjects/wgpu/naga/src/arena.rs` | Arena-based memory management for IR objects |
| Handle | `/Users/chaos/RustroverProjects/wgpu/naga/src/handle.rs` | Handle types for IR object references |
| Types | `/Users/chaos/RustroverProjects/wgpu/naga/src/types.rs` | Type system definitions (Scalar, Vector, Matrix, etc.) |
| Expressions | `/Users/chaos/RustroverProjects/wgpu/naga/src/expression.rs` | Expression tree definitions |
| Statements | `/Users/chaos/RustroverProjects/wgpu/naga/src/statement.rs` | Statement definitions |
| Functions | `/Users/chaos/RustroverProjects/wgpu/naga/src/function.rs` | Function definitions and processing |
| Binary | `/Users/chaos/RustroverProjects/wgpu/naga/src/binary.rs` | Binary serialization/deserialization of IR |

### Frontends

| File | Path | Description |
|------|------|-------------|
| WGSL Parser | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parser.rs` | WGSL recursive descent parser |
| WGSL Lexer | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/lexer.rs` | WGSL lexer/tokenizer |
| WGSL Mod | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/mod.rs` | WGSL frontend module entry point |
| GLSL Parser | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/glsl/parser.rs` | GLSL parser |
| SPIR-V Parser | `/Users/chaos/RustroverProjects/wgpu/naga/src/front/spv/parser.rs` | SPIR-V binary parser |

### Processing

| File | Path | Description |
|------|------|-------------|
| Constant Evaluator | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/constant_evaluator.rs` | Constant expression evaluation |
| Typifier | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/typifier.rs` | Type inference for untyped IR |
| Layouter | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/layouter.rs` | Memory layout calculation |
| Namer | `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/namer.rs` | Name generation and sanitization |
| Validator | `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/mod.rs` | IR validation and capability checking |

### Backends

| File | Path | Description |
|------|------|-------------|
| MSL Writer | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/msl/writer.rs` | Metal Shading Language code generation |
| HLSL Writer | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/writer.rs` | HLSL code generation |
| GLSL Writer | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/writer.rs` | GLSL code generation |
| WGSL Writer | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/writer.rs` | WGSL code generation (round-trip) |
| SPIR-V Writer | `/Users/chaos/RustroverProjects/wgpu/naga/src/back/spv/writer.rs` | SPIR-V binary generation |

### Tests

| File | Path | Description |
|------|------|-------------|
| WGSL Tests In | `/Users/chaos/RustroverProjects/wgpu/naga/tests/in/wgsl/` | Input WGSL shader files for tests |
| WGSL Tests Out | `/Users/chaos/RustroverProjects/wgpu/naga/tests/out/wgsl/` | Expected output WGSL files |
| MSL Tests Out | `/Users/chaos/RustroverProjects/wgpu/naga/tests/out/msl/` | Expected output MSL files |
| HLSL Tests Out | `/Users/chaos/RustroverProjects/wgpu/naga/tests/out/hlsl/` | Expected output HLSL files |
| GLSL Tests Out | `/Users/chaos/RustroverProjects/wgpu/naga/tests/out/glsl/` | Expected output GLSL files |
| SPIR-V Tests Out | `/Users/chaos/RustroverProjects/wgpu/naga/tests/out/spv/` | Expected output SPIR-V files |
| Test Runner | `/Users/chaos/RustroverProjects/wgpu/naga/src/test.rs` | Test runner and utilities |

### Project Files

| File | Path | Description |
|------|------|-------------|
| Cargo.toml | `/Users/chaos/RustroverProjects/wgpu/naga/Cargo.toml` | Naga crate configuration |
| lib.rs | `/Users/chaos/RustroverProjects/wgpu/naga/src/lib.rs` | Library entry point |

---

## Kotlin & JVM

### Kotlin Language

| Resource | URL | Description |
|----------|-----|-------------|
| Kotlin Language Documentation | https://kotlinlang.org/docs/home.html | Official Kotlin documentation |
| Kotlin Specification | https://kotlinlang.org/spec/doc/reference/__temporary-directory-for-documentation.md | Kotlin language specification |
| Kotlin for Java Developers | https://kotlinlang.org/docs/java-to-kotlin.html | Migration guide from Java to Kotlin |
| Kotlin Coroutines | https://kotlinlang.org/docs/coroutines-guide.html | Coroutine documentation |
| Kotlin Flows | https://kotlinlang.org/docs/flow.html | Kotlin Flow documentation |

### Kotlin Standard Library

| Resource | URL | Description |
|----------|-----|-------------|
| kotlin-stdlib | https://kotlinlang.org/api/latest/jvm/stdlib/ | Kotlin Standard Library API |
| kotlinx.serialization | https://github.com/Kotlin/kotlinx.serialization | Kotlin serialization library |
| kotlinx.coroutines | https://github.com/Kotlin/kotlinx.coroutines | Kotlin coroutines library |

### JVM

| Resource | URL | Description |
|----------|-----|-------------|
| JVM Specification | https://docs.oracle.com/javase/specs/jvms/se17/html/ | Java Virtual Machine Specification |
| Java SE Documentation | https://docs.oracle.com/en/java/javase/17/ | Java Standard Edition documentation |

### Build Tools

| Resource | URL | Description |
|----------|-----|-------------|
| Gradle Documentation | https://docs.gradle.org/current/userguide/userguide.html | Gradle user guide |
| Gradle Kotlin DSL | https://docs.gradle.org/current/userguide/kotlin_dsl.html | Kotlin DSL for Gradle |
| Gradle Properties | https://docs.gradle.org/current/userguide/build_environment.html | Build environment configuration |

---

## Build & Testing

### Testing Frameworks

| Resource | URL | Description |
|----------|-----|-------------|
| JUnit 5 | https://junit.org/junit5/ | JUnit 5 testing framework |
| AssertJ | https://assertj.github.io/doc/ | Fluent assertion library |
| Truth | https://truth.dev/ | Google's assertion library |
| Kotest | https://kotest.io/ | Kotlin-first testing framework |

### Coverage Tools

| Resource | URL | Description |
|----------|-----|-------------|
| JaCoCo | https://www.jacoco.org/jacoco/ | Java Code Coverage Library |
| JaCoCo Gradle Plugin | https://docs.gradle.org/current/userguide/jacoco_plugin.html | Gradle plugin for JaCoCo |

### CI/CD

| Resource | URL | Description |
|----------|-----|-------------|
| GitHub Actions | https://docs.github.com/en/actions | GitHub Actions documentation |
| GitHub Actions Workflows | https://docs.github.com/en/actions/using-workflows | Workflow syntax and examples |

---

## Validation Tools

### Metal

| Tool | Command | Description |
|------|---------|-------------|
| Metal Compiler | `metal` or `metalc` | Apple Metal Shading Language compiler |
| Metal Validation | `metal -c -o /dev/null shader.metal` | Validate MSL by compiling |

**Installation (macOS):**
```bash
xcode-select --install
```

**Validation Command:**
```bash
metal -c -o /dev/null input.metal 2>&1
```

### HLSL / DirectX

| Tool | Command | Description |
|------|---------|-------------|
| FXC Compiler | `fxc` | DirectX Effects Compiler (legacy) |
| DXC Compiler | `dxc` | DirectX Shader Compiler (modern) |

**Installation (Windows):**
- FXC: Included with Windows SDK
- DXC: https://github.com/microsoft/DirectXShaderCompiler/releases

**Validation Commands:**
```bash
# DXC (recommended)
dxc -T ps_6_0 -E main -Vd shader.hlsl 2>&1

# FXC (legacy)
fxc /T ps_5_0 /E main /Zi shader.hlsl 2>&1
```

### GLSL

| Tool | Command | Description |
|------|---------|-------------|
| glslangValidator | `glslangValidator` | Khronos GLSL validator and compiler |

**Installation:**
```bash
# macOS (Homebrew)
brew install glslang

# Linux (Ubuntu/Debian)
sudo apt-get install glslang-tools

# Windows (Chocolatey)
choco install glslang
```

**Validation Command:**
```bash
# GLSL 450 (Vulkan)
glslangValidator -V -x shader.vert -o /dev/null 2>&1

# GLSL 330 (OpenGL)
glslangValidator -G 330 -x shader.vert -o /dev/null 2>&1
```

### SPIR-V

| Tool | Command | Description |
|------|---------|-------------|
| spirv-val | `spirv-val` | SPIR-V validator |
| spirv-as | `spirv-as` | SPIR-V assembler |
| spirv-dis | `spirv-dis` | SPIR-V disassembler |

**Installation:**
```bash
# macOS (Homebrew)
brew install spirv-tools

# Linux (Ubuntu/Debian)
sudo apt-get install spirv-tools
```

**Validation Command:**
```bash
spirv-val shader.spv 2>&1
```

---

## Related Projects

### Shader Translation

| Project | URL | Description |
|---------|-----|-------------|
| SpirV-Cross | https://github.com/KhronosGroup/SPIRV-Cross | SPIR-V to GLSL/HLSL/MSL translator |
| glslang | https://github.com/KhronosGroup/glslang | Khronos GLSL/SPIR-V toolchain |
| Shaderc | https://github.com/google/shaderc | GLSL to SPIR-V compiler |

### WebGPU

| Project | URL | Description |
|---------|-----|-------------|
| gfx-rs/wgpu | https://github.com/gfx-rs/wgpu | Rust WebGPU implementation |
| google/dawn | https://github.com/google/dawn | Google's WebGPU implementation |
| webgpu-native | https://github.com/webgpu-native/webgpu-native | WebGPU native implementation |

### Kotlin GPU

| Project | URL | Description |
|---------|-----|-------------|
| Kotlin GPU | https://github.com/Kotlin/gpu | Kotlin GPU programming (experimental) |
| tornadoVM | https://github.com/beehive-lab/TornadoVM | TornadoVM for GPU acceleration |

### Parsing & Compilers

| Project | URL | Description |
|---------|-----|-------------|
| ANTLR | https://www.antlr.org/ | Parser generator |
| JavaCC | https://javacc.github.io/javacc/ | Parser generator for Java |
| Kotlin Poet | https://github.com/square/kotlinpoet | Kotlin code generation library |
