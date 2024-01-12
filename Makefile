# Makefile

# Check if Java 18 or higher is available
MIN_JAVA_VERSION := 18
JAVA_VERSION := $(shell java -version 2>&1 | grep -E -o 'version "([0-9]+)\.([0-9]+)\.([0-9]+)' | awk -F'"' '{print $$2}' | awk -F'.' '{print $$1}')

$(info Detected Java version: $(JAVA_VERSION))

ifeq ($(shell [ $(JAVA_VERSION) -ge $(MIN_JAVA_VERSION) ] && echo 1),1)
    JAVA_HOME := $(shell dirname $(shell dirname $(shell readlink -f $$(which javac))))
    $(info Java version is $(JAVA_VERSION), setting JAVA_HOME to $(JAVA_HOME))
else
    $(error Java $(MIN_JAVA_VERSION) or higher is not installed. Please install Java $(MIN_JAVA_VERSION) or later.)
endif

# Set JAVA_HOME environment variable
export JAVA_HOME

.PHONY: compile

# Default target
all: compile

# Target to run 'ant compile'
compile:
	$(info Compiling with Ant...)
	ant compile
