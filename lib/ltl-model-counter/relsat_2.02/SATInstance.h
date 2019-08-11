#ifndef SATInstance_h
#define SATInstance_h

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// SATInstance

/////////////////
// OS Includes

//////////////
// Includes
#include "ClauseList.h"

/////////////
// Defines

////////////////////////
// Class Declarations

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Class Definitions

class SATInstance : public ClauseList {
public:
  SATInstance(ostream& xOutputStream_) : xOutputStream(xOutputStream_) {}
  ~SATInstance(); 
  void vOutput() const;
  void vOutputDimacs(ostream&) const;

  boolean bReadDimacs(const char* aFileName_);
  boolean bReadDimacs(istream&);

  // Primary variable file format: list of (any kind of) whitespace separated variable numbers.
  VariableSet* pReadPrimaryVariables(const char* aFileName_);
  VariableSet* pReadPrimaryVariables(istream&);

  void vMakeRandom_3SAT(int iVars_, int iClauses_);

  int iVariableCount;

  ostream& xOutputStream;
};

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Inlines

#endif // SATInstance_h
