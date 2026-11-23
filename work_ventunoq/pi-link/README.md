# Direct cable between the Pi (vactube1500w) and the VENTUNO Q

In service since 2026-11-23. Pi `192.168.251.1` (interface `ventuno0`, a TP-Link
UE300 USB Ethernet adapter) to VENTUNO `192.168.251.2` (`end0`, connection
`rpi-link`). The panel's Modbus server (port 1288) and the assistant (port 7000)
talk over this cable only.

On the Pi (Raspbian 11, `usb-modeswitch` installed):

1. Copy `70-ventuno-link.rules` to `/etc/udev/rules.d/`. It switches the adapter
   out of its driver CD-ROM mode at power-up and names the interface `ventuno0`
   by its hardware address (change the address in the rule for another adapter).
2. Add to `/etc/dhcpcd.conf`:

   ```
   interface ventuno0
   static ip_address=192.168.251.1/24
   nogateway
   ```
3. `sudo udevadm control --reload`, then re-plug the adapter or reboot.

Checked after a reboot of both machines: `ventuno0` comes up by itself and the
assistant answers from the Pi at `http://192.168.251.2:7000/status`.

On the VENTUNO the assistant starts at boot as the default app:
`arduino-app-cli properties set default user:vacuum-assistant`
(`... set default none` undoes it).

The Pi's other interfaces: `eth1` (Realtek USB adapter) `192.168.250.1` for the
RGA; `wlan0` on the lab Wi-Fi.
