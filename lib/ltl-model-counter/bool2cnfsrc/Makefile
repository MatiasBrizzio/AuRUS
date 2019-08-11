# -*- mode: Makefile -*-

DESTDIR =
prefix = /usr/local
exec_prefix = $(prefix)
bindir = $(exec_prefix)/bin

CC = gcc
CPPFLAGS =
CFLAGS = -W -Wall -g -O2
LDFLAGS = 
LIBS =

LEX = flex
LFLAGS =
YACC = bison
YFLAGS = -y

INSTALL = install


OBJ = y.tab.o trans.o

.SUFFIXES:
.SUFFIXES: .c .l .y .o

all: lex.yy.c bool2cnf

install: $(BOOL2CNF)
	$(INSTALL) -m 755 $< $(DESTDIR)$(bindir)

clean:
	rm -f lex.yy.c y.tab.c $(OBJ) bool2cnf

lex.yy.c: scan.l
	$(LEX) $(LFLAGS) $<

y.tab.c: parse.y
	$(YACC) $(YFLAGS) $<

%.o: %.c
	$(CC) $(CPPFLAGS) $(CFLAGS) -c $< -o $@

bool2cnf: $(OBJ)
	$(CC) $(CFLAGS) $(OBJ) $(LDFLAGS) $(LIBS) -o $@
