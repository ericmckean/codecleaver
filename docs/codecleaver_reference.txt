help all
codecleaver 

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


workflow 

Code Cleaver has a command line interface. Executing Code Cleaver yields a
command prompt. The command prompt is found in the Console Window when running
Code Cleaver in Eclipse.
A typical work flow would include:
        1 - Loading some java jar files into Code Cleaver with the open command.
        2 - Building user defined sets of symbols using the create, assign, add
        and remove commands.
        3 - Querying the dependency graph with the list command and set/graph
        query expressions.
        4 - Modifying your user defined sets using the add, remove and move
        commands and query expressions.
        5 - Iterating through steps 2, 3, and 4 until you have an answer to
        your dependency question.
        6 - Saving your command history with the write command so that you can
        restore your state with the read command.
        7 - Writing out set expressions with the writeList command.
        8 - Removing parts of a jar and converting it to Java 1.6 bytecode
        format using the cleave command.


commands 

Each line of input is treated as a command.
        command:
                #  comment
                named-command
                set-expression
                assignment-statement
A set-expression command is equivalent to the list set-expression named command.


named-commands 

help
exit
history
clearHistory
read
write
writeOutput
writeList
readList
cd
pwd
open
openAssign
openSymbols
close
clear
clearAll
listSets
listOpenFiles
listSymbolFiles
list
to
from
size
info
create
delete
add
remove
assign
move
cleave


help topic

Display list of commands.


exit 

Exit CodeCleaver.


history 

Display list of commands executed this session.


clearHistory 

Clears the command and output history. Does not change the open files, symbols
or dependency graph.


read file-name

Read and execute a list of commands from a file. When reading commands from a
file, the write command is disabled.


write file-name

Write the list of commands executed this session to a file.


writeOutput file-name

Write the entire session output including commands and the output of those
commands to a file.


writeList file-name set-expression

Write the contents of set-expression to file-name.


readList set-name file-name

Reads the contents of a file-name previously written with writeList and assign
the result to set-name.


cd directory-name

Change current directory.


pwd 

Print current directory.


open jar-file-name

Loads symbols and dependency graph for a Java jar file. Creates a new symbol
set whose name is 'jar-file' and contents includes all symbols defined in (but
not referenced from) the jar file.


openAssign new-set-name jar-file-name

Loads symbols and dependency graph for a jar file and assigns the loaded
symbols to the set-name. Equivalent to:
        open jar-file
        assign set-name jar-file


openSymbols jar-file-name

Loads symbols for a Java jar file. Creates a new symbol set whose name is
'jar-file' and contents includes all symbols defined in (but not referenced
from) the jar file. Does not load dependency information for the jar file.


close jar-file-name

Unloads symbols for a Java jar-file previously opened with open, openAssign or
openSymbols.


clear 

Clears the current open files, symbols and dependency graph. Does not clear
command history or output history.


clearAll 

Clears all state including open files, symbols, dependency graph, command
history and output history. Equivalent to:
        clear
        clearHistory


listSets 

Displays all symbol sets currently defined.


listOpenFiles 

Displays a list of all files which have been open-ed.


listSymbolFiles 

Displays a list of all files which have been openSymbol-ed.


list set-expression

Displays all symbols in the set expression.


to set-expression-to set-expression-from

Displays all symbols which depend on symbols in set-expression-to. The output
is grouped by minimum distance to a member in the 'to' set. Output stops when a
member of set-expression-from is displayed.


from set-expression-from set-expression-to

Displays all symbols which the set-expression-from symbols depend on. The
output is grouped by minimum distance to the 'from' set. Output stops when a
member of set-expression-to is displayed


size set-expression

Displays the number of symbols in the set expression.


info set-expression

Displays detailed information on all symbols in a set.


create set-name

Create a new empty set.


delete set-name

Delete a named set. Predefined sets cannot be deleted.


add set-name set-expression

Add contents of set-expression to set-name.


remove set-name set-expression

Remove contents of set-expression to set-name.


assign set-name set-expression

Replaces the contents of set-name with set-expression. Will create set-name if
it does not already exist.


move from-set-name to-set-name set-expression

Removes set-expression from the from-set-name. Adds contents of set-expression
to to-set-name.


cleave input-jar-file-name output-jar-file-name set-expression

Reads in input-jar-file, and writes output-jar-file. Output-jar-file contains
all symbols from input-far-file that are also contained in set-expression. The
output-jar-file is written in java 1.6 bytecode format regardless of the format
of the input-jar-file.


predefined-sets 

All
Empty
Packages
Types
Methods
Fields
Definitions
NativeMethods
AbstractMethods
Interfaces
Classes
Publics
Privates
Protecteds
PackagePrivates
Statics
Finals
SynchronizedMethods
VolatileFields
VarArgsMethods
TransientFields
AbstractClasses
Stricts
Synthetics
Annotations
Externals
Overrides
UnextensiblePackages
Initializers
StaticInitializers


All 

Every Symbol known to the system.


Empty 

The empty set.


Packages 

All package symbols currently loaded.


Types 

All type symbols currently loaded.


Methods 

All method symbols currently loaded.


Fields 

All field symbols currently loaded.


Definitions 

Symbols which have been added to the dependency graph.


NativeMethods 

All methods which are implemented by native (C++) code in the dependency graph.


AbstractMethods 

All methods which are abstract. Includes both abstract methods in classes as
well as abstract methods in interfaces.
Note: Only methods loaded using open are included in AbstractMethods set.
Symbols loaded with openSymbols are never added to NativeMethods.


Interfaces 

All interface types.


Classes 

All types which are not interfaces.
Note: class/interface is only set correctly for symbols loaded using open. All
types loaded with openSymbols are considered as classes.


Publics 

All types, fields, and methods which are public.


Privates 

All types, fields, and methods which are private.


Protecteds 

All types, fields, and methods which are protected.


PackagePrivates 

All types, fields, and methods which are package private.


Statics 

All static fields and methods.


Finals 

All types, fields and methods which are final.


SynchronizedMethods 

Methods which are synchronized.


VolatileFields 

Fields which are read and written by the VM in exactly the order described in
source. Prevents VM optimizations which may be unsafe during multi-threaded
access.


VarArgsMethods 

Methods whose last argument is a varargs array and may be called with a
variable number of arguments.


TransientFields 

Fields that are not part of the persistent state of the object.


AbstractClasses 

Abstract classes. Does not include interfaces.


Stricts 

All classes and methods which perform strict precision floating point
operations. By default floating point operations and intermediate values may
have more precision than the minimum required.


Synthetics 

Types, fields and methods which are synthetic. Synthetic symbols are symbols
which are not found in source. Typically means they are compiler generated for
anonymous inner classes for example.


Annotations 

Types which are annotations.


Externals 

All packages, types, fields, and methods which are visible outside their
containing jar. Package private members in the packages in the user defined set
UnextensiblePackages will be considered non-External.


Overrides 

All methods which override a method from a base class or implement an interface
method.


UnextensiblePackages 

A mutable set used to determine which package privates are visible externally.


Initializers 

All methods which are instance initializers. Aka constructors.


StaticInitializers 

All methods which are static initializers.


symbols 

Symbols include packages, types, methods and fields.
Packages and types use the '/' character as separator.
Example packages are 'java/lang', or 'com/google/common'.
Example types are 'java/lang/Object' or 'java/io/File'.
Member names use '.' as separator and have member signature appended.
Example member names:
java/lang/Object.<init>.()V
java/lang/Object.clone.()Ljava/lang/Object;
java/lang/Object.equals.(Ljava/lang/Object;)Z
java/lang/Object.finalize.()V
java/lang/Object.getClass.()Ljava/lang/Class;


set-expressions 

Sets of symbols are specified using set expressions.
Syntax:
        set-expression:
            binary-expression


binary-expression 

Syntax:
        binary-expression:
            binary-expression + unary-expression
            binary-expression - unary-expression
            binary-expression ^ unary-expression
            unary-expression
Binary operators are:
            +  union
            -  difference
            ^  intersection


unary-expression 

Unary Expressions
Syntax:
        unary-expression:
            <  unary-expression
            >  unary-expression
            <*  unary-expression
            >*  unary-expression
            [  unary-expression
            ]  unary-expression
            [*  unary-expression
            ]*  unary-expression
            !  unary-expression
            !*  unary-expression
            primary-expression
Unary operators are:
        Dependency Graph Query Operators:
            <  direct callees
            >  direct callers
            <*  transitive closure of direct callees
            >*  transitive closure of direct callers
        Inheritance Graph Query Operators:
            [  direct overrides/inherits from
            ]  directly overridden by/super class of
            [*  transitive overrides/inherits from
            ]*  transitive overridden by/super class of
        Expand Operators:
            !  direct children of all elements of the set
            !*  all descendants of all elements of the set, including the
            original set


primary-expression 

Primary Expressions
Syntax:
        primary-expression:
            (  expression  )
            annotation-expression
            predefined-set
            symbol
            jar-file-name
Primary expressions are either a predefined set, a symbol or a jar file.
Primary expressions can be quoted in double quotes. There are no escapes within
quoted expressions.
When types, packages and jar file are used in a set expression the resulting
expression includes all of their contained symbols: methods, fields and nested
types.


annotation-expression 

Annotation Expressions
Syntax:
        annotation-expression:
            @  type-symbol
            @  type-symbol  (  )
            @  type-symbol  (  element-value  )
            @  type-symbol  (  element-name  =  element-value
                    [  ,  element-name   =  element-value ] *  )
Annotation expressions match any symbol with a matching annotation. The first
two forms match any symbol with an annotation of the 'type-symbol' type
regardless of annotation elements. The 'element-value' form is shorthand for
@type-symbol(value=element-value). The form with element name value pairs
matches symbols with an annotation of 'type-symbol' type, that have an element
of name 'element-name' whose value, when converted to a string, is equal to
'element-value'. If the type of the element named by 'element-name' is an array
type then the pair matches if the element value contains 'element-value'.
For Example:
        @java/lang/annotation/Target(METHOD)
Matches any annotation type which can target a method.


assignment-statement 

Assignment Statement
Syntax:
        assignment-statement:
            set-name  =  set-expression
            set-name  +=  set-expression
            set-name  -=  set-expression
            set-name  ^=  set-expression
The simple assignment statement:
        set-name  =  set-expression
is equivalent to:
        assign set-name set-expression
A compound assignment:
        set-name op= set-expression
is equivalent to:
        assign set-name ( set-name op set-expression ).


topics 

codecleaver
workflow
commands
named-commands
help
exit
history
clearHistory
read
write
writeOutput
writeList
readList
cd
pwd
open
openAssign
openSymbols
close
clear
clearAll
listSets
listOpenFiles
listSymbolFiles
list
to
from
size
info
create
delete
add
remove
assign
move
cleave
predefined-sets
All
Empty
Packages
Types
Methods
Fields
Definitions
NativeMethods
AbstractMethods
Interfaces
Classes
Publics
Privates
Protecteds
PackagePrivates
Statics
Finals
SynchronizedMethods
VolatileFields
VarArgsMethods
TransientFields
AbstractClasses
Stricts
Synthetics
Annotations
Externals
Overrides
UnextensiblePackages
Initializers
StaticInitializers
symbols
set-expressions
binary-expression
unary-expression
primary-expression
annotation-expression
assignment-statement


