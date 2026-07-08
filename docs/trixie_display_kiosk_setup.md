# Raspberry Pi OS Trixie Display Setup: Auto-Hiding Taskbar and Mouse Cursor

This note describes how to configure a Raspberry Pi OS **Trixie** (Debian 13,
64-bit) display Pi so that the desktop behaves like a kiosk around a
full-screen SCADARPI panel:

- the **taskbar** stays hidden and slides back down when the mouse touches the
  top edge of the screen;
- the **mouse cursor** disappears after a few seconds without movement and
  reappears as soon as the mouse moves.

The procedure was first applied to `vacmoni10` (particle-monitoring display,
2026-07). It applies to the stock Trixie desktop session: **labwc** (Wayland
compositor) with **wf-panel-pi** as the taskbar.

## 1. Why the stock taskbar autohide is broken

wf-panel-pi has a built-in autohide option (`autohide=true` in
`~/.config/wf-panel-pi/wf-panel-pi.ini`, also exposed in the GUI panel
settings). Hiding works, but the panel **never comes back on mouse hover**:
the reveal is implemented with the Wayfire-specific `zwf_hotspot_v2` Wayland
protocol (see `src/panel/wf-autohide-window.cpp` in the wf-panel-pi sources),
and labwc does not implement that protocol — checked on labwc 0.9.7 and 0.9.8
as shipped by Raspberry Pi. The hotspot creation is silently skipped, so an
autohidden panel is unreachable except by editing the config file.

Two properties make a workaround possible:

- wf-panel-pi **watches its config file**: flipping `autohide` in the ini
  shows/hides the panel immediately, no restart needed;
- any client can place its own input surface on the screen edge via the
  standard **wlr-layer-shell** protocol, which labwc does implement.

## 2. Installing packages on a Pi without internet access

The display Pis on the Virgo network have no route to the internet, so
`apt install` fails. Download the `.deb` files on a workstation (plain HTTP
avoids TLS/CA trouble), copy them over, and install with `dpkg`:

```bash
# on the workstation
curl -O http://deb.debian.org/debian/pool/main/g/gtk-layer-shell/gir1.2-gtklayershell-0.1_0.9.0-2_arm64.deb
curl -O http://deb.debian.org/debian/pool/main/w/wtype/wtype_0.4-3+b2_arm64.deb
curl -O http://deb.debian.org/debian/pool/main/w/wlrctl/wlrctl_0.2.2-2_arm64.deb   # optional, for testing
scp *.deb pi@<display-pi>:/tmp/

# on the Pi
sudo dpkg -i /tmp/gir1.2-gtklayershell-0.1_0.9.0-2_arm64.deb /tmp/wtype_0.4-3+b2_arm64.deb /tmp/wlrctl_0.2.2-2_arm64.deb
```

Match the `gir1.2-gtklayershell` version to the installed
`libgtk-layer-shell0` (`dpkg -l libgtk-layer-shell0`). `swayidle`, `grim` and
`python3-gi` are already part of the stock Trixie image.

## 3. Auto-hiding taskbar with hover reveal

### 3.1 Panel configuration

Add to the `[panel]` section of `~/.config/wf-panel-pi/wf-panel-pi.ini`:

```ini
autohide=true
autohide_duration=500
```

### 3.2 Hover helper

Install the helper script as `/home/pi/.local/bin/taskbar-hover.py`
(executable). It keeps an invisible 2-pixel layer-shell strip on the top
edge; touching it with the mouse flips `autohide=false` (panel slides down),
and after `SHOW_SECONDS` it flips back to `true` (panel hides). Panel menus
that are open keep the panel visible regardless.

```python
#!/usr/bin/env python3
"""Hover-to-reveal for wf-panel-pi under labwc.

labwc does not implement the Wayfire zwf_hotspot_v2 protocol that
wf-panel-pi relies on to un-hide an autohidden taskbar, so the panel can
hide but never come back. This helper keeps an invisible 2 px strip at
the top screen edge; touching it with the mouse flips 'autohide' to
false in wf-panel-pi.ini (the panel watches its config and slides down
immediately). After SHOW_SECONDS it flips back to true and the panel
hides again. Open panel menus keep it visible regardless.
"""
import gi
gi.require_version('Gtk', '3.0')
gi.require_version('GtkLayerShell', '0.1')
from gi.repository import Gtk, Gdk, GLib, GtkLayerShell
from pathlib import Path

INI = Path.home() / '.config/wf-panel-pi/wf-panel-pi.ini'
SHOW_SECONDS = 6


def set_autohide(value):
    old = 'autohide=false' if value else 'autohide=true'
    new = 'autohide=true' if value else 'autohide=false'
    text = INI.read_text()
    if old in text:
        INI.write_text(text.replace(old, new))


class Strip(Gtk.Window):
    def __init__(self):
        super().__init__()
        GtkLayerShell.init_for_window(self)
        GtkLayerShell.set_layer(self, GtkLayerShell.Layer.OVERLAY)
        GtkLayerShell.set_namespace(self, 'taskbar-hotspot')
        for edge in (GtkLayerShell.Edge.TOP,
                     GtkLayerShell.Edge.LEFT,
                     GtkLayerShell.Edge.RIGHT):
            GtkLayerShell.set_anchor(self, edge, True)
        self.set_size_request(-1, 2)
        visual = self.get_screen().get_rgba_visual()
        if visual:
            self.set_visual(visual)
        self.add_events(Gdk.EventMask.ENTER_NOTIFY_MASK |
                        Gdk.EventMask.POINTER_MOTION_MASK)
        self.connect('enter-notify-event', self.on_enter)
        self.connect('motion-notify-event', self.on_enter)
        self.hide_timer = None

    def on_enter(self, *args):
        set_autohide(False)      # reveal the panel
        self.hide()              # don't steal clicks from it
        if self.hide_timer:
            GLib.source_remove(self.hide_timer)
        self.hide_timer = GLib.timeout_add_seconds(SHOW_SECONDS, self.rearm)
        return True

    def rearm(self):
        set_autohide(True)       # panel hides again
        self.hide_timer = None
        self.show_all()          # re-arm the hotspot
        return False


css = Gtk.CssProvider()
css.load_from_data(b'window { background-color: rgba(0,0,0,0); }')
Gtk.StyleContext.add_provider_for_screen(
    Gdk.Screen.get_default(), css,
    Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION)

set_autohide(True)
Strip().show_all()
Gtk.main()
```

The reveal delay is the `SHOW_SECONDS` constant at the top of the script.

## 4. Hiding the mouse cursor when idle

labwc provides a `HideCursor` **action** (cursor reappears on the next mouse
motion), but no idle trigger for it. The chain used instead:

1. **swayidle** detects input idleness (labwc implements the
   `ext_idle_notifier_v1` protocol);
2. on timeout it runs **wtype** to inject a synthetic **F13** key press
   (a key absent from real keyboards, so no conflict);
3. a labwc **keybind** maps F13 to the `HideCursor` action.

### 4.1 Keybind

The Raspberry Pi session starts labwc with `-m` (merge-config), so the user
`~/.config/labwc/rc.xml` is **merged** with the system
`/etc/xdg/labwc/rc.xml` — it only needs to contain the additional keybind.
Add inside `<openbox_config>`:

```xml
<keyboard>
  <keybind key="F13"><action name="HideCursor"/></keybind>
</keyboard>
```

Reload labwc with `labwc -r` (or `pkill -HUP -x labwc`).

### 4.2 Idle watcher

The swayidle command (8 s timeout):

```bash
swayidle timeout 8 'wtype -P f13 -p f13'
```

## 5. Autostart

Do **not** create `~/.config/labwc/autostart`: with merge-config the
semantics for autostart files are ambiguous and risk double-starting or
losing session components (panel, file manager, kanshi). Use standard XDG
autostart instead — the system labwc autostart runs
`lxsession-xdg-autostart`, which launches `~/.config/autostart/*.desktop`.
The `flock` guard prevents accidental double instances.

`~/.config/autostart/taskbar-hover.desktop`:

```ini
[Desktop Entry]
Type=Application
Name=Taskbar hover reveal
Exec=flock -n /tmp/taskbar-hover.lock /usr/bin/python3 /home/pi/.local/bin/taskbar-hover.py
X-GNOME-Autostart-enabled=true
```

`~/.config/autostart/cursor-idle-hide.desktop`:

```ini
[Desktop Entry]
Type=Application
Name=Hide cursor when idle
Exec=flock -n /tmp/cursor-idle-hide.lock swayidle timeout 8 "wtype -P f13 -p f13"
X-GNOME-Autostart-enabled=true
```

Reboot and verify both processes are up:

```bash
pgrep -af 'taskbar-hover|swayidle'
```

## 6. Testing over ssh (no physical mouse needed)

Both behaviors can be exercised remotely with a virtual pointer (`wlrctl`)
and screenshots (`grim`; `-c` includes the cursor in the capture). All
commands need the session environment:

```bash
export XDG_RUNTIME_DIR=/run/user/1000 WAYLAND_DISPLAY=wayland-0

# taskbar: push pointer to the top edge, panel should appear
wlrctl pointer move 0 -3000; sleep 1; grim /tmp/shot1.png
# move away, panel should hide after SHOW_SECONDS
wlrctl pointer move 0 400; sleep 8; grim /tmp/shot2.png

# cursor: move (cursor visible), then idle past the timeout (cursor gone)
wlrctl pointer move -200 -200; grim -c /tmp/cur1.png
sleep 10; grim -c /tmp/cur2.png
```

Pitfall when restarting the helpers over ssh: `pkill -f <pattern>` matches
the *remote shell's own command line* if the pattern appears anywhere in it
(e.g. in a later `nohup ... taskbar-hover.py` of the same compound command)
and kills the session. Run the `pkill` in a separate ssh invocation.
