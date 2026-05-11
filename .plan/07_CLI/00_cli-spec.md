# Spécification CLI

## Sommaire

1. [Overview](#overview)
2. [Objectifs](#objectifs)
3. [Architecture](#architecture)
4. [Fonctionnalités](#fonctionnalités)
5. [Interface Utilisateur](#interface-utilisateur)
6. [Arguments et Options](#arguments-et-options)
7. [Sortie et Formatage](#sortie-et-formatage)
8. [Gestion des Erreurs](#gestion-des-erreurs)
9. [Exemples d'Utilisation](#exemples-dutilisation)
10. [Intégration avec le Build](#intégration-avec-le-build)

---

## Overview

### Description

Le **CLI (Command Line Interface)** de WebGPU-KTypes est un outil en ligne de commande qui permet de manipuler les shaders WGSL :
- **Parsing** : Valider et parser le code WGSL
- **Conversion** : Convertir WGSL vers d'autres langages (MSL, HLSL, GLSL)
- **Inspection** : Afficher la représentation IR d'un shader
- **Validation** : Valider avec les compilateurs natifs
- **Batch Processing** : Traiter plusieurs fichiers en une seule commande

### Diagramme d'Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        WebGPU-KTypes CLI                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────┐ │
│  │   Argument  │    │   Command   │    │    Output   │    │  Error   │ │
│  │   Parser    │───▶│   Handler   │───▶│   Format   │───▶│ Handling │ │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────┘ │
│           ▲                  ▲                  ▲                  ▲    │
│           │                  │                  │                  │    │
│  ┌────────┴────────┐ ┌──────┴──────┐ ┌──────┴──────┐ ┌─────┴──────┐ │
│  │  CLI Arguments   │ │  Processing │ │  Formatting │ │  Logging   │ │
│  │  (raw strings)   │ │   Pipeline  │ │  (colors,  │ │  (stderr)  │ │
│  └─────────────────┘ │             │ │   JSON,   │ └────────────┘ │
│                      └──────┬──────┘ │   etc.)    │                │
│                             │         └─────────────┘                │
│                             │                                          │
│                      ┌──────▼──────┐                                  │
│                      │   Backend    │ ◄───── wgsl:msl, wgsl:hlsl,    │
│                      │   Registry   │       wgsl:glsl, wgsl:wgsl    │
│                      └─────────────┘                                  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Flux de Traitement

```
1. Parsing des arguments de ligne de commande
   │
2. Validation des arguments
   │
3. Sélection de la commande
   │
4. Exécution de la commande
   │   ├─ Parsing WGSL → IR (si nécessaire)
   │   ├─ Transformation IR (si nécessaire)
   │   └─ Génération de code backend
   │
5. Formatage de la sortie
   │
6. Affichage du résultat ou des erreurs
```

---

## Objectifs

### Objectifs Principaux

1. **Simplicité** : Interface intuitive et facile à utiliser
2. **Flexibilité** : Support de multiples formats d'entrée/sortie
3. **Robustesse** : Gestion graceuse des erreurs
4. **Performance** : Traitement rapide des fichiers
5. **Intégrabilité** : Utilisable dans des scripts et pipelines CI

### Cas d'Usage

| Cas d'Usage | Commande | Description |
|-------------|----------|-------------|
| Valider un shader | `wgpu-kt validate shader.wgsl` | Valider la syntaxe WGSL |
| Convertir en MSL | `wgpu-kt convert shader.wgsl -t msl` | Convertir WGSL → MSL |
| Convertir en HLSL | `wgpu-kt convert shader.wgsl -t hlsl` | Convertir WGSL → HLSL |
| Convertir en GLSL | `wgpu-kt convert shader.wgsl -t glsl` | Convertir WGSL → GLSL |
| Afficher l'IR | `wgpu-kt ir shader.wgsl` | Afficher la représentation IR |
| Traiter un dossier | `wgpu-kt batch ./shaders/ -t msl -o ./out/` | Convertir tous les shaders |
| Valider avec Metal | `wgpu-kt validate shader.wgsl --native metal` | Validation native |

### Cibles de Performance

| Opération | Temps Cible (fichier moyen) | Temps Cible (100 fichiers) |
|-----------|----------------------------|---------------------------|
| Parsing WGSL | < 50ms | < 5s |
| Génération MSL | < 50ms | < 5s |
| Génération HLSL | < 50ms | < 5s |
| Génération GLSL | < 50ms | < 5s |
| Validation native | < 200ms (appel externe) | < 20s |
| Round-trip | < 100ms | < 10s |

---

## Architecture

### Structure du Projet

```
wgsl:cli/
├── build.gradle.kts                    # Configuration Gradle du module CLI
├── src/
│   └── main/
│       └── kotlin/
│           └── dev/gfxrs/naga/cli/
│               ├── Main.kt            # Point d'entrée principal
│               ├── CliApp.kt          # Application CLI principale
│               ├── command/           # Commandes
│               │   ├── Command.kt     # Interface de base pour les commandes
│               │   ├── ConvertCommand.kt
│               │   ├── ValidateCommand.kt
│               │   ├── IrCommand.kt
│               │   ├── BatchCommand.kt
│               │   └── InfoCommand.kt
│               ├── config/            # Configuration
│               │   ├── CliConfig.kt
│               │   └── OutputConfig.kt
│               ├── parser/            # Parsing des arguments
│               │   ├── ArgumentParser.kt
│               │   ├── CommandParser.kt
│               │   └── OptionParser.kt
│               ├── output/            # Formatage de sortie
│               │   ├── OutputFormatter.kt
│               │   ├── JsonFormatter.kt
│               │   ├── TextFormatter.kt
│               │   └── ColorFormatter.kt
│               ├── backend/           # Registre des backends
│               │   ├── BackendRegistry.kt
│               │   └── BackendFactory.kt
│               ├── error/             # Gestion des erreurs
│               │   ├── CliError.kt
│               │   ├── ErrorHandler.kt
│               │   └── ErrorFormatter.kt
│               └── util/              # Utilitaires
│                   ├── FileUtils.kt
│                   ├── PathUtils.kt
│                   └── Version.kt
└── resources/
    └── version.properties          # Version de l'application
```

### Diagramme des Classes

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                CliApp                                     │
├─────────────────────────────────────────────────────────────────────────┤
│ + main(args: Array<String>): Unit                                          │
│ + run(): Int                                                               │
│ - parseArguments(args: Array<String>): Result<CliConfig>                 │
│ - executeCommand(config: CliConfig): Result<Unit>                        │
│ - handleError(error: CliError): Int                                       │
└────────────────────────┬──────────────────────────────────────────────┘
                         │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
┌────────▼──────────┐ ┌────────▼──────────┐ ┌────────▼──────────┐
│  ArgumentParser   │ │   CommandParser   │ │  ErrorHandler     │
│                  │ │                  │ │                  │
│ + parse(args):   │ │ + parse(config): │ │ + handle(error): │
│   Result<Config> │ │   Result<Command> │ │   Int            │
└───────────────────┘ └───────────────────┘ └───────────────────┘
                         │
         ┌───────────────────┴───────────────────┐
         │
┌────────▼──────────┐
│     Command       │
│ (Interface)       │
├───────────────────┤
│ + name: String    │
│ + description:    │
│   String          │
│ + execute(config: │
│   CliConfig):     │
│   Result<Unit>    │
└────────┬──────────┘
         │
   ┌─────┴─────┬─────┴─────┬─────┴─────┬─────┴─────┐
   │           │           │           │           │
┌──▼──┐ ┌──▼──┐ ┌──▼──┐ ┌──▼──┐ ┌──▼──┐
│Convert│ │Validate│ │  IR   │ │ Batch │ │ Info  │
│Command│ │Command│ │Command│ │Command│ │Command│
└───────┘ └───────┘ └───────┘ └───────┘ └───────┘
         │           │           │           │
         ▼           ▼           ▼           ▼
┌─────────────────────────────────────────────────────────┐
│                    BackendRegistry                          │
│  + getBackend(type: BackendType): BackendWriter          │
│  + getAllBackends(): List<BackendType>                   │
│  + isAvailable(type: BackendType): Boolean                │
└─────────────────────────────────────────────────────────┘
```

---

## Fonctionnalités

### Commandes Principales

| Commande | Description | Alias |
|----------|-------------|-------|
| `convert` | Convertir WGSL vers un autre langage | `c`, `conv` |
| `validate` | Valider la syntaxe WGSL | `v`, `val` |
| `ir` | Afficher la représentation IR | `i` |
| `batch` | Traiter plusieurs fichiers | `b` |
| `info` | Afficher les informations de version | `-v`, `--version` |
| `help` | Afficher l'aide | `-h`, `--help` |

### Backends Supportés

| Backend | Type | Description | Plateforme |
|---------|------|-------------|-----------|
| WGSL | `wgsl` | WebGPU Shading Language | Toutes |
| MSL | `msl` | Metal Shading Language | macOS/iOS |
| HLSL | `hlsl` | High-Level Shading Language | Windows |
| GLSL | `glsl` | OpenGL Shading Language | Toutes |
| SPIR-V | `spv` | SPIR-V Binary | Toutes (optionnel) |

### Formats de Sortie

| Format | Description | Extension | Utilisation |
|--------|-------------|-----------|------------|
| WGSL | WebGPU Shading Language | `.wgsl` | Sortie par défaut pour convert |
| MSL | Metal Shading Language | `.msl` | Sortie par défaut pour MSL |
| HLSL | High-Level Shading Language | `.hlsl` | Sortie par défaut pour HLSL |
| GLSL | OpenGL Shading Language | `.glsl` | Sortie par défaut pour GLSL |
| JSON | JSON (pour IR) | `.json` | Sortie pour commande `ir` |
| Pretty | WGSL formaté | `.wgsl` | Avec option `--pretty` |
| Minified | WGSL minifié | `.wgsl` | Avec option `--minify` |

---

## Interface Utilisateur

### Structure de la Commande

```
wgpu-kt [OPTIONS] <COMMAND> [ARGS]
```

### Aide Générale

```bash
$ wgpu-kt --help

WebGPU-KTypes CLI - WebGPU Shader Compiler

Usage: wgpu-kt [OPTIONS] <COMMAND>

Options:
  -h, --help           Show this help message
  -v, --version        Show version information
  --verbose, -V        Enable verbose output
  --quiet, -q          Suppress non-error output
  --color              Enable colored output (default: auto)
  --no-color           Disable colored output

Commands:
  convert, c, conv    Convert WGSL to another shading language
  validate, v, val    Validate WGSL syntax
  ir, i               Show IR representation of WGSL
  batch, b            Batch process multiple files
  info                Show version and build information

Run 'wgpu-kt <COMMAND> --help' for more information on a command.
```

### Aide pour une Commande

```bash
$ wgpu-kt convert --help

Usage: wgpu-kt convert [OPTIONS] <INPUT>... 

Convert WGSL shaders to another shading language.

Arguments:
  <INPUT>...    Input WGSL file(s) or directory

Options:
  -t, --target <TARGET>      Target language: msl, hlsl, glsl, wgsl (default: msl)
  -o, --output <PATH>       Output file or directory (default: stdout)
  --stdout                  Output to stdout (default)
  --pretty                 Pretty-print the output
  --minify                  Minify the output
  --overwrite              Overwrite existing files
  --create-dir             Create output directories if they don't exist
  --validate               Validate the output with native compiler
  --dry-run                Don't write files, just show what would be done
  
  Backend-specific options:
    --msl-version <VERSION>    MSL version (default: latest)
    --hlsl-version <VERSION>   HLSL shader model (default: ps_6_0)
    --glsl-version <VERSION>   GLSL version (default: 450)

Examples:
  wgpu-kt convert shader.wgsl -t msl -o shader.msl
  wgpu-kt convert shader.wgsl -t hlsl --hlsl-version vs_6_0
  wgpu-kt convert ./shaders/ -t msl -o ./out/
  wgpu-kt convert shader.wgsl -t wgsl --pretty
```

---

## Arguments et Options

### Options Globales

| Option | Alias | Description | Type | Valeur par défaut |
|--------|-------|-------------|------|------------------|
| `--help` | `-h` | Afficher l'aide | Flag | - |
| `--version` | `-v` | Afficher la version | Flag | - |
| `--verbose` | `-V` | Mode verbeux | Flag | false |
| `--quiet` | `-q` | Mode silencieux (erreurs seulement) | Flag | false |
| `--color` | - | Activer les couleurs | Flag | auto |
| `--no-color` | - | Désactiver les couleurs | Flag | false |
| `--config` | `-c` | Fichier de configuration | Path | - |

### Commande `convert`

**Description** : Convertir un ou plusieurs fichiers WGSL vers un autre langage.

**Usage** : `wgpu-kt convert [OPTIONS] <INPUT>...`

#### Arguments

| Argument | Description | Obligatoire | Multiple |
|----------|-------------|------------|----------|
| `<INPUT>...` | Fichier(s) WGSL ou répertoire d'entrée | ✅ | ✅ |

#### Options

| Option | Alias | Description | Type | Valeur par défaut | Exemple |
|--------|-------|-------------|------|------------------|---------|
| `--target` | `-t` | Langage cible | `msl`, `hlsl`, `glsl`, `wgsl` | `msl` | `-t hlsl` |
| `--output` | `-o` | Fichier ou répertoire de sortie | Path | stdout | `-o out.msl` |
| `--stdout` | - | Sortie vers stdout | Flag | true (si pas -o) | `--stdout` |
| `--pretty` | - | Formater la sortie | Flag | false | `--pretty` |
| `--minify` | - | Minifier la sortie | Flag | false | `--minify` |
| `--overwrite` | - | Écraser les fichiers existants | Flag | false | `--overwrite` |
| `--create-dir` | - | Créer les répertoires de sortie | Flag | true | `--create-dir` |
| `--validate` | - | Valider avec le compilateur natif | Flag | false | `--validate` |
| `--dry-run` | - | Mode test (ne pas écrire) | Flag | false | `--dry-run` |
| `--msl-version` | - | Version MSL | String | `latest` | `--msl-version ios` |
| `--hlsl-version` | - | Shader Model HLSL | String | `ps_6_0` | `--hlsl-version vs_6_0` |
| `--glsl-version` | - | Version GLSL | String | `450` | `--glsl-version 460` |

#### Exemples

```bash
# Convertir un fichier WGSL en MSL
wgpu-kt convert shader.wgsl -t msl -o shader.msl

# Convertir en HLSL avec version spécifique
wgpu-kt convert shader.wgsl -t hlsl --hlsl-version vs_6_0 -o shader.hlsl

# Convertir tous les fichiers d'un répertoire
wgpu-kt convert ./shaders/ -t msl -o ./out/

# Convertir en WGSL formaté
wgpu-kt convert shader.wgsl -t wgsl --pretty -o shader-formatted.wgsl

# Convertir avec validation native (macOS)
wgpu-kt convert shader.wgsl -t msl --validate -o shader.msl

# Mode test (dry-run)
wgpu-kt convert ./shaders/ -t msl -o ./out/ --dry-run
```

### Commande `validate`

**Description** : Valider la syntaxe et la sémantique d'un ou plusieurs fichiers WGSL.

**Usage** : `wgpu-kt validate [OPTIONS] <INPUT>...`

#### Arguments

| Argument | Description | Obligatoire | Multiple |
|----------|-------------|------------|----------|
| `<INPUT>...` | Fichier(s) WGSL ou répertoire | ✅ | ✅ |

#### Options

| Option | Alias | Description | Type | Valeur par défaut |
|--------|-------|-------------|------|------------------|
| `--native` | - | Valider avec le compilateur natif | Flag | false |
| `--native-target` | - | Cible pour la validation native | `auto`, `metal`, `dxc`, `glslang` | `auto` |
| `--strict` | - | Mode strict (toutes les warnings = erreurs) | Flag | false |
| `--warnings` | `-w` | Afficher les warnings | Flag | true |
| `--no-warnings` | - | Ne pas afficher les warnings | Flag | false |

#### Exemples

```bash
# Valider un fichier WGSL
wgpu-kt validate shader.wgsl

# Valider avec validation native (macOS)
wgpu-kt validate shader.wgsl --native

# Valider avec validation native spécifique
wgpu-kt validate shader.wgsl --native --native-target glslang

# Valider tous les fichiers d'un répertoire
wgpu-kt validate ./shaders/

# Valider en mode strict
wgpu-kt validate shader.wgsl --strict
```

### Commande `ir`

**Description** : Afficher la représentation IR d'un fichier WGSL.

**Usage** : `wgpu-kt ir [OPTIONS] <INPUT>`

#### Arguments

| Argument | Description | Obligatoire |
|----------|-------------|------------|
| `<INPUT>` | Fichier WGSL | ✅ |

#### Options

| Option | Alias | Description | Type | Valeur par défaut |
|--------|-------|-------------|------|------------------|
| `--output` | `-o` | Fichier de sortie | Path | stdout |
| `--format` | `-f` | Format de sortie | `json`, `yaml`, `debug` | `json` |
| `--pretty` | - | Formater la sortie JSON | Flag | false |
| `--compact` | - | Sortie JSON compacte | Flag | false |
| `--module-info` | - | Afficher les informations du module | Flag | false |
| `--type-info` | - | Afficher les informations des types | Flag | false |

#### Exemples

```bash
# Afficher l'IR d'un shader
wgpu-kt ir shader.wgsl

# Sauvegarder l'IR dans un fichier JSON
wgpu-kt ir shader.wgsl -o shader.ir.json

# Afficher l'IR en format YAML
wgpu-kt ir shader.wgsl -f yaml

# Afficher l'IR avec informations du module
wgpu-kt ir shader.wgsl --module-info --type-info

# Afficher l'IR formaté
wgpu-kt ir shader.wgsl --pretty
```

### Commande `batch`

**Description** : Traiter plusieurs fichiers WGSL en batch.

**Usage** : `wgpu-kt batch [OPTIONS] <COMMAND> [ARGS...]`

#### Arguments

| Argument | Description | Obligatoire |
|----------|-------------|------------|
| `<COMMAND>` | Commande à exécuter en batch | ✅ |
| `[ARGS...]` | Arguments de la commande | ❌ |

#### Options

| Option | Alias | Description | Type | Valeur par défaut |
|--------|-------|-------------|------|------------------|
| `--input` | `-i` | Répertoire ou pattern d'entrée | Path | `.` |
| `--output` | `-o` | Répertoire de sortie | Path | `<input>` |
| `--pattern` | `-p` | Pattern pour filtrer les fichiers | String | `**/*.wgsl` |
| `--recursive` | `-r` | Recherche récursive | Flag | true |
| `--include` | - | Patterns à inclure | String | - |
| `--exclude` | - | Patterns à exclure | String | - |
| `--parallel` | - | Traitement parallèle | Int | 1 |
| `--workers` | `-w` | Nombre de workers | Int | Nombre de cœurs |
| `--continue-on-error` | - | Continuer sur erreur | Flag | false |
| `--summary` | - | Afficher un résumé | Flag | true |

#### Exemples

```bash
# Convertir tous les fichiers WGSL en MSL
wgpu-kt batch convert -t msl -i ./shaders/ -o ./out/

# Valider tous les fichiers WGSL
wgpu-kt batch validate -i ./shaders/

# Convertir avec pattern spécifique
wgpu-kt batch convert -t msl -i ./src/ -p **/shaders/*.wgsl

# Traitement parallèle avec 4 workers
wgpu-kt batch convert -t msl -i ./shaders/ -o ./out/ -w 4

# Continuer sur erreur
wgpu-kt batch convert -t hlsl -i ./shaders/ -o ./out/ --continue-on-error

# Sans résumé
wgpu-kt batch validate -i ./shaders/ --no-summary
```

### Commande `info`

**Description** : Afficher les informations de version et de build.

**Usage** : `wgpu-kt info [OPTIONS]`

#### Options

| Option | Alias | Description | Type | Valeur par défaut |
|--------|-------|-------------|------|------------------|
| `--json` | - | Sortie JSON | Flag | false |
| `--full` | - | Informations complètes | Flag | false |

#### Exemples

```bash
# Afficher la version
wgpu-kt info

# Afficher la version en JSON
wgpu-kt info --json

# Afficher toutes les informations
wgpu-kt info --full
```

---

## Sortie et Formatage

### Formatage des Couleurs

| Élément | Couleur (terminal) | Style |
|---------|-------------------|-------|
| Succès | Vert | Normal |
| Erreur | Rouge | Normal |
| Warning | Jaune | Normal |
| Info | Bleu | Normal |
| Debug | Cyan | Normal |
| Fichier | Vert clair | Normal |
| Répertoire | Bleu clair | Normal |
| Commande | Magenta | Normal |
| Option | Jaune | Normal |
| Valeur | Cyan | Normal |

### Exemples de Sortie

#### Sortie de Succès

```bash
$ wgpu-kt convert shader.wgsl -t msl -o shader.msl

✅ Success: shader.wgsl → shader.msl
   Time: 45ms
   Input:  1234 bytes
   Output: 1567 bytes
```

#### Sortie avec Warning

```bash
$ wgpu-kt convert shader.wgsl -t msl -o shader.msl

⚠️  Warning: Unsupported feature 'compute' in shader.wgsl:42
   Feature will be ignored

✅ Success: shader.wgsl → shader.msl (with warnings)
   Time: 45ms
   Warnings: 1
```

#### Sortie d'Erreur

```bash
$ wgpu-kt convert invalid.wgsl -t msl

❌ Error: Failed to parse invalid.wgsl
   Cause: Unexpected token '}' at line 15, column 8
   
   12 │     let x = 1 + 2;
   13 │     return x
   14 │ }
   15 │ }
      │ ^ unexpected token
   16 │ 
```

#### Sortie JSON

```json
{
  "success": false,
  "error": {
    "message": "Failed to parse invalid.wgsl",
    "cause": "Unexpected token '}' at line 15, column 8",
    "location": {
      "file": "invalid.wgsl",
      "line": 15,
      "column": 8
    },
    "type": "ParseError"
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "version": "0.1.0"
}
```

### Niveaux de Verbosité

| Niveau | Description | Affichage |
|--------|-------------|----------|
| `--quiet` | Mode silencieux | Erreurs seulement |
| Par défaut | Mode normal | Messages, warnings, erreurs |
| `--verbose` | Mode verbeux | Tout + debug info |
| `--debug` | Mode debug | Tout + stack traces |

---

## Gestion des Erreurs

### Hiérarchie des Erreurs

```
CliError (sealed class)
├── ParseError
│   ├── ArgumentParseError
│   ├── InvalidOptionError
│   └── MissingArgumentError
├── FileError
│   ├── FileNotFoundError
│   ├── FileReadError
│   ├── FileWriteError
│   └── PermissionError
├── ProcessingError
│   ├── ParseWgslError
│   ├── ValidationError
│   ├── GenerationError
│   └── BackendError
└── SystemError
    ├── UnsupportedPlatformError
    └── NativeCompilerError
```

### Codes de Sortie

| Code | Description | Exemple |
|------|-------------|---------|
| 0 | Succès | Commande exécutée avec succès |
| 1 | Erreur générale | Erreur de traitement |
| 2 | Erreur d'argument | Option invalide, argument manquant |
| 3 | Erreur de fichier | Fichier non trouvé, permission refusée |
| 4 | Erreur de parsing | WGSL invalide |
| 5 | Erreur de validation | Code généré invalide |
| 6 | Erreur de backend | Backend non disponible |

### Gestion des Erreurs

```kotlin
// src/main/kotlin/dev/gfxrs/naga/cli/error/CliError.kt

package io.ygdrasil.wgsl.cli.error

/**
 * Base class for all CLI errors.
 */
sealed class CliError(
    val message: String,
    val cause: Throwable? = null
) {
    /**
     * Get the exit code for this error.
     */
    abstract val exitCode: Int
    
    /**
     * Get a user-friendly error message.
     */
    fun getFormattedMessage(): String = message
}

/**
 * Error during argument parsing.
 */
data class ArgumentParseError(
    val argument: String,
    val reason: String
) : CliError(
    message = "Invalid argument '$argument': $reason",
    exitCode = 2
)

/**
 * Missing required argument.
 */
data class MissingArgumentError(
    val argument: String
) : CliError(
    message = "Missing required argument: $argument",
    exitCode = 2
)

/**
 * Invalid option provided.
 */
data class InvalidOptionError(
    val option: String,
    val reason: String
) : CliError(
    message = "Invalid option '$option': $reason",
    exitCode = 2
)

/**
 * File-related errors.
 */
sealed class FileError(
    message: String,
    cause: Throwable? = null
) : CliError(message, cause) {
    override val exitCode: Int = 3
}

/**
 * File not found.
 */
data class FileNotFoundError(
    val path: String
) : FileError("File not found: $path")

/**
 * Permission denied.
 */
data class PermissionError(
    val path: String
) : FileError("Permission denied: $path")

/**
 * Processing-related errors.
 */
sealed class ProcessingError(
    message: String,
    cause: Throwable? = null
) : CliError(message, cause) {
    override val exitCode: Int = 4
}

/**
 * WGSL parsing error.
 */
data class ParseWgslError(
    val file: String,
    val line: Int,
    val column: Int,
    val message: String
) : ProcessingError(
    message = "$file:$line:$column: $message",
    cause = null
)

/**
 * Backend error.
 */
data class BackendError(
    val backend: String,
    val message: String
) : ProcessingError(
    message = "Backend '$backend' error: $message"
) {
    override val exitCode: Int = 6
}

/**
 * System/environment errors.
 */
sealed class SystemError(
    message: String,
    cause: Throwable? = null
) : CliError(message, cause) {
    override val exitCode: Int = 6
}

/**
 * Native compiler not available.
 */
data class NativeCompilerError(
    val compiler: String
) : SystemError(
    message = "Native compiler '$compiler' not available. Please install it."
)
```

### Error Handler

```kotlin
// src/main/kotlin/dev/gfxrs/naga/cli/error/ErrorHandler.kt

package io.ygdrasil.wgsl.cli.error

import java.io.PrintStream

/**
 * Handles CLI errors and formats error messages.
 */
class ErrorHandler(
    private val output: PrintStream = System.err
) {
    
    /**
     * Handle an error and return the appropriate exit code.
     */
    fun handle(error: CliError, verbose: Boolean = false): Int {
        val message = if (verbose) {
            getVerboseMessage(error)
        } else {
            getFormattedMessage(error)
        }
        
        output.println(message)
        return error.exitCode
    }
    
    /**
     * Handle a generic throwable.
     */
    fun handle(throwable: Throwable, verbose: Boolean = false): Int {
        val message = if (verbose) {
            getVerboseStackTrace(throwable)
        } else {
            getFormattedThrowable(throwable)
        }
        
        output.println(message)
        return 1
    }
    
    private fun getFormattedMessage(error: CliError): String = buildString {
        val symbol = when (error.exitCode) {
            0 -> "✅"
            2 -> "❌"
            3 -> "❌"
            4 -> "❌"
            5 -> "❌"
            6 -> "⚠️"
            else -> "❌"
        }
        
        appendLine("$symbol  ${error.message}")
        
        when (error) {
            is ParseWgslError -> {
                appendLine()
                appendLine("   at ${error.file}:${error.line}:${error.column}")
            }
            is BackendError -> {
                appendLine()
                appendLine("   Backend: ${error.backend}")
            }
            is NativeCompilerError -> {
                appendLine()
                appendLine("   Compiler: ${error.compiler}")
                appendLine("   Install: https://gpuweb.github.io/gpuweb/wgsl/#implementation-notes")
            }
            else -> {}
        }
    }
    
    private fun getVerboseMessage(error: CliError): String = buildString {
        appendLine(getFormattedMessage(error))
        appendLine()
        
        when (error) {
            is ProcessingError -> {
                error.cause?.let { cause ->
                    appendLine("Cause:")
                    appendLine(cause.stackTraceToString().prependIndent("  "))
                }
            }
            else -> {
                error.cause?.let { cause ->
                    appendLine("Stack Trace:")
                    appendLine(cause.stackTraceToString().prependIndent("  "))
                }
            }
        }
    }
    
    private fun getFormattedThrowable(throwable: Throwable): String = buildString {
        appendLine("❌  Unexpected error: ${throwable.message}")
        appendLine()
        appendLine("   This is a bug. Please report it at:")
        appendLine("   https://github.com/gfx-rs/webgpu-ktypes/issues")
    }
    
    private fun getVerboseStackTrace(throwable: Throwable): String = buildString {
        appendLine(getFormattedThrowable(throwable))
        appendLine()
        appendLine("Stack Trace:")
        appendLine(throwable.stackTraceToString().prependIndent("  "))
    }
}
```

---

## Exemples d'Utilisation

### Exemples de Base

```bash
# Afficher l'aide
wgpu-kt --help

# Afficher la version
wgpu-kt --version

# Valider un shader
wgpu-kt validate simple.wgsl

# Convertir un shader en MSL
wgpu-kt convert simple.wgsl -t msl -o simple.msl

# Afficher l'IR d'un shader
wgpu-kt ir simple.wgsl
```

### Exemples Avancés

```bash
# Convertir tous les shaders d'un répertoire en HLSL
wgpu-kt batch convert -t hlsl -i ./shaders/ -o ./hlsl/

# Valider avec validation native (macOS)
wgpu-kt convert shader.wgsl -t msl --validate -o shader.msl

# Convertir avec version spécifique de HLSL
wgpu-kt convert shader.wgsl -t hlsl --hlsl-version ps_6_0 -o shader.ps

# Convertir en GLSL 460 (Vulkan)
wgpu-kt convert shader.wgsl -t glsl --glsl-version 460 -o shader.glsl

# Traiter avec pattern personnalisé
wgpu-kt batch convert -t msl -i ./src/ -p **/shaders/*.wgsl -o ./out/

# Traitement parallèle
wgpu-kt batch convert -t msl -i ./shaders/ -o ./out/ -w 4

# Continuer sur erreur et afficher résumé
wgpu-kt batch validate -i ./shaders/ --continue-on-error
```

### Exemples de Pipeline

```bash
# Pipeline de build : Valider tous les shaders
#!/bin/bash
set -e

echo "Validating all shaders..."
wgpu-kt batch validate -i ./src/shaders/ -r

# Pipeline de build : Convertir et valider
#!/bin/bash
set -e

SHADER_DIR="./src/shaders"
OUTPUT_DIR="./build/shaders"

echo "Converting shaders to MSL..."
wgpu-kt batch convert -t msl -i "$SHADER_DIR" -o "$OUTPUT_DIR" -r

echo "Validating MSL with Metal compiler..."
for file in "$OUTPUT_DIR"/*.msl; do
    xcrun -sdk macosx metal "$file" -o /dev/null --validate-only
done

echo "All shaders validated successfully!"

# Pipeline CI : Tester la conversion
#!/bin/bash
# Dans .github/workflows/test.yml

echo "Testing WGSL → MSL conversion..."
wgpu-kt convert test.wgsl -t msl -o test.msl

# Vérifier que le fichier existe
if [ ! -f test.msl ]; then
    echo "Error: MSL output not generated"
    exit 1
fi

# Valider avec Metal (macOS)
if [ "$RUNNER_OS" == "macOS" ]; then
    xcrun -sdk macosx metal test.msl -o /dev/null --validate-only
fi
```

---

## Intégration avec le Build

### Configuration Gradle

```kotlin
// wgsl:cli/build.gradle.kts

plugins {
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("io.ygdrasil.wgsl.cli.MainKt")
}

group = "dev.gfxrs"
version = "0.1.0-SNAPSHOT"

dependencies {
    implementation(project(":wgsl:core"))
    implementation(project(":wgsl:wgsl"))
    implementation(project(":wgsl:msl"))
    implementation(project(":wgsl:hlsl"))
    implementation(project(":wgsl:glsl"))
    
    // CLI-specific dependencies
    implementation("com.github.ajalt:clikt:4.2.0")
    implementation("org.fusesource.jansi:jansi:2.4.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
}

shadowJar {
    archiveBaseName.set("wgpu-kt")
    archiveClassifier.set("")
    archiveVersion.set("")
    
    // Exclusions pour éviter les conflits
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    
    // Relocalisation des dépendances
    relocate("com.github.ajalt.clikt", "io.ygdrasil.wgsl.cli.clikt")
}

tasks {
    shadowJar {
        dependsOn("build")
    }
    
    // Tâche pour builder un binaire natif avec GraalVM
    val nativeImage by creating {
        dependsOn("shadowJar")
        
        doLast {
            exec {
                commandLine(
                    "native-image",
                    "-jar", shadowJar.outputFile,
                    "-H:Name=wgpu-kt",
                    "-H:Classification=Application",
                    "-H:Description=WebGPU-KTypes CLI",
                    "-H:Version=${project.version}",
                    "--no-fallback"
                )
            }
        }
    }
}
```

### Création du Binaire

```bash
# Builder le JAR avec toutes les dépendances
./gradlew :wgsl:cli:shadowJar

# Le JAR est généré dans :
# wgsl:cli/build/libs/wgpu-kt.jar

# Exécuter le CLI
java -jar wgsl:cli/build/libs/wgpu-kt.jar --help

# Builder un binaire natif (GraalVM requis)
./gradlew :wgsl:cli:nativeImage

# Le binaire est généré dans :
# wgsl:cli/build/native-image/wgpu-kt

# Exécuter le binaire natif
./wgsl:cli/build/native-image/wgpu-kt --help
```

### Scripts de Distribution

```bash
# scripts/build-cli.sh
#!/bin/bash
set -e

echo "Building WebGPU-KTypes CLI..."

# Builder le JAR
./gradlew :wgsl:cli:shadowJar

# Créer le répertoire de distribution
mkdir -p dist

# Copier le JAR
cp wgsl:cli/build/libs/wgpu-kt.jar dist/

# Builder le binaire natif (si GraalVM disponible)
if command -v native-image &> /dev/null; then
    echo "Building native binary..."
    ./gradlew :wgsl:cli:nativeImage
    
    # Copier le binaire
    cp wgsl:cli/build/native-image/wgpu-kt dist/
    
    # Copier les binaires par plateforme
    mkdir -p dist/macos dist/linux dist/windows
    cp wgsl:cli/build/native-image/wgpu-kt dist/macos/wgpu-kt
    # ... builder pour autres plateformes
fi

# Créer un script de lancement
cat > dist/wgpu-kt << 'EOF'
#!/bin/sh
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
exec java -jar "$SCRIPT_DIR/wgpu-kt.jar" "$@"
EOF
chmod +x dist/wgpu-kt

# Créer une archive
tar czf webgpu-ktypes-cli.tar.gz -C dist .

echo "CLI build complete: dist/ and webgpu-ktypes-cli.tar.gz"
```

### Installation

```bash
# Installation manuelle
1. Télécharger l'archive depuis les releases
2. Décompresser : tar xzf webgpu-ktypes-cli.tar.gz
3. Ajouter au PATH :
   echo 'export PATH="$PATH:/chemin/vers/webgpu-ktypes-cli"' >> ~/.zshrc
   source ~/.zshrc

# Vérifier l'installation
wgpu-kt --version
```

### Mise à Jour

```bash
# Mise à jour via script
curl -L https://github.com/gfx-rs/webgpu-ktypes/releases/latest/download/webgpu-ktypes-cli.tar.gz \
  | tar xzf - -C /usr/local/bin --strip-components=1

# Ou via package manager (Homebrew)
brew update
brew upgrade webgpu-ktypes
```

---

## Résumé

### Checklist d'Implémentation

- [ ] Créer la structure du module `wgsl:cli`
- [ ] Implémenter `Main.kt` (point d'entrée)
- [ ] Implémenter `CliApp.kt` (application principale)
- [ ] Implémenter le système de commandes
  - [ ] `Command.kt` (interface)
  - [ ] `ConvertCommand.kt`
  - [ ] `ValidateCommand.kt`
  - [ ] `IrCommand.kt`
  - [ ] `BatchCommand.kt`
  - [ ] `InfoCommand.kt`
- [ ] Implémenter le parsing des arguments
  - [ ] `ArgumentParser.kt`
  - [ ] `CommandParser.kt`
  - [ ] `OptionParser.kt`
- [ ] Implémenter le formatage de sortie
  - [ ] `OutputFormatter.kt`
  - [ ] `JsonFormatter.kt`
  - [ ] `TextFormatter.kt`
  - [ ] `ColorFormatter.kt`
- [ ] Implémenter la gestion des erreurs
  - [ ] `CliError.kt` (hiérarchie)
  - [ ] `ErrorHandler.kt`
  - [ ] `ErrorFormatter.kt`
- [ ] Implémenter le registre des backends
  - [ ] `BackendRegistry.kt`
  - [ ] `BackendFactory.kt`
- [ ] Configurer Gradle (shadow plugin)
- [ ] Configurer les scripts de build
- [ ] Tester toutes les commandes

### Commandes à Implémenter

| Commande | Priorité | Complexité | État |
|----------|----------|------------|------|
| `help` | ⭐⭐⭐ | Faible | ⬜ |
| `info` / `version` | ⭐⭐⭐ | Faible | ⬜ |
| `convert` | ⭐⭐⭐⭐⭐ | Moyenne | ⬜ |
| `validate` | ⭐⭐⭐⭐ | Moyenne | ⬜ |
| `ir` | ⭐⭐⭐ | Moyenne | ⬜ |
| `batch` | ⭐⭐⭐⭐ | Élevée | ⬜ |

### Fonctionnalités à Implémenter

- [ ] Parsing des arguments de ligne de commande
- [ ] Validation des arguments
- [ ] Exécution des commandes
- [ ] Formatage de la sortie (texte, JSON)
- [ ] Gestion des couleurs (terminal)
- [ ] Gestion des erreurs
- [ ] Support des fichiers et répertoires
- [ ] Traitement en batch
- [ ] Traitement parallèle
- [ ] Intégration avec les backends
- [ ] Validation native
- [ ] Version et build info

---

## Références

- [Clikt (Kotlin CLI Library)](https://ajalt.github.io/clikt/)
- [Picocli (Alternative Java)](https://picocli.info/)
- [JCommander (Alternative Java)](https://jcommander.org/)
- [GraalVM Native Image](https://www.graalvm.org/latest/graalvm-as-a-platform/)
- [Shadow Plugin](https://imperceptiblethoughts.com/shadow/)
- [JANSI (Color Library)](https://github.com/fusesource/jansi)
- [Command Line Interface Guidelines](https://clig.dev/)
- [12 Factor App - CLI](https://12factor.net/cli)
- [Unix Philosophy](https://en.wikipedia.org/wiki/Unix_philosophy)

---

**Dernière mise à jour** : 2024-XX-XX
**Version** : 0.1.0-SNAPSHOT
**Responsable** : Équipe WebGPU-KTypes
