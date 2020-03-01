#!/bin/bash
for K in {0..9}
do
	sbatch --job-name=$K-SReal --output=sensitivityresult/sensitivity-Real-$K.out ./run-all-sensitivity.sh $K "REAL" 
	#sbatch --job-name=$K-SSyn --output=sensitivityresult/sensitivity-Syn-$K.out ./run-all-sensitivity.sh $K "SYN" 
	#sbatch --job-name=$K-SSem --output=sensitivityresult/sensitivity-Sem-$K.out ./run-all-sensitivity.sh $K "SEM" 
done


