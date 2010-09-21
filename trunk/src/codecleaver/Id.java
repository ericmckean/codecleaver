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

import codecleaver.iterable.ConcatIterable;
import codecleaver.iterable.EmptyIterable;
import codecleaver.iterable.SingletonIterable;

import com.google.common.collect.ImmutableList;

/**
 * An identifier for a java package, type, field or method. Ids are just representations of the
 * string form with a parent/child structure. Id's are unique - the equality and identity operation
 * on Ids yield the same result.
 *
 * <p>Id's should never be created directly. They should be retrieved from an IdTable.
 *
 * <p>Ideally Id's would be immutable. Unfortunately the parent of an Id representing a nested type
 * cannot be distinguished from a non-nested type with a '$' in the name until after the Id is
 * created. As a result, Id's can be re-parented.
 *
 *  TODO(peterhal): In retrospect, it would be better to model the parent/child relationship outside
 * the Id class
 */
public abstract class Id {
  public final IdType type;
  private ContainerId parent;
  private boolean attachedToParent;

  protected Id(IdType type, ContainerId parent) {

    this.parent = parent;
    this.type = type;
  }

  public ContainerId getParent() {
    return this.parent;
  }

  /**
   * Should only be called by IdTable.
   */
  public void attachToParent() {
    if (!attachedToParent) {
      if (this.parent != null) {
        this.parent.children.add(this);
      }
      attachedToParent = true;
    }
  }

  private void detachFromParent() {
    if (attachedToParent && parent != null) {
      parent.children.remove(this);
      attachedToParent = false;
    }
  }

  /**
   * Should only be called when reparenting nested classes.
   * @param newParent
   */
  public void setParent(ContainerId newParent) {
    if (newParent == this) {
      throw new RuntimeException("Setting parent to self");
    }
    if (attachedToParent && newParent == this.parent) {
      return;
    }
    detachFromParent();
    this.parent = newParent;
    attachToParent();
  }

  /**
   * Is id in the parent chain of this.
   */
  public boolean isDescendantOf(Id id) {
    return this == id || this.parent != null && this.parent.isDescendantOf(id);
  }

  public Iterable<Id> getChildren() {
    return ImmutableList.<Id>of();
  }

  /**
   * Returns the transitive closure of children of this id. Does not include this.
   */
  public Iterable<Id> allDescendants() {
    return EmptyIterable.<Id>value();
  }

  /**
   * Returns the transitive closure of children of this id. Does include this.
   */
  public Iterable<Id> idAndDescendants() {
    return new ConcatIterable<Id>(new SingletonIterable<Id>(this), allDescendants());
  }
}
