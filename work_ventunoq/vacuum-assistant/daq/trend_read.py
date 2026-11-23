#!/usr/bin/env python
"""Read-only access to the DAQ trend files for the vacuum assistant (runs on a Virgo server).

It is the only thing the assistant's ssh key may run there (forced command in authorized_keys):
    command="/cvmfs/software.igwn.org/conda/envs/igwn-py39/bin/python <path>/trend_read.py",no-pty,
    no-port-forwarding,no-agent-forwarding,no-X11-forwarding ssh-ed25519 AAAA... vacuum-assistant

Request (ssh command line, or arguments):  <unix start> <duration s> <max rows> <channel,channel,...>
Answer (one JSON object):  {"step": s, "rows": [[unix time, value, value, ...], ...], "missing": [...]}
The values are the 1 Hz samples of the trend frames, one row every `step` seconds; a channel absent
from the frames at some time (rack not acquired) gives no row for that time. Nothing is ever written.
"""
import json
import os
import re
import sys

FFL = "/virgoData/ffl/trend.ffl"
GPS_MINUS_UNIX = -315964800 + 18          # GPS epoch and leap seconds
MAX_SPAN, MAX_CHANNELS, MAX_ROWS = 40 * 86400, 80, 2000


def main(argv):
    if len(argv) != 4:
        return {"error": "expected: <unix start> <duration s> <max rows> <channels>"}
    if not re.fullmatch(r"\d{9,11}", argv[0]) or not re.fullmatch(r"\d{1,8}", argv[1]) or not re.fullmatch(r"\d{1,5}", argv[2]):
        return {"error": "bad numbers"}
    start, duration, max_rows = int(argv[0]), int(argv[1]), int(argv[2])
    channels = argv[3].split(",")
    if not 1 <= duration <= MAX_SPAN or not 1 <= max_rows <= MAX_ROWS or not 1 <= len(channels) <= MAX_CHANNELS:
        return {"error": "request out of limits"}
    if any(not re.fullmatch(r"V1:[A-Za-z0-9_]{1,80}", c) for c in channels):
        return {"error": "bad channel name"}
    from virgotools import getChannel
    step = max(1, -(-duration // max_rows))
    chunk = max(200, min(86400, duration // 10 or 200))     # a missing period only loses its own chunk
    rows, missing = [], set()
    t = start
    while t < start + duration:
        n = min(chunk, start + duration - t)
        columns = []
        for c in channels:
            try:
                columns.append(getChannel(FFL, c, t + GPS_MINUS_UNIX, n).data)
            except Exception:
                missing.add(c)
                columns = None
                break
        if columns:
            first = (-(t - start)) % step                    # keep the rows on the same grid in every chunk
            for i in range(first, min(len(col) for col in columns), step):
                row = [float(col[i]) for col in columns]
                if all(v == v for v in row):                 # no row where the frames hold no value (NaN)
                    rows.append([t + i] + row)
        t += n
    return {"step": step, "rows": rows, "missing": sorted(missing)}


if __name__ == "__main__":
    request = os.environ.get("SSH_ORIGINAL_COMMAND", "").split() or sys.argv[1:]
    try:
        out = main(request)
    except Exception as e:                                     # never a traceback to the caller
        out = {"error": "%s: %s" % (type(e).__name__, str(e)[:200])}
    sys.stdout.write(json.dumps(out) + "\n")
