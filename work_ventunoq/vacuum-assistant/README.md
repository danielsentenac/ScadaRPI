# vacuum-assistant — read-only LLM assistant for the vactube1500w panel

An Arduino App Lab app for the VENTUNO Q. It polls the `work_tube1500w` panel
over Modbus TCP, keeps the latest values, and lets the local model
(Qwen 3 4B Instruct, `arduino:llm` brick) answer questions about them through a
web page (`arduino:web_ui` brick, port 7000).

**It cannot send commands.** `python/modbus_ro.py` implements Modbus function 3
(read holding registers) only; there is no write function in the app.

Status (2026-11-20): works against a **simulated panel**. Not yet tested against
the real Pi (offline during the Pi 3 → Pi 4 swap).

## Files

| File | Role |
|---|---|
| `app.yaml` | App definition (bricks `arduino:llm`, `arduino:web_ui`) |
| `python/main.py` | Polling thread, value summary for the model, web API (`/state`, `/ask?q=`, `/snapshot?q=`) |
| `python/modbus_ro.py` | Read-only Modbus TCP client (raw sockets, no dependency) |
| `python/registers.csv` | Register map of the panel: 329 channels, offsets 0–582 (generated, see below) |
| `python/channel_notes.csv` | Human descriptions and units per channel prefix — **to be reviewed** |
| `python/config.json` | Panel address, poll period, optional page label |
| `assets/index.html` | Web page: question box, answer, live value table |
| `sim/PanelSim.java` | Stand-in for the panel: same Modbus library and settings, plausible values |
| `regdump/RegDump.java` | Regenerates `registers.csv` from the panel source |

## How it works

1. Every 2 s the app opens a connection, reads registers 0–582 and closes it
   (the panel drops connections idle for more than 5 s, so it reconnects each time).
2. Values are decoded with `registers.csv`. FLOAT32 is stored low word first
   (jlibmodbus `setFloat32At`, as used by the panel's `DataManager`).
3. For a question, the app builds a compact text: all pressures, the devices
   not communicating (`*_COMST` ≠ 0), then the full blocks of the devices most
   relevant to the question, up to 2500 characters. For the RGA only the six
   highest partial pressures are given.
4. The model answers from that text only. Each question is independent
   (`llm.clear_memory()` before each call).

Conventions taken from the panel source: `*_COMST` = 0 means communication OK,
1 means failure; on failure the panel sets values to 0 and status words to 255.

## Limits found

- **Model context: 4096 tokens.** A prompt of about 4500 characters makes the
  Genie model service fail and restart. Hence the 2500-character budget and the
  selection of values by question.
- **The LLM brick keeps the conversation between calls** even without
  `with_memory()`. Without `clear_memory()` the third question overflowed the context.
- **Status words are not explained to the model.** `channel_notes.csv` has no
  meaning for the status codes (valve open/closed, pump states, gauge states),
  so the model can only quote the number, and it tends to guess anyway.
  Filling in these meanings from the panel's GUI code is the main quality
  improvement still to do.
- Answers take 2–20 s depending on length.

## Test results (simulated panel)

Correct: all gauge pressures; dominant RGA mass (18); bake-out temperatures;
valve status list; "is any instrument not communicating"; requests for actions
("Open valve V21", "Switch off the RGA filament") are refused with the current
value quoted. Weak: "Is the turbo pump running normally?" — it reasons from
status words it does not understand.

## Deploy on the board

```
arduino-app-cli app new vacuum-assistant -b arduino:llm -b arduino:web_ui --no-sketch
# copy app.yaml, python/*, assets/index.html into ~/ArduinoApps/vacuum-assistant
arduino-app-cli app start user:vacuum-assistant        # or: app restart
arduino-app-cli app logs  user:vacuum-assistant
```
Page: `http://<board address>:7000`.

`config.json` in this repository points at the real panel over the direct
cable (`192.168.251.1:1288`). On the board it currently points at the simulator
(`172.17.0.1`, the Docker host address) with the label `SIMULATED VALUES`.

## Simulator (on the board)

```
cd ~/panelsim     # PanelSim.java, registers.csv, jlibmodbus-1.2.9.7.jar (from SCADARPI/lib)
~/opt/jdk21/bin/javac -cp jlibmodbus-1.2.9.7.jar PanelSim.java
~/opt/jdk21/bin/java -cp .:jlibmodbus-1.2.9.7.jar PanelSim registers.csv 1288
```

## Regenerate the register map

From `work_tube1500w` on the development PC (Java 8), after any change to the
device list in `Main.java` (the dump tool repeats that list — keep them in step):

```
javac -cp ".:classes:../lib/*" -d /tmp/regdump ../work_ventunoq/vacuum-assistant/regdump/RegDump.java
java  -cp "/tmp/regdump:.:classes:../lib/*" RegDump 8 9 > registers.csv   # 8, 9 = Controllino I2C addresses
```
(A pi4j "wrong ELF class" message on a PC is harmless.)

## Modbus server of the panel — two masters

Checked with the same library and settings (jlibmodbus 1.2.9.7, 5 s read timeout):
two masters polling and writing at once gave 600 good reads and no error; up to
10 simultaneous connections are served; an idle connection is dropped after 5 s
and a reconnect works at once. The server listens on all interfaces, port 1288.
Not yet verified on the running panel.

For a future command path: only single-register writes (function 6) go through
the panel's checks and reach the device; multi-register writes (function 16)
write any register directly, unchecked. There is no arbitration between masters.
