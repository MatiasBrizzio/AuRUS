#!/bin/bash
echo "Translating LTL formula to CNF"
infile=$1
outfile=$2
bound=$3
solver=$4

./lib/ltl-model-counter/ltl2pl $infile $outfile $bound

plfile=$2-k$3.sat
auxfile=$2-k$3.aux
./lib/ltl-model-counter/bool2cnf -s $plfile > $auxfile 

varsfile=$2-k$3.vars
cnffile=$2-k$3.cnf

> $varsfile
> $cnffile
if [[ $solver == ganak ]]
then
    echo "c ind "|tr -d '\n' > $cnffile
fi
vars=true
while read -r line; do
	if $vars ;  
	then
		if [[ $line == p* ]] 	
		then
			vars=false
            if [[ $solver == ganak ]]
            then
                echo "0" >> $cnffile
            fi
			echo "$line"  >> $cnffile
		else
			echo "$line"  >> $varsfile
            if [[ $solver == ganak ]]
            then
                echo "${line} "|tr -d '\n' >> $cnffile
            fi
		fi
	else
		echo "$line"  >> $cnffile
	fi
done < "$auxfile"
if [[ $solver == cachet ]]
then
	./lib/ltl-model-counter/cachet $cnffile
else
	if [[ $solver == ganak ]]
	then
		python3 lib/ltl-model-counter/ganak.py $cnffile
	
	else
		./lib/ltl-model-counter/relsat -p0 -#c -u300 -on -t3600 -v $varsfile $cnffile 
	fi
fi
