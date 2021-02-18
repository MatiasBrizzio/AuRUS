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

# outname = "configuration.pdf"
# data = pd.read_csv('data-configuration.cvs')
#ifile = sys.argv[1]
#CASE_STUDIES=["arbiter","minepump","RG1","RG2","Lift","HumanoidLTL_458","GyroUnrealizable_Var1","GyroUnrealizable_Var2"]
CASE_STUDIES=["detector","full_arbiter","lily02","lily11","lily15","lily16","load_balancer","ltl2dba_R_2","ltl2dba_theta_2","ltl2dba27","prioritized_arbiter","round_robin","simple_arbiter"]
#CONFIGS=["50-25-25","50-15-35","50-20-30","60-20-20","60-15-25","70-15-15","70-10-20","70-20-10","80-10-10","80-07-13","90-05-05"]

#add initial lines of cvs
ifolder = sys.argv[1]
config = sys.argv[2] 
out_config = config
if len(sys.argv) >= 4:
  out_config = sys.argv[3]

for case_study in CASE_STUDIES:
    print (case_study , "\n")
    ofile = ifolder+"/"+case_study+"/"+case_study+"-"+out_config+"-"+"distances.csv"
    f = open(ofile, "w")
    f.write("id,fit,syn,sem,sol\n")

    #for each case study compute the avg in each run
    # fit_mean = []
    # syn_mean = []
    # sem_mean = []
    for K in range(10):
        cvs_file = ifolder+"/"+case_study+"/"+case_study+"-"+config+"-"+str(K)+"/distances.csv"
        if path.exists(cvs_file):
            data = pd.read_csv(cvs_file)
            fit = np.mean(data['fit'])
            syn = np.mean(data['syn'])
            sem = np.mean(data['sem'])
            sol = np.max(data['id'])
            # fit_mean.append(fit)
            # syn_mean.append(syn)
            # sem_mean.append(sem)
            f.write(str(K))
            f.write(",{:.2f}".format(fit))
            f.write(",{:.2f}".format(syn))
            f.write(",{:.2f}".format(sem))
            f.write(","+str(sol))
            f.write("\n")

    #mean_maximum = np.max(config_means)
    f.close()




