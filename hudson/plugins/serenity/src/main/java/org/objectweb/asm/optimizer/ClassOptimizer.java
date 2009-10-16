/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2007 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.asm.optimizer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

/**
 * A {@link ClassAdapter} that renames fields and methods, and removes debug
 * info.
 * 
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public class ClassOptimizer extends RemappingClassAdapter {

    private String pkgName;

    public ClassOptimizer(final ClassVisitor cv, final Remapper remapper) {
        super(cv, remapper);
    }

    // ------------------------------------------------------------------------
    // Overridden methods
    // ------------------------------------------------------------------------

    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces)
    {
        super.visit(version, access, name, null, superName, interfaces);
        pkgName = name.substring(0, name.lastIndexOf('/'));
    }

    public void visitSource(final String source, final String debug) {
        // remove debug info
    }

    public void visitOuterClass(
        final String owner,
        final String name,
        final String desc)
    {
        // remove debug info
    }

    public AnnotationVisitor visitAnnotation(
        final String desc,
        final boolean visible)
    {
        // remove annotations
        return null;
    }

    public void visitAttribute(final Attribute attr) {
        // remove non standard attributes
    }

    public void visitInnerClass(
        final String name,
        final String outerName,
        final String innerName,
        final int access)
    {
        // remove debug info
    }

    public FieldVisitor visitField(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final Object value)
    {
        String s = remapper.mapFieldName(className, name, desc);
        if ("-".equals(s)) {
            return null;
        }
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            if ((access & Opcodes.ACC_FINAL) != 0
                    && (access & Opcodes.ACC_STATIC) != 0 && desc.length() == 1)
            {
                return null;
            }
            if ("org/objectweb/asm".equals(pkgName) && s.equals(name)) {
                System.out.println("INFO: " + s + " could be renamed");
            }
            super.visitField(access, name, desc, null, value);
        } else {
            if (!s.equals(name)) {
                throw new RuntimeException("The public or protected field "
                        + className + '.' + name + " must not be renamed.");
            }
            super.visitField(access, name, desc, null, value);
        }
        return null; // remove debug info
    }

    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        String s = remapper.mapMethodName(className, name, desc); 
        if ("-".equals(s)) {
            return null;
        }
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            if ("org/objectweb/asm".equals(pkgName) && !name.startsWith("<")
                    && s.equals(name))
            {
                System.out.println("INFO: " + s + " could be renamed");
            }
            return super.visitMethod(access, name, desc, null, exceptions); 
        } else {
            if (!s.equals(name)) {
                throw new RuntimeException("The public or protected method "
                        + className + '.' + name + desc
                        + " must not be renamed.");
            }
            return super.visitMethod(access, name, desc, null, exceptions); 
        }
    }
    
    protected MethodVisitor createRemappingMethodAdapter(
        int access,
        String newDesc,
        MethodVisitor mv)
    {
        return new MethodOptimizer(access, newDesc, mv, remapper); 
    }
}
