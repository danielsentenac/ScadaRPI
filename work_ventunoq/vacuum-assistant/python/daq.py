"""Past values from the DAQ trend files (the data DataViewer shows), read through ssh.

The assistant's ssh key may only run daq/trend_read.py on a Virgo server (forced command), which
returns 1 Hz trend samples as JSON. Configuration, in config.json:
    "daq": {"host": "olserver134.virgo.infn.it", "user": "<account>", "prefix": "V1:VAC_TUBE1500W_",
            "rename": {"ROT_GA5COMST": "ROT_Ga5COMST"}}
The private key is data/daq_key on the board (never in the repository). Without it, or when the
server cannot be reached, the caller falls back on the assistant's own record.
"""
import json
import os


class DaqError(Exception):
    pass


class DaqTrend:
    def __init__(self, config, data_dir):
        self.config = config or {}
        self.key_path = os.path.join(data_dir, "daq_key")
        self.known_hosts = os.path.join(data_dir, "daq_known_hosts")
        self.rename = self.config.get("rename", {})
        self.state = "not tried yet"

    def available(self):
        return bool(self.config.get("host")) and os.path.exists(self.key_path)

    def rows(self, t0, t1, channels, max_rows=300):
        """[(unix time, {panel channel: value})] between t0 and t1, and the panel channels the DAQ
        does not hold. Raises DaqError when the DAQ cannot be read."""
        if not self.available():
            self.state = "not configured (no key file daq_key on the board)"
            raise DaqError(self.state)
        import paramiko                                   # listed in requirements.txt
        prefix = self.config.get("prefix", "")
        names = [prefix + self.rename.get(c, c) for c in channels]
        request = "%d %d %d %s" % (t0, max(1, int(t1 - t0)), max_rows, ",".join(names))
        if self.config.get("command"):                    # only needed if the key has no forced command
            request = "%s %s" % (self.config["command"], request)
        client = paramiko.SSHClient()
        if os.path.exists(self.known_hosts):
            client.load_host_keys(self.known_hosts)
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())    # the first connection records the server
        try:
            client.connect(self.config["host"], username=self.config["user"], key_filename=self.key_path,
                           timeout=10, banner_timeout=10, auth_timeout=10, allow_agent=False, look_for_keys=False)
            client.save_host_keys(self.known_hosts)
            _, out, _ = client.exec_command(request, timeout=60)
            text = out.read().decode("utf-8", "replace")
        except Exception as e:
            self.state = "DAQ not reachable: %s: %s" % (type(e).__name__, str(e)[:120])
            raise DaqError(self.state)
        finally:
            client.close()
        try:
            reply = json.loads(text.strip().splitlines()[-1])
        except (ValueError, IndexError):
            self.state = "unexpected answer from the DAQ reader"
            raise DaqError(self.state)
        if "error" in reply:
            self.state = "DAQ reader: %s" % reply["error"]
            raise DaqError(self.state)
        self.state = "ok"
        back = dict(zip(names, channels))
        rows = [(int(r[0]), dict(zip(channels, r[1:]))) for r in reply["rows"]]
        return rows, [back[m] for m in reply.get("missing", []) if m in back], reply.get("step", 1)
