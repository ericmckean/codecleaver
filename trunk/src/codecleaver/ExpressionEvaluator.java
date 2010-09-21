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

import java.util.HashMap;
import java.util.Map;

import codecleaver.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Checks the semantics of and evaluates parsed expressions.
 */
public final class ExpressionEvaluator {
  private final ErrorReporter errorReporter;
  private final State state;

  /**
   * Evaluates the expression in the context of state reporting errors to errorReporter.
   *
   * @return the result of the expression or null if an error was reported.
   */
  public static Iterable<Id> eval(ErrorReporter errorReporter, State state, Expression expression) {
    return new ExpressionEvaluator(errorReporter, state).eval(expression);
  }

  /**
   * Create a new Expression evaluator for the given errorReporter and state.
   */
  public ExpressionEvaluator(ErrorReporter errorReporter, State state) {
    this.errorReporter = errorReporter;
    this.state = state;
  }

  /**
   * Evaluates the expression, reporting any semantic errors during evaluation.
   *
   * @return the result of the expression or null if an error was reported.
   */
  public Iterable<Id> eval(Expression expression) {
    switch (expression.type) {
      case Binary:
        return evalBinary(expression.asBinary());
      case Unary:
        return evalUnary(expression.asUnary());
      case Primary:
        return evalPrimary(expression.asPrimary());
      case Annotation:
        return evalAnnotation(expression.asAnnotation());
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * Evaluates a unary expression. Returns null and reports errors if the expression contains
   * semantic errors.
   */
  private Iterable<Id> evalUnary(UnaryExpression expression) {
    Iterable<Id> values = eval(expression.expression);
    if (hadError()) {
      return null;
    }

    switch (expression.operator) {
      case From:
        return state.getFrom(values);
      case To:
        return state.getTo(values);
      case TransitiveFrom:
        return state.getTransitiveFrom(values);
      case TransitiveTo:
        return state.getTransitiveTo(values);
      case Overrides:
        return state.getOverrides(values);
      case Overridden:
        return state.getOverridden(values);
      case TransitiveOverrides:
        return state.getTransitiveOverrides(values);
      case TransitiveOverridden:
        return state.getTransitiveOverridden(values);
      case Expand:
        return state.getExpand(values);
      case TransitiveExpand:
        return state.getTransitiveExpand(values);
      default:
        throw new IllegalArgumentException();
    }

  }

  /**
   * Evaluates a binary expression. Returns null and reports errors if the expression contains
   * semantic errors.
   */
  private Iterable<Id> evalBinary(BinaryExpression expression) {
    Iterable<Id> left = eval(expression.left);
    Iterable<Id> right = eval(expression.right);
    if (hadError()) {
      return null;
    }

    switch (expression.operator) {
      case Intersect:
        return state.getIntersect(left, right);
      case Union:
        return state.getUnion(left, right);
      case Minus:
        return state.getMinus(left, right);
      default:
        throw new IllegalStateException();
    }
  }

  /**
   * Evaluates a primary expression. Returns null and reports errors if the expression contains
   * semantic errors.
   */
  private Iterable<Id> evalPrimary(PrimaryExpression expression) {
    return getSet(expression.value);
  }

  /**
   * Evaluates an annotation expression. Returns null and reports errors if the expression contains
   * semantic errors.
   */
  private Iterable<Id> evalAnnotation(AnnotationExpression annotation) {
    return getAnnotationSet(annotation.typeName, annotation.elements);
  }

  /**
   * Returns the id matching the given identifier token. Returns null and reports an error if no
   * matching id is found.
   */
  private Id getExistingId(IdentifierToken word) {
    String[] parts = word.value.split("\\.", -1);
    Id id;
    if (parts.length == 1) {
      id = state.ids.getExistingPackage(parts[0]);
      if (id == null) {
        id = state.ids.getExistingType(parts[0]);
      }
    } else if (parts.length == 3) {
      TypeId type = state.ids.getExistingType(parts[0]);
      if (type == null) {
        reportError(word.startIndex, word.startIndex + parts[0].length(),
            "'%s' is not a valid type id", parts[0]);
        return null;
      }
      if (parts[2].length() > 0 && parts[2].charAt(0) == '(') {
        id = state.ids.getExistingMethod(type, parts[1], parts[2]);
      } else {
        id = state.ids.getExistingField(type, parts[1], parts[2]);
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

  /**
   * Returns the set of Ids matching the given identifier. Checks named sets then ids. Returns null
   * and reports an error if no set is found.
   */
  private Iterable<Id> getSet(IdentifierToken word) {
    if (state.hasSet(word.value)) {
      return state.getSet(word.value);
    }

    Id id = getExistingId(word);
    if (id == null) {
      return null;
    }

    return state.getSet(id);
  }

  private Iterable<Id> getAnnotationSet(
      IdentifierToken typeName, ImmutableList<Pair<IdentifierToken, IdentifierToken>> elements) {
    TypeId type = getExistingTypeId(typeName);
    if (type == null) {
      return null;
    }

    if (elements == null) {
      return state.getAnnotationSet(type);
    }

    Map<String, String> elementMap = new HashMap<String, String>();
    for (Pair<IdentifierToken, IdentifierToken> element : elements) {
      if (elementMap.containsKey(element.first)) {
        reportError(
            element.first, "Duplicate element name '%s' in attribute expression.", element.first);
      } else {
        // TODO(peterhal): check that typeName contains an element named
        // element.first.value
        elementMap.put(element.first.value, element.second.value);
      }
    }
    return state.getAnnotationSet(type, ImmutableMap.copyOf(elementMap));
  }

  /**
   * Returns the TypeId for the string contained in the typeName token. Reports an error and returns
   * null if no existing type id is found.
   */
  private TypeId getExistingTypeId(IdentifierToken typeName) {
    TypeId type = state.ids.getExistingType(typeName.value);
    if (type == null) {
      reportError(typeName, "'%s' is not a valid type id", typeName);
    }
    return type;
  }

  /**
   * @return Has the evaluation of this expression detected a semantic error.
   */
  private boolean hadError() {
    return errorReporter.hadError();
  }

  /**
   * Reports an error to the errorReporter located at a given token.
   */
  private void reportError(Token token, String format, Object... arguments) {
    reportError(token.startIndex, token.endIndex, format, arguments);
  }

  /**
   * Reports an error to the errorReporter.
   */
  private void reportError(int startIndex, int endIndex, String format, Object... arguments) {
    errorReporter.reportError(startIndex, endIndex, String.format(format, arguments));
  }
}
