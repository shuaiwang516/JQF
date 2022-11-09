package org.conffuzz.internal;

import edu.berkeley.cs.jqf.fuzz.configfuzz.ReplayData;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ReplayGuidance implements Guidance {
    private final ReplayData replayData;
    private boolean consumed = false;
    private Throwable failure = null;

    public ReplayGuidance(File input) throws IOException, ClassNotFoundException {
        this.replayData = ReplayData.readReplayData(input);
    }

    @Override
    public InputStream getInput() throws IllegalStateException, GuidanceException {
        replayData.configure();
        return new ByteArrayInputStream(replayData.getInput());
    }

    @Override
    public boolean hasInput() {
        boolean result = !consumed;
        consumed = true;
        return result;
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        if (result == Result.FAILURE) {
            this.failure = error;
        }
    }

    @Override
    public void setBlind(boolean blind) {
    }

    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        return e -> {
        };
    }

    public Throwable getFailure() {
        return failure;
    }

    public static File[] getInputs(File directory) {
        List<File> list = new ArrayList<>();
        for (File f : Objects.requireNonNull(directory.listFiles())) {
            if (f.getName().startsWith("id_") && !f.getName().endsWith(ReplayData.EXTENSION)) {
                list.add(f);
            }
        }
        return list.toArray(new File[0]);
    }
}
