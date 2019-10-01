#!/bin/bash
./modelcount.sh -vars="a,b,c" "-ltl=G (a -> b)" "-ref=F((a & !b))" "-ref=(F(!a) | F(b))" "-ref=G((!a & b))" "-ref=G(!a)" "-ref=G(a)" "-ref=G((!a | c))" "-ref=G((b | c))" "-ref=(G(a & !b))" "-ref=X((!a | b))" "-ref=G((a | b))" "-out=result/simple/simple-prefix.out" -prefix -k=8
