#!/bin/csh

#-- This script extract the "Totals" line from each individual log file,
#-- puts these lines together, and convert the resulting data to a 
#-- space-separated file format for Gnuplot

grep Totals seri-*k*.log > totals.tmp

# seri-005k.log:Totals for all 69 users: out of 66831 actions, visible 57557 (143456 rec lists out of 108883365)

echo '# Series users allActions visActions allLists visLists' > totals.dat
perl -pe 's/seri-0*(\d+)k.*?.log:Totals for all (\d+) users: out of (\d+) actions, visible (\d+) \((\d+) rec lists out of (\d+)\).*/$1 $2 $3 $4 $6 $5/' totals.tmp >> totals.dat

~/bin/sum-rows.pl totals.dat > sum-totals.dat
