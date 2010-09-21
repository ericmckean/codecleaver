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

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class SingletonIterable<T> implements Iterable<T> {

  private static enum State {
    Begin, Current, End,
  }

  private final T value;

  public SingletonIterable(T value) {
    this.value = value;
  }

  @Override public Iterator<T> iterator() {
    return
        new Iterator<T>() {

          private State state = State.Begin;
          private T current = value;

          @Override public boolean hasNext() {
            switch (state) {
              case Begin:
                state = State.Current;
                return hasNext();
              case Current:
                return true;
              case End:
                return false;
              default:
                throw new IllegalStateException();
            }
          }

          @Override public T next() {
            switch (state) {
              case Current:
                T result = current;
                current = null;
                state = State.End;
                return result;
              case Begin:
                hasNext();
                return next();
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
