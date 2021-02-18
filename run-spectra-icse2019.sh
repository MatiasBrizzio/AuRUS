#!/bin/bash

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

module load swenv/default-env/devel
module load tools/Singularity/3.5.2

pushd ${BASEDIR}
echo $JAVA_HOME
echo $PATH

echo $(java -version)
echo $(javac -version)

ant compile

K=$1
RESULT_DIR=$2
CONFIG='-Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -geneNUM=0 -factors=0.7,0.1,0.2'

echo "Running SPECTRA case studies..."

echo "Running HumanoidLTL_503_Humanoid_fixed_unrealizable.tlsf..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/SYNTECH15/HumanoidLTL_503_Humanoid_fixed_unrealizable/HumanoidLTL_503_Humanoid_fixed_unrealizable-genuine-$K examples/icse2019/SYNTECH15/tlsf_specs/HumanoidLTL_503_Humanoid_fixed_unrealizable.tlsf > $RESULT_DIR/SYNTECH15/HumanoidLTL_503_Humanoid_fixed_unrealizable/HumanoidLTL_503_Humanoid_fixed_unrealizable-genuine-$K.out 

echo "Running HumanoidLTL_531_Humanoid_unrealizable.tlsf..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/SYNTECH15/HumanoidLTL_531_Humanoid_unrealizable/HumanoidLTL_531_Humanoid_unrealizable-genuine-$K examples/icse2019/SYNTECH15/tlsf_specs/HumanoidLTL_531_Humanoid_unrealizable.tlsf > $RESULT_DIR/SYNTECH15/HumanoidLTL_531_Humanoid_unrealizable/HumanoidLTL_531_Humanoid_unrealizable-genuine-$K.out 

echo "Running HumanoidLTL_741_Humanoid_unrealizable.tlsf..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/SYNTECH15/HumanoidLTL_741_Humanoid_unrealizable/HumanoidLTL_741_Humanoid_unrealizable-genuine-$K examples/icse2019/SYNTECH15/tlsf_specs/HumanoidLTL_741_Humanoid_unrealizable.tlsf > $RESULT_DIR/SYNTECH15/HumanoidLTL_741_Humanoid_unrealizable/HumanoidLTL_741_Humanoid_unrealizable-genuine-$K.out 


echo "Running HumanoidLTL_742_Humanoid_unrealizable.tlsf..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/SYNTECH15/HumanoidLTL_742_Humanoid_unrealizable/HumanoidLTL_742_Humanoid_unrealizable-genuine-$K examples/icse2019/SYNTECH15/tlsf_specs/HumanoidLTL_742_Humanoid_unrealizable.tlsf > $RESULT_DIR/SYNTECH15/HumanoidLTL_742_Humanoid_unrealizable/HumanoidLTL_742_Humanoid_unrealizable-genuine-$K.out 

echo "Running PcarLTL_769_PCar_fixed_unrealizable.tlsf..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/SYNTECH15/PcarLTL_769_PCar_fixed_unrealizable/PcarLTL_769_PCar_fixed_unrealizable-genuine-$K examples/icse2019/SYNTECH15/tlsf_specs/PcarLTL_769_PCar_fixed_unrealizable.tlsf > $RESULT_DIR/SYNTECH15/PcarLTL_769_PCar_fixed_unrealizable/PcarLTL_769_PCar_fixed_unrealizable-genuine-$K.out 

echo "Running PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable.tlsf..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/SYNTECH15/PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable/PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable-genuine-$K examples/icse2019/SYNTECH15/tlsf_specs/PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable.tlsf > $RESULT_DIR/SYNTECH15/PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable/PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable-genuine-$K.out 

echo "Running PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable.tlsf..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/SYNTECH15/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable-genuine-$K examples/icse2019/SYNTECH15/tlsf_specs/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable.tlsf > $RESULT_DIR/SYNTECH15/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable-genuine-$K.out 




