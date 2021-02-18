# echo "RANDOM...."

declare -a CONFIGS=("0-0-0" "0-0-100" "0-100-0" "0-50-50" "100-0-0" "70-0-30" "70-30-0" "50-25-25" "50-15-35" "50-20-30" "60-20-20" "60-15-25" "70-15-15" "70-10-20" "70-20-10" "80-10-10" "80-07-13" "90-05-05")

for CONFIG in ${CONFIGS[@]}; do
	echo "$CONFIG...."
	sbatch --job-name=$CONFIG-HumanoidLTL_503 --output=statistics/$CONFIG-HumanoidLTL_503.out ./compute-SynSemDistances.sh examples/icse2019/SYNTECH15/tlsf_specs/HumanoidLTL_503_Humanoid_fixed_unrealizable.tlsf result-spectra-$CONFIG/SYNTECH15/HumanoidLTL_503_Humanoid_fixed_unrealizable/HumanoidLTL_503_Humanoid_fixed_unrealizable-genuine

	sbatch --job-name=$CONFIG-HumanoidLTL_531 --output=statistics/$CONFIG-HumanoidLTL_531.out ./compute-SynSemDistances.sh examples/icse2019/SYNTECH15/tlsf_specs/HumanoidLTL_531_Humanoid_unrealizable.tlsf result-spectra-$CONFIG/SYNTECH15/HumanoidLTL_531_Humanoid_unrealizable/HumanoidLTL_531_Humanoid_unrealizable-genuine

	sbatch --job-name=$CONFIG-HumanoidLTL_741 --output=statistics/$CONFIG-HumanoidLTL_741.out ./compute-SynSemDistances.sh examples/icse2019/SYNTECH15/tlsf_specs/HumanoidLTL_741_Humanoid_unrealizable.tlsf result-spectra-$CONFIG/SYNTECH15/HumanoidLTL_741_Humanoid_unrealizable/HumanoidLTL_741_Humanoid_unrealizable-genuine

	sbatch --job-name=$CONFIG-HumanoidLTL_742 --output=statistics/$CONFIG-HumanoidLTL_742.out ./compute-SynSemDistances.sh examples/icse2019/SYNTECH15/tlsf_specs/HumanoidLTL_742_Humanoid_unrealizable.tlsf result-spectra-$CONFIG/SYNTECH15/HumanoidLTL_742_Humanoid_unrealizable/HumanoidLTL_742_Humanoid_unrealizable-genuine

	sbatch --job-name=$CONFIG-PcarLTL_769 --output=statistics/$CONFIG-PcarLTL_769.out ./compute-SynSemDistances.sh examples/icse2019/SYNTECH15/tlsf_specs/PcarLTL_769_PCar_fixed_unrealizable.tlsf result-spectra-$CONFIG/SYNTECH15/PcarLTL_769_PCar_fixed_unrealizable/PcarLTL_769_PCar_fixed_unrealizable-genuine

	sbatch --job-name=$CONFIG-PCarLTL_Unrealizable_V_1 --output=statistics/$CONFIG-PCarLTL_Unrealizable_V_1.out ./compute-SynSemDistances.sh examples/icse2019/SYNTECH15/tlsf_specs/PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable.tlsf result-spectra-$CONFIG/SYNTECH15/PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable/PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable-genuine	

	sbatch --job-name=$CONFIG-PCarLTL_Unrealizable_V_2 --output=statistics/$CONFIG-PCarLTL_Unrealizable_V_2.out ./compute-SynSemDistances.sh examples/icse2019/SYNTECH15/tlsf_specs/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable.tlsf result-spectra-$CONFIG/SYNTECH15/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable-genuine	

done
