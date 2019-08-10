#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

LTL=$(./lib/syfco -f ltlxba -m fully $1)
INS=$(./lib/syfco  -f ltlxba --print-input-signals $1)
OUTS=$(./lib/syfco  -f ltlxba --print-output-signals $1)

./lib/new_strix/strix -f "$LTL" --ins "$INS" --outs "$OUTS" ${@:2}
