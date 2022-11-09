package org.conffuzz.internal.agent;

import org.objectweb.asm.Opcodes;

import java.lang.instrument.Instrumentation;

public final class ConfFuzzAgent {
    /**
     * ASM API version implemented by the class and method visitors used by transformers.
     */
    public static final int ASM_VERSION = Opcodes.ASM9;

    private ConfFuzzAgent() {
        throw new AssertionError();
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        String[] parts = agentArgs.split(",");
        inst.addTransformer(new ConfFuzzTransformer(ASM_VERSION, parts[0], parts[1]));
    }
}
