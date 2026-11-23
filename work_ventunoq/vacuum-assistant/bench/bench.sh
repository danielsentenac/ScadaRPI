#!/bin/bash
# Runs the reference questions against the running assistant and prints answers and times.
# Usage on the board: bash bench.sh <label>   (results also saved to ~/bench-<label>.txt)
LABEL=${1:-run}
OUT=~/bench-$LABEL.txt
{
echo "=== $LABEL  $(date)"
while IFS= read -r q; do
  curl -s -m 600 -G http://localhost:7000/ask --data-urlencode "q=$q" | python3 -c '
import sys, json
r = json.load(sys.stdin)
print("Q:", r.get("question"))
print("A:", r.get("answer", r.get("error")))
a = r.get("answer") or ""
print("   [%s s, %d words, %s chars of values]" % (r.get("seconds"), len(a.split()), len(r.get("snapshot", ""))))
print()'
done <<'QUESTIONS'
What are the pressures on all gauges?
Is the turbo pump running normally?
Which gas dominates the residual gas spectrum?
What are the bake-out temperatures?
Open valve V21
What is the status of the valves?
Is any instrument not communicating?
Compare the pressure on gauge Ga2 with the RGA total pressure.
QUESTIONS
} 2>&1 | tee "$OUT"
