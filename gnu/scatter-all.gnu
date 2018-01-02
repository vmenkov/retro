set term x11 1
set logscale x

set mytics
set yrange [0:1]
set grid xtics mytics
set xlabel "User's action count"
set ylabel "User's action visibility rate"
set key right bottom 
set style data points

set title "Visibility rate vs action count (T=0, n=10, score cutoff=1)"
plot \
'../log-seri-c1-0hr/sample-all.dat' using ($3):($4/$3) title 'All users' with points pt 19 lc rgb 'red'


set term png large
set out "visibility-c1-0hr-all.png"
replot
set out

set title "Visibility rate vs action count (T=0, n=10, score cutoff=5)"
set term x11 2
plot \
'../log-seri-c5-0hr/sample-all.dat' using ($3):($4/$3) title 'All users' with points pt 19 lc rgb 'red'

set term png large
set out "visibility-c5-0hr-all.png"
replot
set out
