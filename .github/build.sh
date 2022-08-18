#!/bin/sh

# Required for mounting m2 into docker
mkdir -p $HOME/.m2
chmod a+rw $HOME/.m2

curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/ci-build.sh
sh ci-build.sh
