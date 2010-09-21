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
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A visitor to visit a Java class. The methods of this interface must be called in the following
 * order: <tt>visitFile</tt> (<tt>visit</tt> [ <tt>visitSource</tt> ] [ <tt>visitOuterClass</tt> ] (
 * <tt>visitAnnotation</tt> | <tt>visitAttribute</tt> )* (<tt>visitInnerClass</tt> |
 * <tt>visitField</tt> | <tt>visitMethod</tt> )* <tt>visitEnd</tt> )*.
 */
public interface IdClassVisitor {
  /**
   * Visit the containing jar file or directory.
   * 
   * @param fileName The name of the containing jar file or directory.
   */
  void visitFile(String fileName);

  /**
   * Visits the header of the class.
   *
   * @param version the class version.
   * @param access the class's access flags (see {@link org.objectweb.asm.Opcodes}). This parameter
   *        also indicates if the class is deprecated.
   * @param name the internal name of the class (see {@link org.objectweb.asm.Type#getInternalName()
   *        getInternalName}).
   * @param signature the signature of this class. May be <tt>null</tt> if the class is not a
   *        generic one, and does not extend or implement generic classes or interfaces.
   * @param superName the internal of name of the super class (see {@link
   *        org.objectweb.asm.Type#getInternalName() getInternalName}). For interfaces, the super
   *        class is {@link Object}. May be <tt>null</tt>, but only for the {@link Object} class.
   * @param interfaces the internal names of the class's interfaces (see {@link
   *        org.objectweb.asm.Type#getInternalName() getInternalName}). May be <tt>null</tt>.
   */
  void visit(TypeId id,
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces);

  /**
   * Visits an annotation of the class.
   *
   * @param desc the class descriptor of the annotation class.
   * @param visible <tt>true</tt> if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or <tt>null</tt> if this visitor is not
   *         interested in visiting this annotation.
   */
  AnnotationVisitor visitAnnotation(String desc, boolean visible);

  /**
   * Visit an EnclosingMethod attribute. Used for anonymous inner classes.
   * 
   * @param owner The enclosing class of this anonymous inner class.
   * @param name The method name of the enclosing method of this inner class. May be null.
   * @param desc The method desc of the enclosing method of this inner class. May be null.
   */
  void visitOuterClass(String owner, String name, String desc);

  /**
   * Visit an InnerClass attribute.
   * 
   * NOTE: 
   * 
   * InnerClass attributes can be placed in either the inner class's class file, the outer
   * class's class file, or on a class file completely unrelated to the named inner/outer types.
   * 
   * @param innerFullName Full name of the class with the attribute.
   * @param outerName Full name of the containing class. Null if named class is anonymous. 
   * @param innerUnqualifiedName Unqualified name of the inner class.
   * @param access Access flags of named class.
   */
  void visitInnerClass(
      String innerFullName, String outerName, String innerUnqualifiedName, int access);

  /**
   * Visits a field of the class.
   *
   * @param access the field's access flags (see {@link org.objectweb.asm.Opcodes}). This parameter
   *        also indicates if the field is synthetic and/or deprecated.
   * @param name the field's name.
   * @param desc the field's descriptor (see {@link org.objectweb.asm.Type Type}).
   * @param signature the field's signature. May be <tt>null</tt> if the field's type does not use
   *        generic types.
   * @param value the field's initial value. This parameter, which may be <tt>null</tt> if the field
   *        does not have an initial value, must be an {@link Integer}, a {@link Float}, a {@link
   *        Long}, a {@link Double} or a {@link String} (for <tt>int</tt>, <tt>float</tt> ,
   *        <tt>long</tt> or <tt>String</tt> fields respectively). <i>This parameter is only used
   *        for static fields</i>. Its value is ignored for non static fields, which must be
   *        initialized through bytecode instructions in constructors or methods.
   * @return a visitor to visit field annotations and attributes, or <tt>null</tt> if this class
   *         visitor is not interested in visiting these annotations and attributes.
   */
  FieldVisitor visitField(FieldId id,
      int access,
      String name,
      String desc,
      String signature,
      Object value);

  /**
   * Visits a method of the class. This method <i>must</i> return a new {@link MethodVisitor}
   * instance (or <tt>null</tt>) each time it is called, i.e., it should not return a previously
   * returned visitor.
   *
   * @param access the method's access flags (see {@link org.objectweb.asm.Opcodes}). This parameter
   *        also indicates if the method is synthetic and/or deprecated.
   * @param name the method's name.
   * @param desc the method's descriptor (see {@link org.objectweb.asm.Type Type}).
   * @param signature the method's signature. May be <tt>null</tt> if the method parameters, return
   *        type and exceptions do not use generic types.
   * @param exceptions the internal names of the method's exception classes (see {@link
   *        org.objectweb.asm.Type#getInternalName() getInternalName}). May be <tt>null</tt>.
   * @return an object to visit the byte code of the method, or <tt>null</tt> if this class visitor
   *         is not interested in visiting the code of this method.
   */
  MethodVisitor visitMethod(MethodId id,
      int access,
      String name,
      String desc,
      String signature,
      String[] exceptions);

  void visitEnd();
}
