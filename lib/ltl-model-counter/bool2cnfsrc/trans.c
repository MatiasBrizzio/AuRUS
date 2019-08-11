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

/**************************************************
Translation into CNF

Files: node.h, scan.l, parse.y trans.c

Compilation:
%flex scan.l
%bison parse.y
%cc parse.tab.c trans.c -lfl

Usage:
@host% cat sample.exp
(a0 & !b0 & !c0)
& ((a1 = c0) & (b1 = a0) & (c1=b0))
& ((a2 = c1) & (b2 = a1) & (c2=b1))
& ((a3 = c2) & (b3 = a2) & (c3=b2))
& ((a3 = a0) & (b3 = b0) & (c3 = c0))
@host% a.out < sample.exp > smp.cnf 
***************************************************/

#include <stdio.h>
#include "node.h"

extern char *malloc();
extern int line_no;
extern int no_clauses;
extern int result_output;

int plus(Node *node);
int minus(Node *node);

void yyerror(char *msg)
{
  fprintf( stderr, "Error: %s at %d\n", msg, line_no);
  exit( -1);
} 

Node *make_node(Node_Type type, Node *left, Node *right, int index)
{
  Node *node = (Node *)malloc( sizeof( Node));
  node->type = type;
  node->left = left;
  node->right = right;
  node->index = index;

  return( node);
}

/*****************************************************/

int plus(Node *node)
{
  if( node == NULL)
    return(0);
  switch( node->type)
    {
    case VAR_Node:
      break;
    case AND_Node:
      if (result_output) 
	printf( "%d %d 0\n%d %d 0\n",
		-node->index, node->left->index,
		-node->index, node->right->index);
      else
	no_clauses += 2;

      plus( node->left);
      plus( node->right);
      break;
    case OR_Node:
      if (result_output) 
	printf( "%d %d %d 0\n",
		-node->index , node->left->index , node->right->index );
      else
	no_clauses++;

      plus( node->left);
      plus( node->right);
      break;
    case EQ_Node:
      if (result_output) 
	printf( "%d %d %d 0\n%d %d %d 0\n",
		-node->index , -node->left->index , node->right->index ,
		-node->index , node->left->index , -node->right->index );
      else
	no_clauses += 2;

      plus( node->left);
      plus( node->right);
      minus( node->left);
      minus( node->right);
      break;
    case IMP_Node:
      if (result_output) 
	printf( "%d %d %d 0\n",
		-node->index , -node->left->index , node->right->index );
      else
	no_clauses++;

      minus( node->left);
      plus( node->right);
      break;
    case NOT_Node:
      minus( node->left);
      break;
    default:
      yyerror( "Unknown Node type");
      break;
    }
  return(0);
}

int minus(Node *node)
{
  if( node == NULL)
    return(0);
  switch( node->type)
    {
    case VAR_Node:
      break;
    case AND_Node:
      if (result_output) 
	printf( "%d %d %d 0\n",
		-node->left->index, -node->right->index, node->index);
      else
	no_clauses++;

      minus( node->left);
      minus( node->right);
      break;
    case OR_Node:
      if (result_output) 
	printf( "%d %d %d 0\n%d %d %d 0\n%d %d %d 0\n",
		-node->left->index , node->right->index, node->index,
		node->left->index , -node->right->index, node->index,
		-node->left->index , -node->right->index, node->index);
      else
	no_clauses += 3;

      minus( node->left);
      minus( node->right);
      break;
    case EQ_Node:
      if (result_output) 
	printf( "%d %d %d 0\n%d %d %d 0\n",
		node->left->index , node->right->index , node->index ,
		-node->left->index , -node->right->index , node->index );
      else
	no_clauses += 2;

      minus( node->left);
      minus( node->right);
      plus( node->left);
      plus( node->right);
      break;
    case IMP_Node:
      if (result_output) 
	printf( "%d %d %d 0\n%d %d %d 0\n%d %d %d 0\n",
		-node->left->index , -node->right->index, node->index,
		node->left->index , -node->right->index, node->index,
		node->left->index , node->right->index, node->index);
      else
	no_clauses += 3;

      plus( node->left);
      minus( node->right);
      break;
    case NOT_Node:
      plus( node->left);
      break;
    default:
      yyerror( "Unknown Node type");
      break;
    }
  return(0);
}
