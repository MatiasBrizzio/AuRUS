#ifndef SATSolver_h
#define SATSolver_h

//////////////////////////////////////////////////////////////////////////////////////////////////
// SATSolver

/////////////////
// OS Includes
#include <time.h>

//////////////
// Includes
#ifdef NO_GMP
#include "BigNum_fake.h"
#else
#ifdef MICROSOFT
#include "BigNum_dll.h"
#else
#include "BigNum.h"
#endif
#endif
#include "Clause.h"
#include "ClauseList.h"
#include "VariableList.h"
#include "VariableSet.h"
#include "Random.h"

/////////////
// Defines

////////////////////////
// Class Declarations
class SATInstance;
class SATPreprocessor;
class VariableList;
class VariableSet;

//////////////////////////////////////////////////////////////////////////////////////////////////
// Class Definitions

enum relsat_enum { UNSAT, SAT, TIMEOUT };

class SolutionInfo {
public:
  SolutionInfo(int iVariableCount_) : xSolutionCount(1), xGoodList(iVariableCount_),
  xGoodReason(iVariableCount_), pOldSolutionCount(0) {}
  ~SolutionInfo() {
    delete pOldSolutionCount;
  }

  VariableList xGoodList;
  VariableSet xGoodReason;
  BigNum xSolutionCount;
  BigNum* pOldSolutionCount;
};

class VariableStruct {
 public:
  VariableStruct() : bBranch(0), pSolutionInfo(0), pReason(0), pDeleteReason(0) {}
  ~VariableStruct() {
    delete pSolutionInfo;
    delete pDeleteReason;
    xDeleteList.vDestroy();
  }
  // stores list of clauses containing positive occurence of this variable.
  ClauseList xPositiveClauses; 
  // stores list of clauses containing negative occurence of this variable.
  ClauseList xNegativeClauses; 
  Clause* pReason; // stores the clause that excludes some assignment
  Clause* pDeleteReason; // set if the reason needs to be deleted once assignment is changed
  ClauseList xUnitClause; // stores a list of non-trivial unit clauses
  // stores a list of clauses that must be deleted once assignment is changed.
  ClauseList xDeleteList; 
  boolean bBranch;
  SolutionInfo* pSolutionInfo;
};

class SATSolver {
  friend class SATPreprocessor;
public:
  SATSolver(SATInstance*, ostream&);
  ~SATSolver();

  // Use to modify runtime parameters.
  void vSetLearnOrder(int iLearnOrder_) { _iLearnOrder = iLearnOrder_; }
  void vSetFindAll(boolean bFindAll_) { _bFindAll = bFindAll_; }
  void vSetSolutionLimit(int iMaxSolutions_) { _iMaxSolutions = iMaxSolutions_; }
  // ^^ set iMaxSolutions_ to 0 to find all solutions.
  void vSetTimeout(long int iSeconds_) { _iMaxTime = iSeconds_; _bNoTimeLimit = 0; }
  void vSetNoTimeLimit(boolean bWhich_) { _bNoTimeLimit = bWhich_; }
  void vSetFavorSmallClauses(boolean bWhich_) { _bFavorSmallClauses = bWhich_; }
  void vSetRelevanceBounding(boolean bWhich_) { _bRelevanceBounding = bWhich_; }
  void vSetPrintStack(boolean bWhich_) { _bPrintStack = bWhich_; }
  void vSetPrintStackPeriod(long int iSeconds_);
  void vSetFudgeFactor(float fTo_) { _fFudgeFactor = fTo_; }
  void vSetRestarts(boolean bRestart_) { _bRestarts = bRestart_; }
  void vSetRestartInterval(int iSeconds_);
  void vSetRestartIncrement(int iSeconds_);
  
  boolean bPreprocess(int iLevel_, int iIterationBound_);
  void vIncorporateLearnedClauses();
  void vOutputWarnings();
  void vOutputStatusUpdateInterval();
  relsat_enum eSolve();

  unsigned long iBranchSelections() {return _iBranchSelections;}
  unsigned long iVariablesLabeled() {return _iVariablesLabeled;}
  unsigned long iContradictionsDiscovered() {return _iContradictions;}
  time_t iElapsedSeconds() { return _iElapsedTime;}

  boolean bIsCounting() { return !_bFindAll; }
  BigNum* pSolutionCount() { return &_xSolutionCount; }  
  // ^^ Use only if bIsCounting(), after solving the instance.

  Random xRandom;
  ostream& xOutputStream;

  // Primary variable list will be deleted upon destruction of this solver, or upon
  // the setting of another primary variable list.
  void vSetPrimaryVariables(VariableSet* pPrimaryVariables_) {
    _pPrimaryVariables = pPrimaryVariables_;
  }

private:
  boolean _bVerifySolution();
  boolean _bOutputSolution();
  boolean _bInitialize();
  boolean _bInitializeClause(Clause*);
  boolean _bInitializeLearnedClause(Clause*);
  boolean _bInitializeUnaryClause(Clause*);
  void _vCleanup();

  VariableID _eGiveMeTheBest(boolean& bZeroFirst_, boolean bFavorPrimary_);
  void _vComputeNoBinaryScores(double& fBest_, boolean bFavorPrimary_);
  VariableID _eMostDistantVariable();
  VariableID _eFindContradiction(boolean& bZeroFirst_, VariableID eID_);
  int _iScoreClauseList(Clause** pStart_, Clause** const pEnd_);
  inline double _iCombineScores(double i1_, double i2_);
  boolean _bLoop(boolean&);
  boolean _bRestartLoop(boolean&);

  boolean _bFastUnitPropagate(VariableID eWhich_, DomainValue iAssignment_, int& iScore_);

  void _vFastBackup(const int iIndex_);
  void _vFastBackupScore();

  boolean _bUnitPropagate();
  boolean _bNonTrivialUnitPropagate(ClauseList& xUnitClauses_);
  boolean _bBackup();
  boolean _bSpecialBackup();
  VariableStruct* _pBackupToFirstBranch();

  boolean _bFilterWithClauseList(Clause** pStart_, Clause** pEnd_);
  void _vSatisfyWithClauseList(Clause** pStart_, Clause** pEnd_);

  inline void _vDecideFilterClause(VariableID eID_, Clause* pWorkClause_);
  boolean _bCreateBackupClauseFromContradiction();
  boolean _bCreateNewBackupClause(boolean);
  Clause* _pLearn();

  void _vCreateGoodReason();
  void _vUpdateGoodReason(Clause** pStart, Clause** pEnd, VariableSet&);
  VariableID _eFindDeepestID(VariableList& xList_);

  inline void _vLabelVariable(VariableID, DomainValue);
  inline void _vDeleteClauses(ClauseList& rClauseList_);
  void _vUndoClauseModifications();
  inline void _vSetContradiction(VariableID, Clause*);

  boolean _bTimeLimitExpired();
  boolean _bPrintStackCheck();
  void _vPrintStack();

  // Preprocessor methods.
  void _vCleanClauseLists();
  boolean _bFastUnitPropagate();
  int _iFastUnitPropagate(VariableID& eUnit_);
  boolean _bMakesRedundant(Clause* pClause1_, Clause* pClause2_);
  boolean _bRedundancyCheck(Clause* pClause_);
  void _vOneSidedRedundancyCheck(Clause* pClause_, int& iClausesRemoved_);
  int _iRedundancyCheck(Clause* pClause1_, Clause* pClause2_);
  boolean _bResolve(Clause* pClause_, int& iCount_);
  boolean _bResolve(int& iCount_);
  Clause* _pResolve(Clause* pClause1_, 
		    Clause* pClause2_, 
		    VariableID iResolveVariable_);
  boolean _bUnitReduce(int& iNewClauses_);
  boolean _bBinaryReduce(VariableID eWith_, DomainValue lWhich_, int& iNewClauses_);
  void _vRemoveRedundancies(int iStartIndex_, int& iClausesRemoved);
  Clause* _pReduceClause(Clause* pReduceMe_);
  boolean _bReduceClauses(int& iNewClauses_);
  boolean _bBinaryInfer(int& iNewClauses_);
  void _vCleanup(time_t iStart_, int iInitialClauseCount_);

  // Private data
  VariableID _eLastWasBranch;
  time_t _iLastRestart;

  BigNum _xSolutionCount;
  BigNum _xKnownSolutions;
  long int _iSolutionCount; // when not counting all solutions, use this long

  unsigned long _iVariablesLabeled, _iBranchSelections, _iContradictions;
  time_t _iElapsedTime;
  time_t _iLastCheckTime;

  long int _iCurrentVariable;
  VariableID _eCurrentID;
  int _iVariableCount;
  VariableID _eContradictionID;
  Clause* _pContradictionClause1;
  Clause* _pContradictionClause2;

  VariableStruct* _aVariableStruct;
  VariableID* _aPositionToID; // Maps variable stack position to variable ID
  long int* _aIDToPosition; // Maps variable ID to stack position.
  int* _aScore0;
  int* _aScore1;
  double* _aScore;
  long int* _aBinaryCount0;
  long int* _aBinaryCount1;

  VariableSet* _pUnitVariables0;  // Variables with unit sized domains containing 0
  VariableSet* _pUnitVariables1;  // Variables with unit sized domains containing 1
  VariableSet* _pPositiveBackup;
  VariableSet* _pNegativeBackup;
  VariableList* _pUnitList;

  VariableSet* _pGoodList;
  VariableList* _pGoodReason;

  SATInstance* _pInstance;
  DomainValue* _aAssignment;
  ClauseList _xLearnedClauses;

  // Ugly hack vars.
  boolean _bReverse;

  // Preprocessor vars.
  VariableSet* _pSet0;
  VariableSet* _pSet1;

  // Runtime parameters:
  boolean _bNoTimeLimit;
  long int _iMaxTime;
  int _iLearnOrder;
  boolean _bFavorSmallClauses;
  boolean _bPrintStack;
  int _iPrintStackPeriod;
  boolean _bRelevanceBounding;
  float _fFudgeFactor;
  boolean _bFindAll;
  long int _iMaxSolutions;
  time_t _iRestartInterval;
  time_t iLastRestart;
  int _iRestartIncrement;
  boolean _bRestarts;
  int _iRelevantClauses;

  // primary variable related vars
  VariableSet* _pPrimaryVariables;
};

//////////////////////////////////////////////////////////////////////////////////////////////////
// Inlines

inline double SATSolver::_iCombineScores(double i1_, double i2_)
{
  return (2 * i1_ * i2_) + i1_ + i2_ + 1;
}

#endif // SATSolver_h
