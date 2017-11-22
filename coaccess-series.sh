#!/bin/csh

time java  -cp ../lib/retro.jar:../lib/commons-lang-2.4.jar:../lib/commons-csv-1.1.jar \
-Dinc=true -Dcompact=true -Dindex=out-index -Dstep=24 -Dprofile=false -Dstructure=true -Dcutoff=1 \
 edu.rutgers.retro.Coaccess urange 0 100 10


