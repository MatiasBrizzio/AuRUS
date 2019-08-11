#!/bin/bash
echo "Translating LTL formula to CNF"
infile=$1
outfile=$2
bound=$3
./lib/ltl-model-counter/ltl2pl $infile $outfile $bound

plfile=$2-k$3.sat
auxfile=$2-k$3.aux
./lib/ltl-model-counter/bool2cnf -s $plfile > $auxfile 

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
./lib/ltl-model-counter/relsat -p0 -#c -u300 -on -t3600 -v $varsfile $cnffile 
#./lib/ltl-model-counter/cachet $cnffile
#./lib/ltl-model-counter/miniC2D -C -c $cnffile