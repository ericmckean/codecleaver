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

public class ConcatIterable<T> implements Iterable<T> {

  private final Iterable<? extends T> head;
  private final Iterable<? extends T> tail;

  public ConcatIterable(Iterable<? extends T> head, Iterable<? extends T> tail) {
    this.head = head;
    this.tail = tail;
  }

  private static enum State {
    Begin, BeforeHead, HeadCurrent, BeforeTail, TailCurrent, End,
  }

  @Override public Iterator<T> iterator() {
    return
        new Iterator<T>() {

          private State state = State.Begin;
          private T current;
          private Iterator<? extends T> iter;

          @Override public boolean hasNext() {
            switch (state) {
              case Begin:
                iter = head.iterator();
                state = State.BeforeHead;
                return hasNext();
              case BeforeHead:
                if (iter.hasNext()) {
                  current = iter.next();
                  state = State.HeadCurrent;
                  return hasNext();
                }
                iter = tail.iterator();
                state = State.BeforeTail;
                return hasNext();
              case BeforeTail:
                if (iter.hasNext()) {
                  current = iter.next();
                  state = State.TailCurrent;
                  return hasNext();
                }
                iter = null;
                state = State.End;
                return hasNext();
              case HeadCurrent:
              case TailCurrent:
                return true;
              case End:
                return false;
              default:
                throw new IllegalStateException();
            }
          }

          @Override public T next() {
            T result;
            switch (state) {
              case HeadCurrent:
                state = State.BeforeHead;
                result = current;
                current = null;
                return result;
              case TailCurrent:
                state = State.BeforeTail;
                result = current;
                current = null;
                return result;
              case End:
                throw new NoSuchElementException();
              case Begin:
              case BeforeHead:
              case BeforeTail:
                this.hasNext();
                return next();
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
