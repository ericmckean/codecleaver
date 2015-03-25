Code Cleaver is a tool for understanding dependencies in large Java codebases.
Code Cleaver can help you understand how to split large Java jar files into
smaller jars. Code Cleaver can also help in porting a java library from one
java platform to another - from J2SE to J2ME for example.

Code Cleaver operates on 2 levels. First, Code Cleaver manipulates sets of Java
symbols. Symbols include packages, types, methods, and fields. Second, Code
Cleaver allows querying of the dependency graph between symbols. An entry in
the dependency graph is made whenever a symbol requires another symbol to run.
For example if method print() calls toString() then the print() symbol depends
on the toString() symbol. There are many ways that symbols can depend on each
other. Some of the dependencies include: Class symbols depend on the symbols
representing the class's base class and implemented interfaces. Field symbols
depend on the symbol representing the field's type. Method symbols depend on
all other methods called, fields referenced as well as the method's return
type, parameter types and throws types.

Code Cleaver has a command line interface. Executing Code Cleaver yields a
command prompt. The command prompt is found in the Console Window when running
Code Cleaver in Eclipse.

A typical work flow would include:
  1. Loading some java jar files into Code Cleaver with the open command.
  1. Building user defined sets of symbols using the create, assign, add and remove commands.
  1. Querying the dependency graph with the list command and set/graph query expressions.
  1. Modifying your user defined sets using the add, remove and move commands and query expressions.
  1. Iterating through steps 2, 3, and 4 until you have an answer to your dependency question.
  1. Saving your command history with the write command so that you can restore your state with the read command.
  1. Writing out set expressions with the writeList command.
  1. Removing parts of a jar and converting it to Java 1.6 bytecode format using the cleave command.

See the help [documentation](http://code.google.com/p/codecleaver/source/browse/trunk/docs/codecleaver_reference.txt) for more details.