#!/bin/csh

# time java -cp ../lib/retro.jar edu.rutgers.retro.UserStats users /data/json/usage/2017/170902_usage.json.gz
#time java -cp ../lib/retro.jar:../lib/javax.json-1.1.jar ...

time java  -cp ../lib/retro.jar -Dout=out \
 edu.rutgers.retro.UserStats readActions
