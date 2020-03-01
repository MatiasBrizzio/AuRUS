#!/bin/bash
RESULT_DIR="result"
for K in {0..9}
do
	sbatch --job-name=$K-UnrealRepair --output=$RESULT_DIR/all-together-$K.out ./run-all-together.sh $K $RESULT_DIR
	#sbatch --job-name=$K-Random --output=$RESULT_DIR/random-all-together-$K.out ./run-random-all-together.sh $K $RESULT_DIR
done
