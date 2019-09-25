#!/bin/bash
echo "Translating LTL formula to CNF"
infile=$1
outfile=$2
bound=$3
solver=$4

./lib/ltl-model-counter/ltl2pl_macos $infile $outfile $bound

plfile=$2-k$3.sat
auxfile=$2-k$3.aux
./lib/ltl-model-counter/bool2cnf_macos -s $plfile > $auxfile 

varsfile=$2-k$3.vars
cnffile=$2-k$3.cnf

> $varsfile
> $cnffile

vars=true
while read -r line; do
	if $vars ;  
	then
		if [[ $line == p* ]] 	
		then
			vars=false
			echo "$line"  >> $cnffile
		else
			echo "$line"  >> $varsfile
		fi
	else
		echo "$line"  >> $cnffile
	fi
done < "$auxfile"

if [[ $solver == cachet ]]
then
	./lib/ltl-model-counter/cachet $cnffile
else
	if [[ $solver == miniC2D ]]
	then
		./lib/ltl-model-counter/miniC2D -C -c $cnffile
	
	else
		./lib/ltl-model-counter/relsat_macos -p0 -#c -u300 -on -t3600 -v $varsfile $cnffile 
	fi
fi