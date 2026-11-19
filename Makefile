# Project tasks — canonical commands for humans and agents. Keep README § Run it and AGENTS.md in sync with this file.

# Use Android Studio's bundled JDK when no JAVA_HOME is set (macOS default install path).
STUDIO_JDK := /Applications/Android Studio.app/Contents/jbr/Contents/Home
# (shell test, not $(wildcard): the path contains spaces)
export JAVA_HOME ?= $(shell test -d '$(STUDIO_JDK)' && echo '$(STUDIO_JDK)')

GRADLE := ./gradlew
APP_ID := gugabguerra.treineco

.PHONY: help run build release test lint fmt clean

help: ## Show available tasks
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-12s %s\n", $$1, $$2}'

run: ## Install the debug build on the connected device/emulator and launch it
	$(GRADLE) installDebug
	adb shell am start -n $(APP_ID)/com.example.gymplan.MainActivity

build: ## Build the debug APK (app/build/outputs/apk/debug/)
	$(GRADLE) assembleDebug

release: ## Build the signed, minified release APK (app/build/outputs/apk/release/); needs docs/local/keystore.properties
	@test -f docs/local/keystore.properties || { echo "Missing docs/local/keystore.properties (release signing key)"; exit 1; }
	$(GRADLE) assembleRelease

test: ## Run JVM unit tests
	$(GRADLE) testDebugUnitTest

lint: ## Run Android Lint
	$(GRADLE) lintDebug

fmt: ## Format code (no formatter configured yet)
	@echo "No formatter configured — follow the official Kotlin style (see docs/CONVENTIONS.md)."

clean: ## Remove build artifacts
	$(GRADLE) clean
