#!/bin/bash

# Fitness factors (as percentage)
REAL_INIT=70
SYN_INIT=10
WEAK_INIT=0
# STRONG_INIT would be computed as 100 - REAL_INIT - SYN_INIT - WEAK_INIT

CASE_STUDY_SPEC="case-studies/arbiter/arbiter.tlsf"
# Handcrafted reference solutions for the arbiter case study
REFERENCE=(-ref=case-studies/arbiter/genuine/arbiter_fixed0.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed1.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed2.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed3.tlsf)
FLAGS=(-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA)
# The number of runs per configuration
N_RUNS_PER_CONFIG=100

ant compile

total_time=0
for run in $(seq 1 $N_RUNS_PER_CONFIG); do
    for i in {0..20..5}; do
        weak_tmp=$((WEAK_INIT + i))
        strong_tmp=$((100 - REAL_INIT - SYN_INIT - weak_tmp))
        # Convert percentages to factors between 0 and 1
        FACTORS="-factors=$(bc<<<"scale=2; ${REAL_INIT}/100"),$(bc<<<"scale=2; ${SYN_INIT}/100"),$(bc<<<"scale=2; ${weak_tmp}/100"),$(bc<<<"scale=2; ${strong_tmp}/100")"
        OUT=-out=result/arbiter-$REAL_INIT-$SYN_INIT-$weak_tmp-$strong_tmp/run_$run

        echo -n "  Running ${REAL_INIT}-${SYN_INIT}-${weak_tmp}-${strong_tmp}: "
        SECONDS=0
        ./unreal-repair.sh "${FLAGS[@]}" "$OUT" "${REFERENCE[@]}" "$FACTORS" "$CASE_STUDY_SPEC" >/dev/null 2>&1
        echo "completed in $SECONDS seconds"
        total_time=$((total_time + SECONDS))
    done
    echo "Run $run/$N_RUNS_PER_CONFIG completed for all configs."
done
echo "Total time taken: $total_time seconds"
