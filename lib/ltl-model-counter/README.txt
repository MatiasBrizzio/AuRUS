HOW TO INSTALL:

- The binaries provided here are for MAC OS. However, you can recompile the tool following the bellow steps:

1) remove binaries files: bool2cnf, relsat, ltl2pl
2) Move into bool2cnfsrc folder, and run: 
		make 
3) Move into relsat_2.02 folder and run: 
		make -f Makefile.linux
4) Move into the ltl-model-enumerator folder, and run: 
		ghc -XNPlusKPatterns -outputdir build LTL.hs -o ltl2pl 

Notice that, to compile all the tools, you will need: flex, bison, g++ and ghc.


HOW TO RUN:
In order to reprocude all the experiments of the paper submitted to ASE 2017, 
just run the script "run-case-studies.sh".

RESULTS:
You can find the results reported in the paper submitted to ASE 2017 in the "results" folder.

You can find the plotted images and the datas in the plot and plot-goal-satisfaction folders.
