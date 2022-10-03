package edu.berkeley.cs.jqf.fuzz.configfuzz;

import java.util.Map;
import java.util.TreeMap;

public class ConfigTracker {

    private static Map<String, String> configMap = new TreeMap<>();
    private String curTestClass;
    private String curTestName;
    private static int counter = 0;

    public ConfigTracker(String curTestClass, String curTestName) {
        this.curTestClass = curTestClass;
        this.curTestName = curTestName;
    }

    public static void track(String key, String value) {
        configMap.put(key, value);
    }

    public static Map<String, String> getConfigMap() {
        if (configMap == null) {
            return new TreeMap<>();
        }
        return configMap;
    }

    public static Boolean freshMap() {
        configMap = new TreeMap<>();
        if (configMap.size() == 0) {
            return true;
        }
        return false;
    }

    public static int getMapSize() {
        if (configMap == null) {
            return 0;
        }
        return configMap.size();
    }
}
