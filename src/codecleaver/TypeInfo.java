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

import com.google.common.collect.ImmutableSet;

public class TypeInfo extends ContainerInfo {
  @SuppressWarnings("hiding")
  public final TypeId id;
  public final TypeId superId;
  public final TypeId[] interfaces;
  public final String file;

  public TypeInfo(String file,
      TypeId id,
      ContainerInfo parent,
      int access,
      TypeId superId,
      TypeId[] interfaces,
      ImmutableSet<AnnotationInfo> annotations) {

    super(id, parent, access, annotations);

    if (file == null) {
      throw new RuntimeException(String.format("Missing file for '%s'.", id));
    }
    
    this.file = file;
    this.id = id;
    this.superId = superId;
    this.interfaces = interfaces;
  }

  @Override public String getFileName() {
    return file;
  }
}
