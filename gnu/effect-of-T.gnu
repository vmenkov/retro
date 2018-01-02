set term x11 1
#set logscale x
set grid xtics ytics
set title "T=0 vs T=24"
set xlabel "Visibility with T=24"
set ylabel "Visibility with T=0"
set key right bottom 
set style data points

plot \
'totals-cmp.dat' using ($4/$3):($10/$9) title 'Cutoff=1' with points pt 5 lc rgb 'red', \
'../log-seri-c5/totals-cmp.dat' using ($4/$3):($10/$9) title 'Cutoff=5' with points pt 4 lc rgb 'blue',\
x with lines title 'x=y'

set term png large
set out 'effect-of-T.png'
replot