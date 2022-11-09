package edu.berkeley.cs.jqf.fuzz.configfuzz;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;

import java.io.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReplayData implements Serializable {
    public static final String EXTENSION = ".replay";
    private static final long serialVersionUID = -6570908799876448036L;
    private final byte[] input;
    private final Map<String, String> configurationContext;

    public ReplayData(byte[] input, Map<String, String> configurationContext) {
        this.input = input.clone();
        this.configurationContext = Collections.unmodifiableMap(new LinkedHashMap<>(configurationContext));
    }

    public byte[] getInput() {
        return input;
    }

    public void configure() {
        ConfigTracker.setMap(configurationContext);
    }

    public static ReplayData readReplayData(File saveFile) throws IOException, ClassNotFoundException {
        File replayFile = toReplayFile(saveFile);
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(replayFile.toPath()))) {
            return (ReplayData) in.readObject();
        }
    }

    public static void writeReplayData(File saveFile, Map<String, String> context, ZestGuidance.Input<?> input)
            throws IOException {
        File replayFile = toReplayFile(saveFile);
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(replayFile.toPath()))) {
            out.writeObject(new ReplayData(convert(input), context));
        }
    }

    private static byte[] convert(ZestGuidance.Input<?> input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int b : input) {
            assert (b >= 0 && b < 256);
            out.write(b);
        }
        return out.toByteArray();
    }

    private static File toReplayFile(File saveFile) {
        return new File(saveFile.getParentFile(), saveFile.getName() + EXTENSION);
    }
}
