import sys
import chart_studio.plotly as py
import plotly.graph_objs as go
import plotly.figure_factory as FF
import plotly.express as px

import numpy as np
import pandas as pd
import scipy

import os.path
from os import path
import operator

MAX=10

def compute_distances(cvs_file):
    dtype = [('id', int), ('fit', float), ('syn', float), ('sem', float)]
    try:
        data = pd.read_csv(cvs_file)
        #data = np.array(data,dtype)
        #print(data[0:10])
        sorted_data = data.sort_values(by=['fit'], ascending=False)
        #print("\n-------\n")
        #print(sorted_data[0:10])
        sorted_data = sorted_data[0:MAX]
        fit = np.mean(sorted_data['fit'])
        syn = np.mean(sorted_data['syn'])
        sem = np.mean(sorted_data['sem'].replace(1, np.NaN))
        sol = np.max(data['id']+1)
        avg_distances = "{:.2f}".format(fit) + ",{:.2f}".format(syn) + ",{:.2f}".format(sem) + ","+str(sol) + "\n"
        return avg_distances
    except:
        return "0.0,0.0,0.0,0\n"


# outname = "configuration.pdf"
# data = pd.read_csv('data-configuration.cvs')
# ifile = sys.argv[1]

CONFIGS=["50-25-25","50-15-35","50-20-30","60-20-20","60-15-25","70-15-15","70-10-20","70-20-10","80-10-10","80-07-13","90-05-05"]
CONFIGS_SENSITIVITY=["REALSEM","REALSYN","RREAL","SEM","SYN", "SYNSEM"]

#add initial lines of cvs
# ifolder = sys.argv[1]
# config = sys.argv[2] 
# out_config = config
# if len(sys.argv) >= 4:
#  out_config = sys.argv[3]

CASE_STUDIES_LIT=["arbiter","minepump","RG1","RG2","Lift","HumanoidLTL_458","GyroUnrealizable_Var1","GyroUnrealizable_Var2"]
# for config in CONFIGS:
#     for case_study in CASE_STUDIES_LIT:
#         print (case_study , "\n")
#         ofile = "../results/result-"+config+"/"+case_study+"/"+case_study+"-"+config+"-"+"distances.csv"
#         f = open(ofile, "w")
#         f.write("id,fit,syn,sem,sol\n")
#         #for each case study compute the avg in each run
#         for K in range(10):
#             cvs_file = "../results/result-"+config+"/"+case_study+"/"+case_study+"-genuine-"+str(K)+"/distances.csv"
#             if path.exists(cvs_file):
#                 f.write(str(K)+",")
#                 avg_distances = compute_distances(cvs_file)
#                 f.write(avg_distances)
#         f.close()
# #random
# for case_study in CASE_STUDIES_LIT:
#     print (case_study , "\n")
#     ofile = "../results/result-random/"+case_study+"/"+case_study+"-random-distances.csv"
#     f = open(ofile, "w")
#     f.write("id,fit,syn,sem,sol\n")
#     #for each case study compute the avg in each run
#     for K in range(10):
#         cvs_file = "../results/result-random/"+case_study+"/"+case_study+"-random-"+str(K)+"/distances.csv"
#         if path.exists(cvs_file):
#             f.write(str(K)+",")
#             avg_distances = compute_distances(cvs_file)
#             f.write(avg_distances)
#     f.close()

# #sensitivity

for config in CONFIGS_SENSITIVITY:
    for case_study in CASE_STUDIES_LIT:
        print (case_study , "\n")
        ofile = "../results/sensitivityresult/"+case_study+"/"+case_study+"-"+config+"-"+"distances.csv"
        f = open(ofile, "w")
        f.write("id,fit,syn,sem,sol\n")
        #for each case study compute the avg in each run
        for K in range(10):
            cvs_file = "../results/sensitivityresult/"+case_study+"/"+case_study+"-"+config+"-"+str(K)+"/distances.csv"
            if path.exists(cvs_file):
                f.write(str(K)+",")
                avg_distances = compute_distances(cvs_file)
                f.write(avg_distances)
        f.close()



CASE_STUDIES_SYNTCOMP=["detector","full_arbiter","lily02","lily11","lily15","lily16","load_balancer","ltl2dba_R_2","ltl2dba_theta_2","ltl2dba27","prioritized_arbiter","round_robin","simple_arbiter"]

# for config in CONFIGS:
#     for case_study in CASE_STUDIES_SYNTCOMP:
#         print (case_study , "\n")
#         ofile = "../results-syntcomp/result-syntcomp-"+config+"/syntcomp-unreal/"+case_study+"/"+case_study+"-"+config+"-"+"distances.csv"
#         f = open(ofile, "w")
#         f.write("id,fit,syn,sem,sol\n")
#         #for each case study compute the avg in each run
#         for K in range(10):
#             cvs_file = "../results-syntcomp/result-syntcomp-"+config+"/syntcomp-unreal/"+case_study+"/"+case_study+"-genuine-"+str(K)+"/distances.csv"
#             if path.exists(cvs_file):
#                 f.write(str(K)+",")
#                 avg_distances = compute_distances(cvs_file)
#                 f.write(avg_distances)
#         f.close()

# #random
# for case_study in CASE_STUDIES_SYNTCOMP:
#     print (case_study , "\n")
#     ofile = "../results-syntcomp/result-syntcomp-random/syntcomp-unreal/"+case_study+"/"+case_study+"-random-distances.csv"
#     f = open(ofile, "w")
#     f.write("id,fit,syn,sem,sol\n")
#     #for each case study compute the avg in each run
#     for K in range(10):
#         cvs_file = "../results-syntcomp/result-syntcomp-random/syntcomp-unreal/"+case_study+"/"+case_study+"-genuine-"+str(K)+"/distances.csv"
#         if path.exists(cvs_file):
#             f.write(str(K)+",")
#             avg_distances = compute_distances(cvs_file)
#             f.write(avg_distances)
#     f.close()


# for config in CONFIGS_SENSITIVITY:
#     print (config , "\n")
#     for case_study in CASE_STUDIES_SYNTCOMP:
#         print (case_study , "\n")
#         ofile = "../results-syntcomp/sensitivityresult-syntcomp/syntcomp-unreal/"+case_study+"/"+case_study+"-"+config+"-"+"distances.csv"
#         f = open(ofile, "w")
#         f.write("id,fit,syn,sem,sol\n")
#         #for each case study compute the avg in each run
#         for K in range(10):
#             print (str(K) , "\n")
#             cvs_file = "../results-syntcomp/sensitivityresult-syntcomp/syntcomp-unreal/"+case_study+"/"+case_study+"-"+config+"-"+str(K)+"/distances.csv"
#             if path.exists(cvs_file):
#                 f.write(str(K)+",")
#                 avg_distances = compute_distances(cvs_file)
#                 f.write(avg_distances)
#         f.close()



# CASE_STUDIES_SYNTECH=["HumanoidLTL_503_Humanoid_fixed_unrealizable","HumanoidLTL_531_Humanoid_unrealizable","HumanoidLTL_741_Humanoid_unrealizable","HumanoidLTL_742_Humanoid_unrealizable","PcarLTL_769_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable"]
# #CONFIGS_SPECTRA=CONFIGS+["0-0-0","0-0-100","0-50-50","0-100-0","70-0-30","70-30-0","100-0-0"]
# CONFIGS_SPECTRA=["0-0-0","0-0-100","0-50-50","0-100-0","70-0-30","70-30-0","100-0-0"]
# for config in CONFIGS_SPECTRA:
#     for case_study in CASE_STUDIES_SYNTECH:
#         print (case_study , "\n")
#         ofile = "../results-spectra/result-spectra-"+config+"/SYNTECH15/"+case_study+"/"+case_study+"-"+config+"-"+"distances.csv"
#         f = open(ofile, "w")
#         f.write("id,fit,syn,sem,sol\n")
#         #for each case study compute the avg in each run
#         for K in range(10):
#             cvs_file = "../results-spectra/result-spectra-"+config+"/SYNTECH15/"+case_study+"/"+case_study+"-genuine-"+str(K)+"/distances.csv"
#             if path.exists(cvs_file):
#                 f.write(str(K)+",")
#                 avg_distances = compute_distances(cvs_file)
#                 f.write(avg_distances)
#         f.close()


