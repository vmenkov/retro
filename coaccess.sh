#!/bin/csh

time java  -cp ../lib/retro.jar:../lib/commons-lang-2.4.jar \
 -Dinc=true -Dcompact=true -Dindex=out-index -Dstep=24 -Dprofile=false -Dstructure=true -Dcutoff=1 \
 edu.rutgers.retro.Coaccess uname \
f375259a9068 0b2a849197ac e19772187909 dac0bf416e4c b4a8892d4f8b

