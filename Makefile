SHELL := /bin/bash

WORK ?= all
RUN_ARGS ?=

.PHONY: help list compile run runjar clean

help:
	@./scadarpi help

list:
	@./scadarpi list

compile:
	@./scadarpi compile $(WORK)

run:
	@if [[ "$(WORK)" == "all" ]]; then echo "Set WORK=<work_name>"; exit 1; fi
	@./scadarpi run $(WORK) -- $(RUN_ARGS)

runjar:
	@if [[ "$(WORK)" == "all" ]]; then echo "Set WORK=<work_name>"; exit 1; fi
	@./scadarpi runjar $(WORK)

clean:
	@./scadarpi clean $(WORK)
