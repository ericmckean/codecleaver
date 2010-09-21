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

import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationIterable<T> implements Iterable<T> {
  private final Enumeration<? extends T> values;

  public EnumerationIterable(Enumeration<? extends T> values) {
    this.values = values;
  }

  @Override public Iterator<T> iterator() {
    return
        new Iterator<T>() {

          @Override public boolean hasNext() {
            return values.hasMoreElements();
          }

          @Override public T next() {
            return values.nextElement();
          }

          @Override public void remove() {
            throw new UnsupportedOperationException();
          }
        };
  }

}
