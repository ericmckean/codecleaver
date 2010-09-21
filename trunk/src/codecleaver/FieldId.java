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

public class FieldId extends Id {
  @SuppressWarnings("hiding")
  public final TypeId type;
  public final String name;
  public final String descriptor;

  public FieldId(TypeId type, String name, String descriptor) {
    super(IdType.Field, type);

    if (type == null || name == null || descriptor == null) {
      throw new IllegalArgumentException();
    }

    this.type = type;
    this.name = name;
    this.descriptor = descriptor;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof FieldId)) {
      return false;
    }

    FieldId other = (FieldId) obj;

    return other.type.equals(this.type) && other.name.equals(this.name)
        && other.descriptor.equals(this.descriptor);
  }

  @Override public int hashCode() {
    return type.hashCode() ^ name.hashCode();
  }

  @Override public String toString() {
    return type.toString() + '.' + name + '.' + descriptor;
  }
}
