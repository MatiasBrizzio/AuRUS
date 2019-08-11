#ifndef Random_h
#define Random_h

///////////////////////////////////////////////////////////////////////////////
// Initializes random number generation

/////////////////
// OS Includes
#include <stdlib.h>

//////////////
// Includes
#include "LightweightTypes.h"
#include "RandGen.h"

/////////////
// Defines

////////////////////////
// Class Declarations
class Random {
public:
   void vInitRandom(int);
   double dRandom1() {return ((double)(xRandGen.genrand())/((double)2147483648UL));} // [0-1.0)
   double dRandom2() {return ((double)(xRandGen.genrand()+1)/((double)2147483648UL));}  // (0-1.0]
   double dRandom3() {return ((double)(xRandGen.genrand())/((double)2147483647UL));}  // [0-1.0]
   double dRandom4() {return ((double)(xRandGen.genrand()+1)/((double)2147483649UL));}  // (0-1.0)
   unsigned int iRandom(unsigned int iMax_) {return ((unsigned int)xRandGen.genrand())%iMax_;}
   unsigned long lRandom(unsigned long lMax_) {return ((unsigned long)xRandGen.genrand())%lMax_;}
   boolean bRandom() {return (boolean)(xRandGen.genrand()%2);}

private:
  RandGen xRandGen;
};

///////////////////////////////////////////////////////////////////////////////
// Class Definitions

///////////////////////////////////////////////////////////////////////////////
// Inlines

#endif // Random_h

