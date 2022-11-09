package org.conffuzz.internal;

import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.neu.ccs.prl.meringue.Replayer;
import edu.neu.ccs.prl.meringue.ReplayerManager;
import edu.neu.ccs.prl.meringue.SystemPropertyUtil;

import java.io.File;
import java.io.IOException;

public final class ConfFuzzReplayer implements Replayer {
    private Class<?> testClass;
    private String testMethodName;

    public ConfFuzzReplayer() throws IOException {
        SystemPropertyUtil.loadSystemProperties(FuzzForkMain.PROPERTIES_KEY);
    }

    @Override
    public void configure(String testClassName, String testMethodName, ClassLoader classLoader)
            throws ClassNotFoundException {
        if (testClassName == null || testMethodName == null) {
            throw new NullPointerException();
        }
        this.testMethodName = testMethodName + FuzzForkMain.TARGET_SUFFIX;
        this.testClass = Class.forName(testClassName, true, classLoader);
    }

    @Override
    public void accept(ReplayerManager manager) throws IOException, ClassNotFoundException {
        while (manager.hasNextInput()) {
            File input = manager.nextInput();
            Throwable failure = execute(input);
            manager.handleResult(failure);
        }
    }

    private Throwable execute(File input) throws IOException, ClassNotFoundException {
        ReplayGuidance guidance = new ReplayGuidance(input);
        GuidedFuzzing.run(testClass, testMethodName, guidance, System.out);
        return guidance.getFailure();
    }
}