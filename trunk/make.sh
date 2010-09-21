#!/bin/bash

set -e

# Build codecleaver
# $1 - output directory

if [ -z $1 ]; then
  DESTINATION_DIR='out'
else
  DESTINATION_DIR=$1
fi
CLASSES_DIR=$DESTINATION_DIR/classes

pushd $(dirname $0) > /dev/null

echo Building CodeCleaver ...
mkdir -p $CLASSES_DIR
javac -target 6 -cp lib/asm-3.2/lib/asm-3.2.jar:lib/guava-r06/guava-r06.jar -sourcepath src -d $CLASSES_DIR -implicit:class src/codecleaver/Program.java

jar cfm $DESTINATION_DIR/codecleaver.jar Manifest.txt -C $CLASSES_DIR .

cp lib/asm-3.2/lib/asm-3.2.jar $DESTINATION_DIR
cp lib/guava-r06/guava-r06.jar $DESTINATION_DIR

popd > /dev/null

