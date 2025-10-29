#!/bin/bash

printf "%14s %14s %14s %14s %14s\n" "Time(s)" "Total #Sol" "Genuine #Sol" "Weaker #Sol" "Stronger #Sol"

FOLDERS=("$1"/*)
for folder in "${FOLDERS[@]}"; do
    file=$folder/out.txt
    GAtime=$(grep '^Time:' "$file" | grep -o ....$)
    Sol=$(grep "Num. of Solutions: " "$file" | grep -o ....$)
    printf "%14s %14s " "${GAtime}" "${Sol}"
    GenSol=$(grep "Genuine Solutions:" "$file" | grep -o ....$)
    printf "%14s " "${GenSol}"
    WSol=$(grep "Weaker Solutions:" "$file" | grep -o ....$)
    printf "%14s " "${WSol}"
    SSol=$(grep "Stronger Solutions:" "$file" | grep -o ....$)
    printf "%14s\n" "${SSol}"
done
