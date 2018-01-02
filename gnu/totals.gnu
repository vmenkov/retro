# Gnuplot presentation of varios "totals" for the retroactive 
# experiments with the model coaccess recomennder
#-------------------------------------------------
# Column names:
# Series users allActions visActions allLists visLists

set nologscale x
set key right top
set grid xtics ytics

set xlabel "Subsample"
set ylabel "Visibility risk"

set term x11 1
#-- set termoption dash #-- does not work; use thickness instead

#-- step between subsamples
q=5

plot \
'../log-seri-c1/totals.dat' using ($1/q):($4/$3) title 'T=24 hr, cutoff=1' with linespoints lw 1 lc 1, \
'../log-seri-c5/totals.dat' using ($1/q):($4/$3) title 'T=24 hr, cutoff=5' with linespoints lw 1 lc 2, \
'../log-seri-c1-0hr/totals.dat' using ($1/q):($4/$3) title 'T=0, cutoff=1' with linespoints lw 3 lc 1, \
 '../log-seri-c5-0hr/totals.dat' using ($1/q):($4/$3) title 'T=0, cutoff=5' with linespoints lw 3 lc 2 

set term png
set termoption dash
set out "totals.png"
replot
set out

set term x11 2
set xlabel "Subsample"
set ylabel "Avg user's article count"


plot \
'../log-seri-c5/totals.dat' using ($1/q):($3/$2) title 'Article cnt' with linespoints lc 1

set xlabel "Subsample"
set ylabel "Affected portion of rec lists"

set term png
set termoption dash
set out "count.png"
replot
set out

set term x11 3


plot \
'../log-seri-c1/totals.dat' using ($1/q):($6/$5) title 'T=24 hr, cutoff=1' with linespoints lw 1 lc 1, \
'../log-seri-c5/totals.dat' using ($1/q):($6/$5) title 'T=24 hr, cutoff=5' with linespoints lw 1 lc 2, \
'../log-seri-c1-0hr/totals.dat' using ($1/q):($6/$5) title 'T=0, cutoff=1' with linespoints lw 3 lc 1, \
 '../log-seri-c5-0hr/totals.dat' using ($1/q):($6/$5) title 'T=0, cutoff=5' with linespoints lw 3 lc 2 

set term png
set termoption dash
set out "lists.png"
replot
set out
