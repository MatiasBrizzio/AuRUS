#ifndef SATPreprocessor_h
#define SATPreprocessor_h

//////////////////////////////////////////////////////////////////////////////////////////////////
// SATPreprocessor

/////////////////
// OS Includes

//////////////
// Includes

/////////////
// Defines

////////////////////////
// Class Declarations
class ClauseList;
class VariableSet;
class VariableStruct;

//////////////////////////////////////////////////////////////////////////////////////////////////
// Class Definitions

class SATPreprocessor {
public:
  SATPreprocessor() {}
  boolean bPreprocess(ClauseList& xAllClauses_, 
		      VariableStruct*, 
		      int iVariableCount_, 
		      ClauseList& _xLearnedList);
  
private:
  boolean _bRedundancyCheck(Clause* pClause_);
  int _iRedundancyCheck(Clause* pClause1_, Clause* pClause2_);
  boolean _bResolve(Clause* pClause_);
  Clause* _pResolve(Clause* pClause1_, 
		    Clause* pClause2_, 
		    VariableID iResolveVariable_);
  void _vAddClause(Clause*);

  VariableStruct* _aVariableStruct;
  ClauseList* _pLearnedList;
  ClauseList _xDeleteUs;
  VariableSet* _pSet0;
  VariableSet* _pSet1;
  int _iRedundantCount;
};

//////////////////////////////////////////////////////////////////////////////////////////////////
// Inlines

#endif //<file>_h

