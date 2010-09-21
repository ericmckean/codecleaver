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
 * An abstract syntax tree for a CodeCleaver command. Expressions add syntactic structure but have 
 * not been semantically checked. Expressions are immutable.
 * 
 * The asX() methods are overridden in the XExpression derived classes to avoid casting operations.
 */
public class Expression {
  protected Expression(ExpressionType type) {
    this.type = type;
  }

  public UnaryExpression asUnary() {
    throw new IllegalArgumentException();
  }

  public BinaryExpression asBinary() {
    throw new IllegalArgumentException();
  }

  public AssignmentExpression asAssignment() {
    throw new IllegalArgumentException();
  }

  public PrimaryExpression asPrimary() {
    throw new IllegalArgumentException();
  }

  public AnnotationExpression asAnnotation() {
    throw new IllegalArgumentException();
  }

  public final ExpressionType type;
}
