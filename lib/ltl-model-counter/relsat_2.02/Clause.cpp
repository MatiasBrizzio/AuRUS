/////////////////
// OS Includes

//////////////
// Includes
#include "Clause.h"
#include "Debug.h"

/////////////
// Defines

///////////////////////////
// Static Initialization

//////////////////////////////////////////////////////////////////////////////////////////////////
// Public Methods

void Clause::vOutput(ostream& xOutputStream_) const
{
  for (int i=0; i<_iVariableCount; i++) {
    if (iIsNegated(i))
      xOutputStream_ << '-';
    xOutputStream_ << eConstrainedVariable(i)+1;
    xOutputStream_ << ' ';
  }
}

int Clause::iCompare(const Clause& xWithMe_) const
{
  // Compare, first by length, then by lexical variable order.
  // Assumes variables in the literal list are lexically sorted.
  // Ignores negated/non-negated status of the literal.
  if (iVariableCount() > xWithMe_.iVariableCount()) {
    return 1;
  }
  else if (iVariableCount() < xWithMe_.iVariableCount()) {
    return -1;
  }
  else {
    for (int i=0; i<iVariableCount(); i++) {
      if (eConstrainedVariable(i) >
	  xWithMe_.eConstrainedVariable(i)) {
	return 1;
      }
      else if (eConstrainedVariable(i) <
	       xWithMe_.eConstrainedVariable(i)) {
	return -1;
      }
    }
    return 0;
  }
}

boolean Clause::bIsEqual(const Clause& xWithMe_) const
{
  // Assumes variable lists are sorted.
  if (iVariableCount() != xWithMe_.iVariableCount()) {
    return 0;
  }
  else {
    for (int i=0; i<iVariableCount(); i++) {
      if (eConstrainedLiteral(i) != 
	  xWithMe_.eConstrainedLiteral(i)) {
	return 0;
      }
    }
    return 1;
  }
}

void Clause::vSortVariableList()
{
  // Simple bubble sort to sort the (non-perma) variables in lexical order.
  //return; // temp
  for (int i=0; i<_iPermaCount-1; i++) {
    for (int j=i+1; j<_iPermaCount; j++) {
      if (eConstrainedVariable(i) > eConstrainedVariable(j)) {
	LiteralID temp = _aConstrainedLiteral[i];
	_aConstrainedLiteral[i] = _aConstrainedLiteral[j];
	_aConstrainedLiteral[j] = temp;
      }
    }
  }
}
