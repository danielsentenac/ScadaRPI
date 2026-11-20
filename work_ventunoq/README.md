# work_ventunoq — local AI on an Arduino VENTUNO Q for the vactube1500w experiment

Status as of **2026-11-20**. This folder collects everything done so far on the
VENTUNO Q board (`ventunoq1`): setup notes, test programs, scripts and reference
documents. Nothing here is deployed in production yet.

## Goal

Run an open-source AI model **locally** on an Arduino VENTUNO Q and let it
supervise / drive the vactube1500w vacuum experiment, which is today controlled
by a Raspberry Pi running the `work_tube1500w` panel (Java 8 + pi4j 1.2 + GLG).

Two architectures are under discussion (no decision yet):

| | A — Pi stays the control layer | B — VENTUNO replaces the Pi |
|---|---|---|
| Link | Direct Ethernet cable, Modbus TCP (the Pi panel already serves it) | none (single board) |
| Work | Small: tool layer on the VENTUNO | Port of the panel's hardware layer off pi4j 1.2 |
| Risk | AI and control stay independent | AI software, Docker and Arduino updates share the machine with the vacuum control |

For B (and for a future Raspberry Pi 5), the idea is to move the panel to
**Pi4J 4.x**, which runs on 64-bit boards through standard Linux interfaces.

## Alternative considered: running the model on the Raspberry Pi itself

Discussed on 2026-11-20, with the VENTUNO set aside for the moment. The Pi
speed figures are **estimates** from general experience (CPU-only llama.cpp);
only the VENTUNO figure was measured. Nothing was tested on a Pi.

| | Model speed (4B model) | Panel software | Extra hardware |
|---|---|---|---|
| Pi 3 B+ (1 GB) | Not feasible | Runs as is | None |
| Pi 4 (8 GB) | About 1 word/s | Runs as is (32-bit system; 64-bit kernel to test) | None |
| Pi 5 (8–16 GB) | About 3–5 words/s | Needs the Pi4J 4 port | Cooler, 5 V / 5 A supply |
| Pi 3 or 4 + VENTUNO | 12 words/s (measured) | Runs as is | Ethernet link between the two |

- **Pi 3 B+:** 1 GB of RAM in total. Even a 1B model (about 1 GB) does not fit
  beside the panel; only tiny models would, and they are too unreliable to
  summarise a vacuum state.
- **Pi 4:** no AI accelerator, four Cortex-A72 cores. A 1B model (Gemma 3 1B,
  Qwen 3.5 0.8B) should give a few words per second; a 4B model needs the 8 GB
  board and is only usable for slow background tasks. The current SD card is
  32-bit Raspbian (32-bit Java 8, pi4j 1.2) while model runtimes need 64-bit:
  the least disruptive route is the 64-bit kernel with the 32-bit system, the
  model running as a self-contained 64-bit program. pi4j 1.2 under the 64-bit
  kernel is untested. The model must be limited (cores, priority) because the
  panel's serial polling and bit-banged thermocouple reads are delay-sensitive.
- **Pi 5:** cores two to three times faster than the Pi 4, 8 or 16 GB, still
  CPU only. wiringPi / pi4j 1.2 do not work on it (RP1 GPIO chip), so the panel
  needs the same port as for the VENTUNO (Pi4J 4, `jSerialComm`, recent 64-bit
  Java). Unlike the VENTUNO, the Pi 5 is Pi4J's officially supported platform
  and its header I2C bus works out of the box.

Summary: for a single board doing both jobs, the Pi 5 is the better target and
the Pi4J 4 port becomes the main work. For the best model with the least change
to the control side, a Pi plus the VENTUNO remains the stronger combination.

## The board

- Arduino VENTUNO Q, name `ventunoq1`, USB serial `2177781137`.
- Linux side: Qualcomm Dragonwing IQ8 (QCS8275), 16 GB RAM, 55 GB eMMC,
  Ubuntu 24.04.4, kernel `6.8.0-1078-qcom`, aarch64 only.
- MCU side: STM32H5F5 (Zephyr), reached through Arduino's Bridge (`arduino-router`).
- Arduino software: `arduino-app-cli` 0.14.1, `arduino-linux-config` 0.3.0, Docker.
- User `arduino` (password chosen by the owner at first setup; not recorded here).

Reference: `docs/ventuno-q-user-manual.md` (copy of Arduino's manual, from
<https://github.com/arduino/docs-content>, path
`content/hardware/ventuno/boards/ventuno-q/tutorials/first-steps/user-manual/user-manual.md`).

## What has been set up

### Access
- **ssh:** `ssh ventunoq1` from the development PC. Key `~/.ssh/id_ed25519_ventunoq`,
  `Host ventunoq1` entry in `~/.ssh/config` pointing at the Wi-Fi address
  `172.31.10.187` (DHCP — may change).
- **adb over USB-C:** works from the development PC after adding
  `/etc/udev/rules.d/99-arduino-adb.rules` there (vendor `2341`, mode 0666).
  The board must be powered by the barrel jack so that USB-C is free.
- The two USB-A ports are host-only; they cannot be used to reach the board from a PC.

### First setup
- Done on the board's own screen: terms accepted, password, name `ventunoq1`, updates.
- Keyboard layout: Italian (`arduino-app-cli system keyboard it`).
- HDMI: the board outputs standard 1920x1080@60 (148.5 MHz). A "timing not
  supported" message on the Dell U2312HM was caused by a **too long cable**,
  not by the mode. `board-scripts/drmstate.py` reads the current video timing without root.

### Network
- **Wi-Fi `EGO`** (WPA2-Enterprise, PEAP/MSCHAPv2, no CA check). The board's
  first-setup screen cannot enter enterprise credentials, so the profile was
  created by hand:
  ```
  nmcli con add type wifi ifname wlp3s0 con-name EGO ssid EGO \
    wifi-sec.key-mgmt wpa-eap 802-1x.eap peap 802-1x.phase2-auth mschapv2 \
    802-1x.identity <user> 802-1x.anonymous-identity <user> 802-1x.system-ca-certs no
  nmcli --ask con up EGO        # asks for the password and stores it
  ```
- **Ethernet `rpi-link`** (prepared for architecture A): fixed `192.168.251.2/24`
  on `end0`, never the default route, autoconnect priority 10. Planned peer:
  the Pi at `192.168.251.1` on an extra USB-Ethernet dongle (not done; the RGA
  already uses `192.168.250.x` on the Pi's `eth1`).

### Local AI model
- Installed: **Qwen 3 4B Instruct** (`genie:qwen3_4b_instruct_2507`, Apache-2.0),
  via `arduino-app-cli model install genie:qwen3_4b_instruct_2507`.
- It is the default model of the `arduino:llm` brick on this board and runs in
  the Qualcomm Genie runner container. Whether inference is really on the NPU
  was not verified.
- Test app `llm-test/` (on the board: `~/ArduinoApps/llm-test`):
  ```
  arduino-app-cli app start user:llm-test
  arduino-app-cli app logs  user:llm-test
  arduino-app-cli app stop  user:llm-test
  ```
  Result: first call 24.6 s (model load), then a 49-word answer in 4.1 s
  (about 12 words/s). The runner uses about 3.2 GB RAM while loaded.
- Other relevant bricks on the board: `arduino:mcp_client` (lets the model call
  external tools), `arduino:dbstorage_tsstore` (InfluxDB), `arduino:web_ui`.
- Other models in the catalogue (not installed): Qwen 3-VL 4B/8B, Qwen 2.5-VL 7B,
  Gemma 4 E2B/E4B, Gemma 3 1B, Qwen 3.5 0.8B.

## Hardware access tests (for architecture B)

### What the Pi panel uses today (`work_tube1500w`)
- pi4j 1.2 (wiringPi, 32-bit, Raspberry-only) for everything.
- Serial ports (gauges, Sicem) — about 10 source files.
- I2C bus 1 (slave modules) — `I2C_Comm.java`.
- Seven bit-banged GPIO lines for the thermocouple multiplexer — `TCMuxShield.java`.

wiringPi / pi4j 1.x **cannot** run on the VENTUNO (Broadcom-specific, 32-bit).

### 40-pin header
The header is wired to the Linux processor through 1.8 V → 3.3 V level
translators, in the Raspberry Pi layout (manual, section "RPi 40-Pin Header").
GPIO chip for the header lines: `/dev/gpiochip2` (`f100000.pinctrl`); line
offset = the `MD_GPIO_n` number.

| Use in the Pi panel | wiringPi pin | Header pin | VENTUNO line | Status |
|---|---|---|---|---|
| I2C SDA / SCL | — | 3 / 5 | MD_GPIO_17 / 18 (`i2c0`) | bus disabled in the device tree |
| TCMux `pinso` (input) | 0 | 11 | MD_GPIO_85 | shared with the Bluetooth UART — unusable while Bluetooth is on |
| TCMux `pinen` | 2 | 13 | MD_GPIO_109 | read + output tested OK |
| TCMux `pina0` | 5 | 18 | MD_GPIO_106 | read tested OK |
| TCMux `pina1` | 6 | 22 | MD_GPIO_107 | read tested OK |
| TCMux `pina2` | 21 | 29 | MD_GPIO_89 | read tested OK |
| TCMux `pincs` | 22 | 31 | MD_GPIO_80 | read tested OK |
| TCMux `pinsc` | 23 | 33 | MD_GPIO_81 | read tested OK |

### Pi4J 2.8.0 (Java 11+) — works only with a code workaround
- Its GPIO plugin refuses non-Raspberry boards in two places (native library
  loading and chip opening).
- `pi4j2-test/com/pi4j/library/gpiod/internal/GpioChipOpener.java` bypasses both
  (loads the native libraries itself and opens the chip by label). With it,
  `Probe2` reads the six lines correctly.
- Relies on Pi4J internals → fragile on upgrades. No usable serial provider.

### Pi4J 4.0.2 (Java 25) — works with a one-time system setup — **recommended**
- No Raspberry check; the GPIO chip is selected in the API (`bus(2)`).
- It insists on Raspberry-OS-style groups (`gpio`, `i2c`, `spi`, `dialout`) on
  the user and on the device files. `board-scripts/setup-pi4j-groups.sh` creates them
  (run once as root: `sudo bash setup-pi4j-groups.sh`; undo commands inside).
  **Already applied on `ventunoq1`.** Arduino services still run normally after it.
- Tests passed: `Probe4` (six inputs), `Out4` (pin 13 toggled as output).
- Providers present: digital in/out, I2C, SPI, PWM. **No serial provider** →
  serial devices must use `jSerialComm` (already in the panel's `lib/`).

How to rerun on the board:
```
cd ~/pi4j4-test
~/opt/jdk25/bin/javac -cp "lib/*" Probe4.java Out4.java
~/opt/jdk25/bin/java --enable-native-access=ALL-UNNAMED -cp ".:lib/*" Probe4 109 106 107 89 80 81
~/opt/jdk25/bin/java --enable-native-access=ALL-UNNAMED -cp ".:lib/*" Out4 109
```

### Header I2C bus — **not available yet**
- The header bus is controller `i2c0` (`/soc@0/geniqup@9c0000/i2c@980000`,
  pins gpio17/18). It is `disabled` in the shipped device tree; only the
  internal bus (`/dev/i2c-12`: audio codec, HDMI bridge, USB-PD, power monitor)
  is enabled. No SPI device is enabled either.
- Arduino's tool `arduino-linux-config hw enable automation` is the intended
  route (it applies a HAT overlay), but in release 0.3.0 it answers
  `unsupported overlay for automation` even as root
  (`board-scripts/try-enable-i2c.sh`, output in `board-scripts/try-enable-i2c.log`).
- Arduino's `automation` overlay (`docs/arduino-automation-hat-overlay.dts`)
  only adds two chips under `i2c0`; it does not set `status = "okay"`. So HAT
  support on the header looks unfinished in this release.
- The device tree is not a boot file: it lives in flash partitions `dtb_a` /
  `dtb_b` as a combined image read by the Qualcomm firmware. Writing a custom
  one is possible in principle but a mistake leaves the board unbootable until
  re-flashed. **Not attempted.**

Options: (1) USB-to-I2C adapter (MCP2221 / CP2112) — no risk, works with Pi4J;
(2) wait for / ask Arduino; (3) custom device tree, only after the re-flash
procedure has been tried.

## What is installed on the board (all in `/home/arduino`, nothing system-wide except the groups)

| Path | Content |
|---|---|
| `~/opt/jdk21`, `~/opt/jdk25` | Temurin JDK 21.0.13 and 25.0.5 (tarballs from adoptium.net) |
| `~/pi4j2-test` | Pi4J 2.8.0 jars + probes + workaround |
| `~/pi4j4-test` | Pi4J 4.0.2 jars (`pi4j-core`, `pi4j-plugin-ffm`), slf4j 2.0.12, probes, the two scripts |
| `~/ArduinoApps/llm-test` | LLM test app |
| `~/ArduinoApps/vacuum-assistant` | read-only assistant app (running, against the simulator) |
| `~/panelsim` | panel simulator (running on port 1288) |
| `~/qwen3_install.log` | model download log |
| `/etc/udev/rules.d/99-pi4j.rules`, groups `gpio` `i2c` `spi` | from `setup-pi4j-groups.sh` |

Jars come from Maven Central (`com.pi4j`, versions above); they are not copied
into this repository.

## Contents of this folder

```
README.md
docs/ventuno-q-user-manual.md            Arduino user manual (reference copy)
docs/arduino-automation-hat-overlay.dts  decoded Arduino HAT overlay
llm-test/                                first local-LLM test app (LLM brick)
vacuum-assistant/                        read-only LLM assistant for the panel (see its README)
pi4j2-test/                              Pi4J 2.8.0 probes + GpioChipOpener workaround
pi4j4-test/                              Pi4J 4.0.2 probes (input, output)
board-scripts/setup-pi4j-groups.sh             root setup for Pi4J 4 (applied)
board-scripts/try-enable-i2c.sh, .log          dry run of Arduino's HAT tool (fails)
board-scripts/drmstate.py                      read current HDMI timing without root
```

## Read-only assistant (`vacuum-assistant/`) — working against a simulated panel

Decision of 2026-11-20: architecture A (Pi 4 as control layer, VENTUNO as AI
supervisor over a direct Ethernet cable), **read-only first, no commands**.

The app `vacuum-assistant` polls the panel's Modbus TCP server, and the local
model answers questions from the live values through a web page on port 7000.
It has no write function. Details, limits and test results:
`vacuum-assistant/README.md`.

- The panel's Modbus server accepts several masters at once (DAQ + VENTUNO):
  checked with the same library and settings, not yet on the running panel.
- On the board the app is running against a simulator (`~/panelsim`), with the
  page labelled `SIMULATED VALUES`.

## Where we are — next steps

0. **Assistant:** connect to the real panel once the Pi 4 is up (set
   `panel_host` to `192.168.251.1`, remove the label, stop the simulator); add
   the meaning of the status codes to `channel_notes.csv`; have the channel
   descriptions reviewed.

1. **Decide the architecture** (A: Pi + VENTUNO over a cable, or B: VENTUNO only).
2. **Java 25 check of the existing panel** (GLG, `MainApp.jar`, jlibmodbus) on the
   VENTUNO with hardware devices disabled — needed for B and for any Pi 5 move.
   Not started.
3. **I2C bench test** with a spare slave module, through a USB-to-I2C adapter
   (or the header bus if it becomes available).
4. **Serial test** with `jSerialComm` and one USB-RS232 adapter.
5. For A: USB-Ethernet dongle on the Pi 4, `192.168.251.1`, then a first
   read-only Modbus connection from the board, then a restricted tool layer
   (MCP) for the model. Commands to valves and pumps must be limited by that
   layer, not by the model.
6. Fixed address for the board (the ssh entry currently uses a DHCP Wi-Fi address).

Related, outside this folder: the vactube1500w Pi 3 is being replaced by a Pi 4
(needed for the Leyspec RGA's USB-Ethernet bridge); see `work_tube1500w`.
