package org.jvnet.hudson;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Java agent that makes all classes and methods non-final.
 *
 * @author Kohsuke Kawaguchi
 */
public class NonFinalTransformer implements ClassFileTransformer {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Installing non-final class transformer");
        inst.addTransformer(new NonFinalTransformer());
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        // performance improvement. 
        if(className.startsWith("java/") || className.startsWith("javax/"))
            return classfileBuffer;

//        System.out.println("Transforming "+className);

        ClassReader r = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(r,0) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access&=~Opcodes.ACC_FINAL, name, signature, superName, interfaces);
            }

            public MethodVisitor visitMethod(int access, String method, String desc, String signature, String[] exceptions) {
                return super.visitMethod(access&=~Opcodes.ACC_FINAL, method, desc, signature, exceptions);
            }
        };
        r.accept(cw,0);
        return cw.toByteArray();
    }
}
