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

def compute_avg(cvs_file,config):
    data = pd.read_csv(cvs_file)
    fit = np.mean(data['fit'].replace(0, np.NaN))
    syn = np.mean(data['syn'].replace(0, np.NaN))
    sem = np.mean(data['sem'].replace(0, np.NaN).replace(1, np.NaN))
    sol = np.mean(data['sol'])
    sol_max = np.max(data['sol'])
    # fit_mean.append(fit)
    # syn_mean.append(syn)
    # sem_mean.append(sem)
    case_avg = config + ","+ case_study + ",{:.2f}".format(fit) + ",{:.2f}".format(syn) + ",{:.2f}".format(sem) + ",{:.0f}".format(sol) + ",{:.0f}".format(sol_max) + "\n"
    return case_avg

# outname = "configuration.pdf"
# data = pd.read_csv('data-configuration.cvs')
#ifile = sys.argv[1]
CASE_STUDIES_LIT=["arbiter","minepump","RG1","RG2","Lift","HumanoidLTL_458","GyroUnrealizable_Var1","GyroUnrealizable_Var2"]
CASE_STUDIES_SYNTCOMP=["detector","full_arbiter","lily02","lily11","lily15","lily16","load_balancer","ltl2dba_R_2","ltl2dba_theta_2","ltl2dba27","prioritized_arbiter","round_robin","simple_arbiter"]
CASE_STUDIES_SYNTECH15=["HumanoidLTL_503_Humanoid_fixed_unrealizable","HumanoidLTL_531_Humanoid_unrealizable","HumanoidLTL_741_Humanoid_unrealizable","HumanoidLTL_742_Humanoid_unrealizable", "PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable"]
    #"PcarLTL_769_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable",
CONFIGS=["50-25-25","50-15-35","50-20-30","60-20-20","60-15-25","70-15-15","70-10-20","70-20-10","80-10-10","80-07-13","90-05-05"]


CONFIGS_LIT_SYNTCOMP=CONFIGS+["random"]
#add initial lines of cvs
ofile = sys.argv[1]
f = open(ofile, "w")
f.write("config,case,fit,syn,sem,sol,solMax\n")
for config in CONFIGS_LIT_SYNTCOMP:
    for case_study in CASE_STUDIES_LIT:
        print (case_study , "\n")
        #for each case study compute the avg in each run
        cvs_file = "../results/result-"+config+"/"+case_study+"/"+case_study+"-"+config+"-distances.csv"
        if path.exists(cvs_file):
            case_avg = compute_avg(cvs_file,config) 
            f.write(case_avg)

    for case_study in CASE_STUDIES_SYNTCOMP:
        print (case_study , "\n")
        #for each case study compute the avg in each run
        cvs_file = "../results-syntcomp/result-syntcomp-"+config+"/syntcomp-unreal/"+case_study+"/"+case_study+"-"+config+"-distances.csv"
        if path.exists(cvs_file):
            case_avg = compute_avg(cvs_file,config) 
            f.write(case_avg)

CONFIGS_LIT_SYNTCOMP=["REALSEM","REALSYN","RREAL","SEM","SYN", "SYNSEM"]
for config in CONFIGS_LIT_SYNTCOMP:
    for case_study in CASE_STUDIES_LIT:
        print (case_study , "\n")
        #for each case study compute the avg in each run
        cvs_file = "../results/sensitivityresult/"+case_study+"/"+case_study+"-"+config+"-distances.csv"
        if path.exists(cvs_file):
            case_avg = compute_avg(cvs_file,config) 
            f.write(case_avg)

    for case_study in CASE_STUDIES_SYNTCOMP:
        print (case_study , "\n")
        #for each case study compute the avg in each run
        cvs_file = "../results-syntcomp/sensitivityresult-syntcomp/syntcomp-unreal/"+case_study+"/"+case_study+"-"+config+"-distances.csv"
        if path.exists(cvs_file):
            case_avg = compute_avg(cvs_file,config) 
            f.write(case_avg)


#CONFIGS_SPECTRA=CONFIGS+["0-0-0","0-0-100","0-50-50","0-100-0","70-0-30","70-30-0","100-0-0"]
for config in CONFIGS:   
    for case_study in CASE_STUDIES_SYNTECH15:
        print (case_study , "\n")
        #for each case study compute the avg in each run
        cvs_file = "../results-spectra/result-spectra-"+config+"/SYNTECH15/"+case_study+"/"+case_study+"-"+config+"-distances.csv"
        if path.exists(cvs_file):
            case_avg = compute_avg(cvs_file,config) 
            f.write(case_avg)

CONFIGS_SPECTRA=["0-0-0","0-0-100","0-50-50","0-100-0","70-0-30","70-30-0","100-0-0"]
SPECTRA_LABELS=["random", "SEM","SYNSEM","SYN","REALSEM","REALSYN", "RREAL"]
for config in CONFIGS_SPECTRA:   
    for case_study in CASE_STUDIES_SYNTECH15:
        print (case_study , "\n")
        #for each case study compute the avg in each run
        cvs_file = "../results-spectra/result-spectra-"+config+"/SYNTECH15/"+case_study+"/"+case_study+"-"+config+"-distances.csv"
        if path.exists(cvs_file):
            label = SPECTRA_LABELS[CONFIGS_SPECTRA.index(config)]
            case_avg = compute_avg(cvs_file,label)  
            f.write(case_avg)

#mean_maximum = np.max(config_means)
f.close()




