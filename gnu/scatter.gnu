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
'../log-seri-c1-0hr/sample-000k-0hr.dat' using ($3):($4/$3) title 'Early users (subsample 0)' with points pt 5 lc rgb 'red',\
'../log-seri-c1-0hr/sample-095k-0hr.dat' using ($3):($4/$3) title 'Middle users (subsample 19)' with points pt 7 lc rgb 'cyan',\
'../log-seri-c1-0hr/sample-190k-0hr.dat' using ($3):($4/$3) title 'Late users (subsample 38)' with points pt 19 lc rgb 'blue'



set term png large
set out "visibility-c1-0hr.png"
replot
set out

set title "Visibility rate vs action count (T=0, n=10, score cutoff=5)"
set term x11 2
plot \
'../log-seri-c5-0hr/sample-000k-0hr.dat' using ($3):($4/$3) title 'Early users (subsample 0)' with points pt 5 lc rgb 'red',\
'../log-seri-c5-0hr/sample-095k-0hr.dat' using ($3):($4/$3) title 'Middle users (subsample 19)' with points pt 7 lc rgb 'cyan',\
'../log-seri-c5-0hr/sample-190k-0hr.dat' using ($3):($4/$3) title 'Late users (subsample 38)' with points pt 19 lc rgb 'blue'

set term png large
set out "visibility-c5-0hr.png"
replot
set out
