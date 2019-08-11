#ifndef getopt_h
#define getopt_h
/*
  This is a hack of some ancient version of getopt.  I include it
  because many compilers don't support getopt(), e.g. Micosoft visual
  C++, and getopt() does not appear to be even close to thread safe.
*/

#include <string.h>
#include <istream>
using namespace std;

class GetOpt {
  private:

  void err(char* s, char c) {
      char errbuf[2];
      errbuf[0] = c; errbuf[1] = '\n';
      cerr << argv[0];
      cerr << s;
      cerr << errbuf;
  }

public:

  GetOpt(int argc_, const char** argv_, const char* opts_)
    : optind(1), argc(argc_), argv(argv_), opts(opts_), sp(1) {}

  int argc;
  const char** argv;
  const char* opts;

  int optind;
  const char* optarg;
  int sp;

  int getopt() {
    int     optopt;

    register int c;
    register const char *cp;

    if(sp == 1)
      if(optind >= argc ||
	 argv[optind][0] != '-' || argv[optind][1] == '\0')
	return(-1);
      else if(strcmp(argv[optind], "--") == 0) {
	optind++;
	return(-1);
      }
    optopt = c = argv[optind][sp];
    if(c == ':' || (cp=strchr(opts, c)) == 0) {
      err(": illegal option -- ", c);
      if(argv[optind][++sp] == '\0') {
	optind++;
	sp = 1;
                }
                return('?');
        }
        if(*++cp == ':') {
                if(argv[optind][sp+1] != '\0')
                        optarg = &argv[optind++][sp+1];
                else if(++optind >= argc) {
                        err(": option requires an argument -- ", c);
                        sp = 1;
                        return('?');
                } else
                        optarg = argv[optind++];
                sp = 1;
        } else {
                if(argv[optind][++sp] == '\0') {
                        sp = 1;
                        optind++;
                }
                optarg = 0;
        }
        return(c);
}

};

#endif //getopt_h
