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

import codecleaver.util.Func;

import com.google.common.collect.ImmutableMap;

/**
 * A Java Annotation which has been applied to a java type, field or method.
 * AnnotationInfo's are immutable.
 */
public class AnnotationInfo {
  public final TypeId type;
  /**
   * Elements are the name value pairs.
   */
  public final ImmutableMap<String, Object> elements;

  /**
   * getType is a function object which extracts the type from an AnnotationInfo.
   */
  public static final Func<AnnotationInfo, TypeId> getType =
      new Func<AnnotationInfo, TypeId>() {

        @Override public TypeId apply(AnnotationInfo annotation) {
          return annotation.type;
        }
      };

  public AnnotationInfo(TypeId type, ImmutableMap<String, Object> elements) {
    this.type = type;
    this.elements = elements;
  }
}
