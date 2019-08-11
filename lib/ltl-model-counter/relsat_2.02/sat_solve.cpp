/////////////////
// OS Includes
#include <fstream>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "GetOpt.h"

//////////////
// Includes
#include "ClauseList.h"
#include "Clause.h"
#include "Random.h"
#include "SATSolver.h"
#include "SATInstance.h"

/////////////
// Defines

void printHelpMessage(ostream& xOutputStream_)
{
    xOutputStream_ << "==== relsat, version 2.20 (alpha1) ====" << endl;
    xOutputStream_ << "Usage: relsat [options] {filename}";
    xOutputStream_ << endl;
    xOutputStream_ << "Options include: " << endl;
    xOutputStream_ << " -l<int>          Learn order" << endl;
    xOutputStream_ << " -#{<int>|a|c}    {# of solutions | all | count}" << endl;
    xOutputStream_ << " -s<int>          Random # seed (> 0)" << endl;
    xOutputStream_ << " -r{<int>|n}      Restart interval {seconds | no restarts}" << endl;
    xOutputStream_ << " -a{<int>}        Restart interval increment {seconds}" << endl;
    xOutputStream_ << " -f<float>        Fudge factor, in the range [0.0-1.0]" << endl;
    xOutputStream_ << " -t{<int>|n}      Timeout interval {seconds | no timeout}" << endl;
    xOutputStream_ << " -o{<filename>|n} Output instance {filename | no output instance}" << endl;
    xOutputStream_ << " -u{<int>|n}      Status update interval {seconds | no updates}" << endl;
    xOutputStream_ << " -p{0|1|2|3}      Pre-processing level" << endl;
    xOutputStream_ << " -c{0|1|2|3}      Post-processing level" << endl;
    xOutputStream_ << " -i{<int>|n}      Pre & post-processing iteration bound {bound | no bound}" << endl;
    xOutputStream_ << " -v{<filename>}   Filename containing primary variable list." << endl;
    xOutputStream_ << endl;
    xOutputStream_ << "Default options: " << endl;
    xOutputStream_ << "    -p1 -l3 -#1 -s1 -rn -a0 -f.9 -t43200 -u10 -o timeout.cnf -c3 -in"
	 << endl;
}

void invoke_relsat(int iArgc_, const char* aArgv_[], ostream& xOutputStream_, int iIteration_)
{
  if (iArgc_ < 2) {
    printHelpMessage(xOutputStream_);
    return;
  }

  int c;
  extern int optind;
  extern char *optarg;
  SATInstance xInstance(xOutputStream_);
  SATSolver xSATSolver(&xInstance, xOutputStream_);
  int iPreprocessLevel = 1;
  int iOutputPreprocessLevel = 3;
  int iIterationBound = -1;
  int iRandomSeed = 1;
  int iWork;
  float fWork;
  boolean bPrimaryVariableList = 0;
  char aPrimaryVariableFilename[512];
  boolean bOutputInstance = 1;
  char aOutputInstance[512];
  boolean bNoTimeout = 0;
  strcpy(aOutputInstance, "timeout.cnf");
  GetOpt xGetOpt(iArgc_, aArgv_, "i:p:l:#:s:t:r:u:f:o:c:v:a:h?");
  while ((c = xGetOpt.getopt()) != -1) {
    switch(c) {
    case '?':
    case 'h':
      printHelpMessage(xOutputStream_);
      return;
      break;
    case 'v':
      {
	int length = strlen(xGetOpt.optarg);
	if (length >= 512) {
	  printHelpMessage(xOutputStream_);
	  return;
	}
	else {
	  bPrimaryVariableList = 1;
	  strcpy(aPrimaryVariableFilename, xGetOpt.optarg);
	}
      }
      break;
    case 'p':
      sscanf(xGetOpt.optarg, "%d", &iPreprocessLevel);
      if (iPreprocessLevel < 0 || iPreprocessLevel > 4) {
	printHelpMessage(xOutputStream_);
	return;
      }
      break;
    case 'c':
      sscanf(xGetOpt.optarg, "%d", &iOutputPreprocessLevel);
      if (iOutputPreprocessLevel < 0 || iOutputPreprocessLevel > 4) {
	printHelpMessage(xOutputStream_);
	return;
      }
      break;
    case 'i':
      if (xGetOpt.optarg[0] == 'n') {
	iIterationBound = -1;
      }
      else {
	sscanf(xGetOpt.optarg, "%d", &iIterationBound);
	if (iIterationBound < 1) {
	  printHelpMessage(xOutputStream_);
	  return;
	}
      }
      break;
    case 'l':
      sscanf(xGetOpt.optarg, "%d", &iWork);
      if (iWork < 0) {
	printHelpMessage(xOutputStream_);
	return;
      }
      xSATSolver.vSetLearnOrder(iWork);
      break;
    case '#':
      if (xGetOpt.optarg[0] == 'a') {
	xSATSolver.vSetSolutionLimit(0);
      }
      else if (xGetOpt.optarg[0] == 'c') {
	xSATSolver.vSetFindAll(0);
      }
      else {
	sscanf(xGetOpt.optarg, "%d", &iWork);
	if (iWork < 1) {
	  printHelpMessage(xOutputStream_);
	  return;
	}
	xSATSolver.vSetSolutionLimit(iWork);
      }
      break;
    case 's':
      sscanf(xGetOpt.optarg, "%d", &iRandomSeed);
      if (iRandomSeed < 1) {
	printHelpMessage(xOutputStream_);
	return;
      }
      break;
    case 'r':
      if (xGetOpt.optarg[0] != 'n') {
	sscanf(xGetOpt.optarg, "%d", &iWork);
	if (iWork <= 0) {
	  printHelpMessage(xOutputStream_);
	  return;
	}
	xSATSolver.vSetRestartInterval(iWork);
      }
      break;
    case 'a':
	sscanf(xGetOpt.optarg, "%d", &iWork);
	if (iWork <= 0) {
	  printHelpMessage(xOutputStream_);
	  return;
	}
	xSATSolver.vSetRestartIncrement(iWork);
	break;
    case 't':
      if (xGetOpt.optarg[0] == 'n') {
	xSATSolver.vSetNoTimeLimit(1);
	bNoTimeout = 1;
      }
      else {
	sscanf(xGetOpt.optarg, "%d", &iWork);
	if (iWork < 0) {
	  printHelpMessage(xOutputStream_);
	  return;
	}
	xSATSolver.vSetTimeout(iWork);
      }
      break;
    case 'f':
      sscanf(xGetOpt.optarg, "%f", &fWork);
      if (fWork <0.0 || fWork > 1.0) {
	printHelpMessage(xOutputStream_);
	return;
      }
      xSATSolver.vSetFudgeFactor(fWork);
      break;
    case 'u':
      if (xGetOpt.optarg[0] == 'n') {
	xSATSolver.vSetPrintStack(0);
      }
      else {
	sscanf(xGetOpt.optarg, "%d", &iWork);
	if (iWork < 1) {
	  printHelpMessage(xOutputStream_);
	  return;
	}
	xSATSolver.vSetPrintStackPeriod(iWork);
      }
      break;
    case 'o':
      int len = strlen(xGetOpt.optarg);
      if (len >= 512) {
	printHelpMessage(xOutputStream_);
	return;
      }
      if (len == 1 && xGetOpt.optarg[0] == 'n') {
	bOutputInstance = 0;
      }
      else {
	strcpy(aOutputInstance, xGetOpt.optarg);
      }
      break;
    } // switch
  }

  if (xGetOpt.optind >= iArgc_) {
    printHelpMessage(xOutputStream_);
    return;
  }
  xOutputStream_ << "c Instance is: " << aArgv_[xGetOpt.optind] << endl;
  if (!xInstance.bReadDimacs(aArgv_[xGetOpt.optind]))
    return;
  if (bPrimaryVariableList) {
    xOutputStream_ << "c Primary variable list specified: " << aPrimaryVariableFilename << endl;
    VariableSet* pPrimaryVariables = xInstance.pReadPrimaryVariables(aPrimaryVariableFilename);
    if (pPrimaryVariables == 0) {
      // error
      return;
    }
    xOutputStream_ << "c Primary variables: ";
    for (int i=0; i<xInstance.iVariableCount; i++) {
      if (pPrimaryVariables->bHasVariable(i)) {
	xOutputStream_ << (i+1) << " ";
      }
    }
    xOutputStream_ << endl;
    xSATSolver.vSetPrimaryVariables(pPrimaryVariables);
  }
  iRandomSeed += iIteration_;
  xSATSolver.xRandom.vInitRandom(iRandomSeed);
  xOutputStream_ << "c Random number seed: " << iRandomSeed << endl;

  if (iPreprocessLevel) {
    xOutputStream_ << "c Pre-processing level: " << iPreprocessLevel << endl;
  }
  if (!bNoTimeout && bOutputInstance) {
    if (iOutputPreprocessLevel) {
      xOutputStream_ << "c Post-processing level: " << iOutputPreprocessLevel << endl;
    }
    xOutputStream_ << "c Output instance filename: " << aOutputInstance << endl;
  }
  if ((iPreprocessLevel || iOutputPreprocessLevel) && iIterationBound >= 0) {
    xOutputStream_ << "c Pre/Post-Processing iteration bound: " << iIterationBound << endl;
  }

  boolean bFailed;

  xSATSolver.vOutputStatusUpdateInterval();
  if (xSATSolver.bPreprocess(iPreprocessLevel,iIterationBound)) {
    xOutputStream_ << "c Determined during preprocessing:\n";
    xOutputStream_ << "UNSAT" << endl;
  }
  else {
    char* aCount;
    xSATSolver.vOutputWarnings();
    relsat_enum eResult = xSATSolver.eSolve();
    xOutputStream_ << "c Solution phase stats: " << endl;
    xOutputStream_ << "c   Variable Assignments        : " << xSATSolver.iVariablesLabeled() << endl;
    xOutputStream_ << "c   Branch Selections           : " << xSATSolver.iBranchSelections() << endl;
    xOutputStream_ << "c   Contradictions Discovered   : "
	 << xSATSolver.iContradictionsDiscovered()
	 << endl;
    xOutputStream_ << "c   Seconds Elapsed (real time) : " << xSATSolver.iElapsedSeconds() << endl;
    switch (eResult) {
    case SAT:
      if (xSATSolver.bIsCounting()) {
	xOutputStream_ << "Number of solutions: ";
	aCount = xSATSolver.pSolutionCount()->aToString();
	xOutputStream_ << aCount << endl;
	delete [] aCount;
      }
      xOutputStream_ << "SAT" << endl;
      break;
    case UNSAT:
      xOutputStream_ << "UNSAT" << endl;
      break;
    case TIMEOUT:
      xSATSolver.vIncorporateLearnedClauses();
      if (bOutputInstance && xSATSolver.bPreprocess(iOutputPreprocessLevel,
						    iIterationBound)) {
	xOutputStream_ << "c Determined during preprocessing after a timeout:\n";
	xOutputStream_ << "UNSAT" << endl;
      }
      else {
	if (bOutputInstance) {
	  // output instance and its learned clauses.
	  xOutputStream_ << "c Writing instance into " << aOutputInstance << "..." << flush;
	  ofstream xNewInstance;
	  xNewInstance.open(aOutputInstance, ios::out);
	  if (!xNewInstance) {
	    xOutputStream_ << "Failed to open output instance!" << endl;
	    return;
	  }
          xInstance.vDestroyDeletedClauses();
          xInstance.vSortClausesByLength();
	  xInstance.vOutputDimacs(xNewInstance);
	  xOutputStream_ << "..done." << endl;
	}
	xOutputStream_ << "TIME LIMIT EXPIRED" << endl;
      }
    }
  }
}
