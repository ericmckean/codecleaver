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

import codecleaver.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * Parses string representing set expressions into Expressions. Does not do semantic analysis.
 */
public final class ExpressionParser {

  private final Scanner scanner;
  private final ErrorReporter errorReporter;

  /**
   * Create an expression parser which gets tokens from scanner and reports parse errors on
   * errorReporter.
   */
  public ExpressionParser(Scanner scanner, ErrorReporter errorReporter) {
    this.scanner = scanner;
    this.errorReporter = errorReporter;
  }

  /**
   * Parse a non-statement expression. May returns null if an error was reported.
   */
  public Expression parseExpression() {
    return parseBinaryExpression();
  }

  /**
   * Parse a statement or an expression. May returns null if an error was reported.
   */
  public Expression parseStatementOrExpression() {
    if (peekType(0) == TokenType.Id && peekAssignmentOperator(1)) {
      PrimaryExpression left = parseIdentifierExpression();
      return new AssignmentExpression(left, parseAssignmentOperator(), parseExpression());
    } else {
      return parseExpression();
    }
  }

  private Expression parseBinaryExpression() {
    Expression result = parseUnaryExpression();
    while (peekBinaryOperator()) {
      result = parseBinarySuffix(result);
    }
    return result;
  }

  /**
   * Is the token index positions ahead an assignment operator token.
   */
  private boolean peekAssignmentOperator(int index) {
    return peekType(index).isAssignmentOperator();
  }

  /**
   * Is the next token a binary operator token.
   */
  private boolean peekBinaryOperator() {
    return peekType().isBinaryOperator();
  }

  private AssignmentOperator parseAssignmentOperator() {
    Token token = nextToken();
    return token.type.assignmentOperator;
  }

  private BinaryOperator parseBinaryOperator() {
    Token token = nextToken();
    return token.type.binaryOperator;
  }

  private Expression parseBinarySuffix(Expression left) {
    return new BinaryExpression(left, parseBinaryOperator(), parseUnaryExpression());
  }

  /**
   * Is the next token a unary operator token.
   */
  private boolean peekUnaryOperator() {
    return peekType().isUnaryOperator();
  }

  private UnaryOperator parseUnaryOperator() {
    Token token = nextToken();
    return token.type.unaryOperator;
  }

  private Expression parseUnaryExpression() {

    if (peekUnaryOperator()) {
      return new UnaryExpression(parseUnaryOperator(), parseUnaryExpression());
    }
    return parsePrimaryExpression();
  }

  private Expression parsePrimaryExpression() {
    if (peekType() == TokenType.OpenParen) {
      return parseParenExpression();
    }
    if (peekType() == TokenType.At) {
      return parseAnnotationExpression();
    }
    return parseIdentifierExpression();
  }

  private Expression parseAnnotationExpression() {
    eat(TokenType.At);
    IdentifierToken typeName = nextTypeName();
    if (peekType() == TokenType.OpenParen) {
      eat(TokenType.OpenParen);
      if (peekType() == TokenType.CloseParen) {
        eat(TokenType.CloseParen);
        return new AnnotationExpression(typeName);
      }
      IdentifierToken element = eatId();
      if (peekType() != TokenType.Assign) {
        eat(TokenType.CloseParen);
        return new AnnotationExpression(typeName, element);
      }
      eat(TokenType.Assign);
      IdentifierToken value = eatId();

      ImmutableList.Builder<Pair<IdentifierToken, IdentifierToken>> elements =
          new ImmutableList.Builder<Pair<IdentifierToken, IdentifierToken>>();
      elements.add(new Pair<IdentifierToken, IdentifierToken>(element, value));
      while (peekType() == TokenType.Comma) {
        eat(TokenType.Comma);
        element = eatId();
        eat(TokenType.Assign);
        value = eatId();
        elements.add(new Pair<IdentifierToken, IdentifierToken>(element, value));
      }
      eat(TokenType.CloseParen);
      return new AnnotationExpression(typeName, elements.build());
    } else {
      return new AnnotationExpression(typeName);
    }
  }

  /**
   * Consume the next token from the scanner and returns it. Returns null and reports an error if it
   * is not an identifier token.
   */
  private IdentifierToken eatId() {
    return (IdentifierToken) eat(TokenType.Id);
  }

  private PrimaryExpression parseIdentifierExpression() {
    IdentifierToken id = eatId();
    return new PrimaryExpression(id);
  }

  private Expression parseParenExpression() {
    eat(TokenType.OpenParen);
    Expression result = parseExpression();
    eat(TokenType.CloseParen);
    return result;
  }

  /**
   * Consume the next token from the scanner. Report an error and return null if the consumed token
   * is not of the desired type, otherwise return the token.
   */
  private Token eat(TokenType type) {
    Token token = nextToken();
    if (token.type != type) {
      reportExpectedError(token, type);
      return null;
    }
    return token;
  }

  /**
   * Report an error that an expected token was not found.
   *
   * @param token the token that was found instead
   * @param expected what was expected to be found
   */
  private void reportExpectedError(Token token, Object expected) {
    reportError(token, "'%s' expected", expected);
  }

  /**
   * Consumes the next token as a type name from the scanner and returns it. Reports an error and
   * returns null if the token is not a type name.
   */
  private IdentifierToken nextTypeName() {
    Token token = scanner.scanTypeName();
    if (token.type != TokenType.Id) {
      reportExpectedError(token, "type name");
      return null;
    }
    return (IdentifierToken) token;
  }

  /**
   * Consumes the next token from the scanner and returns it.
   */
  private Token nextToken() {
    return scanner.nextToken();
  }

  /**
   * Return the type of the next token. Does not consume the token.
   */
  private TokenType peekType() {
    return peekType(0);
  }

  /**
   * Return the type of the token index tokens ahead. Does not consume the token.
   */
  private TokenType peekType(int index) {
    return peekToken(index).type;
  }

  /**
   * Return the next token. Does not consume the token.
   */
  @SuppressWarnings("unused")
  private Token peekToken() {
    return peekToken(0);
  }

  /**
   * Return the token index positions ahead of the scanner. Does not consume the token.
   */
  private Token peekToken(int index) {
    return scanner.peekToken(index);
  }

  /**
   * Report an error at the given token.
   */
  private void reportError(Token token, String message, Object... arguments) {
    if (token == null) {
      reportError(message, arguments);
    } else {
      errorReporter.reportError(token.startIndex, token.endIndex, message, arguments);
    }
  }

  /**
   * Report an error at the current scanner position.
   */
  private void reportError(String message, Object... arguments) {
    errorReporter.reportError(scanner.getPosition(), scanner.getPosition() + 1, message, arguments);
  }

}
