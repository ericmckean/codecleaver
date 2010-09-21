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

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerId extends Id {
  public final List<Id> children = new ArrayList<Id>();

  private static final Func<Id, Iterable<Id>> getChildren =
      new Func<Id, Iterable<Id>>() {

        @Override public Iterable<Id> apply(Id arg) {
          if (arg instanceof ContainerId) {
            return ((ContainerId) arg).children;
          }

          // TODO(peterhal): figure out the generics variance/wildcards
          return EmptyIterable.<Id>value();
        }
      };

  public ContainerId(IdType type, PackageId parent) {
    super(type, parent);
  }

  @Override public Iterable<Id> getChildren() {
    return children;
  }

  @Override public Iterable<Id> allDescendants() {
    return new TreeIterable<Id>(this, getChildren);
  }
}
