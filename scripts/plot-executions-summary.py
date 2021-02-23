import sys
import chart_studio.plotly as py
import plotly.graph_objs as go
import plotly.figure_factory as FF
import plotly.express as px

import numpy as np
import pandas as pd
import scipy

ifile = "executions-summary.csv"


data = pd.read_csv(ifile)


CASE_STUDIES_LIT=["arbiter","minepump","RG1","RG2","Lift","HumanoidLTL_458","GyroUnrealizable_Var1","GyroUnrealizable_Var2"]
CASE_STUDIES_SYNTCOMP=["detector","full_arbiter","lily02","lily11","lily15","lily16","load_balancer","ltl2dba_R_2","ltl2dba_theta_2","ltl2dba27","prioritized_arbiter","round_robin","simple_arbiter"]
CASE_STUDIES_SYNTECH15=["HumanoidLTL_503_Humanoid_fixed_unrealizable","HumanoidLTL_531_Humanoid_unrealizable","HumanoidLTL_741_Humanoid_unrealizable","HumanoidLTL_742_Humanoid_unrealizable","PcarLTL_769_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_1_unrealizable.0_870_PCar_fixed_unrealizable","PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable"]
ALL_CASE_STUDIES = CASE_STUDIES_LIT+CASE_STUDIES_SYNTCOMP+CASE_STUDIES_SYNTECH15

CONFIGS=["50-25-25","50-15-35","50-20-30","60-20-20","60-15-25","70-15-15","70-10-20","70-20-10","80-10-10","80-07-13","90-05-05"]
CONFIGS_SENSITIVITY=["random","REALSEM","REALSYN","RREAL","SEM","SYN", "SYNSEM"]
ALL_CONFIGS = CONFIGS_SENSITIVITY+CONFIGS
# print(ALL_CONFIGS)
# sol_traces = []
# syn_traces = []
# sem_traces = []


# config_ids = enumerate(.unique())
# for C in data['config']:
#     x.append(config_ids.get(ALL_CONFIGS.index(data['config'])))
# print(x)
# V = 0
# for config in ALL_CONFIGS:
#     config_indexes = data['config']==config 
#     syn_values = data['syn'][config_indexes]
#     sem_values = data['sem'][config_indexes]
#     sol_avg = data['sol'][config_indexes]
#     sol_max = data['solMax'][config_indexes]
#     sol_values = sol_avg / sol_max
    # print(syn_values)
    # print("\n------\n")
    # print(sem_values)
    # print("\n------\n")
    # print(sol_avg)
    # print("\n------\n")
    # print(sol_max)
    # print("\n------\n")
    # print(sol_values)
    # print("\n------\n")

    #data[config]

sol_MAX_values = []
for case in ALL_CASE_STUDIES:
    case_indexes = data['case']==case 
    # print(case+"\n")
    sol_max = np.max(data['solMax'][case_indexes])
    print(case+" = "+ str(sol_max)+"\n")
    # newrow = (case,sol_max)
    sol_MAX_values.append(sol_max)

print(sol_MAX_values)
print("\n-------\n")
sol_values = []
for I in range(len(data['config'])):
    case = data['case'][I]
    maximum = sol_MAX_values[ALL_CASE_STUDIES.index(case)]
    print(case+" = "+ str(maximum)+"\n")
    sol_values.append(data['sol'][I]/maximum)

fig = go.Figure() 
x = data['config']
fig.add_trace(go.Box(x=x, y=sol_values, name = 'SOL', marker_color='red')) 
fig.add_trace(go.Box(x=x, y=data['syn'], name = 'SYN', marker_color='green'))
fig.add_trace(go.Box(x=x, y=data['sem'], name = 'SEM', marker_color='blue'))
fig.update_traces(quartilemethod="exclusive")

fig.update_layout(
    boxmode='group',
    # width=500,
    margin=dict(
        l=10,
        r=10,
        b=10,
        t=10,
        pad=4
    ),
    # template="plotly_white", #"plotly", "plotly_white", "plotly_dark", "ggplot2", "seaborn", "simple_white", "none"
    # showlegend=False,
    xaxis=dict(
        title = 'Configuration',
        type='category',
        #showticklabels=False
    ),
    yaxis=dict(
        title='Percentage of Solutions',
        zeroline=False,
        autorange=False, 
        range=[0,1.05]
    ),
)

#fig.show()
ofile = "executions-summary-syn.pdf"
fig.write_image(ofile)
#py.iplot(fig, filename='random.pdf')


