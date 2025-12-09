#!/bin/bash

set -e

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <results_directory>"
    exit 1
fi

ant compile

FOLDERS=("$1"/*)

for folder in "${FOLDERS[@]}"; do

    echo "Processing folder: $folder"
    if [ ! -d "$folder" ]; then
        continue
    fi
    subfolders=("$folder"/*)

    for subfolder in "${subfolders[@]}"; do
        if [ ! -d "$subfolder" ]; then
            continue
        fi
        echo "  Processing subfolder: $subfolder"
        java \
            -Xmx8g -Djava.library.path=/usr/local/lib -cp \
            "bin:lib/commons-math3-3.6.1.jar:lib/rltlconv.jar:lib/JFLAP-7.0_With_Source.jar:lib/owl-18.10-snapshot.jar:lib/ejml/ejml-core-0.34.jar:lib/ejml/ejml-cdense-0.34.jar:lib/ejml/ejml-ddense-0.34.jar:lib/ejml/ejml-fdense-0.34.jar:lib/ejml/ejml-simple-0.34.jar:lib/ejml/ejml-zdense-0.34.jar:lib/ejml/ejml-dsparse-0.34.jar:lib/ejml/ejml-experimental-0.34.jar:lib/ltl2buchi.jar" main.WellSeparationAnalysis \
            "-d=$subfolder"
    done
done
