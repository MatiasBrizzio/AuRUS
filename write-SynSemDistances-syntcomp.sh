# echo "RANDOM...."
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/detector/detector_unreal_2_basic.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/detector/detector-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/full_arbiter/full_arbiter_unreal1_3_2_basic.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/full_arbiter/full_arbiter-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/lily02/lilydemo02.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/lily02/lily02-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/lily11/lilydemo11.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/lily11/lily11-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/lily15/lilydemo15.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/lily15/lily15-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/lily16/lilydemo16.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/lily16/lily16-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/load_balancer/load_balancer_unreal1_2_2.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/load_balancer/load_balancer-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/ltl2dba_R_2/ltl2dba_R_2.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/ltl2dba_R_2/ltl2dba_R_2-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/ltl2dba_theta_2/ltl2dba_theta_2.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/ltl2dba_theta_2/ltl2dba_theta_2-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/ltl2dba27/ltl2dba27.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/ltl2dba27/ltl2dba27-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/prioritized_arbiter/prioritized_arbiter_unreal1_3_2.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/prioritized_arbiter/prioritized_arbiter-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/round_robin/round_robin_arbiter_unreal1_2_3.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/round_robin/round_robin-genuine
./compute-SynSemDistances.sh case-studies/syntcomp-unreal/simple_arbiter/simple_arbiter_unreal2_2.tlsf results-syntcomp/result-syntcomp-random/syntcomp-unreal/simple_arbiter/simple_arbiter-genuine


echo "70-10-20...."
sbatch --job-name=detector --output=statistics/detector.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/detector/detector_unreal_2_basic.tlsf result-syntcomp-70-10-20/syntcomp-unreal/detector/detector-genuine
sbatch --job-name=full_arbiter --output=statistics/full_arbiter.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/full_arbiter/full_arbiter_unreal1_3_2_basic.tlsf result-syntcomp-70-10-20/syntcomp-unreal/full_arbiter/full_arbiter-genuine
sbatch --job-name=lily02 --output=statistics/lily02.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/lily02/lilydemo02.tlsf result-syntcomp-70-10-20/syntcomp-unreal/lily02/lily02-genuine
sbatch --job-name=lily11 --output=statistics/lily11.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/lily11/lilydemo11.tlsf result-syntcomp-70-10-20/syntcomp-unreal/lily11/lily11-genuine
sbatch --job-name=lily15 --output=statistics/lily15.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/lily15/lilydemo15.tlsf result-syntcomp-70-10-20/syntcomp-unreal/lily15/lily15-genuine
sbatch --job-name=lily16 --output=statistics/lily16.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/lily16/lilydemo16.tlsf result-syntcomp-70-10-20/syntcomp-unreal/lily16/lily16-genuine
sbatch --job-name=load_balancer --output=statistics/load_balancer.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/load_balancer/load_balancer_unreal1_2_2.tlsf result-syntcomp-70-10-20/syntcomp-unreal/load_balancer/load_balancer-genuine
sbatch --job-name=ltl2dba_R_2 --output=statistics/ltl2dba_R_2.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/ltl2dba_R_2/ltl2dba_R_2.tlsf result-syntcomp-70-10-20/syntcomp-unreal/ltl2dba_R_2/ltl2dba_R_2-genuine
sbatch --job-name=ltl2dba_theta_2 --output=statistics/ltl2dba_theta_2.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/ltl2dba_theta_2/ltl2dba_theta_2.tlsf result-syntcomp-70-10-20/syntcomp-unreal/ltl2dba_theta_2/ltl2dba_theta_2-genuine
sbatch --job-name=ltl2dba27 --output=statistics/ltl2dba27.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/ltl2dba27/ltl2dba27.tlsf result-syntcomp-70-10-20/syntcomp-unreal/ltl2dba27/ltl2dba27-genuine
sbatch --job-name=prioritized_arbiter --output=statistics/prioritized_arbiter.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/prioritized_arbiter/prioritized_arbiter_unreal1_3_2.tlsf result-syntcomp-70-10-20/syntcomp-unreal/prioritized_arbiter/prioritized_arbiter-genuine
sbatch --job-name=v --output=statistics/round_robin.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/round_robin/round_robin_arbiter_unreal1_2_3.tlsf result-syntcomp-70-10-20/syntcomp-unreal/round_robin/round_robin-genuine
sbatch --job-name=simple_arbiter --output=statistics/simple_arbiter.out ./compute-SynSemDistances.sh case-studies/syntcomp-unreal/simple_arbiter/simple_arbiter_unreal2_2.tlsf result-syntcomp-70-10-20/syntcomp-unreal/simple_arbiter/simple_arbiter-genuine

