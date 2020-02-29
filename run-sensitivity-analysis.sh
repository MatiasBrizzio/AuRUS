#!/bin/bash
CONFIG_REAL="-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -geneNUM=0 -onlySAT -factors=.0,.5,.5"
CONFIG_SYN="-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -geneNUM=0 -factors=.5,.0,.5"
CONFIG_SEM="-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -geneNUM=0 -factors=.5,.5,.0"

for K in {0..9}
do
	sbatch --job-name=$K-SReal --output=sensitivityresult/sensitivity-Real-$K.out ./run-all-sensitivity.sh $K "REAL" $CONFIG_REAL
	sbatch --job-name=$K-SSyn --output=sensitivityresult/sensitivity-Syn-$K.out ./run-all-sensitivitys.sh $K "SYN" $CONFIG_SYN
	sbatch --job-name=$K-SSem --output=sensitivityresult/sensitivity-Sem-$K.out ./run-all-sensitivity.sh $K "SEM" $CONFIG_SEM
done


