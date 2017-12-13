#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh

git clone git://github.com/openmicroscopy/omero-test-infra .omero
env DOCKER_ARGS="-v $HOME/.m2:/home/mvn/.m2" .omero/lib-docker
