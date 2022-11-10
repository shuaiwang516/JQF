package org.conffuzz.internal.agent;

import org.objectweb.asm.Opcodes;

import java.lang.instrument.Instrumentation;

public final class ConfFuzzAgent {
    /**
     * ASM API version implemented by the class and method visitors used by this Agent's transformers.
     */
    public static final int ASM_VERSION = Opcodes.ASM9;

    private ConfFuzzAgent() {
        throw new AssertionError();
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        String[] parts = agentArgs.split(",");
        String testClassName = parts[0];
        String testMethodName = parts[1];
        String generatorClassName = parts[2];
        inst.addTransformer(new ConfFuzzTransformer(ASM_VERSION, testClassName, testMethodName, generatorClassName));
    }
}
