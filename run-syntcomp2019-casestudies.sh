#!/bin/bash
for tlsf_file in `find examples/syntcomp2019/LTL-Real/ -iname "*unreal*" -print`; do
	#dir="$(dirname $tlsf_file)"   # Returns "/from/hear/to"
	echo "Running $tlsf_file"
	#base="$(basename $tlsf_file)"  # Returns just "to"
	outname="${tlsf_file%.*}.out"
		#echo `mkdir results/$dir/`
	#echo `cp $res_file results/$dir/`
	./unreal-repair.sh $tlsf_file > $outname
	#echo $tlsf_file
done
