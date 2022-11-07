package edu.berkeley.cs.jqf.instrument.instrumentTest;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class FuzzAnnotationCFT implements ClassFileTransformer {

    public static String RUN_WITH_CLASS = "Lorg/junit/runner/RunWith;";
    public static String JQF_TYPE = "edu/berkeley/cs/jqf/fuzz/JQF";
    public static String CONFIGURATION_DESC = "(Lorg/apache/hadoop/conf/Configuration;)V";
    public static String QUICK_CHECK_FROM_CLASS = "Lcom/pholser/junit/quickcheck/From;";
    public static String CONFIGURATION_CLASS = "Lorg/apache/hadoop/conf/Configuration;";
    public static String FUZZ_CLASS = "Ledu/berkeley/cs/jqf/fuzz/Fuzz;";

    public static class FuzzAnnotationClassVisitor extends ClassVisitor {
        private Boolean runWithByASM = false;
        public FuzzAnnotationClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if(desc.equals(RUN_WITH_CLASS)) {
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
            AnnotationVisitor av = this.visitAnnotation(RUN_WITH_CLASS, true);
            av.visit("value", Type.getObjectType(JQF_TYPE));
            av.visitEnd();
            super.visitEnd();
        }

        /*
        (1) Replace @Test with @Fuzz
        (2) Add (@From(ConfigurationGenerator.class) Configuration generated) as argument
         */
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv;
            if(name.equals(Utils.getCurrentMethod())) {
                mv = new FuzzAnnotationMethodVisitor(super.visitMethod(access, name, CONFIGURATION_DESC,
                        signature, exceptions));
            } else {
                 mv = super.visitMethod(access, name, desc, signature, exceptions);
            }
            return mv;
        }

        private static class FuzzAnnotationMethodVisitor extends MethodVisitor {
            public FuzzAnnotationMethodVisitor(MethodVisitor mv) {
                super(Opcodes.ASM9, mv);
            }

            @Override
            public void visitCode() {
                AnnotationVisitor annotationVisitor0;
                annotationVisitor0 = super.visitTypeAnnotation(22, null, QUICK_CHECK_FROM_CLASS, true);
                annotationVisitor0.visit("value", Type.getType(CONFIGURATION_CLASS));
                annotationVisitor0.visitEnd();

                super.visitAnnotableParameterCount(1, true);

                annotationVisitor0 = super.visitParameterAnnotation(0, QUICK_CHECK_FROM_CLASS, true);
                annotationVisitor0.visit("value", Type.getType("Lorg/apache/hadoop/conf/ConfigurationGenerator;"));
                annotationVisitor0.visitEnd();

                super.visitCode();
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return super.visitAnnotation(FUZZ_CLASS, true);
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
            //Debug.write("/Users/alenwang/Documents/xlab/fuzz-hadoop/hadoop-common-project/hadoop-common/auto.class", classWriter.toByteArray());
            return classWriter.toByteArray();
        }
        return classfileBuffer;
    }
}
