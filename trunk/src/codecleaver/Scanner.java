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

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Scans a command string into tokens. Note that scanning is contextual. The results differ when
 * scanning in a set expression, file name, or type name. The default is to scan identifiers 
 * using the set expression rules.
 *
 * <p>Peek operations do not consume input. Next operations consume input.
 */
public final class Scanner {

  /**
   * Create a scanner which can produce tokens from the source string and will report errors on the
   * errorReporter.
   */
  public Scanner(ErrorReporter errorReporter, String source) {
    this.errorReporter = errorReporter;
    this.source = source;
    this.index = 0;
    this.currentTokens = new LinkedList<Token>();
  }

  /**
   * Returns the current index into the source string.
   */
  public int getPosition() {
    return index;
  }

  /**
   * Sets the current index into the source string.
   */
  public void setPosition(int position) {
    if (position < 0 || position > source.length()) {
      throw new IllegalArgumentException();
    }

    this.index = position;
  }

  /**
   * Scans the remainder of the source string and returns the resulting tokens. Identifiers are
   * scanned as expression ids.
   */
  public ArrayList<Token> getTokens() {
    ArrayList<Token> result = new ArrayList<Token>();
    Token token;
    do {
      token = nextToken();
      result.add(token);
    } while (token.type != TokenType.EndOfLine);
    return result;
  }

  /**
   * Scans the next token and returns it. The token is consumed - the position in the source string
   * is advanced past the returned token. If an identifier is found it is scanned as an expression
   * id.
   */
  public Token nextToken() {
    Token result = peekToken();
    return currentTokens.remove();
  }

  /**
   * Scans the next token and returns it. The token is not consumed - the position in the source
   * string is is left at the start of the returned token. If an identifier is found it is scanned
   * as an expression id.
   */
  public Token peekToken() {
    return peekToken(0);
  }

  /**
   * Scans forward index tokens and returns the token found there. Tokens are not consumed - the
   * position in the source string is left unchanged. Scanning past the end of the source string
   * yields an infinite sequence of EndOfLine tokens. If an identifier is found it is scanned as an
   * expression id.
   */
  public Token peekToken(int index) {
    while (currentTokens.size() <= index) {
      currentTokens.add(scanToken(IdentifierScanType.Expression));
    }
    return currentTokens.get(index);
  }

  private boolean isAtEnd() {
    return index == source.length();
  }

  /**
   * Advances position past any whitespace.
   */
  private void skipWhitespace() {
    while (!isAtEnd() && peekWhitespace()) {
      nextChar();
    }
  }

  /**
   * Is the character at the current position a whitespace char. Does not advance position.
   */
  private boolean peekWhitespace() {
    return Character.isWhitespace(peekChar());
  }

  /**
   * Advances position past any whitespace or '#' comments.
   */
  private void skipComment() {
    skipWhitespace();
    if (!isAtEnd() && peekChar() == '#') {
      index = source.length();
    }
  }

  /**
   * Scan the next token. If an identifier is found, use the file name scanning rules.
   */
  public Token scanFileName() {
    if (!currentTokens.isEmpty()) {
      throw new IllegalArgumentException();
    }
    return scanToken(IdentifierScanType.FileName);
  }

  /**
   * Scan the next token. If an identifier is found, use the type name scanning rules.
   */
  public Token scanTypeName() {
    if (!currentTokens.isEmpty()) {
      throw new IllegalArgumentException();
    }
    return scanToken(IdentifierScanType.TypeName);
  }

  /**
   * Returns the next whitespace delimited string.
   */
  public String scanHelpTopic() {
    skipComment();
    if (isAtEnd()) {
      return null;
    }
    String result = source.substring(index);
    if (result.indexOf(' ') > 0) {
      result = result.substring(0, result.indexOf(' '));
    }
    index += result.length();
    return result;
  }

  /**
   * The different ways that identifiers can be scanned.
   */
  private enum IdentifierScanType {
    /**
     * Identifiers in expressions may contain '/', '$', '-', '+', '#', '='. They may not contain 
     * '<', '>', '(', ')'.
     */
    Expression,
    /**
     * File names may contain '/', '$', '-', '+', '#', '='. They may not contain '<', '>', '(', ')'.
     */
    FileName,
    /**
     * Type names may contain '/', '$' and may not contain '-', '+', '#', '=', '<', '>', '(', ')'.
     */
    TypeName,
  }

  /**
   * Scan the next token at the current source position.
   * 
   * @param identifierScanType how to scan an identifier if found
   * @return the token scanned
   */
  private Token scanToken(IdentifierScanType identifierScanType) {
    skipComment();
    int beginToken = index;
    if (isAtEnd()) {
      return new Token(TokenType.EndOfLine, beginToken, index);
    }
    char ch = nextChar();
    switch (ch) {
      case '(':
        return new Token(TokenType.OpenParen, beginToken, index);
      case ')':
        return new Token(TokenType.CloseParen, beginToken, index);
      case '+':
        if (peekChar() == '=') {
          nextChar();
          return new Token(TokenType.UnionAssign, beginToken, index);
        }
        return new Token(TokenType.Union, beginToken, index);
      case '^':
        if (peekChar() == '=') {
          nextChar();
          return new Token(TokenType.IntersectAssign, beginToken, index);
        }
        return new Token(TokenType.Intersect, beginToken, index);
      case '-':
        if (peekChar() == '=') {
          nextChar();
          return new Token(TokenType.MinusAssign, beginToken, index);
        }
        return new Token(TokenType.Minus, beginToken, index);
      case '=':
        return new Token(TokenType.Assign, beginToken, index);
      case '<':
        if (peekChar() == '*') {
          nextChar();
          return new Token(TokenType.TransitiveFrom, beginToken, index);
        }
        return new Token(TokenType.From, beginToken, index);
      case '>':
        if (peekChar() == '*') {
          nextChar();
          return new Token(TokenType.TransitiveTo, beginToken, index);
        }
        return new Token(TokenType.To, beginToken, index);
      case '[':
        if (peekChar() == '*') {
          nextChar();
          return new Token(TokenType.TransitiveOverrides, beginToken, index);
        }
        return new Token(TokenType.Overrides, beginToken, index);
      case ']':
        if (peekChar() == '*') {
          nextChar();
          return new Token(TokenType.TransitiveOverridden, beginToken, index);
        }
        return new Token(TokenType.Overridden, beginToken, index);
      case '!':
        if (peekChar() == '*') {
          nextChar();
          return new Token(TokenType.TransitiveExpand, beginToken, index);
        }
        return new Token(TokenType.Expand, beginToken, index);
      case '*':
        return new Token(TokenType.TransitiveExpand, beginToken, index);
      case '@':
        return new Token(TokenType.At, beginToken, index);
      case ',':
        return new Token(TokenType.Comma, beginToken, index);
      case '"':
        int beginQuotedString = beginToken + 1;
        while (!isAtEnd()) {
          ch = nextChar();
          if (ch == '"') {
            return new IdentifierToken(
                beginToken, index, source.substring(beginQuotedString, index - 1));
          }
        }
        reportError(beginToken, beginToken + 1, "Unterminated quoted string");
        return new IdentifierToken(beginToken, index, source.substring(beginQuotedString, index));

      default:
        int parenDepth = 0;
        int angleDepth = 0;
        endIdentifier: while (!isAtEnd() && !peekWhitespace()) {
          ch = peekChar();
          switch (ch) {
            case '(':
              if (identifierScanType != IdentifierScanType.Expression) {
                break endIdentifier;
              }
              parenDepth++;
              break;
            case ')':
              if (parenDepth > 0 && identifierScanType == IdentifierScanType.Expression) {
                parenDepth--;
              } else {
                break endIdentifier;
              }
              break;
            case '<':
              if (identifierScanType != IdentifierScanType.Expression) {
                break endIdentifier;
              }
              angleDepth++;
              break;
            case '>':
              if (angleDepth > 0 && identifierScanType == IdentifierScanType.Expression) {
                angleDepth--;
              } else {
                break endIdentifier;
              }
              break;
            case '+': // TODO(peterhal): can '+' appear in nested class names?
            case '^':
            case '-':
            case '#':
            case '=':
              if (identifierScanType != IdentifierScanType.FileName) {
                break endIdentifier;
              }
          }
          nextChar();
        }
        return new IdentifierToken(beginToken, index, source.substring(beginToken, index));
    }
  }

  /**
   * Returns the char at the current source position. Advances the source position one char.
   */
  private char nextChar() {
    return source.charAt(index++);
  }

  /**
   * Returns the char at the current source position. Does not advance the source position.
   */
  private char peekChar() {
    return isAtEnd() ? '\0' : source.charAt(index);
  }

  private void reportError(int startIndex, int endIndex, String format, Object... arguments) {
    errorReporter.reportError(startIndex, endIndex, String.format(format, arguments));
  }

  private final ErrorReporter errorReporter;
  private final String source;
  private int index;
  private LinkedList<Token> currentTokens;
}
