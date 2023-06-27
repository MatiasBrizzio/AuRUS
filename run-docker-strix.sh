#!/bin/bash
#BASE="$(basename "$1")"
FORMULA=$1
INPUT=$2
OUTPUT=$3
#cp $1 docker/$BASE
DOCKER_BUILDKIT=0 docker build --no-cache --build-arg formula="$FORMULA" --build-arg input="$INPUT" --build-arg output="$OUTPUT" "${PWD}/../unreal-repair/docker/."
docker rmi -f $(docker images | grep "^<none>" | awk '{print $3}')
