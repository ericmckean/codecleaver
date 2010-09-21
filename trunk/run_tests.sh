#!/bin/bash

set -e

# Runs codecleaver tests

pushd $(dirname $0) > /dev/null

./codecleaver.sh < test/test.codecleaver > /dev/null

if diff test/expected-results.txt out/actual-results.txt > out/results.diff ; then
  echo Tests Passed.
else
  echo Tests Failed. See out/results.diff for details.
fi

popd > /dev/null

