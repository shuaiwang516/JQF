package edu.berkeley.cs.jqf.fuzz.instrumentTest;

import edu.berkeley.cs.jqf.fuzz.log.Log;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Objects;

public class Agent {
    /** Java Agent */
    private static Instrumentation instrumentation;

    /**
     * Static instrumentation
     * @param agentArgs
     * @param inst
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        Log.d2f("premain_haha");
        inst.addTransformer(new FuzzAnnotationCFT());
        throw new RuntimeException("123123");
    }

    /** Dynamical instrumentation for/from Maven
     * @param agentArgs
     * @param inst
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        Log.d2f("agentmain_haha");
        inst.addTransformer(new FuzzAnnotationCFT(), true);
        reTransformMaven(inst);
        Log.d2f("agentmain_haha123");
    }

    /**
     * Instrument maven surefire classes if they have already
     * been loaded into JVM.
     * @param instrumentation
     */
    private static void reTransformMaven(Instrumentation instrumentation) {
        for(Class<?> clz : instrumentation.getAllLoadedClasses()) {
            try {
                instrumentation.retransformClasses(clz);
            } catch (UnmodifiableClassException e) {
                System.err.println("[CONFFUZZ-ERROR]: Unable to retransform Maven classes: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
