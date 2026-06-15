#!/usr/bin/env bash
#
# Build docs/Information_Panel_Displays_for_Virgo_Operations.pdf from the
# markdown, with print-friendly pagination.
#
# Pipeline:
#   1. pandoc  : markdown -> self-contained HTML (images + print.css embedded)
#   2. perl    : bind each bold sub-label (<p><strong>..</strong></p>) to the
#                table right below it in a <div class="keep"> so the label is
#                never stranded at a page foot away from its table
#   3. chrome  : headless print-to-PDF, A4, no browser header/footer
#
# Requires: pandoc, google-chrome (or chromium-browser), perl, pdfinfo.
# Usage:    docs/build_pdf.sh        # from anywhere; paths are script-relative
#
set -euo pipefail

DOCS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MD="$DOCS/Information_Panel_Displays_for_Virgo_Operations.md"
CSS="$DOCS/print.css"
PDF="$DOCS/Information_Panel_Displays_for_Virgo_Operations.pdf"
HTML="$(mktemp --suffix=.html)"
trap 'rm -f "$HTML"' EXIT

[[ -f "$MD"  ]] || { echo "build_pdf: markdown not found: $MD"  >&2; exit 1; }
[[ -f "$CSS" ]] || { echo "build_pdf: stylesheet not found: $CSS" >&2; exit 1; }

# Pick a Chrome/Chromium binary.
CHROME=""
for c in google-chrome google-chrome-stable chromium-browser chromium chrome; do
  command -v "$c" >/dev/null 2>&1 && { CHROME="$c"; break; }
done
[[ -n "$CHROME" ]] || { echo "build_pdf: no chrome/chromium found" >&2; exit 1; }

echo "[1/3] pandoc -> HTML"
pandoc "$MD" -s --self-contained --resource-path="$DOCS" -c "$CSS" -o "$HTML"

echo "[2/3] bind sub-label + table blocks"
# Only label-only paragraphs (<p><strong>X</strong></p>) that are immediately
# followed by a <table> are wrapped; inline bold like "Scope:"/"Note:" (which
# has text after </strong>) is left untouched.
perl -0777 -i -pe \
  's{(<p><strong>[^<]*</strong></p>)\s*(<table>.*?</table>)}{<div class="keep">$1$2</div>}gs' \
  "$HTML"

echo "[3/3] chrome -> PDF"
"$CHROME" --headless --disable-gpu --no-sandbox --print-to-pdf-no-header \
  --run-all-compositor-stages-before-draw --virtual-time-budget=30000 \
  --print-to-pdf="$PDF" "file://$HTML" 2>/dev/null

pages="$(pdfinfo "$PDF" 2>/dev/null | awk '/Pages/{print $2}')"
echo "Built $PDF (${pages:-?} pages)"
