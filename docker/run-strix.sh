#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

LTL=$(./syfco -f ltlxba -m fully $1)
INS=$(./syfco  -f ltlxba --print-input-signals $1)
OUTS=$(./syfco  -f ltlxba --print-output-signals $1)

./bin/strix -f "$LTL" --ins "$INS" --outs "$OUTS" ${@:2}