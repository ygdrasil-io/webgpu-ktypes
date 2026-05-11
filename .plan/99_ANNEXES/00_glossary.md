# Glossaire des Termes

## Sommaire

1. [Termes Généraux](#termes-généraux)
2. [Termes WebGPU/WGSL](#termes-webgpuwgsl)
3. [Termes Naga](#termes-naga)
4. [Termes Backend](#termes-backend)
5. [Termes Kotlin](#termes-kotlin)
6. [Termes de Compilation](#termes-de-compilation)
7. [Termes de Test](#termes-de-test)

---

## Termes Généraux

### A

| Terme | Définition | Contexte |
|-------|------------|----------|
| **API** | Application Programming Interface | Interface de programmation pour interagir avec une bibliothèque ou un service |
| **AST** | Abstract Syntax Tree | Représentation arborescente d'un code source |

### B

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Backend** | Module de génération de code | Convertit l'IR vers un langage cible (MSL, HLSL, GLSL) |

### C

| Terme | Définition | Contexte |
|-------|------------|----------|
| **CLI** | Command Line Interface | Interface en ligne de commande |
| **Compiler** | Programme qui traduit du code source vers un code machine | Conversion WGSL → MSL/HLSL/GLSL |
| **Cross-platform** | Compatible avec plusieurs plateformes | JVM permet l'exécution sur macOS, Linux, Windows |

### F

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Frontend** | Module de parsing | Convertit le code source WGSL en IR |

### G

| Terme | Définition | Contexte |
|-------|------------|----------|
| **GPU** | Graphics Processing Unit | Unité de traitement graphique |
| **Graphique** | Représentation visuelle | Rendering 2D/3D |

### I

| Terme | Définition | Contexte |
|-------|------------|----------|
| **IR** | Intermediate Representation | Représentation intermédiaire du code |

### L

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Lexer** | Analyseur lexicographique | Tokenise le code source en tokens |

### M

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Module** | Unité de compilation | Contient types, fonctions, variables globales |

### O

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Optimisation** | Amélioration des performances | Réduction de la taille du code, amélioration de la vitesse |

### P

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Parser** | Analyseur syntaxique | Convertit les tokens en AST/IR |
| **Pipeline** | Chaîne de traitement | Parsing → Validation → Optimisation → Génération |

### R

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Runtime** | Environnement d'exécution | JVM pour Kotlin |

### S

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Shader** | Programme exécuté sur GPU | Code WGSL/MSL/HLSL/GLSL |
| **Source Code** | Code lisible par un humain | Fichiers .wgsl, .msl, etc. |

### T

| Terme | Définition | Contexte |
|-------|------------|----------|
| **Token** | Unité lexicographique | Résultat du lexer (mot-clé, identifiant, littéral) |
| **Transpiler** | Compilateur source-à-source | Convertit entre langages de shader |

---

## Termes WebGPU/WGSL

### A

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **@alignment** | Attribut pour l'alignement | `@alignment(16)` pour un struct |
| **@binding** | Attribut pour le binding | `@binding(0)` pour une ressource |
| **@builtin** | Attribut pour les builtins | `@builtin(position)` pour la position |
| **@compute** | Attribut pour compute shader | `@compute @workgroup_size(8,8,1)` |
| **@fragment** | Attribut pour fragment shader | `@fragment` fonction |
| **@group** | Attribut pour le groupe | `@group(0)` pour une ressource |
| **@interpolate** | Attribut pour l'interpolation | `@interpolate(linear, center)` |
| **@location** | Attribut pour la location | `@location(0)` pour un input/output |
| **@size** | Attribut pour la taille | `@size(4)` pour un tableau |
| **@vertex** | Attribut pour vertex shader | `@vertex` fonction |
| **@workgroup_size** | Attribut pour la taille du workgroup | `@workgroup_size(8,8,1)` |

### B

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **binding** | Association d'une ressource | Buffer, texture, sampler |
| **bool** | Type booléen | `true`, `false` |

### C

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **compute shader** | Shader de calcul | Exécuté pour chaque élément d'un workgroup |
| **const** | Variable constante | `const x: i32 = 42;` |

### D

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **discard** | Instruction de rejet | `discard;` dans un fragment shader |

### E

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **entry point** | Point d'entrée | Fonction marquée avec `@vertex`, `@fragment`, `@compute` |
| **expression** | Expression | `a + b`, `fn_call(x, y)` |

### F

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **f16** | Type float 16 bits | `f16` |
| **f32** | Type float 32 bits | `f32` |
| **for** | Boucle for | `for (var i = 0; i < 10; i = i + 1) { ... }` |
| **function** | Fonction | `fn add(a: i32, b: i32) -> i32 { ... }` |

### G

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **group** | Groupe de ressources | `@group(0)` |

### I

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **i32** | Type entier signé 32 bits | `i32` |
| **if** | Conditionnelle | `if (condition) { ... } else { ... }` |

### L

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **let** | Déclaration de variable | `let x: i32 = 42;` |

### M

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **mat2x2<T>** | Matrice 2x2 | `mat2x2<f32>` |
| **mat2x3<T>** | Matrice 2x3 | `mat2x3<f32>` |
| **mat2x4<T>** | Matrice 2x4 | `mat2x4<f32>` |
| **mat3x2<T>** | Matrice 3x2 | `mat3x2<f32>` |
| **mat3x3<T>** | Matrice 3x3 | `mat3x3<f32>` |
| **mat3x4<T>** | Matrice 3x4 | `mat3x4<f32>` |
| **mat4x2<T>** | Matrice 4x2 | `mat4x2<f32>` |
| **mat4x3<T>** | Matrice 4x3 | `mat4x3<f32>` |
| **mat4x4<T>** | Matrice 4x4 | `mat4x4<f32>` |

### P

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **pointer** | Pointeur | `ptr<function, i32>` |

### R

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **return** | Instruction de retour | `return value;` |

### S

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **sampler** | Échantillonneur | Pour échantillonner les textures |
| **scalar** | Type scalaire | `i32`, `u32`, `f32`, `bool` |
| **stage** | Stage de shader | `vertex`, `fragment`, `compute` |
| **struct** | Structure | `struct Vertex { position: vec4<f32>, ... }` |
| **switch** | Instruction switch | `switch (value) { case 1: ... }` |

### T

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **texture** | Texture | Ressource pour le sampling |
| **type** | Type | Définition de type |

### U

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **u32** | Type entier non signé 32 bits | `u32` |

### V

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **var** | Variable mutable | `var x: i32 = 42;` |
| **vec2<T>** | Vecteur 2D | `vec2<f32>` |
| **vec3<T>** | Vecteur 3D | `vec3<f32>` |
| **vec4<T>** | Vecteur 4D | `vec4<f32>` |
| **vertex shader** | Shader de sommet | Transforme les sommets |

### W

| Terme | Définition | Contexte WGSL |
|-------|------------|---------------|
| **while** | Boucle while | `while (condition) { ... }` |
| **workgroup** | Groupe de travail | Pour compute shaders |

---

## Termes Naga

### A

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Arena** | Structure de stockage | Stockage efficace des objets avec des handles |
| **Arena<T>** | Arena générique | `Arena<Type>`, `Arena<Expression>` |

### B

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **BinaryOperation** | Opération binaire | Addition, soustraction, multiplication, etc. |

### C

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Capabilities** | Capacités | Fonctionnalités supportées par un backend |

### E

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **EntryPoint** | Point d'entrée | Fonction d'entrée pour un stage de shader |
| **Expression** | Expression | Nœud dans l'AST représentant une opération |

### F

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Function** | Fonction | Définition de fonction |

### G

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **GlobalVariable** | Variable globale | Variable au niveau du module |

### H

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Handle** | Poignée | Référence à un objet dans une arena |
| **Handle<T>** | Handle générique | `Handle<Type>`, `Handle<Function>` |

### I

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **IR** | Intermediate Representation | Représentation intermédiaire du shader |

### L

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **LocalVariable** | Variable locale | Variable dans une fonction |

### M

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Module** | Module | Conteneur racine pour tout le code |

### P

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Proc** | Procédure | Pour l'évaluation des expressions constantes |

### R

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Range** | Plage | Plage d'indices |

### S

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Scalar** | Scalaire | Type de base (i32, u32, f32, bool) |
| **ScalarKind** | Type de scalaire | `I32`, `U32`, `F32`, `Bool` |
| **Span** | Étendue | Position dans le code source |
| **Spanned<T>** | Avec étendue | `Spanned<Expression>` |
| **Statement** | Instruction | Nœud dans l'AST représentant une instruction |
| **StructType** | Type struct | Définition de structure |

### T

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Type** | Type | Type dans l'IR |
| **TypeIndex** | Index de type | Référence à un type dans le module |

### U


| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **UniqueArena** | Arena unique | Arena qui garantit l'unicité |

### V

| Terme | Définition | Contexte Naga |
|-------|------------|---------------|
| **Validation** | Validation | Vérification de la validité de l'IR |
| **Validator** | Validateur | Vérifie la validité de l'IR |

---

## Termes Backend

### G

| Terme | Définition | Contexte Backend |
|-------|------------|------------------|
| **GLSL** | OpenGL Shading Language | Langage de shader pour OpenGL/Vulkan |

### H

| Terme | Définition | Contexte Backend |
|-------|------------|------------------|
| **HLSL** | High-Level Shading Language | Langage de shader pour DirectX |

### M

| Terme | Définition | Contexte Backend |
|-------|------------|------------------|
| **MSL** | Metal Shading Language | Langage de shader pour Metal (Apple) |

### S

| Terme | Définition | Contexte Backend |
|-------|------------|------------------|
| **SPIR-V** | Standard Portable Intermediate Representation | Représentation intermédiaire pour Vulkan |

---

## Termes Kotlin

### A

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **abstract** | Classe/propriété abstraite | Doit être implémenté dans les sous-classes |

### C

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **class** | Classe | Définition de classe |
| **companion object** | Objet compagnon | Singleton associé à une classe |
| **coroutine** | Coroutine | Concurrence légère |

### D

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **data class** | Classe de données | Classe avec méthodes utilitaires automatiques |

### E

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **extension function** | Fonction d'extension | Ajoute une fonction à une classe existante |

### F

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **fun** | Fonction | Définition de fonction |

### I

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **interface** | Interface | Contrat pour les classes |

### L

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **lambda** | Expression lambda | Fonction anonyme |

### O

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **object** | Objet | Singleton |

### P

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **package** | Package | Espace de nommage |
| **property** | Propriété | Variable avec getter/setter |

### S

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **sealed class** | Classe scellée | Classe avec sous-classes connues |

### T

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **typealias** | Alias de type | Créer un alias pour un type |

### V

| Terme | Définition | Contexte Kotlin |
|-------|------------|-----------------|
| **val** | Variable immuable | `val x: Int = 42` |
| **var** | Variable mutable | `var x: Int = 42` |

---

## Termes de Compilation

### B

| Terme | Définition | Contexte Compilation |
|-------|------------|----------------------|
| **Build** | Construction | Compilation du code source |

### C

| Terme | Définition | Contexte Compilation |
|-------|------------|----------------------|
| **Compile** | Compiler | Traduire le code source vers le code machine |

### D

| Terme | Définition | Contexte Compilation |
|-------|------------|----------------------|
| **Dependency** | Dépendance | Bibliothèque requise pour la compilation |

### L

| Terme | Définition | Contexte Compilation |
|-------|------------|----------------------|
| **Link** | Lier | Combiner plusieurs fichiers objet |

### O

| Terme | Définition | Contexte Compilation |
|-------|------------|----------------------|
| **Optimize** | Optimiser | Améliorer les performances du code |

### T

| Terme | Définition | Contexte Compilation |
|-------|------------|----------------------|
| **Target** | Cible | Plateforme cible (JVM, Native, JS) |
| **Transpile** | Transpiler | Convertir entre langages de même niveau |

---

## Termes de Test

### C

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **CI** | Continuous Integration | Intégration continue |
| **Coverage** | Couverture | Pourcentage de code testé |

### E

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **E2E Test** | Test End-to-End | Test de bout en bout |

### F

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **Fixture** | Configuration de test | Données/état pour les tests |

### G

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **Golden Test** | Test Golden | Test par comparaison avec une référence |

### I

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **Integration Test** | Test d'intégration | Test de l'intégration des composants |

### M

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **Mock** | Mock | Objet simulé pour les tests |
| **Mocking** | Simulation | Remplacer des dépendances par des mocks |

### R

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **Regression Test** | Test de régression | Test pour détecter les régressions |

### S

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **Snapshot Test** | Test de snapshot | Test par comparaison de snapshots |
| **Stub** | Stub | Implémentation minimale pour les tests |

### U

| Terme | Définition | Contexte Test |
|-------|------------|---------------|
| **Unit Test** | Test unitaire | Test d'une unité de code |

---

## Abréviations

| Abréviation | Définition |
|-------------|------------|
| API | Application Programming Interface |
| AST | Abstract Syntax Tree |
| CLI | Command Line Interface |
| CPU | Central Processing Unit |
| GPU | Graphics Processing Unit |
| IR | Intermediate Representation |
| JVM | Java Virtual Machine |
| MSL | Metal Shading Language |
| SDK | Software Development Kit |
| SPIR-V | Standard Portable Intermediate Representation |
| WGSL | WebGPU Shading Language |

---

**Dernière mise à jour** : 2024-XX-XX
