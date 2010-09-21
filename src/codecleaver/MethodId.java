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

public class MethodId extends Id {
  public final String initializerName = "<init>";
  public final String classInitializerName = "<clinit>";
  public final String classInitializerDescriptor = "()V";

  public MethodId(TypeId classId, String name, String desc) {
    super(IdType.Method, classId);

    if (classId == null || name == null || desc == null) {
      throw new IllegalArgumentException();
    }
    this.classId = classId;
    this.name = name;
    this.desc = desc;
  }

  public final TypeId classId;
  public final String name;
  public final String desc;

  @Override public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof MethodId)) {
      return false;
    }

    MethodId other = (MethodId) obj;
    return other.classId.equals(this.classId) && other.name.equals(this.name)
        && other.desc.equals(this.desc);
  }

  public boolean isInitializer() {
    return this.name.equals(initializerName);
  }
  
  public boolean isStaticInitializer() {
    return this.name.equals(classInitializerName)
        && this.desc.equals(classInitializerDescriptor);
  }

  @Override public int hashCode() {
    return classId.hashCode() ^ name.hashCode() ^ desc.hashCode();
  }

  @Override public String toString() {
    return classId.toString() + '.' + name + '.' + desc;
  }
}
