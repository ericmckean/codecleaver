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

import codecleaver.iterable.EmptyIterable;
import codecleaver.iterable.TreeIterable;
import codecleaver.util.Func;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerInfo extends Info {

  public final List<Info> children = new ArrayList<Info>();

  private static final Func<Info, Iterable<Info>> getChildren =
      new Func<Info, Iterable<Info>>() {

        @Override public Iterable<Info> apply(Info arg) {
          if (arg instanceof ContainerInfo) {
            return ((ContainerInfo) arg).children;
          }

          // TODO(peterhal): figure out the generics variance/wildcards
          return EmptyIterable.<Info>value();
        }
      };

  @Override public Iterable<Info> allDescendants() {
    return new TreeIterable<Info>(this, getChildren);
  }

  protected ContainerInfo(Id id, ImmutableSet<AnnotationInfo> annotations) {
    super(id, annotations);
  }

  protected ContainerInfo(Id id, ContainerInfo parent, ImmutableSet<AnnotationInfo> annotations) {
    super(id, parent, annotations);
  }

  protected ContainerInfo(
      Id id, ContainerInfo parent, int access, ImmutableSet<AnnotationInfo> annotations) {
    super(id, parent, access, annotations);
  }
}
