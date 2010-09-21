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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class IdClassReader implements ClassVisitor {

  private final IdTable ids;
  private final ClassReader reader;
  private IdClassVisitor visitor;
  private TypeId currentTypeId;

  public static void visitJarFile(IdTable ids, IdClassVisitor visitor, String fileName)
      throws StateException {
    try {
      ZipFile file = null;
      try {
        file = new ZipFile(fileName);

        visitor.visitFile(fileName);

        for (Enumeration<? extends ZipEntry> entries = file.entries(); entries.hasMoreElements();) {
          ZipEntry entry = entries.nextElement();
          if (entry.getName().endsWith(".class")) {
            visitClassFile(ids, visitor, file.getInputStream(entry));
          }
        }
      } finally {
        if (file != null) {
          file.close();
        }
      }
    } catch (IOException e) {
      throw new StateException(e, fileName);
    }
  }

  public static void visitClassFile(IdTable ids, IdClassVisitor visitor, InputStream classFile)
      throws IOException {
    IdClassReader reader = new IdClassReader(ids, new ClassReader(classFile));
    reader.accept(visitor);
  }

  public void accept(IdClassVisitor visitor) {
    this.visitor = visitor;
    this.reader.accept(this, 0);
  }

  public IdClassReader(IdTable ids, ClassReader reader) {
    this.ids = ids;
    this.reader = reader;
  }

  @Override public void visit(int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {
    currentTypeId = ids.getIdOfType(name);
    visitor.visit(currentTypeId, version, access, name, signature, superName, interfaces);
  }

  @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return visitor.visitAnnotation(desc, visible);
  }

  @Override public void visitAttribute(Attribute attr) {}

  @Override public void visitEnd() {
    visitor.visitEnd();
    currentTypeId = null;
  }

  @Override public FieldVisitor visitField(
      int access, String name, String desc, String signature, Object value) {
    FieldId fieldId = ids.getIdOfField(currentTypeId, name, desc);
    return visitor.visitField(fieldId, access, name, desc, signature, value);
  }

  @Override public void visitInnerClass(
      String name, String outerName, String innerName, int access) {
    visitor.visitInnerClass(name, outerName, innerName, access);
  }

  @Override public MethodVisitor visitMethod(
      int access, String name, String desc, String signature, String[] exceptions) {
    MethodId methodId = ids.getIdOfMethod(currentTypeId, name, desc);
    return visitor.visitMethod(methodId, access, name, desc, signature, exceptions);
  }

  // owner, name and desc are the containing method for an anonymous inner class
  @Override public void visitOuterClass(String owner, String name, String desc) {
    visitor.visitOuterClass(owner, name, desc);
  }

  @Override public void visitSource(String source, String debug) {}

}
