package edu.berkeley.cs.jqf.fuzz.instrumentTest;

import edu.berkeley.cs.jqf.fuzz.asm.*;

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
                } else {
                    av = new FuzzAnnotationVisitor(av);
                }
            }
            return av;
        }

        @Override
        public void visitEnd() {
            runWithByASM = true;
            AnnotationVisitor av = this.visitAnnotation("Lorg/junit/jupiter/api/extension/ExtendWith;", true);
            av = av.visitArray("value");
            av.visitEnd();
            super.visitEnd();
        }

        private static class FuzzAnnotationVisitor extends AnnotationVisitor {
            public FuzzAnnotationVisitor(AnnotationVisitor av) {
                super(Instr.ASM_API_VERSION, av);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                AnnotationVisitor av = super.visitArray(name);
                if (name.equals("value")) {
                    av = new FuzzArrayAnnotationVisitor(av);
                }
                return av;
            }

            private static class FuzzArrayAnnotationVisitor extends AnnotationVisitor {
                public FuzzArrayAnnotationVisitor(AnnotationVisitor av) {
                    super(Instr.ASM_API_VERSION, av);
                }

                @Override
                public void visitEnd() {
                    visit("null", Type.getObjectType("edu/berkeley/cs/jqf/fuzz/JQF"));
                    super.visitEnd();
                }
            }

        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(Utils.isTestClassTransformNeeded(className)) {
            ClassReader classReader = new ClassReader(classfileBuffer);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            FuzzAnnotationClassVisitor visitor = new FuzzAnnotationClassVisitor(classWriter);
            classReader.accept(visitor, 0);
            Debug.write("/Users/alenwang/Documents/xlab/junit4_demo/debugFile", classWriter.toByteArray());
            throw new RuntimeException("Haha");
            //return classWriter.toByteArray();
        }
        throw new RuntimeException("Haha");
        //return null;
    }
}
