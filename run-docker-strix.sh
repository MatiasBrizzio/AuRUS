#!/bin/bash
docker build --no-cache docker/.
docker rmi -f $(docker images | grep "^<none>" | awk '{print $3}')
