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

public class PackageInfo extends ContainerInfo {
  @SuppressWarnings("hiding")
  public final PackageId id;

  public PackageInfo(PackageId id) {
    super(id, ImmutableSet.<AnnotationInfo>of());

    this.id = id;
  }

  public PackageInfo(PackageId id, PackageInfo parent) {

    super(id, parent, ImmutableSet.<AnnotationInfo>of());

    this.id = id;
  }

  @Override public PackageInfo getParent() {
    return (PackageInfo) super.getParent();
  }

  @Override public String getFileName() {
    return "<Package>";
  }
}
