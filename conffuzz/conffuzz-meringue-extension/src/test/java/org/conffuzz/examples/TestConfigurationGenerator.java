package org.conffuzz.examples;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.configfuzz.ConfigGenerator;
import edu.berkeley.cs.jqf.fuzz.configfuzz.ConfigTracker;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@SuppressWarnings("rawtypes")
public final class TestConfigurationGenerator extends Generator<Map> {
    public static LinkedList<Map<String, String>> generated = new LinkedList<>();

    public TestConfigurationGenerator() {
        super(Map.class);
    }

    @Override
    public Map<String, String> generate(SourceOfRandomness random, GenerationStatus generationStatus) {
        // Read a boolean to prevent Zest from throwing an exception if the input is empty
        random.nextBoolean();
        Map<String, String> result = new HashMap<>();
        if (!Boolean.getBoolean("preround")) {
            for (Map.Entry<String, String> entry : ConfigTracker.getConfigMap().entrySet()) {
                String value = ConfigGenerator.isNullOrEmpty(entry.getValue()) ? "" : entry.getValue();
                result.put(entry.getKey(),
                           ConfigGenerator.randomValue(entry.getKey(), value, random));
            }
        }
        generated.add(new HashMap<>(result));
        return result;
    }
}
