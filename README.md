# ScadaRPI

Root-level build/run/clean orchestration for all `work_*` projects.

## Quick Start

```bash
./scadarpi list
./scadarpi compile all
./scadarpi run mainpanel
./scadarpi clean flowmeter
```

`work` names can be written as:
- full: `work_mainpanel`
- short: `mainpanel`

## Commands

```bash
./scadarpi list
./scadarpi compile [all|work_name...]
./scadarpi run <work_name> [-- <main_args...>]
./scadarpi runjar <work_name>
./scadarpi clean [all|work_name...]
```

`compile` and `clean` default to all `work_*` directories if no target is passed.

## Makefile Shortcuts

```bash
make list
make compile WORK=all
make compile WORK=pcounter
make run WORK=mainpanel
make clean WORK=work_sqz
```

## Java Selection

The root script uses:
- `JAVAC_BIN` (if set), else `JAVA_HOME/bin/javac`, else `javac` from `PATH`
- `JAVA_BIN` (if set), else `JAVA_HOME/bin/java`, else `java` from `PATH`
