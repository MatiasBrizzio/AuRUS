#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

FORMULA=$(./lib/syfco_macos -f ltlxba -m fully $1)
INPUT=$(./lib/syfco_macos -f ltlxba --print-input-signals $1)
OUTPUT=$(./lib/syfco_macos -f ltlxba --print-output-signals $1)

#BASE="$(basename "$LTL")"


DOCKER_BUILDKIT=0 docker build --no-cache --build-arg formula="$FORMULA" --build-arg input="$INPUT" --build-arg output="$OUTPUT" "${PWD}/../unreal-repair/docker/."
docker rmi -f $(docker images | grep "^<none>" | awk '{print $3}')
