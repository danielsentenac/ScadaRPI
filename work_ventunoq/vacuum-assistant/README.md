# vacuum-assistant — read-only LLM assistant for the vactube1500w panel

An Arduino App Lab app for the VENTUNO Q. It polls the `work_tube1500w` panel
over Modbus TCP, keeps the latest values, and lets the local model
(`arduino:llm` brick; Qwen 3 4B Instruct, see the model comparison below) answer questions about them through a
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
| `python/channel_notes.csv` | Per channel: panel name (G21, V21, P21...), what it is, unit, meaning of each status code — **to be reviewed** |
| `python/config.json` | Panel address, poll period, optional page label |
| `assets/index.html` | Web page: question box, answer, live value table |
| `sim/PanelSim.java` | Stand-in for the panel: same Modbus library and settings, plausible values |
| `regdump/RegDump.java` | Regenerates `registers.csv` from the panel source |

## How it works

1. Every 2 s the app opens a connection, reads registers 0–582 and closes it
   (the panel drops connections idle for more than 5 s, so it reconnects each time).
2. Values are decoded with `registers.csv`. FLOAT32 is stored low word first
   (jlibmodbus `setFloat32At`, as used by the panel's `DataManager`).
3. For a question, the app builds a compact text (at most 2500 characters):
   - a header computed by the app, covering **all** devices: which devices are not
     communicating, every state that means a problem (fail, error, alarm, excess,
     out of range), all pressures by panel name, the main states (valves, pumps);
   - then the full blocks of the devices the question points at (matched on panel
     names such as `Ga2`, `V21`, and on keywords). Each line reads
     `panel name, what it is [CHANNEL] = value`, and a status code is followed by
     its meaning, e.g. `V23 valve [M1_V23ST] = 2 (closed)`;
   - for the RGA, the dominant gas and the next five peaks, already ranked, with
     the usual gas for each mass.
4. The model answers from that text only. Each question is independent
   (`llm.clear_memory()` before each call), at temperature 0.2.
5. A request to act ("Open valve V21") is recognised by the app: the model is only
   asked for the current state of the item, and the answer always starts with
   "I am read-only and cannot send commands."

Whatever must be exact (ranking, searching all devices for a fault, refusing
commands) is done by the app; the model only words the answer.

Conventions taken from the panel source: `*_COMST` = 0 means communication OK,
1 means failure; on failure the panel sets values to 0 and status words to 255.

## Limits found

- **Model context: 4096 tokens.** A prompt of about 4500 characters makes the
  Genie model service fail and restart. Hence the 2500-character budget and the
  selection of values by question.
- **The LLM brick keeps the conversation between calls** even without
  `with_memory()`. Without `clear_memory()` the third question overflowed the context.
- **The model still over-states.** Asked "Is everything OK?" it may add that
  pressures are "within normal ranges" although no limits are given to it.
  Reference ranges per gauge would be the next thing to add.
- **Meanings to confirm:** status codes come from the panel source
  (`ChannelList.java`, the drivers, the Controllino firmware). Uncertain ones:
  the Sicem ramp values (units unknown), `SICEM_TCMUX_T<n>`, `ROT_PR1ST`.
- **Not given to the model** (marked `-` in `channel_notes.csv`): gauge Ga2 (XGS
  controller present, gauge not installed; the whole XGS device is left out,
  including its communication state), valve V23, the bypass, and the unused
  MaxiGauge channel 4. G4 and G5 (Pirani gauges of the H2O vapour line) are
  MaxiGauge channels 5 and 6.
- **Still to give to the model:** ranges and normal regime per gauge, and which
  heater each Sicem channel is (CH8 around VM3, CH9 around V21).
- Answers take 1–15 s depending on length.

## Test results (simulated panel, 2026-11-23)

Raw answers: `bench/results/qwen3-4b-instructions-v2.txt`. With the new value
text and instructions, all eight reference questions are answered correctly in
about 50 s in total (125 s before): gauge pressures by panel name, turbo pump
state from the decoded codes, dominant gas (mass 18, H2O), bake-out temperatures,
valve states, communication, Ga2 against the RGA total pressure (Ga2 is no
longer confused with `MG_PR2`), and command requests refused. The header was
also checked with injected faults (compressed air FAIL, valve discordance,
turbo error code, gauge overrange, one device not communicating).

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

## Model comparison (2026-11-20, simulated panel)

Eight reference questions (`bench/bench.sh`; raw answers in `bench/results/`),
same value budget (2500 characters), temperature 0.2.

| Model | Runner | Loads? | Total time for the 8 questions | Generation speed | Behaviour |
|---|---|---|---|---|---|
| Qwen 3 4B Instruct (`genie:qwen3_4b_instruct_2507`) | Qualcomm Genie, 4096-token context | yes | about 125 s | not reported by the runner | Guesses from status words ("may indicate a fault"); bake-out answer garbled |
| Qwen 3.5 4B (`llamacpp:unsloth/Qwen3.5-4B-GGUF/Qwen3.5-4B-Q4_K_M`, 2.7 GB) | llama.cpp with Hexagon NPU backend, 16384-token context | yes | about 130 s | 9.6 tokens/s; prompt read at about 345 tokens/s | Declines to judge "normal" from a status word, as instructed; complete bake-out answer |
| Qwen 3.5 9B (`llamacpp:unsloth/Qwen3.5-9B-GGUF/Qwen3.5-9B-Q4_K_M`, 5.7 GB) | same | **no** | — | — | Runner aborts at load: `ggml-hex: HTP2 buffer mapping failed ... size 889982976`, `fastrpc_mmap failed`; same with a 4096 context |

Notes:
- **Decision (2026-11-23): stay on Qwen 3 4B Instruct** (set in `app.yaml`).
  Qwen 3.5 4B gave more careful answers and four times the context at the same
  overall time, but the gain was judged too small to change model. Both
  Qwen 3.5 models have been deleted from the board.
- **Temperature matters.** At the brick's default (0.7) both 4B models were less
  reliable; in one run Qwen 3.5 4B called mass 18 "H2" and said the turbo pump
  was not running at 1000 Hz. The app now uses 0.2.
- **Giving all values at once** (3650 characters, possible with the 16384
  context) did not improve the answers in the one run tried (at temperature 0.7).
- **Both 4B models take "Ga2" to be `MG_PR2`** (it is `XGS_PR7`), although the
  description says so. The value text should put the panel's own names (Ga2,
  G21, V21...) first; to be done together with the status-code meanings.
- Switch model: `arduino-app-cli app brick config user:vacuum-assistant arduino:llm --model <id>` then restart the app.
- To repeat the test: `arduino-app-cli model install <id>`, and `arduino-app-cli model delete <id>` afterwards.
