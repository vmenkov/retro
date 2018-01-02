# Column names:
# Series users allActions visActions allLists visLists

set xlabel "series"
set ylabel "Rec lists affected portion"

set term x11 1

plot \
'totals.dat' using ($1):($6/$5) title 'c=1' with lines, \
'../log-seri-c5/totals.dat' using ($1):($6/$5) title 'c=5'  with linespoints


set term x11 2

set xlabel "series"
set ylabel "Avg user's article count"


plot \
'../log-seri-c5/totals.dat' using ($1):($3/$2) title 'Article cnt' with linespoints
