package org.conffuzz.internal.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class ConfFuzzTransformer implements ClassFileTransformer {
    private static final String ANNOTATION_DESC = Type.getDescriptor(ConfFuzzInstrumented.class);
    private final int api;
    private final String testClassInternalName;
    private final String testMethodName;
    private final String generatorClassName;

    ConfFuzzTransformer(int api, String testClassName, String testMethodName, String generatorClassName) {
        this.api = api;
        this.testClassInternalName = testClassName.replace('.', '/');
        this.testMethodName = testMethodName;
        this.generatorClassName = generatorClassName;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        if (shouldDynamicallyInstrument(className, classBeingRedefined)) {
            try {
                return transform(new ClassReader(classFileBuffer));
            } catch (ClassTooLargeException | MethodTooLargeException e) {
                return null;
            } catch (Throwable t) {
                // Print the stack trace for the error to prevent it from being silently swallowed by the JVM
                t.printStackTrace();
                throw t;
            }
        }
        return null;
    }

    byte[] transform(ClassReader cr) {
        if (testClassInternalName.equals(cr.getClassName())) {
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
            if (isAnnotated(cn)) {
                // The class has already been instrumented
                return null;
            }
            // Add an annotation indicating that the class has been instrumented
            cn.visitAnnotation(ANNOTATION_DESC, false);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            cn.accept(new FuzzTargetGeneratingClassVisitor(api, cw, testMethodName, generatorClassName));
            return cw.toByteArray();
        } else {
            // The class is not the test class
            return null;
        }
    }

    private static boolean isAnnotated(ClassNode cn) {
        if (cn.invisibleAnnotations != null) {
            for (AnnotationNode a : cn.invisibleAnnotations) {
                if (ANNOTATION_DESC.equals(a.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldDynamicallyInstrument(String className, Class<?> classBeingRedefined) {
        return classBeingRedefined == null // Class is being loaded and not redefined or retransformed
                // Class is not a dynamically generated accessor for reflection
                && (className == null || !className.startsWith("sun") || className.startsWith("sun/nio"));
    }
}
