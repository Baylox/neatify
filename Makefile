# Convenience targets for Linux / macOS / WSL. (Windows users: use .\neatify.cmd
# directly — make is not available by default on Windows.)
#
# All run targets delegate to the ./neatify launcher; the build uses ./mvnw, so
# the only requirement is a JDK 21+ (Maven is downloaded by the wrapper).
#
# Override the variables on the command line, e.g.:
#   make apply DIR=~/Documents
#   make preview RULES=rules.properties ARGS="--on-collision skip"

SHELL := /bin/sh

DIR   ?= $(HOME)/Downloads
RULES ?= default
ARGS  ?=

# "default" selects the built-in rules; any other value is treated as a rules file.
ifeq ($(RULES),default)
  RULES_ARG := --use-default-rules
else
  RULES_ARG := --rules $(RULES)
endif

.PHONY: help build run preview apply undo dev verify clean

help:
	@echo "neatify - convenience targets (Linux/macOS/WSL)"
	@echo
	@echo "  make build              Build the jar (./mvnw package)"
	@echo "  make run                Launch interactive mode"
	@echo "  make preview            Dry-run on DIR (no changes)"
	@echo "  make apply              Organize DIR for real (--apply)"
	@echo "  make undo               Undo the last run on DIR"
	@echo "  make dev                build, then preview"
	@echo "  make verify             Full quality gate (./mvnw verify)"
	@echo "  make clean              ./mvnw clean"
	@echo
	@echo "Variables (override on the command line):"
	@echo "  DIR=$(DIR)"
	@echo "  RULES=$(RULES)   (\"default\" => --use-default-rules, else a rules file)"
	@echo "  ARGS=            (extra flags, e.g. --on-collision skip)"
	@echo
	@echo "Examples:"
	@echo "  make apply DIR=~/Documents"
	@echo "  make preview RULES=rules.properties ARGS=\"--on-collision skip\""

build:
	./mvnw -q package

run:
	./neatify

preview:
	./neatify --source $(DIR) $(RULES_ARG) $(ARGS)

apply:
	./neatify --source $(DIR) $(RULES_ARG) --apply $(ARGS)

undo:
	./neatify --source $(DIR) --undo

dev: build preview

verify:
	./mvnw verify

clean:
	./mvnw clean
