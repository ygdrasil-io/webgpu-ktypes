# Validation par Validateurs Natifs

## Sommaire

1. [Overview](#overview)
2. [spirv-val (SPIR-V)](#spirv-val-spir-v)
3. [glslangValidator (GLSL)](#glslangvalidator-glsl)
4. [Metal Compiler (MSL)](#metal-compiler-msl)
5. [DXC / FXC (HLSL)](#dxc--fxc-hlsl)
6. [Intégration Kotlin](#intégration-kotlin)
7. [Configuration CI](#configuration-ci)
8. [Gestion des Erreurs](#gestion-des-erreurs)

---

## Overview

### Principe

La **validation native** consiste à utiliser les compilateurs et validateurs officiels de chaque plateforme pour vérifier que le code généré par WebGPU-KTypes est valide et peut être compilé.

| Backend | Validateur | Plateforme | Commande |
|---------|------------|------------|----------|
| SPIR-V | `spirv-val` | Multiplateforme | `spirv-val file.spv` |
| GLSL | `glslangValidator` | Multiplateforme | `glslangValidator file.glsl` |
| MSL | Metal Compiler | macOS | `xcrun -sdk macosx metal file.msl` |
| HLSL | DXC | Windows | `dxc file.hlsl -T ps_6_0` |
| HLSL | FXC (legacy) | Windows | `fxc file.hlsl /T ps_5_0` |

### Stratégie de Validation

```
Input WGSL
    │
    ▼
┌─────────────────┐
│   Parse → IR    │ ◄──── Golden Test (Phase 5.0)
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  Backend Code   │
│  Generation      │
└─────────────────┘
    │   │   │   │
    ▼   ▼   ▼   ▼
┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
│ MSL │ │HLSL │ │GLSL │ │SPIR-V│
└─────┘ └─────┘ └─────┘ └─────┘
    │   │   │   │
    ▼   ▼   ▼   ▼
┌─────────────────────────────────────────┐
│           Native Validators               │ ◄──── This Document
└─────────────────────────────────────────┘
    │   │   │   │
    ▼   ▼   ▼   ▼
  Pass  Pass  Pass  Pass  (Validation Result)
```

### Niveaux de Validation

| Niveau | Description | Outil | Fréquence |
|--------|-------------|-------|-----------|
| 1 | Syntaxe correcte | Validateur | Tous les commits |
| 2 | Sémantique correcte | Compilateur | Tous les commits |
| 3 | Comportement correct | Runtime | Tests d'intégration |
| 4 | Performance | Profiling | PR spécifiques |

---

## spirv-val (SPIR-V)

### Installation

#### macOS (Homebrew)
```bash
brew install vulkan-tools
```

#### Linux (Debian/Ubuntu)
```bash
sudo apt-get install spirv-tools
```

#### Windows (Chocolatey)
```powershell
choco install spirv-tools
```

#### Manuel
```bash
# Télécharger depuis https://github.com/KhronosGroup/SPIRV-Tools/releases
wget https://github.com/KhronosGroup/SPIRV-Tools/releases/download/v2023.3/spirv-tools-v2023.3-linux-x86_64.tar.gz
tar xzf spirv-tools-*.tar.gz
cd spirv-tools-v2023.3-linux-x86_64
export PATH=$PATH:$(pwd)/bin
```

### Utilisation

#### Validation basique
```bash
spirv-val file.spv
```

#### Options utiles
```bash
# Afficher la version
spirv-val --version

# Valider avec des extensions spécifiques
spirv-val --capability shader --capability float16 file.spv

# Afficher l'AST
spirv-val --disassemble file.spv

# Valider contre une version spécifique
spirv-val --target-env spirv1.3 file.spv
```

### Sortie

- **Succès** : Code de sortie 0, aucun message
- **Échec** : Code de sortie non-zero, messages d'erreur détaillés

```
Example error output:
error: instruction at line 123, column 45: OpAccessChain Pointer <id> '16' of type <id> '15' does not have a valid base
error: 1 validation error(s).
```

### Intégration avec WebGPU-KTypes

Le backend SPIR-V n'est pas une priorité immédiate (voir contraintes du projet), mais la validation peut être ajoutée plus tard.

---

## glslangValidator (GLSL)

### Installation

#### macOS (Homebrew)
```bash
brew install glslang
```

#### Linux (Debian/Ubuntu)
```bash
sudo apt-get install glslang-tools
```

#### Windows
- **Via Vulkan SDK** : Installer depuis [LunarG Vulkan SDK](https://vulkan.lunarg.com/sdk/home)
- **Via Chocolatey** :
```powershell
choco install vulkan-sdk
```

#### Manuel
```bash
# Télécharger depuis https://github.com/KhronosGroup/glslang/releases
wget https://github.com/KhronosGroup/glslang/releases/download/14.1.0/glslang-14.1.0-linux-x64.tar.xz
tar xf glslang-*.tar.xz
cd glslang-14.1.0-linux-x64/bin
export PATH=$PATH:$(pwd)
```

### Utilisation

#### Validation basique
```bash
# GLSL 450 (Vulkan)
glslangValidator -V file.glsl

# GLSL 460 (Vulkan 1.2)
glslangValidator -V460 file.glsl

# OpenGL ES 3.2
glslangValidator -e file.glsl
```

#### Options utiles
```bash
# Afficher la version
glslangValidator --version

# Valider comme vertex shader
glslangValidator -V -S vert file.glsl

# Valider comme fragment shader
glslangValidator -V -S frag file.glsl

# Valider comme compute shader
glslangValidator -V -S comp file.glsl

# Afficher l'AST
glslangValidator -V --aml file.glsl

# Valider avec des extensions
glslangValidator -V -D GL_EXT_shader_explicit_arithmetic_types file.glsl
```

### Profiles par Backend

| Backend | Profile | Commande |
|---------|---------|----------|
| OpenGL 4.5 | `-G` | `glslangValidator -G file.glsl` |
| OpenGL ES 3.2 | `-e` | `glslangValidator -e file.glsl` |
| Vulkan 1.0 | `-V` | `glslangValidator -V file.glsl` |
| Vulkan 1.1 | `-V110` | `glslangValidator -V110 file.glsl` |
| Vulkan 1.2 | `-V460` | `glslangValidator -V460 file.glsl` |

### Sortie

- **Succès** : Affiche le SPIR-V généré ou "No errors."
- **Échec** : Messages d'erreur avec numéros de ligne

```
Example success output:
#version 450
#extension GL_GOOGLE_include_directive : require
... (SPIR-V assembly output)

Example error output:
ERROR: 0:12: 'struct' : syntax error
```

### Intégration Kotlin

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/validator/GlslValidator.kt

package dev.gfxrs.naga.test.validator

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Wrapper for glslangValidator
 */
class GlslValidator : BackendValidator {
    
    override val name: String = "glslangValidator"
    override val backendType: BackendType = BackendType.GLSL
    
    private var executable: String = findExecutable()
    
    /**
     * Validate GLSL code
     * @param code GLSL source code
     * @param version GLSL version (default: 450 for Vulkan)
     * @param stage Shader stage (default: FRAGMENT)
     * @return ValidationResult
     */
    override fun validate(
        code: String,
        version: String?,
        stage: ShaderStage?
    ): ValidationResult {
        val tempFile = createTempFile(suffix = ".glsl")
        try {
            tempFile.writeText(code)
            
            val command = buildList {
                add(executable)
                
                // Version
                when (version?.uppercase()) {
                    "460", "V460" -> add("-V460")
                    "450", "V" -> add("-V")
                    "320", "E" -> add("-e")
                    "400", "G" -> add("-G")
                    else -> add("-V") // Default to Vulkan 450
                }
                
                // Stage
                val stageArg = when (stage) {
                    ShaderStage.VERTEX -> "-S vert"
                    ShaderStage.FRAGMENT -> "-S frag"
                    ShaderStage.COMPUTE -> "-S comp"
                    null -> "-S frag" // Default to fragment
                }
                add(stageArg)
                
                add(tempFile.absolutePath)
            }
            
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()
            
            return ValidationResult(
                success = exitCode == 0,
                output = output,
                exitCode = exitCode,
                command = command.joinToString(" ")
            )
            
        } finally {
            tempFile.delete()
        }
    }
    
    private fun findExecutable(): String {
        val candidates = listOf(
            "glslangValidator",
            "glslangValidator.exe",
            "/usr/local/bin/glslangValidator",
            "/usr/bin/glslangValidator",
            "C:/VulkanSDK/${System.getenv("VULKAN_SDK_VERSION") ?: "1.3.268.0"}/Bin/glslangValidator.exe"
        )
        
        for (candidate in candidates) {
            val file = File(candidate)
            if (file.exists() && file.canExecute()) {
                return candidate
            }
        }
        
        error("glslangValidator not found. Please install it from https://github.com/KhronosGroup/glslang")
    }
    
    companion object {
        fun isAvailable(): Boolean = try {
            GlslValidator().findExecutable()
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

---

## Metal Compiler (MSL)

### Installation

Le compilateur Metal est inclus avec Xcode sur macOS. Aucune installation supplémentaire nécessaire.

### Prérequis

- macOS 10.15+ (Catalina) ou supérieur
- Xcode installé avec les outils de ligne de commande
- SDK Metal disponible

```bash
# Installer les outils de ligne de commande Xcode
xcode-select --install

# Accepter la licence (si nécessaire)
sudo xcodebuild -license accept
```

### Utilisation

#### Validation basique
```bash
# Compiler un shader Metal
excrun -sdk macosx metal file.msl -o /dev/null

# Compiler pour iOS
excrun -sdk iphoneos metal file.msl -o /dev/null
```

#### Options utiles
```bash
# Afficher la version
xcrun -sdk macosx metal --version

# Compiler comme vertex shader
excrun -sdk macosx metal file.msl -o output.air -c

# Compiler comme fragment shader
excrun -sdk macosx metal file.msl -o output.air -f

# Compiler comme compute shader
excrun -sdk macosx metal file.msl -o output.air -k

# Afficher l'AST Metal
excrun -sdk macosx metal --print-ast file.msl

# Afficher le code MSL après préprocessing
excrun -sdk macosx metal --preprocess file.msl

# Valider sans générer de sortie
xcrun -sdk macosx metal file.msl --validate-only
```

### Profiles par Plateforme

| Plateforme | SDK | Commande |
|------------|-----|----------|
| macOS | `macosx` | `xcrun -sdk macosx metal` |
| iOS (Device) | `iphoneos` | `xcrun -sdk iphoneos metal` |
| iOS (Simulator) | `iphonesimulator` | `xcrun -sdk iphonesimulator metal` |
| macOS Catalyst | `mac catalyst` | `xcrun -sdk mac_catalyst metal` |

### Sortie

- **Succès** : Code de sortie 0, fichier `.air` généré
- **Échec** : Code de sortie non-zero, messages d'erreur détaillés

```
Example success output:
(no output, file.air is created)

Example error output:
file.msl:12:45: error: use of undeclared identifier 'unknownVariable'
file.msl:12:45: note: did you mean 'knownVariable'?
error: Metal compiler failed with 1 error
```

### Intégration Kotlin

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/validator/MetalValidator.kt

package dev.gfxrs.naga.test.validator

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Wrapper for Metal Shader Compiler (macOS only)
 */
class MetalValidator : BackendValidator {
    
    override val name: String = "Metal Compiler"
    override val backendType: BackendType = BackendType.MSL
    
    private var executable: String = findExecutable()
    
    /**
     * Validate MSL code
     * @param code MSL source code
     * @param platform Target platform (default: MACOS)
     * @return ValidationResult
     */
    override fun validate(
        code: String,
        platform: String?,
        stage: ShaderStage?
    ): ValidationResult {
        require(isMacOs()) { "Metal Compiler is only available on macOS" }
        
        val tempFile = createTempFile(suffix = ".msl")
        try {
            tempFile.writeText(code)
            
            val sdk = when (platform?.uppercase()) {
                "IOS", "IPHONEOS" -> "iphoneos"
                "SIMULATOR", "IPHONESIMULATOR" -> "iphonesimulator"
                "CATALYST", "MACCATALYST" -> "mac_catalyst"
                else -> "macosx" // Default to macOS
            }
            
            val command = listOf(
                "xcrun", "-sdk", sdk, "metal",
                tempFile.absolutePath,
                "-o", "/dev/null",
                "--validate-only"
            )
            
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()
            
            return ValidationResult(
                success = exitCode == 0,
                output = output,
                exitCode = exitCode,
                command = command.joinToString(" ")
            )
            
        } finally {
            tempFile.delete()
        }
    }
    
    private fun findExecutable(): String {
        // Metal compiler is accessed via xcrun
        val process = ProcessBuilder("xcrun", "-sdk", "macosx", "metal", "--version").start()
        if (process.waitFor() == 0) {
            return "xcrun"
        }
        error("Metal compiler not found. Please install Xcode and the Metal SDK.")
    }
    
    private fun isMacOs(): Boolean {
        return System.getProperty("os.name").contains("Mac", ignoreCase = true)
    }
    
    companion object {
        fun isAvailable(): Boolean = try {
            MetalValidator().findExecutable()
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

---

## DXC / FXC (HLSL)

### Installation

#### Windows (DXC - Recommandé)

DXC (DirectX Shader Compiler) est inclus avec le Windows 10/11 SDK.

```powershell
# Via Visual Studio Installer
# Sélectionner "Windows 10 SDK" ou "Windows 11 SDK"

# Ou télécharger directement depuis Microsoft
# https://github.com/microsoft/DirectXShaderCompiler/releases
```

#### Windows (FXC - Legacy)

FXC (Effect Compiler) est inclus avec DirectX SDK ou Windows SDK.

```powershell
# FXC est généralement dans :
# C:\Program Files (x86)\Windows Kits\10\bin\<version>\x64\fxc.exe
```

#### Linux (Via Wine ou WSI)

Pas officiellement supporté, mais possible via :
- Wine avec DLLs DirectX
- Windows Subsystem for Linux 2 (WSL2)

#### macOS

Pas officiellement supporté. Utiliser :
- Machine virtuelle Windows
- WSL2 sur machine distante

### Utilisation DXC

#### Validation basique
```powershell
# Compiler comme Pixel Shader 6.0
dxc file.hlsl -T ps_6_0 -Zi -Fd file.pdb -Fe file.dxil

# Compiler comme Vertex Shader 6.0
dxc file.hlsl -T vs_6_0 -Zi -Fd file.pdb -Fe file.dxil

# Compiler comme Compute Shader 6.0
dxc file.hlsl -T cs_6_0 -Zi -Fd file.pdb -Fe file.dxil
```

#### Options utiles
```powershell
# Afficher la version
dxc --version

# Valider sans générer de sortie
dxc file.hlsl -T ps_6_0 /Zs

# Générer le code assembly
dxc file.hlsl -T ps_6_0 -Fc file.asm

# Spécifier le niveau de shader
dxc file.hlsl -T ps_6_0 -D SHADER_MODEL=60

# Inclure les warnings comme erreurs
dxc file.hlsl -T ps_6_0 -WX

# Désactiver les optimisations
dxc file.hlsl -T ps_6_0 -Od

# Afficher les includes
dxc file.hlsl -T ps_6_0 -Zi -Fd file.pdb --show-includes
```

#### Profiles par Shader Model

| Shader Model | Target | Description |
|--------------|--------|-------------|
| 5.0 | `ps_5_0`, `vs_5_0`, `cs_5_0` | DirectX 11 |
| 5.1 | `ps_5_1`, `vs_5_1`, `cs_5_1` | DirectX 11.1 |
| 6.0 | `ps_6_0`, `vs_6_0`, `cs_6_0` | DirectX 12 (recommandé) |
| 6.1 | `ps_6_1`, `vs_6_1`, `cs_6_1` | DirectX 12 Ultimate |
| 6.2 | `ps_6_2`, `vs_6_2`, `cs_6_2` | DirectX 12 Ultimate |
| 6.3 | `ps_6_3`, `vs_6_3`, `cs_6_3` | DirectX 12 Ultimate |
| 6.4 | `ps_6_4`, `vs_6_4`, `cs_6_4` | DirectX 12 Ultimate |
| 6.5 | `ps_6_5`, `vs_6_5`, `cs_6_5` | DirectX 12 Ultimate |
| 6.6 | `ps_6_6`, `vs_6_6`, `cs_6_6` | DirectX 12 Ultimate |
| 6.7 | `ps_6_7`, `vs_6_7`, `cs_6_7` | DirectX 12 Ultimate |

### Utilisation FXC (Legacy)

#### Validation basique
```powershell
# Compiler comme Pixel Shader 5.0
fxc file.hlsl /T ps_5_0 /Fo file.obj

# Compiler comme Vertex Shader 5.0
fxc file.hlsl /T vs_5_0 /Fo file.obj

# Compiler comme Compute Shader 5.0
fxc file.hlsl /T cs_5_0 /Fo file.obj
```

#### Options utiles
```powershell
# Afficher la version
fxc /?

# Valider sans générer de sortie
fxc file.hlsl /T ps_5_0 /Vn file_validation

# Générer le code assembly
fxc file.hlsl /T ps_5_0 /Fc file.asm

# Désactiver les optimisations
fxc file.hlsl /T ps_5_0 /Od

# Niveau de warning
fxc file.hlsl /T ps_5_0 /WX
```

### Sortie

- **Succès (DXC)** : Code de sortie 0, fichier `.dxil` généré
- **Échec (DXC)** : Code de sortie non-zero, messages d'erreur détaillés
- **Succès (FXC)** : Code de sortie 0, fichier `.obj` généré
- **Échec (FXC)** : Code de sortie non-zero, messages d'erreur

```
Example DXC error output:
file.hlsl(12,45): error C0000: syntax error: unexpected token ';'
file.hlsl(12,45): note: did you mean ','?

Example FXC error output:
file.hlsl(12,45): error X3000: syntax error: unexpected token ';'
```

### Intégration Kotlin

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/validator/HlslValidator.kt

package dev.gfxrs.naga.test.validator

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Wrapper for DXC and FXC validators
 */
class HlslValidator : BackendValidator {
    
    override val name: String = "DXC/FXC Validator"
    override val backendType: BackendType = BackendType.HLSL
    
    private var useDxc: Boolean = true
    private var dxcExecutable: String? = null
    private var fxcExecutable: String? = null
    
    init {
        detectExecutables()
    }
    
    private fun detectExecutables() {
        // Try DXC first
        val dxcCandidates = listOf(
            "dxc",
            "dxc.exe",
            "C:/Program Files (x86)/Windows Kits/10/bin/10.0.22621.0/x64/dxc.exe",
            "C:/Program Files (x86)/Microsoft DirectX Shader Compiler/dxc.exe"
        )
        
        for (candidate in dxcCandidates) {
            val file = File(candidate)
            if (file.exists()) {
                dxcExecutable = candidate
                return
            }
        }
        
        // Try FXC
        val fxcCandidates = listOf(
            "fxc",
            "fxc.exe",
            "C:/Program Files (x86)/Windows Kits/10/bin/10.0.22621.0/x64/fxc.exe"
        )
        
        for (candidate in fxcCandidates) {
            val file = File(candidate)
            if (file.exists()) {
                fxcExecutable = candidate
                useDxc = false
                return
            }
        }
        
        error("Neither DXC nor FXC found. Please install the Windows SDK or DirectX Shader Compiler.")
    }
    
    /**
     * Validate HLSL code
     * @param code HLSL source code
     * @param target Shader target (e.g., "ps_6_0", "vs_6_0")
     * @return ValidationResult
     */
    override fun validate(
        code: String,
        target: String?,
        stage: ShaderStage?
    ): ValidationResult {
        val tempFile = createTempFile(suffix = ".hlsl")
        try {
            tempFile.writeText(code)
            
            val command = if (useDxc) {
                buildDxcCommand(tempFile, target, stage)
            } else {
                buildFxcCommand(tempFile, target, stage)
            }
            
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()
            
            return ValidationResult(
                success = exitCode == 0,
                output = output,
                exitCode = exitCode,
                command = command.joinToString(" ")
            )
            
        } finally {
            tempFile.delete()
        }
    }
    
    private fun buildDxcCommand(
        file: File,
        target: String?,
        stage: ShaderStage?
    ): List<String> {
        val shaderTarget = target ?: when (stage) {
            ShaderStage.VERTEX -> "vs_6_0"
            ShaderStage.FRAGMENT -> "ps_6_0"
            ShaderStage.COMPUTE -> "cs_6_0"
            null -> "ps_6_0" // Default to pixel shader
        }
        
        return listOf(
            dxcExecutable!!,
            file.absolutePath,
            "-T", shaderTarget,
            "-Zs", // Validate only (no output)
            "-WX"  // Treat warnings as errors
        )
    }
    
    private fun buildFxcCommand(
        file: File,
        target: String?,
        stage: ShaderStage?
    ): List<String> {
        val shaderTarget = target ?: when (stage) {
            ShaderStage.VERTEX -> "vs_5_0"
            ShaderStage.FRAGMENT -> "ps_5_0"
            ShaderStage.COMPUTE -> "cs_5_0"
            null -> "ps_5_0" // Default to pixel shader 5.0
        }
        
        return listOf(
            fxcExecutable!!,
            file.absolutePath,
            "/T", shaderTarget,
            "/Vn", "${file.name}_validation", // Validate only (no output file)
            "/WX"  // Treat warnings as errors
        )
    }
    
    companion object {
        fun isAvailable(): Boolean = try {
            HlslValidator()
            true
        } catch (e: Exception) {
            false
        }
        
        fun isDxcAvailable(): Boolean = try {
            val validator = HlslValidator()
            validator.useDxc && validator.dxcExecutable != null
        } catch (e: Exception) {
            false
        }
        
        fun isFxcAvailable(): Boolean = try {
            val validator = HlslValidator()
            !validator.useDxc && validator.fxcExecutable != null
        } catch (e: Exception) {
            false
        }
    }
}
```

---

## Intégration Kotlin

### Interface Commune

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/validator/BackendValidator.kt

package dev.gfxrs.naga.test.validator

import java.io.File

/**
 * Common interface for backend validators
 */
interface BackendValidator {
    
    val name: String
    val backendType: BackendType
    
    /**
     * Validate the generated code
     * @param code Source code to validate
     * @param target Target specification (version, shader model, etc.)
     * @param stage Shader stage (optional)
     * @return ValidationResult with success/failure info
     */
    fun validate(
        code: String,
        target: String? = null,
        stage: ShaderStage? = null
    ): ValidationResult
}

/**
 * Result of a validation attempt
 */
data class ValidationResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int,
    val command: String
) {
    val isFailure: Boolean get() = !success
    
    override fun toString(): String = buildString {
        appendLine("Validation: ${if (success) "SUCCESS" else "FAILURE"}")
        appendLine("Command: $command")
        appendLine("Exit Code: $exitCode")
        if (output.isNotBlank()) {
            appendLine("Output:")
            appendLine(output)
        }
    }
}

/**
 * Shader stages
 */
enum class ShaderStage {
    VERTEX, FRAGMENT, COMPUTE
}

/**
 * Backend types
 */
enum class BackendType {
    IR, SPIRV, GLSL, MSL, HLSL, WGSL
}

/**
 * Validator factory
 */
object ValidatorFactory {
    
    private val validators: MutableMap<BackendType, BackendValidator> = mutableMapOf()
    
    init {
        // Register available validators
        if (GlslValidator.isAvailable()) {
            validators[BackendType.GLSL] = GlslValidator()
        }
        
        if (MetalValidator.isAvailable()) {
            validators[BackendType.MSL] = MetalValidator()
        }
        
        if (HlslValidator.isAvailable()) {
            validators[BackendType.HLSL] = HlslValidator()
        }
        
        // SPIR-V validator
        if (SpirvValidator.isAvailable()) {
            validators[BackendType.SPIRV] = SpirvValidator()
        }
    }
    
    /**
     * Get validator for backend type
     */
    fun getValidator(backendType: BackendType): BackendValidator? {
        return validators[backendType]
    }
    
    /**
     * Get all available validators
     */
    fun getAllValidators(): Map<BackendType, BackendValidator> {
        return validators.toMap()
    }
    
    /**
     * Check if validator is available for backend type
     */
    fun isAvailable(backendType: BackendType): Boolean {
        return validators.containsKey(backendType)
    }
}

/**
 * SPIR-V validator wrapper
 */
class SpirvValidator : BackendValidator {
    override val name: String = "spirv-val"
    override val backendType: BackendType = BackendType.SPIRV
    
    private var executable: String = findExecutable()
    
    override fun validate(
        code: String,
        target: String?,
        stage: ShaderStage?
    ): ValidationResult {
        val tempFile = File.createTempFile("spirv", ".spv")
        try {
            tempFile.writeBytes(code.toByteArray())
            
            val command = listOf(executable, tempFile.absolutePath)
            
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()
            
            return ValidationResult(
                success = exitCode == 0,
                output = output,
                exitCode = exitCode,
                command = command.joinToString(" ")
            )
        } finally {
            tempFile.delete()
        }
    }
    
    private fun findExecutable(): String {
        val candidates = listOf(
            "spirv-val",
            "spirv-val.exe",
            "/usr/local/bin/spirv-val",
            "/usr/bin/spirv-val"
        )
        
        for (candidate in candidates) {
            val file = File(candidate)
            if (file.exists() && file.canExecute()) {
                return candidate
            }
        }
        
        error("spirv-val not found. Please install SPIRV-Tools from https://github.com/KhronosGroup/SPIRV-Tools")
    }
    
    companion object {
        fun isAvailable(): Boolean = try {
            SpirvValidator().findExecutable()
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

### Tests d'Intégration

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/validator/NativeValidatorTests.kt

package dev.gfxrs.naga.test.validator

import dev.gfxrs.naga.backends.msl.writeMsl
import dev.gfxrs.naga.backends.hlsl.writeHlsl
import dev.gfxrs.naga.backends.glsl.writeGlsl
import dev.gfxrs.naga.frontends.wgsl.parseWgsl
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest

class NativeValidatorTests {
    
    @TestFactory
    @DisplayName("MSL Validation")
    fun mslValidationTests(): List<org.junit.jupiter.api.DynamicTest> {
        assumeTrue(ValidatorFactory.isAvailable(BackendType.MSL)) {
            "Metal Compiler not available, skipping MSL validation tests"
        }
        
        val validator = ValidatorFactory.getValidator(BackendType.MSL)!!
        
        val testCases = listOf(
            "simple-compute" to """
                @compute @workgroup_size(8, 8, 1)
                fn main() {
                    // Simple compute shader
                }
            """,
            "vertex-shader" to """
                struct VertexInput {
                    @location(0) position: vec4<f32>,
                };
                
                @vertex
                fn main(in: VertexInput) -> @builtin(position) vec4<f32> {
                    return in.position;
                }
            """
        )
        
        return testCases.map { (name, wgsl) ->
            dynamicTest(name) {
                val module = parseWgsl(wgsl).getOrThrow()
                val msl = writeMsl(module)
                
                val result = validator.validate(msl)
                
                if (!result.success) {
                    println("MSL Validation failed:")
                    println(result.output)
                }
                
                assert(result.success) { "MSL validation failed: ${result.output}" }
            }
        }
    }
    
    @TestFactory
    @DisplayName("GLSL Validation")
    fun glslValidationTests(): List<org.junit.jupiter.api.DynamicTest> {
        assumeTrue(ValidatorFactory.isAvailable(BackendType.GLSL)) {
            "glslangValidator not available, skipping GLSL validation tests"
        }
        
        val validator = ValidatorFactory.getValidator(BackendType.GLSL)!!
        
        val testCases = listOf(
            "simple-fragment" to """
                @fragment
                fn main() -> @location(0) vec4<f32> {
                    return vec4<f32>(1.0, 0.0, 0.0, 1.0);
                }
            """
        )
        
        return testCases.map { (name, wgsl) ->
            dynamicTest(name) {
                val module = parseWgsl(wgsl).getOrThrow()
                val glsl = writeGlsl(module)
                
                val result = validator.validate(glsl, target = "450")
                
                if (!result.success) {
                    println("GLSL Validation failed:")
                    println(result.output)
                }
                
                assert(result.success) { "GLSL validation failed: ${result.output}" }
            }
        }
    }
    
    @TestFactory
    @DisplayName("HLSL Validation")
    fun hlslValidationTests(): List<org.junit.jupiter.api.DynamicTest> {
        assumeTrue(ValidatorFactory.isAvailable(BackendType.HLSL)) {
            "DXC/FXC not available, skipping HLSL validation tests"
        }
        
        val validator = ValidatorFactory.getValidator(BackendType.HLSL)!!
        
        val testCases = listOf(
            "simple-pixel" to """
                @fragment
                fn main() -> @location(0) vec4<f32> {
                    return vec4<f32>(1.0, 0.0, 0.0, 1.0);
                }
            """
        )
        
        return testCases.map { (name, wgsl) ->
            dynamicTest(name) {
                val module = parseWgsl(wgsl).getOrThrow()
                val hlsl = writeHlsl(module)
                
                val result = validator.validate(hlsl, target = "ps_6_0")
                
                if (!result.success) {
                    println("HLSL Validation failed:")
                    println(result.output)
                }
                
                assert(result.success) { "HLSL validation failed: ${result.output}" }
            }
        }
    }
    
    @Test
    @DisplayName("Validator Availability")
    fun testValidatorAvailability() {
        println("\nAvailable validators:")
        ValidatorFactory.getAllValidators().forEach { (backend, validator) ->
            println("  - $backend: ${validator.name}")
        }
    }
}
```

---

## Configuration CI

### GitHub Actions (macOS)

```yaml
# .github/workflows/validation-macos.yml
name: Native Validation (macOS)

on: [push, pull_request]

jobs:
  validation:
    runs-on: macos-13
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Install glslangValidator
        run: brew install glslang
      
      - name: Run Validation Tests
        run: ./gradlew test --tests "dev.gfxrs.naga.test.validator.*"
        env:
          # Metal compiler is pre-installed on macOS runners
          VULKAN_SDK: /usr/local/share/vulkan
```

### GitHub Actions (Windows)

```yaml
# .github/workflows/validation-windows.yml
name: Native Validation (Windows)

on: [push, pull_request]

jobs:
  validation:
    runs-on: windows-2022
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Install Vulkan SDK
        uses: humbletim/setup-vulkan-sdk@v1.2.0
        with:
          vulkan-sdk-version: '1.3.268.0'
      
      - name: Run Validation Tests
        run: .\gradlew test --tests "dev.gfxrs.naga.test.validator.*"
        env:
          VULKAN_SDK: ${{ env.VULKAN_SDK }}
```

### GitHub Actions (Linux)

```yaml
# .github/workflows/validation-linux.yml
name: Native Validation (Linux)

on: [push, pull_request]

jobs:
  validation:
    runs-on: ubuntu-22.04
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Install Vulkan Tools
        run: |
          sudo apt-get update
          sudo apt-get install -y vulkan-tools glslang-tools spirv-tools
      
      - name: Run Validation Tests
        run: ./gradlew test --tests "dev.gfxrs.naga.test.validator.*"
```

---

## Gestion des Erreurs

### Classification des Erreurs

| Type | Exemple | Action |
|------|---------|--------|
| **Erreur de syntaxe** | `error: syntax error` | Corriger le générateur de code |
| **Type non supporté** | `error: unsupported type` | Implémenter le type manquant |
| **Fonction intrinseque manquante** | `error: unknown builtin` | Implémenter la fonction builtin |
| **Version non supportée** | `error: version not supported` | Mettre à jour la version cible |
| **Extension manquante** | `error: extension required` | Activer l'extension ou trouver une alternative |

### Stratégie de Résolution

1. **Reproduire localement** : Exécuter le validateur avec la même commande
2. **Analyser l'erreur** : Comprendre la cause racine
3. **Corriger dans WebGPU-KTypes** : Modifier le backend concerné
4. **Mettre à jour les golden files** : Si le changement est intentionnel
5. **Valider la correction** : Vérifier que l'erreur est résolue

### Journalisation des Échecs

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/validator/ValidationLogger.kt

package dev.gfxrs.naga.test.validator

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Logger for validation failures
 */
object ValidationLogger {
    
    private val logDir = File("build/reports/validation")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    
    init {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }
    
    /**
     * Log a validation failure
     */
    fun logFailure(
        backendType: BackendType,
        inputFile: String,
        result: ValidationResult,
        generatedCode: String
    ) {
        val timestamp = LocalDateTime.now().format(formatter)
        val logFile = File(logDir, "validation-failure_${backendType}_$timestamp.log")
        
        logFile.writeText("""
Validation Failure Report
========================

Timestamp: $timestamp
Backend: ${backendType.name}
Input File: $inputFile
Command: ${result.command}
Exit Code: ${result.exitCode}

Error Output:
-------------
${result.output}

Generated Code:
---------------
$generatedCode

========================
        """.trimIndent())
        
        println("Validation failure logged to: ${logFile.absolutePath}")
    }
    
    /**
     * Log all available validators
     */
    fun logAvailableValidators() {
        val logFile = File(logDir, "available-validators.log")
        
        logFile.writeText("""
Available Validators
===================

${ValidatorFactory.getAllValidators().entries.joinToString("\n") { (backend, validator) ->
            "- $backend: ${validator.name}"
        }}

===================
        """.trimIndent())
    }
}
```

---

## Résumé des Commandes

| Validateur | Commande | Installation |
|-----------|----------|--------------|
| spirv-val | `spirv-val file.spv` | SPIRV-Tools |
| glslangValidator | `glslangValidator -V file.glsl` | Vulkan SDK / glslang |
| Metal Compiler | `xcrun -sdk macosx metal file.msl` | Xcode |
| DXC | `dxc file.hlsl -T ps_6_0 -Zs` | Windows SDK |
| FXC | `fxc file.hlsl /T ps_5_0 /Vn name` | Windows SDK (legacy) |

---

## Références

- [SPIRV-Tools](https://github.com/KhronosGroup/SPIRV-Tools)
- [glslang](https://github.com/KhronosGroup/glslang)
- [Metal Shading Language Guide](https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf)
- [DirectX Shader Compiler (DXC)](https://github.com/microsoft/DirectXShaderCompiler)
- [FXC Documentation](https://docs.microsoft.com/en-us/windows/win32/dxtecharts/fxc)
- [Vulkan Tools](https://vulkan.lunarg.com/doc/view/latest/windows/getting_started.html)
