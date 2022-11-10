package org.conffuzz.internal;

import edu.berkeley.cs.jqf.fuzz.configfuzz.DefConfCollectionGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.neu.ccs.prl.meringue.SystemPropertyUtil;

import java.io.File;

public final class FuzzForkMain {
    public static final String TARGET_SUFFIX = "$$CONFFUZZ";
    public static final String PROPERTIES_KEY = "conffuzz.properties";

    private FuzzForkMain() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Throwable {
        // Usage: testClassName testMethodName outputDirectory
        try {
            String testClassName = args[0];
            String testMethodName = args[1] + TARGET_SUFFIX;
            File outputDirectory = new File(args[2]);
            // Note: must set system properties before loading the test class
            SystemPropertyUtil.loadSystemProperties(PROPERTIES_KEY);
            // Run preliminary round to get default configuration
            Guidance guidance = new DefConfCollectionGuidance(null);
            GuidedFuzzing.run(testClassName, testMethodName, guidance, System.out);
            // Run the main campaign
            guidance = new ZestGuidance(testClassName + "#" + testMethodName, null, outputDirectory);
            GuidedFuzzing.run(testClassName, testMethodName, FuzzForkMain.class.getClassLoader(), guidance, System.out);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}
