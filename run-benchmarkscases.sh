#!/bin/bash
for file in `find examples/modelcountExamples/random_formulas/ -iname "*.form*" -print`; do
	echo "Running $file"
	outname="${file%.*}.out"
	./modelcount.sh -b="$file" -vars="a,b,c" -k=20 -prefixes 
done