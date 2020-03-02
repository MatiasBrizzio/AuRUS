#!/bin/bash -l

#SBATCH -n 1
#SBATCH -N 1
#SBATCH -c 4
#SBATCH --time=0-24:00:00
#SBATCH -p batch
#SBATCH --qos=qos-batch
#SBATCH -J Unreal-Repair
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

#K=$1
INPUT_DIR=$1
RESULT_DIR="statistics"
for K in {0..9}
do
echo "Running Minepump..."
mkdir -p $RESULT_DIR/minepump/minepump-genuine-$K/
./compute-statistics.sh -ref=case-studies/minepump/genuine/minepump_fixed0.tlsf -ref=case-studies/minepump/genuine/minepump_fixed1.tlsf -ref=case-studies/minepump/genuine/minepump_fixed2.tlsf -o=case-studies/minepump/minepump.tlsf $INPUT_DIR/minepump/minepump-genuine-$K > $RESULT_DIR/minepump/minepump-genuine-$K/out.txt 

#echo "Running Arbiter..."
#mkdir -p $RESULT_DIR/arbiter/arbiter-genuine-$K/
#./compute-statistics.sh -ref=case-studies/arbiter/genuine/arbiter_fixed0.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed1.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed2.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed3.tlsf -o=case-studies/arbiter/arbiter.tlsf $INPUT_DIR/arbiter/arbiter-genuine-$K  > $RESULT_DIR/arbiter/arbiter-genuine-$K/out.txt  

#echo "Running Lily02..."
#mkdir -p $RESULT_DIR/lilydemo02/lilydemo02-genuine-$K/
#./compute-statistics.sh -ref=case-studies/lily02/genuine/lilydemo02_fixed.tlsf -o=case-studies/lily02/lilydemo02.tlsf $INPUT_DIR/lilydemo02/lilydemo02-genuine-$K  > $RESULT_DIR/lilydemo02/lilydemo02-genuine-$K/out.txt 

#echo "Running RG1..."
#mkdir -p $RESULT_DIR/RG1/RG1-genuine-$K/
#./compute-statistics.sh -ref=case-studies/RG1/genuine/RG1_fixed0.tlsf -ref=case-studies/RG1/genuine/RG1_fixed1.tlsf -ref=case-studies/RG1/genuine/RG1_fixed2.tlsf -ref=case-studies/RG1/genuine/RG1_fixed3.tlsf -o=case-studies/RG1/RG1.tlsf $INPUT_DIR/RG1/RG1-genuine-$K > $RESULT_DIR/RG1/RG1-genuine-$K/out.txt 

#echo "Running RG2..."
#mkdir -p $RESULT_DIR/RG2/RG2-genuine-$K/
#./compute-statistics.sh -ref=case-studies/RG2/genuine/RG2_fixed0.tlsf -ref=case-studies/RG2/genuine/RG2_fixed1.tlsf -o=case-studies/RG2/RG2.tlsf $INPUT_DIR/RG2/RG2-genuine-$K > $RESULT_DIR/RG2/RG2-genuine-$K/out.txt 

#echo "Running Lift..."
#mkdir -p $RESULT_DIR/Lift/Lift-genuine-$K/
#./compute-statistics.sh -ref=case-studies/lift/genuine/Lift_fixed0.tlsf -ref=case-studies/lift/genuine/Lift_fixed1.tlsf -o=case-studies/lift/Lift.tlsf $INPUT_DIR/Lift/Lift-genuine-$K > $RESULT_DIR/Lift/Lift-genuine-$K/out.txt  

#echo "Running HumanoidLTL_458..."
#mkdir -p $RESULT_DIR/HumanoidLTL_458/HumanoidLTL_458-genuine-$K
#./compute-statistics.sh -ref=case-studies/HumanoidLTL_458/genuine/HumanoidLTL_460_Humanoid.tlsf -o=case-studies/HumanoidLTL_458/HumanoidLTL_458_Humanoid_fixed_unrealizable.tlsf $INPUT_DIR/HumanoidLTL_458/HumanoidLTL_458-genuine-$K  > $RESULT_DIR/HumanoidLTL_458/HumanoidLTL_458-genuine-$K/out.txt 

#echo "Running GyroUnrealizable_Var1..."
#mkdir -p $RESULT_DIR/GyroUnrealizable_Var1/GyroUnrealizable_Var1-genuine-$K/
#./compute-statistics.sh -ref=case-studies/GyroUnrealizable_Var1/genuine/GyroLTLVar3_702_GyroAspect.tlsf -o=case-studies/GyroUnrealizable_Var1/GyroUnrealizable_Var1_710_GyroAspect_unrealizable.tlsf $INPUT_DIR/GyroUnrealizable_Var1/GyroUnrealizable_Var1-genuine-$K  > $RESULT_DIR/GyroUnrealizable_Var1/GyroUnrealizable_Var1-genuine-$K/out.txt 

#echo "Running GyroUnrealizable_Var2..."
#mkdir -p $RESULT_DIR/GyroUnrealizable_Var2/GyroUnrealizable_Var2-genuine-$K/
#./compute-statistics.sh -ref=case-studies/GyroUnrealizable_Var2/genuine/GyroLTLVar3_702_GyroAspect.tlsf -o=case-studies/GyroUnrealizable_Var2/GyroUnrealizable_Var2_710_GyroAspect_unrealizable.tlsf $INPUT_DIR/GyroUnrealizable_Var2/GyroUnrealizable_Var2-genuine-$K  > $RESULT_DIR/GyroUnrealizable_Var2/GyroUnrealizable_Var2-genuine-$K/out.txt 

done

echo "Finished."
popd
