#!/bin/csh

# For user 120990 (10e9c0c0d11b), out of 23, visible 13 (27 rec lists out of 276)

set x=$1

grep ', out of' $x | perl -pe 's/For user (\d+) \(([0-9a-f]+)\), out of (\d+), visible (\d+) \((\d+) rec lists out of (\d+)\).*/$1 $2 $3 $4 $6 $5/'
