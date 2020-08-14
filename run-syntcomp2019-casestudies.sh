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
#for K in {9..9}
#do
echo "Running SYNTCOMP case studies..."

echo "Running detector..."
./unreal-repair.sh $CONFIG -ref=case-studies/syntcomp-unreal/detector/genuine/detector_2.tlsf -out=$RESULT_DIR/syntcomp-unreal/detector/detector-genuine-$K case-studies/syntcomp-unreal/detector/detector_unreal_2.tlsf > $RESULT_DIR/syntcomp-unreal/detector/detector-genuine-$K.out 

echo "Running full_arbiter..."
./unreal-repair.sh $CONFIG -ref=case-studies/syntcomp-unreal/full_arbiter/genuine/full_arbiter_3.tlsf -out=$RESULT_DIR/syntcomp-unreal/full_arbiter/full_arbiter-genuine-$K case-studies/syntcomp-unreal/full_arbiter/full_arbiter_unreal1_3_2.tlsf > $RESULT_DIR/syntcomp-unreal/full_arbiter/full_arbiter-genuine-$K.out 

echo "Running Lily02..."
./unreal-repair.sh $CONFIG -ref=case-studies/syntcomp-unreal/lily02/genuine/lilydemo03.tlsf -ref=case-studies/syntcomp-unreal/lily02/genuine/lilydemo04.tlsf -ref=case-studies/syntcomp-unreal/lily02/genuine/lilydemo05.tlsf -ref=case-studies/syntcomp-unreal/lily02/genuine/lilydemo06.tlsf -ref=case-studies/syntcomp-unreal/lily02/genuine/lilydemo07.tlsf -out=$RESULT_DIR/syntcomp-unreal/lily02/lily02-genuine-$K case-studies/syntcomp-unreal/lily02/lilydemo02.tlsf > $RESULT_DIR/syntcomp-unreal/lily02/lily02-genuine-$K.out 

echo "Running Lily11..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/syntcomp-unreal/lily11/lily11-genuine-$K case-studies/syntcomp-unreal/lily11/lilydemo11.tlsf > $RESULT_DIR/syntcomp-unreal/lily11/lily11-genuine-$K.out 

echo "Running Lily15..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/syntcomp-unreal/lily15/lily15-genuine-$K case-studies/syntcomp-unreal/lily15/lilydemo15.tlsf > $RESULT_DIR/syntcomp-unreal/lily15/lily15-genuine-$K.out 

echo "Running Lily16..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/syntcomp-unreal/lily16/lily16-genuine-$K case-studies/syntcomp-unreal/lily16/lilydemo16.tlsf > $RESULT_DIR/syntcomp-unreal/lily16/lily16-genuine-$K.out 

echo "Running load_balancer..."
./unreal-repair.sh $CONFIG -ref=case-studies/syntcomp-unreal/load_balancer/genuine/load_balancer_2.tlsf -out=$RESULT_DIR/syntcomp-unreal/load_balancer/load_balancer-genuine-$K case-studies/syntcomp-unreal/load_balancer/load_balancer_unreal1_2_2.tlsf > $RESULT_DIR/syntcomp-unreal/load_balancer/load_balancer-genuine-$K.out 

echo "Running ltl2dba_R_2..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/syntcomp-unreal/ltl2dba_R_2/ltl2dba_R_2-genuine-$K case-studies/syntcomp-unreal/ltl2dba_R_2/ltl2dba_R_2.tlsf > $RESULT_DIR/syntcomp-unreal/ltl2dba_R_2/ltl2dba_R_2-genuine-$K.out 

echo "Running ltl2dba_theta_2..."
./unreal-repair.sh $CONFIG -out=$RESULT_DIR/syntcomp-unreal/ltl2dba_theta_2/ltl2dba_theta_2-genuine-$K case-studies/syntcomp-unreal/ltl2dba_theta_2/ltl2dba_theta_2.tlsf > $RESULT_DIR/syntcomp-unreal/ltl2dba_theta_2/ltl2dba_theta_2-genuine-$K.out 

echo "Running ltl2dba27..."
./unreal-repair.sh $CONFIG -ref=case-studies/syntcomp-unreal/ltl2dba27/genuine/ltl2dba22.tlsf -ref=case-studies/syntcomp-unreal/ltl2dba27/genuine/ltl2dba24.tlsf -out=$RESULT_DIR/syntcomp-unreal/ltl2dba27/ltl2dba27-genuine-$K case-studies/syntcomp-unreal/ltl2dba27/ltl2dba27.tlsf > $RESULT_DIR/syntcomp-unreal/ltl2dba27/ltl2dba27-genuine-$K.out 

echo "Running prioritized_arbiter..."
./unreal-repair.sh $CONFIG -ref=case-studies/syntcomp-unreal/prioritized_arbiter/genuine/prioritized_arbiter_3.tlsf -out=$RESULT_DIR/syntcomp-unreal/prioritized_arbiter/prioritized_arbiter-genuine-$K case-studies/syntcomp-unreal/prioritized_arbiter/prioritized_arbiter_unreal1_3_2.tlsf > $RESULT_DIR/syntcomp-unreal/prioritized_arbiter/prioritized_arbiter-genuine-$K.out 

echo "Running round_robin..."
./unreal-repair.sh $CONFIG -ref=case-studies/syntcomp-unreal/round_robin/genuine/round_robin_arbiter_2.tlsf -out=$RESULT_DIR/syntcomp-unreal/round_robin/round_robin-genuine-$K case-studies/syntcomp-unreal/round_robin/round_robin_arbiter_unreal1_2_3.tlsf > $RESULT_DIR/syntcomp-unreal/round_robin/round_robin-genuine-$K.out 

echo "Running simple_arbiter..."
./unreal-repair.sh $CONFIG -ref=case-studies/syntcomp-unreal/simple_arbiter/genuine/simple_arbiter_2.tlsf -out=$RESULT_DIR/syntcomp-unreal/simple_arbiter/simple_arbiter-genuine-$K case-studies/syntcomp-unreal/simple_arbiter/simple_arbiter_unreal2_2.tlsf > $RESULT_DIR/syntcomp-unreal/simple_arbiter/simple_arbiter-genuine-$K.out 


echo "Finished."
popd