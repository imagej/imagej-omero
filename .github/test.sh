#!/bin/sh

# Note: script has been extracted from build.sh to simplify local testing

set -e
set -x

export BACKUPURL=${BACKUPURL:-https://downloads.imagej.net/test/omero/omero_test_infra_backup.zip}
export INFRABRANCH=${INFRABRANCH:-v0.6.3}
export INFRAREPO=${INFRAREPO:-git://github.com/openmicroscopy/omero-test-infra}

if [ ! -d .omero ];
then
    git clone -b ${INFRABRANCH} ${INFRAREPO} .omero
fi

.omero/download.sh ${BACKUPURL} $PWD/.github
.omero/persist.sh --restore $PWD/.github

# FIXME: setting DOCKER_ARGS here should work but it's not.
# It fails with:
# [ERROR] Could not create local repository at /home/mvn/.m2/repository -> [Help 1]
# env DOCKER_ARGS="-v $HOME/.m2:/home/mvn/.m2"

export OMERO_SERVER_VERSION=5.6
export OMERO_WEB_VERSION=5.6
export POSTGRES_VERSION=9.6
export COMPOSE_FILE="docker-compose.yml:volumes.yml"

.omero/docker lib

# Optionally persist for a next run
