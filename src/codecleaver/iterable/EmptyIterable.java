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

public final class EmptyIterable<T> implements Iterable<T> {

  // TODO(peterhal): generics and wildcards
  @SuppressWarnings("unchecked")
  private static final EmptyIterable theValue = new EmptyIterable();

  @SuppressWarnings({"unchecked", "cast"})
  public static <E> EmptyIterable<E> value() {
    return (EmptyIterable<E>) theValue;
  }

  private final Iterator<T> iterator =
      new Iterator<T>() {

        @Override public boolean hasNext() {
          return false;
        }

        @Override public T next() {
          // TODO(peterhal): should throw IllegalStateException if hasNext has not
          // been called
          // but then we can't make this a singleton class
          throw new NoSuchElementException();
        }

        @Override public void remove() {
          throw new UnsupportedOperationException();
        }
      };

  @Override public Iterator<T> iterator() {
    return iterator;
  }

}
