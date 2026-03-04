#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOXYFILE_PATH="${DOXYFILE_PATH:-$ROOT_DIR/docs/Doxyfile}"
OUTPUT_HTML_DIR="${OUTPUT_HTML_DIR:-$ROOT_DIR/build/api-docs/html}"
declare -a STATIC_DOC_ASSETS=(
  "Illustration.png"
  "docs/architecture_schema.png"
)

die() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

command -v doxygen >/dev/null 2>&1 || die "doxygen not found in PATH."

if ! command -v dot >/dev/null 2>&1; then
  echo "Warning: Graphviz 'dot' not found. Class diagrams may be disabled."
fi

[[ -f "$DOXYFILE_PATH" ]] || die "Doxyfile not found: $DOXYFILE_PATH"
mkdir -p "$ROOT_DIR/build/api-docs"
rm -rf "$OUTPUT_HTML_DIR"

echo "[api-docs] generating HTML documentation with doxygen..."
(
  cd "$ROOT_DIR"
  doxygen "$DOXYFILE_PATH"
)

[[ -f "$OUTPUT_HTML_DIR/index.html" ]] || die "Missing generated index: $OUTPUT_HTML_DIR/index.html"

for asset in "${STATIC_DOC_ASSETS[@]}"; do
  src="$ROOT_DIR/$asset"
  dst="$OUTPUT_HTML_DIR/$asset"
  if [[ -f "$src" ]]; then
    mkdir -p "$(dirname "$dst")"
    cp -f "$src" "$dst"
  else
    echo "Warning: static docs asset not found, skipping: $asset"
  fi
done

echo "[api-docs] done: $OUTPUT_HTML_DIR/index.html"
