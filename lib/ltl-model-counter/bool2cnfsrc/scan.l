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

#include <stdlib.h>
#include <string.h>
#include <search.h>

int line_no = 1;
int variable_index(void);

%}

%%
[ \t]    { ; }
\n       { line_no ++;}
"->"     { return( IMP);}
[A-Za-z][A-Za-z0-9_]*   {
           yylval.var = variable_index();
           return( VAR);
         }
.        { return( yytext[ 0]); }
%%

/* symbol table for variable names, maps to the corresponding term number */
static int variable_table[MAX_No_Variables];
static int* info_ptr = variable_table;

extern int option_signature;

int variable_index(void)
{
  ENTRY item;
  ENTRY *found_item; /* Name to look for in table. */

  item.key = yytext;

  if ((found_item = hsearch(item, FIND)) != NULL)
    {
      return *((int*) found_item->data);
    }

  no_variables++; no_terms++;
  if ( no_variables >= MAX_No_Variables)
    {
      fprintf(stderr, "too many variables\n");
      exit( 1);
    }

  item.key = strdup(yytext);

  *info_ptr = no_terms;
  item.data = info_ptr;
  info_ptr++;
 
  (void) hsearch(item, ENTER);

  if (option_signature)
    {
      printf("c var %s %d\n", yytext, no_terms);
      /*printf("%d\n", no_terms);*/
    }

  return no_terms;
}
