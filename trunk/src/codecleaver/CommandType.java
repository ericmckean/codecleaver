/* Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package codecleaver;


public enum CommandType implements HelpTopic {
  // console commands
  help("topic", 
      "Display list of commands."),
  exit("", 
      "Exit CodeCleaver."),
  history("", 
      "Display list of commands executed this session."),
  clearHistory(
      "",
      "Clears the command and output history. Does not change the open files, symbols or " 
        + "dependency graph."),
  read("file-name",
      "Read and execute a list of commands from a file. When reading commands from a file, the "
        + "write command is disabled."),
  write("file-name",
      "Write the list of commands executed this session to a file."),
  writeOutput("file-name",
      "Write the entire session output including commands and the output of those commands to a "
        + "file."),
  writeList("file-name set-expression", 
      "Write the contents of set-expression to file-name."),
  readList("set-name file-name",
      "Reads the contents of a file-name previously written with writeList and assign the result "
        + "to set-name."),
  cd("directory-name",
      "Change current directory."),
  pwd("",
      "Print current directory."),

  // change the symbol set
  open("jar-file-name",
      "Loads symbols and dependency graph for a Java jar file. Creates a new symbol set whose "
        + "name is 'jar-file' and contents includes all symbols defined in (but not referenced "
        + "from) the jar file."),
  openAssign("new-set-name jar-file-name",
      "Loads symbols and dependency graph for a jar file and assigns the loaded symbols to the "
        + "set-name. Equivalent to:",
      "\topen jar-file",
      "\tassign set-name jar-file"),
  openSymbols("jar-file-name",
      "Loads symbols for a Java jar file. Creates a new symbol set whose name is 'jar-file' and "
        + "contents includes all symbols defined in (but not referenced from) the jar file. Does "
        + "not load dependency information for the jar file."),
  close("jar-file-name",
      "Unloads symbols for a Java jar-file previously opened with open, openAssign or "
        + "openSymbols."),
  clear("",
      "Clears the current open files, symbols and dependency graph. Does not clear command "
        + "history or output history."),
  clearAll("",
      "Clears all state including open files, symbols, dependency graph, command history and "
        + "output history. Equivalent to:",
      "\tclear",
      "\tclearHistory"),

  // display methods
  listSets("",
      "Displays all symbol sets currently defined."),
  listOpenFiles("",
      "Displays a list of all files which have been open-ed."),
  listSymbolFiles("",
      "Displays a list of all files which have been openSymbol-ed."),
  list("set-expression",
      "Displays all symbols in the set expression."),
  to("set-expression-to set-expression-from",
      "Displays all symbols which depend on symbols in set-expression-to. The output is grouped by "
      + "minimum distance to a member in the 'to' set. Output stops when a member of "
      + "set-expression-from is displayed."),
  from("set-expression-from set-expression-to",
      "Displays all symbols which the set-expression-from symbols depend on. The output is grouped "
      + "by minimum distance to the 'from' set. Output stops when a member of set-expression-to "
      + "is displayed"),
  size("set-expression",
      "Displays the number of symbols in the set expression."),
  info("set-expression",
      "Displays detailed information on all symbols in a set."),

  // set mutation
  create("set-name",
      "Create a new empty set."),
  delete("set-name",
      "Delete a named set. Predefined sets cannot be deleted."),
  add("set-name set-expression",
      "Add contents of set-expression to set-name."),
  remove("set-name set-expression",
      "Remove contents of set-expression to set-name."),
  assign("set-name set-expression",
      "Replaces the contents of set-name with set-expression. Will create set-name if it does "
        + "not already exist."),
  move("from-set-name to-set-name set-expression",
      "Removes set-expression from the from-set-name. Adds contents of set-expression to "
        + "to-set-name."),

  // generate output
  cleave("input-jar-file-name output-jar-file-name set-expression",
      "Reads in input-jar-file, and writes output-jar-file. Output-jar-file contains all symbols "
        + "from input-far-file that are also contained in set-expression. The output-jar-file is "
        + "written in java 1.6 bytecode format regardless of the format of the input-jar-file.");

  private final String arguments;
  private final String[] helpText;
  
  private CommandType(String arguments, String... help) {
    this.arguments = arguments;
    this.helpText = help;
  }
  
  @Override public String[] getHelp() {
    return helpText;
  }
  
  @Override public String getArguments() {
    return arguments;
  }

  @Override public String getTopic() {
    return this.toString();
  }
}
