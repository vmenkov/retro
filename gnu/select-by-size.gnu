set term x11 1
set nologscale x

set mytics
set yrange [0:1]
set grid xtics mytics
set xlabel "User's joining order"
set ylabel "User's action visibility rate"
set key right bottom 
set style data points

set title "Visibility rate vs time of joining (T=0, n=10, score cutoff=1)"
plot \
'../log-seri-c1-0hr/size-sample-200.dat' using ($1):($4/$3) title 'Users with 200-299 articles' with points pt 5 lc rgb 'red',\
'../log-seri-c1-0hr/size-sample-500.dat' using ($1):($4/$3) title 'Users with 500-599 articles' with points pt 7 lc rgb 'cyan',\
'../log-seri-c1-0hr/size-sample-1000.dat' using ($1):($4/$3) title 'Users with 1000-1099 articles' with points pt 19 lc rgb 'blue'


set term png large
set out "visibility-c1-0hr-by-size.png"
replot
set out

set term x11 2



set title "Visibility rate vs time of joining (T=0, n=10, score cutoff=5)"
plot \
'../log-seri-c5-0hr/size-sample-200.dat' using ($1):($4/$3) title 'Users with 200-299 articles' with points pt 5 lc rgb 'red',\
'../log-seri-c5-0hr/size-sample-500.dat' using ($1):($4/$3) title 'Users with 500-599 articles' with points pt 7 lc rgb 'cyan',\
'../log-seri-c5-0hr/size-sample-1000.dat' using ($1):($4/$3) title 'Users with 1000-1099 articles' with points pt 19 lc rgb 'blue'

set term png large
set out "visibility-c5-0hr-by-size.png"
replot
set out
