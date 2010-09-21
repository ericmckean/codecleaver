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

public class PackageId extends ContainerId {
  public final String name;

  public PackageId() {
    this(null, "");
  }

  public PackageId(PackageId container, String name) {
    super(IdType.Package, container);

    this.name = name;
  }

  @Override public PackageId getParent() {
    return (PackageId) super.getParent();
  }
  
  @Override public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof PackageId) {
      PackageId o = (PackageId) other;
      return this.name.equals(o.name);
    }

    return false;
  }

  @Override public int hashCode() {
    return name.hashCode();
  }

  @Override public String toString() {
    return name;
  }
}
