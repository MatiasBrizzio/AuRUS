#!/bin/bash
for K in {0..9}
do
	sbatch --output=result/all-together-$K.out ./run-all-together.sh $K
	#sbatch --output=result/random-all-together-$K.out ./run-random-all-together.sh $K
done
