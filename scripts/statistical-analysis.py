import sys
import chart_studio.plotly as py
import plotly.graph_objs as go
import plotly.figure_factory as FF
import plotly.express as px

import numpy as np
import pandas as pd
import scipy

import itertools as it

from bisect import bisect_left
from typing import List

import numpy as np
import pandas as pd
import scipy.stats as ss
from pandas import Categorical


def VD_A(treatment: List[float], control: List[float]):
    """
    Computes Vargha and Delaney A index
    A. Vargha and H. D. Delaney.
    A critique and improvement of the CL common language
    effect size statistics of McGraw and Wong.
    Journal of Educational and Behavioral Statistics, 25(2):101-132, 2000

    The formula to compute A has been transformed to minimize accuracy errors
    See: http://mtorchiano.wordpress.com/2014/05/19/effect-size-of-r-precision/

    :param treatment: a numeric list
    :param control: another numeric list

    :returns the value estimate and the magnitude
    """
    m = len(treatment)
    n = len(control)

        # if m != n:
        #     raise ValueError("Data d and f must have the same length")

    r = ss.rankdata(treatment + control)
    r1 = sum(r[0:m])

    # Compute the measure
    # A = (r1/m - (m+1)/2)/n # formula (14) in Vargha and Delaney, 2000
    A = (2 * r1 - m * (m + 1)) / (2 * n * m)  # equivalent formula to avoid accuracy errors

    levels = [0.147, 0.33, 0.474]  # effect sizes from Hess and Kromrey, 2004
    magnitude = ["negligible", "small", "medium", "large"]
    scaled_A = (A - 0.5) * 2

    magnitude = magnitude[bisect_left(levels, abs(scaled_A))]
    estimate = A

    return estimate, magnitude

def compare_medians():

    ifile = "executions-summary-sem1-removed.csv"

    data = pd.read_csv(ifile)
    # data = data.dropna()
    print(data)
    # CASE_STUDIES_LIT=["arbiter","minepump","RG1","RG2","Lift","HumanoidLTL_458","GyroUnrealizable_Var1","GyroUnrealizable_Var2"]
    # CASE_STUDIES_SYNTCOMP=["detector","full_arbiter","lily02","lily11","lily15","lily16","load_balancer","ltl2dba_R_2","ltl2dba_theta_2","ltl2dba27","prioritized_arbiter","round_robin","simple_arbiter"]
    # CASE_STUDIES_SYNTECH15=["HumanoidLTL_503_Humanoid_fixed_unrealizable","HumanoidLTL_531_Humanoid_unrealizable","HumanoidLTL_741_Humanoid_unrealizable","HumanoidLTL_742_Humanoid_unrealizable","PcarLTL_769_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable"]
    # ALL_CASE_STUDIES = CASE_STUDIES_LIT+CASE_STUDIES_SYNTCOMP+CASE_STUDIES_SYNTECH15

    CONFIGS=["50-25-25","50-15-35","50-20-30","60-20-20","60-15-25","70-15-15","70-20-10","80-10-10","80-07-13","90-05-05"]
    CONFIGS_SENSITIVITY=["REALSEM","REALSYN","REAL","SEM","SYN", "SYNSEM"]
    ALL_CONFIGS = CONFIGS_SENSITIVITY+CONFIGS
    #ALL_CONFIGS=["50-25-25"]

    PROPERTIES=["sol","syn","sem"]

    for PROP in PROPERTIES:
        ofile = "statistical-analysis-70-10-20"+PROP+".csv"
        f = open(ofile, "w")
        f.write("config,w,p,estimate,maginute\n")

        random_indexes = data['config']=="70-10-20"
        random_values = data[PROP][random_indexes]
        clean_random_values = [x for x in random_values.values if ~np.isnan(x)]
        print(clean_random_values)
        for config in ALL_CONFIGS:
            config_indexes = data['config']==config 
            config_values = data[PROP][config_indexes]
            clean_config_values = [x for x in config_values.values if ~np.isnan(x)]
            # print(config_values)
            d = random_values.values - config_values.values
            # print(d)
            w,p = ss.wilcoxon(d)
            estimate,magnitude = VD_A(clean_random_values, clean_config_values) 
            print(estimate)
            print(magnitude)
            exp = config+",{:.2f},{:.10f},{:.10f},".format(w,p,estimate)+magnitude+"\n"
            print(exp)
            f.write(exp)
        f.close()

def compute_VDA_per_case():

    ifile = "summary-per-case.csv"

    data = pd.read_csv(ifile)
    # data = data.dropna()
    print(data)
    CASE_STUDIES_LIT=["arbiter","minepump","RG1","RG2","Lift","HumanoidLTL_458","GyroUnrealizable_Var1","GyroUnrealizable_Var2"]
    CASE_STUDIES_SYNTCOMP=["detector","full_arbiter","lily02","lily11","lily15","lily16","load_balancer","ltl2dba_R_2","ltl2dba_theta_2","ltl2dba27","prioritized_arbiter","round_robin","simple_arbiter"]
    CASE_STUDIES_SYNTECH15=["HumanoidLTL_503_Humanoid_fixed_unrealizable","HumanoidLTL_531_Humanoid_unrealizable","HumanoidLTL_741_Humanoid_unrealizable","HumanoidLTL_742_Humanoid_unrealizable", "PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable"]
    #"PcarLTL_769_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable",
    ALL_CASE_STUDIES = CASE_STUDIES_LIT+CASE_STUDIES_SYNTCOMP+CASE_STUDIES_SYNTECH15

    CONFIGS=["50-25-25","50-15-35","50-20-30","60-20-20","60-15-25","70-15-15","70-10-20","70-20-10","80-10-10","80-07-13","90-05-05"]
    CONFIGS_SENSITIVITY=["REALSEM","REALSYN","REAL","SEM","SYN", "SYNSEM"]
    ALL_CONFIGS = CONFIGS_SENSITIVITY+CONFIGS+["random"]
    #ALL_CONFIGS=["random","70-10-20"]

    PROPERTIES=["sol","syn","sem"]

    summary_f = open("statistics/summary.csv", "w")
    summary_f.write("c1,c2,num,ratio\n")
    for C1 in ALL_CONFIGS:
        for C2 in ALL_CONFIGS:
            if (C1 != C2):
                for PROP in PROPERTIES:
                    ofile = "statistics/"+PROP+"-"+C1+"-"+C2+".csv"
                    f = open(ofile, "w")
                    f.write("c1,c2,case,estimate,maginute\n")
                    #c1Betterc2 = 0
                    for case in ALL_CASE_STUDIES:   
                        C1_indexes = np.where((data['config']==C1) & (data['case']==case))
                        print(case)
                        print(C1_indexes)
                        C1_values = data.loc[C1_indexes][PROP]
                        clean_C1_values = [x for x in C1_values.values if ~np.isnan(x)]

                        C2_indexes = np.where((data['config']==C2) & (data['case']==case))
                        C2_values = data.loc[C2_indexes][PROP]
                        clean_C2_values = [x for x in C2_values.values if ~np.isnan(x)] 

                        print(clean_C1_values)
                        print(len(clean_C1_values))
                        print(clean_C2_values)
                        print(len(clean_C2_values))
                        if (len(clean_C1_values)>0 and len(clean_C2_values)>0):
                            # d = C1_values.values - C2_values.values
                            # # print(d)
                            # w,p = ss.wilcoxon(d)
                            estimate,magnitude = VD_A(clean_C1_values, clean_C2_values) 
                            print(estimate)
                            print(magnitude)
                            # if estimate > 0.5:
                            #     c1Betterc2++
                            exp = C1+","+C2+","+case+",{:.10f},".format(estimate)+magnitude+"\n"
                            print(exp)
                            f.write(exp)
                    f.close()

    summary_f.close()


def compute_sucess_ratio():

    ifile = "summary-per-case.csv"

    data = pd.read_csv(ifile)
    # data = data.dropna()
    print(data)
    CASE_STUDIES_LIT=["arbiter","minepump","RG1","RG2","Lift","HumanoidLTL_458","GyroUnrealizable_Var1","GyroUnrealizable_Var2"]
    CASE_STUDIES_SYNTCOMP=["detector","full_arbiter","lily02","lily11","lily15","lily16","load_balancer","ltl2dba_R_2","ltl2dba_theta_2","ltl2dba27","prioritized_arbiter","round_robin","simple_arbiter"]
    CASE_STUDIES_SYNTECH15=["HumanoidLTL_503_Humanoid_fixed_unrealizable","HumanoidLTL_531_Humanoid_unrealizable","HumanoidLTL_741_Humanoid_unrealizable","HumanoidLTL_742_Humanoid_unrealizable", "PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable"]
    #"PcarLTL_769_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable",
    ALL_CASE_STUDIES = CASE_STUDIES_LIT+CASE_STUDIES_SYNTCOMP+CASE_STUDIES_SYNTECH15

    CONFIGS=["50-25-25","50-15-35","50-20-30","60-20-20","60-15-25","70-15-15","70-10-20","70-20-10","80-10-10","80-07-13","90-05-05"]
    CONFIGS_SENSITIVITY=["REALSEM","REALSYN","REAL","SEM","SYN", "SYNSEM"]
    ALL_CONFIGS = CONFIGS_SENSITIVITY+CONFIGS+["random"]
    #ALL_CONFIGS=["random","70-10-20"]

    PROPERTIES=["sol","syn","sem"]

    summary_f = open("statistics/best-config-summary.csv", "w")
    summary_f.write("case,sol,syn,sem\n")
    
    for case in ALL_CASE_STUDIES: 
        case_max = case
        for PROP in PROPERTIES: 
            C1 = ALL_CONFIGS[0]
            MAX = C1
            for I in range(1,len(ALL_CONFIGS)):
                C2 = ALL_CONFIGS[I]            
            
                #if (C1 != C2):                     
                C1_indexes = np.where((data['config']==C1) & (data['case']==case))
                print(case)
                print(C1_indexes)
                C1_values = data.loc[C1_indexes][PROP]
                clean_C1_values = [x for x in C1_values.values if ~np.isnan(x)]

                C2_indexes = np.where((data['config']==C2) & (data['case']==case))
                C2_values = data.loc[C2_indexes][PROP]
                clean_C2_values = [x for x in C2_values.values if ~np.isnan(x)] 

                print(clean_C1_values)
                print(len(clean_C1_values))
                print(clean_C2_values)
                print(len(clean_C2_values))
                if (len(clean_C1_values)>0 and len(clean_C2_values)>0):
                    # d = C1_values.values - C2_values.values
                    # # print(d)
                    # w,p = ss.wilcoxon(d)
                    estimate,magnitude = VD_A(clean_C1_values, clean_C2_values) 
                    print(estimate)
                    print(magnitude)
                    if estimate < 0.5:
                        MAX = C2
                C1 = C2

            case_max +=","+MAX
        case_max += "\n"
        summary_f.write(case_max)
    summary_f.close()

if __name__ == "__main__":
    #compute_VDA_per_case()
    compute_sucess_ratio()
