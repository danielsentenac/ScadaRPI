"""Copy of the reports to a network share (SMB), e.g. smb://netdisk.virgo.infn.it/vacuum/TUBE1500W.

Configuration, in config.json:  "netdisk": {"server": ..., "share": ..., "folder": ...}
Account, in data/netdisk.credentials (kept on the board only, never in the repository):
    username=<account>
    password=<password>
    domain=<domain>          (optional)
Every local file missing on the share is uploaded; nothing is ever deleted on the share (old
reports are removed from the board only: Controller._cleanup in actions.py).
"""
import os


def credentials(path):
    out = {}
    with open(path) as f:
        for line in f:
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.rstrip("\n").split("=", 1)
                out[key.strip().lower()] = value
    if not out.get("username") or "password" not in out:
        raise ValueError("username= and password= are required in %s" % os.path.basename(path))
    return out


def sync(config, credentials_path, local_dir, extensions=(".xlsx",)):
    """Upload the missing files. Returns the list of uploaded names; raises on any failure."""
    import smbclient                                  # smbprotocol, listed in requirements.txt
    if not os.path.exists(credentials_path):
        raise FileNotFoundError("no account file %s on the board" % os.path.basename(credentials_path))
    account = credentials(credentials_path)
    user = account["username"]
    if account.get("domain"):
        user = "%s\\%s" % (account["domain"], user)
    server = config["server"]
    smbclient.register_session(server, username=user, password=account["password"], connection_timeout=15)
    try:
        remote_dir = "\\\\%s\\%s" % (server, config["share"])
        for part in [p for p in config.get("folder", "").replace("\\", "/").split("/") if p]:
            remote_dir += "\\" + part
            if not smbclient.path.isdir(remote_dir):
                smbclient.mkdir(remote_dir)
        present = set(smbclient.listdir(remote_dir))
        uploaded = []
        for name in sorted(os.listdir(local_dir)):
            if not name.endswith(tuple(extensions)) or name in present:
                continue
            with open(os.path.join(local_dir, name), "rb") as src:
                data = src.read()
            with smbclient.open_file(remote_dir + "\\" + name + ".part", mode="wb") as dst:
                dst.write(data)
            smbclient.rename(remote_dir + "\\" + name + ".part", remote_dir + "\\" + name)   # complete files only
            uploaded.append(name)
        return uploaded
    finally:
        smbclient.reset_connection_cache()
