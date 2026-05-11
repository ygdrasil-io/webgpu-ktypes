# CLI - Checklist

## Phase 7 : CLI
**Objectif** : Créer une interface en ligne de commande complète pour WebGPU-KTypes, permettant de manipuler les shaders WGSL de manière flexible et intégrée.

---

## 📋 Structure

- [ ] **00_cli-spec.md** - Spécification complète du CLI ✅
- [ ] **01_cli-commands.md** - Documentation détaillée des commandes ✅
- [ ] **99_checklist.md** - Checklist complète pour la Phase 7 ✅

---

## 🎯 Objectifs de la Phase

### Objectifs Principaux
- [ ] Créer un CLI fonctionnel pour WebGPU-KTypes
- [ ] Implémenter toutes les commandes principales (convert, validate, ir, batch, info, help)
- [ ] Supporter les backends MSL, HLSL, GLSL, WGSL
- [ ] Gérer les fichiers et répertoires en entrée/sortie
- [ ] Intégrer avec les validateurs natifs
- [ ] Fournir une aide complète et des messages d'erreur clairs
- [ ] Créer un binaire exécutable (JAR avec dépendances)

### Objectifs Secondaires
- [ ] Créer un binaire natif avec GraalVM
- [ ] Supporter la complétion automatique (bash, zsh, fish)
- [ ] Implémenter la coloration syntaxique des erreurs
- [ ] Ajouter des options de configuration avancées
- [ ] Intégrer avec des éditeurs de code (LSP)

---

## 📁 Structure des Répertoires

### Module CLI
- [ ] Créer `wgsl:cli/` module Gradle
- [ ] Configurer `wgsl:cli/build.gradle.kts`
- [ ] Créer `src/main/kotlin/dev/gfxrs/naga/cli/`
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/cli/`

### Répertoires Source
```
wgsl:cli/
├── build.gradle.kts
└── src/
    └── main/
        └── kotlin/
            └── dev/gfxrs/naga/cli/
                ├── Main.kt                    # Point d'entrée
                ├── CliApp.kt                  # Application CLI
                ├── CliContext.kt              # Contexte CLI
                ├── command/                   # Commandes
                │   ├── Command.kt             # Interface
                │   ├── ConvertCommand.kt
                │   ├── ValidateCommand.kt
                │   ├── IrCommand.kt
                │   ├── BatchCommand.kt
                │   ├── InfoCommand.kt
                │   └── HelpCommand.kt
                ├── config/                    # Configuration
                │   └── CliConfig.kt
                ├── parser/                    # Parsing des arguments
                │   ├── ArgumentParser.kt
                │   ├── CommandParser.kt
                │   └── OptionParser.kt
                ├── output/                    # Formatage de sortie
                │   ├── OutputFormatter.kt
                │   ├── JsonFormatter.kt
                │   ├── TextFormatter.kt
                │   └── ColorFormatter.kt
                ├── backend/                   # Registre des backends
                │   ├── BackendRegistry.kt
                │   └── BackendWriter.kt
                ├── error/                     # Gestion des erreurs
                │   ├── CliError.kt
                │   ├── ErrorHandler.kt
                │   └── ErrorFormatter.kt
                └── util/                      # Utilitaires
                    ├── FileUtils.kt
                    ├── PathUtils.kt
                    └── Version.kt
```

### Répertoires de Ressources
- [ ] Créer `wgsl:cli/src/main/resources/`
- [ ] Créer `version.properties` pour la version
- [ ] Créer les templates d'aide

---

## 📄 Fichiers de Configuration

### Configuration Gradle
- [ ] Créer `wgsl:cli/build.gradle.kts`
  - [ ] Configurer le plugin `application`
  - [ ] Configurer le plugin `shadow` pour le JAR fat
  - [ ] Ajouter les dépendances CLI
  - [ ] Configurer la tâche `shadowJar`
  - [ ] Configurer la tâche `nativeImage` (optionnel)

### Dépendances
- [ ] Ajouter la dépendance à `wgsl:core`
- [ ] Ajouter la dépendance à `wgsl:wgsl`
- [ ] Ajouter la dépendance à `wgsl:msl`
- [ ] Ajouter la dépendance à `wgsl:hlsl`
- [ ] Ajouter la dépendance à `wgsl:glsl`
- [ ] Ajouter `clikt` pour le parsing des arguments
- [ ] Ajouter `jansi` pour la coloration
- [ ] Ajouter `kotlinx-serialization` pour le JSON

### Configuration du Build
- [ ] Configurer `settings.gradle.kts` pour inclure `wgsl:cli`
- [ ] Configurer la version du projet
- [ ] Configurer les tâches de distribution

---

## 🏗️ Infrastructure de Base

### Point d'Entrée
- [ ] Créer `Main.kt` avec la fonction `main`
- [ ] Initialiser le logger
- [ ] Gérer les exceptions non attrapées
- [ ] Configurer le gestionnaire de signaux (Ctrl+C)

```kotlin
// src/main/kotlin/dev/gfxrs/naga/cli/Main.kt

package io.ygdrasil.wgsl.cli

/**
 * Point d'entrée principal du CLI.
 */
fun main(args: Array<String>) {
    val exitCode = try {
        CliApp().run(args)
    } catch (e: Exception) {
        System.err.println("Unexpected error: ${e.message}")
        if (System.getProperty("wgpu.verbose") == "true") {
            e.printStackTrace()
        }
        1
    }
    
    System.exit(exitCode)
}
```

### Application CLI
- [ ] Créer `CliApp.kt`
- [ ] Implémenter le parsing des arguments
- [ ] Implémenter la sélection de la commande
- [ ] Implémenter l'exécution de la commande
- [ ] Implémenter la gestion des erreurs
- [ ] Implémenter la gestion des signaux

```kotlin
// src/main/kotlin/dev/gfxrs/naga/cli/CliApp.kt

package io.ygdrasil.wgsl.cli

import io.ygdrasil.wgsl.cli.command.HelpCommand
import io.ygdrasil.wgsl.cli.error.CliError
import io.ygdrasil.wgsl.cli.error.ErrorHandler
import io.ygdrasil.wgsl.cli.parser.CommandParser

/**
 * Application CLI principale.
 */
class CliApp {
    
    private val parser: CommandParser = CommandParser()
    private val errorHandler: ErrorHandler = ErrorHandler()
    
    /**
     * Exécuter l'application CLI.
     */
    fun run(args: Array<String>): Int {
        if (args.isEmpty()) {
            // Afficher l'aide par défaut
            return executeCommand(HelpCommand(), arrayOf("--help"))
        }
        
        // Parser les arguments
        val result = parser.parse(args)
        
        if (result.isFailure) {
            val error = result.exceptionOrNull()!!
            if (error is CliError) {
                return errorHandler.handle(error)
            } else {
                return errorHandler.handle(error)
            }
        }
        
        val (command, context) = result.getOrThrow()
        
        // Valider la commande
        val validation = command.validate(context)
        if (validation.isFailure) {
            val error = validation.exceptionOrNull()!!
            if (error is CliError) {
                return errorHandler.handle(error)
            } else {
                return errorHandler.handle(CliError.from(error))
            }
        }
        
        // Exécuter la commande
        return executeCommand(command, context)
    }
    
    private fun executeCommand(
        command: io.ygdrasil.wgsl.cli.command.Command,
        context: CliContext
    ): Int {
        return try {
            val result = command.execute(context)
            if (result.isFailure) {
                val error = result.exceptionOrNull()!!
                if (error is CliError) {
                    errorHandler.handle(error, context.verbose)
                } else {
                    errorHandler.handle(CliError.from(error), context.verbose)
                }
            } else {
                0
            }
        } catch (e: Exception) {
            errorHandler.handle(CliError.from(e), context.verbose)
        }
    }
}
```

---

## 📝 Commandes

### Interface Command
- [ ] Créer `Command.kt` interface
- [ ] Définir les propriétés (name, aliases, description, category)
- [ ] Définir les méthodes (execute, validate, getOptions, getExamples)

### ConvertCommand
- [ ] Implémenter `ConvertCommand.kt`
  - [ ] Définir les options (target, output, pretty, minify, etc.)
  - [ ] Implémenter la validation des arguments
  - [ ] Implémenter la résolution des inputs
  - [ ] Implémenter le traitement des fichiers
  - [ ] Implémenter l'écriture des sorties
  - [ ] Implémenter le formatage (pretty, minify)
  - [ ] Implémenter la validation native optionnelle
  - [ ] Implémenter le mode dry-run

### ValidateCommand
- [ ] Implémenter `ValidateCommand.kt`
  - [ ] Définir les options (native, native-target, strict, warnings)
  - [ ] Implémenter la validation avec le parser
  - [ ] Implémenter la validation native optionnelle
  - [ ] Implémenter le mode strict
  - [ ] Implémenter l'affichage des warnings
  - [ ] Implémenter le résumé de validation

### IrCommand
- [ ] Implémenter `IrCommand.kt`
  - [ ] Définir les options (output, format, pretty, compact)
  - [ ] Implémenter le parsing WGSL → IR
  - [ ] Implémenter la sérialisation JSON
  - [ ] Implémenter le format YAML (optionnel)
  - [ ] Implémenter le format debug
  - [ ] Implémenter les options de formatage

### BatchCommand
- [ ] Implémenter `BatchCommand.kt`
  - [ ] Définir les options (input, output, pattern, recursive, workers)
  - [ ] Implémenter la validation des arguments
  - [ ] Implémenter la résolution des inputs en batch
  - [ ] Implémenter le traitement séquentiel
  - [ ] Implémenter le traitement parallèle
  - [ ] Implémenter la gestion des erreurs en batch
  - [ ] Implémenter le résumé

### InfoCommand
- [ ] Implémenter `InfoCommand.kt`
  - [ ] Définir les options (json, full)
  - [ ] Implémenter la récupération des informations de version
  - [ ] Implémenter le format texte
  - [ ] Implémenter le format JSON
  - [ ] Implémenter l'affichage des backends disponibles
  - [ ] Implémenter l'affichage des validateurs disponibles

### HelpCommand
- [ ] Implémenter `HelpCommand.kt`
  - [ ] Implémenter l'aide générale
  - [ ] Implémenter l'aide par commande
  - [ ] Implémenter le formatage des options
  - [ ] Implémenter l'affichage des exemples

---

## 🔧 Parsing des Arguments

### ArgumentParser
- [ ] Créer `ArgumentParser.kt`
- [ ] Implémenter le parsing des arguments bruts
- [ ] Implémenter la détection des options globales
- [ ] Implémenter l'extraction du nom de la commande
- [ ] Implémenter l'extraction des arguments de la commande

### CommandParser
- [ ] Créer `CommandParser.kt`
- [ ] Implémenter le mapping nom → commande
- [ ] Implémenter le parsing des options de la commande
- [ ] Implémenter la création du contexte CLI
- [ ] Implémenter la validation des arguments

### OptionParser
- [ ] Créer `OptionParser.kt`
- [ ] Implémenter le parsing des options courtes (`-t`)
- [ ] Implémenter le parsing des options longues (`--target`)
- [ ] Implémenter le parsing des arguments d'options (`-t msl`)
- [ ] Implémenter le parsing des flags (`--pretty`)
- [ ] Implémenter les valeurs par défaut

---

## 🎨 Formatage de Sortie

### OutputFormatter
- [ ] Créer `OutputFormatter.kt` interface
- [ ] Définir les méthodes de formatage

### JsonFormatter
- [ ] Implémenter `JsonFormatter.kt`
- [ ] Implémenter le formatage JSON pretty
- [ ] Implémenter le formatage JSON compact

### TextFormatter
- [ ] Implémenter `TextFormatter.kt`
- [ ] Implémenter le formatage texte des résultats
- [ ] Implémenter le formatage des erreurs
- [ ] Implémenter le formatage des warnings

### ColorFormatter
- [ ] Implémenter `ColorFormatter.kt`
- [ ] Définir les couleurs pour chaque type de message
- [ ] Implémenter la détection automatique du support des couleurs
- [ ] Implémenter l'activation/désactivation des couleurs
- [ ] Implémenter les codes ANSI

---

## ❌ Gestion des Erreurs

### CliError
- [ ] Créer `CliError.kt` comme sealed class
- [ ] Implémenter les sous-classes d'erreur
  - [ ] `ArgumentParseError`
  - [ ] `InvalidOptionError`
  - [ ] `MissingArgumentError`
  - [ ] `FileError` (FileNotFound, PermissionError)
  - [ ] `ProcessingError` (ParseError, ValidationError, GenerationError)
  - [ ] `BackendError`
  - [ ] `SystemError` (NativeCompilerError, UnsupportedPlatformError)
- [ ] Implémenter les codes de sortie
- [ ] Implémenter les messages d'erreur formatés

### ErrorHandler
- [ ] Créer `ErrorHandler.kt`
- [ ] Implémenter la gestion des CliError
- [ ] Implémenter la gestion des exceptions génériques
- [ ] Implémenter le formatage des messages d'erreur
- [ ] Implémenter le mode verbeux (avec stack traces)
- [ ] Implémenter le mode silencieux

### ErrorFormatter
- [ ] Créer `ErrorFormatter.kt`
- [ ] Implémenter le formatage des erreurs de parsing
- [ ] Implémenter le formatage des erreurs de fichier
- [ ] Implémenter le formatage des erreurs de backend
- [ ] Implémenter le formatage des erreurs système

---

## 🏭 Registre des Backends

### BackendType
- [ ] Créer `BackendType.kt` enum
- [ ] Définir les types : WGSL, MSL, HLSL, GLSL, SPIRV

### BackendInfo
- [ ] Créer `BackendInfo.kt` data class
- [ ] Définir les propriétés (type, name, description, extension, available)

### BackendRegistry
- [ ] Créer `BackendRegistry.kt` object
- [ ] Implémenter le mapping type → backend
- [ ] Implémenter getBackend(type: BackendType)
- [ ] Implémenter getBackendByName(name: String)
- [ ] Implémenter getAllBackends()
- [ ] Implémenter isAvailable(type: BackendType)
- [ ] Implémenter getExtension(type: BackendType)

### BackendWriter
- [ ] Créer `BackendWriter.kt` data class
- [ ] Définir les propriétés (name, description, extension, writer)

---

## 📁 Gestion des Fichiers

### FileUtils
- [ ] Créer `FileUtils.kt`
- [ ] Implémenter resolvePath(path: String, workingDir: Path)
- [ ] Implémenter findFiles(basePath: Path, pattern: String, recursive: Boolean)
- [ ] Implémenter getExtension(path: Path)
- [ ] Implémenter changeExtension(path: Path, newExtension: String)
- [ ] Implémenter createParentDirectories(path: Path)
- [ ] Implémenter readFile(path: Path)
- [ ] Implémenter writeFile(path: Path, content: String)

### PathUtils
- [ ] Créer `PathUtils.kt`
- [ ] Implémenter normalizePath(path: Path)
- [ ] Implémenter relativePath(path: Path, base: Path)
- [ ] Implémenter isSubdirectory(parent: Path, child: Path)

---

## ℹ️ Informations de Version

### Version.kt
- [ ] Créer `Version.kt`
- [ ] Implémenter getVersion()
- [ ] Implémenter getBuildDate()
- [ ] Implémenter getGitCommit()
- [ ] Implémenter getGitBranch()
- [ ] Charger depuis version.properties

### version.properties
- [ ] Créer `src/main/resources/version.properties`
- [ ] Définir version, buildDate, gitCommit, gitBranch

---

## 🔌 Intégration avec les Validateurs

### ValidatorFactory
- [ ] Intégrer avec le `ValidatorFactory` existant
- [ ] Utiliser les validateurs pour la validation native
- [ ] Gérer les cas où les validateurs ne sont pas disponibles

### Validation dans les Commandes
- [ ] Intégrer la validation native dans `ConvertCommand`
- [ ] Intégrer la validation native dans `ValidateCommand`
- [ ] Gérer les options de validation (--native, --native-target)

---

## 📦 Distribution

### Configuration Shadow Plugin
- [ ] Configurer le plugin shadow dans `build.gradle.kts`
- [ ] Configurer le nom de l'archive
- [ ] Configurer les exclusions
- [ ] Configurer la relocalisation des packages

### Tâche de Distribution
- [ ] Créer une tâche `dist`
- [ ] Créer le répertoire dist/
- [ ] Copier le JAR
- [ ] Créer le script de lancement (wgpu-kt)
- [ ] Créer une archive tar.gz

### Script de Lancement
- [ ] Créer `scripts/wgpu-kt` (Unix)
- [ ] Créer `scripts/wgpu-kt.bat` (Windows)
- [ ] Implémenter la détection du classpath
- [ ] Implémenter le passage des arguments

### Documentation d'Installation
- [ ] Créer INSTALL.md
- [ ] Documenter l'installation manuelle
- [ ] Documenter l'installation via Homebrew (si applicable)
- [ ] Documenter l'installation via package managers

---

## 🧪 Tests

### Tests Unitaires
- [ ] Créer les tests pour `CliApp`
- [ ] Créer les tests pour chaque commande
- [ ] Créer les tests pour le parsing des arguments
- [ ] Créer les tests pour le formatage de sortie
- [ ] Créer les tests pour la gestion des erreurs
- [ ] Créer les tests pour les utilitaires

### Tests d'Intégration
- [ ] Tester le CLI de bout en bout
- [ ] Tester avec différents types de fichiers
- [ ] Tester avec différentes options
- [ ] Tester la gestion des erreurs
- [ ] Tester la validation native

### Tests de Performance
- [ ] Tester le traitement de nombreux fichiers
- [ ] Tester le mode parallèle
- [ ] Mesurer le temps d'exécution

---

## 📊 Intégration CI

### Workflow de Build
- [ ] Créer `.github/workflows/cli.yml`
- [ ] Configurer le build du JAR
- [ ] Configurer le build du binaire natif
- [ ] Configurer l'upload des artefacts

### Workflow de Release
- [ ] Créer `.github/workflows/release-cli.yml`
- [ ] Configurer le build des binaires par plateforme
- [ ] Configurer la création des packages
- [ ] Configurer l'upload vers GitHub Releases

---

## 📚 Documentation

### Documentation Technique
- [ ] Documenter l'architecture du CLI
- [ ] Documenter les commandes disponibles
- [ ] Documenter les options de chaque commande
- [ ] Documenter les exemples d'utilisation
- [ ] Documenter l'intégration avec les backends
- [ ] Documenter la gestion des erreurs

### Documentation Utilisateur
- [ ] Créer CLI.md
- [ ] Documenter l'installation
- [ ] Documenter l'utilisation de base
- [ ] Documenter les commandes avancées
- [ ] Documenter la configuration
- [ ] Documenter le dépannage

### Man Pages
- [ ] Créer les man pages pour chaque commande
- [ ] Formater selon les conventions Unix

---

## ✅ État Actuel

| Élément | État | Date | Notes |
|---------|------|------|-------|
| 00_cli-spec.md | ✅ | 2024-XX-XX | Complété |
| 01_cli-commands.md | ✅ | 2024-XX-XX | Complété |
| 99_checklist.md | ✅ | 2024-XX-XX | En cours |
| Module CLI créé | ⬜ | - | À créer |
| Infrastructure de base | ⬜ | - | À implémenter |
| Commandes | ⬜ | - | À implémenter |
| Parsing des arguments | ⬜ | - | À implémenter |
| Formatage de sortie | ⬜ | - | À implémenter |
| Gestion des erreurs | ⬜ | - | À implémenter |
| Registre des backends | ⬜ | - | À implémenter |
| Gestion des fichiers | ⬜ | - | À implémenter |
| Intégration validateurs | ⬜ | - | À implémenter |
| Tests | ⬜ | - | À implémenter |
| Documentation | ⬜ | - | À créer |

---

## 🎯 Prochaines Étapes

1. [ ] **Phase 99 - Annexes** : Créer les documents annexes
   - [ ] 00_glossary.md - Glossaire des termes
   - [ ] 01_comparison-rust-kotlin.md - Comparaison Rust/Kotlin
   - [ ] 02_references.md - Références complètes
   - [ ] 99_checklist.md - Checklist des annexes

2. [ ] **Finalisation** :
   - [ ] Revoir tous les documents
   - [ ] Vérifier la cohérence entre les phases
   - [ ] Corriger les erreurs et omissions
   - [ ] Valider avec l'utilisateur

3. [ ] **Implémentation** (si la phase de planification est terminée) :
   - [ ] Commencer l'implémentation du code
   - [ ] Suivre l'ordre des phases définies

---

## 📞 Contacts et Ressources

### Ressources Internes
- Projet Rust : `/Users/chaos/RustroverProjects/wgpu/naga/`
- CLI Rust : `/Users/chaos/RustroverProjects/wgpu/naga/src/bin/naga.rs`
- Documentation Rust : `docs/`

### Outils Externes
- Clikt : [https://ajalt.github.io/clikt/](https://ajalt.github.io/clikt/)
- Picocli : [https://picocli.info/](https://picocli.info/)
- GraalVM : [https://www.graalvm.org/](https://www.graalvm.org/)
- Shadow Plugin : [https://imperceptiblethoughts.com/shadow/](https://imperceptiblethoughts.com/shadow/)
- JANSI : [https://github.com/fusesource/jansi](https://github.com/fusesource/jansi)

### Documentation de Référence
- [Command Line Interface Guidelines](https://clig.dev/)
- [12 Factor App - CLI](https://12factor.net/cli)
- [Unix Philosophy](https://en.wikipedia.org/wiki/Unix_philosophy)
- [Kotlin CLI Documentation](https://kotlinlang.org/docs/command-line.html)
- [Gradle Shadow Plugin](https://docs.gradle.org/current/userguide/shadow_plugin.html)

---

## 📝 Notes

### Notes d'Implémentation
- Utiliser Clikt pour le parsing des arguments
- Utiliser JANSI pour la coloration du terminal
- Utiliser kotlinx.serialization pour le JSON
- Suivre les conventions Unix pour les codes de sortie
- Rendre les messages d'erreur clairs et actionnables
- Supporter à la fois les options courtes et longues
- Rendre le CLI idiomatique et intuitif

### Notes de Compatibilité
- Le CLI doit fonctionner sur macOS, Linux et Windows
- Tester avec différentes versions de JDK
- Gérer les chemins de fichiers de manière portable
- Gérer les différences de ligne de commande entre les OS

### Notes de Performance
- Le traitement en batch doit être optimisé
- Le mode parallèle doit être efficace
- La mémoire doit être gérée correctement
- Les gros fichiers doivent être traités sans problème

### Notes de Sécurité
- Ne pas permettre l'exécution de code arbitraire
- Valider tous les chemins de fichiers
- Gérer les permissions de manière sécurisée
- Éviter les vulnérabilités d'injection de commandes

---

**Dernière mise à jour** : 2024-XX-XX
**Responsable** : Équipe WebGPU-KTypes
**Statut** : En cours
