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

public class CharIterable implements Iterable<Character> {
  private CharSequence values;
  private int index;

  public CharIterable(CharSequence values) {
    if (values != null && values.length() > 0) {
      this.values = values;
    }
  }

  @Override public Iterator<Character> iterator() {
    return
        new Iterator<Character>() {

          @Override public boolean hasNext() {
            return values != null;
          }

          @Override public Character next() {
            if (values == null) {
              throw new NoSuchElementException();
            }
            char result = values.charAt(index);
            index++;
            if (index == values.length()) {
              values = null;
            }
            return result;
          }

          @Override public void remove() {
            throw new UnsupportedOperationException();
          }
        };
  }


}
