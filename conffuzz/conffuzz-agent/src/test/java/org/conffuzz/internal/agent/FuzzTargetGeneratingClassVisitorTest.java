package org.conffuzz.internal.agent;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.java.lang.BooleanGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import org.conffuzz.examples.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FuzzTargetGeneratingClassVisitorTest {
    @BeforeEach
    void setUp() {

    }

    @ParameterizedTest
    @MethodSource("arguments")
    void instrumentedTestRunsLikeOriginal(Class<?> clazz) {
        ClassNode cn = AgentTestUtil.getClassNode(clazz);
        BiFunction<Integer, ClassVisitor, ClassVisitor> function =
                (api, cv) -> new FuzzTargetGeneratingClassVisitor(api, cv, "test",
                                                                  TestBooleanGenerator.class.getName());
        cn = AgentTestUtil.instrument(cn, function);
        Class<?> instrumented = new ByteArrayClassLoader().createClass(cn);
        compareExecutions(clazz, instrumented);
    }

    static void compareExecutions(Class<?> original, Class<?> instrumented) {
        ExampleUtil.values.clear();
        new JUnitCore().run(Request.method(original, "test"));
        List<String> expected = new LinkedList<>(ExampleUtil.values);
        ExampleUtil.values.clear();
        TestBooleanGenerator.callsToGenerate = 0;
        GuidedFuzzing.setGuidance(new TestGuidance());
        try {
            new JUnitCore().run(Request.method(instrumented, "test" + FuzzTargetGeneratingClassVisitor.TARGET_SUFFIX));
        } finally {
            GuidedFuzzing.unsetGuidance();
        }
        // Check that generate was called
       Assertions.assertEquals(1, TestBooleanGenerator.callsToGenerate);
        // Check that the same methods in the test class were called in the same order
        Assertions.assertEquals(expected, ExampleUtil.values);
    }

    static Stream<Class<?>> arguments() {
        return Stream.of(ChildExample.class, ChildExampleWithSetup.class, ChildRunWithExample.class, Example.class,
                         ExampleWithSetup.class, RunWithExample.class);
    }

    static final class TestGuidance implements Guidance {
        private int inputs = 1;

        @Override
        public InputStream getInput() throws IllegalStateException, GuidanceException {
            return new InputStream() {
                @Override
                public int read() {
                    return 0;
                }
            };
        }

        @Override
        public boolean hasInput() {
            return inputs-- > 0;
        }

        @Override
        public void handleResult(Result result, Throwable error) throws GuidanceException {
        }

        @Override
        public void setBlind(boolean blind) {
        }

        @Override
        public Consumer<TraceEvent> generateCallBack(Thread thread) {
            return (t) -> {
            };
        }
    }

    static class ByteArrayClassLoader extends ClassLoader {
        public Class<?> createClass(ClassNode cn) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            byte[] bytes = cw.toByteArray();
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    public static class TestBooleanGenerator extends BooleanGenerator {
        static int callsToGenerate = 0;

        public TestBooleanGenerator(Class<?> ignored) {
        }

        @Override
        public Boolean generate(SourceOfRandomness random, GenerationStatus status) {
            callsToGenerate++;
            return super.generate(random, status);
        }
    }
}