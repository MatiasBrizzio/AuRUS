#!/bin/bash

# Fitness factors (as percentage)
REAL_INIT=70
SYN_INIT=10
WEAK_INIT=0
# STRONG_INIT would be computed as 100 - REAL_INIT - SYN_INIT - WEAK_INIT

CASE_STUDY_SPEC="case-studies/arbiter/arbiter.tlsf"
# The number of runs per configuration
N_RUNS_PER_CONFIG=150

total_time=0
for i in {0..20..5}; do
    weak_tmp=$((WEAK_INIT + i))
    strong_tmp=$((100 - REAL_INIT - SYN_INIT - weak_tmp))

    FACTORS="-factors=$(bc<<<"scale=2; ${REAL_INIT}/100"),$(bc<<<"scale=2; ${SYN_INIT}/100"),$(bc<<<"scale=2; ${weak_tmp}/100"),$(bc<<<"scale=2; ${strong_tmp}/100")"
    FLAGS=(-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA)
    # Handcrafted reference solutions for the arbiter case study
    REFERENCE=(-ref=case-studies/arbiter/genuine/arbiter_fixed0.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed1.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed2.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed3.tlsf)

    for run in $(seq 1 $N_RUNS_PER_CONFIG);
    do
        SECONDS=0
        OUT=-out=result/arbiter-$REAL_INIT-$SYN_INIT-$weak_tmp-$strong_tmp/run_$run
        ./unreal-repair.sh "${FLAGS[@]}" "$OUT" "${REFERENCE[@]}" "$FACTORS" "$CASE_STUDY_SPEC" >/dev/null 2>&1
        echo "  Run completed in $SECONDS seconds"
        total_time=$((total_time + SECONDS))
    done
    echo "Experiment with factors Real=${REAL_INIT}, Syn=${SYN_INIT}, Weak=${weak_tmp}, Strong=${strong_tmp} completed."
done

echo "Total time taken: $total_time seconds"
