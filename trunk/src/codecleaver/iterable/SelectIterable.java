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

public class SelectIterable<S, T> implements Iterable<S> {

  private final Iterable<? extends T> values;
  private final Func<? super T, ? extends S> selector;

  public SelectIterable(Iterable<? extends T> values, Func<? super T, ? extends S> selector) {
    this.values = values;
    this.selector = selector;
  }

  private static enum State {
    Begin, BeforeValue, Current, End,
  }

  @Override public Iterator<S> iterator() {
    return
        new Iterator<S>() {

          private State state = State.Begin;
          private Iterator<? extends T> iterator;
          private S current;

          @Override public boolean hasNext() {
            switch (state) {
              case Begin:
                iterator = values.iterator();
                state = State.BeforeValue;
                return hasNext();
              case BeforeValue:
                if (iterator.hasNext()) {
                  current = selector.apply(iterator.next());
                  state = State.Current;
                  return hasNext();
                }
                current = null;
                iterator = null;
                state = State.End;
                return hasNext();
              case Current:
                return true;
              case End:
                return false;
              default:
                throw new IllegalStateException();
            }
          }

          @Override public S next() {
            switch (state) {
              case Current:
                S result = current;
                current = null;
                state = State.BeforeValue;
                return result;
              case Begin:
              case BeforeValue:
                hasNext();
                return next();
              case End:
                throw new NoSuchElementException();
              default:
                throw new IllegalStateException();
            }
          }

          @Override public void remove() {
            throw new UnsupportedOperationException();
          }
        };
  }

}
