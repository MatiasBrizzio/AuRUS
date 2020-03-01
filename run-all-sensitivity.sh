#!/bin/bash -l

#SBATCH -n 1
#SBATCH -N 1
#SBATCH -c 4
#SBATCH --time=0-24:00:00
#SBATCH -p batch
#SBATCH --qos=qos-batch
#SBATCH -J Sensitivity
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
FACTOR=$2
CONFIG='-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -geneNUM=0 -onlySAT -factors=.0,.5,.5'
#CONFIG='-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -geneNUM=0 -factors=.5,.0,.5'
#CONFIG='-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -geneNUM=0 -factors=.5,.5,.0'

echo "Running Minepump..."
./unreal-repair.sh $CONFIG -ref=case-studies/minepump/genuine/minepump_fixed0.tlsf -ref=case-studies/minepump/genuine/minepump_fixed1.tlsf -ref=case-studies/minepump/genuine/minepump_fixed2.tlsf -out=sensitivityresult/minepump/minepump-$FACTOR-$K case-studies/minepump/minepump.tlsf > sensitivityresult/minepump/minepump-$FACTOR-$K.out 

echo "Running Arbiter..."
./unreal-repair.sh $CONFIG -ref=case-studies/arbiter/genuine/arbiter_fixed0.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed1.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed2.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed3.tlsf -out=sensitivityresult/arbiter/arbiter-$FACTOR-$K case-studies/arbiter/arbiter.tlsf > sensitivityresult/arbiter/arbiter-$FACTOR-$K.out  

echo "Running Lily02..."
./unreal-repair.sh $CONFIG -ref=case-studies/lily02/genuine/lilydemo02_fixed.tlsf -out=sensitivityresult/lilydemo02/lilydemo02-$FACTOR-$K case-studies/lily02/lilydemo02.tlsf > sensitivityresult/lilydemo02/lilydemo02-$FACTOR-$K.out 

echo "Running RG1..."
./unreal-repair.sh $CONFIG -ref=case-studies/RG1/genuine/RG1_fixed0.tlsf -ref=case-studies/RG1/genuine/RG1_fixed1.tlsf -ref=case-studies/RG1/genuine/RG1_fixed2.tlsf -ref=case-studies/RG1/genuine/RG1_fixed3.tlsf -out=sensitivityresult/RG1/RG1-$FACTOR-$K case-studies/RG1/RG1.tlsf > sensitivityresult/RG1/RG1-$FACTOR-$K.out 

echo "Running RG2..."
./unreal-repair.sh $CONFIG -ref=case-studies/RG2/genuine/RG2_fixed0.tlsf -ref=case-studies/RG2/genuine/RG2_fixed1.tlsf -out=sensitivityresult/RG2/RG2-$FACTOR-$K case-studies/RG2/RG2.tlsf > sensitivityresult/RG2/RG2-$FACTOR-$K.out 

echo "Running Lift..."
./unreal-repair.sh $CONFIG -ref=case-studies/lift/genuine/Lift_fixed0.tlsf -ref=case-studies/lift/genuine/Lift_fixed1.tlsf -out=sensitivityresult/Lift/Lift-$FACTOR-$K case-studies/lift/Lift.tlsf > sensitivityresult/Lift/Lift-$FACTOR-$K.out  

echo "Running HumanoidLTL_458..."
./unreal-repair.sh $CONFIG -ref=case-studies/HumanoidLTL_458/genuine/HumanoidLTL_460_Humanoid.tlsf -out=sensitivityresult/HumanoidLTL_458/HumanoidLTL_458-$FACTOR-$K case-studies/HumanoidLTL_458/HumanoidLTL_458_Humanoid_fixed_unrealizable.tlsf > sensitivityresult/HumanoidLTL_458/HumanoidLTL_458-$FACTOR-$K.out 

#echo "Running HumanoidLTL_531..."
#sbatch --output=sensitivityresult/HumanoidLTL_531/HumanoidLTL_531-$FACTOR-$K.out ./run-job.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -onlyInputsA -ref=case-studies/HumanoidLTL_531/genuine/HumanoidLTL_533_Humanoid.tlsf -out=sensitivityresult/HumanoidLTL_531/HumanoidLTL_531-$FACTOR-$K case-studies/HumanoidLTL_531/HumanoidLTL_531_Humanoid_unrealizable.tlsf

echo "Running GyroUnrealizable_Var1..."
./unreal-repair.sh $CONFIG -ref=case-studies/GyroUnrealizable_Var1/genuine/GyroLTLVar3_702_GyroAspect.tlsf -out=sensitivityresult/GyroUnrealizable_Var1/GyroUnrealizable_Var1-$FACTOR-$K case-studies/GyroUnrealizable_Var1/GyroUnrealizable_Var1_710_GyroAspect_unrealizable.tlsf > sensitivityresult/GyroUnrealizable_Var1/GyroUnrealizable_Var1-$FACTOR-$K.out 

echo "Running GyroUnrealizable_Var2..."
./unreal-repair.sh $CONFIG -ref=case-studies/GyroUnrealizable_Var2/genuine/GyroLTLVar3_702_GyroAspect.tlsf -out=sensitivityresult/GyroUnrealizable_Var2/GyroUnrealizable_Var2-$FACTOR-$K case-studies/GyroUnrealizable_Var2/GyroUnrealizable_Var2_710_GyroAspect_unrealizable.tlsf > sensitivityresult/GyroUnrealizable_Var2/GyroUnrealizable_Var2-$FACTOR-$K.out 

#done

echo "Finished."
popd
