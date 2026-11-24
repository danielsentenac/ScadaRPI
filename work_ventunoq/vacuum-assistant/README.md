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
| `python/config.json` | Panel address, poll period, optional page label, `commands_enabled` (default false), `baking_setpoint_degC` (150), `max_setpoint_degC` (150), the `rga_*` settings of the vacuum report |
| `python/rules.py` | Operating knowledge: gauge ranges, normal regime, interlocks, heating rules |
| `python/actions.py` | The sequences (Safety, Background baking), the experiment-file interpreter, the action log |
| `python/modbus_cmd.py` | Modbus write client used by `actions.py` only |
| `data/assistant.log`, `data/instructions.exp` | On the board: action log and last experiment file loaded |
| `python/history_report.py` | Historical vacuum report: trends and RGA spectra since a chosen event, from the DAQ trend data |
| `python/history.py` | The assistant's own record of the panel values and the parsing of the time asked in a question |
| `python/daq.py`, `daq/trend_read.py` | Past values from the DAQ trend files: board side (ssh) and the read-only script run on the Virgo server |
| `python/report.py` | Status data of the vacuum report and its PNG page (Pillow) |
| `python/report_xlsx.py` | The vacuum report as an Excel workbook (openpyxl) |
| `python/netdisk.py` | Upload of the reports to the network share (smbprotocol) |
| `python/requirements.txt` | Packages installed by App Lab at the first start (needs internet once): openpyxl, smbprotocol |
| `examples/bake-150.exp` | Example experiment file (experiment files must end with `.exp`) |
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
     900 Hz (above 600 Hz when its standby mode is on, nominal 666 Hz); V21 open; G21 1E-5 to 5E-11 mbar - each condition stated as met or not;
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

## Direct answers (no model)

A question about the value or state of named items is answered by the app
itself, from a Modbus read made for that question (`refresh()` in `main.py`),
with the time of the read. The model is not asked, so it cannot give a number
that was not read.

- Items are found by their panel name (G21, G22, G4, G5, Ga4, Ga5, Ga3, V1, V21,
  V22, P21, P22, CH1-CH9, bypass, compressed air, Sicem) or by a group word
  (pressures, temperatures, valves, pumps; "rack" for the rack thermocouples).
- An item whose device is not communicating gives `NO VALID VALUE - the <device>
  is not communicating with the panel` (the panel publishes 0 in that case, which
  must never be reported as a value). If nothing valid was read, the answer stops
  there, whatever the question.
- "Is there any problem?", "status", "is everything ok?" are answered by the
  app with the checks it makes over all devices (communication, problem states,
  normal regime, heating) and its own verdict. For a procedure question naming
  a failure, the rule is worded by the model and the current state is added by
  the app.
- A plain value question ("G22 value", "what are the pressures") gets only the
  values read. A question needing interpretation (why, normal, should...) still
  goes to the model, with the values read given to it and printed before its
  answer; procedure questions are answered from the operating rules as before.

## Past values

A question naming a past time is answered without the model, from two sources:

1. **The DAQ trend files** (the data DataViewer shows), when they hold the
   channels asked for at that time. The rack is acquired by the Modbus master
   configured in `/virgoData/Vacuum/racks/TUBE1500W.cfg` (olserver38); the
   channels are `V1:VAC_TUBE1500W_<panel channel>` at 1 Hz in
   `/virgoData/ffl/trend.ffl`. The board reads them through ssh
   (`python/daq.py`, paramiko): its key may only run the read-only script
   `daq/trend_read.py` on a Virgo server, which returns the samples as JSON.
2. **The assistant's own record** otherwise (`python/history.py`): one row per
   `history_seconds` (60) in `data/history/<date>.csv`, kept `history_days`
   (30). It covers what the DAQ does not hold (the RGA channels, periods when
   the rack is not acquired) and starts when this version was installed.

The answer names the source used.

| Question | Answer |
|---|---|
| `G22 1h`, `G21 30 min ago`, `V21 2 days ago` | the value recorded nearest to that time |
| `G22 at 14:30` | today at 14:30 (yesterday if that time is still to come) |
| `V21 yesterday`, `G22 yesterday at 9:00` | |
| `G22 last 2h`, `CH3 since 14:00`, `P21 trend 45 min` | over the period: first and last value, lowest, highest; for a state, its changes |

A record where the device was not communicating counts as "no valid value",
never as 0. Over a long period the DAQ values are sampled (at most 300 rows), so
a short excursion between two samples is not seen.

Set-up of the DAQ access:

- on the server: `daq/trend_read.py` in the account's `~/vacuum_assistant/`,
  and in its `~/.ssh/authorized_keys` the board's public key with a forced
  command (one line):
  `command="/cvmfs/software.igwn.org/conda/envs/igwn-py39/bin/python /users/<account>/vacuum_assistant/trend_read.py",no-pty,no-port-forwarding,no-agent-forwarding,no-X11-forwarding ssh-ed25519 AAAA... vacuum-assistant`
- on the board: the private key in `data/daq_key` (created there with
  `ssh-keygen -t ed25519 -N "" -f data/daq_key`, never copied elsewhere) and
  `"daq": {"host", "user", "prefix", "rename"}` in `config.json`.

## Actions (HMI panel on the Pi)

The Pi HMI has an AI ASSISTANT window (`work_tube1500w/AssistantGui.java`,
opened by the AI ASSISTANT button of the synoptic). It calls this app over
HTTP: `/ask` (chat), `/action?name=safety|baking|report|stop`, `/play` (POST
`{"name", "text"}`), `/log?since=n`, `/file?name=instructions|log`, `/grammar`, `/check` (POST `{"text"}`), `/status`,
`/reports` (names, newest first), `/report?name=<name>&format=png|xlsx`.

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
  REPORT                           the vacuum report sequence below
  ```
- **EDIT EXPERIMENT**: the editor of the HMI to write an experiment file on the touchscreen.
  Its right side is the grammar dictionary, read from the assistant (`/grammar`, the list
  `GRAMMAR` in `python/actions.py`, kept next to the parser): touching an instruction or a word
  (valves, pumps, names of WAIT UNTIL) inserts it in the text. CHECK sends the text to the
  assistant's own parser (`POST /check`): nothing is run, the bad lines are listed. SAVE writes
  `<name>.exp` in the `play` folder of the panel, where PLAY offers it; OPEN reads a file from
  that folder. The editor only writes files: an experiment starts with PLAY alone.
- **VACUUM REPORT** (`python/report.py`, `Controller.seq_report`):
  1. If the RGA scan is not running it is started, only with G21 valid and below
     `rga_max_pressure_mbar` (1E-5), to protect the filament.
  2. One spectrum (masses 1 to `rga_last_mass`, 100) is recorded every
     `rga_cycle_seconds` (25 s: 100 masses at 200 ms).
  3. The recording stops when masses 2, 18, 28 and 32 are stable: for each,
     (highest - lowest) / mean over the last `rga_stable_cycles` (5) spectra is
     within `rga_stable_tolerance` (10 %). A mass whose mean is below
     `rga_noise_floor_A` (1E-12 A) is at the noise level and is not tested.
     Without stabilization it stops after `rga_max_minutes` (30) and says so.
  4. The scan and the filament are put back as they were before the report
     (the panel clears the spectrum when the scan stops, so the last spectrum is
     taken before).
  5. The report is saved in `data/reports/` as
     `TUBE1500W_report_<YYYY-MM-DD>_<HHMMSS>.xlsx` (local time, `timezone` in
     `config.json`), with a `.png` page of the same name for the HMI. Content:
     regime verdict, valves, pumps, heating, pressures and temperatures CH1-CH9
     at the start and at the stop of the recording (with out-of-range
     warnings), the last spectrum, the trend of masses 2, 18, 28, 32, 44 from
     start to stop, the stabilization result per mass, and two pressure trends
     over the same time: the cold cathodes G21, Ga4 and G4 inside their range
     (5E-11 to 5E-5 mbar), joined by the spinning rotor gauge Ga5 only at
     5E-6 mbar and above; and the Pirani gauges G22 and G5 (5E-4 to 800 mbar).
     Ga3 is the RGA, not a pressure gauge. Its own total pressure reading is
     not used anywhere (it stays at the bottom of its scale): the total
     pressure given with the spectrum, and in the answers about the RGA, is G21. The workbook has the sheets Report (tables and the four
     charts), RGA spectrum, RGA trend and Gauges (the numbers behind them).
  6. The `.xlsx` files missing on the network share
     `smb://netdisk.virgo.infn.it/vacuum/TUBE1500W` (`netdisk` in
     `config.json`) are uploaded after each report and every 10 minutes, so a
     report made while the share was unreachable is sent later. Nothing is
     ever deleted on the share. On the board, once a day, the report files
     older than `reports_days` (90) and the lines of `data/assistant.log`
     older than `log_days` (90) are removed (0 keeps everything); this is
     done only after a successful copy to the share. The account is read from
     `data/netdisk.credentials` on the board (never in the repository):

     ```
     username=<account>
     password=<password>
     domain=<domain>        (optional)
     ```
     Without this file the reports stay on the board and the log says so.
  The decision to stop is this numeric criterion computed by the app, not a
  judgement of the language model. The REPORTS button of the HMI lists and
  shows the reports; the workbook is at `/report?name=<name>&format=xlsx`.
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

- **Model context: 4096 tokens, but the model service allows much less.** The
  service (Qualcomm GenAI runner 1.5.0, container `genie-models-runner`) counts
  words x 1.3 as tokens and:
  - cuts the system prompt at 256 tokens (about 160 words of ours);
  - after each answer, if question + answer exceed about 676 tokens, summarises
    them and "extracts facts" with the model. That step crashes the model
    process (`Event ...-facts: LLM process socket closed`), and the dead process
    leaves about 7 GB of NPU memory locked: every later request fails with
    `500 issue while cleaning up memory`, then `503 not configured correctly to
    run AI models`, **until the board is rebooted**.

  Hence `MAX_PROMPT_WORDS` (300) and `MAX_ANSWER_TOKENS` (200) in `main.py`:
  only the operating rules sharing a word with the question are sent, and the
  values are added most important first within the word limit. Do not raise
  these limits without redoing the sum given in `main.py`.
- **Never stop or restart the whole app once the model has answered**
  (`arduino-app-cli app restart|stop`, or the stop button of App Lab): killing
  the model service while the model is loaded locks the NPU memory in the same
  way, and only a reboot clears it. To load a new `main.py`, restart the
  program only: `docker restart vacuum-assistant-main-1` (verified 2026-11-24:
  the model keeps answering). For anything else, reboot the board.
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
arduino-app-cli app start user:vacuum-assistant        # first start only
arduino-app-cli app logs  user:vacuum-assistant
```
Page: `http://<board address>:7000`.

Later updates: copy the files, then `docker restart vacuum-assistant-main-1`.
Not `app restart`: see "Limits found".

`config.json` in this repository points at the real panel over the direct
cable (`192.168.251.1:1288`), and since 2026-11-23 so does the board (set-up of
the cable in `../pi-link/`). To go back to the simulator on the board, set
`panel_host` to `172.17.0.1` (the Docker host address) and a `panel_label` such
as `SIMULATED VALUES`; a copy of those settings is in `python/config.json.sim`
on the board. The app starts at boot as the board's default app.

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

## Historical vacuum report

The VACUUM REPORT describes the present. The HISTORICAL REPORT button of the HMI describes what
happened between a START and a STOP, from the trend data saved by the DAQ (`python/history_report.py`):

1. **START** is an event or a date. An event is an item taking a state (V22 closed, P22 pump off,
   heating CH1 off, RGA scan running...) or a temperature set point changing (CH1 to CH9), searched
   over the last 2, 7, 30 or 60 days. `/events` looks for its times: one DAQ request per 2 days,
   four at a time, then each change is located to the second. A state that lasted less than about
   90 seconds can be missed. The operator picks one of the times found.
2. **STOP** is the present, a date, or an event chosen the same way among those found after START
   (`/events?...&after=<START>`).
3. `/action?name=history&channel=&code=&t=&channel2=&code2=&t2=` builds the report (an empty
   `channel` or `channel2` means the date `t` or `t2`; `t2=0` means the present):
   - states, pressures, set points and temperatures at START and at STOP, read just after each
     event to show its effect (a change is shown in red);
   - trends of the pressure gauges and of masses 2, 18, 28, 32, 44, START and STOP marked by red lines;
   - the RGA spectrum at START (first full scan after it) and at STOP (the scan running now, read
     on the panel, when STOP is the present; else the last one the DAQ holds before STOP), on the
     same scale;
   - the AI commentary (below);
   - in the workbook: the same tables, the two scans in columns (mass, START, STOP), the trend data.
4. The files are `<prefix>HIST_report_<date>_<time>.png/.xlsx`, listed and copied to the share like
   the other reports.

Nothing is written to the panel: the RGA must be left scanning for its trend to exist. RGA masses
recorded before `rga_current_since` (unix time, config.json) were published 100 times too low and
are multiplied by 100. Channels the DAQ did not record at the time appear as "no data", and so
does a heating value of -99 (no sensor cable connected on the channel); a channel without any
value is left out of the temperature table.

### AI commentary

The last block of the page, labelled as written by the language model, is the only part of the
report the model takes part in, and it only words it:

- `history_report.facts()` computes what there is to say: what differs at STOP compared with
  START, each pressure and each main RGA mass at both bounds (the values of the tables and of the
  two spectra) with its evolution (rose or fell by a factor, stayed steady: a factor under 1.5),
  a peak in between, the dominant gases at STOP, the regime.
- `main.report_commentary()` gives these lines to the model within the prompt limit and asks for
  three short sentences.
- `history_report.checked()` removes from the answer any sentence that quotes a number absent from
  the facts, that gives a gauge or a gas an evolution other than the computed one, or that is cut.
  The page says how many sentences were removed. Other errors of wording remain possible: the
  tables are the reference.
- The workbook holds the commentary and the facts given to the model. If the model does not
  answer, the report is made without commentary.
