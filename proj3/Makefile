# This is a very simple makefile for building the LISP wordParser
# project when using Java on stdlinux. Feel free to add any improvements:
# e.g. pattern rules, automatic tracking of dependencies, etc. There
# is a lot of info about "make" on the web.

# Java compiler
JAVAC = javac

# Java compiler flags
JAVAFLAGS = -g 

# Creating a .class file
COMPILE = $(JAVAC) $(JAVAFLAGS)

# One of these should be the "main" class listed in Runfile
CLASS_FILES = ParseException.class  WordParser.class ParseTree.class Main.class

# The first target is the one that is executed when you invoke
# "make". 

all: $(CLASS_FILES) 

clean:
	$(RM) *.class
# The line describing the action starts with <TAB>
%.class : %.java
	$(COMPILE) $<




