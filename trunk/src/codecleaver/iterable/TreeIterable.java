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

package codecleaver.iterable;

import codecleaver.util.Func;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class TreeIterable<T> implements Iterable<T> {

  private static enum State {
    Begin, BeforeChild, ChildCurrent, End, AfterChild, DescendantCurrent, AfterDescendant,
  }

  private final Func<T, Iterable<T>> getChildren;
  private final T root;

  public TreeIterable(T root, Func<T, Iterable<T>> getChildren) {
    this.root = root;
    this.getChildren = getChildren;
  }

  @Override public Iterator<T> iterator() {
    return
        new Iterator<T>() {

          private State state = State.Begin;
          private T current;
          private Iterator<T> childrenIter;
          private Iterator<T> childDescendantIter;

          @Override public boolean hasNext() {
            switch (state) {
              case Begin:
                childrenIter = getChildren.apply(root).iterator();
                state = State.BeforeChild;
                return hasNext();
              case BeforeChild:
                if (childrenIter.hasNext()) {
                  current = childrenIter.next();
                  state = State.ChildCurrent;
                  return hasNext();
                }
                state = State.End;
                childrenIter = null;
                return hasNext();
              case ChildCurrent:
              case DescendantCurrent:
                return true;
              case AfterChild:
                childDescendantIter = new TreeIterable<T>(current, getChildren).iterator();
                current = null;
                state = State.AfterDescendant;
                return this.hasNext();
              case AfterDescendant:
                if (childDescendantIter.hasNext()) {
                  current = childDescendantIter.next();
                  state = State.DescendantCurrent;
                  return hasNext();
                }
                childDescendantIter = null;
                state = State.BeforeChild;
                return this.hasNext();
              case End:
                return false;
              default:
                throw new IllegalStateException();
            }
          }

          @Override
          // TODO(peterhal): handle multiple next() calls without
          // intervening hasNext()
          // calls
          public T next() {
            switch (state) {
              case ChildCurrent:
                state = State.AfterChild;
                return current;
              case DescendantCurrent:
                state = State.AfterDescendant;
                T result = current;
                current = null;
                return result;
              case AfterChild:
              case AfterDescendant:
              case Begin:
                this.hasNext();
                return next();
              case BeforeChild:
              default:
                throw new IllegalStateException();
              case End:
                throw new NoSuchElementException();
            }
          }

          @Override public void remove() {
            throw new UnsupportedOperationException();
          }
        };
  }
}
