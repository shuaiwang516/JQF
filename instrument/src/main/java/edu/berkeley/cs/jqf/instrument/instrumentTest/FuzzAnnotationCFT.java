package edu.berkeley.cs.jqf.instrument.instrumentTest;

import edu.berkeley.cs.jqf.instrument.asm.*;
import edu.berkeley.cs.jqf.instrument.log.Log;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class FuzzAnnotationCFT implements ClassFileTransformer {

    public static class FuzzAnnotationClassVisitor extends ClassVisitor {
        private Boolean runWithByASM = false;
        public FuzzAnnotationClassVisitor(ClassVisitor cv) {
            super(Instr.ASM_API_VERSION, cv);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if(desc.equals("Lorg/junit/runner/RunWith;")) {
                // Test class itself already has RunWith annotation
                if (!runWithByASM) {
                    throw new RuntimeException("Test class already have RunWith Annotation, can't be fuzzed");
                }
            }
            return av;
        }

        @Override
        public void visitEnd() {
            runWithByASM = true;
            AnnotationVisitor av = this.visitAnnotation("Lorg/junit/runner/RunWith;", true);
            //av = av.visitArray("value");
            av.visit("value", Type.getObjectType("edu/berkeley/cs/jqf/fuzz/JQF"));
            av.visitEnd();
            super.visitEnd();
        }

        // Add @Fuzz to method

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if(name.equals(Utils.getCurrentMethod())) {
                mv = new FuzzAnnotationMethodVisitor(mv);
            }
            return mv;
        }

        private static class FuzzAnnotationMethodVisitor extends MethodVisitor {
            public FuzzAnnotationMethodVisitor(MethodVisitor mv) {
                super(Instr.ASM_API_VERSION, mv);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return super.visitAnnotation("Ledu/berkeley/cs/jqf/fuzz/Fuzz;", true);
            }
        }
    }

    public static byte[] transf(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(Utils.isTestClassTransformNeeded(className)) {
            ClassReader classReader = new ClassReader(classfileBuffer);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            FuzzAnnotationClassVisitor visitor = new FuzzAnnotationClassVisitor(classWriter);
            classReader.accept(visitor, 0);
            Debug.write("/Users/alenwang/Documents/xlab/fuzz-hadoop/hadoop-common-project/hadoop-common/debug.class", classWriter.toByteArray());
            Log.d2f("transf1");
            return classWriter.toByteArray();
            //return classfileBuffer;
        }
        Log.d2f("transf2");
        return null;
    }
}
