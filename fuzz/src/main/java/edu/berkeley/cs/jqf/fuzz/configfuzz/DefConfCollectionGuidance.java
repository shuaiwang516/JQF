package edu.berkeley.cs.jqf.fuzz.configfuzz;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Used to run test once to get default exercised configuration set
 */
public class DefConfCollectionGuidance implements Guidance {
    private int executionCounter = 0;
    private boolean finishPreRound = false;
    private final PrintStream out;
    private Random random = new Random();
    private Coverage coverage;
    private String isPreRound = "preround";

    public DefConfCollectionGuidance(PrintStream out) {
        System.setProperty(isPreRound, "true");
        this.out = out;
    }

    @Override
    public InputStream getInput() {
        return Guidance.createInputStream(() -> random.nextInt(256));
    }

    @Override
    public boolean hasInput() {
        return !finishPreRound;
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        executionCounter++;

        // Display error stack trace in case of failure
        if (result == Result.FAILURE) {
            if (out != null) {
                error.printStackTrace(out);
            }
            this.finishPreRound = true;
            System.setProperty(isPreRound, "false");
        }

        if (executionCounter == 1) {
            finishPreRound = true;
            System.setProperty(isPreRound, "false");
        }

        if (result == Result.INVALID) {
            finishPreRound = false;
            System.setProperty(isPreRound, "true");
        }
        System.out.println("[JQF] After pre round flag = " + System.getProperty(isPreRound));
    }

    @Override
    public void setBlind(boolean blind) {
        return;
    }

    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        return getCoverage()::handleEvent;
    }

    public Coverage getCoverage() {
        if (coverage == null) {
            coverage = new Coverage();
        }
        return coverage;
    }
}
