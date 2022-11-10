package org.conffuzz.internal.agent;

import org.objectweb.asm.*;

public class FuzzTargetGeneratingClassVisitor extends ClassVisitor {
    public static final String TARGET_SUFFIX = "$$CONFFUZZ";
    private static final String RUN_WITH_DESCRIPTOR = "Lorg/junit/runner/RunWith;";
    private static final Type JQF_TYPE = Type.getObjectType("edu/berkeley/cs/jqf/fuzz/JQF");
    private static final String TARGET_METHOD_DESCRIPTOR = "(Ljava/lang/Object;)V";
    private static final String FROM_DESCRIPTOR = "Lcom/pholser/junit/quickcheck/From;";
    private static final String FUZZ_DESCRIPTOR = "Ledu/berkeley/cs/jqf/fuzz/Fuzz;";
    private final String testMethodName;
    private final Type generatorType;
    /**
     * Name of the class being visited.
     */
    private String className;

    FuzzTargetGeneratingClassVisitor(int api, ClassVisitor cv, String testMethodName, String generatorClassName) {
        super(api, cv);
        this.testMethodName = testMethodName;
        this.generatorType = Type.getObjectType(generatorClassName.replace('.', '/'));
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        // Skip existing RunWith annotation
        return RUN_WITH_DESCRIPTOR.equals(descriptor) ? null : super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitEnd() {
        // Add the RunWith annotation
        AnnotationVisitor av = super.visitAnnotation(RUN_WITH_DESCRIPTOR, true);
        av.visit("value", JQF_TYPE);
        av.visitEnd();
        addModifiedTestMethod();
        super.visitEnd();
    }

    private void addModifiedTestMethod() {
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, testMethodName + TARGET_SUFFIX,
                                             TARGET_METHOD_DESCRIPTOR, null, null);
        AnnotationVisitor av = mv.visitAnnotation(FUZZ_DESCRIPTOR, true);
        av.visitEnd();
        av = mv.visitTypeAnnotation(TypeReference.METHOD_FORMAL_PARAMETER, null,
                                    FROM_DESCRIPTOR, true);
        av.visit("value", generatorType);
        av.visitEnd();
        av = mv.visitParameterAnnotation(0, FROM_DESCRIPTOR, true);
        av.visit("value", generatorType);
        av.visitEnd();
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, testMethodName, "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
