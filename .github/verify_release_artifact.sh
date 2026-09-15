#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Release version is required}"
ARCHIVE="build/distributions/CodeScene-${VERSION}.zip"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

if [[ ! -f "$ARCHIVE" ]]; then
  echo "Missing release archive: $ARCHIVE" >&2
  exit 1
fi

unzip -q "$ARCHIVE" -d "$TEMP_DIR"
PLUGIN_JAR="$TEMP_DIR/CodeScene/lib/CodeScene-${VERSION}.jar"

if [[ ! -f "$PLUGIN_JAR" ]]; then
  echo "Missing plugin JAR: CodeScene/lib/CodeScene-${VERSION}.jar" >&2
  exit 1
fi

unzip -p "$PLUGIN_JAR" META-INF/plugin.xml > "$TEMP_DIR/plugin.xml"

if ! grep -Fq "<version>${VERSION}</version>" "$TEMP_DIR/plugin.xml"; then
  echo "Plugin descriptor does not contain version $VERSION." >&2
  exit 1
fi

echo "Verified CodeScene-${VERSION}.zip and its plugin descriptor."
