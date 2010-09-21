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

import codecleaver.iterable.Sequence;
import codecleaver.util.Func;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;

public class InfoTable {

  public InfoTable(IdTable ids) {
    this.ids = ids;
    this.rootPackage = new PackageInfo(ids.getIdOfPackage(""));
    add(rootPackage);
  }

  public final PackageInfo rootPackage;
  public final IdTable ids;
  private final HashMap<Id, Info> infosById = new HashMap<Id, Info>();

  /**
   * Returns our best guess at the containing info of an id. If Id is a type contained in a package
   * then it will return or create the containing package info. If Id is a nested type, then the
   * parent of the Id may be the outer type for which no info has been created. In this case, we
   * can't create the outer type here, so return the package containing the outer type and then
   * we'll reparent the nested Info later.
   */
  public ContainerInfo getBestGuessContainerOf(Id id) {
    ContainerId containerId = id.getParent();
    ContainerInfo result = null;
    while (result == null && containerId.type == IdType.Type) {
      result = (ContainerInfo) infosById.get(containerId);
      containerId = containerId.getParent();
    }
    if (result == null) {
      result = getOrCreatePackage((PackageId) containerId);
    }
    return result;
  }

  /**
   * Returns the existing info for the given package id or creates a new package info for the id if
   * one has not been created yet.
   */
  public PackageInfo getOrCreatePackage(PackageId id) {
    PackageInfo result = (PackageInfo) infosById.get(id);
    if (result == null) {
      result = createPackage(id, getOrCreatePackage(id.getParent()));
    }
    return result;
  }

  /**
   * Has an info been created for this id yet.
   */
  public Boolean hasInfo(Id id) {
    return infosById.containsKey(id);
  }

  /**
   * Returns the existing info for an id.
   *
   * @throws MissingInfoException if !hasInfo(id)
   */
  public Info getInfo(Id id) {
    Info result = getOptionalInfo(id);
    if (result == null) {
      throw new MissingInfoException(id);
    }
    return result;
  }

  /**
   * Returns the info for an id. Returns null if there is no existing info for id. 
   */
  public Info getOptionalInfo(Id id) {
    if (id == null) {
      throw new IllegalArgumentException();
    }
    Info result = infosById.get(id);
    return result;
  }

  /**
   * Returns the existing info for a package.
   *
   * @throws MissingInfoException if !hasInfo(id)
   */
  public PackageInfo getPackage(PackageId id) {
    return (PackageInfo) getInfo(id);
  }

  /**
   * Returns the existing info for a type.
   *
   * @throws MissingInfoException if !hasInfo(id)
   */
  public TypeInfo getType(TypeId id) {
    return (TypeInfo) getInfo(id);
  }

  /**
   * Returns the existing info for a type. Returns null if no info for this type exists.
   */
  public TypeInfo getOptionalType(TypeId id) {
    return (TypeInfo) getOptionalInfo(id);
  }

  /**
   * Returns the existing info for a method.
   *
   * @throws MissingInfoException if !hasInfo(id)
   */
  public MethodInfo getMethod(MethodId id) {
    return (MethodInfo) getInfo(id);
  }

  /**
   * Returns the existing info for a field.
   *
   * @throws MissingInfoException if !hasInfo(id)
   */
  public FieldInfo getField(FieldId id) {
    return (FieldInfo) getInfo(id);
  }

  /**
   * Returns an info for an annotation.
   */
  public AnnotationInfo getAnnotation(TypeId type, ImmutableMap<String, Object> elements) {
    // TODO(peterhal): intern annotation values
    return new AnnotationInfo(type, elements);
  }

  /**
   * All the infos which have been created.
   */
  public Iterable<Info> getAll() {
    return rootPackage.allDescendants();
  }

  /**
   * Create a new TypeInfo and add it to this table. An info for id must not have been created yet.
   */
  public TypeInfo createType(String file,
      TypeId id,
      ContainerInfo parent,
      int access,
      TypeId superId,
      TypeId[] interfaces,
      ImmutableSet<AnnotationInfo> annotations) {
    TypeInfo result = new TypeInfo(file, id, parent, access, superId, interfaces, annotations);
    add(result);
    return result;
  }

  /**
   * Create a new packageInfo and add it to this table. An info for id must not have been created
   * yet.
   */
  public PackageInfo createPackage(PackageId id, PackageInfo parent) {
    PackageInfo result = new PackageInfo(id, parent);
    add(result);
    return result;
  }

  /**
   * Create a new MethodInfo and add it to this table. An info for id must not have been created
   * yet.
   */
  public MethodInfo createMethod(MethodId id, TypeInfo parent, int access, String desc,
      ImmutableSet<AnnotationInfo> annotations) {
    MethodInfo result = new MethodInfo(id, parent, access, desc, annotations);
    add(result);
    return result;
  }

  /**
   * Create a new FieldInfo and add it to this table. An info for id must not have been created yet.
   */
  public FieldInfo createField(FieldId id, TypeInfo parent, int access, String desc,
      ImmutableSet<AnnotationInfo> annotations) {
    FieldInfo result = new FieldInfo(id, parent, access, desc, annotations);
    add(result);
    return result;
  }

  /**
   * Add a new info to the table.
   */
  private void add(Info info) {
    if (infosById.containsKey(info.id)) {
      throw new IllegalArgumentException("Duplicate definition: " + info.id.toString());
    }
    infosById.put(info.id, info);
  }

  /**
   * Resolves a field reference to an info. Searches typeInfo, super interfaces of typeInfo then
   * super classes of typeInfo. Returns null if the reference could not be resolved. See 5.4.3.2
   * Field Resolution of the JVM spec for details.
   */
  public FieldInfo resolveField(TypeInfo typeInfo, String name, String desc) {

    // check this class
    FieldInfo result = lookupField(typeInfo, name, desc);
    if (result != null) {
      return result;
    }

    // check direct super interfaces
    for (TypeId interfaceId : typeInfo.interfaces) {
      result = resolveField(interfaceId, name, desc);
      if (result != null) {
        return result;
      }
    }

    // check super class
    result = resolveField(typeInfo.superId, name, desc);
    if (result != null) {
      return result;
    }

    return null;
  }

  /**
   * Returns a field in typeInfo matching name and desc if one exists. Returns null if no match is
   * found. Does not search super types/interfaces.
   */
  private FieldInfo lookupField(TypeInfo typeInfo, String name, String desc) {
    FieldId fieldId = ids.getExistingField(typeInfo.id, name, desc);
    if (hasInfo(fieldId)) {
      return getField(fieldId);
    }
    return null;
  }

  /**
   * Resolves a field reference and returns it if found. If not found, create a new FieldId for the
   * reference and return that instead.
   */
  public FieldId resolveOrAddFieldReference(String owner, String name, String desc) {
    TypeId typeId = this.ids.getIdOfType(owner);
    FieldInfo fieldInfo = resolveField(typeId, name, desc);
    if (fieldInfo != null) {
      return fieldInfo.id;
    }

    return ids.getIdOfField(typeId, name, desc);
  }

  /**
   * Resolves a method reference and returns it if found. If not found, create a new MethodId for
   * the reference and return that instead.
   */
  public Id resolveOrAddMethodReference(String owner, String name, String desc) {
    TypeId typeId = this.ids.getIdOfType(owner);
    MethodInfo methodInfo = resolveMethod(typeId, name, desc);
    if (methodInfo != null) {
      return methodInfo.id;
    }

    return ids.getIdOfMethod(typeId, name, desc);
  }

  /**
   * Resolves a field reference to an info. Searches typeId, super interfaces of typeId then super
   * classes of typeId. Returns null if the reference could not be resolved. See 5.4.3.2 Field
   * Resolution of the JVM spec for details.
   */
  public FieldInfo resolveField(TypeId typeId, String name, String desc) {
    if (hasInfo(typeId)) {
      return resolveField(getType(typeId), name, desc);
    }
    return null;
  }

  /**
   * Resolves a method reference to an info. Searches typeInfo, super classes of typeInfo and super
   * interfaces of typeInfo. Returns null if the reference could not be resolved. See 5.4.3.3 Method
   * Resolution of the JVM spec for details.
   */
  public MethodInfo resolveMethod(TypeInfo typeInfo, String name, String desc) {

    // check this class
    MethodInfo result = lookupMethod(typeInfo, name, desc);
    if (result != null) {
      return result;
    }

    // check super class
    result = resolveMethod(typeInfo.superId, name, desc);
    if (result != null) {
      return result;
    }

    // check direct super interfaces
    for (TypeId interfaceId : typeInfo.interfaces) {
      result = resolveMethod(interfaceId, name, desc);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  /**
   * Returns a method in typeInfo matching name and desc if one exists. Returns null if no match is
   * found. Does not search super types/interfaces.
   */
  public MethodInfo lookupMethod(TypeId type, String name, String desc) {
    if (hasInfo(type)) {
      return lookupMethod(getType(type), name, desc);
    }
    return null;
  }

  /**
   * Returns a method in typeInfo matching name and desc if one exists. Returns null if no match is
   * found. Does not search super types/interfaces.
   */
  public MethodInfo lookupMethod(TypeInfo typeInfo, String name, String desc) {
    MethodId methodId = ids.getExistingMethod(typeInfo.id, name, desc);
    if (hasInfo(methodId)) {
      return getMethod(methodId);
    }
    return null;
  }

  /**
   * Resolves a method reference to an info. Searches typeId, super classes of typeId and super
   * interfaces of typeId. Returns null if the reference could not be resolved. See 5.4.3.3 Method
   * Resolution of the JVM spec for details.
   */
  public MethodInfo resolveMethod(TypeId typeId, String name, String desc) {
    if (hasInfo(typeId)) {
      return resolveMethod(getType(typeId), name, desc);
    }
    return null;
  }

  /**
   * Returns true if Info's exist for type, type's super classes and type's implemented interfaces.
   *
   * CONSIDER: cache positive results
   */
  public boolean canResolveInheritance(final TypeId type) {
    return findFirstMissingTypeInInheritance(type) == null;
  }

  /**
   * Searches the inheritance hierarchy of the given type and returns the first type for which no
   * TypeInfo is available. Returns null if every type in the inheritance hierarchy for the type are
   * available.
   */
  public TypeId findFirstMissingTypeInInheritance(final TypeId type) {
    if (type == null) {
      return null;
    }

    if (!hasInfo(type)) {
      return type;
    }

    TypeInfo info = getType(type);
    TypeId missing = findFirstMissingTypeInInheritance(info.superId);
    if (missing != null) {
      return missing;
    }

    for (TypeId iface : info.interfaces) {
      missing = findFirstMissingTypeInInheritance(iface);
      if (missing != null) {
        return null;
      }
    }
    return null;
  }

  /**
   * Returns the info for type's direct super class. Returns null if the type is object or an
   * interface.
   */

  public TypeInfo getSuperInfo(final TypeInfo type) {
    return type.superId == null ? null : getType(type.superId);
  }


  /**
   * Returns the infos for the type's directly implemented interfaces.
   */
  public Iterable<TypeInfo> getInterfaceInfos(final TypeInfo type) {
    return Sequence.select(Sequence.toSequence(type.interfaces),
        new Func<TypeId, TypeInfo>() {

          @Override public TypeInfo apply(TypeId arg) {
            return getType(arg);
          }
        });
  }


  /**
   * Is derived a direct or indirect subclass of base.
   */
  public boolean isSubclassOf(final TypeInfo derived, final TypeInfo base) {
    for (TypeInfo test = derived; test != null; test = getSuperInfo(test)) {
      if (base == test) {
        return true;
      }
    }

    return false;
  }


  /**
   * Does type implement iface directly or indirectly.
   */
  public boolean implementsInterface(final TypeInfo type, final TypeInfo iface) {
    if (type == iface) {
      return true;
    }
    for (TypeInfo test = type; test != null; test = getSuperInfo(test)) {
      for (TypeInfo ifaceTest : getInterfaceInfos(test)) {
        if (ifaceTest == iface) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Returns the Info for java/lang/Object.
   */
  public TypeInfo getObject() {
    return getType(ids.object);
  }

  /**
   * Do we have an info for java/lang/Object?
   */
  public boolean hasObject() {
    return hasInfo(ids.object);
  }

  /**
   * Is a value of type from assignable to a variable of type to.
   */
  public boolean isAssignableFrom(final TypeInfo to, final TypeInfo from) {
    if (isSubclassOf(from, to)) {
      return true;
    }

    if (implementsInterface(from, to)) {
      return true;
    }

    if (from.isInterface() && to.id == ids.object) {
      return true;
    }

    return false;
  }


  /**
   * Return common super class of type1 and type2. This mirrors the implementation in the asm
   * library.
   */
  public TypeInfo getCommonSuperClass(final TypeInfo type1, final TypeInfo type2) {
    if (isAssignableFrom(type2, type1)) {
      return type2;
    }
    if (isAssignableFrom(type1, type2)) {
      return type1;
    }
    if (type1.isInterface() || type2.isInterface()) {
      return getObject();
    }

    TypeInfo test1 = type1;
    do {
      test1 = getSuperInfo(test1);
    } while (!isAssignableFrom(test1, type2));
    return test1;
  }

  // NOTE: this only works for class/interface types. It does not work for
  // arrays or primitives.

  /**
   * Return common super class of type1 and type2. This mirrors the implementation in the asm
   * library.
   */
  public String getCommonSuperClass(final String type1, final String type2) {
    return getCommonSuperClass(
        getType(ids.getExistingType(type1)), getType(ids.getExistingType(type2))).toString();
  }
}
