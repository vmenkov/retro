set term x11 1
set logscale x

set mytics
set yrange [0:1]
set grid xtics mytics
set xlabel "User's action count"
set ylabel "User's action visibility risk"
set key right bottom 
set style data points

set parametric
set title "Visibility rate vs action count (T=0, n=10, score cutoff=1)"
plot  [1:100]  \
2*t, 1/(2*t) lc 2,  3*t, 2/(3*t) lc 2, 4*t, 3/(4*t) lc 2, \
5*t, 4/(5*t) lc 2, 6*t, 5/(6*t) lc 2, 7*t, 6/(7*t) lc 2, \
8*t, 7/(8*t) lc 2, 9*t, 8/(9*t) lc 2, \
2*t, (t-1)/(2*t) lc 4, 6*t, (3*t-2)/(6*t) lc 4, \
 2*t, 1-1/(2*t) lc 3, 3*t, 1-2/(3*t) lc 3,  4*t, 1-3/(4*t) lc 3, \
5*t, 1-4/(5*t) lc 3,  6*t, 1-5/(6*t) lc 3, 7*t, 1-6/(7*t) lc 3, \
 8*t, 1-7/(8*t) lc 3, 9*t, 1-8/(9*t) lc 3, \
'../log-seri-c1-0hr/sample-all.dat' using ($3):($4/$3) title 'All users' with points pt 19 lc rgb 'red'


set term png giant
set out "visibility-c1-0hr-all-with-circles.png"
replot
set out

#set title "Visibility rate vs action count (T=0, n=10, score cutoff=5)"
#set term x11 2
#plot \
#'../log-seri-c5-0hr/sample-all.dat' using ($3):($4/$3) title 'All users' with points pt 19 lc rgb 'red'

#set term png large
#set out "visibility-c5-0hr-all-with-circles.png"
#replot
#set out
