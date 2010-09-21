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

public class FilteredIterable<T> implements Iterable<T> {

  private final Func<? super T, Boolean> filter;
  private final Iterable<? extends T> values;

  public FilteredIterable(Iterable<? extends T> values, Func<? super T, Boolean> filter) {
    this.values = values;
    this.filter = filter;
  }

  @Override public Iterator<T> iterator() {
    return new FilteredIterator();
  }

  private static enum State {
    Begin, Current, Next, End,
  }

  private class FilteredIterator implements Iterator<T> {

    private Iterator<? extends T> iter;
    private T current;
    private State state = State.Begin;

    @Override public boolean hasNext() {
      switch (state) {
        case Begin:
          iter = values.iterator();
          state = State.Next;
          return hasNext();
        case Next:
          while (iter.hasNext()) {
            current = iter.next();
            if (filter.apply(current)) {
              state = State.Current;
              return hasNext();
            }
            current = null;
          }
          state = State.End;
          iter = null;
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
          state = State.Next;
          return result;
        case Begin:
        case Next:
          this.hasNext();
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

  }
}
