%{

/*
 * Copyright 2011 Tatsuhiro Tsuchiya. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY Tatsuhiro Tsuchiya ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Tatsuhiro Tsuchiya OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Tatsuhiro Tsuchiya.
 * 
 */ 

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <unistd.h>
#include <search.h>
#include "node.h"

int no_variables;
int no_terms;

int no_clauses;
int result_output;
int option_signature;
char** arguments;
int no_arguments;
extern int plus(Node *);
extern int yyparse();
extern FILE* yyin;

  %}

%union {
  int var; /* variable index */
  Node *node;
}

%token <var> VAR
%token IMP

%type <node> program exp

%left  '=' 
%left IMP
%left  '|'
%left '&'
%right '!'

%start program
%%
program
: exp { result_output = 0; 
     plus( $1);
     printf("p cnf %d %d\n", no_terms, ++no_clauses);

     printf("%d 0\n", $1->index);
     result_output = 1; 
     plus( $1);
}
;

exp
: VAR           { $$ = make_node( VAR_Node, NULL, NULL, $1); }
| '(' exp ')'   { $$ = $2; }
| exp '&' exp   { no_terms++; $$ = make_node( '&', $1, $3, no_terms); }
| exp '|' exp   { no_terms++; $$ = make_node( '|', $1, $3, no_terms); }
| exp '=' exp   { no_terms++; $$ = make_node( '=', $1, $3, no_terms); }
| exp IMP exp   { no_terms++; $$ = make_node( IMP_Node, $1, $3, no_terms); }
| '!' exp         { $$ = make_node( '!', $2, NULL, -$2->index); }
;
%%


void usage(void)
{
  printf(
	 "Usage: bool2cnf [option] [file ...]\n"
	 "Translates formulas between BOOL and DIMACS CNF formats.\n"
	 "\nOptions:\n"
	 "   -s   print signature\n"
	 "   -v   display version information\n"
	 "   -h   display this information\n"
	 );
}

#define MAX_No_Variables 10000

#include "lex.yy.c"

int yywrap(void)
{
  if (no_arguments <= 1) /* no files left to process */
    {
      return 1;
    }

  char* filename = *++arguments;
  no_arguments--;

  if (yyin != stdin) fclose(yyin);

  if (strncmp(filename, "-", 1) != 0) /* open file */
    {
      yyin = fopen (filename, "r");

      if ((yyin = fopen (filename, "r")) == NULL)
	{
	  fprintf(stderr, "bool2cnf: %s: %s\n", filename, strerror(errno));
	  return 1;
	}
    }
  else /* reopen stdin */
    {
      yyin = freopen(NULL, "r", stdin);
    }

  return 0;
}


int main(int argc, char* argv[])
{
  int ch;

  option_signature = 0;

  while ((ch = getopt(argc, argv, "shv")) != -1)
    {
      switch (ch)
	{
        case 's':
	  option_signature = 1;
	  break;
        case 'v':
	  printf("bool2cnf 1.1\nCopyright (C) 2011 Tatsuhiro Tsuchiya\n");
	  exit(0);
	  break;
        case 'h':
	  usage();
	  exit(0);
	  break;
        case '?':
        default:
	  usage();
	  exit(1);
        }
    }
  argc -= optind;
  argv += optind;

  yyin = stdin;
  no_arguments = argc;
  arguments = argv;

  /* check for input file(s) different from '-' */
  if (argc > 0 && strncmp(*argv, "-", 1))
    {
      if ((yyin = fopen (*argv, "r")) == NULL)
	{
	  fprintf(stderr, "bool2cnf: %s: %s\n", *argv, strerror(errno));
	  return 1;
	}
    }

  no_variables = no_terms = no_clauses = 0;

  /* Create symbol table */
  if (hcreate(MAX_No_Variables) == 0)
    {
      perror("Cannot create symbol table");
      exit(1);
    }

  (void) yyparse();

  fclose(yyin);

  hdestroy();

  return(0);
}
