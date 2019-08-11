#!/bin/bash
echo "Translating LTL formula to CNF"
infile=$1
outfile=$2
bound=$3
./ltl2pl $infile $outfile $bound

plfile=$2-k$3.sat
cnffile=$2-k$3.cnf
./bool2cnf $plfile > $cnffile 
