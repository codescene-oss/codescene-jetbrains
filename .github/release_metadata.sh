#!/usr/bin/env bash
set -euo pipefail

TAG="${GITHUB_REF_NAME:-${1:-}}"
OUTPUT="${GITHUB_OUTPUT:-${2:-}}"
SERVER_URL="${GITHUB_SERVER_URL:-${3:-}}"
REPOSITORY="${GITHUB_REPOSITORY:-${4:-}}"

if [[ -z "$TAG" || -z "$OUTPUT" ]]; then
  echo "GITHUB_REF_NAME and GITHUB_OUTPUT are required." >&2
  exit 1
fi
STABLE_PATTERN='^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'
TEST_PATTERN='^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)-test\.[0-9a-f]{7,40}$'
PLUGIN_VERSION="$(awk -F '=' '/^pluginVersion[[:space:]]*=/{value=$2; gsub(/[[:space:]]/, "", value); print value}' gradle.properties)"

if [[ -z "$PLUGIN_VERSION" ]]; then
  echo "Could not read pluginVersion from gradle.properties." >&2
  exit 1
fi

if ! git show-ref --verify --quiet "refs/tags/$TAG"; then
  echo "Tag does not exist: $TAG." >&2
  exit 1
fi

if [[ "$(git cat-file -t "$TAG")" != "tag" ]]; then
  echo "Release tag $TAG must be annotated." >&2
  exit 1
fi

HEAD_SHA="$(git rev-parse HEAD)"
TAG_SHA="$(git rev-list -n 1 "$TAG")"

if [[ "$TAG_SHA" != "$HEAD_SHA" ]]; then
  echo "Tag $TAG does not identify checked out commit $HEAD_SHA." >&2
  exit 1
fi

STABLE_TAGS=()
while IFS= read -r CANDIDATE; do
  if [[ "$CANDIDATE" =~ $STABLE_PATTERN ]]; then
    STABLE_TAGS+=("$CANDIDATE")
  fi
done < <(git tag --merged HEAD --sort=-version:refname)

is_next_version() {
  local candidate="$1"
  local base="$2"
  local major_text minor_text patch_text
  IFS='.' read -r major_text minor_text patch_text <<< "$base"
  local major=$((10#$major_text))
  local minor=$((10#$minor_text))
  local patch=$((10#$patch_text))
  [[ "$candidate" == "$major.$minor.$((patch + 1))" ||
     "$candidate" == "$major.$((minor + 1)).0" ||
     "$candidate" == "$((major + 1)).0.0" ]]
}

VERSION=""
IS_TEST=false
NOTES_START_TAG=""
CUMULATIVE_NOTES=""

if [[ "$TAG" =~ $STABLE_PATTERN ]]; then
  VERSION="${TAG#v}"
  if [[ "$VERSION" != "$PLUGIN_VERSION" ]]; then
    echo "Stable tag $TAG does not match pluginVersion $PLUGIN_VERSION." >&2
    exit 1
  fi

  for CANDIDATE in "${STABLE_TAGS[@]}"; do
    if [[ "$CANDIDATE" != "$TAG" ]]; then
      NOTES_START_TAG="$CANDIDATE"
      break
    fi
  done

  if [[ -n "$NOTES_START_TAG" ]] && ! is_next_version "$VERSION" "${NOTES_START_TAG#v}"; then
    echo "Stable version $VERSION is not the next patch, minor, or major version after ${NOTES_START_TAG#v}." >&2
    exit 1
  fi
elif [[ "$TAG" =~ $TEST_PATTERN ]]; then
  VERSION="${TAG#v}"
  TEST_BASE="${VERSION%%-test.*}"
  TEST_SHA="${VERSION##*.}"
  IS_TEST=true
  LATEST_STABLE="${STABLE_TAGS[0]:-}"

  if [[ "$HEAD_SHA" != "$TEST_SHA"* ]]; then
    echo "Test tag SHA $TEST_SHA does not identify tagged commit $HEAD_SHA." >&2
    exit 1
  fi

  if [[ "$LATEST_STABLE" != "v$PLUGIN_VERSION" ]]; then
    echo "pluginVersion $PLUGIN_VERSION does not match latest stable tag ${LATEST_STABLE:-<none>}." >&2
    exit 1
  fi

  if ! is_next_version "$TEST_BASE" "$PLUGIN_VERSION"; then
    echo "Test base $TEST_BASE is not the next patch, minor, or major version after $PLUGIN_VERSION." >&2
    exit 1
  fi

  while IFS= read -r CANDIDATE; do
    if [[ "$CANDIDATE" != "$TAG" && "$CANDIDATE" =~ $TEST_PATTERN ]]; then
      CANDIDATE_SHA="${CANDIDATE##*.}"
      CANDIDATE_COMMIT="$(git rev-list -n 1 "$CANDIDATE")"
      if [[ "$(git cat-file -t "$CANDIDATE")" == "tag" && "$CANDIDATE_COMMIT" == "$CANDIDATE_SHA"* ]]; then
        NOTES_START_TAG="$CANDIDATE"
        break
      fi
    fi
  done < <(git tag --merged HEAD --list "v${TEST_BASE}-test.*" --sort=-creatordate)

  if [[ -z "$NOTES_START_TAG" ]]; then
    NOTES_START_TAG="$LATEST_STABLE"
  fi

  if [[ -z "$SERVER_URL" || -z "$REPOSITORY" ]]; then
    echo "GITHUB_SERVER_URL and GITHUB_REPOSITORY are required for test releases." >&2
    exit 1
  fi

  COMPARE_URL="${SERVER_URL}/${REPOSITORY}/compare/${LATEST_STABLE}...${TAG}"
  CUMULATIVE_NOTES="[Full changes since ${LATEST_STABLE}](${COMPARE_URL})"
else
  echo "Tag must match vX.Y.Z or vX.Y.Z-test.<hex-sha>; got $TAG." >&2
  exit 1
fi

echo "version=$VERSION" >> "$OUTPUT"
echo "is_test=$IS_TEST" >> "$OUTPUT"
echo "notes_start_tag=$NOTES_START_TAG" >> "$OUTPUT"
echo "cumulative_notes=$CUMULATIVE_NOTES" >> "$OUTPUT"
