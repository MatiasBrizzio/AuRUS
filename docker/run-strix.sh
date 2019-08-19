#!/bin/bash
formula=`cat formula.ltl`
./bin/strix -r -f "$formula"