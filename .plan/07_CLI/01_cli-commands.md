# Commandes CLI - Documentation Technique

## Sommaire

1. [Structure des Commandes](#structure-des-commandes)
2. [Commande `convert`](#commande-convert)
3. [Commande `validate`](#commande-validate)
4. [Commande `ir`](#commande-ir)
5. [Commande `batch`](#commande-batch)
6. [Commande `info`](#commande-info)
7. [Commande `help`](#commande-help)

---

## Structure des Commandes

### Hiérarchie

```
Command (Interface)
├── ConvertCommand
├── ValidateCommand
├── IrCommand
├── BatchCommand
├── InfoCommand
└── HelpCommand
```

### Interface de Base

```kotlin
interface Command {
    val name: String
    val aliases: List<String>
    val description: String
    val category: String
    val help: String
    
    fun execute(context: CliContext): Result<Unit>
    fun validate(context: CliContext): Result<Unit>
    fun getOptions(): List<CliOption>
    fun getExamples(): List<String>
}
```

---

## Commande `convert`

### Description
Convertit un ou plusieurs fichiers WGSL vers un autre langage de shader.

### Options

| Option | Type | Défaut | Description |
|--------|------|--------|-------------|
| `-t, --target` | String | `msl` | Langage cible (msl, hlsl, glsl, wgsl) |
| `-o, --output` | Path | stdout | Fichier ou répertoire de sortie |
| `--stdout` | Flag | true | Sortie vers stdout |
| `--pretty` | Flag | false | Formater la sortie |
| `--minify` | Flag | false | Minifier la sortie |
| `--overwrite` | Flag | false | Écraser les fichiers existants |
| `--create-dir` | Flag | true | Créer les répertoires |
| `--validate` | Flag | false | Valider avec compilateur natif |
| `--dry-run` | Flag | false | Mode test |

### Exemples

```bash
# Convertir en MSL
wgpu-kt convert shader.wgsl -t msl -o shader.msl

# Convertir en HLSL avec version
wgpu-kt convert shader.wgsl -t hlsl --hlsl-version vs_6_0

# Batch conversion
wgpu-kt convert ./shaders/ -t msl -o ./out/

# Avec validation native
wgpu-kt convert shader.wgsl -t msl --validate
```

---

## Commande `validate`

### Description
Valide la syntaxe et la sémantique des fichiers WGSL.

### Options

| Option | Type | Défaut | Description |
|--------|------|--------|-------------|
| `--native` | Flag | false | Valider avec compilateur natif |
| `--native-target` | String | `auto` | Cible de validation (metal, dxc, glslang) |
| `--strict` | Flag | false | Warnings = erreurs |
| `-w, --warnings` | Flag | true | Afficher les warnings |
| `--no-warnings` | Flag | false | Masquer les warnings |

### Exemples

```bash
# Validation basique
wgpu-kt validate shader.wgsl

# Avec validation native
wgpu-kt validate shader.wgsl --native

# Valider un répertoire
wgpu-kt validate ./shaders/
```

---

## Commande `ir`

### Description
Affiche la représentation IR d'un fichier WGSL.

### Options

| Option | Type | Défaut | Description |
|--------|------|--------|-------------|
| `-o, --output` | Path | stdout | Fichier de sortie |
| `-f, --format` | String | `json` | Format (json, yaml, debug) |
| `--pretty` | Flag | false | JSON pretty-print |
| `--compact` | Flag | false | JSON compact |
| `--module-info` | Flag | false | Infos module seulement |
| `--type-info` | Flag | false | Infos types |

### Exemples

```bash
# Afficher IR
wgpu-kt ir shader.wgsl

# Sauvegarder en JSON
wgpu-kt ir shader.wgsl -o shader.ir.json

# Format debug
wgpu-kt ir shader.wgsl -f debug
```

---

## Commande `batch`

### Description
Traite plusieurs fichiers en batch.

### Options

| Option | Type | Défaut | Description |
|--------|------|--------|-------------|
| `-i, --input` | Path | `.` | Répertoire ou pattern d'entrée |
| `-o, --output` | Path | `<input>` | Répertoire de sortie |
| `-p, --pattern` | String | `**/*.wgsl` | Pattern de fichiers |
| `-r, --recursive` | Flag | true | Recherche récursive |
| `-w, --workers` | Int | cores | Nombre de workers |
| `--parallel` | Flag | true | Traitement parallèle |
| `--continue-on-error` | Flag | false | Continuer sur erreur |
| `--summary` | Flag | true | Afficher résumé |

### Exemples

```bash
# Convertir tous les shaders
wgpu-kt batch convert -t msl -i ./shaders/ -o ./out/

# Valider tous les shaders
wgpu-kt batch validate -i ./shaders/

# Avec 4 workers
wgpu-kt batch convert -t msl -i ./shaders/ -o ./out/ -w 4
```

---

## Commande `info`

### Description
Affiche les informations de version et de build.

### Options

| Option | Type | Défaut | Description |
|--------|------|--------|-------------|
| `--json` | Flag | false | Sortie JSON |
| `--full` | Flag | false | Informations complètes |

### Exemples

```bash
# Version
wgpu-kt info

# JSON
wgpu-kt info --json

# Complète
wgpu-kt info --full
```

---

## Commande `help`

### Description
Affiche l'aide pour une commande.

### Options
Aucune option spécifique.

### Exemples

```bash
# Aide générale
wgpu-kt help

# Aide pour une commande
wgpu-kt help convert
wgpu-kt convert --help
```

---

## Options Globales

| Option | Alias | Type | Défaut | Description |
|--------|-------|------|--------|-------------|
| `--help` | `-h` | Flag | - | Afficher l'aide |
| `--version` | `-v` | Flag | - | Afficher la version |
| `--verbose` | `-V` | Flag | false | Mode verbeux |
| `--quiet` | `-q` | Flag | false | Mode silencieux |
| `--color` | - | Flag | auto | Activer couleurs |
| `--no-color` | - | Flag | false | Désactiver couleurs |

---

**Dernière mise à jour** : 2024-XX-XX
