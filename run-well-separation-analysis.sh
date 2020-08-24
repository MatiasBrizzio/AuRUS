#!/bin/bash

# declare -a CASE_STUDIES=("arbiter" "minepump" "lilydemo02" "RG1" "RG2" "Lift" "HumanoidLTL_458" "GyroUnrealizable_Var1" "GyroUnrealizable_Var2")
declare -a CASE_STUDIES=("detector" "full_arbiter" "lily02" "lily11" "lily15" "lily16" "load_balancer" "ltl2dba_R_2" "ltl2dba_theta_2" "ltl2dba27" "prioritized_arbiter" "round_robin" "simple_arbiter")
#declare -a CONFIGS=("50-25-25" "50-15-35" "50-20-30" "60-20-20" "60-15-25" "70-15-15" "70-10-20" "70-20-10" "80-10-10" "80-07-13" "90-05-05")
declare -a CONFIGS=("random")
#CONFIG_FOLDER=$1
#EXP=$2
for case_study in ${CASE_STUDIES[@]}; do
	#dir="$(dirname $tlsf_file)"   # Returns "/from/hear/to"
	for config in ${CONFIGS[@]}; do
		oname=results-syntcomp/result-syntcomp-$config/syntcomp-unreal/$case_study/$case_study-wellseparation.csv
		> $oname
		for K in {0..9}; do
			echo "$case_study"
			fname=results-syntcomp/result-syntcomp-$config/syntcomp-unreal/$case_study/$case_study-genuine-$K
			echo "$oname"
			./check-well-separation.sh -d=$fname -out=$oname
		done
	done
done
