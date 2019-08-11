#ifndef BigNum_h
#define BigNum_h

//////////////////////////////////////////////////////////////////////////////////////////////////
// BigNum.h: Encapsulates bignum functionality. This implementation uses the Gnu "gmp" package.

/////////////////
// OS Includes
#include "my_gmp.h"

//////////////
// Includes
#include "LightweightTypes.h"

/////////////
// Defines

////////////////////////
// Class Declarations

//////////////////////////////////////////////////////////////////////////////////////////////////
// Class Definitions

#include "my_gmp.h"

extern "C" {
__declspec(dllexport) void  mpz_clear_dll(mpz_ptr xInitMe_);

__declspec(dllexport) void  mpz_init_dll(mpz_ptr xInitMe_);

__declspec(dllexport) void  mpz_init_set_si_dll(mpz_ptr xInitMe_, long int iWithMe_);

__declspec(dllexport) void  mpz_init_set_dll(mpz_ptr xInitMe_, mpz_srcptr xWithMe_);

__declspec(dllexport) void  mpz_add_dll(mpz_ptr xAddMe_, mpz_srcptr xToMe_);

__declspec(dllexport) void  mpz_mul_dll(mpz_ptr xMulMe_, mpz_srcptr xToMe_);

__declspec(dllexport) void  mpz_mul_ui_dll(mpz_ptr xMulMe_, unsigned long iToMe_);

__declspec(dllexport) int  mpz_cmp_dll(mpz_ptr x1_, mpz_srcptr x2_);
  
__declspec(dllexport) int  mpz_sizeinbase_dll(mpz_ptr x_, int base_);

__declspec(dllexport) void  mpz_get_str_dll(char* buf, int base_, mpz_srcptr x_);

__declspec(dllexport) void  mpz_set_dll(mpz_ptr xSetMe_, mpz_srcptr xTo_);

__declspec(dllexport) void  mpz_set_si_dll(mpz_ptr xSetMe_, long int iTo_);
}

class BigNum {
public:
  BigNum() { mpz_init_dll(_xCount); }
  BigNum(long int iInitWithMe_) { mpz_init_set_si_dll(_xCount, iInitWithMe_); }
  BigNum(const BigNum& xBigNum_) { mpz_init_set_dll(_xCount, xBigNum_._xCount); }
  ~BigNum() { mpz_clear_dll(_xCount); }
  
  void operator +=(const BigNum& xMe_) { mpz_add_dll(_xCount, xMe_._xCount); }
  void operator *=(const BigNum& xMe_) { mpz_mul_dll(_xCount, xMe_._xCount); }
  void operator *=(unsigned long iCount_) { mpz_mul_ui_dll(_xCount, iCount_); }

  boolean operator >(const BigNum& xMe_) { return (mpz_cmp_dll(_xCount, xMe_._xCount)) == 1 ? 1 : 0; }

  void vSet(long int iTo_) { mpz_set_si_dll(_xCount, iTo_); }
  void vSet(const BigNum& xMe_) { mpz_set_dll(_xCount, xMe_._xCount); }

  char* aToString() {
    // Caller responsible for deleting the returned string.
    int buf_size = mpz_sizeinbase_dll(_xCount, 10) + 2;
    char* buffer = new char[buf_size];
    mpz_get_str_dll(buffer, 10, _xCount);
    return buffer;
  }

private:
  mpz_t _xCount;
};

//////////////////////////////////////////////////////////////////////////////////////////////////
// Inlines

#endif //BigNum_h
