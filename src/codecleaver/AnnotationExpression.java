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
 * A parsed CodeCleaver expression representing an annotation query.
 * AnnotationExpression's are immutable.
 */
public class AnnotationExpression extends Expression {
  public final IdentifierToken typeName;
  public final ImmutableList<Pair<IdentifierToken, IdentifierToken>> elements;

  /**
   * The special 'value' element name, used in the shorthand constructor.
   */
  public static final String VALUE = "value";

  /**
   * Create an annotation expression which only matches the annotation type and does not distinguish
   * on the elements.
   */
  public AnnotationExpression(IdentifierToken typeName) {
    this(typeName, ImmutableList.<Pair<IdentifierToken, IdentifierToken>>of());
  }

  /**
   * Create an annotation expression which matches an annotation type and matches a single element
   * whose name is the special 'value' element name.
   */
  public AnnotationExpression(IdentifierToken typeName, IdentifierToken value) {
    this(typeName, ImmutableList.of(
        new Pair<IdentifierToken, IdentifierToken>(
            (value == null) ? null : new IdentifierToken(value.startIndex, value.endIndex, VALUE),
            value)));
  }

  /**
   * Create an annotation expression which matches an annotation type and matches on a list
   * of name/value element pairs.
   */
  public AnnotationExpression(
      IdentifierToken typeName, ImmutableList<Pair<IdentifierToken, IdentifierToken>> elements) {
    super(ExpressionType.Annotation);
    this.typeName = typeName;
    this.elements = elements;
  }

  @Override public AnnotationExpression asAnnotation() {
    return this;
  }
}
