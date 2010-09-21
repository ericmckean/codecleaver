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

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

public final class Sequence {

  private Sequence() {}

  public static <T> Iterable<T> empty() {
    return EmptyIterable.value();
  }

  public static <T> void addAll(Collection<? super T> set, Iterable<? extends T> values) {
    for (T value : values) {
      set.add(value);
    }
  }

  public static <T> HashSet<T> createSet(Iterable<? extends T> values) {
    HashSet<T> result = new HashSet<T>();
    addAll(result, values);
    return result;
  }

  public static <T> Collection<T> createCollection(Iterable<? extends T> values) {
    ArrayList<T> result = new ArrayList<T>();
    addAll(result, values);
    return result;
  }

  public static <K, V> ImmutableMap<K, V> createMap(
      Iterable<? extends V> values, Func<? super V, ? extends K> computeKey) {
    ImmutableMap.Builder<K, V> builder = new ImmutableMap.Builder<K, V>();
    for (V value : values) {
      builder.put(computeKey.apply(value), value);
    }
    return builder.build();
  }

  public static <T> boolean all(Iterable<? extends T> values, Func<? super T, Boolean> filter) {
    for (T value : values) {
      if (!filter.apply(value)) {
        return false;
      }
    }
    return true;
  }

  public static boolean all(CharSequence values, Func<? super Character, Boolean> filter) {
    return all(new CharIterable(values), filter);
  }

  public static <T> void removeAll(Collection<? super T> set, Iterable<? extends T> values) {
    for (T value : values) {
      set.remove(value);
    }
  }

  public static <S, T> Iterable<S> select(
      Iterable<? extends T> values, Func<? super T, S> selector) {
    return new SelectIterable<S, T>(values, selector);
  }

  public static <T> Iterable<T> toSequence(Enumeration<? extends T> values) {
    return new EnumerationIterable<T>(values);
  }

  public static <T> Iterable<T> toSequence(T[] values) {
    ArrayList<T> result = new ArrayList<T>(values.length);
    for (T value : values) {
      result.add(value);
    }
    return result;
  }

  public static <T> Iterable<T> singleton(T value) {
    return new SingletonIterable<T>(value);
  }

  public static Object[] sortStrings(Iterable<? extends Object> values) {
    ArrayList<String> strings = new ArrayList<String>();
    for (Object value : values) {
      strings.add(value.toString());
    }
    Object[] stringsArray = strings.toArray();
    Arrays.sort(stringsArray);
    return stringsArray;
  }
}
