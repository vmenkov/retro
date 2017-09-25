#!/bin/csh

# time java -cp ../lib/retro.jar edu.rutgers.retro.UserStats users /data/json/usage/2017/170902_usage.json.gz
#time java -cp ../lib/retro.jar:../lib/javax.json-1.1.jar ...

time java  -cp ../lib/retro.jar \
 -Danon=false \
 edu.rutgers.retro.UserStats users /data/json/usage/
