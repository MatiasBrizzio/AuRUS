#!/bin/bash -l

#SBATCH -n 1
#SBATCH -N 1
#SBATCH -c 4
#SBATCH --time=0-00:20:00
#SBATCH -J Unreal-Repair
#SBATCH --mail-user=renzo.degiovanni@uni.lu
#SBATCH --mail-type=all

module purge

export BASEDIR=/home/users/rdegiovanni/unreal-repair

export JAVA_HOME=/home/users/rdegiovani/envlib/java-11-oracle/
export ANT_HOME=/home/users/rdegiovani/envlib/ant/
export PATH=$PATH:$JAVA_HOME/bin:$ANT_HOME:bin

pushd ${BASEDIR}
CP="bin:lib/commons-math3-3.6.1.jar:lib/rltlconv.jar:lib/JFLAP-7.0_With_Source.jar:lib/owl-18.10-snapshot.jar:lib/ejml/ejml-core-0.34.jar:lib/ejml/ejml-cdense-0.34.jar:lib/ejml/ejml-ddense-0.34.jar:lib/ejml/ejml-fdense-0.34.jar:lib/ejml/ejml-simple-0.34.jar:lib/ejml/ejml-zdense-0.34.jar:lib/ejml/ejml-dsparse-0.34.jar:lib/ejml/ejml-experimental-0.34.jar:lib/ltl2buchi.jar"
java -Xmx8g -Djava.library.path=/usr/local/lib -cp $CP main.Main "$@"

popd

