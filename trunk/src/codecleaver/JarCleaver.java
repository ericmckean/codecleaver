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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarCleaver {
  private Set<Id> whiteList;
  private final InfoTable infos;
  private final IdTable ids;

  private class CleaveAdapter extends ClassAdapter {
    TypeId classId;

    public CleaveAdapter(TypeId classId, ClassVisitor cv) {
      super(cv);
      if (classId == null) {
        throw new IllegalArgumentException("Invalid class id");
      }
      this.classId = classId;
    }

    @Override public void visit(int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces) {
      boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
      if (isInterface) {
        access |= Opcodes.ACC_ABSTRACT;
      } else {
        access |= Opcodes.ACC_SUPER;
      }
      cv.visit(Opcodes.V1_6, access, name, signature, superName, interfaces);
    }

    @Override public FieldVisitor visitField(
        int access, String name, String desc, String signature, Object value) {
      Id id = ids.getExistingField(classId, name, desc);
      if (whiteList.contains(id)) {
        return cv.visitField(access, name, desc, signature, value);
      } else {
        return null;
      }
    }

    @Override public MethodVisitor visitMethod(
        int access, String name, String desc, String signature, String[] exceptions) {
      Id id = ids.getExistingMethod(classId, name, desc);
      if (whiteList.contains(id)) {
        return cv.visitMethod(access, name, desc, signature, exceptions);
      } else {
        return null;
      }
    }

  }

  private class CleaveWriter extends ClassWriter {

    CleaveWriter(int flags) {
      super(flags);
    }

    @Override protected String getCommonSuperClass(final String type1, final String type2) {
      return infos.getCommonSuperClass(type1, type2);
    }
  }

  private byte[] convert(TypeId classid, byte[] cl) {
    ClassReader cr = new ClassReader(cl);
    ClassWriter cw = new CleaveWriter(ClassWriter.COMPUTE_FRAMES);
    ClassAdapter ca = new CleaveAdapter(classid, cw);
    cr.accept(ca, ClassReader.SKIP_FRAMES);
    return cw.toByteArray();
  }

  public JarCleaver(InfoTable infos) {
    this.ids = infos.ids;
    this.infos = infos;
  }

  public void cleave(String inputFileName, String outputFileName, Iterable<Id> whiteListIterable)
      throws IOException {
    // Build the white list as a set.
    whiteList = new HashSet<Id>();
    Sequence.addAll(whiteList, whiteListIterable);

    // Set input and output streams to the corresponding files.
    JarInputStream in = new JarInputStream(new FileInputStream(inputFileName));
    try {
      Manifest manifest = in.getManifest();
      cleaveManifest(manifest);
      JarOutputStream out = new JarOutputStream(new FileOutputStream(outputFileName), manifest);
      try {
        for (JarEntry entry = in.getNextJarEntry(); entry != null; entry = in.getNextJarEntry()) {

          String name = entry.getName();
          byte[] input = getBytesofJarEntry(in);
          byte[] output = convertJarEntry(name, input);

          if (output != null) {
            writeJarEntry(out, name, output);
          }
        }
      } finally {
        out.close();
      }
    } catch (MissingInfoException exception) {
      new File(outputFileName).delete();
      throw exception;
    } finally {
      in.close();
    }
  }

  private void cleaveManifest(Manifest manifest) {
    if (manifest != null) {
      String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
      if (mainClass != null) {
        Id mainClassId = ids.getExistingType(mainClass.replace('.', '/'));
        if (mainClassId == null || !whiteList.contains(mainClassId)) {
          manifest.getMainAttributes().remove(Attributes.Name.MAIN_CLASS);
        }
      }
    }
  }

  private byte[] convertJarEntry(String name, byte[] input) {
    byte[] output = null;
    // Convert if a class file.
    if (name.endsWith(".class")) {
      String className = name.substring(0, name.length() - ".class".length());
      TypeId id = ids.getExistingType(className);
      // Keep only if on the white list.
      if (whiteList.contains(id)) {
        output = convert(id, input);
      }
    } else {
      // Pass through non-class resources.
      output = input;
    }
    return output;
  }

  private void writeJarEntry(JarOutputStream out, String name, byte[] output) throws IOException {
    JarEntry newEntry = new JarEntry(name);
    out.putNextEntry(newEntry);
    out.write(output, 0, output.length);
    out.closeEntry();
  }

  private byte[] getBytesofJarEntry(JarInputStream in) throws IOException {
    ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
    byte[] buffer = new byte[1000];
    while (true) {
      int nbytes = in.read(buffer);
      if (nbytes == -1) {
        break;
      }
      bytestream.write(buffer, 0, nbytes);
    }
    byte[] input = bytestream.toByteArray();
    return input;
  }
}
