#!/bin/bash
BASE="$(basename "$1")"
cp $1 docker/$BASE
docker build --no-cache --build-arg filename=$BASE docker/.
docker rmi -f $(docker images | grep "^<none>" | awk '{print $3}')
