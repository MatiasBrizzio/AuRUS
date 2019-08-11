#include "LightweightTypes.h"

void invoke_relsat(int iArgc_, const char* aArgv_[], ostream& xOutputStream_, int iTaskID_); // sat_solve.cpp

int main(int iArgc_, const char* aArgv_[])
{
  /*for (int i=0; i<1000000; i++) {
    invoke_relsat(iArgc_, aArgv_, cout, i);
    }*/
  invoke_relsat(iArgc_, aArgv_, cout, 0);
  return 0;
}
