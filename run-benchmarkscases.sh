#!/bin/bash
for K in {0..0}
do
echo "Running Minepump..."
sbatch --output=result/minepump/minepump-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/minepump/genuine/minepump_fixed0.tlsf -ref=case-studies/minepump/genuine/minepump_fixed1.tlsf -ref=case-studies/minepump/genuine/minepump_fixed2.tlsf -out=result/minepump/minepump-genuine-$K case-studies/minepump/minepump.tlsf

echo "Running Arbiter..."
sbatch --output=result/arbiter/arbiter-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/arbiter/genuine/arbiter_fixed0.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed1.tlsf -out=result/arbiter/arbiter-genuine-$K case-studies/arbiter/arbiter.tlsf 

echo "Running Lily02..."
sbatch --output=result/lilydemo02/lilydemo02-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/lily02/genuine/lilydemo02_fixed.tlsf -out=result/lilydemo02/lilydemo02-genuine-$K case-studies/lily02/lilydemo02.tlsf

echo "Running RG1..."
sbatch --output=result/RG1/RG1-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/RG1/genuine/RG1_fixed0.tlsf -ref=case-studies/RG1/genuine/RG1_fixed1.tlsf -ref=case-studies/RG1/genuine/RG1_fixed2.tlsf -ref=case-studies/RG1/genuine/RG1_fixed3.tlsf -out=result/RG1/RG1-genuine-$K case-studies/RG1/RG1.tlsf

echo "Running RG2..."
sbatch --output=result/RG2/RG2-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/RG2/genuine/RG2_fixed0.tlsf -ref=case-studies/RG2/genuine/RG2_fixed1.tlsf -out=result/RG2/RG2-genuine-$K case-studies/RG2/RG2.tlsf

echo "Running Lift..."
sbatch --output=result/Lift/Lift-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/lift/genuine/Lift_fixed0.tlsf -ref=case-studies/lift/genuine/Lift_fixed1.tlsf -out=result/Lift/Lift-genuine-$K case-studies/lift/Lift.tlsf

echo "Running HumanoidLTL_458..."
sbatch --output=result/HumanoidLTL_458/HumanoidLTL_458-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/HumanoidLTL_458/genuine/HumanoidLTL_460_Humanoid.tlsf -out=result/HumanoidLTL_458/HumanoidLTL_458-genuine-$K case-studies/HumanoidLTL_458/HumanoidLTL_458_Humanoid_fixed_unrealizable.tlsf

echo "Running HumanoidLTL_531..."
sbatch --output=result/HumanoidLTL_531/HumanoidLTL_531-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/HumanoidLTL_531/genuine/HumanoidLTL_533_Humanoid.tlsf -out=result/HumanoidLTL_531/HumanoidLTL_531-genuine-$K case-studies/HumanoidLTL_531/HumanoidLTL_531_Humanoid_unrealizable.tlsf

echo "Running GyroUnrealizable_Var1..."
sbatch --output=result/GyroUnrealizable_Var1/GyroUnrealizable_Var1-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/GyroUnrealizable_Var1/genuine/GyroLTLVar3_702_GyroAspect.tlsf -out=result/GyroUnrealizable_Var1/GyroUnrealizable_Var1-genuine-$K case-studies/GyroUnrealizable_Var1/GyroUnrealizable_Var1_710_GyroAspect_unrealizable.tlsf

echo "Running GyroUnrealizable_Var2..."
sbatch --output=result/GyroUnrealizable_Var2/GyroUnrealizable_Var2-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/GyroUnrealizable_Var2/genuine/GyroLTLVar3_702_GyroAspect.tlsf -out=result/GyroUnrealizable_Var2/GyroUnrealizable_Var2-genuine-$K case-studies/GyroUnrealizable_Var2/GyroUnrealizable_Var2_710_GyroAspect_unrealizable.tlsf
done
