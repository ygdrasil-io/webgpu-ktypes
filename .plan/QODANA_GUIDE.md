# 🔧 Guide Qodana - Récupération et Analyse des Rapports

Ce guide explique comment **récupérer, télécharger et analyser** les rapports Qodana via GitHub CLI (`gh`) et outils associés.

---

## 📥 Récupération des Rapports Qodana

### 1️⃣ Trouver l'ID du dernier Run Qodana

```bash
# Lister les runs du workflow Qodana (derniers 5)
gh api repos/ygdrasil-io/webgpu-ktypes/actions/workflows/qodana_code_quality.yml/runs \
  | jq -r '.workflow_runs[:5] | .[] | "ID: \(.id) | Status: \(.status) | Conclusion: \(.conclusion) | Branch: \(.head_branch) | Date: \(.created_at)"'
```

**Sortie exemple** :
```
ID: 25804156777 | Status: completed | Conclusion: success | Branch: feature/naga-port-phase1 | Date: 2026-05-13T13:00:21Z
ID: 25800700250 | Status: completed | Conclusion: success | Branch: feature/naga-port-phase1 | Date: 2026-05-13T11:23:42Z
```

---

### 2️⃣ Récupérer l'ID de l'Artifact

```bash
RUN_ID=25804156777  # Remplacer par l'ID du run

gh api repos/ygdrasil-io/webgpu-ktypes/actions/runs/$RUN_ID/artifacts \
  | jq -r '.artifacts[] | "ID: \(.id) | Nom: \(.name) | Taille: \(.size_in_bytes) bytes | Créé: \(.created_at)"'
```

**Sortie exemple** :
```
ID: 6972483738 | Nom: qodana-report | Taille: 4530867 bytes | Créé: 2026-05-13T14:13:54Z
```

---

### 3️⃣ Télécharger l'Artifact

#### Méthode 1 : Via `gh api` (recommandé)

```bash
RUN_ID=25804156777
ARTIFACT_ID=6972483738

# Télécharger l'artifact
gh api repos/ygdrasil-io/webgpu-ktypes/actions/artifacts/$ARTIFACT_ID/zip \
  -H "Accept: application/vnd.github.v3+json" \
  --jq '.[]' > qodana-report.zip

# Décompresser (archive imbriquée)
cd /tmp
unzip -o qodana-report.zip
# L'archive contient qodana-report.zip, la décompresser aussi
unzip -o qodana-report.zip
```

#### Méthode 2 : Via `curl` direct

```bash
RUN_ID=25804156777
ARTIFACT_ID=6972483738
TOKEN=$(gh auth token)

curl -L \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/ygdrasil-io/webgpu-ktypes/actions/artifacts/$ARTIFACT_ID/zip" \
  -o qodana-report.zip

unzip -o qodana-report.zip
unzip -o qodana-report.zip  # Décompresser l'archive imbriquée
```

---

## 📊 Analyse du Rapport SARIF

Le rapport Qodana est au format **SARIF (Static Analysis Results Interchange Format)**.

### Structure du fichier `qodana.sarif.json`

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/...",
  "version": "2.1.0",
  "runs": [
    {
      "tool": {"driver": {"name": "Qodana", "version": "2026.1.0"}},
      "results": [
        {
          "ruleId": "UnusedSymbol",
          "level": "warning",
          "message": {"text": "Unused symbol 'foo'"},
          "locations": [
            {
              "physicalLocation": {
                "artifactLocation": {"uri": "wgsl/core/src/.../File.kt"},
                "region": {"startLine": 24, "startColumn": 1, "endColumn": 10}
              }
            }
          ]
        }
      ]
    }
  ]
}
```

---

### Commandes `jq` pour analyser le rapport

#### 1. Statistiques globales

```bash
# Nombre total de problèmes
jq '.runs[0].results | length' qodana.sarif.json

# Par niveau de sévérité
jq '[.runs[0].results[] | .level] | group_by(.) | map({level: .[0], count: length})' qodana.sarif.json
```

**Sortie** :
```json
[
  {"level": "note", "count": 22},
  {"level": "warning", "count": 459}
]
```

---

#### 2. Top fichiers avec le plus de problèmes

```bash
jq '.runs[0].results | group_by(.locations[0].physicalLocation.artifactLocation.uri) | map({file: .[0].locations[0].physicalLocation.artifactLocation.uri, count: length}) | sort_by(.count) | reverse' qodana.sarif.json
```

---

#### 3. Lister tous les problèmes avec détails

```bash
# Format : Ligne Fichier: Message [Niveau]
jq '.runs[0].results[] | "L\(.locations[0].physicalLocation.region.startLine) \(.locations[0].physicalLocation.artifactLocation.uri): \(.message.text) [\(.level)]"' qodana.sarif.json
```

---

#### 4. Filtrer par type de problème

```bash
# Tous les "Unused symbol"
jq '.runs[0].results[] | select(.ruleId == "UnusedSymbol") | "L\(.locations[0].physicalLocation.region.startLine) \(.locations[0].physicalLocation.artifactLocation.uri): \(.message.text)"' qodana.sarif.json

# Tous les problèmes dans un fichier spécifique
FILE="wgsl/core/src/commonMain/kotlin/ir/Function.kt"
jq --arg file "$FILE" \
  '.runs[0].results[] | select(.locations[0].physicalLocation.artifactLocation.uri == $file) | "L\(.locations[0].physicalLocation.region.startLine): \(.message.text) [\(.level)]"' \
  qodana.sarif.json
```

---

#### 5. Statistiques par règle

```bash
jq '.runs[0].results | group_by(.ruleId) | map({rule: .[0].ruleId, count: length, level: .[0].level}) | sort_by(.count) | reverse' qodana.sarif.json
```

**Sortie exemple** :
```json
[
  {"rule": "UnusedSymbol", "count": 292, "level": "warning"},
  {"rule": "RedundantQualifierName", "count": 91, "level": "warning"},
  {"rule": "RedundantVisibilityModifier", "count": 60, "level": "warning"}
]
```

---

#### 6. Exporter en CSV

```bash
# En-tête
printf "File,Ligne,Colonne,Sévérité,Règle,Message\n"

# Données
jq -r '.runs[0].results[] | [
  .locations[0].physicalLocation.artifactLocation.uri,
  .locations[0].physicalLocation.region.startLine,
  .locations[0].physicalLocation.region.startColumn,
  .level,
  .ruleId,
  .message.text
] | @csv' qodana.sarif.json
```

---

## 📁 Fichiers disponibles dans l'Artifact

```
qodana-report/
├── qodana.sarif.json              # Rapport principal SARIF (4.6 Mo)
├── qodana-short.sarif.json        # Version courte SARIF (2.7 Ko)
├── report/
│   ├── index.html                 # Rapport HTML navigable
│   ├── css/                       # Styles CSS
│   ├── js/                        # Scripts JavaScript
│   └── results/
│       ├── result-allProblems.json # Tous les problèmes en JSON
│       ├── descriptions/          # Descriptions des inspections
│       └── projectStructure/      # Structure du projet
└── log/                          # Logs de l'analyse
    ├── code-inspection.log
    ├── idea.log
    └── qodana/                    # Logs Qodana spécifiques
```

---

## 🎯 Commandes Pratiques

### Générer un résumé rapide

```bash
#!/bin/bash
RUN_ID=$1
ARTIFACT_ID=$(gh api repos/ygdrasil-io/webgpu-ktypes/actions/runs/$RUN_ID/artifacts | jq -r '.artifacts[0].id')

gh api repos/ygdrasil-io/webgpu-ktypes/actions/artifacts/$ARTIFACT_ID/zip \
  -H "Accept: application/vnd.github.v3+json" \
  --jq '.[]' > /tmp/qodana.zip

cd /tmp && rm -rf qodana-report && mkdir qodana-report
cd qodana-report && unzip -o ../qodana.zip && unzip -o qodana-report.zip

echo "=== Statistiques Qodana ==="
jq '.runs[0].results | length' qodana.sarif.json
echo "problèmes au total"

echo ""
echo "=== Par niveau ==="
jq '[.runs[0].results[] | .level] | group_by(.) | map({level: .[0], count: length})' qodana.sarif.json

echo ""
echo "=== Top 5 fichiers ==="
jq '.runs[0].results | group_by(.locations[0].physicalLocation.artifactLocation.uri) | map({file: .[0].locations[0].physicalLocation.artifactLocation.uri, count: length}) | sort_by(.count) | reverse | .[:5]' qodana.sarif.json
```

---

### Comparer deux rapports SARIF

```bash
# Récupérer les IDs de deux runs
RUN1=25800700250
RUN2=25804156777

# Télécharger les deux rapports
# ... (télécharger et extraire comme ci-dessus)

# Comparer le nombre de problèmes
echo "Run $RUN1: $(jq '.runs[0].results | length' report1/qodana.sarif.json) problèmes"
echo "Run $RUN2: $(jq '.runs[0].results | length' report2/qodana.sarif.json) problèmes"

# Trouver les problèmes résolus
jq --argfile a report1/qodana.sarif.json --argfile b report2/qodana.sarif.json \
  '($b.runs[0].results | map(.locations[0].physicalLocation) | unique) - ($a.runs[0].results | map(.locations[0].physicalLocation) | unique) | length' && \
echo "problèmes résolus"
```

---

## 📊 Visualisation avec `sarif-viewer`

Pour une visualisation locale du rapport SARIF :

```bash
# Installer sarif-viewer (Node.js requis)
npm install -g @microsoft/sarif-viewer

# Servir le rapport localement
sarif-viewer --port 8080 qodana.sarif.json

# Puis ouvrir http://localhost:8080 dans le navigateur
```

---

## 🔄 Automatisation

### Script complet : `get-qodana-report.sh`

```bash
#!/bin/bash
# Usage: ./get-qodana-report.sh [RUN_ID]
# Si RUN_ID non fourni, utilise le dernier run

REPO="ygdrasil-io/webgpu-ktypes"
WORKFLOW="qodana_code_quality.yml"
OUTPUT_DIR="./qodana-reports/$(date +%Y%m%d-%H%M%S)"

# Créer le dossier de sortie
mkdir -p "$OUTPUT_DIR"

# Récupérer le dernier RUN_ID si non fourni
RUN_ID=${1:-$(gh api repos/$REPO/actions/workflows/$WORKFLOW/runs | jq -r '.workflow_runs[0].id')}

echo "Récupération du rapport Qodana pour Run #$RUN_ID..."

# Récupérer l'ID de l'artifact
ARTIFACT_ID=$(gh api repos/$REPO/actions/runs/$RUN_ID/artifacts | jq -r '.artifacts[0].id')

if [ -z "$ARTIFACT_ID" ]; then
  echo "❌ Aucun artifact trouvé pour le run #$RUN_ID"
  exit 1
fi

# Télécharger et extraire
gh api repos/$REPO/actions/artifacts/$ARTIFACT_ID/zip \
  -H "Accept: application/vnd.github.v3+json" \
  --jq '.[]' > "$OUTPUT_DIR/qodana-archive.zip"

cd "$OUTPUT_DIR"
unzip -o qodana-archive.zip > /dev/null 2>&1
unzip -o qodana-report.zip > /dev/null 2>&1
rm qodana-archive.zip qodana-report.zip

echo "✅ Rapport téléchargé dans: $OUTPUT_DIR"
echo ""
echo "Fichiers disponibles:"
ls -lh $OUTPUT_DIR | tail -n +2

# Afficher le résumé
echo ""
echo "=== Résumé Qodana ==="
TOTAL=$(jq '.runs[0].results | length' qodana.sarif.json)
WARNINGS=$(jq '[.runs[0].results[] | select(.level == "warning")] | length' qodana.sarif.json)
NOTES=$(jq '[.runs[0].results[] | select(.level == "note")] | length' qodana.sarif.json)

echo "Total: $TOTAL problèmes"
echo "  - Warnings: $WARNINGS"
echo "  - Notes: $NOTES"
```

---

## 📚 Références

- **Qodana Documentation** : [https://www.jetbrains.com/help/qodana/](https://www.jetbrains.com/help/qodana/)
- **SARIF Specification** : [https://sarifweb.azurewebsites.net/](https://sarifweb.azurewebsites.net/)
- **Qodana Action** : [https://github.com/JetBrains/qodana-action](https://github.com/JetBrains/qodana-action)
- **GitHub Actions API** : [https://docs.github.com/en/rest/actions](https://docs.github.com/en/rest/actions)

---

## 💡 Conseils

1. **Artifacts expirent après 90 jours** - Téléchargez-les rapidement
2. **Un run = un commit** - Chaque push sur une branche déclenche un nouveau scan
3. **SARIF est standard** - Compatible avec VS Code, GitHub Code Scanning, etc.
4. **Qodana Cloud** - Pour des rapports plus détaillés avec historique

---

*Dernière mise à jour : 13 mai 2026*
