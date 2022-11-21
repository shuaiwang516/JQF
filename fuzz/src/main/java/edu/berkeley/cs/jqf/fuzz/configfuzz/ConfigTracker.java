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

    /** externalConfigMap records the configuration parameter used by the current fuzzed test without setting */
    private static Map<String, String> externalConfigMap = new TreeMap<>();

    public ConfigTracker(String curTestClass, String curTestName) {
        this.curTestClass = curTestClass;
        this.curTestName = curTestName;
    }

    /**
     * Called by projects' configuration API to record used
     * configuration parameter by test
     * @param key
     * @param value
     */
    public synchronized static void trackGet(String key, String value) {
        if (!configMap.containsKey(key)) {
            externalConfigMap.put(key, value);
        }
        configMap.put(key, value);
    }

    /**
     * Called by projects' configuration API to record 
     * configuration parameter set by test
     * @param key
     * @param value
     */
    public synchronized static void trackSet(String key, String value) {
        configMap.put(key, value);
    }

    /**
     * Get configMap
     * @return A map stores the pairs of configuration parameter name and value
     */
    public synchronized static Map<String, String> getConfigMap() {
        Map<String, String> res = new TreeMap<>();
        res.putAll(externalConfigMap);
        return res;
    }

    /**
     * Clear configMap
     * @return True if clear successes, else false
     */
    public static Boolean freshMap() {
        configMap.clear();
        externalConfigMap.clear();
        if (configMap.size() == 0 && externalConfigMap.size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the size of externalConfigMap
     * @return
     */
    public synchronized static int getMapSize() {
        if (externalConfigMap == null) {
            return 0;
        }
        return externalConfigMap.size();
    }

    public synchronized static void setMap(Map<String, String> map) {
        externalConfigMap.clear();
        configMap.clear();
        externalConfigMap.putAll(map);
    }
}
