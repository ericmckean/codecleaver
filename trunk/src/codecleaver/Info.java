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
import codecleaver.iterable.Sequence;
import codecleaver.iterable.SingletonIterable;
import codecleaver.util.Func;

import com.google.common.collect.ImmutableMap;

import org.objectweb.asm.Opcodes;

/**
 * A java package, type, method or field including semantic information. Info's should never be
 * created directly - they should always be created by an InfoTable.
 *
 * Info's are unique - the identity and equality operations on Info's yield the same result.
 */
public abstract class Info {
  public final Id id;
  public final int access;
  private ContainerInfo parent;
  private final String stringId;
  public final ImmutableMap<TypeId, AnnotationInfo> annotations;

  /**
   * Converts an Iterable<Info> to an Iterable<Id>
   */
  public static Iterable<Id> idsOfInfos(Iterable<Info> values) {
    return Sequence.select(values, GET_ID);
  }

  /**
   * Function object returning the id of an info.
   */
  public static final Func<Info, Id> GET_ID =
      new Func<Info, Id>() {

        @Override public Id apply(Info arg) {
          return arg.id;
        }
      };

  /**
   * The jar file containing the definition of this info.
   */
  public abstract String getFileName();

  public IdType getType() {
    return id.type;
  }

  protected Info(Id id, Iterable<AnnotationInfo> annotations) {
    this.id = id;
    this.access = Opcodes.ACC_PUBLIC;
    setParent(null);
    this.stringId = id.toString();
    this.annotations = Sequence.createMap(annotations, AnnotationInfo.getType);
  }

  protected Info(Id id, ContainerInfo parent, Iterable<AnnotationInfo> annotations) {
    this(id, parent, Opcodes.ACC_PUBLIC, annotations);
  }

  protected Info(Id id, ContainerInfo parent, int access, Iterable<AnnotationInfo> annotations) {
    this.id = id;
    this.access = access;
    setParent(parent);
    this.stringId = id.toString();
    this.annotations = Sequence.createMap(annotations, AnnotationInfo.getType);
  }

  public ContainerInfo getParent() {
    return this.parent;
  }

  /**
   * Nested classes are created before their outer classes. Otherwise this wouldn't be required.
   */
  public void setParent(ContainerInfo newParent) {
    if (newParent != this.parent) {
      this.id.setParent((ContainerId) newParent.id);
      if (parent != null) {
        parent.children.remove(this);
      }
      this.parent = newParent;
      if (this.parent != null) {
        this.parent.children.add(this);
      }
    }
  }

  @Override public String toString() {
    return stringId;
  }

  public boolean isDescendantOf(Id id) {
    return this.id == id || this.parent != null && this.parent.isDescendantOf(id);
  }

  /**
   * Returns all infos which have this info in their parent chain. Does not include this info.
   */
  public Iterable<Info> allDescendants() {
    return EmptyIterable.<Info>value();
  }

  /**
   * Returns all infos which have this info in their parent chain. Includes this info.
   */
  public Iterable<Info> infoAndDescendants() {
    return new ConcatIterable<Info>(new SingletonIterable<Info>(this), allDescendants());
  }

  public boolean isPublic() {
    return (access & Opcodes.ACC_PUBLIC) != 0;
  }

  public boolean isPrivate() {
    return (access & Opcodes.ACC_PRIVATE) != 0;
  }

  public boolean isProtected() {
    return (access & Opcodes.ACC_PROTECTED) != 0;
  }

  public boolean isPackagePrivate() {
    return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) == 0;
  }

  public boolean isStatic() {
    return (access & Opcodes.ACC_STATIC) != 0;
  }

  public boolean isFinal() {
    return (access & Opcodes.ACC_FINAL) != 0;
  }

  public boolean isSuper() {
    return (access & Opcodes.ACC_SUPER) != 0;
  }

  public boolean isSynchronized() {
    return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
  }

  public boolean isVolatile() {
    return (access & Opcodes.ACC_VOLATILE) != 0;
  }

  public boolean isBridge() {
    return (access & Opcodes.ACC_BRIDGE) != 0;
  }

  public boolean isVarArgs() {
    return (access & Opcodes.ACC_VARARGS) != 0;
  }

  public boolean isTransient() {
    return (access & Opcodes.ACC_TRANSIENT) != 0;
  }

  public boolean isNative() {
    return (access & Opcodes.ACC_NATIVE) != 0;
  }

  public boolean isInterface() {
    return (access & Opcodes.ACC_INTERFACE) != 0;
  }

  public boolean isAbstract() {
    return (access & Opcodes.ACC_ABSTRACT) != 0;
  }

  public boolean isStrict() {
    return (access & Opcodes.ACC_STRICT) != 0;
  }

  public boolean isSynthetic() {
    return (access & Opcodes.ACC_SYNTHETIC) != 0;
  }

  public boolean isAnnotation() {
    return (access & Opcodes.ACC_ANNOTATION) != 0;
  }

  public boolean isEnum() {
    return (access & Opcodes.ACC_ENUM) != 0;
  }

  public PackageInfo getContainingPackage() {
    Info info = this;
    do {
      info = info.getParent();
    } while (info != null && info.getType() != IdType.Package);

    return (PackageInfo) info;
  }

  /**
   * Returns null if not contained in a type.
   */
  public TypeInfo getContainingType() {
    Info info = this;
    do {
      info = info.getParent();
    } while (info != null && info.getType() != IdType.Type);

    return (TypeInfo) info;
  }
}
