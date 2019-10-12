#!/bin/bash
for file in `find examples/modelcountExamples/random_formulas/ -iname "*.form*" -print`; do
	echo "Running $file"
	base=$(basename $file)
    folder="result/random_formulas/${base%.*}"
    outname="${base%.*}.out"
    mkdir $folder
	./modelcount.sh -b="$file" -vars="a,b,c" -out="$folder/$outname" -k=10 -prefixes 
done
