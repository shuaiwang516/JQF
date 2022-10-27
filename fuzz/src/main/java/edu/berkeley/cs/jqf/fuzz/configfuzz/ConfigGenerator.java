package edu.berkeley.cs.jqf.fuzz.configfuzz;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class ConfigGenerator {

    /** Separator to split parameter key and value */
    private static String PARAM_EQUAL_MARK = "=";
    /** Separator to split different parameters */
    private static String PARAM_VALUE_SPLITOR = ";";
    /** File path that stores all parameter constraints (e.g., valid values) */
    private static String constraintFile = null;
    /** Mapping that keeps all parameter valid values supported (from the constraint file) */
    private static Map<String, List<String>> paramConstraintMapping;
    /** Flag to print Debug Information */
    private static boolean debugEnabled = Boolean.getBoolean("generator.debug");
    /** Flag to print Debug Information */
    private static boolean notString = Boolean.getBoolean("generator.nostring");

    static {
        try {
            constraintFile = System.getProperty("constraint.file", "constraint");
            paramConstraintMapping = parseParamConstraint();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Read configuration parameter constraints from file;
     * @return A map that stores the pair of parameter name (string) and potential valid values (list of string)
     */
    public static Map<String, List<String>> getParamConstraintMapping() {
        if (paramConstraintMapping == null || paramConstraintMapping.size() == 0) {
            try {
                constraintFile = System.getProperty("constraint.file", "constraint");
                paramConstraintMapping = parseParamConstraint();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
        return paramConstraintMapping;
    }

    /**
     * Return a random value based on the type of @param value
     * @param value
     * @return
     */
    // TODO: Types can be cached the type rather than doing it for multiple times.
    // Pass the type instead of the value and check the type
    public static String randomValue(String name, String value, SourceOfRandomness random) {
        // TODO: Next to find a way to randomly generate string that we don't know
        // Some parameter may only be able to fit into such values
        if (paramHasConstraints(name)) {
            debugPrint("In Constraint!!");
            return randomValueFromConstraint(name, random);
        }
        if (isBoolean(value)) {
            return String.valueOf(random.nextBoolean());
        } else if (isInteger(value)) {
            return String.valueOf(Math.abs(random.nextInt()));
        } else if (isFloat(value)) {
            return String.valueOf(Math.abs(random.nextFloat()));
        }
        // for now we only fuzz numeric and boolean configuration parameters.
        String returnStr;
        if (notString) {
            returnStr = value;
        } else {
	    byte[] bytes = random.nextBytes(10);
            returnStr = Base64.getEncoder().encodeToString(bytes);
	    System.out.print("Key=" + name + ", bytes=");
	    System.out.println(Arrays.toString(bytes));
        }
        return returnStr;
    }

    /**
     * Return a random value from paramConstraintMapping if the parameter has constraint
     * @param name parameter name
     * @param random
     * @return
     */
    private static String randomValueFromConstraint(String name, SourceOfRandomness random) {
        return random.choose(paramConstraintMapping.get(name));
    }

    /**
     * Check whether a parameter has constraint
     * @param name parameter name
     * @return True if has constraint
     */
    private static boolean paramHasConstraints(String name) {
        debugPrint(String.valueOf(paramConstraintMapping.containsKey(name)));
        return paramConstraintMapping.containsKey(name);
    }


    /** Helper Functions */
    private static Map<String, List<String>> parseParamConstraint() throws IOException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        File file = Paths.get(constraintFile).toFile();
        if (!file.exists() || !file.isFile()){
            throw new IOException("Unable to read file: " + file.getPath() + "; Please make sure to set " +
                    "-Dconstraint.file with the correct file path");
        }
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int index;
        while ((line = br.readLine()) != null) {
            index = line.indexOf(PARAM_EQUAL_MARK);
            if (index != -1) {
                String name = line.substring(0, index).trim();
                String[] values = line.substring(index + 1).split(PARAM_VALUE_SPLITOR);
                List<String> valueList = new ArrayList(Arrays.asList(values));
                result.put(name, valueList);
            }
        }
        return result;
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static boolean isBoolean(String value) {
        String trimStr = value.toLowerCase().trim();
        if (trimStr.equals("true") || trimStr.equals("false")) {
            return true;
        }
        return false;
    }

    private static boolean isFloat(String value) {
        try {
            Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.equals("null") || value.equals("");
    }

    /** For Internal test */
    public static void printListMap(Map<String, List<String>> map) {
        System.out.println("In printing!!");
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = "";
            for (String s : entry.getValue()) {
                value = value + ";" + s;
            }
            System.out.println(name + "=" + value);
        }
    }

    public static void printMap(Map<String, String> map) {
        System.out.println("In printing!!");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            System.out.println(name + "=" + value);
        }
        System.out.println("Number of map size = " + map.size());
    }

    public static void debugPrint(String str) {
        if (debugEnabled) {
            System.out.println(str);
        }
    }
}
