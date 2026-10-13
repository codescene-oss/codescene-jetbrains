#!/usr/bin/env bash
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SPIKE="$SCRIPT_DIR/cs-ide-macos-spike.clj"
API_VERSION="1.0-fdef705"
REPORT="${REPORT:-$PWD/cs-ide-macos-spike-report.md}"

die() { echo "error: $*" >&2; exit 2; }

[[ "$(uname -s)" == "Darwin" ]] || die "run this on macOS"
command -v git >/dev/null || die "git not found (xcode-select --install)"
command -v bb >/dev/null || die "babashka not found (brew install borkdude/brew/babashka)"
[[ -f "$SPIKE" ]] || die "spike script not found: $SPIKE"

find_ide() {
  local app
  for app in /Applications/*.app "$HOME"/Applications/*.app "$HOME"/Applications/*/*.app; do
    [[ -x "$app/Contents/jbr/Contents/Home/bin/java" ]] || continue
    [[ -d "$app/Contents/lib" ]] || continue
    echo "$app"
  done
}

find_jar() {
  if [[ -n "${API_JAR:-}" ]]; then
    echo "$API_JAR"
    return
  fi
  local jar
  for jar in "$HOME"/Library/Application\ Support/JetBrains/*/plugins/*/lib/api-"$API_VERSION".jar \
             "$HOME"/.gradle/caches/modules-2/files-2.1/codescene.extension/api/"$API_VERSION"/*/api-"$API_VERSION".jar \
             "$HOME"/Library/Application\ Support/JetBrains/*/plugins/*/lib/api-*.jar; do
    if [[ -f "$jar" ]]; then
      echo "$jar"
      return
    fi
  done
}

IDE_APP="${1:-}"
if [[ -z "$IDE_APP" ]]; then
  IDES="$(find_ide)"
  [[ -n "$IDES" ]] || die "no JetBrains IDE with a bundled JBR found in /Applications or ~/Applications; pass the .app path"
  echo "JetBrains IDEs found:"
  echo "$IDES" | sed 's/^/  /'
  IDE_APP="$(echo "$IDES" | head -n 1)"
  echo "Using: $IDE_APP (pass another .app path as the first argument to choose)"
fi
JAVA="$IDE_APP/Contents/jbr/Contents/Home/bin/java"
[[ -x "$JAVA" ]] || die "no JBR java at $JAVA"

JAR="$(find_jar)"
[[ -n "$JAR" && -f "$JAR" ]] || die "extension API jar not found. Install the CodeScene plugin in the IDE, or set API_JAR=/path/to/api-$API_VERSION.jar"
echo "Extension API jar: $JAR"

JNA_DIR="$(dirname "$(ls "$IDE_APP"/Contents/lib/jna/*/libjnidispatch.jnilib 2>/dev/null | head -n 1)" 2>/dev/null)"
IDE_NAME="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleName' "$IDE_APP/Contents/Info.plist" 2>/dev/null)"
IDE_VERSION="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$IDE_APP/Contents/Info.plist" 2>/dev/null)"
IDE_BUILD="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$IDE_APP/Contents/Info.plist" 2>/dev/null)"
JBR_VERSION="$("$JAVA" -version 2>&1 | head -n 2 | tr '\n' ' ')"
ENTITLEMENTS="$(codesign -d --entitlements - "$JAVA" 2>/dev/null \
  | grep -o 'com\.apple\.security\.[a-z.-]*' | sort -u | tr '\n' ' ')"
QUARANTINE="$(xattr -p com.apple.quarantine "$IDE_APP" 2>/dev/null || echo none)"

run_spike() {
  local label="$1" tool_options="$2" log
  log="$(mktemp -t cs-ide-spike)"
  echo
  echo "=== $label ==="
  local code
  if [[ -n "$tool_options" ]]; then
    JAVA_TOOL_OPTIONS="$tool_options" bb "$SPIKE" "$JAVA" "$JAR" 2>&1 | tee "$log"
    code=${PIPESTATUS[0]}
  else
    bb "$SPIKE" "$JAVA" "$JAR" 2>&1 | tee "$log"
    code=${PIPESTATUS[0]}
  fi
  local verdict
  verdict="$(grep -E '^(PASS|FAIL)' "$log" | tail -n 1)"
  [[ -n "$verdict" ]] || verdict="no verdict (exit $code)"
  local linkage
  linkage="$(grep -E 'LinkageError|UnsatisfiedLinkError' "$log" | head -n 5)"
  {
    echo "### $label"
    echo
    echo "- Result: \`$verdict\` (exit $code)"
    [[ -n "$tool_options" ]] && echo "- JVM options: \`$tool_options\`"
    if [[ -n "$linkage" ]]; then
      echo "- Linkage errors:"
      echo '```text'
      echo "$linkage"
      echo '```'
    fi
    echo
  } >> "$REPORT.runs"
  rm -f "$log"
  return "$code"
}

rm -f "$REPORT.runs"
run_spike "Default JNA unpack" ""
DEFAULT_CODE=$?
if [[ -n "$JNA_DIR" ]]; then
  JNA_LINK="$(mktemp -d -t cs-ide-jna)/jna"
  ln -s "$JNA_DIR" "$JNA_LINK"
  run_spike "IDE-bundled libjnidispatch" "-Djna.boot.library.path=$JNA_LINK -Djna.nounpack=true"
  BUNDLED_CODE=$?
  rm -rf "$(dirname "$JNA_LINK")"
else
  echo "No libjnidispatch.jnilib under $IDE_APP/Contents/lib/jna; skipping the bundled-library run"
  BUNDLED_CODE=skipped
fi

{
  echo "## CS-12077 macOS spike (cs-ide watcher)"
  echo
  echo "- Date: $(date '+%Y-%m-%d %H:%M %Z')"
  echo "- macOS: $(sw_vers -productVersion) ($(uname -m))"
  echo "- IDE: $IDE_NAME $IDE_VERSION ($IDE_BUILD) at \`$IDE_APP\`"
  echo "- JBR: $JBR_VERSION"
  echo "- JBR entitlements: ${ENTITLEMENTS:-none}"
  echo "- IDE quarantine attribute: $QUARANTINE"
  echo "- IDE JNA directory: \`${JNA_DIR:-not found}\`"
  echo "- Extension API jar: \`$JAR\`"
  echo
  cat "$REPORT.runs"
} > "$REPORT"
rm -f "$REPORT.runs"

echo
echo "Report written to $REPORT"
echo "Default unpack exit: $DEFAULT_CODE, bundled library exit: $BUNDLED_CODE"
[[ "$DEFAULT_CODE" == 0 || "$BUNDLED_CODE" == 0 ]]
