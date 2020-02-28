#!/bin/bash

#echo -e "\t\t\t\t\tGenuine\t\tWeaker\t\tStronger\t"
#echo -e "time(s)  \t#Sol.  Best Fit.  AVG Fit.  #Sol Found Best Fit. AVG Fit. #Sol Found Best Fit. AVG Fit. #Sol Found Best Fit. AVG Fit."

for K in {2..9}
do
DIR=$1-$K
file=$DIR/out.txt

GAtime=$(grep "GA Time:" $file | grep -o ..........$)
Sol=$(ls -1q $DIR/spec* | wc -l)
BestFit=$(grep "Best fitness:" $file | grep -o ....$)
AvgFit=$(grep "AVG fitness:" $file | grep -o ....$)

GenSol=$(grep "Genuine Solutions found" $file | awk -F"," '{print NF-1}')
GenBestFit=$(grep "Best Genuine fitness:" $file | grep -o ....$)
GenAvgFit=$(grep "AVG Genuine fitness:" $file | grep -o ....$)

WSol=$(grep "Weaker Solutions found" $file | awk -F"," '{print NF-1}')
WBestFit=$(grep "Best Weaker fitness:" $file | grep -o ....$)
WAvgFit=$(grep "AVG Weaker fitness:" $file | grep -o ....$)

SSol=$(grep "Stronger Solutions found" $file | awk -F"," '{print NF-1}')
SBestFit=$(grep "Best Stronger fitness:" $file | grep -o ....$)
SAvgFit=$(grep "AVG Stronger fitness:" $file | grep -o ....$)

echo -e "${GAtime}\t${Sol}\t${BestFit}\t${AvgFit}\t${GenSol}\t${GenBestFit}\t${GenAvgFit}\t${WSol}\t${WBestFit}\t${WAvgFit}\t${SSol}\t${SBestFit}\t${SAvgFit}"

done
