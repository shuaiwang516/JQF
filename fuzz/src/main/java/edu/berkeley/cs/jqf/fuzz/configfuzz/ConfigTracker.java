package edu.berkeley.cs.jqf.fuzz.configfuzz;

import java.util.Map;
import java.util.TreeMap;

public class ConfigTracker {

    /** Current test class name */
    private String curTestClass;
    /** Current test method name */
    private String curTestName;
    /** configMap records the exercised configuration parameter by the current fuzzed test */
    private static Map<String, String> configMap = new TreeMap<>();

    public ConfigTracker(String curTestClass, String curTestName) {
        this.curTestClass = curTestClass;
        this.curTestName = curTestName;
    }

    /**
     * Called by projects' configuration API to record exercised
     * configuration parameter by test
     * @param key
     * @param value
     */
    public static void track(String key, String value) {
        configMap.put(key, value);
    }

    /**
     * Get configMap
     * @return A map stores the pairs of configuration parameter name and value
     */
    public static Map<String, String> getConfigMap() {
        if (configMap == null) {
            return new TreeMap<>();
        }
        return configMap;
    }

    /**
     * Clear configMap
     * @return True if clear successes, else false
     */
    public static Boolean freshMap() {
        configMap = new TreeMap<>();
        if (configMap.size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the size of configMap
     * @return
     */
    public static int getMapSize() {
        if (configMap == null) {
            return 0;
        }
        return configMap.size();
    }
}
