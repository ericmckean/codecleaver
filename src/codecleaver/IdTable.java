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

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Set;

/**
 * A scope and factory for Ids. getIdOfX methods will return an existing Id if found or create a new
 * Id if a matching Id has not already been created. getExistingX will return an existing Id if a
 * matching Id has already been created or will return null if not matching Id has been created.
 *
 * <p>Within the scope of an IdTable identity and equality operations should produce the same
 * results. Ids from different IdTables should not be expected to interoperate.
 */
public class IdTable {
  // TODO(peterhal): use a Map<String, Id> to avoid allocations during lookup 
  public final HashMap<Id, Id> ids = new HashMap<Id, Id>();
  public final PackageId root;
  public final TypeId object;

  public IdTable() {
    root = getUniqueId(new PackageId());
    object = getIdOfType("java/lang/Object");
  }

  /**
   * Returns all the Ids created by this table.
   */
  public Set<Id> getAll() {
    return ids.keySet();
  }

  /**
   * Get an existing or create a package Id.
   */
  public PackageId getIdOfPackage(String packageInternalName) {
    return getUniqueId(
        new PackageId(ensureParentPackage(packageInternalName), packageInternalName));
  }

  /**
   * Get an existing or create a type Id.
   */
  public TypeId getIdOfType(String typeInternalName) {
    return getUniqueId(new TypeId(ensureParentPackage(typeInternalName), typeInternalName));
  }

  /**
   * Get an existing or create a package Id which is the parent of the named id.
   */
  private PackageId ensureParentPackage(String name) {
    int length = name.lastIndexOf('/');
    if (length == -1) {
      return root;
    }
    return getIdOfPackage(name.substring(0, length));
  }

  /**
   * Get an existing or create a field Id.
   */
  public FieldId getIdOfField(TypeId classId, String name, String signature) {
    return getUniqueId(new FieldId(classId, name, signature));
  }

  /**
   * Get an existing or create a method Id.
   */
  public MethodId getIdOfMethod(TypeId classId, String name, String desc) {
    return getUniqueId(new MethodId(classId, name, desc));
  }

  /**
   * Get an existing package Id. Returns null if an existing package Id has not been created.
   */
  public PackageId getExistingPackage(String value) {
    return getExistingId(new PackageId(null, value));
  }

  /**
   * Get an existing type Id. Returns null if an existing type Id has not been created.
   */
  public TypeId getExistingType(String value) {
    return getExistingId(new TypeId(null, value));
  }

  /**
   * Get an existing field Id. Returns null if an existing field Id has not been created.
   */
  public FieldId getExistingField(TypeId classId, String name, String signature) {
    return getExistingId(new FieldId(classId, name, signature));
  }

  /**
   * Get an existing method Id. Returns null if an existing method Id has not been created.
   */
  public MethodId getExistingMethod(TypeId classId, String name, String desc) {
    return getExistingId(new MethodId(classId, name, desc));
  }

  /**
   * Get or create a type Id from an asm type. If type represents an array, then returns a TypeId
   * for the element type of the array. Returns null if type is not an object or array type.
   */
  public TypeId getIdOfType(Type type) {
    switch (type.getSort()) {
      case Type.ARRAY:
        return getIdOfType(type.getElementType());
      case Type.OBJECT:
        return getIdOfType(type.getInternalName());
      default:
        return null;
    }
  }

  /**
   * Returns the containing type id for an existing typeId. For example, given nested
   * classes A, A.B, and A.B.C, the outer types are null, A, and A.B respectively.
   * 
   * @param typeId
   * @return The containing type of a given type.
   */
  public TypeId getOuterType(TypeId typeId) {
    for (int index = typeId.name.length(); (index = typeId.name.lastIndexOf('$', index - 1)) != -1;) {
      TypeId result = getExistingType(typeId.name.substring(0, index));
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the outermost containing type id for an existing typeId. For example, given nested
   * classes A, A.B, and A.B.C, the outermost type of all three classes is A.
   * 
   * @param typeId
   * @return The outermost containing type of a given type.
   */
  public TypeId getOutermostType(TypeId typeId) {
    for (int index = -1; (index = typeId.name.indexOf('$', index + 1)) != -1;) {
      TypeId result = getExistingType(typeId.name.substring(0, index));
      if (result != null) {
        return result;
      }
    }
    return typeId;
  }

  /**
   * Get or create a type Id from a type descriptor. Class descriptors include the 'L' prefix and
   * ';' suffix. Array descriptors include the '[' prefix. If typeName represents a primitive type
   * then null is returned.
   */
  public Id idOfObjectType(String typeName) {
    Type type = Type.getObjectType(typeName);
    return getIdOfType(type);
  }

  /**
   * Get or create a type Id from a type descriptor. Class descriptors include the 'L' prefix and
   * ';' suffix. Array descriptors include the '[' prefix. If typeName represents a primitive type
   * then null is returned.
   */
  public TypeId idOfDescriptor(String descriptor) {
    Type type = Type.getType(descriptor);
    return getIdOfType(type);
  }


  /**
   * Returns an existing Id if one has been added to the table. Otherwise add newId to the table
   * and return newId.
   * @param <T> the type of Id to match
   * @param newId the id to match. The id is matched based on the string of the id.
   */
  @SuppressWarnings("unchecked")
  private <T extends Id> T getUniqueId(T newId) {
    if (ids.containsKey(newId)) {
      return (T) ids.get(newId);
    } else {
      ids.put(newId, newId);
      newId.attachToParent();
      return newId;
    }
  }

  /**
   * Returns an existing Id or null if no matching Id has been created by this IdTable.
   * @param <T> the type of Id to match
   * @param id the id to match. The id is matched based on the string of the id.
   */
  @SuppressWarnings("unchecked")
  private <T extends Id> T getExistingId(T id) {
    if (ids.containsKey(id)) {
      return (T) ids.get(id);
    } else {
      return null;
    }
  }
}
