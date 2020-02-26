#!/bin/bash
#docker build --no-cache docker/.
#docker rmi -f $(docker images | grep "^<none>" | awk '{print $3}')
#SINGULARITYENV_STRIX_SPEC=$(pwd)/$1
#export SINGULARITYENV_STRIX_SPEC
#echo $SINGULARITYENV_STRIX_SPEC
cp $1 docker/Spec.tlsf
pushd docker
singularity exec strix_image_docker_runner.sif ./run-singularity-strix.sh Spec.tlsf
popd
