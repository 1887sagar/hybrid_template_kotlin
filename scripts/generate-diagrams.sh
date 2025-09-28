#!/bin/bash
# Script to generate SVG files from PlantUML diagrams

# Check if plantuml is installed
if ! command -v plantuml &> /dev/null; then
    echo "Error: plantuml is not installed"
    echo "Install with: brew install plantuml (macOS) or apt-get install plantuml (Ubuntu)"
    exit 1
fi

# Change to docs/diagrams directory
cd "$(dirname "$0")/docs/diagrams" || exit 1

echo "Generating SVG diagrams from PlantUML files..."

# Generate SVG for each PUML file
for puml_file in *.puml; do
    if [ -f "$puml_file" ]; then
        echo "Processing: $puml_file"
        plantuml -tsvg "$puml_file" || echo "Failed to process: $puml_file"
    fi
done

echo "Done! SVG files generated in docs/diagrams/"

# List generated files
echo -e "\nGenerated files:"
ls -la *.svg 2>/dev/null | awk '{print "  " $9}' || echo "No SVG files found"