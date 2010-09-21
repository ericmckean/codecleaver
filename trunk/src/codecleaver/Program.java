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

import codecleaver.iterable.Sequence;
import codecleaver.util.IntegerReference;
import codecleaver.util.Pair;

import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NOTE: Update documentation in HelpTopics whenever changes are made here.
 * 
 * TODO(peterhal): 
 * - create script for automatically generating help text
 * - uniqueify annotation infos 
 * - used annotations predefined set 
 * - add command parser 
 * - add prefix/regular expression support for symbols/files 
 * - use console rather than out/in 
 * - up arrow 
 * - command completion 
 * - shortest path reporting: trim distance groups 
 * - min-cut reporting : http://www.internetmathematics.org/volumes/1/4/Flake.pdf JUNG graph library
 * - command short names 
 * - table based commands rather than enum 
 * - cross-set dependency reporting (from, to) => >>to ^ from 
 * - special sets 
 *   - current, last
 */
public final class Program {

  private SessionType sessionType;
  private BufferedReader in;
  private PrintStream out;
  private PrintStream err;
  private ErrorReporter promptErrorReporter;
  private ErrorReporter currentErrorReporter;
  private boolean isDone;
  private ArrayList<String> history;
  private ArrayList<String> outputHistory;
  private State state;
  private String currentDirectory;

  private static final String prompt = "CodeCleaver > ";
  private static final int CONSOLE_WIDTH = 80;

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {

    Program p = new Program();
    p.initialize();
    p.mainLoop();
  }

  private Program() {
    clearAll();
  }

  private void clearAll() {
    clear();
    clearHistory();
  }

  private void mainLoop() throws IOException {
    while (!isDone) {
      displayPrompt();
      String line = in.readLine();
      if (line == null) {
        isDone = true;
      } else {
        logLine(line);
        executeLine(line, Writes.Enabled);
      }
    }
  }

  private void initialize() {
    sessionType = System.console() != null ? SessionType.Interactive : SessionType.Scripted;
    in = new BufferedReader(new InputStreamReader(System.in));
    out = System.out;
    err = System.err;
    promptErrorReporter =
        new ErrorReporter() {
          @Override public void reportMessage(
              int startIndex, int endIndex, String messageKind, String format, Object... arguments) {
            if (sessionType.enablePrompt) {
              char[] spaces = new char[startIndex];
              Arrays.fill(spaces, ' ');
              char[] carets = new char[endIndex - startIndex];
              Arrays.fill(carets, '^');
              err.println(String.format("%s%s", String.valueOf(spaces), String.valueOf(carets)));
              err.println(
                  String.format("(%d) %s: %s", startIndex, messageKind, 
                      String.format(format, arguments)));
            } else {
              err.println(String.format("%s: %s", messageKind, String.format(format, arguments)));
            }
          }
        };
    currentErrorReporter = promptErrorReporter;
  }

  private void logLine(String line) {
    history.add(line);
    if (this.sessionType.echoInput) {
      println(line);
    } else {
      outputHistory.add(line);
    }
  }
  
  private enum SessionType {
    Interactive(true, false), 
    Scripted(false, true),;
        
    private SessionType(boolean enablePrompt, boolean echoInput) {
      this.enablePrompt = enablePrompt;
      this.echoInput = echoInput;
    }
    
    public final boolean enablePrompt;
    public final boolean echoInput;
  }
  
  private enum Writes {
    Enabled(true),
    Disabled(false),;
    
    private Writes(boolean isEnabled) {
      this.isEnabled = isEnabled;
    }
    
    public final boolean isEnabled;
  }

  private void executeLine(String line, Writes writesType) {
    currentErrorReporter.clearError();
    CommandScanner scanner = new CommandScanner(state, line, currentErrorReporter);
    if (scanner.isAtEnd()) {
      return;
    }

    if (scanner.peekCommand()) {
      CommandType command = scanner.getCommand();
      if (command != null) {
        try {
          doCommand(command, scanner, writesType);
        } catch (Throwable exception) {
          // Catch everything here to give good error messages for bugs in the
          // program.
          reportError("Fatal Error '%s'", exception.getMessage());
          for (StackTraceElement stackFrame : exception.getStackTrace()) {
            reportError(stackFrame.toString());
          }
        }
      }
    } else {
      doStatement(scanner, writesType);
    }
  }

  private void doStatement(CommandScanner scanner, Writes disableWrites) {
    Expression statement = scanner.getStatement();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    if (!ensureLoaded()) {
      return;
    }
    if (statement.type != ExpressionType.Assignment) {
      Iterable<Id> values = ExpressionEvaluator.eval(currentErrorReporter, state, statement);
      if (values != null) {
        doPrintSortedStrings(values);
      }
    } else {
      AssignmentExpression assignment = transform((AssignmentExpression) statement);
      IdentifierToken setName = assignment.left.value;
      if (!validateMutableSetName(setName)) {
        return;
      }
      Iterable<Id> values = ExpressionEvaluator.eval(currentErrorReporter, state, assignment.right);
      if (values == null) {
        return;
      }

      state.assignSet(setName.value, Sequence.createSet(values));
    }
  }

  private AssignmentExpression transform(AssignmentExpression statement) {
    BinaryOperator op = statement.operator.underlyingOperator;
    if (op == null) {
      return statement;
    }

    return new AssignmentExpression(statement.left, AssignmentOperator.Assign,
        new BinaryExpression(statement.left, op, statement.right));
  }

  private boolean ensureLoaded() {
    try {
      Iterable<Pair<TypeId, String>> ignoredTypes = state.ensureInfos();
      if (ignoredTypes != null) {
        for (Pair<TypeId, String> ignoredType : ignoredTypes) {
          reportWarning("Ignoring duplicate definition of '%s' in file '%s'.", ignoredType.first, 
              ignoredType.second);
        }
      }
      return true;
    } catch (StateException e) {
      reportError("'%s' reading jar file '%s'.", e.exception, e.fileName);
      return false;
    }
  }

  private void doCommand(CommandType command, CommandScanner scanner, Writes writes) {
    switch (command) {
      case help:
        doHelp(scanner);
        break;
      case exit:
        doExit(scanner);
        break;
      case history:
        doHistory(scanner);
        break;
      case clearHistory:
        doClearHistory(scanner);
        break;
      case read:
        doRead(scanner);
        break;
      case write:
        if (writes.isEnabled) {
          doWrite(scanner);
        }
        break;
      case writeOutput:
        if (writes.isEnabled) {
          doWriteOutput(scanner);
        }
        break;
      case open:
        doOpen(scanner);
        break;
      case openAssign:
        doOpenAssign(scanner);
        break;
      case openSymbols:
        doOpenSymbols(scanner);
        break;
      case close:
        doClose(scanner);
        break;
      case listOpenFiles:
        doListOpenFiles(scanner);
        break;
      case listSymbolFiles:
        doListSymbolFiles(scanner);
        break;
      case clear:
        doClear(scanner);
        break;
      case clearAll:
        doClearAll(scanner);
        break;
      case pwd:
        doPwd(scanner);
        break;
      case cd:
        doCd(scanner);
        break;
      default:
        if (!ensureLoaded()) {
          return;
        }
        switch (command) {
          case listSets:
            doListSets(scanner);
            break;
          case list:
            doList(scanner);
            break;
          case size:
            doSize(scanner);
            break;
          case info:
            doInfo(scanner);
            break;
          case create:
            doCreate(scanner);
            break;
          case delete:
            doDelete(scanner);
            break;
          case add:
            doAdd(scanner);
            break;
          case remove:
            doRemove(scanner);
            break;
          case assign:
            doAssign(scanner);
            break;
          case move:
            doMove(scanner);
            break;
          case to:
            doTo(scanner);
            break;
          case from:
            doFrom(scanner);
            break;
          case writeList:
            doWriteList(scanner);
            break;
          case readList:
            doReadList(scanner);
            break;
          case cleave:
            doCleave(scanner);
            break;
          default:
            doUnimplementedCommand(command, scanner);
            break;
        }
    }
  }

  private void doListSymbolFiles(CommandScanner scanner) {
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    doPrintSortedStrings(state.getSymbolFiles());
  }

  private void doListOpenFiles(CommandScanner scanner) {
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    doPrintSortedStrings(state.getFiles());
  }

  private void doFrom(CommandScanner scanner) {
    Iterable<Id> from = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    ArrayList<ArrayList<Id>> distances = state.getDistancesFrom(from);
    doPrintDistances(distances);
  }

  private void doTo(CommandScanner scanner) {
    Iterable<Id> to = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    ArrayList<ArrayList<Id>> distances = state.getDistancesTo(to);
    doPrintDistances(distances);
  }

  private void doPrintDistances(ArrayList<ArrayList<Id>> distances) {
    int distance = 0;
    for (ArrayList<Id> ids : distances) {
      if (distance > 0) {
        println(String.format("Distance %s", distance));
        doPrintSortedStrings(ids);
      }
      distance++;
    }
  }

  private void doMove(CommandScanner scanner) {
    HashSet<Id> from = scanner.getMutableSet();
    HashSet<Id> to = scanner.getMutableSet();
    Iterable<Id> values = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    Sequence.removeAll(from, values);
    Sequence.addAll(to, values);
  }

  private void doAssign(CommandScanner scanner) {
    IdentifierToken setName = scanner.getWord("new set name");
    Iterable<Id> values = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError() || !validateMutableSetName(setName)) {
      return;
    }

    state.assignSet(setName.value, Sequence.createSet(values));
  }

  private boolean validateMutableSetName(IdentifierToken setName) {
    if (!this.state.isValidMutableSetName(setName.value)) {
      reportError(setName, "'%s' is not a valid set name", setName.value);
      return false;
    }
    return true;
  }

  private void doRemove(CommandScanner scanner) {
    HashSet<Id> set = scanner.getMutableSet();
    Iterable<Id> values = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    Sequence.removeAll(set, values);
  }

  private void doAdd(CommandScanner scanner) {
    HashSet<Id> set = scanner.getMutableSet();
    Iterable<Id> values = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    Sequence.addAll(set, values);
  }

  private void doDelete(CommandScanner scanner) {
    IdentifierToken setName = scanner.getMutableSetName();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    state.deleteSet(setName.value);
  }

  private void doCreate(CommandScanner scanner) {
    IdentifierToken setName = scanner.getWord("new set name");
    scanner.ensureEmpty();
    if (hadError() || !validateNewSetName(setName)) {
      return;
    }

    state.createSet(setName.value);
  }

  private boolean validateNewSetName(IdentifierToken setName) {
    if (!this.state.isValidNewSetName(setName.value)) {
      reportError(setName, "'%s' is not a valid new set name", setName.value);
      return false;
    }
    return true;
  }

  private void doClearAll(CommandScanner scanner) {
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    clearAll();
  }

  private void doPwd(CommandScanner scanner) {
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    pwd();
  }

  private void pwd() {
    println(currentDirectory == null ? "" : currentDirectory);
  }

  private void doCd(CommandScanner scanner) {
    IdentifierToken directoryName = scanner.getFileName();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    currentDirectory = directoryName.value;
    if (currentDirectory.length() == 0) {
      currentDirectory = null;
    }
  }

  private String adjustFileName(IdentifierToken fileName) {
    return adjustFileName(fileName.value);
  }

  private String adjustFileName(String fileName) {
    File f = new File(currentDirectory, fileName);
    return f.getPath();
  }

  private void doClear(CommandScanner scanner) {
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    clear();
  }

  private void clear() {
    state = new State();
  }

  private void doListSets(CommandScanner scanner) {
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    doPrintSortedStrings(state.getSetNames());
  }

  private void doList(CommandScanner scanner) {
    Iterable<Id> set = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    doPrintSortedStrings(set);
  }

  private void doSize(CommandScanner scanner) {
    Iterable<Id> set = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    println(Iterables.size(set));
  }

  private void doInfo(CommandScanner scanner) {
    Iterable<Id> set = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    boolean first = true;
    for (Id id : set) {
      if (!first) {
        println("");
      } else {
        first = false;
      }
      printId(id);
    }
  }
  
  private static String objectToString(Object value) {
    if (value == null) {
      return "null";
    }
    if (value.getClass().isArray()) {
      boolean first = true;
      String result = "{";
      Object[] values = (Object[]) value; // UNDONE: primitive arrays?
      for (Object object : values) {
        if (first) {
          first = false;
          result += " ";
        } else {
          result += ", ";
        }
        result += objectToString(object);
      }
      if (!first) {
        result += " ";
      }
      result += "}";
      return result;
    }
    
    return value.toString();
  }

  private void printId(Id id) {
    if (state.getInfos().hasInfo(id)) {
      printInfo(state.getInfos().getInfo(id));
    } else {
      println("Name: %s", id);
    }
  }

  private void printInfo(Info info) {
    println("Name: %s", info.id);
    println("Type: %s", info.getType());
    println("Access: %X", info.access);
    println("Parent: %s", info.getParent());
    if (info instanceof TypeInfo) {
      TypeInfo type = (TypeInfo) info;
      println("Super: %s", type.superId);
      for (TypeId iface : type.interfaces) {
        println("Interface: %s", iface);
      }
      println("File: %s", type.file);
    }
    
    int index = 0;
    for (AnnotationInfo annotation : info.annotations.values()) {
      index++;
      println("  Annotation %d. Type: %s", index, annotation.type);
      for (String name : annotation.elements.keySet()) {
        println("    %s: %s", name, objectToString(annotation.elements.get(name)));
      }
    }
  }

  private void doPrintSortedStrings(Iterable<? extends Object> values) {
    Object[] stringsArray = Sequence.sortStrings(values);
    for (Object value : stringsArray) {
      println(value);
    }
  }

  private void doClose(CommandScanner scanner) {
    IdentifierToken fileName = scanner.getFileName();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    closeFileOrDirectory(fileName);
  }

  private boolean closeFileOrDirectory(IdentifierToken fileName) {
    return closeFiles(fileName, jarsOfFileOrDirectory(adjustFileName(fileName)));
  }

  private boolean closeFiles(Token location, Iterable<String> files) {
    for (String fileName : files) {
      if (!closeFile(location, fileName)) {
        return false;
      }
    }
    return true;
  }

  private boolean closeFile(Token location, String fileName) {
    if (state.containsSymbolFile(fileName)) {
      state.removeSymbolsFile(fileName);
      state.removeDefinitionsOfFile(fileName);
      return true;
    }
    // TODO(peterhal): report error
    return false;
  }

  private void doOpen(CommandScanner scanner) {
    IdentifierToken fileName = scanner.getFileName();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    openFileOrDirectory(fileName);
  }

  private boolean openFileOrDirectory(IdentifierToken fileName) {
    return openFiles(fileName, jarsOfFileOrDirectory(adjustFileName(fileName)));
  }

  private Iterable<String> jarsOfFileOrDirectory(String fileOrDirectoryName) {
    if (new File(fileOrDirectoryName).isDirectory()) {
      return State.jarsOfDirectory(fileOrDirectoryName);
    }
    return Sequence.singleton(fileOrDirectoryName);
  }

  private boolean openFiles(Token location, Iterable<String> files) {
    for (String fileName : files) {
      if (!openFile(location, fileName)) {
        return false;
      }
    }
    return true;
  }

  private boolean openFile(Token location, String fileName) {
    if (openSymbolsFromFile(location, fileName)) {
      state.addDefinitionsOfFile(fileName);
      return true;
    }
    return false;
  }

  private void doOpenAssign(CommandScanner scanner) {
    IdentifierToken setName = scanner.getWord("new set name");
    IdentifierToken fileOrDirectoryName = scanner.getFileName();
    scanner.ensureEmpty();
    if (hadError() || !validateMutableSetName(setName)) {
      return;
    }

    if (openFileOrDirectory(fileOrDirectoryName)) {
      HashSet<Id> values = new HashSet<Id>();
      for (String fileName : jarsOfFileOrDirectory(adjustFileName(fileOrDirectoryName))) {
        values.addAll(state.getSet(fileName));
      }
      state.assignSet(setName.value, values);
    }
  }

  private void doOpenSymbols(CommandScanner scanner) {
    IdentifierToken fileName = scanner.getFileName();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    openSymbolsFromFileOrDirectory(fileName);
  }

  private void openSymbolsFromFileOrDirectory(IdentifierToken fileOrDirectoryName) {
    openSymbolsFiles(
        fileOrDirectoryName, jarsOfFileOrDirectory(adjustFileName(fileOrDirectoryName)));
  }

  private void openSymbolsFiles(Token location, Iterable<String> fileNames) {
    for (String fileName : fileNames) {
      openSymbolsFromFile(location, fileName);
    }
  }

  private boolean openSymbolsFromFile(Token location, String fileName) {
    try {
      if (!state.containsSymbolFile(fileName)) {
        SymbolReader reader = new SymbolReader();
        IdClassReader.visitJarFile(state.ids, reader, fileName);
        state.addSymbolsFile(fileName, reader.getResult());
      }
      return true;
    } catch (StateException e) {
      reportError(location, "error '%s' reading jar file '%s'.", e.exception, e.fileName);
      return false;
    }
  }

  private void doHelp(CommandScanner scanner) {
    String topic = scanner.getHelpTopic();

    if ("all".equals(topic)) {
      for (HelpTopic help : HelpTopics.getTopics()) {
        printHelpTopic(help);
        println();
        println();
      }
    } else {
      HelpTopic help;
      if (topic == null) {
        help = HelpTopics.getTopicsTopic();
      } else if (!HelpTopics.hasTopic(topic)) {
        reportError("No help topic for '%s'.", topic);
        help = HelpTopics.getTopicsTopic();
      } else {
        help = HelpTopics.getTopic(topic);
      }
      
      printHelpTopic(help);
    }
  }

  private void printHelpTopic(HelpTopic help) {
    println(help.getTopic() + " " + help.getArguments());
    println();
    for (String line : help.getHelp()) {
      printWrappedLine(line);
    }
  }

  private void doWriteOutput(CommandScanner arguments) {
    IdentifierToken fileName = arguments.getFileName();
    arguments.ensureEmpty();
    if (hadError()) {
      return;
    }

    writeLinesToFile(fileName, outputHistory);
  }

  private void doWriteList(CommandScanner arguments) {
    IdentifierToken fileName = arguments.getFileName();
    Iterable<Id> set = arguments.getValue();
    arguments.ensureEmpty();
    if (hadError()) {
      return;
    }

    writeLinesToFile(fileName, Sequence.toSequence(Sequence.sortStrings(set)));
  }

  private void doWrite(CommandScanner arguments) {
    IdentifierToken fileName = arguments.getFileName();
    arguments.ensureEmpty();
    if (hadError()) {
      return;
    }

    writeLinesToFile(fileName, history);
  }

  private void writeLinesToFile(IdentifierToken fileName, Iterable<?> lines) {
    String fileNameValue = adjustFileName(fileName);
    try {
      PrintWriter writer = null;
      try {
        writer = new PrintWriter(new FileWriter(fileNameValue));
        for (Object line : lines) {
          writer.println(line.toString());
        }
      } finally {
        if (writer != null) {
          writer.close();
        }
      }
    } catch (IOException e) {
      reportError(fileName, String.format("Error '%s' writing to file:'%s'", e, fileNameValue));
    }
  }

  private void doReadList(CommandScanner arguments) {
    final IdentifierToken fileName = arguments.getFileName();
    IdentifierToken setName = arguments.getWord("new set name");
    arguments.ensureEmpty();
    if (hadError()) {
      return;
    }
    final String fileNameValue = adjustFileName(fileName);

    List<String> lines;
    try {
      lines = Utility.readLines(fileNameValue);
    } catch (IOException e) {
      reportError(
          fileName, String.format("Error '%s' reading commands from file:'%s'", e, fileNameValue));
      return;
    }

    ErrorReporter oldReporter = currentErrorReporter;
    boolean hadError = false;
    final IntegerReference lineNumber = new IntegerReference();
    HashSet<Id> values = new HashSet<Id>();
    try {
      currentErrorReporter =
          new ErrorReporter() {

            @Override public void reportMessage(
                int startIndex, int endIndex, String messageKind, String format, Object... arguments) {
              err.println(
                  String.format("%s(%d, %d) %s: %s", fileName, lineNumber.value, startIndex,
                      messageKind, String.format(format, arguments)));
            }

          };
      boolean firstLine = true;
      for (String line : lines) {
        lineNumber.value++;
        println(line);
        values.add(parseId(line));
        if (firstLine && currentErrorReporter.hadError()) {
          break;
        }
        firstLine = false;
      }
    } finally {
      hadError = currentErrorReporter.hadError(); 
      currentErrorReporter = oldReporter;
    }

    if (!hadError) {
      state.assignSet(setName.value, values);
    }
  }

  private void doCleave(CommandScanner scanner) {
    IdentifierToken inputName = scanner.getFileName();
    IdentifierToken outputName = scanner.getFileName();
    Iterable<Id> whiteList = scanner.getValue();
    scanner.ensureEmpty();
    if (hadError()) {
      return;
    }

    // ensure input file is opened, and we've loaded all symbols
    openFileOrDirectory(inputName);
    if (hadError() || !ensureLoaded()) {
      return;
    }

    // input file must contain the entire whitelist
    HashSet<Id> inputSet = state.getSet(inputName.value);
    for (Id missingId : state.getMinus(whiteList, inputSet)) {
      reportError(inputName, "File '%s' does not contain '%s'.", inputName, missingId);
    }
    if (hadError()) {
      return;
    }

    // must have inheritance hierarchy of all types
    // TODO(peterhal): should this be whiteList, < whiteList or <* whiteList ?
    Set<TypeId> reportedMissingIds = new HashSet<TypeId>();
    for (Id id : whiteList) {
      if (id.type == IdType.Type) {
        TypeId missing = state.getInfos().findFirstMissingTypeInInheritance((TypeId) id);
        if (missing != null && !reportedMissingIds.contains(missing)) {
          reportedMissingIds.add(missing);
          reportError(inputName, "Type '%s' requires type '%s' which is not defined.", id, missing);
        }
      }
    }
    if (hadError()) {
      return;
    }

    JarCleaver cleaver = new JarCleaver(state.getInfos());
    try {
      cleaver.cleave(adjustFileName(inputName), adjustFileName(outputName), whiteList);
    } catch (IOException exception) {
      // TODO(peterhal): improve error reporting
      reportError("IO Error '%s'", exception.getMessage());
    } catch (MissingInfoException exception) {
      reportError(inputName, "Missing definition for '%s'", exception.id);
    } catch (RuntimeException exception) {
      if (exception.getMessage().equals("JSR/RET are not supported with computeFrames option")) {
        reportError(inputName, 
            "'%s' uses JSR/RET instructions which cannot be automatically converted to java 1.6. " +
            "It was likely compiled with an old (pre 1.5) javac. " +
            "Try recompiling from source with a newer javac.", inputName);  
      }
    }
  }

  private Id parseId(String line) {
    CommandScanner scanner = new CommandScanner(state, line, currentErrorReporter);
    Id id = scanner.getId();
    scanner.ensureEmpty();
    return id;
  }

  private void doRead(CommandScanner arguments) {
    final IdentifierToken fileName = arguments.getFileName();
    arguments.ensureEmpty();
    if (hadError()) {
      return;
    }
    final String fileNameValue = adjustFileName(fileName);

    List<String> lines;
    try {
      lines = Utility.readLines(fileNameValue);
    } catch (IOException e) {
      reportError(
          fileName, String.format("Error '%s' reading commands from file:'%s'", e, fileNameValue));
      return;
    }

    final IntegerReference lineNumber = new IntegerReference();
    ErrorReporter oldReporter = currentErrorReporter;
    try {
      currentErrorReporter =
          new ErrorReporter() {

            @Override public void reportMessage(
                int startIndex, int endIndex, String messageKind, String format, Object... arguments) {
              err.println(
                  String.format("%s(%d, %d) %s: %s", fileName, lineNumber.value, startIndex,
                      messageKind, String.format(format, arguments)));
            }

          };
      boolean firstLine = true;
      for (String line : lines) {
        lineNumber.value++;
        println(line);
        executeLine(line, Writes.Disabled);
        if (firstLine && currentErrorReporter.hadError()) {
          break;
        }
        firstLine = false;
      }
    } finally {
      currentErrorReporter = oldReporter;
    }
  }

  private void doClearHistory(CommandScanner arguments) {
    arguments.ensureEmpty();
    if (hadError()) {
      return;
    }

    clearHistory();
  }

  private void clearHistory() {
    history = new ArrayList<String>();
    outputHistory = new ArrayList<String>();
  }

  private void doHistory(CommandScanner arguments) {
    arguments.ensureEmpty();
    if (hadError()) {
      return;
    }

    for (String line : history) {
      println(line);
    }
  }

  private void doExit(CommandScanner arguments) {
    arguments.ensureEmpty();
    if (hadError()) {
      return;
    }

    isDone = true;
    println("Bye...");
  }

  private void doUnimplementedCommand(CommandType command, CommandScanner scanner) {
    reportError("Unimplemented command '%s'.", command);
  }

  private boolean hadError() {
    return currentErrorReporter.hadError();
  }

  private void reportError(String format, Object... arguments) {
    reportError(0, 1, format, arguments);
  }

  private void reportWarning(String format, Object... arguments) {
    reportWarning(0, 1, format, arguments);
  }

  private void reportError(Token token, String format, Object... arguments) {
    reportError(token.startIndex, token.endIndex, format, arguments);
  }

  private void reportWarning(int startIndex, int endIndex, String format, Object... arguments) {
    currentErrorReporter.reportWarning(startIndex, endIndex, String.format(format, arguments));
  }

  private void reportError(int startIndex, int endIndex, String format, Object... arguments) {
    currentErrorReporter.reportError(startIndex, endIndex, String.format(format, arguments));
  }

  private void displayPrompt() {
    if (sessionType.enablePrompt) {
      out.println(prompt);
    }
  }
  
  private void println() {
    println("");
  }
  
  private void println(Object format, Object... arguments) {
    String s = String.valueOf(format);
    if (s == null) {
      s = "null";
    }
    
    String result = String.format(s, arguments);

    outputHistory.add(result);
    out.println(result);
  }

  private void printWrappedLine(String text) {
    // TODO: get console width rather than use constant 80 columns
    String indent = getIndent(text);
    int indentLength = indent.length();
    int printableWidth = CONSOLE_WIDTH - indentLength;
    text = text.trim();
    while (text.length() > 0) {
      int lineLength = Math.min(text.length(), printableWidth);
      if (lineLength < text.length()) {
        int wordBreakIndex = lineLength - 1;
        while (wordBreakIndex > 0 && !Character.isWhitespace(text.charAt(wordBreakIndex))) {
          wordBreakIndex --;
        }
        if (wordBreakIndex > 0) {
          lineLength = wordBreakIndex;
        }
      }
      String line = text.substring(0, lineLength);
      println(indent + line);
      text = text.substring(lineLength).trim();
    }
  }

  private String getIndent(String line) {
    String result = "";
    int index = 0;
    while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
      switch (line.charAt(index)) {
      case ' ':
        result += ' ';
        break;
      case '\t':
        result += "        ";
        break;
      default:
        throw new RuntimeException("Unrecognized whitespace in help topic indent.");
      }
      index ++;
    }
    return result;
  }
}
