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

import codecleaver.util.DirectedGraph;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Builds the graph of dependencies from the java infos. Every use of an Id x by an info y
 * adds a dependency from y to x.
 */
public class DependencyGraphBuilder implements IdClassVisitor {
  private TypeId currentTypeId;
  private TypeInfo currentTypeInfo;
  private String currentFileName;

  private final IdTable ids;
  private final InfoTable infos;
  private final DirectedGraph<Id> inheritanceGraph;
  private final DirectedGraph<Id> result;

  public DependencyGraphBuilder(InfoTable infos, DirectedGraph<Id> inheritanceGraph) {
    this.ids = infos.ids;
    this.infos = infos;
    this.inheritanceGraph = inheritanceGraph;
    this.result = new DirectedGraph<Id>();
  }
  
  public final DirectedGraph<Id> getResult() {
    return result;
  }

  public void visitFile(String file) {
    this.currentFileName = file;
  }

  private void setIgnoredClass(TypeId id) {
    currentTypeId = null;
    currentTypeInfo = null;
  }
  
  private boolean isIgnoredClass() {
    return currentTypeId == null;
  }

  // ignore classes which already have a definition, or which have an outer class from a different
  // jar file.
  private boolean shouldIgnoreClass(TypeId id) {
    if (!infos.hasInfo(id) || !infos.getType(id).getFileName().equals(currentFileName)) {
      return true;
    }
    
    TypeId outerId = id; 
    do {
      outerId = ids.getOuterType(outerId);
      if (outerId != null && infos.hasInfo(outerId)) {
        TypeInfo outerInfo = infos.getType(outerId);
        if (!outerInfo.getFileName().equals(currentFileName)) {
          return true;
        }
      }
    } while (outerId != null);
    
    return false;
  }

  private void addImplements(TypeId classId, TypeId interfaceId) {
    result.addEdge(classId, interfaceId);
  }

  private void addExtends(TypeId classId, TypeId superId) {
    if (superId != null) {
      result.addEdge(classId, superId);
    }
  }
  
  /**
   * Types are dependent on their base class, implemented interfaces and outer class (if any).
   */
  @Override public void visit(TypeId id,
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {

    if (shouldIgnoreClass(id)) {
      setIgnoredClass(id);
      return;
    }

    currentTypeId = id;
    currentTypeInfo = infos.getType(id);

    addExtends(currentTypeId, currentTypeInfo.superId);
    for (TypeId interfaceId : currentTypeInfo.interfaces) {
      addImplements(currentTypeId, interfaceId);
    }

    if (signature != null) {
      // TODO(peterhal): signature contains generic bases & interfaces
      // throw new IllegalArgumentException();
    }
  }

  @Override public void visitEnd() {
    currentTypeId = null;
    currentTypeInfo = null;
  }

  /**
   * Fields are dependent on their type.
   */
  @Override public FieldVisitor visitField(FieldId id,
      int access,
      String name,
      String desc,
      String signature,
      Object value) {

    if (isIgnoredClass()) {
      return null;
    }

    result.addEdge(id, currentTypeId);
    result.addOptionalEdge(id, ids.idOfDescriptor(desc));

    if (signature != null) {
      // TODO(peterhal): throw new IllegalArgumentException();
    }

    return null;
  }

  /**
   * Methods are dependent on all types in their signature and throws clauses, as well as anything
   * referenced by their bytecode. Types are dependent on their static initializers.
   */
  @Override public MethodVisitor visitMethod(MethodId methodId,
      int access,
      String name,
      String desc,
      String signature,
      String[] exceptions) {

    if (isIgnoredClass()) {
      return null;
    }

    result.addEdge(methodId, currentTypeId);
    result.addOptionalEdge(methodId, ids.getIdOfType(Type.getReturnType(desc)));
    for (Type argumentType : Type.getArgumentTypes(desc)) {
      result.addOptionalEdge(methodId, ids.getIdOfType(argumentType));
    }

    if (infos.getMethod(methodId).isStaticInitializer()) {
      result.addEdge(currentTypeId, methodId);
    }

    if (exceptions != null) {
      for (String exceptionName : exceptions) {
        result.addEdge(methodId, ids.getIdOfType(exceptionName));
      }
    }

    if (signature != null) {
      // TODO(peterhal): generics
    }

    if (inheritanceGraph.containsVertex(methodId)) {
      // This method overrides an inherited method.
      // The thinking being that a type requires all of its override methods to implement
      // its contracts whether the contract is an implemented interface or a base class.
      // It turns out that this dependency is problematic for some clients and can be worked around
      // using the Overrides predefined set.
      //
      // TODO(peterhal): consider adding this back in as we get more feedback.
      // graph.addEdge(currentTypeId, methodId);
    }
    
    return new GraphBuilderMethodVisitor(ids, infos, result, methodId);
  }

  @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return null;
  }

  private void addOuterClass(String innerName, String outerName) {
    TypeId innerId = ids.getExistingType(innerName);
    TypeId outerId = ids.getExistingType(outerName);

    // outerId can be null, for example:
    // javax/swing/html/parser/CUP$parser$actions
    if (outerId != null && innerId != null) {
      // InnerClass attributes can occur in either the inner or the outer type
      // and they can also appear in class files for completely unrelated types. No I am not making
      // this up. Discard InnerClass attributes not in either the inner or outer type.
      if (innerId != this.currentTypeId && outerId != this.currentTypeId) {
        return;
      }
      // This shouldn't happen ... just being defensive.
      TypeInfo outerInfo = infos.getOptionalType(outerId);
      TypeInfo innerInfo = infos.getOptionalType(innerId);
      if (outerInfo == null || innerInfo == null) {
        return;
      }
      if (!outerInfo.getFileName().equals(currentTypeInfo.getFileName())) {
        throw new RuntimeException(String.format("Inner Type '%s' contained in file '%s' is in a different file than outer type '%s' in file '%s'.",
            innerId,
            currentTypeInfo.getFileName(),
            outerId,
            outerInfo.getFileName()));
      }

      // innner classes depend on their containing class
      result.addEdge(innerId, outerId);
      
      // Nested types are created parent-ed to their containing package. Re-parent them here to 
      // their containing type.
      innerInfo.setParent(outerInfo);
    }
  }

  @Override public void visitInnerClass(
      String name, String outerName, String innerName, int access) {

    if (isIgnoredClass()) {
      return;
    }
    // anonymous inner classes have null outerName
    if (outerName != null) {
      addOuterClass(name, outerName);
    }
  }

  @Override public void visitOuterClass(String owner, String name, String desc) {

    if (isIgnoredClass()) {
      return;
    }
    addOuterClass(currentTypeId.name, owner);
  }
}
