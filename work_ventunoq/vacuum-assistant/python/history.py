"""History of the panel values, kept by the assistant (the panel itself keeps none).

One row per `period` seconds in data/history/<YYYY-MM-DD>.csv: the time (epoch seconds) and the
value of every channel given in `channels`. Files older than `keep_days` are deleted. If the
list of channels changes during a day, a new file <YYYY-MM-DD>.<n>.csv is started.
"""
import glob
import os
import re
import time


class History:
    def __init__(self, directory, channels, keep_days=30):
        self.directory, self.channels, self.keep_days = directory, list(channels), keep_days
        self.header = "time," + ",".join(self.channels)
        os.makedirs(directory, exist_ok=True)

    # ---------------------------------------------------------------- writing
    def _file_for(self, day):
        n = 0
        while True:
            path = os.path.join(self.directory, "%s%s.csv" % (day, ".%d" % n if n else ""))
            if not os.path.exists(path):
                with open(path, "w") as f:
                    f.write(self.header + "\n")
                self._purge()
                return path
            with open(path) as f:
                if f.readline().rstrip("\n") == self.header:
                    return path
            n += 1

    def record(self, values, t=None):
        t = time.time() if t is None else t
        path = self._file_for(time.strftime("%Y-%m-%d", time.localtime(t)))
        with open(path, "a") as f:
            f.write("%d,%s\n" % (t, ",".join("%.6g" % values.get(c, 0) for c in self.channels)))

    def _purge(self):
        limit = time.strftime("%Y-%m-%d", time.localtime(time.time() - self.keep_days * 86400))
        for path in glob.glob(os.path.join(self.directory, "*.csv")):
            if os.path.basename(path)[:10] < limit:
                os.remove(path)

    # ---------------------------------------------------------------- reading
    def _files(self, t0, t1):
        days = {time.strftime("%Y-%m-%d", time.localtime(t)) for t in (t0, t1)}
        t = t0
        while t < t1:
            days.add(time.strftime("%Y-%m-%d", time.localtime(t)))
            t += 43200
        return sorted(p for p in glob.glob(os.path.join(self.directory, "*.csv")) if os.path.basename(p)[:10] in days)

    def rows(self, t0, t1):
        """(time, {channel: value}) of every row with t0 <= time <= t1, oldest first."""
        out = []
        for path in self._files(t0, t1):
            with open(path) as f:
                names = f.readline().rstrip("\n").split(",")[1:]
                for line in f:
                    stamp, _, rest = line.partition(",")
                    if not stamp.isdigit() or not t0 <= int(stamp) <= t1:
                        continue
                    try:
                        out.append((int(stamp), dict(zip(names, map(float, rest.split(","))))))
                    except ValueError:
                        pass                      # a row cut by a power loss
        out.sort(key=lambda r: r[0])
        return out

    def at(self, t, tolerance):
        """The row recorded nearest to time t, within `tolerance` seconds; None if there is none."""
        near = self.rows(t - tolerance, t + tolerance)
        return min(near, key=lambda r: abs(r[0] - t)) if near else None

    def first_time(self):
        files = sorted(glob.glob(os.path.join(self.directory, "*.csv")))
        for path in files:
            with open(path) as f:
                f.readline()
                stamp = f.readline().partition(",")[0]
                if stamp.isdigit():
                    return int(stamp)
        return None


# -------------------------------------------------------- time asked in a question
UNIT = {"s": 1, "sec": 1, "second": 1, "seconds": 1, "m": 60, "mn": 60, "min": 60, "mins": 60, "minute": 60,
        "minutes": 60, "h": 3600, "hr": 3600, "hour": 3600, "hours": 3600, "d": 86400, "day": 86400, "days": 86400}
DURATION = re.compile(r"(?<![a-z0-9.:])(\d+(?:\.\d+)?)\s*(%s)\b" % "|".join(sorted(UNIT, key=len, reverse=True)))
CLOCK = re.compile(r"(?<![a-z0-9.:])([01]?\d|2[0-3])\s*(?::|h)\s*([0-5]\d)(?![0-9])")
PERIOD = re.compile(r"\b(last|past|over|during|since|trend|evolution|history|between|from)\b")


def asked_time(question, now):
    """What past time a question refers to: None, ("at", t) or ("span", t0, t1).

    "G22 1h" / "G22 1 h ago"      the value one hour ago
    "G22 at 14:30"                today at 14:30 (yesterday if that is still in the future)
    "G22 yesterday" / "yesterday at 9:00"
    "G22 last 2h" / "since 14:00" / "trend 30 min"     what happened over that period
    """
    q = question.lower()
    period = bool(PERIOD.search(q))
    yesterday = "yesterday" in q
    clock = CLOCK.search(q)
    duration = DURATION.search(q)
    t = None
    if clock:
        lt = time.localtime(now)
        t = time.mktime((lt.tm_year, lt.tm_mon, lt.tm_mday, int(clock.group(1)), int(clock.group(2)), 0, 0, 0, -1))
        if yesterday or t > now:
            t -= 86400
    elif duration:
        t = now - float(duration.group(1)) * UNIT[duration.group(2)]
    elif yesterday:
        t = now - 86400
    if t is None:
        return None
    return ("span", t, now) if period else ("at", t)
