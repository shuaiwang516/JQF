package org.conffuzz.internal.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.function.BiFunction;


public final class AgentTestUtil {
    static ClassNode getClassNode(Class<?> clazz) {
        try {
            ClassNode cn = new ClassNode();
            new ClassReader(clazz.getName()).accept(cn, ClassReader.EXPAND_FRAMES);
            return cn;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static ClassNode expandFramesAndComputeMaxStack(ClassNode cn) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        ClassReader cr = new ClassReader(cw.toByteArray());
        ClassNode result = new ClassNode();
        cr.accept(result, ClassReader.EXPAND_FRAMES);
        return result;
    }

    static ClassNode instrument(ClassNode cn, BiFunction<Integer, ClassVisitor, ClassVisitor> f) {
        ClassNode result = new ClassNode();
        cn = expandFramesAndComputeMaxStack(cn);
        cn.accept(f.apply(ConfFuzzAgent.ASM_VERSION, result));
        return expandFramesAndComputeMaxStack(result);
    }
}
