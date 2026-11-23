# vacuum-assistant — read-only LLM assistant for the vactube1500w panel

An Arduino App Lab app for the VENTUNO Q. It polls the `work_tube1500w` panel
over Modbus TCP, keeps the latest values, and lets the local model
(`arduino:llm` brick; Qwen 3 4B Instruct, see the model comparison below) answer questions about them through a
web page (`arduino:web_ui` brick, port 7000).

**Commands.** The language model never sends a command. Actions are fixed
sequences in `python/actions.py`, started from the HMI panel on the Pi (buttons
SAFETY, BACKGROUND BAKING, PLAY) and written to the panel's Modbus server by
`python/modbus_cmd.py`. They are **disabled by default**: with
`"commands_enabled": false` in `config.json` every step is only logged as
`DRY RUN`. Reading (`python/modbus_ro.py`, function 3) is separate.

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
| `python/config.json` | Panel address, poll period, optional page label, `commands_enabled` (default false), `baking_setpoint_degC` (150), `max_setpoint_degC` (150) |
| `python/rules.py` | Operating knowledge: gauge ranges, normal regime, interlocks, heating rules |
| `python/actions.py` | The sequences (Safety, Background baking), the experiment-file interpreter, the action log |
| `python/modbus_cmd.py` | Modbus write client used by `actions.py` only |
| `data/assistant.log`, `data/instructions.txt` | On the board: action log and last experiment file loaded |
| `examples/bake-150.txt` | Example experiment file |
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

6. **Operating knowledge** is in `python/rules.py` (from the operators'
   instruction sheet "FILE ISTRUZIONI PER TUBO 1500W" and what they said). The
   app checks it on the values and writes the verdicts in the header:
   - **gauge measuring ranges** (Pirani 800 to 5E-4 mbar, cold cathode 5E-5 to
     5E-11, spinning rotor 1 to 5E-6): a reading outside is marked not reliable;
   - **normal regime**: P22 on; G22 0.5 to 5E-4 mbar; V22 open; P21 on above
     900 Hz; V21 open; G21 1E-5 to 5E-11 mbar - each condition stated as met or not;
   - **interlocks of V22, V21 and V1**, each with the conditions present now and
     a verdict (can / cannot be opened now). G22 acts through two MaxiGauge
     relays (LOW 1.0 mbar, HIGH 5.0 mbar): V21 and V22 cannot be opened unless
     G22 is below 1.0 mbar and are closed above 5.0 mbar. The bypass, when on,
     switches off this G22 safety for V22; V1 (H2O line) opens only with P22 on
     and the bypass on. All close if the compressed air is below 5 bar;
   - **where each gauge and valve sits** (in `channel_notes.csv`): G21 and Ga5
     in the tube (Ga5 for high pressure only, not below 5E-6 mbar), G22 fore
     vacuum, Ga4 at the NEG, G4 (cold cathode, MaxiGauge 4) in the H2O line,
     G5 (Pirani, MaxiGauge 5) in the H2O reservoir;
   - **heating**: CH1-CH9 must have the same setpoint; if the heating is on
     while a problem is present (state meaning fail/error/alarm, device not
     communicating, normal regime not met), the header says
     `SAFETY MODE REQUIRED`: the operator must press CH1 OFF and CH2 OFF.
   The short text `OPERATING_RULES` is given to the model with every question; a
   question about a procedure is answered from it only.

Whatever must be exact (ranking, searching all devices for a fault, refusing
commands) is done by the app; the model only words the answer.

Conventions taken from the panel source: `*_COMST` = 0 means communication OK,
1 means failure; on failure the panel sets values to 0 and status words to 255.

## Actions (HMI panel on the Pi)

The Pi HMI has an AI ASSISTANT window (`work_tube1500w/AssistantGui.java`,
opened by the AI ASSISTANT button of the synoptic). It calls this app over
HTTP: `/ask` (chat), `/action?name=safety|baking|stop`, `/play` (POST
`{"name", "text"}`), `/log?since=n`, `/file?name=instructions|log`, `/status`.

- **SAFETY**: CH1 OFF, CH2 OFF, setpoints CH1-CH9 to 0, close V1, close V21,
  stop P21, close V22, stop P22. It preempts anything running. No confirmation.
- **BACKGROUND BAKING**: refused unless the normal regime is met; then setpoints
  CH1-CH9 to 150 degC, CH1 ON, CH2 ON.
- **PLAY**: an experiment file, one instruction per line (`#` = comment). The
  whole file is checked before anything runs; a bad line refuses the file.

  ```
  CHECK REGIME                     stop unless the normal regime is met
  SETPOINT <degC>                  same setpoint on CH1-CH9 (0 to 150)
  HEATING ON | HEATING OFF         ON only if the normal regime is met
  OPEN <valve> | CLOSE <valve>     V1, V21, V22 (open refused if an interlock condition is present)
  START <pump> | STOP <pump>       P21, P22
  BYPASS ON | BYPASS OFF
  WAIT <duration>                  30, 30s, 10min, 2h
  WAIT UNTIL <name> < <value> [TIMEOUT <duration>]    or >; names G21 G22 G4 G5 GA4 GA5 P21SPEED TEMP
  LOG <text>
  SAFETY
  ```
- **STOP EXPERIMENT**: stops the running sequence; the system is left as it is.
- After each command the app waits for the panel's status to confirm it; a step
  that is not confirmed, or a failed check, stops the sequence.
- While heating switched on by the assistant is running, it watches the normal
  regime and the Sicem alarm, and switches CH1 and CH2 off if a problem appears.
- Every step goes to the action log, shown live on the HMI.

**Not yet approved by the operators:** the order of the SAFETY sequence, how the
baking setpoint is reached (directly or by ramp), the experiment-file language.
Commands stay disabled until then. Tested only in dry run against the simulator,
and the panel's write path with a fake device (see `work_tube1500w`).

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
  including its communication state), valve V23 and the unused
  MaxiGauge channel 6.
- **Confirmed by the operators (2026-11-23):** Ga5 is reliable from 1 mbar
  down to 5E-6 mbar; the bypass must be ON (red on the panel) to allow opening
  V1; the two G22 relays work with hysteresis (opening allowed below 1.0 mbar,
  forced closed above 5.0 mbar); this G22 interlock is what protects P21 from
  high pressure.
- **Sicem global alarm** (confirmed 2026-11-23): a failure of the Sicem controller
  itself, not wired to any interlock; if in alarm the operator presses CH1 OFF
  and CH2 OFF. "Interlock" in the safety rule means the valve interlocks.
- **The model is wordy** and ignores the three-sentence limit; its verdicts are
  right because they are copied from the header.
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
