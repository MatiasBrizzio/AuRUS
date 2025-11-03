#!/bin/bash

# printf "%14s %14s %28s %14s %14s %14s %14s\n" "Time(s)" "Total #Sol" "Weaker than original #Sol" "Weaker ratio" "Genuine #Sol" "Weaker #Sol" "Stronger #Sol"

TotalSol=0
TotalWOrigSol=0
TotalWSol=0
TotalRatio=0
NRuns=0

FOLDERS=("$1"/*)
for folder in "${FOLDERS[@]}"; do
    file=$folder/out.txt
    # GAtime=$(grep '^Time:' "$file" | grep -o ....$)
    Sol=$(grep "Num. of Solutions: " "$file" | grep -o ....$)
    WOrigSol=$(grep "Num. of Solutions weaker than original:" "$file" | grep -o ...$ | sed 's/[^0-9]*//g')
    # GenSol=$(grep "Genuine Solutions:" "$file" | grep -o ....$)
    WSol=$(grep "Weaker Solutions:" "$file" | grep -o ....$)
    # SSol=$(grep "Stronger Solutions:" "$file" | grep -o ....$)
    Ratio=$(bc -l<<<"${WOrigSol}/${Sol}")
    # printf "%14s %14s %28s %14s %14s %14s %14s\n" "${GAtime}" "${Sol}" "${WOrigSol}" "${Ratio}" "${GenSol}" "${WSol}" "${SSol}"
    TotalSol=$((TotalSol + Sol))
    TotalWOrigSol=$((TotalWOrigSol + WOrigSol))
    TotalWSol=$((TotalWSol + WSol))
    TotalRatio=$(bc -l<<<"${TotalRatio} + ${Ratio}")
    NRuns=$((NRuns + 1))
done

AvgSol=$((TotalSol / NRuns))
AvgWOrigSol=$((TotalWOrigSol / NRuns))
AvgWSol=$((TotalWSol / NRuns))
NRunsFloat=$(bc -l<<<"${NRuns}")
AvgRatio=$(bc -l<<<"scale=2; ${TotalRatio}/${NRunsFloat}")

SolStandardDev=0
WOrigSolStandardDev=0
WSolStandardDev=0
RatioStandardDev=0
for folder in "${FOLDERS[@]}"; do
    file=$folder/out.txt
    Sol=$(grep "Num. of Solutions: " "$file" | grep -o ....$)
    Diff=$((Sol - AvgSol))
    SqDiff=$((Diff * Diff))
    SolStandardDev=$((SolStandardDev + SqDiff))
    WOrigSol=$(grep "Num. of Solutions weaker than original:" "$file" | grep -o ...$ | sed 's/[^0-9]*//g')
    Diff=$((WOrigSol - AvgWOrigSol))
    SqDiff=$((Diff * Diff))
    WOrigSolStandardDev=$((WOrigSolStandardDev + SqDiff))
    WSol=$(grep "Weaker Solutions:" "$file" | grep -o ....$)
    Diff=$((WSol - AvgWSol))
    SqDiff=$((Diff * Diff))
    WSolStandardDev=$((WSolStandardDev + SqDiff))
    Ratio=$(bc -l<<<"${WOrigSol}/${Sol}")
    Diff=$(bc -l<<<"${Ratio} - ${AvgRatio}")
    SqDiff=$(bc -l<<<"${Diff} * ${Diff}")
    RatioStandardDev=$(bc -l<<<"${RatioStandardDev} + ${SqDiff}")
done

SolStandardDev=$(bc -l<<<"scale=2; sqrt(${SolStandardDev}/${NRunsFloat})")
WOrigSolStandardDev=$(bc -l<<<"scale=2; sqrt(${WOrigSolStandardDev}/${NRunsFloat})")
WSolStandardDev=$(bc -l<<<"scale=2; sqrt(${WSolStandardDev}/${NRunsFloat})")
RatioStandardDev=$(bc -l<<<"scale=4; sqrt(${RatioStandardDev}/${NRunsFloat})")

printf "Average total #Sol: %s (StdDev: %s)\n" "${AvgSol}" "${SolStandardDev}"
printf "Average weaker than original #Sol: %s (StdDev: %s)\n" "${AvgWOrigSol}" "${WOrigSolStandardDev}"
printf "Average weaker than original ratio: %s (StdDev: %s)\n" "${AvgRatio}" "${RatioStandardDev}"
printf "Average weaker than genuine #Sol: %s (StdDev: %s)\n" "${AvgWSol}" "${WSolStandardDev}"

# pushd "$(mktemp -d)" > /dev/null || exit 1

# function cleanup {
#   popd > /dev/null || exit 1
# }
# trap cleanup EXIT

# TABLETXT="table.txt"
# TABLECSV="table.csv"

# printf "%18s%10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n" "" "" "Total" "" "" "Genuine" "" "" "Weaker" "" "" "Stronger" > "$TABLETXT"
# printf "%18s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n" \
#     "Run" "time(s)" "#Sol" "BestFit" "AvgFit" "#Sol" "BestFit" "AvgFit" "#Sol" "BestFit" "AvgFit" "#Sol" "BestFit" "AvgFit" >> "$TABLETXT"

# per_file() {
#     FOLDERS=("$1"/*)
#     printf "%-18s " "$(basename "$1")" >> "$TABLETXT"
#     mean_across_files() {
#         local pattern="$1"
#         local file
#         for folder in "${FOLDERS[@]}"; do
#             file="$folder/out.txt"
#             if [ -r "$file" ]; then
#                 grep -F "$pattern" "$file" 2>/dev/null |
#                     grep -oE '[+-]?([0-9]*\.)?[0-9]+([eE][+-]?[0-9]+)?'
#             fi
#         done | awk '
#                 BEGIN{sum=0;count=0}
#                 /^[+-]?([0-9]*\.)?[0-9]+([eE][+-]?[0-9]+)?$/ { sum += $1; count++ }
#                 END{ if(count==0) print "NaN"; else printf "%.2f", sum/count }
#             '
#     }
#     GAtime=$(mean_across_files "Time:")
#     Sol=$(mean_across_files "Num. of Solutions:")
#     BestFit=$(mean_across_files "Best fitness:")
#     AvgFit=$(mean_across_files "AVG fitness:")
#     printf "%10s %10s %10s %10s " "${GAtime}" "${Sol}" "${BestFit}" "${AvgFit}" >> "$TABLETXT"
#     GenSol=$(mean_across_files "Genuine Solutions:")
#     GenBestFit=$(mean_across_files "Best Genuine fitness:")
#     GenAvgFit=$(mean_across_files "AVG Genuine fitness:")
#     printf "%10s %10s %10s " "${GenSol}" "${GenBestFit}" "${GenAvgFit}" >> "$TABLETXT"
#     WSol=$(mean_across_files "Weaker Solutions:")
#     WBestFit=$(mean_across_files "Best Weaker fitness:")
#     WAvgFit=$(mean_across_files "AVG Weaker fitness:")
#     printf "%10s %10s %10s " "${WSol}" "${WBestFit}" "${WAvgFit}" >> "$TABLETXT"
#     SSol=$(mean_across_files "Stronger Solutions:")
#     SBestFit=$(mean_across_files "Best Stronger fitness:")
#     SAvgFit=$(mean_across_files "AVG Stronger fitness:")
#     printf "%10s %10s %10s\n" "${SSol}" "${SBestFit}" "${SAvgFit}" >> "$TABLETXT"
# }

# RUNS=(result/*)

# for run in "${RUNS[@]}"; do
#     per_file "$run"
# done

# # awk -v OFS=, '
# # NR==1 {
# #   # top header groups (Total, Genuine, ...)
# #   n1 = NF
# #   for(i=1;i<=NF;i++) H1[i]=$i
# #   next
# # }
# # NR==2 {
# #   # second header (Run time(s) #Sol BestFit ...)
# #   n2 = NF
# #   for(i=1;i<=NF;i++) H2[i]=$i
# #   hdr[1]=H2[1]; hdr[2]=H2[2]
# #   for(i=3;i<=n2;i++){
# #     gi = int((i-3)/3) + 1
# #     grp = (gi <= n1 ? H1[gi] : ("G"gi))
# #     g = grp; s = H2[i]
# #     gsub(/#/,"Num", g); gsub(/[()\/ ]/,"", g)
# #     gsub(/#/,"Num", s); gsub(/[()\/ ]/,"", s)
# #     hdr[i] = g "_" s
# #   }
# #   for(i=1;i<=n2;i++) printf "%s%s", hdr[i], (i<n2?OFS:ORS)
# #   next
# # }
# # {
# #   # data rows: default FS (whitespace) is good — prints fields as CSV
# #   for(i=1;i<=NF;i++) printf "%s%s", $i, (i<NF?OFS:ORS)
# # }
# # ' "$TABLETXT" > "$TABLECSV"

# cat "$TABLETXT"
