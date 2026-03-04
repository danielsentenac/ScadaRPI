#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REMOTE="${REMOTE:-origin}"
PAGES_BRANCH="${PAGES_BRANCH:-gh-pages}"
SITE_SUBDIR="${SITE_SUBDIR:-api}"
GENERATED_HTML_DIR="${GENERATED_HTML_DIR:-$ROOT_DIR/build/api-docs/html}"

die() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

origin_url() {
  git -C "$ROOT_DIR" remote get-url "$REMOTE" 2>/dev/null || true
}

pages_url() {
  local url owner repo
  url="$(origin_url)"
  if [[ "$url" =~ github\.com[:/]([^/]+)/([^.]+)(\.git)?$ ]]; then
    owner="${BASH_REMATCH[1]}"
    repo="${BASH_REMATCH[2]}"
    printf 'https://%s.github.io/%s/%s/\n' "$owner" "$repo" "$SITE_SUBDIR"
  fi
}

publish_message() {
  printf 'Publish API docs (%s UTC)\n' "$(date -u +'%Y-%m-%d %H:%M:%S')"
}

prepare_worktree() {
  local wt="$1"
  if git -C "$ROOT_DIR" show-ref --verify --quiet "refs/heads/$PAGES_BRANCH"; then
    git -C "$ROOT_DIR" worktree add "$wt" "$PAGES_BRANCH" >/dev/null
    return
  fi

  if git -C "$ROOT_DIR" ls-remote --exit-code --heads "$REMOTE" "$PAGES_BRANCH" >/dev/null 2>&1; then
    git -C "$ROOT_DIR" fetch "$REMOTE" "$PAGES_BRANCH:$PAGES_BRANCH"
    git -C "$ROOT_DIR" worktree add "$wt" "$PAGES_BRANCH" >/dev/null
    return
  fi

  git -C "$ROOT_DIR" worktree add --detach "$wt" >/dev/null
  git -C "$wt" checkout --orphan "$PAGES_BRANCH" >/dev/null
  find "$wt" -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +
}

main() {
  local wt publish_url

  command -v git >/dev/null 2>&1 || die "git not found in PATH."
  "$ROOT_DIR/tools/generate_api_docs.sh"
  [[ -d "$GENERATED_HTML_DIR" ]] || die "Generated HTML directory not found: $GENERATED_HTML_DIR"

  wt="$(mktemp -d)"
  trap 'set +e; git -C "$ROOT_DIR" worktree remove --force "'"$wt"'" >/dev/null 2>&1 || true; rm -rf "'"$wt"'"' EXIT

  prepare_worktree "$wt"

  # Keep .git and rebuild publish contents.
  find "$wt" -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +
  mkdir -p "$wt/$SITE_SUBDIR"
  cp -a "$GENERATED_HTML_DIR"/. "$wt/$SITE_SUBDIR"/
  touch "$wt/.nojekyll"

  cat > "$wt/index.html" <<EOF
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="refresh" content="0; url=./${SITE_SUBDIR}/">
    <title>SCADARPI API Docs</title>
  </head>
  <body>
    <p>Redirecting to <a href="./${SITE_SUBDIR}/">API documentation</a>...</p>
  </body>
</html>
EOF

  git -C "$wt" add -A
  if git -C "$wt" diff --cached --quiet; then
    echo "[api-docs] no changes to publish."
    return
  fi

  git -C "$wt" commit -m "$(publish_message)"
  git -C "$wt" push "$REMOTE" "HEAD:$PAGES_BRANCH"

  publish_url="$(pages_url || true)"
  if [[ -n "$publish_url" ]]; then
    echo "[api-docs] published: $publish_url"
  else
    echo "[api-docs] published branch '$PAGES_BRANCH' to remote '$REMOTE'."
  fi
}

main "$@"
