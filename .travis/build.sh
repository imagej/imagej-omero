#!/bin/sh

# Required for mounting m2 into docker
mkdir -p $HOME/.m2
chmod a+rw $HOME/.m2

curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh

git clone git://github.com/awalter17/omero-test-infra .omero
# FIXME: setting DOCKER_ARGS here should work but it's not.
# It fails with:
# [ERROR] Could not create local repository at /home/mvn/.m2/repository -> [Help 1]
# env DOCKER_ARGS="-v $HOME/.m2:/home/mvn/.m2"
.omero/lib-docker
