#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOXYFILE_PATH="${DOXYFILE_PATH:-$ROOT_DIR/docs/Doxyfile}"
OUTPUT_HTML_DIR="${OUTPUT_HTML_DIR:-$ROOT_DIR/build/api-docs/html}"
MODULE_BUILD_DIR="${MODULE_BUILD_DIR:-$ROOT_DIR/build/api-docs/.module-build}"
DOC_MODULES="${DOC_MODULES:-}"
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

discover_modules() {
  local -a all_modules
  local -a selected
  local normalized module
  mapfile -t all_modules < <(find "$ROOT_DIR" -maxdepth 1 -mindepth 1 -type d -name 'work_*' -printf '%f\n' | sort)

  [[ ${#all_modules[@]} -gt 0 ]] || die "No work_* modules found."

  if [[ -z "$DOC_MODULES" ]]; then
    printf '%s\n' "${all_modules[@]}"
    return
  fi

  normalized="${DOC_MODULES//,/ }"
  for module in $normalized; do
    [[ -n "$module" ]] || continue
    if [[ "$module" != work_* ]]; then
      module="work_$module"
    fi
    if [[ -d "$ROOT_DIR/$module" ]]; then
      selected+=("$module")
    else
      echo "Warning: requested module '$module' does not exist, skipping."
    fi
  done

  [[ ${#selected[@]} -gt 0 ]] || die "No valid modules left after DOC_MODULES filter."
  printf '%s\n' "${selected[@]}"
}

module_has_java() {
  local module="$1"
  local first_java
  first_java="$(find "$ROOT_DIR/$module" -type f -name '*.java' -print -quit)"
  [[ -n "$first_java" ]]
}

copy_static_assets() {
  local destination_root="$1"
  local src dst asset
  for asset in "${STATIC_DOC_ASSETS[@]}"; do
    src="$ROOT_DIR/$asset"
    dst="$destination_root/$asset"
    if [[ -f "$src" ]]; then
      mkdir -p "$(dirname "$dst")"
      cp -f "$src" "$dst"
    else
      echo "Warning: static docs asset not found, skipping: $asset"
    fi
  done
}

generate_module_docs() {
  local module="$1"
  local module_source_dir="$ROOT_DIR/$module"
  local module_output_dir="$MODULE_BUILD_DIR/$module"
  local module_html_dir="$module_output_dir/html"
  local tmp_doxyfile

  tmp_doxyfile="$(mktemp)"
  cat "$DOXYFILE_PATH" > "$tmp_doxyfile"
  cat >> "$tmp_doxyfile" <<EOF
PROJECT_NAME           = "SCADARPI API (${module})"
PROJECT_BRIEF          = "API documentation for ${module}"
OUTPUT_DIRECTORY       = "$module_output_dir"
INPUT                  = "$module_source_dir" \
                         "$ROOT_DIR/README.md"
STRIP_FROM_PATH        = "$ROOT_DIR"
USE_MDFILE_AS_MAINPAGE = README.md
EXCLUDE_PATTERNS       = */.git/* \
                         */build/* \
                         */lib/* \
                         */docs/theme/* \
                         */docs/doxygen/* \
                         */copy/*
EOF

  echo "[api-docs] generating module: $module"
  (
    cd "$ROOT_DIR"
    doxygen "$tmp_doxyfile"
  )
  rm -f "$tmp_doxyfile"

  [[ -f "$module_html_dir/index.html" ]] || die "Missing generated index for $module: $module_html_dir/index.html"
  rm -rf "$OUTPUT_HTML_DIR/$module"
  mkdir -p "$OUTPUT_HTML_DIR/$module"
  cp -a "$module_html_dir"/. "$OUTPUT_HTML_DIR/$module"/
  copy_static_assets "$OUTPUT_HTML_DIR/$module"
}

write_landing_page() {
  local index_path="$1"
  shift
  local -a modules=("$@")
  local module short_name generated_at

  generated_at="$(date -u +'%Y-%m-%d %H:%M:%S UTC')"

  cat > "$index_path" <<EOF
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SCADARPI API Docs</title>
    <style>
      body {
        font-family: "Segoe UI", Tahoma, sans-serif;
        margin: 0;
        background: #f4f8fb;
        color: #1f2d3d;
      }
      .wrap {
        max-width: 900px;
        margin: 0 auto;
        padding: 24px;
      }
      h1 {
        margin: 0 0 6px;
      }
      .meta {
        margin: 0 0 18px;
        color: #4e6479;
      }
      ul {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 10px;
      }
      li a {
        display: block;
        text-decoration: none;
        background: #ffffff;
        border: 1px solid #d4e1ec;
        border-radius: 8px;
        padding: 12px 14px;
        color: #0f4065;
        font-weight: 600;
      }
      li a:hover {
        border-color: #8cb2cf;
        background: #f8fbfe;
      }
      .sub {
        display: block;
        font-weight: 400;
        color: #5c7286;
        margin-top: 2px;
      }
    </style>
  </head>
  <body>
    <div class="wrap">
      <h1>SCADARPI API Documentation</h1>
      <p class="meta">Generated per module to avoid class-name collisions. Updated: $generated_at</p>
      <ul>
EOF

  for module in "${modules[@]}"; do
    short_name="${module#work_}"
    cat >> "$index_path" <<EOF
        <li><a href="./$module/">$module<span class="sub">module: $short_name</span></a></li>
EOF
  done

  cat >> "$index_path" <<EOF
      </ul>
    </div>
  </body>
</html>
EOF
}

main() {
  local module
  local -a modules
  local -a generated_modules

  mapfile -t modules < <(discover_modules)

  mkdir -p "$ROOT_DIR/build/api-docs"
  rm -rf "$OUTPUT_HTML_DIR" "$MODULE_BUILD_DIR"
  mkdir -p "$OUTPUT_HTML_DIR" "$MODULE_BUILD_DIR"

  for module in "${modules[@]}"; do
    if ! module_has_java "$module"; then
      echo "[api-docs] skipping module without java sources: $module"
      continue
    fi
    generate_module_docs "$module"
    generated_modules+=("$module")
  done

  [[ ${#generated_modules[@]} -gt 0 ]] || die "No module docs generated."
  write_landing_page "$OUTPUT_HTML_DIR/index.html" "${generated_modules[@]}"
  echo "[api-docs] done: $OUTPUT_HTML_DIR/index.html"
}

main "$@"
