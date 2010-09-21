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

/**
 * The different token types for the CodeCleaver command language.
 */
public enum TokenType {
  EndOfLine(null),
  // punctuation
  OpenParen("("),
  CloseParen(")"),
  // For attribute queries
  At("@"),
  Comma(","),
  // binary operators
  Union("+", BinaryOperator.Union),
  Intersect("^", BinaryOperator.Intersect),
  Minus("-", BinaryOperator.Minus),
  Assign("=", AssignmentOperator.Assign),
  UnionAssign("+=", AssignmentOperator.UnionAssign),
  IntersectAssign("^=", AssignmentOperator.IntersectAssign),
  MinusAssign("-=", AssignmentOperator.MinusAssign),

  // unary operators
  From("<", UnaryOperator.From),
  To(">", UnaryOperator.To),
  TransitiveFrom("<*", UnaryOperator.TransitiveFrom),
  TransitiveTo(">*", UnaryOperator.TransitiveTo),
  Overrides("[", UnaryOperator.Overrides),
  Overridden("]", UnaryOperator.Overridden),
  TransitiveOverrides("[*", UnaryOperator.TransitiveOverrides),
  TransitiveOverridden("]*", UnaryOperator.TransitiveOverridden),
  Expand("!", UnaryOperator.Expand),
  TransitiveExpand("!*", UnaryOperator.TransitiveExpand), 
  Id(null);

  public final String value;
  public final UnaryOperator unaryOperator;
  public final BinaryOperator binaryOperator;
  public final AssignmentOperator assignmentOperator;

  public boolean isUnaryOperator() {
    return unaryOperator != null;
  }

  public boolean isBinaryOperator() {
    return binaryOperator != null;
  }

  public boolean isAssignmentOperator() {
    return assignmentOperator != null;
  }

  TokenType(String value, UnaryOperator unaryOperator) {
    this(value, unaryOperator, null, null);
  }

  TokenType(String value, BinaryOperator binaryOperator) {
    this(value, null, binaryOperator, null);
  }

  TokenType(String value, AssignmentOperator assignmentOperator) {
    this(value, null, null, assignmentOperator);
  }
  
  TokenType(String value) {
    this(value, null, null, null);
  }

  TokenType(String value, UnaryOperator unaryOperator, BinaryOperator binaryOperator, 
      AssignmentOperator assignmentOperator) {

    this.value = value;
    this.unaryOperator = unaryOperator;
    this.binaryOperator = binaryOperator;
    this.assignmentOperator = assignmentOperator;
  }
}
