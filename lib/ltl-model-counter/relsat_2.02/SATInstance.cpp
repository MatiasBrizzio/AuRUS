/////////////////
// OS Includes
#include <fstream>
#include <iomanip>

//////////////
// Includes
#include "Clause.h"
#include "Random.h"
#include "SATInstance.h"
#include "SATSolver.h"

/////////////
// Defines

/////////////////////////////
// Static data initialization 

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Public Methods

SATInstance::~SATInstance() 
{
  vDestroy(); 
}

void SATInstance::vOutputDimacs(ostream& xUseMe_) const
{
  xUseMe_ << "p cnf " << iVariableCount << ' ' << _iClauseCount << '\n';
  int i;
  for (i=0; i<_iClauseCount; i++) {
    Clause* pPrintMe = _aClause[i];
    if (!pPrintMe->bLearned()) {
      for (int j=0; j < pPrintMe->iVariableCount(); j++) {
	if (pPrintMe->iIsNegated(j))
	  xUseMe_ << "-";
	xUseMe_ << (int) (pPrintMe->eConstrainedVariable(j)+1) << " "; 
      }
      xUseMe_ << " 0\n";
    }
    xUseMe_ << flush;
  }

  xUseMe_ << "c NOTE: The following clauses are derived (non-essential):\n";
  for (i=0; i<_iClauseCount; i++) {
    Clause* pPrintMe = _aClause[i];
    if (pPrintMe->bLearned()) {
      for (int j=0; j < pPrintMe->iVariableCount(); j++) {
	if (pPrintMe->iIsNegated(j))
	  xUseMe_ << "-";
	xUseMe_ << (int) (pPrintMe->eConstrainedVariable(j)+1) << " "; 
      }
      xUseMe_ << " 0\n";
    }
    xUseMe_ << flush;
  }
}

VariableSet* SATInstance::pReadPrimaryVariables(const char* aFileName_)
{
  // Primary variable file format: list of (any kind of) whitespace separated variable numbers.
  // Call only after bReadDimacs.
  // The caller of this method is responsible for deleting the returned VariableSet.
  ifstream xInputFile(aFileName_);
  VariableSet* return_me =  pReadPrimaryVariables(xInputFile);
  xInputFile.close();
  return return_me;
}

VariableSet* SATInstance::pReadPrimaryVariables(istream& xInputFile)
{
  char buffer[5000];
  VariableID eVariableID;
  VariableSet* return_me = new VariableSet(iVariableCount);
  while (1) {
    xInputFile >> eVariableID;
    if (xInputFile.eof()) {
      return return_me;
    }
    if (eVariableID < 1 || eVariableID > iVariableCount) {
      xOutputStream << "\nError: Primary variable file contains unexpected variable id." << endl;
      return 0;
    }
    else {
      return_me->vAddVariable(eVariableID-1);
    }
  }
}

boolean SATInstance::bReadDimacs(const char* aFileName_)
{
  ifstream xInputFile(aFileName_);
  boolean return_me = bReadDimacs(xInputFile);
  xInputFile.close();
  return return_me;
}

boolean SATInstance::bReadDimacs(istream& xInputFile)
{
  xOutputStream << "c Reading instance..." << flush;
  char buffer[5000];
  VariableID eMaxVar = 0;
  int iClauseCount;
  char cCheck;

  while (1) {
    xInputFile >> cCheck;
    if (cCheck == 'c') {
      xInputFile.getline(buffer, 5000);
      continue;
    }
    else if (cCheck == 'p') {
      xInputFile >> setw(5000) >> buffer;
      xInputFile >> eMaxVar;
      xInputFile >> iClauseCount;
      break;
    }
    else {
      xOutputStream << "\nError: File not in DIMACS format?\n";
      return 0;
    }
  }
  VariableSet xPositiveVariables(eMaxVar);
  VariableSet xNegativeVariables(eMaxVar);

  iVariableCount = eMaxVar;
  int iWorkCount = 0;
  while (1) {
    if (xInputFile.eof()) {
      xOutputStream << "\nError: Unexpected end of file.\n";
      return 0;
    }
    xInputFile >> cCheck;
    if (cCheck == 'c') {
      xInputFile.getline(buffer, 5000);
      continue;
    }
    else xInputFile.putback(cCheck);

    xPositiveVariables.vClear();
    xNegativeVariables.vClear();
    boolean bIgnoreMe = 0;
    do {
      if (xInputFile.eof()) {
	xOutputStream << "\nError: Unexpected end of file.\n";
	return 0;
      }
      VariableID eVar;
      xInputFile >> eVar;
      if (eVar == 0)
	break;
      if (eVar > eMaxVar){
	xOutputStream << "\nError: some variable is numbered larger than the maximum.\n";
	return 0;
      }
      if (eVar < 0) {
	if (xPositiveVariables.bHasVariable(0-(eVar+1))) {
	  bIgnoreMe = 1;// Tautology
	}
	else {
	  xNegativeVariables.vAddVariable(0-(eVar+1));
	}
      }
      else {
	if (xNegativeVariables.bHasVariable(eVar-1)) {
	  bIgnoreMe = 1;// Tautology
	}
	else {
	  xPositiveVariables.vAddVariable(eVar-1);
	}
      }
    } while (1);

    if (!bIgnoreMe) {
      if (xNegativeVariables.iCount() + xPositiveVariables.iCount() == 0) {
	xOutputStream << "\nError: encountered a 0 length clause \n" << endl ;
	return 0;
      }
      // Silly up-casting needed to keep xlC compiler happy
      Clause* pNewConstraint = new Clause((VariableList&)xPositiveVariables, 
					  (VariableList&)xNegativeVariables,
					  1);
      pNewConstraint->vSortVariableList();
      vAddClause(pNewConstraint);
      iWorkCount++;
    }
    else iClauseCount--;
    if (iWorkCount == iClauseCount)
      break;
  }
  xOutputStream << "..done." << endl; 
  return 1;
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Protected Methods

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Private Methods
