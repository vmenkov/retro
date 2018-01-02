#!/bin/csh


perl -pe 's/seri-0*(\d+)k.*?.log:Totals for all (\d+) users: out of (\d+) actions, visible (\d+) \((\d+) rec lists out of (\d+)\).*/$1 $2 $3 $4 $6 $5/' 0hr.log > totals-0hr.dat

egrep '^[0-9]*0 ' totals.dat | head -11 > totals-section.dat
paste totals-section.dat totals-0hr.dat > totals-cmp.dat
