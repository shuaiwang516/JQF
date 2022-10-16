package edu.berkeley.cs.jqf.fuzz.instrumentTest;

import java.util.Objects;

public class Utils {
    private static String className = System.getProperty("class");
    private static String classMethod = System.getProperty("method");

    public static String getCurrentClass() {
        return className;
    }

    public static String getCurrentMethod() {
        return classMethod;
    }

    public static Boolean isTestClassTransformNeeded (String className) {
        if (Objects.equals(className, getCurrentClass())) {
            return true;
        }
        return false;
    }

    public static Boolean isTestMethodTransformNeeded (String classMethod) {
        if (Objects.equals(className, getCurrentMethod())) {
            return true;
        }
        return false;
    }
}
