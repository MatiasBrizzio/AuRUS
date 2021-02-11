#!/bin/bash

ORIG=$1
for K in {0..9}
do
	DIR=$2-$K
	./SynSemDistanceAnalysis.sh -o=$ORIG $DIR
done
