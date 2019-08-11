/////////////////
// OS Includes
#include <stdlib.h>

//////////////
// Includes
#include "Debug.h"

/////////////
// Defines
		    
///////////////////////////
// Static Initialization
Debug Debug::xDebugger;

//////////////////////////////////////////////////////////////////////////////////////////////////
// Public Methods

Debug::Debug()
{
#ifndef NDEBUG
  cerr << "c Debugging is ON." << endl;
#endif
  /*
#ifdef MEM_DEBUG
  cerr << "c Memory debugging is ON (output appears in memtrace.log after termination)." << endl;
  Memtrace::init();
#endif
*/
}

Debug::~Debug()
{
#ifdef MEM_DEBUG
  Memtrace::term();
#endif
}

/*static*/ void Debug::vErrorReport(char* aError_) 
{
  // Report an error and halt program execution

  cerr << "Fatal error: " << aError_ << endl;
  exit(1);
}

//////////////////////////////////////////////////////////////////////////////////////////////////
// Protected Methods

//////////////////////////////////////////////////////////////////////////////////////////////////
// Private Methods





