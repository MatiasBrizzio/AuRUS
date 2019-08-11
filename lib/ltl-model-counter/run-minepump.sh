#!/bin/bash
for K in {3..3}
do
	echo "K = $K"
	echo "MinePump"
	for infile in `ls case-studies/minepump/*.ltl`; do
		param=${infile%.ltl}
		outfile=$param-k$K.out
		./ltl-modelcounter.sh $infile $param $K > $outfile
	done
done

