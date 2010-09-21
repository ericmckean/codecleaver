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

import java.util.HashSet;

public final class CommandScanner {
  private final ErrorReporter errorReporter;
  private final Scanner scanner;
  private final State state;

  public CommandScanner(State state, String line, ErrorReporter errorReporter) {
    this.state = state;
    this.scanner = new Scanner(errorReporter, line);
    this.errorReporter = errorReporter;
  }

  public CommandType getCommand() {
    return getEnum(CommandType.class, "Unrecognized command.");
  }

  public boolean peekCommand() {
    return peekEnum(CommandType.class);
  }

  public IdentifierToken getFileName() {
    Token token = scanner.scanFileName();
    if (token.type == TokenType.Id) {
      return (IdentifierToken) token;
    }

    reportError(token, "Expected File Name.");
    return null;
  }

  public IdentifierToken getWord(String errorMessage) {
    Token token = scanner.nextToken();
    if (token.type == TokenType.Id) {
      return (IdentifierToken) token;
    }

    reportError(token, "Expected %s.", errorMessage);
    return null;
  }

  public void ensureEmpty() {
    Token token = scanner.peekToken();
    if (token.type != TokenType.EndOfLine) {
      reportError(token, "Expected end of line.");
    }
  }

  public boolean isAtEnd() {
    return scanner.peekToken().type == TokenType.EndOfLine;
  }

  public Iterable<Id> getValue() {
    Expression expression = new ExpressionParser(scanner, errorReporter).parseExpression();
    if (hadError()) {
      return null;
    }
    return ExpressionEvaluator.eval(errorReporter, state, expression);
  }

  public Expression getStatement() {
    Expression expression = new ExpressionParser(scanner, errorReporter).parseStatementOrExpression();
    if (hadError()) {
      return null;
    }
    return expression;
  }

  public HashSet<Id> getMutableSet() {
    IdentifierToken word = getMutableSetName();
    if (word == null) {
      return null;
    }

    return state.getSet(word.value);
  }

  public IdentifierToken getMutableSetName() {
    IdentifierToken word = getWord("set");
    if (word == null) {
      return null;
    }

    return getMutableSetName(word);
  }

  public String getHelpTopic() {
    return scanner.scanHelpTopic();
  }

  private <E extends Enum<E>> E getEnum(Class<E> eclass, String message) {
    IdentifierToken token = (IdentifierToken) eat(TokenType.Id);
    if (token == null) {
      return null;
    }

    try {
      return Enum.valueOf(eclass, token.value);
    } catch (IllegalArgumentException e) {
      reportError(token, message);
      return null;
    }
  }

  private <E extends Enum<E>> boolean peekEnum(Class<E> eclass) {
    Token token = peekToken();
    if (token == null || !(token instanceof IdentifierToken)) {
      return false;
    }

    try {
      Enum.valueOf(eclass, ((IdentifierToken) token).value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private Token eat(TokenType type) {
    Token token = nextToken();
    if (token.type != type) {
      reportError(token, "'%s' expected", type);
      return null;
    }
    return token;
  }

  private Token nextToken() {
    return scanner.nextToken();
  }

  private Token peekToken() {
    return scanner.peekToken();
  }

  private boolean hadError() {
    return errorReporter.hadError();
  }

  private void reportError(Token token, String format, Object... arguments) {
    reportError(token.startIndex, token.endIndex, format, arguments);
  }

  private void reportError(int startIndex, int endIndex, String format, Object... arguments) {
    errorReporter.reportError(startIndex, endIndex, String.format(format, arguments));
  }

  public Id getId() {
    IdentifierToken word = getWord("id");
    if (word == null) {
      return null;
    }
    String[] parts = word.value.split("\\.", -1);
    Id id;
    if (parts.length == 1) {
      String[] packageParts = word.value.split("\\/", -1);
      String lastPart = packageParts[packageParts.length - 1];
      if (lastPart.length() > 0 && Character.isUpperCase(lastPart.charAt(0))) {
        id = state.ids.getIdOfType(parts[0]);
      } else {
        id = state.ids.getIdOfPackage(parts[0]);
      }
    } else if (parts.length == 3) {
      TypeId type = state.ids.getIdOfType(parts[0]);
      if (parts[2].length() > 0 && parts[2].charAt(0) == '(') {
        id = state.ids.getIdOfMethod(type, parts[1], parts[2]);
      } else {
        id = state.ids.getIdOfField(type, parts[1], parts[2]);
      }
    } else {
      reportError(word, "'%s' is not a valid id", word.value);
      return null;
    }

    if (id == null) {
      reportError(word, "No existing id '%s'.", word);
    }
    return id;
  }

  private IdentifierToken getMutableSetName(IdentifierToken word) {
    if (!state.hasSet(word.value)) {
      reportError(word, "No set named '%s'.", word);
      return null;
    }
    if (state.isPredefinedSet(word.value)) {
      reportError(word, "Cannot modify predefined set '%s'.", word);
      return null;
    }

    return word;
  }
}
