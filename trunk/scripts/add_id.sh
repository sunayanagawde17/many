#!/bin/bash

#Copyright 2009 Loic BARRAULT.  
#See the file "LICENSE.txt" for information on usage and
#redistribution of this file, and for a DISCLAIMER OF ALL 
#WARRANTIES.

i=0;

#([set][doc][seg])

while read l
do
  echo "$l ([set][doc.00][$i])"  >> $2 
  i=`expr $i + 1`
done < $1

