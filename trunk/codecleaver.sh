#!/bin/bash

set -e

# Runs codecleaver from compiled java class files
#
# $1 - root directory where codecleaver class files were built

if [ -z $1 ]; then
  DESTINATION_DIR=$(dirname $0)/out
else
  DESTINATION_DIR=$1
fi

# Run codecleaver
java -Xmx1024M -jar $DESTINATION_DIR/codecleaver.jar "$@"

