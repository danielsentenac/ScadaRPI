# SCADARPI

SCADARPI is a multi-application Java 8 project for SCADA/HMI stations.  
Each `work_*` directory is a standalone application (own `Main.java`, device logic, GUI drawings, and logs), while top-level tooling (`scadarpi`, `Makefile`) gives one uniform way to compile and run them.

## Project layout

- `scadarpi`: main CLI used to list targets, compile, run, and clean.
- `Makefile`: thin wrapper around `./scadarpi`.
- `lib/`: all third-party JARs used by the applications (Pi4J, jlibmodbus, GLG, serial libs, sqlite, etc.).
- `work_*`: application modules (for example `work_panel`, `work_rga`, `work_venting`, `work_mainpanel`).
- `*.g` files in `work_*`: GLG drawing files used by the GUI classes.

## Runtime model (high level)

Most modules follow this startup pattern:
- `Main` builds a `DeviceManager`, creates one or more `Device` instances, starts their threads, starts a `ModbusSlaveThread`, then opens a `GlgGui`.
- `Device` subclasses encapsulate hardware/network integration (I2C, serial, Modbus TCP, HTTP clients, etc.).
- Some modules include non-ARM safeguards and can skip hardware startup automatically unless forced.

## Requirements

- Linux environment.
- Java 8 JDK available (`java` and `javac` on PATH), or set `JAVA_HOME` / `JAVA_BIN` / `JAVAC_BIN`.
- For hardware-backed modules on Raspberry Pi, access to required serial devices (`/dev/serial/...`), I2C, GPIO, and reachable instrument/network endpoints.
- GUI execution requires an active X display.

Verified in this workspace on March 3, 2026:
- `java version "1.8.0_202"`
- `javac 1.8.0_202`

## Build and run (recommended)

List available modules:

```bash
./scadarpi list
```

Compile one module:

```bash
./scadarpi compile work_panel
# or short name:
./scadarpi compile panel
```

Compile multiple modules:

```bash
./scadarpi compile mainpanel flowmeter rga
```

Compile all modules:

```bash
./scadarpi compile all
```

Run one module:

```bash
./scadarpi run work_panel
# or:
./scadarpi run panel
```

Run in headless mode (no GUI):

```bash
SCADARPI_HEADLESS=1 ./scadarpi run rga
```

Force GUI attempt:

```bash
SCADARPI_GUI=1 ./scadarpi run panel
```

Force hardware startup on non-ARM hosts:

```bash
SCADARPI_FORCE_HARDWARE=1 ./scadarpi run rga
```

Clean generated artifacts:

```bash
./scadarpi clean work_panel
# or clean everything:
./scadarpi clean all
```

## Makefile shortcuts

- `make list`
- `make compile WORK=work_panel`
- `make run WORK=work_panel`
- `make run WORK=work_panel RUN_ARGS="--demo"`
- `make clean WORK=work_panel`

## Current build status

As of March 3, 2026 in this repository state:
- All `work_*` targets compile except `work_pcounter`.
- `work_pcounter` fails due to a Java syntax error at `work_pcounter/Controllino_3.java:163`.

## Notes for local testing

- Some modules perform live network/hardware calls inside device threads; on restricted environments they may start but log communication errors.
- Log files are created inside each `work_*` directory, usually named like `*_YYYY-MM-DD.log`.
