#!/bin/csh

set x=120


while ($x < 200) 

@ y = ($x + 1)

echo  urange  ${x}000 ${y}000 10 

time java  -cp ../lib/retro.jar:../lib/commons-lang-2.4.jar:../lib/commons-csv-1.1.jar \
-Dinc=true -Dcompact=true -Dindex=out-index -Dstep=24 -Dprofile=false -Dstructure=true -Dcutoff=1 \
 edu.rutgers.retro.Coaccess urange  ${x}000 ${y}000 10  >&  seri-${x}k.log

@ x = ($x + 20)
 end

#  191906  2051647 31286765 out-index/users.csv








