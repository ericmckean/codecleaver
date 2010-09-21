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

import codecleaver.util.Action;
import codecleaver.util.Pair;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DefinitionBuilder implements IdClassVisitor {
  protected boolean isIgnore;
  protected TypeId currentTypeId;
  protected String currentFileName;
  protected TypeInfo currentTypeInfo;
  protected final IdTable ids;
  protected final InfoTable infos;
  private final HashSet<Pair<TypeId, String>> ignoredTypes;

  private int access;
  private TypeId superId;
  private TypeId[] interfaceIds;
  private ImmutableSet.Builder<AnnotationInfo> annotations;

  public DefinitionBuilder(IdTable ids, InfoTable infos) {
    this.ids = ids;
    this.infos = infos;
    this.ignoredTypes = new HashSet<Pair<TypeId, String>>();
  }
  
  public Iterable<Pair<TypeId, String>> getIgnoredTypes() {
    return ignoredTypes;
  }

  public void visitFile(String file) {
    this.currentFileName = file;
  }

  @Override public void visitEnd() {
    if (!isIgnore()) {
      getCurrentInfo();
    }
    isIgnore = false;
    currentTypeId = null;
    currentTypeInfo = null;
  }

  private boolean shouldIgnoreClass(TypeId id) {
    if (infos.hasInfo(id)) {
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

  @Override public void visit(TypeId id,
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {

    if (this.access != 0 || this.superId != null
        || this.interfaceIds != null || this.annotations != null
        || currentTypeId != null || isIgnore) {
      throw new RuntimeException();
    }
    if (shouldIgnoreClass(id)) {
      setIgnore(id);
      return;
    }

    isIgnore = false;
    currentTypeId = id;

    this.access = access;
    this.annotations = new ImmutableSet.Builder<AnnotationInfo>();
    this.superId = superName == null ? null : ids.getIdOfType(superName);

    this.interfaceIds = new TypeId[interfaces.length];
    for (int i = 0; i < interfaces.length; i++) {
      interfaceIds[i] = ids.getIdOfType(interfaces[i]);
    }

    if (signature != null) {
      // TODO(peterhal): signature contains generic bases & interfaces
      // throw new IllegalArgumentException();
    }
  }

  private void setIgnore(TypeId id) {
    ignoredTypes.add(new Pair<TypeId, String>(id, currentFileName));
    isIgnore = true;
    currentTypeId = null;
  }

  private boolean isIgnore() {
    return isIgnore;
  }
  
  private TypeInfo getCurrentInfo() {
    if (currentTypeInfo == null) {
      currentTypeInfo = infos.createType(currentFileName,
          currentTypeId,
          infos.getBestGuessContainerOf(currentTypeId),
          access,
          superId,
          interfaceIds,
          annotations.build());
      access = 0;
      superId = null;
      interfaceIds = null;
      annotations = null;
      isIgnore = false;
    }
    return currentTypeInfo;
  }

  @Override public FieldVisitor visitField(final FieldId id,
      final int access,
      final String name,
      final String desc,
      final String signature,
      final Object value) {

    if (isIgnore()) {
      return null;
    }

    final ImmutableSet.Builder<AnnotationInfo> annotations =
        new ImmutableSet.Builder<AnnotationInfo>();
    return
        new FieldVisitor() {

          @Override public AnnotationVisitor visitAnnotation(
              final String desc, final boolean visible) {
            return new AnnotationBuilder(desc, visible, annotations);
          }

          @Override public void visitAttribute(Attribute attr) {}

          @Override public void visitEnd() {
            infos.createField(id, getCurrentInfo(), access, desc, annotations.build());
          }
        };
  }

  @Override public MethodVisitor visitMethod(final MethodId id,
      final int access,
      final String name,
      final String desc,
      final String signature,
      final String[] exceptions) {

    if (isIgnore()) {
      return null;
    }

    final ImmutableSet.Builder<AnnotationInfo> annotations =
        new ImmutableSet.Builder<AnnotationInfo>();
    return
        new MethodVisitor() {

          @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationBuilder(desc, visible, annotations);
          }

          @Override public AnnotationVisitor visitAnnotationDefault() {
            return null;
          }

          @Override public void visitAttribute(Attribute attr) {}

          @Override public void visitCode() {}

          @Override public void visitEnd() {
            infos.createMethod(id, getCurrentInfo(), access, desc, annotations.build());
          }

          @Override public void visitFieldInsn(
              int opcode, String owner, String name, String desc) {}

          @Override public void visitFrame(
              int type, int nLocal, Object[] local, int nStack, Object[] stack) {}

          @Override public void visitIincInsn(int var, int increment) {}

          @Override public void visitInsn(int opcode) {}

          @Override public void visitIntInsn(int opcode, int operand) {}

          @Override public void visitJumpInsn(int opcode, Label label) {}

          @Override public void visitLabel(Label label) {}

          @Override public void visitLdcInsn(Object cst) {}

          @Override public void visitLineNumber(int line, Label start) {}

          @Override public void visitLocalVariable(String name,
              String desc,
              String signature,
              Label start,
              Label end,
              int index) {}

          @Override public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {}

          @Override public void visitMaxs(int maxStack, int maxLocals) {}

          @Override public void visitMethodInsn(
              int opcode, String owner, String name, String desc) {}

          @Override public void visitMultiANewArrayInsn(String desc, int dims) {}

          @Override public AnnotationVisitor visitParameterAnnotation(
              int parameter, String desc, boolean visible) {
            return null;
          }

          @Override public void visitTableSwitchInsn(
              int min, int max, Label dflt, Label[] labels) {}

          @Override public void visitTryCatchBlock(
              Label start, Label end, Label handler, String type) {}

          @Override public void visitTypeInsn(int opcode, String type) {}

          @Override public void visitVarInsn(int opcode, int var) {}
        };
  }

  private static final class ArrayAnnotationVisitor implements AnnotationVisitor {
    private final List<Object> values;
    private final Action<Object> onEnd;

    private ArrayAnnotationVisitor(Action<Object> onEnd) {
      this.values = new ArrayList<Object>();
      this.onEnd = onEnd;
    }

    @Override public void visit(String name, Object value) {
      values.add(value);
    }

    @Override public AnnotationVisitor visitAnnotation(String name, String desc) {
      // TODO(peterhal): nested annotation values
      return null;
    }

    @Override public AnnotationVisitor visitArray(String name) {
      return new ArrayAnnotationVisitor(new Action<Object>() {

        @Override public void invoke(Object value) {
          values.add(value);
        }});
    }

    @Override public void visitEnd() {
      onEnd.invoke(values.toArray());
    }

    @Override public void visitEnum(String name, String desc, String value) {
      values.add(value);
    }
  }

  private final class AnnotationBuilder implements AnnotationVisitor {
    private final ImmutableMap.Builder<String, Object> elements;
    private final TypeId type;
    private final ImmutableSet.Builder<AnnotationInfo> destination;

    public AnnotationBuilder(final String desc, final boolean visible,
        ImmutableSet.Builder<AnnotationInfo> destination) {
      this.elements = new ImmutableMap.Builder<String, Object>();
      this.type = ids.idOfDescriptor(desc);
      this.destination = destination;
    }

    @Override public void visit(String name, Object value) {
      elements.put(name, value);
    }

    @Override public AnnotationVisitor visitArray(final String name) {
      return new ArrayAnnotationVisitor(new Action<Object>() {

        @Override public void invoke(Object value) {
          elements.put(name, value);
          
        }});
    }

    @Override public void visitEnd() {
      destination.add(infos.getAnnotation(type, elements.build()));
    }

    @Override public void visitEnum(String name, String desc, String value) {
      elements.put(name, value);
    }

    @Override public AnnotationVisitor visitAnnotation(String name, String desc) {
      // TODO(peterhal): nested annotation values
      return null;
    }
  }

  @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    if (isIgnore()) {
      return null;
    }
    return new AnnotationBuilder(desc, visible, annotations);
  }

  @Override public void visitInnerClass(
    String name, String outerName, String innerName, int access) {
  }

  @Override public void visitOuterClass(String owner, String name, String desc) {
  }
}
