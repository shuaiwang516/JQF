package org.conffuzz.internal;

import edu.berkeley.cs.jqf.fuzz.configfuzz.ConfigTracker;
import edu.berkeley.cs.jqf.fuzz.configfuzz.DefConfCollectionGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import org.conffuzz.examples.ReplayGuidanceTestExample;
import org.conffuzz.examples.TestConfigurationGenerator;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ReplayGuidanceTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    @After
    public void clearGenerated() {
        TestConfigurationGenerator.generated.clear();
    }

    @Test
    public void testReplayReproducesOriginal() throws IOException, ClassNotFoundException {
        File constraintFile = folder.newFile();
        // It is not ideal to be setting properties but necessary for current implementation
        System.setProperty("constraint.file", constraintFile.getAbsolutePath());
        System.setProperty("configFuzz", "true");
        File outputDirectory = folder.newFolder();
        Map<String, String> original = fuzz(outputDirectory);
        Collection<File> inputs = getFailureInducingInputs(outputDirectory);
        Assert.assertEquals(1, inputs.size());
        Map<String, String> reproduction = reproduce(inputs.iterator().next());
        Assert.assertEquals(original, reproduction);
    }

    static Map<String, String> fuzz(File outputDirectory) throws IOException, ClassNotFoundException {
        ConfigTracker.freshMap();
        TestConfigurationGenerator.generated.clear();
        // Run the preliminary round to collect the default configuration
        runCampaign(new DefConfCollectionGuidance(new PrintStream(new NullOutputStream())));
        // Runt the campaign until the first failure is found
        runCampaign(new TestGuidance("test", outputDirectory));
        return TestConfigurationGenerator.generated.getLast();
    }

    static Map<String, String> reproduce(File input) throws ClassNotFoundException, IOException {
        runCampaign(new ReplayGuidance(input));
        return TestConfigurationGenerator.generated.getLast();
    }

    static List<File> getFailureInducingInputs(File outputDirectory) {
        return Arrays.asList(ReplayGuidance.getInputs(new File(outputDirectory, "failures")));
    }

    static void runCampaign(Guidance guidance) throws ClassNotFoundException {
        GuidedFuzzing.run(ReplayGuidanceTestExample.class.getName(), "test",
                          ReplayGuidanceTest.class.getClassLoader(),
                          guidance, new PrintStream(new NullOutputStream()));
    }

    static class TestGuidance extends ZestGuidance {
        private boolean hasNext = true;

        public TestGuidance(String testName, File outputDirectory) throws IOException {
            super(testName, null, outputDirectory);
        }

        @Override
        public boolean hasInput() {
            return hasNext;
        }

        @Override
        public void handleResult(Result result, Throwable error) throws GuidanceException {
            super.handleResult(result, error);
            if (result == Result.FAILURE) {
                hasNext = false;
            }
        }
    }

    static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) {
        }
    }
}