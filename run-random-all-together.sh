#!/bin/bash -l

#SBATCH -n 1
#SBATCH -N 1
#SBATCH -c 4
#SBATCH --time=0-24:00:00
#SBATCH -p batch
#SBATCH --qos=qos-batch
#SBATCH -J Random-Unreal-Repair
#SBATCH --mail-user=renzo.degiovanni@uni.lu
#SBATCH --mail-type=all

module purge

export BASEDIR=/home/users/rdegiovanni/unreal-repair

export JAVA_HOME=/home/users/rdegiovanni/envlib/java-11-oracle/
export ANT_HOME=/home/users/rdegiovanni/envlib/ant/
export LIB_HOME=/home/users/rdegiovanni/envlib/lib/
#export LD_LIBRARY_PATH=~/envlib/lib/:~/envlib/clib/:$LD_LIBRARY_PATH
export PATH=$JAVA_HOME/bin:$ANT_HOME/bin:$LIB_HOME:$PATH

module load tools/Singularity/3.5.2

pushd ${BASEDIR}
echo $JAVA_HOME
echo $PATH

echo $(java -version)
echo $(javac -version)

ant compile

K=$1
#for K in {9..9}
#do
echo "Running Minepump..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/minepump/genuine/minepump_fixed0.tlsf -ref=case-studies/minepump/genuine/minepump_fixed1.tlsf -ref=case-studies/minepump/genuine/minepump_fixed2.tlsf -out=result/minepump/minepump-random-$K case-studies/minepump/minepump.tlsf > result/minepump/minepump-random-$K.out 

echo "Running Arbiter..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/arbiter/genuine/arbiter_fixed0.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed1.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed2.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed3.tlsf -out=result/arbiter/arbiter-random-$K case-studies/arbiter/arbiter.tlsf > result/arbiter/arbiter-random-$K.out  

echo "Running Lily02..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/lily02/genuine/lilydemo02_fixed.tlsf -out=result/lilydemo02/lilydemo02-random-$K case-studies/lily02/lilydemo02.tlsf > result/lilydemo02/lilydemo02-random-$K.out 

echo "Running RG1..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/RG1/genuine/RG1_fixed0.tlsf -ref=case-studies/RG1/genuine/RG1_fixed1.tlsf -ref=case-studies/RG1/genuine/RG1_fixed2.tlsf -ref=case-studies/RG1/genuine/RG1_fixed3.tlsf -out=result/RG1/RG1-random-$K case-studies/RG1/RG1.tlsf > result/RG1/RG1-random-$K.out 

echo "Running RG2..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/RG2/genuine/RG2_fixed0.tlsf -ref=case-studies/RG2/genuine/RG2_fixed1.tlsf -out=result/RG2/RG2-random-$K case-studies/RG2/RG2.tlsf > result/RG2/RG2-random-$K.out 

echo "Running Lift..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/lift/genuine/Lift_fixed0.tlsf -ref=case-studies/lift/genuine/Lift_fixed1.tlsf -out=result/Lift/Lift-random-$K case-studies/lift/Lift.tlsf > result/Lift/Lift-random-$K.out  

echo "Running HumanoidLTL_458..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/HumanoidLTL_458/genuine/HumanoidLTL_460_Humanoid.tlsf -out=result/HumanoidLTL_458/HumanoidLTL_458-random-$K case-studies/HumanoidLTL_458/HumanoidLTL_458_Humanoid_fixed_unrealizable.tlsf > result/HumanoidLTL_458/HumanoidLTL_458-random-$K.out 

#echo "Running HumanoidLTL_531..."
#sbatch --output=result/HumanoidLTL_531/HumanoidLTL_531-genuine-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/HumanoidLTL_531/genuine/HumanoidLTL_533_Humanoid.tlsf -out=result/HumanoidLTL_531/HumanoidLTL_531-genuine-$K case-studies/HumanoidLTL_531/HumanoidLTL_531_Humanoid_unrealizable.tlsf

echo "Running GyroUnrealizable_Var1..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/GyroUnrealizable_Var1/genuine/GyroLTLVar3_702_GyroAspect.tlsf -out=result/GyroUnrealizable_Var1/GyroUnrealizable_Var1-random-$K case-studies/GyroUnrealizable_Var1/GyroUnrealizable_Var1_710_GyroAspect_unrealizable.tlsf > result/GyroUnrealizable_Var1/GyroUnrealizable_Var1-random-$K.out 

echo "Running GyroUnrealizable_Var2..."
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=1000 -k=20 -GATO=7200 -addA -onlyInputsA -random -ref=case-studies/GyroUnrealizable_Var2/genuine/GyroLTLVar3_702_GyroAspect.tlsf -out=result/GyroUnrealizable_Var2/GyroUnrealizable_Var2-random-$K case-studies/GyroUnrealizable_Var2/GyroUnrealizable_Var2_710_GyroAspect_unrealizable.tlsf > result/GyroUnrealizable_Var2/GyroUnrealizable_Var2-random-$K.out 
#done
echo "Finished."

popd
