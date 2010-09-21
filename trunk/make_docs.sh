pushdir $(dirname $0) &> /dev/null

echo help all | ./codecleaver.sh > docs/codecleaver_reference.txt

popdir &> /dev/null
