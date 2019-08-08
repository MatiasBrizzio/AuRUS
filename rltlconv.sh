#!/bin/bash
BASEDIR="$(dirname "$0")"
java -jar "$BASEDIR/lib/rltlconv.jar" "$@"
